package dev.proxylink.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.proxylink.R
import dev.proxylink.core.History
import dev.proxylink.ui.components.*

private val filters = listOf(
    "all" to R.string.filter_all, "text" to R.string.filter_text, "link" to R.string.filter_links,
    "file" to R.string.filter_files, "image" to R.string.filter_images,
)

@Composable
fun ShareScreen() {
    val actions = rememberSendActions()
    var filter by rememberSaveable { mutableStateOf("all") }
    val shown by remember { derivedStateOf { if (filter == "all") History.items.toList() else History.items.filter { it.type == filter } } }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeader(stringResource(R.string.tab_share))
        TargetStatus()
        TargetPicker()
        SendButtons(actions)
        Row(Modifier.horizontalScrollCompat(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filters.forEach { (key, label) ->
                FilterChip(selected = filter == key, onClick = { filter = key }, label = { Text(stringResource(label)) })
            }
        }
        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            if (shown.isEmpty()) item {
                Text(stringResource(R.string.nothing_here), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(shown, key = { it.id }) { Box(Modifier.animateItem()) { HistoryRow(it, actions = true) } }
        }
    }
}
