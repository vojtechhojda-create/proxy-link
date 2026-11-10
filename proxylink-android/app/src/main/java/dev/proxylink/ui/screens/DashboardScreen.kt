package dev.proxylink.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.proxylink.core.History
import dev.proxylink.core.Link
import dev.proxylink.core.LinkState
import dev.proxylink.ui.components.*

@Composable
fun DashboardScreen(onScan: () -> Unit, onNfc: () -> Unit, onShowAll: () -> Unit) {
    if (Link.state != LinkState.CONNECTED) {
        NotConnectedScreen(onScan, onNfc)
        return
    }
    val actions = rememberSendActions()
    val recent = History.items.filter { it.dir == "in" }.take(3)
    val transfers = Link.transfers.values.sortedByDescending { it.ts }.take(3)

    Column(
        Modifier.fillMaxSize().verticalScrollCompat().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader("proxylink")

        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (Link.guest) "guest · ${Link.pcName}" else Link.pcName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        if (Link.latency >= 0) "connected · ${Link.latency} ms" else "connected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { Link.disconnect() }) { Text("disconnect") }
            }
        }

        TargetPicker()
        SendButtons(actions)

        if (transfers.isNotEmpty()) {
            Panel {
                SectionTitle("transfers")
                transfers.forEach { TransferRow(it) }
            }
        }

        SectionTitle("recent")
        if (recent.isEmpty()) {
            Text("nothing received yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            recent.forEach { HistoryRow(it, actions = false) }
            TextButton(onClick = onShowAll) { Text("all items") }
        }

        if (Link.others.isNotEmpty()) {
            SectionTitle("devices")
            Panel {
                Text("${Link.pcName} · pc", style = MaterialTheme.typography.bodyLarge)
                Link.others.forEach { Text(it.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp)) }
            }
        }
    }
}
