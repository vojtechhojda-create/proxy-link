package app.proxylink

import android.os.Build
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun card(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
fun appheader(orb: Color, pulsing: Boolean, subtitle: String, badges: List<String> = emptyList()) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        statusorb(orb, pulsing, Modifier.size(56.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("proxylink", style = MaterialTheme.typography.titleLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (badges.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    badges.forEach { t ->
                        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(
                                t,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun label(t: String) =
    Text(t, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
fun audiotab(model: mainmodel, onuplink: (String) -> Unit) {
    val audioon by model.audioon.collectAsState()
    val uplink by link.uplink.collectAsState()
    val apps by link.apps.collectAsState()
    val source by model.source.collectAsState()
    val outputs by model.outputs.collectAsState()
    val outkey by model.outkey.collectAsState()
    val buf by model.bufmult.collectAsState()
    val err by link.error.collectAsState()
    var v by remember(buf) { mutableFloatStateOf(buf.toFloat()) }

    LaunchedEffect(Unit) { model.refreshapps() }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        card("pc audio to this phone") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                abutton(if (audioon) "stop" else "start", model::toggleaudio, kind = if (audioon) 1 else 0)
                AnimatedVisibility(audioon) { equalizer(colok) }
            }
            label("source")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = source == 0, onClick = { model.setsource(0) }, label = { Text("all pc audio") })
                apps.forEach { a ->
                    FilterChip(
                        selected = source == a.pid,
                        onClick = { model.setsource(a.pid) },
                        label = { Text(a.name + if (a.active) " *" else "") },
                    )
                }
                AssistChip(onClick = { model.refreshapps() }, label = { Text("refresh") })
            }
            label("play on phone through")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = outkey.isEmpty(), onClick = { model.setoutput("") }, label = { Text("auto") })
                outputs.forEach { o ->
                    FilterChip(selected = outkey == o.key, onClick = { model.setoutput(o.key) }, label = { Text(o.label) })
                }
            }
            label("buffer x${v.roundToInt()} (higher = more stable, more delay)")
            Slider(
                value = v,
                onValueChange = { v = it },
                onValueChangeFinished = { model.setbuf(v.roundToInt()) },
                valueRange = 1f..8f,
                steps = 6,
            )
            err?.let { Text(it, color = colerr) }
        }

        card("this phone to pc") {
            label("plays on the pc speakers")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                abutton(
                    if (uplink == "mic") "stop mic" else "phone mic",
                    { if (uplink == "mic") link.uplinkstop() else onuplink("mic") },
                    kind = if (uplink == "mic") 1 else 2,
                )
                if (Build.VERSION.SDK_INT >= 29) {
                    abutton(
                        if (uplink == "phone") "stop audio" else "phone audio",
                        { if (uplink == "phone") link.uplinkstop() else onuplink("phone") },
                        kind = if (uplink == "phone") 1 else 2,
                    )
                }
            }
            AnimatedVisibility(uplink != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    equalizer(colok)
                    Text("sending to pc")
                }
            }
        }
    }
}

private fun sizeof(n: Long): String = when {
    n >= 1L shl 30 -> "%.1f GB".format(n / 1073741824.0)
    n >= 1L shl 20 -> "%.1f MB".format(n / 1048576.0)
    n >= 1L shl 10 -> "%.0f KB".format(n / 1024.0)
    else -> "$n B"
}

private fun clock(t: Long): String =
    java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(t))

@Composable
fun transfercard() {
    val list by transfers.list.collectAsState()
    if (list.isEmpty()) return
    card("transfers") {
        list.take(6).forEach { t ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        (if (t.incoming) "from pc  " else "to pc  ") + t.name,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    val label = when {
                        t.state != "running" -> t.state
                        t.size > 0 -> "${(t.done * 100 / t.size).coerceIn(0, 100)} %"
                        else -> sizeof(t.done)
                    }
                    Text(label, style = MaterialTheme.typography.labelMedium, color = if (t.state == "failed") colerr else MaterialTheme.colorScheme.onSurfaceVariant)
                    if (t.state == "running" && !t.incoming) {
                        TextButton(onClick = { transfers.cancel(t.id) }) { Text("cancel") }
                    }
                }
                if (t.state == "running") {
                    if (t.size > 0) LinearProgressIndicator(progress = { (t.done.toFloat() / t.size).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
        if (list.any { it.state != "running" }) {
            TextButton(onClick = { transfers.clearfinished() }) { Text("clear finished") }
        }
    }
}

@Composable
fun controltab(model: mainmodel) {
    val ctx = LocalContext.current
    var text by remember { mutableStateOf("") }

    fun sendall(uris: List<Uri>) {
        if (uris.isEmpty()) return
        link.scope.launch { uris.forEach { link.sharefile(ctx.applicationContext, it) } }
    }

    val photos = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { sendall(it) }
    val files = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { sendall(it) }

    val paste: () -> Unit = {
        val clip = ctx.getSystemService(ClipboardManager::class.java).primaryClip
        val first = if (clip != null && clip.itemCount > 0) clip.getItemAt(0) else null
        val uri = first?.uri
        if (uri != null) {
            sendall(listOf(uri))
        } else {
            val t = first?.coerceToText(ctx)?.toString().orEmpty()
            if (t.isNotBlank()) link.sharetext(ctx, t) else link.note("clipboard is empty")
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        card("pc media") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                roundbutton(R.drawable.ic_prev, "previous", { model.media("prev") })
                roundbutton(R.drawable.ic_play, "play or pause", { model.media("play_pause") }, big = true)
                roundbutton(R.drawable.ic_next, "next", { model.media("next") })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
                roundbutton(R.drawable.ic_vol_down, "volume down", { model.media("vol_down") })
                roundbutton(R.drawable.ic_vol_off, "mute", { model.media("mute") })
                roundbutton(R.drawable.ic_nav_audio, "volume up", { model.media("vol_up") })
            }
        }
        card("send to pc") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                iconbutton("photos", R.drawable.ic_image, {
                    photos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                }, Modifier.weight(1f))
                iconbutton("files", R.drawable.ic_file, { files.launch("*/*") }, Modifier.weight(1f))
                iconbutton("paste", R.drawable.ic_paste, paste, Modifier.weight(1f))
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("text or link") },
                minLines = 2,
            )
            abutton("send text", {
                if (text.isNotBlank()) {
                    link.sharetext(ctx, text)
                    text = ""
                }
            })
        }
        transfercard()
    }
}

