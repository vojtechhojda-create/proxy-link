package dev.proxylink.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.proxylink.R
import dev.proxylink.core.Capabilities
import dev.proxylink.core.Compat
import dev.proxylink.core.Feature
import dev.proxylink.core.Link
import dev.proxylink.core.LinkState

// one compact panel under the connected device: the real target and link state, the three real send actions and a collapsed
// "more" part. nothing is sent by opening this panel, each button opens a picker or sends the clipboard on a deliberate tap
@Composable
fun QuickActionsPanel(actions: SendActions) {
    var more by rememberSaveable { mutableStateOf(false) }
    val connected = Link.state == LinkState.CONNECTED
    Panel {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TargetStatus()
            TargetPicker()
            SendButtons(actions)
            if (Link.transfersBlocked) QuickNote(stringResource(R.string.id_blocked_note))
            // versions that do not fit say so; unequal versions with a common baseline add no note, the basic transfers work
            if (connected) compatNote(Capabilities.compat)?.let { QuickNote(stringResource(it)) }
            // why a button is dimmed: a missing pc capability or the guest connection, never presented as "switched off"
            if (connected && !Capabilities.supports(Feature.FILES)) QuickNote(stringResource(R.string.qa_files_unsupported))
            else if (connected && Link.guest && Link.clipEnabled) QuickNote(stringResource(R.string.clip_status_guest))
            TextButton(onClick = { more = !more }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Text(stringResource(R.string.qa_more))
                Icon(if (more) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            AnimatedVisibility(more) {
                Column {
                    // sending over nfc between two phones does not exist in the app: the row is information, not a button
                    InfoRow(Icons.Default.Nfc, stringResource(R.string.qa_nfc), stringResource(R.string.qa_nfc_unavailable))
                    // the pc app has no screen sharing message yet, so there is no switch
                    InfoRow(Icons.Default.ScreenShare, stringResource(R.string.qa_screen), stringResource(if (Capabilities.supports(Feature.SCREEN)) R.string.qa_screen_pc_ready else R.string.qa_screen_soon))
                }
            }
        }
    }
}

@Composable
private fun QuickNote(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// the one line the compatibility state adds under the quick actions; null when there is nothing to warn about
fun compatNote(c: Compat): Int? = when (c) {
    Compat.PC_TOO_OLD -> R.string.compat_note_pc_old
    Compat.MOBILE_TOO_OLD -> R.string.compat_note_mobile_old
    else -> null
}
