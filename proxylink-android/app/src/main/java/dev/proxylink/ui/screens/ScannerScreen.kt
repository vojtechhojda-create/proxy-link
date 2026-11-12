package dev.proxylink.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import androidx.compose.ui.res.stringResource
import dev.proxylink.R
import dev.proxylink.core.Link
import dev.proxylink.core.LinkState
import dev.proxylink.ui.components.AppButton
import dev.proxylink.ui.components.AppOutlinedButton
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

// text and icons over the camera picture are white in every palette, also in the light one
private val OnScrim = androidx.compose.ui.graphics.Color.White

// own qr scanner: camerax preview + ml kit barcode scanning (bundled model, works offline)
@Composable
fun ScannerScreen(onResult: (String) -> Boolean, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) ask.launch(Manifest.permission.CAMERA) }
    var torch by remember { mutableStateOf(false) }
    // after a right code the camera is replaced by the connection result, sending a file stays an explicit action
    var scanned by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim)) {
        if (scanned) {
            ScanResult(onAgain = { scanned = false }, onClose = onClose)
        } else if (granted) {
            CameraPreview(torch = torch, onCode = { raw ->
                val ok = onResult(raw)
                if (ok) scanned = true else Toast.makeText(ctx, R.string.not_proxylink_code, Toast.LENGTH_SHORT).show()
                ok
            })
            Box(
                Modifier.align(Alignment.Center).size(240.dp)
                    .border(2.dp, OnScrim, MaterialTheme.shapes.medium),
            )
            Text(
                stringResource(R.string.scan_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = OnScrim,
                modifier = Modifier.align(Alignment.Center).offset(y = 150.dp),
            )
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = OnScrim)
                }
                IconButton(onClick = { torch = !torch }) {
                    Icon(
                        if (torch) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = stringResource(if (torch) R.string.torch_off else R.string.torch_on),
                        tint = OnScrim,
                    )
                }
            }
            // the other direction (this phone shows a qr code, the pc scans it) needs a pc that reads qr codes.
            // the current pc app and protocol do not, so the button stays disabled and says why
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppOutlinedButton(onClick = {}, enabled = PHONE_QR_SUPPORTED, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.generate_qr))
                }
                if (!PHONE_QR_SUPPORTED) {
                    Text(
                        stringResource(R.string.generate_qr_unsupported),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnScrim,
                    )
                }
            }
        } else {
            Column(
                Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.camera_needed),
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnScrim,
                )
                AppButton(onClick = { ask.launch(Manifest.permission.CAMERA) }) { Text(stringResource(R.string.allow_camera)) }
                AppOutlinedButton(onClick = onClose) { Text(stringResource(R.string.close)) }
            }
        }
    }
}

// pc app does not read qr codes from phones yet (no scanner, no protocol message). change together with the pc side
private const val PHONE_QR_SUPPORTED = false

@Composable
private fun BoxScope.ScanResult(onAgain: () -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    // a normal connection does not stop here: once the link really is connected the screen closes and the app shows its standard UI.
    // only a state change seen after the scan counts, so an older connection does not close the screen before the new one is made.
    // the single button below stays as a fallback when no change was seen. failures and the wait still show their own result
    var sawOther by remember { mutableStateOf(false) }
    LaunchedEffect(Link.state) {
        if (Link.state != LinkState.CONNECTED) sawOther = true
        else if (sawOther) {
            Toast.makeText(ctx, ctx.getString(R.string.scan_connected, Link.pcName), Toast.LENGTH_SHORT).show()
            onClose()
        }
    }
    val failed = Link.state == LinkState.NEEDS_PAIR && Link.pairError
    Column(
        Modifier.align(Alignment.Center).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            when {
                Link.state == LinkState.CONNECTED -> stringResource(R.string.scan_connected, Link.pcName)
                failed || Link.state == LinkState.IDLE || Link.state == LinkState.NEEDS_PAIR -> stringResource(R.string.scan_failed)
                else -> stringResource(R.string.scan_connecting, Link.pcName)
            },
            style = MaterialTheme.typography.titleMedium,
            color = OnScrim,
        )
        when {
            Link.state == LinkState.CONNECTED -> {
                AppButton(onClick = onClose) { Text(stringResource(R.string.done)) }
            }
            failed || Link.state == LinkState.IDLE || Link.state == LinkState.NEEDS_PAIR -> {
                AppButton(onClick = onAgain) { Text(stringResource(R.string.scan_again)) }
                AppOutlinedButton(onClick = onClose) { Text(stringResource(R.string.close)) }
            }
            else -> AppOutlinedButton(onClick = onClose) { Text(stringResource(R.string.close)) }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreview(torch: Boolean, onCode: (String) -> Boolean) {
    val owner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
    }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val latest by rememberUpdatedState(onCode)
    val busy = remember { AtomicBoolean(false) }

    DisposableEffect(Unit) {
        onDispose {
            provider?.unbindAll()
            runCatching { scanner.close() }
            executor.shutdown()
        }
    }
    LaunchedEffect(camera, torch) {
        camera?.takeIf { it.cameraInfo.hasFlashUnit() }?.cameraControl?.enableTorch(torch)
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { c ->
            val view = PreviewView(c)
            val future = ProcessCameraProvider.getInstance(c)
            future.addListener({
                val p = future.get()
                provider = p
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { proxy ->
                    val img = proxy.image
                    if (img == null || busy.get()) {
                        proxy.close()
                        return@setAnalyzer
                    }
                    scanner.process(InputImage.fromMediaImage(img, proxy.imageInfo.rotationDegrees))
                        .addOnSuccessListener { codes ->
                            val raw = codes.firstNotNullOfOrNull { it.rawValue }
                            if (raw != null && busy.compareAndSet(false, true)) {
                                ContextCompat.getMainExecutor(c).execute {
                                    // a wrong code re-arms the scanner after a moment, a right one ends the screen
                                    if (!latest(raw)) Handler(Looper.getMainLooper()).postDelayed({ busy.set(false) }, 1500)
                                }
                            }
                        }
                        .addOnCompleteListener { proxy.close() }
                }
                p.unbindAll()
                camera = p.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }, ContextCompat.getMainExecutor(c))
            view
        },
    )
}
