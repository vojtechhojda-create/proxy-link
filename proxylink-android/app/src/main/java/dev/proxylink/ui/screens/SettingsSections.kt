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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
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
import dev.proxylink.core.Capabilities
import dev.proxylink.core.Feature
import dev.proxylink.core.ImageOptimizer
import kotlinx.coroutines.delay
import dev.proxylink.core.History
import dev.proxylink.core.Hotspot
import dev.proxylink.core.LauncherIcon
import dev.proxylink.core.KnownDevice
import dev.proxylink.core.Link
import dev.proxylink.core.LinkState
import dev.proxylink.ui.components.*
import dev.proxylink.core.AppIdentity
import dev.proxylink.core.IdentityState
import dev.proxylink.ui.theme.AppFont
import dev.proxylink.ui.theme.FontStore
import dev.proxylink.ui.theme.MonoStyle
import dev.proxylink.ui.theme.Palette
import dev.proxylink.ui.theme.PaletteStore
import dev.proxylink.ui.theme.ShapeStore
import dev.proxylink.ui.theme.ShapeStyle
import dev.proxylink.ui.theme.Palettes
import dev.proxylink.ui.theme.pair

@Composable
fun SettingsSection(section: String, onBack: () -> Unit, onScan: () -> Unit, onOpen: (String) -> Unit = {}) {
    when (section) {
        "devices" -> DevicesSection(onBack, onScan)
        "connection" -> ConnectionSection(onBack)
        "appearance" -> AppearanceSection(onBack)
        "clipboard" -> ClipboardSection(onBack)
        "transfers" -> TransfersSection(onBack)
        "notifications" -> NotificationsSection(onBack)
        "privacy" -> PrivacySection(onBack, onOpen)
        "hash" -> HashSection(onBack)
        "diagnostics" -> DiagnosticsSection(onBack)
        else -> AboutSection(onBack, onOpen)
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
                                    (Link.lastError?.let { ": " + linkErrorText(it) } ?: ""),
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
    var confirmReset by remember { mutableStateOf(false) }
    fun syncIcon() = LauncherIcon.apply(ctx, Link.prefs.paletteIcon, Link.prefs.palette, Link.prefs.dynamicColor)

    SubScreen(stringResource(R.string.title_appearance), onBack) {
        SectionTitle(stringResource(R.string.look_colors))
        Palettes.all.forEach { p ->
            PaletteOption(p, selected = !PaletteStore.dynamic && PaletteStore.current.name == p.name) {
                PaletteStore.select(p)
                Link.prefs.palette = p.name
                Link.prefs.dynamicColor = false
                dev.proxylink.Widgets.updateAll(ctx)
                syncIcon()
            }
        }
        // material you: colors from the wallpaper, follows the system light / dark setting
        SelectableCard(selected = PaletteStore.dynamic, onClick = {
            PaletteStore.useSystem()
            Link.prefs.dynamicColor = true
            dev.proxylink.Widgets.updateAll(ctx)
            syncIcon()
        }) {
            Text(stringResource(R.string.system_colors), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.system_colors_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SectionTitle(stringResource(R.string.look_icon))
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
            // a palette without its own launcher icon is not hidden behind a switch that seems to do something
            if (iconByPalette && (PaletteStore.dynamic || !LauncherIcon.hasOwn(PaletteStore.current.name))) {
                Text(
                    stringResource(R.string.icon_fallback),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        FontOptions()
        ShapeOptions()
        // only visual choices: palette, font, corners, icon by palette. devices, history and security stay as they are
        AppOutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.reset_look))
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.reset_look_title)) },
            text = { Text(stringResource(R.string.reset_look_text)) },
            confirmButton = {
                TextButton(onClick = {
                    PaletteStore.select(Palettes.brand); Link.prefs.palette = Palettes.brand.name; Link.prefs.dynamicColor = false
                    FontStore.load("default"); Link.prefs.font = "default"
                    ShapeStore.load("default"); Link.prefs.shape = "default"
                    iconByPalette = false; Link.prefs.paletteIcon = false
                    dev.proxylink.Widgets.updateAll(ctx)
                    syncIcon()
                    confirmReset = false
                }) { Text(stringResource(R.string.reset_look_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

// corner style with a small preview (panel, button, dialog) drawn with that style's own radii. applies at once through the theme
@Composable
private fun ShapeOptions() {
    SectionTitle(stringResource(R.string.shape_title))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ShapeStyle.entries.forEach { st ->
            val selected = ShapeStore.current == st
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.weight(1f).selectable(selected = selected, role = Role.RadioButton) { ShapeStore.select(st); Link.prefs.shape = st.key },
            ) {
                Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // panel, button and dialog drawn with this style's own radii
                    val line = MaterialTheme.colorScheme.outline
                    Box(Modifier.fillMaxWidth().height(22.dp).border(1.dp, line, RoundedCornerShape(st.m.dp)))
                    Box(Modifier.fillMaxWidth(0.7f).height(16.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(st.s.dp)))
                    Box(Modifier.fillMaxWidth().height(22.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(st.xl.dp)).border(1.dp, line, RoundedCornerShape(st.xl.dp)))
                    Text(
                        stringResource(when (st) { ShapeStyle.SQUARE -> R.string.shape_square; ShapeStyle.DEFAULT -> R.string.shape_default; ShapeStyle.ROUND -> R.string.shape_round }),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
    Text(
        stringResource(R.string.shape_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// main ui font with a live preview in each font. the choice applies at once and is saved in Prefs
@Composable
private fun FontOptions() {
    SectionTitle(stringResource(R.string.font_title))
    AppFont.entries.forEach { f ->
        val pair = f.pair()
        SelectableCard(selected = FontStore.current == f, onClick = { FontStore.select(f); Link.prefs.font = f.key }) {
            Text(
                if (f == AppFont.SYSTEM) stringResource(R.string.font_system) else f.label,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = pair.heading),
            )
            Text(
                stringResource(R.string.font_preview),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = pair.body),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
    Text(
        stringResource(R.string.font_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
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
        modifier = Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) { Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), content = content) }
}

@Composable
private fun PaletteOption(p: Palette, selected: Boolean, onClick: () -> Unit) {
    SelectableCard(selected, onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(p.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(p.scheme.background, p.scheme.surfaceContainerHigh, p.scheme.primary, p.scheme.secondary, p.scheme.tertiary).forEach { c ->
                    Box(Modifier.size(width = 20.dp, height = 16.dp).background(c, MaterialTheme.shapes.extraSmall).border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraSmall))
                }
            }
        }
    }
}

@Composable
private fun ClipboardSection(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var history by remember { mutableStateOf(Link.prefs.clipHistory) }
    var enabled by remember { mutableStateOf(Link.clipEnabled) }
    var maxItems by remember { mutableIntStateOf(Link.prefs.clipHistoryMax) }
    var keepDays by remember { mutableIntStateOf(Link.prefs.clipHistoryDays) }
    // the pause can run out while the screen is open, so the status is re-read now and then
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = System.currentTimeMillis() } }
    val paused = Link.clipPausedUntil > now
    fun time(t: Long) = if (t > 0) DateUtils.formatDateTime(ctx, t, DateUtils.FORMAT_SHOW_TIME) else null

    SubScreen(stringResource(R.string.title_clipboard), onBack) {
        Panel {
            SwitchRow(stringResource(R.string.clip_enabled), stringResource(R.string.clip_enabled_desc), enabled) { enabled = it; Link.changeClipEnabled(it) }
        }
        // switched off: only the switch and the deletion of saved items stay, the rest has no effect
        if (enabled) {
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
                AdaptiveButtons {
                    AppOutlinedButton(onClick = { Link.pauseClip(15 * 60_000L) }) { Text(stringResource(R.string.clip_pause_15)) }
                    AppOutlinedButton(onClick = { Link.pauseClip(60 * 60_000L) }) { Text(stringResource(R.string.clip_pause_1h)) }
                    AppOutlinedButton(onClick = { Link.pauseClip(null) }) { Text(stringResource(R.string.clip_pause_manual)) }
                }
            }
        }
        Panel {
            Text(stringResource(R.string.clip_status), style = MaterialTheme.typography.titleMedium)
            // why nothing is synced is told apart: no connection, guest, pause, both directions off
            val reason = when {
                Link.state != LinkState.CONNECTED -> stringResource(R.string.clip_status_not_connected)
                Link.guest -> stringResource(R.string.clip_status_guest)
                !Capabilities.supports(Feature.CLIPBOARD) -> stringResource(R.string.clip_status_peer)
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
        } else {
            Text(stringResource(R.string.clip_status_disabled), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Panel {
            if (enabled) {
                SwitchRow(stringResource(R.string.clip_history), stringResource(R.string.clip_history_desc), history) {
                    history = it; Link.prefs.clipHistory = it
                }
                if (history) {
                    Spacer(Modifier.height(8.dp))
                    LimitChips(
                        stringResource(R.string.clip_history_max),
                        listOf(20 to "20", 50 to "50", 100 to "100", 0 to stringResource(R.string.limit_none)),
                        maxItems,
                    ) { maxItems = it; Link.prefs.clipHistoryMax = it; History.enforceClipLimits() }
                    Spacer(Modifier.height(8.dp))
                    LimitChips(
                        stringResource(R.string.clip_history_days),
                        listOf(
                            1 to stringResource(R.string.keep_1d), 7 to stringResource(R.string.keep_7d),
                            30 to stringResource(R.string.keep_30d), 0 to stringResource(R.string.keep_forever),
                        ),
                        keepDays,
                    ) { keepDays = it; Link.prefs.clipHistoryDays = it; History.enforceClipLimits() }
                    Text(
                        stringResource(R.string.clip_history_limits_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            AppOutlinedButton(onClick = { History.clearClips() }) { Text(stringResource(R.string.clip_history_clear)) }
        }
        if (enabled) Text(
            stringResource(R.string.clipboard_limit) + " " + stringResource(R.string.clip_sensitive_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// one choice out of a few numbers, as chips that scroll sideways at a large text size
@Composable
private fun LimitChips(label: String, options: List<Pair<Int, String>>, value: Int, onSelect: (Int) -> Unit) {
    Text(label, style = MaterialTheme.typography.bodyLarge)
    Row(Modifier.horizontalScrollCompat(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (v, text) -> FilterChip(selected = value == v, onClick = { onSelect(v) }, label = { Text(text) }) }
    }
}

@Composable
private fun TransfersSection(onBack: () -> Unit) {
    var accept by remember { mutableStateOf(Link.prefs.autoAccept) }
    var quality by remember { mutableStateOf(Link.prefs.imageQuality) }
    var acceptMax by remember { mutableIntStateOf(Link.prefs.autoAcceptMaxMb) }
    var openRecv by remember { mutableStateOf(Link.prefs.openAfterReceive) }
    var clearing by remember { mutableStateOf(false) }
    SubScreen(stringResource(R.string.title_transfers), onBack) {
        Panel {
            SwitchRow(stringResource(R.string.accept_automatically), stringResource(R.string.accept_automatically_desc), accept) { accept = it; Link.prefs.autoAccept = it }
            if (accept) {
                Spacer(Modifier.height(8.dp))
                LimitChips(
                    stringResource(R.string.accept_max),
                    listOf(10 to stringResource(R.string.mb_value, 10), 100 to stringResource(R.string.mb_value, 100),
                        1000 to stringResource(R.string.mb_value, 1000), 0 to stringResource(R.string.limit_none)),
                    acceptMax,
                ) { acceptMax = it; Link.prefs.autoAcceptMaxMb = it }
                Text(
                    stringResource(R.string.accept_max_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
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
                        .selectable(selected = quality == q.key, role = Role.RadioButton) { quality = q.key; Link.prefs.imageQuality = q.key }
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
            Text(
                stringResource(R.string.name_conflict_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
            SwitchRow(stringResource(R.string.open_after_receive), stringResource(R.string.open_after_receive_desc), openRecv) {
                openRecv = it; Link.prefs.openAfterReceive = it
            }
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
    var hideContent by remember { mutableStateOf(Link.prefs.notifHideContent) }
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
            SwitchRow(stringResource(R.string.notif_hide), stringResource(R.string.notif_hide_desc), hideContent) { hideContent = it; Link.prefs.notifHideContent = it }
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
private fun PrivacySection(onBack: () -> Unit, onOpen: (String) -> Unit) {
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
        Panel { NavRow(stringResource(R.string.title_hash), Icons.Default.Fingerprint) { onOpen("hash") } }
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
        "clipboard feature: ${Link.clipEnabled}, send: ${Link.clipSend}, receive: ${Link.clipReceive}, paused: ${Link.clipPaused()}",
        "clipboard history: ${Link.prefs.clipHistory}, max items: ${Link.prefs.clipHistoryMax}, days: ${Link.prefs.clipHistoryDays}",
        "auto accept: ${Link.prefs.autoAccept}, ask above mb: ${Link.prefs.autoAcceptMaxMb}",
        "pc protocol: ${if (Capabilities.protocol > 0) Capabilities.protocol.toString() else "n/a"}, clipboard supported: ${Capabilities.supports(Feature.CLIPBOARD)}",
        "auto connect: ${Link.prefs.autoConnect}",
        "app signature check: ${AppIdentity.state(ctx)}",
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
private fun AboutSection(onBack: () -> Unit, onOpen: (String) -> Unit) {
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
        SectionTitle(stringResource(R.string.sec_title))
        AppSignaturePanel()
        // general description first, then the state of the connection right now. every statement is backed by the code (see PROJECT_STATUS.md)
        Panel {
            Text(
                when {
                    Link.state != LinkState.CONNECTED -> stringResource(R.string.sec_now_none)
                    Link.guest -> stringResource(R.string.sec_now_guest, Link.pcName)
                    else -> stringResource(R.string.sec_now_connected, Link.pcName)
                },
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Panel {
            listOf(
                R.string.sec_how_title to R.string.sec_how, R.string.sec_enc_title to R.string.sec_enc,
                R.string.sec_ver_title to R.string.sec_ver, R.string.sec_local_title to R.string.sec_local,
                R.string.sec_keep_title to R.string.sec_keep, R.string.sec_perm_title to R.string.sec_perm,
                R.string.sec_cloud_title to R.string.sec_cloud,
            ).forEachIndexed { i, (title, body) ->
                if (i > 0) Spacer(Modifier.height(12.dp))
                Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        // management stays where it already is, nothing is duplicated here
        Panel {
            NavRow(stringResource(R.string.title_devices), Icons.Default.Devices) { onOpen("devices") }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            NavRow(stringResource(R.string.title_clipboard), Icons.Default.ContentPaste) { onOpen("clipboard") }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            NavRow(stringResource(R.string.title_transfers), Icons.Default.SwapVert) { onOpen("transfers") }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            NavRow(stringResource(R.string.title_privacy), Icons.Default.Lock) { onOpen("privacy") }
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

// signature of this installed app: the real state of the local check, the fingerprint it found and what the check cannot do.
// it is not the fingerprint of a file, and not the identity of the other device
@Composable
private fun AppSignaturePanel() {
    val ctx = LocalContext.current
    val st = remember { AppIdentity.state(ctx) }
    val fps = remember { AppIdentity.signerFingerprints(ctx) }
    var copied by remember { mutableStateOf(false) }
    Panel {
        Text(stringResource(R.string.id_title), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(
                when (st) {
                    IdentityState.DEBUG -> R.string.id_debug
                    IdentityState.NOT_CONFIGURED -> R.string.id_not_configured
                    IdentityState.UNKNOWN -> R.string.id_unknown
                    IdentityState.MATCH -> R.string.id_match
                    IdentityState.MISMATCH -> R.string.id_mismatch
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = if (st == IdentityState.MISMATCH) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        fps.forEach { Text(it, style = MonoStyle, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (fps.isNotEmpty()) {
            TextButton(onClick = {
                ctx.getSystemService(android.content.ClipboardManager::class.java)
                    .setPrimaryClip(android.content.ClipData.newPlainText("sha-256", fps.joinToString("\n")))
                copied = true
            }) { Text(stringResource(if (copied) R.string.copied else R.string.hash_copy)) }
        }
        Text(stringResource(R.string.id_limits), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// the connection error is kept as a plain code in Link (diagnostics show it as it is); the screen translates it.
// the certificate case is an identity mismatch: the connection is refused and nothing is sent, it is never skipped by a dialog
@Composable
private fun linkErrorText(e: String): String = stringResource(
    when (e) {
        "certificate does not match the paired device" -> R.string.err_cert
        "timeout" -> R.string.err_timeout
        "device not reachable" -> R.string.err_unreachable
        else -> R.string.err_lost
    },
)
