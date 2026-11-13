package dev.proxylink.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.proxylink.R
import dev.proxylink.Widgets
import dev.proxylink.core.LauncherIcon
import dev.proxylink.core.Link
import dev.proxylink.ui.components.*
import dev.proxylink.ui.theme.*

// settings > appearance: a short hub with three subpages (theme, font, interface look) and the reset of the visual preferences.
// everything here changes only the look; devices, history and security settings are never touched

private fun syncIcon(ctx: android.content.Context) =
    LauncherIcon.apply(ctx, Link.prefs.paletteIcon, Link.prefs.palette, Link.prefs.dynamicColor)

@Composable
fun AppearanceHub(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val ctx = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }
    SubScreen(stringResource(R.string.title_appearance), onBack) {
        Panel {
            NavRow(
                stringResource(R.string.look_theme), Icons.Default.Palette,
                if (PaletteStore.dynamic) stringResource(R.string.system) else PaletteStore.current.label,
            ) { onOpen("appearance_theme") }
            NavRow(
                stringResource(R.string.look_font), Icons.Default.TextFields,
                if (FontStore.current == AppFont.SYSTEM) stringResource(R.string.font_system) else FontStore.current.label,
            ) { onOpen("appearance_font") }
            NavRow(
                stringResource(R.string.look_ui), Icons.Default.Tune,
                stringResource(
                    when (ShapeStore.current) {
                        ShapeStyle.SQUARE -> R.string.shape_square
                        ShapeStyle.DEFAULT -> R.string.shape_default
                        ShapeStyle.ROUND -> R.string.shape_round
                    },
                ),
            ) { onOpen("appearance_ui") }
        }
        Text(stringResource(R.string.look_hub_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AppOutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.reset_look)) }
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
                    UiStore.selectTextScale(100); Link.prefs.textScale = 100
                    UiStore.selectDensity(UiDensity.DEFAULT); Link.prefs.uiDensity = UiDensity.DEFAULT.key
                    UiStore.selectSize(ElementSize.DEFAULT); Link.prefs.elementSize = ElementSize.DEFAULT.key
                    UiStore.selectMono(MonoFont.BUNDLED); Link.prefs.monoFont = MonoFont.BUNDLED.key
                    Link.prefs.paletteIcon = false
                    Widgets.updateAll(ctx)
                    syncIcon(ctx)
                    confirmReset = false
                }) { Text(stringResource(R.string.reset_look_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

// ---- theme: 10 palettes (6 dark, 4 light) and the system colors, with a preview before the choice is applied ----

@Composable
fun ThemeSection(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var iconByPalette by remember { mutableStateOf(Link.prefs.paletteIcon) }
    val applied = if (PaletteStore.dynamic) "system" else PaletteStore.current.name
    var preview by rememberSaveable { mutableStateOf(applied) }

    fun apply() {
        if (preview == "system") { PaletteStore.useSystem(); Link.prefs.dynamicColor = true }
        else Palettes.all.firstOrNull { it.name == preview }?.let { PaletteStore.select(it); Link.prefs.palette = it.name; Link.prefs.dynamicColor = false }
        Widgets.updateAll(ctx)
        syncIcon(ctx)
    }

    SubScreen(stringResource(R.string.look_theme), onBack) {
        Text(stringResource(R.string.theme_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ThemePreview(preview)
        AppButton(onClick = { apply() }, enabled = preview != applied, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (preview == applied) R.string.theme_applied else R.string.theme_apply))
        }
        SectionTitle(stringResource(R.string.theme_dark))
        Palettes.all.filter { it.dark }.forEach { p -> PaletteRow(p, preview == p.name, applied == p.name) { preview = p.name } }
        SectionTitle(stringResource(R.string.theme_light))
        Palettes.all.filter { !it.dark }.forEach { p -> PaletteRow(p, preview == p.name, applied == p.name) { preview = p.name } }
        SectionTitle(stringResource(R.string.theme_system_title))
        // material you: colors from the wallpaper, follows the system light / dark setting
        SelectableCard(selected = preview == "system", onClick = { preview = "system" }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.system_colors), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (applied == "system") AppliedMark()
            }
            Text(stringResource(R.string.system_colors_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SectionTitle(stringResource(R.string.look_icon))
        Panel {
            SwitchRow(stringResource(R.string.icon_by_palette), stringResource(R.string.icon_by_palette_desc), iconByPalette) {
                iconByPalette = it
                Link.prefs.paletteIcon = it
                syncIcon(ctx)
            }
            // a palette without its own launcher icon is not hidden behind a switch that seems to do something
            if (iconByPalette && (PaletteStore.dynamic || !LauncherIcon.hasOwn(PaletteStore.current.name))) {
                Text(stringResource(R.string.icon_fallback), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AppliedMark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.theme_applied), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun PaletteRow(p: Palette, selected: Boolean, applied: Boolean, onClick: () -> Unit) {
    SelectableCard(selected, onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(p.label, style = MaterialTheme.typography.titleMedium)
                // dark or light is written, not only shown by color
                Text(
                    stringResource(if (p.dark) R.string.theme_is_dark else R.string.theme_is_light),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (applied) AppliedMark()
            Row(Modifier.padding(start = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(p.scheme.background, p.scheme.surfaceContainerHigh, p.scheme.primary, p.scheme.secondary, p.scheme.tertiary).forEach { c ->
                    Box(Modifier.size(width = 18.dp, height = 16.dp).background(c, MaterialTheme.shapes.extraSmall).border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraSmall))
                }
            }
        }
    }
}

// the chosen palette drawn small, with the logo recolored for it. nothing in it is a control
@Composable
private fun ThemePreview(name: String) {
    val ctx = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val palette = Palettes.all.firstOrNull { it.name == name }
    val scheme = when {
        palette != null -> palette.scheme
        systemDark -> dynamicDarkColorScheme(ctx)
        else -> dynamicLightColorScheme(ctx)
    }
    val logo = if (palette != null) LogoColors(palette.logoTile, palette.logoLine) else LogoColors(scheme.surfaceContainerHigh, scheme.primary)
    MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes) {
        CompositionLocalProvider(LocalLogoColors provides logo) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) { MiniPreview() }
        }
    }
}

// logo, a panel with text, two button shapes and an error line, in whatever theme surrounds it
@Composable
private fun MiniPreview() {
    val desc = stringResource(R.string.preview_desc)
    Column(
        Modifier.padding(12.dp).clearAndSetSemantics { contentDescription = desc },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProxyLogo(size = 28.dp)
            Column {
                Text("ProxyLink", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Text(stringResource(R.string.preview_status), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column(Modifier.fillMaxWidth().padding(UiStore.density.pad.dp)) {
                Text(stringResource(R.string.preview_panel), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("192.168.0.12  a1b2c3", style = MonoStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(color = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small) {
                Text(stringResource(R.string.preview_send), Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
            }
            Surface(color = androidx.compose.ui.graphics.Color.Transparent, shape = MaterialTheme.shapes.small, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                Text(stringResource(R.string.cancel), Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
            Text(stringResource(R.string.preview_error), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

// ---- font: ui font, text size, monospace, live preview ----

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FontSection(onBack: () -> Unit) {
    SubScreen(stringResource(R.string.look_font), onBack) {
        SectionTitle(stringResource(R.string.font_title))
        AppFont.entries.forEach { f ->
            val pair = f.pair()
            SelectableCard(selected = FontStore.current == f, onClick = { FontStore.select(f); Link.prefs.font = f.key }) {
                Text(if (f == AppFont.SYSTEM) stringResource(R.string.font_system) else f.label, style = MaterialTheme.typography.titleMedium.copy(fontFamily = pair.heading))
                Text(stringResource(R.string.font_preview), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = pair.body), modifier = Modifier.padding(top = 4.dp))
            }
        }
        Text(stringResource(R.string.font_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        SectionTitle(stringResource(R.string.font_size_title))
        Text(stringResource(R.string.font_size_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextScales.forEach { p ->
                FilterChip(
                    selected = UiStore.textScale == p,
                    onClick = { UiStore.selectTextScale(p); Link.prefs.textScale = p },
                    label = { Text("$p %") },
                )
            }
        }

        SectionTitle(stringResource(R.string.font_mono_title))
        Text(stringResource(R.string.font_mono_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MonoFont.entries.forEach { m ->
            SelectableCard(selected = UiStore.mono == m, onClick = { UiStore.selectMono(m); Link.prefs.monoFont = m.key }) {
                Text(stringResource(if (m == MonoFont.BUNDLED) R.string.mono_bundled else R.string.mono_system), style = MaterialTheme.typography.titleMedium)
                Text(
                    "192.168.0.12  a1b2c3d4",
                    style = MonoStyle.copy(fontFamily = if (m == MonoFont.SYSTEM) FontFamily.Monospace else MonoFamily),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        SectionTitle(stringResource(R.string.look_preview))
        Panel {
            Text(stringResource(R.string.preview_heading), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.font_preview), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            Text("192.168.0.12  a1b2c3d4", style = MonoStyle, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

// ---- interface look: corners, density, element size, animations ----

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InterfaceSection(onBack: () -> Unit) {
    SubScreen(stringResource(R.string.look_ui), onBack) {
        ShapeOptions()

        SectionTitle(stringResource(R.string.density_title))
        Text(stringResource(R.string.density_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            UiDensity.entries.forEach { d ->
                FilterChip(
                    selected = UiStore.density == d,
                    onClick = { UiStore.selectDensity(d); Link.prefs.uiDensity = d.key },
                    label = { Text(stringResource(when (d) { UiDensity.COMPACT -> R.string.density_compact; UiDensity.DEFAULT -> R.string.density_default; UiDensity.COMFORTABLE -> R.string.density_comfortable })) },
                )
            }
        }

        SectionTitle(stringResource(R.string.size_title))
        Text(stringResource(R.string.size_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ElementSize.entries.forEach { e ->
                FilterChip(
                    selected = UiStore.size == e,
                    onClick = { UiStore.selectSize(e); Link.prefs.elementSize = e.key },
                    label = { Text(stringResource(when (e) { ElementSize.SMALL -> R.string.size_small; ElementSize.DEFAULT -> R.string.size_default; ElementSize.LARGE -> R.string.size_large })) },
                )
            }
        }

        // animations are not a switch of the app: they follow the system setting "remove animations"
        SectionTitle(stringResource(R.string.anim_title))
        StateNote(stringResource(if (UiStore.animations) R.string.anim_on else R.string.anim_off))

        SectionTitle(stringResource(R.string.look_preview))
        Surface(color = MaterialTheme.colorScheme.background, shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) { MiniPreview() }
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
