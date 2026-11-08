package dev.proxylink.ui.screens

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.proxylink.core.History
import dev.proxylink.core.Hotspot
import dev.proxylink.core.KnownDevice
import dev.proxylink.core.Link
import dev.proxylink.core.LinkState
import dev.proxylink.ui.components.*
import dev.proxylink.ui.theme.MonoStyle
import dev.proxylink.ui.theme.Palette
import dev.proxylink.ui.theme.PaletteStore
import dev.proxylink.ui.theme.Palettes

@Composable
fun SettingsSection(section: String, onBack: () -> Unit, onScan: () -> Unit) {
    when (section) {
        "devices" -> DevicesSection(onBack, onScan)
        "connection" -> ConnectionSection(onBack)
        "appearance" -> AppearanceSection(onBack)
        "behavior" -> BehaviorSection(onBack)
        else -> AboutSection(onBack)
    }
}

@Composable
private fun DevicesSection(onBack: () -> Unit, onScan: () -> Unit) {
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    var forgetting by remember { mutableStateOf<KnownDevice?>(null) }
    val ctx = LocalContext.current

    SubScreen("devices", onBack) {
        if (Link.known.isEmpty()) {
            Text("no paired devices", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Link.known.forEach { d ->
            val expanded = open == d.id
            Panel {
                Column(Modifier.fillMaxWidth().clickable(role = Role.Button) { open = if (expanded) null else d.id }) {
                    Text(d.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "last connected " + DateUtils.formatDateTime(
                            ctx, d.lastConnected, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AnimatedVisibility(visible = expanded) {
                    Column(Modifier.padding(top = 12.dp)) {
                        Text(d.id, style = MonoStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (Link.state != LinkState.CONNECTED) {
                                AppButton(onClick = { Link.reconnect(d.id) }) { Text("reconnect") }
                            }
                            AppOutlinedButton(onClick = { forgetting = d }) { Text("forget") }
                        }
                    }
                }
            }
        }
        AppOutlinedButton(onClick = onScan, modifier = Modifier.fillMaxWidth()) { Text("pair a new device") }
    }

    forgetting?.let { d ->
        AlertDialog(
            onDismissRequest = { forgetting = null },
            title = { Text("forget ${d.name}?") },
            text = { Text("you will need to pair it again") },
            confirmButton = { TextButton(onClick = { Link.forget(d.id); forgetting = null; open = null }) { Text("forget") } },
            dismissButton = { TextButton(onClick = { forgetting = null }) { Text("cancel") } },
        )
    }
}

@Composable
private fun ConnectionSection(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf(Link.prefs.name) }
    var autoConnect by remember { mutableStateOf(Link.prefs.autoConnect) }
    val perms = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r.values.all { it }) Hotspot.start(ctx) else Hotspot.fail("permission denied")
    }

    SubScreen("connection", onBack) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; Link.prefs.name = it },
            label = { Text("device name") },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
        )
        Panel {
            SwitchRow(
                "connect automatically", "paired devices in range",
                autoConnect,
            ) { autoConnect = it; Link.prefs.autoConnect = it }
        }
        Panel {
            Text("no shared wi-fi", style = MaterialTheme.typography.titleMedium)
            Text(
                "start a hotspot on this phone, join the pc to it and scan the pc qr code",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            if (Hotspot.ssid == null) {
                AppButton(onClick = {
                    perms.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES))
                }) { Text("start hotspot") }
            } else {
                Text("network", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Hotspot.ssid ?: "", style = MonoStyle)
                Text("password", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                Text(Hotspot.pass ?: "", style = MonoStyle)
                Spacer(Modifier.height(8.dp))
                AppOutlinedButton(onClick = { Hotspot.stop() }) { Text("stop hotspot") }
            }
            Hotspot.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun AppearanceSection(onBack: () -> Unit) {
    SubScreen("appearance", onBack) {
        Palettes.all.forEach { p ->
            PaletteOption(p, selected = !PaletteStore.dynamic && PaletteStore.current.name == p.name) {
                PaletteStore.select(p)
                Link.prefs.palette = p.name
                Link.prefs.dynamicColor = false
            }
        }
        // material you: colors from the wallpaper, follows the system light / dark setting
        SelectableCard(selected = PaletteStore.dynamic, onClick = {
            PaletteStore.useSystem()
            Link.prefs.dynamicColor = true
        }) {
            Text("system colors", style = MaterialTheme.typography.titleMedium)
            Text(
                "material you, follows light and dark mode",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SelectableCard(selected: Boolean, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.fillMaxWidth().clickable(role = Role.RadioButton, onClick = onClick),
    ) { Column(Modifier.padding(16.dp), content = content) }
}

@Composable
private fun PaletteOption(p: Palette, selected: Boolean, onClick: () -> Unit) {
    SelectableCard(selected, onClick) {
        Text(p.label, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(p.scheme.background, p.scheme.surfaceContainerHigh, p.scheme.primary, p.scheme.secondary, p.scheme.tertiary).forEach { c ->
                Box(Modifier.size(width = 28.dp, height = 18.dp).background(c, MaterialTheme.shapes.extraSmall))
            }
        }
    }
}

@Composable
private fun BehaviorSection(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var boot by remember { mutableStateOf(Link.prefs.startOnBoot) }
    var accept by remember { mutableStateOf(Link.prefs.autoAccept) }
    var haptics by remember { mutableStateOf(Link.prefs.haptics) }
    var clearing by remember { mutableStateOf(false) }

    // these two can change in the system settings, so they are read again when the app resumes
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val exempt = tick >= 0 && ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)
    val notificationsOn = tick >= 0 && ctx.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
    val batt = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { tick++ }

    SubScreen("behavior", onBack) {
        Panel {
            SwitchRow("start after reboot", null, boot) { boot = it; Link.prefs.startOnBoot = it }
            SwitchRow("sync clipboard", "while the app is open", Link.autoSync) { Link.changeAutoSync(it) }
            SwitchRow("accept automatically", "from your paired pc", accept) { accept = it; Link.prefs.autoAccept = it }
            SwitchRow("haptic feedback", null, haptics) { haptics = it; Link.prefs.haptics = it }
        }
        Panel {
            Text("background", style = MaterialTheme.typography.titleMedium)
            Text(
                if (exempt) "battery optimization is off" else "the system may stop the connection to save battery",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!exempt) {
                    AppButton(onClick = {
                        batt.launch(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}")))
                    }) { Text("allow") }
                }
                AppOutlinedButton(onClick = {
                    ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
                }) { Text("app settings") }
            }
        }
        Panel {
            Text("notifications", style = MaterialTheme.typography.titleMedium)
            Text(
                if (notificationsOn) "on" else "off",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            AppOutlinedButton(onClick = {
                ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
            }) { Text("notification settings") }
        }
        AppOutlinedButton(onClick = { clearing = true }, modifier = Modifier.fillMaxWidth()) { Text("clear history") }
    }

    if (clearing) {
        AlertDialog(
            onDismissRequest = { clearing = false },
            title = { Text("clear history?") },
            text = { Text("received files stay in Download/proxylink") },
            confirmButton = { TextButton(onClick = { History.clear(); clearing = false }) { Text("clear") } },
            dismissButton = { TextButton(onClick = { clearing = false }) { Text("cancel") } },
        )
    }
}

@Composable
private fun AboutSection(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val version = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "" }
    SubScreen("about", onBack) {
        Panel {
            Text("proxylink", style = MaterialTheme.typography.titleMedium)
            Text(version, style = MonoStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Panel {
            Text("everything stays on your devices", style = MaterialTheme.typography.bodyLarge)
            Text(
                "no cloud, no ads, no telemetry, no account",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Panel {
            Text("fonts", style = MaterialTheme.typography.titleMedium)
            Text(
                "Inter, Space Grotesk, Inconsolata (SIL Open Font License)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
