package dev.proxylink.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.proxylink.R
import dev.proxylink.core.Capabilities
import dev.proxylink.core.Feature
import dev.proxylink.core.Link
import dev.proxylink.core.LinkState
import dev.proxylink.core.Transfer

// a short press feedback: the button shrinks a little while it is held. the system setting "remove animations" switches it off
@Composable
private fun Modifier.pressScale(source: MutableInteractionSource): Modifier {
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.97f else 1f, tween(90), label = "press")
    return graphicsLayer { scaleX = s; scaleY = s }
}

@Composable
fun AppButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    val src = remember { MutableInteractionSource() }
    Button(onClick = onClick, modifier = modifier.pressScale(src), enabled = enabled, shape = MaterialTheme.shapes.small, interactionSource = src, content = content)
}

@Composable
fun AppOutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    val src = remember { MutableInteractionSource() }
    OutlinedButton(onClick = onClick, modifier = modifier.pressScale(src), enabled = enabled, shape = MaterialTheme.shapes.small, interactionSource = src, content = content)
}

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
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
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }
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

// the target of what is sent from the app and the real state of the link to the pc; the chips above change the target.
// a device behind the pc is reached through the pc, so the line says "via pc" and claims nothing about that device itself
@Composable
fun TargetStatus() {
    val other = Link.others.firstOrNull { it.id == Link.targetId }?.name
    val pc = Link.pcName.ifBlank { Link.known.firstOrNull()?.name ?: stringResource(R.string.share_no_target) }
    val state = when (Link.state) {
        LinkState.CONNECTED ->
            if (Link.guest) stringResource(R.string.widget_guest)
            else if (Link.latency >= 0) stringResource(R.string.connected_ms, Link.latency.toInt()) else stringResource(R.string.connected)
        LinkState.CONNECTING -> stringResource(R.string.qa_state_connecting)
        LinkState.NEEDS_PAIR -> stringResource(R.string.nc_needs_pair)
        LinkState.IDLE -> stringResource(R.string.nc_not_connected)
    }
    val target = if (other != null) stringResource(R.string.qa_target_via, other, pc) else pc
    Text(
        stringResource(R.string.share_target_line, target) + " · " + state,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// a button is hidden when the user turned the feature off (the clipboard) and disabled, not hidden, when only the connection
// or the pc is missing something
@Composable
fun SendButtons(actions: SendActions) {
    // with a large system font the three buttons are stacked, so labels are not cut off
    if (LocalDensity.current.fontScale > 1.3f) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { SendButtonItems(actions, Modifier.fillMaxWidth()) }
    else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { SendButtonItems(actions, Modifier.weight(1f)) }
}

@Composable
private fun SendButtonItems(actions: SendActions, m: Modifier) {
    val connected = Link.state == LinkState.CONNECTED
    SendButton(stringResource(R.string.btn_file), Icons.Default.AttachFile, connected && Capabilities.supports(Feature.FILES), actions.file, m)
    SendButton(stringResource(R.string.btn_image), Icons.Default.Image, connected && Capabilities.supports(Feature.FILES), actions.image, m)
    if (Link.clipEnabled) {
        SendButton(
            stringResource(R.string.btn_clipboard), Icons.Default.ContentPaste,
            connected && !Link.guest && Capabilities.supports(Feature.CLIPBOARD), actions.clipboard, m,
        )
    }
}

// buttons side by side, stacked under each other when the system text size is large
@Composable
fun AdaptiveButtons(content: @Composable () -> Unit) {
    if (LocalDensity.current.fontScale > 1.3f) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
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

// a transfer line says only what is known: "done" for an outgoing file means the receiver confirmed it, "waiting" means the receiver
// has not accepted yet, and an interrupted link ends as "failed". nothing here is a guess about delivery
@Composable
fun TransferRow(t: Transfer) {
    val who = t.who.ifBlank { "pc" }
    val label = when (t.status) {
        "waiting" -> stringResource(R.string.tr_waiting)
        "active" -> stringResource(if (t.outgoing) R.string.tr_sending else R.string.tr_receiving)
        "done" -> stringResource(if (t.outgoing) R.string.tr_done_out else R.string.tr_done_in)
        "denied" -> stringResource(R.string.tr_denied)
        else -> stringResource(R.string.tr_failed)
    }
    val bad = t.status == "failed" || t.status == "denied"
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(
            stringResource(if (t.outgoing) R.string.tr_to else R.string.tr_from, who) + " · " + t.name,
            maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium,
        )
        if (t.status == "active" || t.status == "waiting") {
            val f = if (t.size > 0) (t.bytes.toFloat() / t.size).coerceIn(0f, 1f) else 0f
            val shown by animateFloatAsState(f, tween(150), label = "progress")
            LinearProgressIndicator(progress = { shown }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
        }
        Text(
            "$label · ${fmtSize(t.bytes)} / ${fmtSize(t.size)}",
            style = MaterialTheme.typography.bodySmall,
            color = if (bad) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

fun fmtSize(b: Long): String = when {
    b < 1024 -> "$b B"
    b < 1_048_576 -> "%.1f KB".format(b / 1024.0)
    b < 1_073_741_824 -> "%.1f MB".format(b / 1_048_576.0)
    else -> "%.2f GB".format(b / 1_073_741_824.0)
}
