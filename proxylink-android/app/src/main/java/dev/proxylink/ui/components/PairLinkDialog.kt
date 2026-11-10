package dev.proxylink.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.proxylink.R
import dev.proxylink.core.Link

// confirmation for a pairing link that came from outside the app (nfc tag, another app)
@Composable
fun PairLinkDialog(info: Link.LinkInfo, onConfirm: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(if (info.kind == "guest") R.string.guest_dialog_title else R.string.pair_dialog_title, info.name)) },
        text = { Text(stringResource(R.string.link_dialog_text, info.host)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.connect)) } },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } },
    )
}
