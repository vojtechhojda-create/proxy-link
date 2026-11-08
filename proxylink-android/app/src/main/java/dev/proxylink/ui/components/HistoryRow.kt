package dev.proxylink.ui.components

import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import android.util.Size
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Link as LinkIcon
import androidx.compose.material.icons.filled.Image as ImageIcon
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.proxylink.core.History
import dev.proxylink.core.Item
import dev.proxylink.core.Link
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun Item.icon(): ImageVector = when (type) {
    "image" -> Icons.Default.ImageIcon
    "file" -> Icons.Default.Description
    "link" -> Icons.Default.LinkIcon
    else -> Icons.Default.TextSnippet
}

// thumbnail for images (loaded by the system, nothing is downloaded), an icon for everything else
@Composable
private fun Thumb(item: Item) {
    val ctx = LocalContext.current
    val bmp by produceState<ImageBitmap?>(null, item.uri, item.type) {
        val u = item.uri
        value = if (u != null && item.type == "image") {
            withContext(Dispatchers.IO) {
                runCatching { ctx.contentResolver.loadThumbnail(Uri.parse(u), Size(160, 160), null).asImageBitmap() }.getOrNull()
            }
        } else null
    }
    Box(
        Modifier.size(44.dp).clip(MaterialTheme.shapes.small).then(Modifier),
        contentAlignment = Alignment.Center,
    ) {
        val b = bmp
        if (b != null) Image(b, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Icon(item.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun HistoryRow(item: Item, actions: Boolean) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val haptic = rememberHaptic()
    val canCopy = item.kind != "file"

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().clickable {
            val u = item.uri
            when {
                u != null -> runCatching {
                    val uri = Uri.parse(u)
                    ctx.startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setDataAndType(uri, ctx.contentResolver.getType(uri) ?: "*/*")
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }.onFailure { Toast.makeText(ctx, "no app can open this", Toast.LENGTH_SHORT).show() }
                item.kind == "link" -> runCatching {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.text)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                else -> clipboard.setText(AnnotatedString(item.text))
            }
        },
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Thumb(item)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.text, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (item.dir == "in") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = if (item.dir == "in") "received" else "sent",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${item.from} · " + DateUtils.formatDateTime(
                                ctx, item.ts, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL,
                            ) + if (item.size > 0 && item.kind == "file") " · ${fmtSize(item.size)}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (actions) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(enabled = canCopy, onClick = { haptic(); clipboard.setText(AnnotatedString(item.text)) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "copy")
                    }
                    IconButton(onClick = {
                        haptic()
                        if (!Link.resend(item)) Toast.makeText(ctx, "not connected, or the file is gone", Toast.LENGTH_SHORT).show()
                    }) { Icon(Icons.Default.Replay, contentDescription = "send again") }
                    IconButton(onClick = { haptic(); History.remove(item.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "delete")
                    }
                }
            }
        }
    }
}
