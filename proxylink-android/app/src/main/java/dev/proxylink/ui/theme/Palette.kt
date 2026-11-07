package dev.proxylink.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

data class Palette(
    val name: String,
    val ink: Color,
    val abyss: Color,
    val night: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val primary: Color,
    val bright: Color,
    val accent: Color,
    val accentBright: Color,
    val text: Color,
    val muted: Color,
    val danger: Color = Color(0xFFFF6B6B),
) {
    fun scheme(): ColorScheme = darkColorScheme(
        primary = primary,
        onPrimary = text,
        primaryContainer = surfaceHigh,
        onPrimaryContainer = text,
        secondary = accent,
        onSecondary = text,
        secondaryContainer = surfaceHigh,
        onSecondaryContainer = accentBright,
        tertiary = bright,
        onTertiary = ink,
        background = abyss,
        onBackground = text,
        surface = surface,
        onSurface = text,
        surfaceVariant = surfaceHigh,
        onSurfaceVariant = muted,
        surfaceContainer = night,
        outline = muted,
        error = danger,
    )
}

object Palettes {
    val navy = Palette(
        name = "navy",
        ink = Color(0xFF010410), abyss = Color(0xFF040C24), night = Color(0xFF071436),
        surface = Color(0xFF0C1D4D), surfaceHigh = Color(0xFF14296A),
        primary = Color(0xFF2A4BAA), bright = Color(0xFF6E8FE0),
        accent = Color(0xFF606C38), accentBright = Color(0xFFA3B16A),
        text = Color(0xFFEAF0FB), muted = Color(0xFF8E9BBE),
    )

    val midnight = Palette(
        name = "midnight",
        ink = Color(0xFF02060D), abyss = Color(0xFF06101F), night = Color(0xFF0A1830),
        surface = Color(0xFF0F2340), surfaceHigh = Color(0xFF183158),
        primary = Color(0xFF1F6F8B), bright = Color(0xFF6FC3DF),
        accent = Color(0xFF3E8E7E), accentBright = Color(0xFF8FD4C2),
        text = Color(0xFFE6F4FA), muted = Color(0xFF8AA4B8),
    )

    val olive = Palette(
        name = "olive",
        ink = Color(0xFF060804), abyss = Color(0xFF0D120A), night = Color(0xFF141C0F),
        surface = Color(0xFF1B2514), surfaceHigh = Color(0xFF263320),
        primary = Color(0xFF606C38), bright = Color(0xFFA3B16A),
        accent = Color(0xFF2A4BAA), accentBright = Color(0xFF6E8FE0),
        text = Color(0xFFEFF3E4), muted = Color(0xFF9AA58A),
    )

    val all = listOf(navy, midnight, olive)
}

// global palette state, persisted through Prefs
object PaletteStore {
    var current by mutableStateOf(Palettes.navy)
        private set

    fun select(p: Palette) {
        current = p
    }

    fun load(name: String) {
        current = Palettes.all.firstOrNull { it.name == name } ?: Palettes.navy
    }
}
