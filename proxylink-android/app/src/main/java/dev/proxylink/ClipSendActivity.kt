package dev.proxylink

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
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
        if (Link.state == LinkState.CONNECTED) {
            Link.pushClipboard(manual = true)
            Toast.makeText(this, "clipboard sent", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "proxylink: not connected", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
