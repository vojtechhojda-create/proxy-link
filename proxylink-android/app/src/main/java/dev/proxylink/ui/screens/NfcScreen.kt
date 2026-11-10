package dev.proxylink.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.proxylink.R
import dev.proxylink.core.Link
import dev.proxylink.core.LinkState
import dev.proxylink.ui.components.*
import kotlinx.coroutines.delay

// nfc only starts the pairing: the phone reads a proxylink:// pairing tag (the same payload as the qr code).
// nothing connects before the user confirms, files are sent afterwards over the normal network link
private enum class NfcStage { READY, CONFIRM, CONNECTING, CONNECTED, FAILED, TIMEOUT, BAD_TAG }

private const val NFC_WAIT_MS = 60_000L

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun readLink(tag: Tag): Link.LinkInfo? {
    val ndef = Ndef.get(tag) ?: return null
    val msg = ndef.cachedNdefMessage ?: runCatching { ndef.use { it.connect(); it.ndefMessage } }.getOrNull()
    val uri = msg?.records?.firstNotNullOfOrNull { it.toUri()?.toString() } ?: return null
    return Link.parseLink(uri)
}

@Composable
fun NfcScreen(onClose: () -> Unit, onScan: () -> Unit) {
    val ctx = LocalContext.current
    val adapter = remember { NfcAdapter.getDefaultAdapter(ctx) }
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }   // the user may come back from the nfc system settings
    val nfcOn = tick >= 0 && adapter?.isEnabled == true
    var stage by remember { mutableStateOf(NfcStage.READY) }
    var found by remember { mutableStateOf<Link.LinkInfo?>(null) }
    val actions = rememberSendActions()
    val activity = remember { ctx.findActivity() }

    // reader mode runs only while the screen waits for a tag, and is always switched off again
    DisposableEffect(adapter, nfcOn, stage, activity) {
        if (adapter != null && nfcOn && activity != null && stage == NfcStage.READY) {
            adapter.enableReaderMode(
                activity,
                { tag ->
                    val info = readLink(tag)
                    Handler(Looper.getMainLooper()).post {
                        if (stage == NfcStage.READY) {
                            if (info != null) { found = info; stage = NfcStage.CONFIRM } else stage = NfcStage.BAD_TAG
                        }
                    }
                },
                NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V,
                null,
            )
        }
        onDispose { if (adapter != null && activity != null) runCatching { adapter.disableReaderMode(activity) } }
    }
    // the session times out, it does not wait for a tag forever
    LaunchedEffect(stage, nfcOn) {
        if (stage == NfcStage.READY && nfcOn) {
            delay(NFC_WAIT_MS)
            if (stage == NfcStage.READY) stage = NfcStage.TIMEOUT
        }
    }
    // the result comes from the real link state, not from reading the tag
    LaunchedEffect(stage, Link.state) {
        if (stage == NfcStage.CONNECTING) when (Link.state) {
            LinkState.CONNECTED -> stage = NfcStage.CONNECTED
            LinkState.IDLE, LinkState.NEEDS_PAIR -> stage = NfcStage.FAILED
            else -> {}
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScrollCompat().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(stringResource(R.string.nfc_title))
        Panel {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Icon(Icons.Default.Nfc, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(8.dp))
            val info = found
            Text(
                when {
                    adapter == null -> stringResource(R.string.nfc_unsupported)
                    !nfcOn -> stringResource(R.string.nfc_off)
                    else -> when (stage) {
                        NfcStage.READY -> stringResource(R.string.nfc_ready)
                        NfcStage.CONFIRM -> stringResource(R.string.pair_dialog_title, info?.name ?: "")
                        NfcStage.CONNECTING -> stringResource(R.string.scan_connecting, Link.pcName)
                        NfcStage.CONNECTED -> stringResource(R.string.scan_connected, Link.pcName)
                        NfcStage.FAILED -> stringResource(R.string.scan_failed)
                        NfcStage.TIMEOUT -> stringResource(R.string.nfc_timeout)
                        NfcStage.BAD_TAG -> stringResource(R.string.nfc_bad_tag)
                    }
                },
                style = MaterialTheme.typography.titleMedium,
            )
            val sub = when {
                adapter == null || !nfcOn -> null
                stage == NfcStage.CONFIRM -> stringResource(R.string.nfc_confirm_hint, info?.host ?: "")
                stage == NfcStage.CONNECTING -> stringResource(R.string.nfc_remove_hint)
                stage == NfcStage.CONNECTED -> stringResource(R.string.scan_connected_hint)
                else -> null
            }
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (adapter != null && !nfcOn) {
            AppButton(onClick = { ctx.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.nfc_turn_on))
            }
        } else if (adapter != null) when (stage) {
            NfcStage.CONFIRM -> {
                AppButton(
                    onClick = { found?.let { Link.connectTo(it) }; found = null; stage = NfcStage.CONNECTING },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.connect)) }
                AppOutlinedButton(onClick = { found = null; stage = NfcStage.READY }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.cancel))
                }
            }
            NfcStage.CONNECTED -> {
                AppButton(onClick = actions.file, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.send_file)) }
            }
            NfcStage.FAILED, NfcStage.TIMEOUT, NfcStage.BAD_TAG -> {
                AppButton(onClick = { stage = NfcStage.READY }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.try_again)) }
            }
            else -> {}
        }

        if (stage != NfcStage.CONNECTED) {
            AppOutlinedButton(onClick = onScan, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.nfc_use_qr)) }
        }
        AppOutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (stage == NfcStage.CONNECTED) R.string.done else R.string.close))
        }

        Text(
            stringResource(R.string.nfc_scope) + " " + stringResource(R.string.nfc_hotspot_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
