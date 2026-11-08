package dev.proxylink.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.proxylink.core.Link
import dev.proxylink.core.LinkState
import dev.proxylink.core.Transfer

@Composable
fun AppButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) =
    Button(onClick = onClick, modifier = modifier, enabled = enabled, shape = MaterialTheme.shapes.small, content = content)

@Composable
fun AppOutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) =
    OutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled, shape = MaterialTheme.shapes.small, content = content)

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) { Column(Modifier.padding(16.dp), content = content) }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp).semantics { heading() },
    )
}

@Composable
fun ScreenHeader(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ProxyLogo(size = 32.dp)
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
    }
}

// settings page: back arrow + title + scrolling content
@Composable
fun SubScreen(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScrollCompat().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back") }
            Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        }
        content()
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String? = null, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onChecked).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
fun NavRow(title: String, icon: ImageVector, value: String? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (value != null) Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// choose where the next send goes: the pc or another device connected to it
@Composable
fun TargetPicker() {
    if (Link.others.isEmpty()) return
    Row(
        Modifier.horizontalScrollCompat(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(selected = Link.targetId == null, onClick = { Link.targetId = null }, label = { Text(Link.pcName.ifBlank { "pc" }) })
        Link.others.forEach { o ->
            FilterChip(selected = Link.targetId == o.id, onClick = { Link.targetId = o.id }, label = { Text(o.name) })
        }
    }
}

@Composable
fun SendButtons(actions: SendActions) {
    val enabled = Link.state == LinkState.CONNECTED
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SendButton("file", Icons.Default.AttachFile, enabled, actions.file, Modifier.weight(1f))
        SendButton("image", Icons.Default.Image, enabled, actions.image, Modifier.weight(1f))
        SendButton("clipboard", Icons.Default.ContentPaste, enabled, actions.clipboard, Modifier.weight(1f))
    }
}

@Composable
private fun SendButton(label: String, icon: ImageVector, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun TransferRow(t: Transfer) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text("${if (t.outgoing) "to" else "from"} ${t.who.ifBlank { "pc" }} · ${t.name}", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
        if (t.status == "active" || t.status == "waiting") {
            val f = if (t.size > 0) (t.bytes.toFloat() / t.size).coerceIn(0f, 1f) else 0f
            LinearProgressIndicator(progress = { f }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
        }
        Text(
            "${t.status} · ${fmtSize(t.bytes)} / ${fmtSize(t.size)}",
            style = MaterialTheme.typography.bodySmall,
            color = if (t.status == "failed") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

fun fmtSize(b: Long): String = when {
    b < 1024 -> "$b B"
    b < 1_048_576 -> "%.1f KB".format(b / 1024.0)
    b < 1_073_741_824 -> "%.1f MB".format(b / 1_048_576.0)
    else -> "%.2f GB".format(b / 1_073_741_824.0)
}
