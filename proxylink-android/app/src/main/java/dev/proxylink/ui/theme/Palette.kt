package dev.proxylink.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

data class Palette(
    val name: String,
    val label: String,
    val scheme: ColorScheme,
    val logoTile: Color,
    val logoLine: Color,
)

object Palettes {
    // base palette from the logo: #111827 and #22C55E
    val brand = Palette(
        name = "brand",
        label = "brand",
        logoTile = Color(0xFF111827),
        logoLine = Color(0xFF22C55E),
        scheme = darkColorScheme(
            background = Color(0xFF0B1220),
            onBackground = Color(0xFFE5E7EB),
            surface = Color(0xFF111827),
            onSurface = Color(0xFFE5E7EB),
            surfaceVariant = Color(0xFF1F2937),
            onSurfaceVariant = Color(0xFF94A3B8),
            surfaceTint = Color(0xFF22C55E),
            surfaceDim = Color(0xFF0B1220),
            surfaceBright = Color(0xFF263140),
            surfaceContainerLowest = Color(0xFF080E19),
            surfaceContainerLow = Color(0xFF151C28),
            surfaceContainer = Color(0xFF1A2330),
            surfaceContainerHigh = Color(0xFF212B3A),
            surfaceContainerHighest = Color(0xFF2A3547),
            outline = Color(0xFF334155),
            outlineVariant = Color(0xFF243044),
            primary = Color(0xFF22C55E),
            onPrimary = Color(0xFF052E16),
            primaryContainer = Color(0xFF14532D),
            onPrimaryContainer = Color(0xFFD1FAE5),
            inversePrimary = Color(0xFF16A34A),
            secondary = Color(0xFF38BDF8),
            onSecondary = Color(0xFF0C4A6E),
            secondaryContainer = Color(0xFF0C4A6E),
            onSecondaryContainer = Color(0xFFE0F2FE),
            tertiary = Color(0xFFF59E0B),
            onTertiary = Color(0xFF78350F),
            tertiaryContainer = Color(0xFF78350F),
            onTertiaryContainer = Color(0xFFFEF3C7),
            error = Color(0xFFEF4444),
            onError = Color(0xFF450A0A),
            errorContainer = Color(0xFF7F1D1D),
            onErrorContainer = Color(0xFFFEE2E2),
            inverseSurface = Color(0xFFE5E7EB),
            inverseOnSurface = Color(0xFF0F172A),
            scrim = Color(0xFF000000),
        ),
    )

    // navy blue with an orange accent
    val navyOrange = Palette(
        name = "navyOrange",
        label = "navy orange",
        logoTile = Color(0xFF1B2743),
        logoLine = Color(0xFFF97316),
        scheme = darkColorScheme(
            background = Color(0xFF0B1120),
            onBackground = Color(0xFFE5E7EB),
            surface = Color(0xFF111A2C),
            onSurface = Color(0xFFE5E7EB),
            surfaceVariant = Color(0xFF1E293B),
            onSurfaceVariant = Color(0xFF94A3B8),
            surfaceTint = Color(0xFFF97316),
            surfaceDim = Color(0xFF0B1120),
            surfaceBright = Color(0xFF2A3A5E),
            surfaceContainerLowest = Color(0xFF080D1A),
            surfaceContainerLow = Color(0xFF16203A),
            surfaceContainer = Color(0xFF1B2743),
            surfaceContainerHigh = Color(0xFF223052),
            surfaceContainerHighest = Color(0xFF2B3C63),
            outline = Color(0xFF334155),
            outlineVariant = Color(0xFF243044),
            primary = Color(0xFFF97316),
            onPrimary = Color(0xFF431407),
            primaryContainer = Color(0xFF7C2D12),
            onPrimaryContainer = Color(0xFFFFEDD5),
            inversePrimary = Color(0xFFEA580C),
            secondary = Color(0xFF38BDF8),
            onSecondary = Color(0xFF0C4A6E),
            secondaryContainer = Color(0xFF0C4A6E),
            onSecondaryContainer = Color(0xFFE0F2FE),
            tertiary = Color(0xFF22C55E),
            onTertiary = Color(0xFF052E16),
            tertiaryContainer = Color(0xFF14532D),
            onTertiaryContainer = Color(0xFFD1FAE5),
            error = Color(0xFFEF4444),
            onError = Color(0xFF450A0A),
            errorContainer = Color(0xFF7F1D1D),
            onErrorContainer = Color(0xFFFEE2E2),
            inverseSurface = Color(0xFFE5E7EB),
            inverseOnSurface = Color(0xFF0F172A),
            scrim = Color(0xFF000000),
        ),
    )

    val all = listOf(brand, navyOrange)
}

// global palette state, persisted through Prefs. dynamic = material you (system colors)
object PaletteStore {
    var current by mutableStateOf(Palettes.brand)
        private set
    var dynamic by mutableStateOf(false)
        private set

    fun select(p: Palette) { current = p; dynamic = false }
    fun useSystem() { dynamic = true }

    fun load(name: String, dynamicColor: Boolean) {
        current = Palettes.all.firstOrNull { it.name == name } ?: Palettes.brand
        dynamic = dynamicColor
    }
}
