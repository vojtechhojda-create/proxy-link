package dev.proxylink.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import dev.proxylink.R
import dev.proxylink.core.Link
import dev.proxylink.ui.components.NavRow
import dev.proxylink.ui.components.Panel
import dev.proxylink.ui.components.ScreenHeader
import dev.proxylink.ui.components.verticalScrollCompat
import dev.proxylink.ui.theme.PaletteStore

// top level: one row per section, each section is its own page
@Composable
fun SettingsScreen(onOpen: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScrollCompat().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(stringResource(R.string.settings))
        Panel {
            NavRow(stringResource(R.string.title_devices), Icons.Default.Devices, "${Link.known.size}") { onOpen("devices") }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            NavRow(stringResource(R.string.title_connection), Icons.Default.Wifi) { onOpen("connection") }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            NavRow(stringResource(R.string.title_appearance), Icons.Default.Palette, if (PaletteStore.dynamic) stringResource(R.string.system) else PaletteStore.current.label) { onOpen("appearance") }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            NavRow(stringResource(R.string.title_clipboard), Icons.Default.ContentPaste) { onOpen("clipboard") }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            NavRow(stringResource(R.string.title_transfers), Icons.Default.SwapVert) { onOpen("transfers") }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            NavRow(stringResource(R.string.title_notifications), Icons.Default.Notifications) { onOpen("notifications") }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            NavRow(stringResource(R.string.title_privacy), Icons.Default.Lock) { onOpen("privacy") }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            NavRow(stringResource(R.string.title_diagnostics), Icons.Default.BugReport) { onOpen("diagnostics") }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            NavRow(stringResource(R.string.title_about), Icons.Default.Info) { onOpen("about") }
        }
    }
}
