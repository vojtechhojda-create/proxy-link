package dev.proxylink.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.proxylink.ui.components.ProxyLogo

private data class ProFeature(val title: String, val description: String)

private val features = listOf(
    ProFeature("unlimited history", "keep every received item instead of only the recent ones"),
    ProFeature("custom themes", "design your own color palette"),
    ProFeature("home screen widget", "send your clipboard to your pc with one tap"),
    ProFeature("quick settings tile", "connect and disconnect from the notification shade"),
    ProFeature("file previews", "see a preview of images and documents before you accept them"),
    ProFeature("transfer resume", "continue an interrupted file transfer where it stopped"),
)

// placeholder: no purchase flow. "continue free" is always visible, also while the details are open.
@Composable
fun UpgradeScreen(onContinueFree: () -> Unit) {
    var details by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))
            ProxyLogo(size = 72.dp)
            Spacer(Modifier.height(16.dp))
            Text("proxylink pro", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "unlock extra features, keep the core free",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    features.forEach { FeatureRow(it, details) }
                }
            }

            AnimatedVisibility(visible = details) {
                Text(
                    "pro is not available to buy yet. nothing in the free app is locked or removed.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }

        Button(onClick = onContinueFree, modifier = Modifier.fillMaxWidth()) { Text("continue free") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { details = !details }, modifier = Modifier.fillMaxWidth()) {
            Text(if (details) "hide pro features" else "see pro features")
        }
    }
}

@Composable
private fun FeatureRow(f: ProFeature, showDescription: Boolean) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(top = 2.dp).size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(f.title, style = MaterialTheme.typography.titleMedium)
            AnimatedVisibility(visible = showDescription) {
                Text(f.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
