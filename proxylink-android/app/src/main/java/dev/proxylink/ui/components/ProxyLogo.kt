package dev.proxylink.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import dev.proxylink.ui.theme.PaletteStore

// base brand colors
object Brand {
    val Tile = Color(0xFF111827)
    val Green = Color(0xFF22C55E)
    val Paper = Color(0xFFFCEDEA)
    val Wordmark = Color(0xFF1F1A1A)
}

// chain-link shape (viewBox 100x100). useBrand = fixed brand colors, otherwise active palette.
@Composable
fun ProxyLogo(size: Dp, useBrand: Boolean = false) {
    val p = PaletteStore.current
    val tile = if (useBrand) Brand.Tile else p.surface
    val line = if (useBrand) Brand.Green else p.accentBright

    Canvas(Modifier.size(size)) {
        val k = this.size.minDimension / 100f
        drawRoundRect(color = tile, cornerRadius = CornerRadius(30f * k))
        val stroke = Stroke(width = 8f * k)
        val corner = CornerRadius(14f * k)
        drawRoundRect(
            color = line,
            topLeft = Offset(17f * k, 36f * k),
            size = Size(42f * k, 28f * k),
            cornerRadius = corner,
            style = stroke,
        )
        drawRoundRect(
            color = line,
            topLeft = Offset(41f * k, 36f * k),
            size = Size(42f * k, 28f * k),
            cornerRadius = corner,
            style = stroke,
        )
    }
}
