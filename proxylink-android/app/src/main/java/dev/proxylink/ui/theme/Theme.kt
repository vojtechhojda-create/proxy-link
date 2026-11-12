package dev.proxylink.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

data class LogoColors(val tile: Color, val line: Color)

val LocalLogoColors = staticCompositionLocalOf { LogoColors(Palettes.brand.logoTile, Palettes.brand.logoLine) }

private fun Context.activity(): Activity? {
    var c = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

@Composable
fun ProxylinkTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val dynamic = PaletteStore.dynamic
    val palette = PaletteStore.current

    // a palette says itself if it is dark; material you follows the system light/dark setting
    val dark = if (dynamic) systemDark else palette.dark
    val scheme = when {
        dynamic && systemDark -> dynamicDarkColorScheme(ctx)
        dynamic -> dynamicLightColorScheme(ctx)
        else -> palette.scheme
    }
    val logo = if (dynamic) LogoColors(scheme.surfaceContainerHigh, scheme.primary)
    else LogoColors(palette.logoTile, palette.logoLine)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            ctx.activity()?.window?.let { w ->
                WindowCompat.getInsetsController(w, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
    }

    CompositionLocalProvider(LocalLogoColors provides logo) {
        val font = FontStore.current
        val typography = remember(font) { font.pair().let { proxyTypography(it.heading, it.body) } }
        val shapes = remember(ShapeStore.current) { proxyShapes(ShapeStore.current) }
        MaterialTheme(colorScheme = scheme, typography = typography, shapes = shapes, content = content)
    }
}
