package dev.proxylink.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.proxylink.core.History
import dev.proxylink.ui.components.*

private val filters = listOf("all" to "all", "text" to "text", "link" to "links", "file" to "files", "image" to "images")

@Composable
fun ShareScreen() {
    val actions = rememberSendActions()
    var filter by rememberSaveable { mutableStateOf("all") }
    val shown by remember { derivedStateOf { if (filter == "all") History.items.toList() else History.items.filter { it.type == filter } } }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeader("share")
        TargetPicker()
        SendButtons(actions)
        Row(Modifier.horizontalScrollCompat(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filters.forEach { (key, label) ->
                FilterChip(selected = filter == key, onClick = { filter = key }, label = { Text(label) })
            }
        }
        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            if (shown.isEmpty()) item {
                Text("nothing here yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(shown, key = { it.id }) { HistoryRow(it, actions = true) }
        }
    }
}
