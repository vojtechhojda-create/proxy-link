package dev.proxylink.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
        ScreenHeader("settings")
        Panel {
            NavRow("devices", Icons.Default.Devices, "${Link.known.size}") { onOpen("devices") }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            NavRow("connection", Icons.Default.Wifi) { onOpen("connection") }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            NavRow("appearance", Icons.Default.Palette, if (PaletteStore.dynamic) "system" else PaletteStore.current.label) { onOpen("appearance") }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            NavRow("behavior", Icons.Default.Tune) { onOpen("behavior") }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            NavRow("about", Icons.Default.Info) { onOpen("about") }
        }
    }
}
