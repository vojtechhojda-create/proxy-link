package dev.proxylink.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// restrained corners, one set for the whole app
val ProxyShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(12.dp),
)
