package dev.proxylink

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import dev.proxylink.core.Capabilities
import dev.proxylink.core.Feature
import dev.proxylink.core.Link
import dev.proxylink.core.LinkState

// opened by the home screen widget. the clipboard can only be read while a window has focus (android 10+).
class ClipSendActivity : Activity() {
    private var done = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Link.init(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || done) return
        done = true
        // the widget stays on the home screen when the clipboard is off, it only says why nothing was sent
        val msg = when {
            !Link.clipEnabled -> R.string.toast_clip_off
            Link.state != LinkState.CONNECTED -> R.string.toast_not_connected
            Link.guest || !Capabilities.supports(Feature.CLIPBOARD) -> R.string.toast_clip_unavailable
            else -> if (Link.sendClipboardNow()) R.string.toast_clip_sent else R.string.toast_clip_empty
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        finish()
    }
}
