package dev.proxylink.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.proxylink.R
import dev.proxylink.core.History
import dev.proxylink.core.Link
import dev.proxylink.core.LinkState
import dev.proxylink.ui.components.*

// connecting fades between the two states instead of jumping
@Composable
fun DashboardScreen(onScan: () -> Unit, onNfc: () -> Unit, onShowAll: () -> Unit) {
    Crossfade(targetState = Link.state == LinkState.CONNECTED, animationSpec = tween(200), label = "dashboard") { connected ->
        if (connected) ConnectedDashboard(onShowAll) else NotConnectedScreen(onScan, onNfc)
    }
}

@Composable
private fun ConnectedDashboard(onShowAll: () -> Unit) {
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
                        if (Link.guest) stringResource(R.string.guest_name, Link.pcName) else Link.pcName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        if (Link.latency >= 0) stringResource(R.string.connected_ms, Link.latency.toInt()) else stringResource(R.string.connected),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { Link.disconnect() }) { Text(stringResource(R.string.disconnect)) }
            }
        }

        QuickActionsPanel(actions)

        if (transfers.isNotEmpty()) {
            Panel {
                SectionTitle(stringResource(R.string.section_transfers))
                transfers.forEach { TransferRow(it) }
            }
        }

        SectionTitle(stringResource(R.string.recent))
        if (recent.isEmpty()) {
            Text(stringResource(R.string.nothing_received), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            recent.forEach { HistoryRow(it, actions = false) }
            TextButton(onClick = onShowAll) { Text(stringResource(R.string.all_items)) }
        }

        if (Link.others.isNotEmpty()) {
            SectionTitle(stringResource(R.string.devices_title))
            Panel {
                Text(stringResource(R.string.pc_suffix, Link.pcName), style = MaterialTheme.typography.bodyLarge)
                Link.others.forEach { Text(it.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp)) }
            }
        }
    }
}
