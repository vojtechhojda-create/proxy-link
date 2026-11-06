package dev.proxylink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.proxylink.core.Link
import dev.proxylink.ui.nav.ProxylinkApp
import dev.proxylink.ui.theme.PaletteStore
import dev.proxylink.ui.theme.ProxylinkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Link.init(this)
        PaletteStore.load(Link.prefs.palette)
        enableEdgeToEdge()
        setContent { ProxylinkTheme { ProxylinkApp() } }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) Link.pushClipboard()
    }
}
