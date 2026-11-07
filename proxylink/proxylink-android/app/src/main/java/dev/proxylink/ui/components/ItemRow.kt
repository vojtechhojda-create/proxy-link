package dev.proxylink.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.proxylink.core.Item
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// tap = open the file, or copy the text to the local clipboard
@Composable
fun ItemRow(item: Item) {
    val cb = LocalClipboardManager.current
    val ctx = LocalContext.current
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable {
            val u = item.uri
            if (u != null) {
                val uri = Uri.parse(u)
                runCatching {
                    ctx.startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setDataAndType(uri, ctx.contentResolver.getType(uri) ?: "*/*")
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            } else {
                cb.setText(AnnotatedString(item.text))
            }
        },
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(item.text, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(
                "${item.kind} · ${item.from} · ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(item.ts))}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
