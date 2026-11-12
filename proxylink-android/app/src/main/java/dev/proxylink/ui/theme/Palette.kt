package dev.proxylink.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
    val dark: Boolean = true,   // false = light palette, the system bars follow it
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

    // neutral graphite with a sky blue accent, a violet second color

    val graphite = Palette(
        name = "graphite",
        label = "graphite",
        logoTile = Color(0xFF15181E),
        logoLine = Color(0xFF38BDF8),
        dark = true,
        scheme = darkColorScheme(
            onBackground = Color(0xFFE5E7EB),
            onSurface = Color(0xFFE5E7EB),
            error = Color(0xFFEF4444),
            onError = Color(0xFF450A0A),
            errorContainer = Color(0xFF7F1D1D),
            onErrorContainer = Color(0xFFFEE2E2),
            inverseSurface = Color(0xFFE5E7EB),
            inverseOnSurface = Color(0xFF0F172A),
            scrim = Color(0xFF000000),
            background = Color(0xFF0F1115),
            surface = Color(0xFF16191F),
            surfaceVariant = Color(0xFF232830),
            onSurfaceVariant = Color(0xFF9AA3B2),
            surfaceTint = Color(0xFF38BDF8),
            surfaceDim = Color(0xFF0F1115),
            surfaceBright = Color(0xFF2B313B),
            surfaceContainerLowest = Color(0xFF0B0D11),
            surfaceContainerLow = Color(0xFF14171C),
            surfaceContainer = Color(0xFF191D23),
            surfaceContainerHigh = Color(0xFF20252C),
            surfaceContainerHighest = Color(0xFF292F38),
            outline = Color(0xFF68717F),
            outlineVariant = Color(0xFF262C35),
            primary = Color(0xFF38BDF8),
            onPrimary = Color(0xFF04283D),
            primaryContainer = Color(0xFF0C4A6E),
            onPrimaryContainer = Color(0xFFE0F2FE),
            inversePrimary = Color(0xFF0284C7),
            secondary = Color(0xFFA78BFA),
            onSecondary = Color(0xFF2E1065),
            secondaryContainer = Color(0xFF4C1D95),
            onSecondaryContainer = Color(0xFFEDE9FE),
            tertiary = Color(0xFF34D399),
            onTertiary = Color(0xFF052E22),
            tertiaryContainer = Color(0xFF065F46),
            onTertiaryContainer = Color(0xFFD1FAE5),
        ),
    )

    // deep green with a yellow second color

    val forest = Palette(
        name = "forest",
        label = "forest",
        logoTile = Color(0xFF111C17),
        logoLine = Color(0xFF4ADE80),
        dark = true,
        scheme = darkColorScheme(
            onBackground = Color(0xFFE5E7EB),
            onSurface = Color(0xFFE5E7EB),
            error = Color(0xFFEF4444),
            onError = Color(0xFF450A0A),
            errorContainer = Color(0xFF7F1D1D),
            onErrorContainer = Color(0xFFFEE2E2),
            inverseSurface = Color(0xFFE5E7EB),
            inverseOnSurface = Color(0xFF0F172A),
            scrim = Color(0xFF000000),
            background = Color(0xFF0C1511),
            surface = Color(0xFF111C17),
            surfaceVariant = Color(0xFF1E2B25),
            onSurfaceVariant = Color(0xFF9DB3A8),
            surfaceTint = Color(0xFF4ADE80),
            surfaceDim = Color(0xFF0C1511),
            surfaceBright = Color(0xFF2A3B33),
            surfaceContainerLowest = Color(0xFF08100D),
            surfaceContainerLow = Color(0xFF0F1914),
            surfaceContainer = Color(0xFF15221C),
            surfaceContainerHigh = Color(0xFF1C2C24),
            surfaceContainerHighest = Color(0xFF24372E),
            outline = Color(0xFF5F7B6C),
            outlineVariant = Color(0xFF223329),
            primary = Color(0xFF4ADE80),
            onPrimary = Color(0xFF052E16),
            primaryContainer = Color(0xFF166534),
            onPrimaryContainer = Color(0xFFDCFCE7),
            inversePrimary = Color(0xFF16A34A),
            secondary = Color(0xFFFACC15),
            onSecondary = Color(0xFF422006),
            secondaryContainer = Color(0xFF713F12),
            onSecondaryContainer = Color(0xFFFEF9C3),
            tertiary = Color(0xFF38BDF8),
            onTertiary = Color(0xFF082F49),
            tertiaryContainer = Color(0xFF0C4A6E),
            onTertiaryContainer = Color(0xFFE0F2FE),
        ),
    )

    // dark plum with a magenta accent

    val plum = Palette(
        name = "plum",
        label = "plum",
        logoTile = Color(0xFF211828),
        logoLine = Color(0xFFE879F9),
        dark = true,
        scheme = darkColorScheme(
            onBackground = Color(0xFFE5E7EB),
            onSurface = Color(0xFFE5E7EB),
            error = Color(0xFFEF4444),
            onError = Color(0xFF450A0A),
            errorContainer = Color(0xFF7F1D1D),
            onErrorContainer = Color(0xFFFEE2E2),
            inverseSurface = Color(0xFFE5E7EB),
            inverseOnSurface = Color(0xFF0F172A),
            scrim = Color(0xFF000000),
            background = Color(0xFF150F1A),
            surface = Color(0xFF1B1422),
            surfaceVariant = Color(0xFF2B2034),
            onSurfaceVariant = Color(0xFFB3A3BF),
            surfaceTint = Color(0xFFE879F9),
            surfaceDim = Color(0xFF150F1A),
            surfaceBright = Color(0xFF3A2B47),
            surfaceContainerLowest = Color(0xFF100A15),
            surfaceContainerLow = Color(0xFF1A131F),
            surfaceContainer = Color(0xFF211828),
            surfaceContainerHigh = Color(0xFF2A2033),
            surfaceContainerHighest = Color(0xFF34283F),
            outline = Color(0xFF7D6A8E),
            outlineVariant = Color(0xFF33263E),
            primary = Color(0xFFE879F9),
            onPrimary = Color(0xFF4A044E),
            primaryContainer = Color(0xFF86198F),
            onPrimaryContainer = Color(0xFFFAE8FF),
            inversePrimary = Color(0xFFC026D3),
            secondary = Color(0xFFFB7185),
            onSecondary = Color(0xFF4C0519),
            secondaryContainer = Color(0xFF881337),
            onSecondaryContainer = Color(0xFFFFE4E6),
            tertiary = Color(0xFF38BDF8),
            onTertiary = Color(0xFF082F49),
            tertiaryContainer = Color(0xFF0C4A6E),
            onTertiaryContainer = Color(0xFFE0F2FE),
        ),
    )

    // the only light palette: warm paper, blue accent

    val paper = Palette(
        name = "paper",
        label = "paper",
        logoTile = Color(0xFF1E293B),
        logoLine = Color(0xFF60A5FA), // 5.8:1 on the tile; 1D4ED8 had only 2.2:1
        dark = false,
        scheme = lightColorScheme(
            background = Color(0xFFFAFAF7),
            onBackground = Color(0xFF1C1B1A),
            surface = Color(0xFFFAFAF7),
            onSurface = Color(0xFF1C1B1A),
            surfaceVariant = Color(0xFFE7E5DF),
            onSurfaceVariant = Color(0xFF55524C),
            surfaceTint = Color(0xFF1D4ED8),
            surfaceDim = Color(0xFFDDDBD5),
            surfaceBright = Color(0xFFFAFAF7),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFF4F3EE),
            surfaceContainer = Color(0xFFEEEDE7),
            surfaceContainerHigh = Color(0xFFE8E6E0),
            surfaceContainerHighest = Color(0xFFE2E0DA),
            outline = Color(0xFF7A766E),
            outlineVariant = Color(0xFFD3D0C8),
            primary = Color(0xFF1D4ED8),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFDBE4FF),
            onPrimaryContainer = Color(0xFF0A2A7A),
            inversePrimary = Color(0xFF93B4FF),
            secondary = Color(0xFFC2410C),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFFFE0CC),
            onSecondaryContainer = Color(0xFF5C2204),
            tertiary = Color(0xFF0F766E),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFCCF1EC),
            onTertiaryContainer = Color(0xFF04322E),
            error = Color(0xFFB3261E),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFF9DEDC),
            onErrorContainer = Color(0xFF410E0B),
            inverseSurface = Color(0xFF31302D),
            inverseOnSurface = Color(0xFFF4F1EA),
            scrim = Color(0xFF000000),
        ),
    )

    val all = listOf(brand, navyOrange, graphite, forest, plum, paper)
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
