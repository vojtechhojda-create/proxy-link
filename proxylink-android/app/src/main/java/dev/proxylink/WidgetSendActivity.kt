package dev.proxylink

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import dev.proxylink.core.Capabilities
import dev.proxylink.core.Feature
import dev.proxylink.core.Link
import dev.proxylink.core.LinkState

// opened by the file and photo actions of the widget. it only opens the system picker: what is sent is what the user picks,
// nothing is sent without that choice. without a connection the picker is not opened at all
class WidgetSendActivity : ComponentActivity() {
    private val pickFiles = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { u ->
            runCatching { contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            Link.sendFile(u)
        }
        finishWith(uris)
    }
    private val pickPhotos = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        uris.forEach { Link.sendImage(it) }
        finishWith(uris)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Link.init(this)
        val msg = when {
            Link.state != LinkState.CONNECTED -> R.string.toast_not_connected
            !Capabilities.supports(Feature.FILES) -> R.string.toast_files_unavailable
            else -> null
        }
        if (msg != null) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); finish(); return }
        // the process has to stay alive while the transfer runs after this window is gone
        runCatching { startForegroundService(Intent(this, LinkService::class.java)) }
        if (intent.getStringExtra(KIND) == "image") {
            pickPhotos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            pickFiles.launch(arrayOf("*/*"))
        }
    }

    private fun finishWith(uris: List<Uri>) {
        if (uris.isNotEmpty()) Toast.makeText(this, R.string.toast_widget_sent, Toast.LENGTH_SHORT).show()
        finish()
    }

    companion object { const val KIND = "kind" }
}
