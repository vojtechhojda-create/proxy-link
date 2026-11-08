package dev.proxylink.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import dev.proxylink.ui.theme.LocalLogoColors

// base brand colors
object Brand {
    val Tile = Color(0xFF111827)
    val Green = Color(0xFF22C55E)
}

// v1 logo measured from the original (100 x 100 units): tile radius 30, two brackets with flat ends
// (stroke 5.5, gap 5) and a short bar. colors come from the active theme, useBrand forces #111827 / #22C55E.
@Composable
fun ProxyLogo(size: Dp, useBrand: Boolean = false) {
    val logo = LocalLogoColors.current
    val tile = if (useBrand) Brand.Tile else logo.tile
    val line = if (useBrand) Brand.Green else logo.line

    Canvas(Modifier.size(size)) {
        val k = this.size.minDimension / 100f
        scale(k, k, pivot = Offset.Zero) {
            drawRoundRect(color = tile, size = Size(100f, 100f), cornerRadius = CornerRadius(30f))
            val st = Stroke(width = 5.5f, cap = StrokeCap.Butt)
            val left = Path().apply {
                moveTo(47.5f, 39.75f)
                lineTo(38f, 39.75f)
                arcTo(Rect(27.75f, 39.75f, 48.25f, 60.25f), -90f, -180f, false)
                lineTo(47.5f, 60.25f)
            }
            val right = Path().apply {
                moveTo(52.5f, 39.75f)
                lineTo(62f, 39.75f)
                arcTo(Rect(51.75f, 39.75f, 72.25f, 60.25f), -90f, 180f, false)
                lineTo(52.5f, 60.25f)
            }
            drawPath(left, line, style = st)
            drawPath(right, line, style = st)
            drawRoundRect(
                color = line,
                topLeft = Offset(39.5f, 47.25f),
                size = Size(21f, 5.5f),
                cornerRadius = CornerRadius(1f),
            )
        }
    }
}
