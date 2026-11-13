package dev.proxylink.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Size
import android.nfc.NfcAdapter
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.proxylink.LinkService
import dev.proxylink.R
import dev.proxylink.core.Capabilities
import dev.proxylink.core.Feature
import dev.proxylink.core.Link
import dev.proxylink.core.LinkState
import dev.proxylink.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class ShareMode { SHEET, NFC, QR }

// small preview of a shared image, made here from the shared uri while the permission is valid; nothing is stored
@Composable
private fun UriThumb(uri: Uri) {
    val ctx = LocalContext.current
    val bmp by produceState<ImageBitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            if (ctx.contentResolver.getType(uri)?.startsWith("image/") != true) return@withContext null
            runCatching { ctx.contentResolver.loadThumbnail(uri, Size(240, 240), null).asImageBitmap() }.getOrNull()
                ?: runCatching {
                    ctx.contentResolver.openInputStream(uri)?.use { s ->
                        BitmapFactory.decodeStream(s, null, BitmapFactory.Options().apply { inSampleSize = 8 })
                    }?.asImageBitmap()
                }.getOrNull()
        }
    }
    bmp?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(72.dp).clip(MaterialTheme.shapes.small)) }
}

// "x:" keys: "pc:<id>" a paired pc, "dev:<id>" another device through the connected pc
private fun normalize(k: String?): String? = when {
    k == null || k == "pc" -> null           // the old shortcut id, means the connected pc
    k.startsWith("pc:") || k.startsWith("dev:") -> k
    else -> "dev:$k"                         // ids of other devices from older shortcuts
}

