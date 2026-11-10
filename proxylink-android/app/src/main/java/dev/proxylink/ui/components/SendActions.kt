package dev.proxylink.ui.components

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import dev.proxylink.core.Link

class SendActions(val file: () -> Unit, val image: () -> Unit, val clipboard: () -> Unit)

// file picker, photo picker (no storage permission) and clipboard, sent to the selected target
@Composable
fun rememberSendActions(): SendActions {
    val ctx = LocalContext.current
    val haptic = rememberHaptic()
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { u ->
            runCatching { ctx.contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            Link.sendFile(u)
        }
    }
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        uris.forEach { Link.sendImage(it) }
    }
    return SendActions(
        file = { haptic(); fileLauncher.launch(arrayOf("*/*")) },
        image = { haptic(); imageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        clipboard = { haptic(); Link.pushClipboard(manual = true) },
    )
}
