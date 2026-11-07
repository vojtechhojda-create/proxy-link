package dev.proxylink.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.proxylink.core.Link
import dev.proxylink.core.LinkState
import dev.proxylink.core.Other
import dev.proxylink.core.Peer
import dev.proxylink.core.Transfer
import dev.proxylink.ui.components.ItemRow
import dev.proxylink.ui.components.ProxyLogo
import dev.proxylink.ui.components.fmtSize
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

@Composable
private fun Panel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth(),
    ) { Column(Modifier.padding(18.dp)) { content() } }
}

@Composable
private fun TransferRow(t: Transfer) {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Text(
            "${if (t.outgoing) "to" else "from"} ${t.who.ifBlank { "pc" }} · ${t.name}",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (t.status == "active" || t.status == "waiting") {
            val f = if (t.size > 0) (t.bytes.toFloat() / t.size).coerceIn(0f, 1f) else 0f
            LinearProgressIndicator(
                progress = { f },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )
        }
        Text(
            "${t.status} · ${fmtSize(t.bytes)} / ${fmtSize(t.size)}",
            style = MaterialTheme.typography.labelLarge,
            color = if (t.status == "failed") MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BatteryHint() {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    var hidden by remember { mutableStateOf(Link.prefs.hideBatteryHint) }
    val exempt = tick >= 0 && ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)
    val launch = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { tick++ }
    if (exempt || hidden) return
    Panel {
        Text("keep the connection alive", style = MaterialTheme.typography.titleMedium)
        Text(
            "allow proxylink to run in the background, otherwise the system may stop it when the screen is off",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.padding(top = 8.dp)) {
            Button(onClick = {
                launch.launch(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))
                )
            }) { Text("allow") }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = { Link.prefs.hideBatteryHint = true; hidden = true }) { Text("hide") }
        }
    }
}

@Composable
fun HomeScreen() {
    var pairFor by remember { mutableStateOf<Peer?>(null) }
    var sending by remember { mutableStateOf(false) }
    var target by remember { mutableStateOf<Other?>(null) }   // null = the pc
    val connected = Link.state == LinkState.CONNECTED
    val ctx = LocalContext.current
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { Link.sendFile(it, target) }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProxyLogo(size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Text("proxylink", style = MaterialTheme.typography.headlineMedium)
        }

        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        when (Link.state) {
                            LinkState.IDLE -> "not connected"
                            LinkState.CONNECTING -> "connecting to ${Link.pcName}"
                            LinkState.NEEDS_PAIR -> "pairing required"
                            LinkState.CONNECTED -> if (Link.guest) "guest session on ${Link.pcName}" else "${Link.pcName} connected"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        when {
                            Link.pairError -> "pairing failed or code expired"
                            Link.latency >= 0 -> "lan · ${Link.latency} ms"
                            else -> "lan"
                        },
                        color = if (Link.pairError) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (connected) TextButton(onClick = { Link.disconnect() }) { Text("disconnect") }
            }
        }

        BatteryHint()

        Panel {
            Text("devices on network", style = MaterialTheme.typography.titleMedium)
            if (Link.peers.isEmpty()) {
                Text("searching...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Link.peers.forEach { p ->
                val paired = Link.prefs.secret(p.id) != null
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (paired && Link.state != LinkState.NEEDS_PAIR) Link.connect(p) else pairFor = p
                        }
                        .padding(vertical = 10.dp),
                ) {
                    Text(p.name, Modifier.weight(1f))
                    Text(
                        if (paired) "paired" else "new",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.padding(top = 8.dp))
            OutlinedButton(
                onClick = {
                    val o = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
                    GmsBarcodeScanning.getClient(ctx, o).startScan()
                        .addOnSuccessListener { b ->
                            if (!Link.pairFromQr(b.rawValue.orEmpty())) {
                                Toast.makeText(ctx, "invalid qr code", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(ctx, "scanner unavailable", Toast.LENGTH_SHORT).show()
                        }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("scan qr to pair") }
        }

        if (connected && !Link.guest && Link.others.isNotEmpty()) {
            Panel {
                Text("other devices via pc", style = MaterialTheme.typography.titleMedium)
                Link.others.forEach { o ->
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(o.name, Modifier.weight(1f))
                        TextButton(onClick = { target = o; sending = true }) { Text("text") }
                        TextButton(onClick = { target = o; pick.launch("*/*") }) { Text("file") }
                    }
                }
            }
        }

        val recent = Link.transfers.values.sortedByDescending { it.ts }.take(4)
        if (recent.isNotEmpty()) {
            Panel {
                Text("transfers", style = MaterialTheme.typography.titleMedium)
                recent.forEach { TransferRow(it) }
            }
        }

        Panel {
            Text("received", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.padding(top = 8.dp))
            val items = Link.received.filter { it.kind != "clip" }.take(5)
            if (items.isEmpty()) {
                Text("none yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { ItemRow(it) }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { target = null; pick.launch("*/*") },
                enabled = connected,
                modifier = Modifier.weight(1f),
            ) { Text("send file") }
            OutlinedButton(
                onClick = { target = null; sending = true },
                enabled = connected,
                modifier = Modifier.weight(1f),
            ) { Text("send text") }
        }
    }

    // incoming request that needs approval (when auto-accept is off)
    Link.requests.firstOrNull()?.let { r ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("incoming from ${Link.pcName}") },
            text = {
                Text(if (r.kind == "file") "${r.name} (${fmtSize(r.size)})" else "${r.kind}: ${r.name}")
            },
            confirmButton = { TextButton(onClick = { Link.accept(r) }) { Text("accept") } },
            dismissButton = { TextButton(onClick = { Link.deny(r) }) { Text("deny") } },
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { Link.connect(p, code); pairFor = null },
                    enabled = code.length == 6,
                ) { Text("pair") }
            },
        )
    }

    if (sending) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { sending = false },
            title = { Text("send to ${target?.name ?: Link.pcName}") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("text or link") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { Link.sendText(text, target); sending = false },
                    enabled = text.isNotBlank(),
                ) { Text("send") }
            },
        )
    }
}
