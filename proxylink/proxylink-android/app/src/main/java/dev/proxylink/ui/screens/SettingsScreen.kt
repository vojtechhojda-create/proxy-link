package dev.proxylink.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.proxylink.core.Hotspot
import dev.proxylink.core.Link
import dev.proxylink.ui.components.ProxyLogo
import dev.proxylink.ui.theme.Palette
import dev.proxylink.ui.theme.PaletteStore
import dev.proxylink.ui.theme.Palettes

@Composable
fun SettingsScreen(onOpenPro: () -> Unit = {}) {
    var deviceName by remember { mutableStateOf(Link.prefs.name) }
    var autoAccept by remember { mutableStateOf(Link.prefs.autoAccept) }
    val ctx = LocalContext.current
    val perms = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r.values.all { it }) Hotspot.start(ctx) else Hotspot.fail("permission denied")
    }
    var startOnBoot by remember { mutableStateOf(Link.prefs.startOnBoot) }
    var tick by remember { mutableIntStateOf(0) }
    val exempt = tick >= 0 && ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)
    val batt = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { tick++ }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProxyLogo(size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Text("settings", style = MaterialTheme.typography.headlineMedium)
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenPro),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("proxylink pro", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "extra features, the core stays free",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
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
                Text("accept automatically", style = MaterialTheme.typography.titleMedium)
                Text(
                    "from your paired pc. off = ask for every item",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = autoAccept, onCheckedChange = { autoAccept = it; Link.prefs.autoAccept = it })
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("start after reboot", style = MaterialTheme.typography.titleMedium)
                Text(
                    "keeps sharing and the quick tile ready without opening the app",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = startOnBoot, onCheckedChange = { startOnBoot = it; Link.prefs.startOnBoot = it })
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("run in background", style = MaterialTheme.typography.titleMedium)
            Text(
                if (exempt) "battery optimization is off for proxylink"
                else "battery optimization may stop the connection. on xiaomi, samsung and similar phones also allow autostart in the system app settings",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!exempt) {
                Button(onClick = {
                    batt.launch(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}")))
                }) { Text("allow in background") }
            }
            OutlinedButton(onClick = {
                ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
            }) { Text("open app settings") }
        }

        Column {
            Text("secure network", style = MaterialTheme.typography.titleMedium)
            Text(
                "tls with a pinned pc certificate, pairing by qr or code, hmac login. unpaired devices need approval on the pc",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column {
            Text("received files", style = MaterialTheme.typography.titleMedium)
            Text("Download/proxylink", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("no shared wi-fi", style = MaterialTheme.typography.titleMedium)
            Text(
                "start a hotspot on this phone, join your pc to it, choose the hotspot address in the pc window, then scan its qr code. some phones drop their own wi-fi while the hotspot runs",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (Hotspot.ssid == null) {
                Button(
                    onClick = {
                        perms.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES))
                    },
                ) { Text("start hotspot") }
            } else {
                Text("network: ${Hotspot.ssid}")
                Text("password: ${Hotspot.pass}")
                OutlinedButton(onClick = { Hotspot.stop() }) { Text("stop hotspot") }
            }
            Hotspot.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
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
