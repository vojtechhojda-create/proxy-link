package dev.proxylink.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.proxylink.core.Link
import dev.proxylink.core.LinkState
import dev.proxylink.core.Peer
import dev.proxylink.ui.components.ItemRow
import dev.proxylink.ui.components.ProxyLogo

@Composable
private fun Panel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth(),
    ) { Column(Modifier.padding(18.dp)) { content() } }
}

@Composable
fun HomeScreen() {
    var pairFor by remember { mutableStateOf<Peer?>(null) }
    var sending by remember { mutableStateOf(false) }
    val connected = Link.state == LinkState.CONNECTED

    Column(
        Modifier.fillMaxSize().padding(20.dp),
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
                            LinkState.CONNECTED -> "${Link.pcName} connected"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        if (Link.pairError) "wrong code" else "lan",
                        color = if (Link.pairError) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (connected) TextButton(onClick = { Link.disconnect() }) { Text("disconnect") }
            }
        }

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
        }

        Panel(Modifier.weight(1f)) {
            Text("received", style = MaterialTheme.typography.titleMedium)
            val files = Link.received.filter { it.kind != "clip" }.take(5)
            if (files.isEmpty()) {
                Text("none yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(files) { ItemRow(it) }
                }
            }
        }

        Button(
            onClick = { sending = true },
            enabled = connected,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("send text or link") }
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
            title = { Text("send to ${Link.pcName}") },
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
                    onClick = { Link.sendText(text); sending = false },
                    enabled = text.isNotBlank(),
                ) { Text("send") }
            },
        )
    }
}
