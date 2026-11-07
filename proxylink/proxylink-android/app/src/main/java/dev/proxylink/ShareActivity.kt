package dev.proxylink

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import dev.proxylink.core.Link
import dev.proxylink.core.LinkState

// target of the android share sheet (also the direct share shortcuts) and of the text selection menu
class ShareActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Link.init(this)
        if (Link.state != LinkState.CONNECTED) {
            Toast.makeText(this, "proxylink: not connected to pc", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // direct share shortcut id: "pc" or the id of another device
        val sc = intent.getStringExtra(Intent.EXTRA_SHORTCUT_ID)
        val target = if (sc == null || sc == "pc") null else Link.others.firstOrNull { it.id == sc }
        if (sc != null && sc != "pc" && target == null) {
            Toast.makeText(this, "proxylink: that device is not connected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val uris: List<Uri> = when (intent.action) {
            Intent.ACTION_SEND -> listOfNotNull(intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))
            Intent.ACTION_SEND_MULTIPLE ->
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java) ?: emptyList<Uri>()
            else -> emptyList()
        }
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()

        if (uris.isNotEmpty()) uris.forEach { Link.sendFile(it, target) }
        else if (!text.isNullOrBlank()) Link.sendText(text, target)
        Toast.makeText(this, "sent to ${target?.name ?: Link.pcName}", Toast.LENGTH_SHORT).show()
        finish()
    }
}
