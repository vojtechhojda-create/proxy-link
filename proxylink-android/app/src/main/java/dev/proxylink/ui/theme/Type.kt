@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package dev.proxylink.ui.theme

import androidx.compose.material3.Typography
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

// material 3 type scale: display + headline = space grotesk, title + body + label = inter
val ProxyTypography = Typography(
    displayLarge = TextStyle(fontFamily = GroteskFamily, fontWeight = FontWeight.Medium, fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontFamily = GroteskFamily, fontWeight = FontWeight.Medium, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = GroteskFamily, fontWeight = FontWeight.Medium, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = GroteskFamily, fontWeight = FontWeight.Medium, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = GroteskFamily, fontWeight = FontWeight.Medium, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = GroteskFamily, fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

// ids, fingerprints, pairing codes
val MonoStyle = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
