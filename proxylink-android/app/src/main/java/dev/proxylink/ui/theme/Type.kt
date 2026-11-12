@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package dev.proxylink.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.proxylink.R

// fonts are bundled (variable ttf, ofl license): they work offline and need no google play services.
private fun inter(w: FontWeight) =
    Font(R.font.inter, w, variationSettings = FontVariation.Settings(FontVariation.weight(w.weight)))

private fun grotesk(w: FontWeight) =
    Font(R.font.space_grotesk, w, variationSettings = FontVariation.Settings(FontVariation.weight(w.weight)))

private fun mono(w: FontWeight) =
    Font(R.font.inconsolata, w, variationSettings = FontVariation.Settings(FontVariation.weight(w.weight)))

val InterFamily = FontFamily(inter(FontWeight.Normal), inter(FontWeight.Medium), inter(FontWeight.SemiBold))
val GroteskFamily = FontFamily(grotesk(FontWeight.Normal), grotesk(FontWeight.Medium), grotesk(FontWeight.Bold))
val MonoFamily = FontFamily(mono(FontWeight.Normal), mono(FontWeight.Medium))

private fun plex(w: FontWeight) =
    Font(R.font.ibm_plex_sans, w, variationSettings = FontVariation.Settings(FontVariation.weight(w.weight)))

val PlexFamily = FontFamily(plex(FontWeight.Normal), plex(FontWeight.Medium), plex(FontWeight.SemiBold), plex(FontWeight.Bold))

// static fonts with two cuts: medium text is drawn in regular, semibold in bold
val AtkinsonFamily = FontFamily(Font(R.font.atkinson_regular, FontWeight.Normal), Font(R.font.atkinson_bold, FontWeight.Bold))

// the main ui font the user can pick. technical data (ids, addresses) always stay in the monospace font (MonoStyle)
enum class AppFont(val key: String, val label: String) {
    DEFAULT("default", "space grotesk + inter"),
    INTER("inter", "inter"),
    PLEX("plex", "ibm plex sans"),
    ATKINSON("atkinson", "atkinson hyperlegible"),
    SYSTEM("system", ""),
}

class FontPair(val heading: FontFamily, val body: FontFamily)

fun AppFont.pair(): FontPair = when (this) {
    AppFont.DEFAULT -> FontPair(GroteskFamily, InterFamily)
    AppFont.INTER -> FontPair(InterFamily, InterFamily)
    AppFont.PLEX -> FontPair(PlexFamily, PlexFamily)
    AppFont.ATKINSON -> FontPair(AtkinsonFamily, AtkinsonFamily)
    AppFont.SYSTEM -> FontPair(FontFamily.Default, FontFamily.Default)
}

// global font state, persisted through Prefs. an unknown stored value falls back to the default
object FontStore {
    var current by mutableStateOf(AppFont.DEFAULT)
        private set

    fun select(f: AppFont) { current = f }
    fun load(key: String) { current = AppFont.entries.firstOrNull { it.key == key } ?: AppFont.DEFAULT }
}

// material 3 type scale: display + headline = space grotesk, title + body + label = inter
fun proxyTypography(heading: FontFamily, body: FontFamily) = Typography(
    displayLarge = TextStyle(fontFamily = heading, fontWeight = FontWeight.Medium, fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontFamily = heading, fontWeight = FontWeight.Medium, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = heading, fontWeight = FontWeight.Medium, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = heading, fontWeight = FontWeight.Medium, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = heading, fontWeight = FontWeight.Medium, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = heading, fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

val ProxyTypography = proxyTypography(GroteskFamily, InterFamily)

// ids, fingerprints, pairing codes
val MonoStyle = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
