package dev.proxylink.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.proxylink.core.Link
import dev.proxylink.core.LinkState
import dev.proxylink.ui.components.ItemRow

@Composable
fun ClipboardScreen() {
    var tab by remember { mutableIntStateOf(0) }
    val list = if (tab == 0) Link.received else Link.sent

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("clipboard", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        TabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
            Tab(selected = tab == 0, onClick = { tab = 0 }) { Text("received", Modifier.padding(12.dp)) }
            Tab(selected = tab == 1, onClick = { tab = 1 }) { Text("sent", Modifier.padding(12.dp)) }
        }
        Spacer(Modifier.height(12.dp))

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (list.isEmpty()) item { Text("empty", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(list) { ItemRow(it) }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("auto sync", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Switch(checked = Link.autoSync, onCheckedChange = { Link.setAutoSync(it) })
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { Link.pushClipboard(manual = true) },
            enabled = Link.state == LinkState.CONNECTED,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("send current clipboard") }
    }
}
