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
import dev.proxylink.LinkService
import dev.proxylink.core.History
import dev.proxylink.core.Hotspot
import dev.proxylink.core.LauncherIcon
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
        "clipboard" -> ClipboardSection(onBack)
        "transfers" -> TransfersSection(onBack)
        "notifications" -> NotificationsSection(onBack)
        "privacy" -> PrivacySection(onBack)
        "diagnostics" -> DiagnosticsSection(onBack)
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
                        if (Link.lastAttempt > 0 && Link.lastAttemptId == d.id) {
                            Text(
                                "last attempt " + DateUtils.formatDateTime(ctx, Link.lastAttempt, DateUtils.FORMAT_SHOW_TIME) +
                                    (Link.lastError?.let { ": $it" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        SwitchRow("preferred device", "auto connect and the tile use this one", Link.preferred == d.id) {
                            Link.setPreferredDevice(if (it) d.id else null)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (Link.state != LinkState.CONNECTED) {
                                AppButton(onClick = { Link.reconnect(d.id) }) { Text(if (Link.lastError != null) "try again" else "reconnect") }
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
    val ctx = LocalContext.current
    var iconByPalette by remember { mutableStateOf(Link.prefs.paletteIcon) }
    fun syncIcon() = LauncherIcon.apply(ctx, Link.prefs.paletteIcon, Link.prefs.palette, Link.prefs.dynamicColor)

    SubScreen("appearance", onBack) {
        Palettes.all.forEach { p ->
            PaletteOption(p, selected = !PaletteStore.dynamic && PaletteStore.current.name == p.name) {
                PaletteStore.select(p)
                Link.prefs.palette = p.name
                Link.prefs.dynamicColor = false
                syncIcon()
            }
        }
        // material you: colors from the wallpaper, follows the system light / dark setting
        SelectableCard(selected = PaletteStore.dynamic, onClick = {
            PaletteStore.useSystem()
            Link.prefs.dynamicColor = true
            syncIcon()
        }) {
            Text("system colors", style = MaterialTheme.typography.titleMedium)
            Text(
                "material you, follows light and dark mode",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Panel {
            SwitchRow(
                "icon by palette",
                "colors the home screen icon with the selected palette. system colors keep the original icon. " +
                    "the system themed icon (android settings) is separate and stays one color",
                iconByPalette,
            ) {
                iconByPalette = it
                Link.prefs.paletteIcon = it
                syncIcon()
            }
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
private fun ClipboardSection(onBack: () -> Unit) {
    SubScreen("clipboard", onBack) {
        Panel {
            SwitchRow("sync clipboard", "while the app is open", Link.autoSync) { Link.changeAutoSync(it) }
        }
        Text(
            "android 13 lets an app read the clipboard only while it is in the foreground, so background sync is not possible",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TransfersSection(onBack: () -> Unit) {
    var accept by remember { mutableStateOf(Link.prefs.autoAccept) }
    var clearing by remember { mutableStateOf(false) }
    SubScreen("transfers", onBack) {
        Panel {
            SwitchRow("accept automatically", "from your paired pc", accept) { accept = it; Link.prefs.autoAccept = it }
        }
        Panel {
            Text("received files", style = MaterialTheme.typography.titleMedium)
            Text(
                "saved in Download/proxylink",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

// these states can change in the system settings, so they are read again when the app resumes
@Composable
private fun rememberSystemTick(): Int {
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    return tick
}

@Composable
private fun NotificationsSection(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var boot by remember { mutableStateOf(Link.prefs.startOnBoot) }
    var haptics by remember { mutableStateOf(Link.prefs.haptics) }
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val exempt = tick >= 0 && ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)
    val notificationsOn = tick >= 0 && ctx.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
    val batt = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { tick++ }

    SubScreen("notifications and control", onBack) {
        Panel {
            SwitchRow("start after reboot", null, boot) { boot = it; Link.prefs.startOnBoot = it }
            SwitchRow("haptic feedback", null, haptics) { haptics = it; Link.prefs.haptics = it }
        }
        Panel {
            Text("notifications", style = MaterialTheme.typography.titleMedium)
            Text(
                if (notificationsOn) "on" else "off in the system settings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            AppOutlinedButton(onClick = {
                ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
            }) { Text("notification settings") }
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
    }
}

@Composable
private fun PrivacySection(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var forgetAll by remember { mutableStateOf(false) }
    val tick = rememberSystemTick()
    fun granted(p: String) = tick >= 0 && ctx.checkSelfPermission(p) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val notif = tick >= 0 && ctx.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
    val batt = tick >= 0 && ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)

    SubScreen("privacy and security", onBack) {
        Panel {
            Text("connections", style = MaterialTheme.typography.titleMedium)
            Text(
                "connections to the pc use tls and a pinned certificate. nothing is sent to the cloud",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Panel {
            Text("permissions", style = MaterialTheme.typography.titleMedium)
            listOf(
                "notifications" to notif,
                "camera (qr scanner)" to granted(Manifest.permission.CAMERA),
                "nearby wi-fi (hotspot only)" to granted(Manifest.permission.NEARBY_WIFI_DEVICES),
                "location (hotspot only)" to granted(Manifest.permission.ACCESS_FINE_LOCATION),
                "ignore battery optimization" to batt,
            ).forEach { (n, ok) ->
                Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Text(n, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(
                        if (ok) "granted" else "not granted",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            AppOutlinedButton(onClick = {
                ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
            }) { Text("app settings") }
        }
        if (Link.known.isNotEmpty()) {
            AppOutlinedButton(onClick = { forgetAll = true }, modifier = Modifier.fillMaxWidth()) { Text("forget all devices") }
        }
    }

    if (forgetAll) {
        AlertDialog(
            onDismissRequest = { forgetAll = false },
            title = { Text("forget all devices?") },
            text = { Text("you will need to pair every device again") },
            confirmButton = {
                TextButton(onClick = { Link.known.toList().forEach { Link.forget(it.id) }; forgetAll = false }) { Text("forget") }
            },
            dismissButton = { TextButton(onClick = { forgetAll = false }) { Text("cancel") } },
        )
    }
}

@Composable
private fun DiagnosticsSection(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val tick = rememberSystemTick()
    val version = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "" }
    val notif = tick >= 0 && ctx.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
    val batt = tick >= 0 && ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)
    var copied by remember { mutableStateOf(false) }

    // no names, ids, addresses, passwords, tokens or clipboard content
    val summary = listOf(
        "proxylink $version",
        "android ${android.os.Build.VERSION.RELEASE} (sdk ${android.os.Build.VERSION.SDK_INT})",
        "link state: ${Link.state}",
        "searching for a paired device: ${Link.reconnecting != null}",
        "last connect attempt: ${if (Link.lastAttempt > 0) DateUtils.formatDateTime(ctx, Link.lastAttempt, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL) else "none"}",
        "last connect error: ${Link.lastError ?: "none"}",
        "last successful connect: ${Link.known.maxOfOrNull { it.lastConnected }?.takeIf { it > 0 }?.let { DateUtils.formatDateTime(ctx, it, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL) } ?: "none"}",
        "failed attempts in a row: ${Link.retries}",
        "background service running: ${LinkService.running}",
        "disconnected by the user: ${Link.prefs.userDisconnected}",
        "latency: ${if (Link.latency >= 0) "${Link.latency} ms" else "n/a"}",
        "paired devices: ${Link.known.size}",
        "notifications enabled: $notif",
        "battery optimization ignored: $batt",
        "clipboard sync: ${Link.autoSync}",
        "auto connect: ${Link.prefs.autoConnect}",
    ).joinToString("\n")

    SubScreen("diagnostics", onBack) {
        Panel { Text(summary, style = MonoStyle) }
        Text(
            "contains no device names, addresses, passwords, tokens or clipboard content",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AppOutlinedButton(onClick = {
            val clip = android.content.ClipData.newPlainText("proxylink diagnostics", summary)
            // marked sensitive so android 13 does not show a preview of it
            clip.description.extras = android.os.PersistableBundle().apply {
                putBoolean(android.content.ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
            ctx.getSystemService(android.content.ClipboardManager::class.java).setPrimaryClip(clip)
            copied = true
        }, modifier = Modifier.fillMaxWidth()) { Text(if (copied) "copied" else "copy summary") }
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
            Text("by Vojtěch Hojda", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
