package dev.proxylink.ui.screens

import android.content.Intent
import android.nfc.NfcAdapter
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.res.stringResource
import dev.proxylink.R
import dev.proxylink.core.KnownDevice
import dev.proxylink.core.Link
import dev.proxylink.core.LinkState
import dev.proxylink.core.Peer
import dev.proxylink.ui.components.*
import dev.proxylink.ui.theme.MonoStyle

@Composable
private fun ActionRow(icon: ImageVector, title: String, subtitle: String? = null, enabled: Boolean = true, onClick: () -> Unit) {
    val haptic = rememberHaptic()
    Row(
        Modifier.fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Button) { haptic(); onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon, contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// shown on the dashboard while no device is connected: connect, reconnect, forget
@Composable
fun NotConnectedScreen(onScan: () -> Unit, onNfc: () -> Unit) {
    val ctx = LocalContext.current
    var pairFor by remember { mutableStateOf<Peer?>(null) }
    var forgetting by remember { mutableStateOf<KnownDevice?>(null) }

    // nfc state is re-read when the user comes back from the system settings
    val adapter = remember { NfcAdapter.getDefaultAdapter(ctx) }
    var nfcTick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { nfcTick++ }
    val nfcOn = nfcTick >= 0 && adapter?.isEnabled == true

    Column(
        Modifier.fillMaxSize().verticalScrollCompat().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader("proxylink")

        Panel {
            Text(
                when (Link.state) {
                    LinkState.CONNECTING -> "connecting to ${Link.pcName}"
                    LinkState.NEEDS_PAIR -> "pairing required"
                    else -> "not connected"
                },
                style = MaterialTheme.typography.titleMedium,
            )
            if (Link.pairError) {
                Text("pairing failed or the code expired", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }

        SectionTitle("connect")
        Panel {
            ActionRow(Icons.Default.QrCodeScanner, "scan qr code", onClick = onScan)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            // the nfc screen itself explains a switched off nfc and offers the system settings
            if (adapter == null) ActionRow(Icons.Default.Nfc, "nfc", stringResource(R.string.nfc_unsupported), enabled = false) {}
            else ActionRow(Icons.Default.Nfc, "nfc", stringResource(if (nfcOn) R.string.nfc_row_ready else R.string.nfc_off), onClick = onNfc)
            // new devices found on the network that are not paired yet
            Link.peers.filter { Link.prefs.secret(it.id) == null }.forEach { p ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ActionRow(Icons.Default.Devices, p.name, "enter code") { pairFor = p }
            }
        }

        if (Link.known.isNotEmpty()) {
            SectionTitle("paired devices")
            Link.known.forEach { d ->
                val inRange = Link.peers.any { it.id == d.id }
                val searching = Link.reconnecting == d.id
                Panel {
                    Text(d.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        when {
                            searching -> "searching…"
                            inRange -> "in range"
                            else -> "not in range"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (searching) {
                            AppOutlinedButton(onClick = { Link.cancelReconnect() }) { Text("stop searching") }
                        } else {
                            AppButton(onClick = { Link.reconnect(d.id) }) { Text("reconnect") }
                        }
                        AppOutlinedButton(onClick = { forgetting = d }) { Text("forget") }
                    }
                }
            }
        }
    }

    forgetting?.let { d ->
        AlertDialog(
            onDismissRequest = { forgetting = null },
            title = { Text("forget ${d.name}?") },
            text = { Text("you will need to pair it again") },
            confirmButton = { TextButton(onClick = { Link.forget(d.id); forgetting = null }) { Text("forget") } },
            dismissButton = { TextButton(onClick = { forgetting = null }) { Text("cancel") } },
        )
    }

    pairFor?.let { p ->
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { pairFor = null },
            title = { Text("pair with ${p.name}") },
            text = {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.filter(Char::isDigit).take(6) },
                    label = { Text("code shown on pc") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    textStyle = MonoStyle,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
            },
            confirmButton = {
                TextButton(onClick = { Link.connect(p, code); pairFor = null }, enabled = code.length == 6) { Text("pair") }
            },
            dismissButton = { TextButton(onClick = { pairFor = null }) { Text("cancel") } },
        )
    }
}
