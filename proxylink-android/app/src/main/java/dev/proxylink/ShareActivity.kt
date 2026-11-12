package dev.proxylink

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.proxylink.core.Link
import dev.proxylink.ui.screens.ShareConfirm
import dev.proxylink.ui.theme.FontStore
import dev.proxylink.ui.theme.ShapeStore
import dev.proxylink.ui.theme.PaletteStore
import dev.proxylink.ui.theme.ProxylinkTheme

// target of the android share sheet (also the direct share shortcuts) and of the text selection menu.
// nothing is sent here by itself: the shared content waits in this screen until the user confirms, also while the connection is made
class ShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Link.init(this)
        PaletteStore.load(Link.prefs.palette, Link.prefs.dynamicColor)
        FontStore.load(Link.prefs.font)
        ShapeStore.load(Link.prefs.shape)

        val uris: List<Uri> = when (intent.action) {
            Intent.ACTION_SEND -> listOfNotNull(intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))
            Intent.ACTION_SEND_MULTIPLE ->
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java) ?: emptyList<Uri>()
            else -> emptyList()
        }
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
        if (uris.isEmpty() && text.isNullOrBlank()) { finish(); return }

        setContent {
            ProxylinkTheme {
                ShareConfirm(uris, text, intent.getStringExtra(Intent.EXTRA_SHORTCUT_ID), onDone = { finish() })
            }
        }
    }
}