// the confirmation of a share from the android share sheet. the content is kept here until the user presses send.
// there is one link at a time: another pc is reached by switching the connection, never by pretending a second one
@Composable
fun ShareConfirm(uris: List<Uri>, text: String?, shortcutId: String?, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val names = remember(uris) {
        uris.map { u ->
            runCatching {
                ctx.contentResolver.query(u, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
            }.getOrNull() ?: u.lastPathSegment ?: "file"
        }
    }
    var key by remember {
        mutableStateOf(normalize(shortcutId) ?: Link.connectedPcId?.let { "pc:$it" } ?: Link.known.firstOrNull()?.let { "pc:${it.id}" })
    }
    val k = key
    val isDev = k?.startsWith("dev:") == true
    val id = k?.substringAfter(':')
    val connected = Link.state == LinkState.CONNECTED
    val dev = if (isDev) Link.others.firstOrNull { it.id == id } else null
    val pc = if (!isDev) Link.known.firstOrNull { it.id == id } else null
    val ready = if (isDev) connected && dev != null else connected && Link.connectedPcId == id
    val name = dev?.name ?: pc?.name ?: stringResource(R.string.share_unavailable_device)
    val hasContent = uris.isNotEmpty() || !text.isNullOrBlank()
    val canSend = ready && hasContent && Capabilities.supports(if (uris.isNotEmpty()) Feature.FILES else Feature.TEXT)

    // nfc and the qr scanner open inside this window, over the shared content, so nothing is lost while connecting
    var mode by remember { mutableStateOf(ShareMode.SHEET) }
    val nfcAvailable = remember { NfcAdapter.getDefaultAdapter(ctx) != null }
    BackHandler(enabled = mode != ShareMode.SHEET) { mode = ShareMode.SHEET }

    fun send() {
        // the files are opened (streams held) inside Link.sendFile before this window closes, the foreground service keeps the process
        // alive while the transfer runs, also after this window is gone
        runCatching { ctx.startForegroundService(android.content.Intent(ctx, LinkService::class.java)) }
        val prev = Link.targetId
        Link.targetId = if (isDev) id else null
        // sendFile is false when the shared file could not be opened; the message says only what really started
        val total = if (uris.isNotEmpty()) uris.size else 1
        val started = if (uris.isNotEmpty()) uris.count { Link.sendFile(it) } else if (!text.isNullOrBlank()) { Link.sendText(text); 1 } else 0
        Link.targetId = prev
        Toast.makeText(
            ctx,
            when {
                started == 0 -> ctx.getString(R.string.share_open_failed)
                started < total -> ctx.getString(R.string.share_open_partial, total - started, name)
                else -> ctx.getString(R.string.toast_sent_to, name)
            },
            Toast.LENGTH_LONG,
        ).show()
        onDone()
    }

    if (mode != ShareMode.SHEET) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (mode == ShareMode.NFC) NfcScreen(onClose = { mode = ShareMode.SHEET }, onScan = { mode = ShareMode.QR })
            else ScannerScreen(onResult = { raw -> Link.pairFromQr(raw) }, onClose = { mode = ShareMode.SHEET })
        }
        return
    }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.BottomCenter) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.share_confirm_title), style = MaterialTheme.typography.titleLarge)

                // what is going to be sent
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (uris.isNotEmpty()) { UriThumb(uris.first()); Spacer(Modifier.width(12.dp)) }
                        Text(
                            if (uris.isNotEmpty()) {
                                if (uris.size == 1) names[0] else stringResource(R.string.share_files_count, uris.size) + ": " + names.first()
                            } else text.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // where it is going: the chosen target, its state and the way to change it
                Panel {
                    Text(stringResource(R.string.share_target_line, name), style = MaterialTheme.typography.titleMedium)
                    Text(
                        when {
                            ready -> stringResource(R.string.connected)
                            isDev -> stringResource(R.string.share_device_offline)
                            connected -> stringResource(R.string.share_connected_other, Link.pcName)
                            Link.state == LinkState.CONNECTING -> stringResource(R.string.nc_connecting, name)
                            Link.reconnecting == id -> stringResource(R.string.nc_searching)
                            else -> stringResource(R.string.nc_not_connected)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // ways to connect to the chosen pc, the shared content stays in this window
                    if (!isDev && !ready && id != null && pc != null) {
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.horizontalScrollCompat(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (connected) {
                                AppOutlinedButton(onClick = { Link.disconnect(); Link.reconnect(id) }) { Text(stringResource(R.string.share_switch)) }
                            } else if (Link.state == LinkState.IDLE && Link.reconnecting != id) {
                                AppOutlinedButton(onClick = { Link.reconnect(id) }) { Text(stringResource(R.string.nc_connect)) }
                            }
                            if (!connected && Link.state == LinkState.IDLE) {
                                if (nfcAvailable) AppOutlinedButton(onClick = { mode = ShareMode.NFC }) { Text(stringResource(R.string.share_nfc)) }
                                AppOutlinedButton(onClick = { mode = ShareMode.QR }) { Text(stringResource(R.string.nc_scan_qr)) }
                            }
                        }
                    }
                    if (Link.known.size > 1 || Link.others.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.horizontalScrollCompat(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Link.known.forEach { d ->
                                FilterChip(selected = key == "pc:${d.id}", onClick = { key = "pc:${d.id}" }, label = { Text(d.name) })
                            }
                            if (connected) Link.others.forEach { o ->
                                FilterChip(selected = key == "dev:${o.id}", onClick = { key = "dev:${o.id}" }, label = { Text(o.name) })
                            }
                        }
                    }
                }
                if (k == null) {
                    StateNote(stringResource(R.string.share_no_paired))
                }

                // why the send button is dimmed when the target itself is connected: the pc lacks the feature
                if (ready && hasContent && !canSend) {
                    Text(
                        stringResource(if (uris.isNotEmpty()) R.string.qa_files_unsupported else R.string.share_text_unsupported),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                AdaptiveButtons {
                    AppButton(onClick = { send() }, enabled = canSend) { Text(stringResource(R.string.share_send)) }
                    AppOutlinedButton(onClick = onDone) { Text(stringResource(R.string.cancel)) }
                }
            }
        }
    }
}
