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
import androidx.compose.ui.res.stringResource
import dev.proxylink.LinkService
import dev.proxylink.R
import dev.proxylink.core.ImageOptimizer
import kotlinx.coroutines.delay
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

    SubScreen(stringResource(R.string.title_devices), onBack) {
        if (Link.known.isEmpty()) {
            Text(stringResource(R.string.no_paired_devices), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Link.known.forEach { d ->
            val expanded = open == d.id
            Panel {
                Column(Modifier.fillMaxWidth().clickable(role = Role.Button) { open = if (expanded) null else d.id }) {
                    Text(d.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(
                            R.string.last_connected,
                            DateUtils.formatDateTime(
                                ctx, d.lastConnected, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL,
                            ),
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
                                stringResource(R.string.last_attempt, DateUtils.formatDateTime(ctx, Link.lastAttempt, DateUtils.FORMAT_SHOW_TIME)) +
                                    (Link.lastError?.let { ": $it" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        SwitchRow(stringResource(R.string.preferred_device), stringResource(R.string.preferred_device_desc), Link.preferred == d.id) {
                            Link.setPreferredDevice(if (it) d.id else null)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (Link.state != LinkState.CONNECTED) {
                                AppButton(onClick = { Link.reconnect(d.id) }) { Text(stringResource(if (Link.lastError != null) R.string.try_again else R.string.reconnect)) }
                            }
                            AppOutlinedButton(onClick = { forgetting = d }) { Text(stringResource(R.string.forget)) }
                        }
                    }
                }
            }
        }
        AppOutlinedButton(onClick = onScan, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.pair_new_device)) }
    }

    forgetting?.let { d ->
        AlertDialog(
            onDismissRequest = { forgetting = null },
            title = { Text(stringResource(R.string.forget_device_title, d.name)) },
            text = { Text(stringResource(R.string.forget_device_text)) },
            confirmButton = { TextButton(onClick = { Link.forget(d.id); forgetting = null; open = null }) { Text(stringResource(R.string.forget)) } },
            dismissButton = { TextButton(onClick = { forgetting = null }) { Text(stringResource(R.string.cancel)) } },
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

    SubScreen(stringResource(R.string.title_connection), onBack) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; Link.prefs.name = it },
            label = { Text(stringResource(R.string.device_name)) },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
        )
        Panel {
            SwitchRow(
                stringResource(R.string.connect_automatically), stringResource(R.string.connect_automatically_desc),
                autoConnect,
            ) { autoConnect = it; Link.prefs.autoConnect = it }
        }
        Panel {
            Text(stringResource(R.string.no_shared_wifi), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.hotspot_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            if (Hotspot.ssid == null) {
                AppButton(onClick = {
                    perms.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES))
                }) { Text(stringResource(R.string.start_hotspot)) }
            } else {
                Text(stringResource(R.string.network), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Hotspot.ssid ?: "", style = MonoStyle)
                Text(stringResource(R.string.password), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                Text(Hotspot.pass ?: "", style = MonoStyle)
                Spacer(Modifier.height(8.dp))
                AppOutlinedButton(onClick = { Hotspot.stop() }) { Text(stringResource(R.string.stop_hotspot)) }
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

    SubScreen(stringResource(R.string.title_appearance), onBack) {
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
            Text(stringResource(R.string.system_colors), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.system_colors_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Panel {
            SwitchRow(
                stringResource(R.string.icon_by_palette),
                stringResource(R.string.icon_by_palette_desc),
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
    val ctx = LocalContext.current
    var history by remember { mutableStateOf(Link.prefs.clipHistory) }
    // the pause can run out while the screen is open, so the status is re-read now and then
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = System.currentTimeMillis() } }
    val paused = Link.clipPausedUntil > now
    fun time(t: Long) = if (t > 0) DateUtils.formatDateTime(ctx, t, DateUtils.FORMAT_SHOW_TIME) else null

    SubScreen(stringResource(R.string.title_clipboard), onBack) {
        Panel {
            SwitchRow(stringResource(R.string.clip_send), stringResource(R.string.clip_send_desc), Link.clipSend) { Link.changeClipSend(it) }
            SwitchRow(stringResource(R.string.clip_receive), stringResource(R.string.clip_receive_desc), Link.clipReceive) { Link.changeClipReceive(it) }
        }
        Panel {
            Text(stringResource(R.string.clip_pause), style = MaterialTheme.typography.titleMedium)
            if (paused) {
                Text(
                    if (Link.clipPausedUntil == Long.MAX_VALUE) stringResource(R.string.clip_paused_manual)
                    else stringResource(R.string.clip_paused_until, time(Link.clipPausedUntil) ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                AppButton(onClick = { Link.resumeClip() }) { Text(stringResource(R.string.clip_resume)) }
            } else {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppOutlinedButton(onClick = { Link.pauseClip(15 * 60_000L) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.clip_pause_15)) }
                    AppOutlinedButton(onClick = { Link.pauseClip(60 * 60_000L) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.clip_pause_1h)) }
                    AppOutlinedButton(onClick = { Link.pauseClip(null) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.clip_pause_manual)) }
                }
            }
        }
        Panel {
            Text(stringResource(R.string.clip_status), style = MaterialTheme.typography.titleMedium)
            // why nothing is synced is told apart: no connection, guest, pause, both directions off
            val reason = when {
                Link.state != LinkState.CONNECTED -> stringResource(R.string.clip_status_not_connected)
                Link.guest -> stringResource(R.string.clip_status_guest)
                paused -> stringResource(R.string.clip_status_paused)
                !Link.clipSend && !Link.clipReceive -> stringResource(R.string.clip_status_off)
                else -> stringResource(R.string.clip_status_active)
            }
            Text(reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                stringResource(R.string.clip_last_sent, time(Link.clipLastSent) ?: stringResource(R.string.clip_never)),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.clip_last_received, time(Link.clipLastReceived) ?: stringResource(R.string.clip_never)),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Panel {
            SwitchRow(stringResource(R.string.clip_history), stringResource(R.string.clip_history_desc), history) {
                history = it; Link.prefs.clipHistory = it
            }
            Spacer(Modifier.height(8.dp))
            AppOutlinedButton(onClick = { History.clearClips() }) { Text(stringResource(R.string.clip_history_clear)) }
        }
        Text(
            stringResource(R.string.clipboard_limit) + " " + stringResource(R.string.clip_sensitive_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TransfersSection(onBack: () -> Unit) {
    var accept by remember { mutableStateOf(Link.prefs.autoAccept) }
    var quality by remember { mutableStateOf(Link.prefs.imageQuality) }
    var clearing by remember { mutableStateOf(false) }
    SubScreen(stringResource(R.string.title_transfers), onBack) {
        Panel {
            SwitchRow(stringResource(R.string.accept_automatically), stringResource(R.string.accept_automatically_desc), accept) { accept = it; Link.prefs.autoAccept = it }
        }
        Panel {
            Text(stringResource(R.string.image_quality), style = MaterialTheme.typography.titleMedium)
            listOf(
                Triple(ImageOptimizer.Quality.ORIGINAL, R.string.image_original, R.string.image_original_desc),
                Triple(ImageOptimizer.Quality.HIGH, R.string.image_high, R.string.image_high_desc),
                Triple(ImageOptimizer.Quality.ECONOMY, R.string.image_economy, R.string.image_economy_desc),
            ).forEach { (q, title, desc) ->
                Row(
                    Modifier.fillMaxWidth()
                        .clickable(role = Role.RadioButton) { quality = q.key; Link.prefs.imageQuality = q.key }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = quality == q.key, onClick = null)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text(stringResource(R.string.image_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Panel {
            Text(stringResource(R.string.received_files), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.saved_in),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AppOutlinedButton(onClick = { clearing = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.clear_history)) }
    }

    if (clearing) {
        AlertDialog(
            onDismissRequest = { clearing = false },
            title = { Text(stringResource(R.string.clear_history_title)) },
            text = { Text(stringResource(R.string.clear_history_text)) },
            confirmButton = { TextButton(onClick = { History.clear(); clearing = false }) { Text(stringResource(R.string.clear)) } },
            dismissButton = { TextButton(onClick = { clearing = false }) { Text(stringResource(R.string.cancel)) } },
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

    SubScreen(stringResource(R.string.title_notifications), onBack) {
        Panel {
            SwitchRow(stringResource(R.string.start_after_reboot), null, boot) { boot = it; Link.prefs.startOnBoot = it }
            SwitchRow(stringResource(R.string.haptic_feedback), null, haptics) { haptics = it; Link.prefs.haptics = it }
        }
        Panel {
            Text(stringResource(R.string.notifications), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(if (notificationsOn) R.string.state_on else R.string.off_in_system),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            AppOutlinedButton(onClick = {
                ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
            }) { Text(stringResource(R.string.notification_settings)) }
        }
        Panel {
            Text(stringResource(R.string.background), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(if (exempt) R.string.battery_opt_off else R.string.battery_opt_on),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!exempt) {
                    AppButton(onClick = {
                        batt.launch(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}")))
                    }) { Text(stringResource(R.string.allow)) }
                }
                AppOutlinedButton(onClick = {
                    ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
                }) { Text(stringResource(R.string.app_settings)) }
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

    SubScreen(stringResource(R.string.title_privacy), onBack) {
        Panel {
            Text(stringResource(R.string.connections), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.connections_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Panel {
            Text(stringResource(R.string.permissions), style = MaterialTheme.typography.titleMedium)
            listOf(
                stringResource(R.string.notifications) to notif,
                stringResource(R.string.perm_camera) to granted(Manifest.permission.CAMERA),
                stringResource(R.string.perm_nearby) to granted(Manifest.permission.NEARBY_WIFI_DEVICES),
                stringResource(R.string.perm_location) to granted(Manifest.permission.ACCESS_FINE_LOCATION),
                stringResource(R.string.perm_battery) to batt,
            ).forEach { (n, ok) ->
                Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Text(n, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(
                        stringResource(if (ok) R.string.granted else R.string.not_granted),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            AppOutlinedButton(onClick = {
                ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
            }) { Text(stringResource(R.string.app_settings)) }
        }
        if (Link.known.isNotEmpty()) {
            AppOutlinedButton(onClick = { forgetAll = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.forget_all)) }
        }
    }

    if (forgetAll) {
        AlertDialog(
            onDismissRequest = { forgetAll = false },
            title = { Text(stringResource(R.string.forget_all_title)) },
            text = { Text(stringResource(R.string.forget_all_text)) },
            confirmButton = {
                TextButton(onClick = { Link.known.toList().forEach { Link.forget(it.id) }; forgetAll = false }) { Text(stringResource(R.string.forget)) }
            },
            dismissButton = { TextButton(onClick = { forgetAll = false }) { Text(stringResource(R.string.cancel)) } },
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
        "clipboard send: ${Link.clipSend}, receive: ${Link.clipReceive}, paused: ${Link.clipPaused()}",
        "auto connect: ${Link.prefs.autoConnect}",
    ).joinToString("\n")

    SubScreen(stringResource(R.string.title_diagnostics), onBack) {
        Panel { Text(summary, style = MonoStyle) }
        Text(
            stringResource(R.string.diag_note),
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
        }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(if (copied) R.string.copied else R.string.copy_summary)) }
    }
}

@Composable
private fun AboutSection(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val version = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "" }
    SubScreen(stringResource(R.string.title_about), onBack) {
        Panel {
            Text("proxylink", style = MaterialTheme.typography.titleMedium)
            Text(version, style = MonoStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.by_author), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Panel {
            Text(stringResource(R.string.stays_on_devices), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.no_cloud),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Panel {
            Text(stringResource(R.string.fonts), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.fonts_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
