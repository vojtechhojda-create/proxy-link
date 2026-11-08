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
import dev.proxylink.ui.components.AppButton
import dev.proxylink.ui.components.AppOutlinedButton
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

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

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim)) {
        if (granted) {
            CameraPreview(torch = torch, onCode = { raw ->
                val ok = onResult(raw)
                if (!ok) Toast.makeText(ctx, "not a proxylink code", Toast.LENGTH_SHORT).show()
                ok
            })
            Box(
                Modifier.align(Alignment.Center).size(240.dp)
                    .border(2.dp, MaterialTheme.colorScheme.inverseSurface, MaterialTheme.shapes.medium),
            )
            Text(
                "point at the qr code on the pc",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.inverseSurface,
                modifier = Modifier.align(Alignment.Center).offset(y = 150.dp),
            )
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "close", tint = MaterialTheme.colorScheme.inverseSurface)
                }
                IconButton(onClick = { torch = !torch }) {
                    Icon(
                        if (torch) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = if (torch) "turn torch off" else "turn torch on",
                        tint = MaterialTheme.colorScheme.inverseSurface,
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
                    "the camera is needed to scan the qr code",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.inverseSurface,
                )
                AppButton(onClick = { ask.launch(Manifest.permission.CAMERA) }) { Text("allow camera") }
                AppOutlinedButton(onClick = onClose) { Text("close") }
            }
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
