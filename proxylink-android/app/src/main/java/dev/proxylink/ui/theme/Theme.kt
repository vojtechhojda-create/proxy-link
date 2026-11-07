package dev.proxylink.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun ProxylinkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PaletteStore.current.scheme(),
        typography = ProxyTypography,
        content = content,
    )
}
