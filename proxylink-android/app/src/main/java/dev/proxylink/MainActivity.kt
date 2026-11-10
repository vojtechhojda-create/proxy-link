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
import dev.proxylink.core.LauncherIcon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.proxylink.ui.components.PairLinkDialog
import dev.proxylink.ui.nav.ProxylinkApp
import dev.proxylink.ui.theme.PaletteStore
import dev.proxylink.ui.theme.ProxylinkTheme

class MainActivity : ComponentActivity() {
    private var pendingLink by mutableStateOf<Link.LinkInfo?>(null)
    private val notifPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { startLink() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Link.init(this)
        PaletteStore.load(Link.prefs.palette, Link.prefs.dynamicColor)
        // keeps exactly one launcher entry enabled and in line with the saved choice
        LauncherIcon.apply(this, Link.prefs.paletteIcon, Link.prefs.palette, Link.prefs.dynamicColor)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            ProxylinkTheme {
                ProxylinkApp()
                pendingLink?.let { info ->
                    PairLinkDialog(info, onConfirm = { Link.connectTo(info); pendingLink = null }, onCancel = { pendingLink = null })
                }
            }
        }

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
        // a tag or a link from another app never pairs by itself, the user has to confirm it
        val info = Link.parseLink(data.toString())
        if (info == null) Toast.makeText(this, R.string.invalid_pairing_link, Toast.LENGTH_SHORT).show()
        else pendingLink = info
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
