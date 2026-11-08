package dev.proxylink

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import dev.proxylink.core.Link
import dev.proxylink.ui.nav.ProxylinkApp
import dev.proxylink.ui.theme.PaletteStore
import dev.proxylink.ui.theme.ProxylinkTheme

class MainActivity : ComponentActivity() {
    private val notifPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { startLink() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Link.init(this)
        PaletteStore.load(Link.prefs.palette, Link.prefs.dynamicColor)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent { ProxylinkTheme { ProxylinkApp() } }

        // a proxylink:// link or nfc tag that started the app
        if (savedInstanceState == null) handleLink(intent)

        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) startLink()
        else notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLink(intent)
    }

    private fun handleLink(i: Intent?) {
        val data = i?.data ?: return
        if (data.scheme != "proxylink") return
        if (!Link.pairFromQr(data.toString())) {
            Toast.makeText(this, "not a valid pairing link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startLink() {
        startForegroundService(Intent(this, LinkService::class.java))
    }

    override fun onResume() {
        super.onResume()
        Link.foreground = true
    }

    override fun onPause() {
        Link.foreground = false
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) Link.pushClipboard()
    }
}
