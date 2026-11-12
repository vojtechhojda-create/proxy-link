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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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

// the screen may only say "hold the phones together" when two phones can really connect over nfc. today the app reads
// pairing tags only (no card emulation, no phone to phone transport), so the texts speak about a tag.
// change this together with the support in the app and the pc
private const val NFC_PHONE_TO_PHONE = false

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
    // connected for real: no extra screen, back to the standard UI (sending a file is a normal action there)
    LaunchedEffect(stage) {
        if (stage == NfcStage.CONNECTED) {
            Toast.makeText(ctx, ctx.getString(R.string.scan_connected, Link.pcName), Toast.LENGTH_SHORT).show()
            onClose()
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

    val info = found
    val ready = adapter != null && nfcOn
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader(stringResource(R.string.nfc_title))

        // the middle of the content area: a plain nfc icon, the main text under it, then the actions of the current stage
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScrollCompat(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            NfcPulse(active = ready && stage == NfcStage.READY)
            Spacer(Modifier.height(16.dp))
            Text(
                when {
                    adapter == null -> stringResource(R.string.nfc_unsupported)
                    !nfcOn -> stringResource(R.string.nfc_off)
                    else -> when (stage) {
                        NfcStage.READY -> stringResource(if (NFC_PHONE_TO_PHONE) R.string.nfc_ready_main else R.string.nfc_tag_main)
                        NfcStage.CONFIRM -> stringResource(R.string.pair_dialog_title, info?.name ?: "")
                        NfcStage.CONNECTING -> stringResource(R.string.scan_connecting, Link.pcName)
                        NfcStage.CONNECTED -> stringResource(R.string.scan_connected, Link.pcName)
                        NfcStage.FAILED -> stringResource(R.string.scan_failed)
                        NfcStage.TIMEOUT -> stringResource(R.string.nfc_timeout)
                        NfcStage.BAD_TAG -> stringResource(R.string.nfc_bad_tag)
                    }
                },
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            val sub = when {
                !ready -> null
                stage == NfcStage.READY -> stringResource(if (NFC_PHONE_TO_PHONE) R.string.nfc_ready_sub else R.string.nfc_tag_sub)
                stage == NfcStage.CONFIRM -> stringResource(R.string.nfc_confirm_hint, info?.host ?: "")
                stage == NfcStage.CONNECTING -> stringResource(R.string.nfc_remove_hint)
                else -> null
            }
            if (sub != null) {
                Text(
                    sub, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            if (adapter != null && !nfcOn) {
                AppButton(onClick = { ctx.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }) { Text(stringResource(R.string.nfc_turn_on)) }
            } else if (adapter != null) when (stage) {
                NfcStage.CONFIRM -> AdaptiveButtons {
                    AppButton(onClick = { found?.let { Link.connectTo(it) }; found = null; stage = NfcStage.CONNECTING }) { Text(stringResource(R.string.connect)) }
                    AppOutlinedButton(onClick = { found = null; stage = NfcStage.READY }) { Text(stringResource(R.string.cancel)) }
                }
                NfcStage.FAILED, NfcStage.TIMEOUT, NfcStage.BAD_TAG ->
                    AppButton(onClick = { stage = NfcStage.READY }) { Text(stringResource(R.string.try_again)) }
                else -> {}
            }
        }

        // cancel and the alternative are always reachable at the bottom
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (stage != NfcStage.CONNECTED) {
                AppOutlinedButton(onClick = onScan, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.nfc_use_qr)) }
            }
            AppOutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (stage == NfcStage.CONNECTED) R.string.done else R.string.cancel))
            }
            Text(
                (if (NFC_PHONE_TO_PHONE) "" else stringResource(R.string.nfc_scope) + " ") + stringResource(R.string.nfc_hotspot_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// a plain nfc icon on a round surface in the palette colors. two thin rings spread out softly while the screen waits.
// no motion when the system animations are off (animator duration scale 0) or when nothing is being waited for
@Composable
private fun NfcPulse(active: Boolean) {
    val ctx = LocalContext.current
    val systemAnimations = remember { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }
    val moving = active && systemAnimations
    val transition = rememberInfiniteTransition(label = "nfc")
    val phase by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "rings")
    val ring = MaterialTheme.colorScheme.primary
    Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        if (moving) {
            Canvas(Modifier.fillMaxSize()) {
                for (k in 0..1) {
                    val q = (phase + k * 0.5f) % 1f
                    drawCircle(ring.copy(alpha = 0.3f * (1f - q)), radius = size.minDimension / 2f * (0.58f + 0.42f * q), style = Stroke(width = 2.dp.toPx()))
                }
            }
        }
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(112.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Nfc, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(64.dp))
            }
        }
    }
}
