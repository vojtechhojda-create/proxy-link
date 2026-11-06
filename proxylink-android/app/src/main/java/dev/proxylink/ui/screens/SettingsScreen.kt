package dev.proxylink.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.proxylink.core.Link
import dev.proxylink.ui.components.ProxyLogo
import dev.proxylink.ui.theme.Palette
import dev.proxylink.ui.theme.PaletteStore
import dev.proxylink.ui.theme.Palettes

@Composable
fun SettingsScreen() {
    var deviceName by remember { mutableStateOf(Link.prefs.name) }
    var secureMode by rememberSaveable { mutableStateOf(true) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProxyLogo(size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Text("settings", style = MaterialTheme.typography.headlineMedium)
        }

        OutlinedTextField(
            value = deviceName,
            onValueChange = { deviceName = it; Link.prefs.name = it },
            label = { Text("device name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("secure network", style = MaterialTheme.typography.titleMedium)
                Text(
                    "accept only paired devices",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = secureMode, onCheckedChange = { secureMode = it })
        }

        Text("color palette", style = MaterialTheme.typography.titleMedium)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Palettes.all.forEach { p ->
                PaletteCard(
                    palette = p,
                    selected = PaletteStore.current.name == p.name,
                    onClick = { PaletteStore.select(p); Link.prefs.palette = p.name },
                )
            }
        }
    }
}

@Composable
private fun PaletteCard(palette: Palette, selected: Boolean, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        border = if (selected) BorderStroke(2.dp, palette.bright) else null,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                palette.name,
                color = palette.text,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            listOf(palette.abyss, palette.primary, palette.accentBright).forEach { c ->
                Box(
                    Modifier
                        .padding(start = 6.dp)
                        .size(width = 22.dp, height = 14.dp)
                        .background(c, RoundedCornerShape(4.dp))
                )
            }
        }
    }
}
