package dev.proxylink.ui.components

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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.proxylink.core.Item
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// tap = copy to local clipboard
@Composable
fun ItemRow(item: Item) {
    val cb = LocalClipboardManager.current
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable { cb.setText(AnnotatedString(item.text)) },
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
