package dev.proxylink.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

// one corner set for the whole app, chosen in appearance. applied only through the theme
enum class ShapeStyle(val key: String, val xs: Int, val s: Int, val m: Int, val l: Int, val xl: Int) {
    SQUARE("square", 2, 3, 4, 6, 8),
    DEFAULT("default", 4, 6, 8, 10, 12),
    ROUND("round", 8, 12, 16, 20, 28),
}

fun proxyShapes(style: ShapeStyle) = Shapes(
    extraSmall = RoundedCornerShape(style.xs.dp),
    small = RoundedCornerShape(style.s.dp),
    medium = RoundedCornerShape(style.m.dp),
    large = RoundedCornerShape(style.l.dp),
    extraLarge = RoundedCornerShape(style.xl.dp),
)

object ShapeStore {
    var current by mutableStateOf(ShapeStyle.DEFAULT)
        private set

    fun select(s: ShapeStyle) { current = s }
    fun load(key: String) { current = ShapeStyle.entries.firstOrNull { it.key == key } ?: ShapeStyle.DEFAULT }
}