@Composable
private fun entryrow(e: entry) {
    val ctx = LocalContext.current
    val kindicon = when (e.kind) {
        "link" -> R.drawable.ic_link
        "image" -> R.drawable.ic_image
        "file" -> R.drawable.ic_file
        else -> R.drawable.ic_text
    }
    val host = if (e.kind == "link") runCatching { Uri.parse(e.data).host }.getOrNull().orEmpty() else ""
    val meta = buildString {
        append(if (e.incoming) "from pc" else "to pc")
        append("  ").append(clock(e.time))
        if ((e.kind == "file" || e.kind == "image") && e.size > 0) append("  ").append(sizeof(e.size))
        if (host.isNotEmpty()) append("  ").append(host)
    }
    val copy = {
        ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("proxylink", e.data))
    }

    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(kindicon), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                if (e.kind == "image" && e.data.isNotEmpty()) thumb(e.data, Modifier.fillMaxSize())
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(e.title.ifBlank { e.data }, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                Text(meta, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row {
                if (e.kind == "link") {
                    IconButton(onClick = {
                        runCatching {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(e.data)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    }) { Icon(painterResource(R.drawable.ic_open), contentDescription = "open") }
                }
                if ((e.kind == "file" || e.kind == "image") && e.incoming && e.data.isNotEmpty()) {
                    IconButton(onClick = {
                        runCatching {
                            ctx.startActivity(
                                Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(e.data), e.mime.ifEmpty { "*/*" })
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            )
                        }
                    }) { Icon(painterResource(R.drawable.ic_open), contentDescription = "open") }
                }
                if (e.kind == "link" || e.kind == "text") {
                    IconButton(onClick = { copy() }) { Icon(painterResource(R.drawable.ic_copy), contentDescription = "copy") }
                }
            }
        }
    }
}

@Composable
fun inboxtab() {
    val ctx = LocalContext.current
    val items by history.items.collectAsState()
    LaunchedEffect(Unit) { history.load(ctx) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        transfercard()
        if (items.isEmpty()) {
            card("inbox") {
                Text(
                    "nothing here yet. links, text, photos and files you send or receive will show up in this list.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items.forEach { entryrow(it) }
            abutton("clear history", { history.clear(ctx) }, kind = 2)
        }
    }
}

@Composable
private fun twocodecard() {
    val ws by link.twocode.collectAsState()
    val out by link.twout.collectAsState()
    val running by link.twrun.collectAsState()
    LaunchedEffect(Unit) { link.twrefresh() }
    card("twocode workspaces") {
        if (ws.isEmpty()) {
            Text(
                "no workspaces. on the pc create %APPDATA%\\proxylink\\twocode.json (see twocode.example.json) and restart proxylink.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ws.forEach { w ->
            Text(
                buildString {
                    append(w.name)
                    if (w.branch.isNotEmpty()) append("  ").append(w.branch)
                    if (w.dirty > 0) append("  (").append(w.dirty).append(" changed)")
                },
                style = MaterialTheme.typography.titleSmall,
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                w.tasks.forEach { t ->
                    AssistChip(onClick = { if (running == null) link.twstart(w.id, t) }, label = { Text(t) }, enabled = running == null)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            abutton("refresh", { link.twrefresh() }, kind = 1)
            if (running != null) abutton("stop task", { link.twstop() }, kind = 2)
        }
        if (out.isNotEmpty()) {
            Text(out, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun toolstab(model: mainmodel) {
    val log by link.log.collectAsState()
    val state by link.state.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        card("connection") {
            Text(if (state is linkstate.paired) "connected to ${(state as linkstate.paired).pcname}" else "not connected")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                abutton("disconnect", model::disconnect, kind = 1)
                abutton("forget pc", model::forget, kind = 2)
            }
        }
        twocodecard()
        card("dev tools") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                abutton("ping", { link.ping() }, kind = 1)
                abutton("echo", { link.chan("echo", "hello") }, kind = 1)
            }
            Text(
                log.takeLast(10).joinToString("\n").ifEmpty { "no events yet" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
