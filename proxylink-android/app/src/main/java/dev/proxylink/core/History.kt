package dev.proxylink.core

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors

// local history of sent and received items, a json file in the app storage. nothing leaves the device.
object History {
    private const val MAX = 10_000
    val items = mutableStateListOf<Item>()
    private val io = Executors.newSingleThreadExecutor()
    private lateinit var file: File

    // (max items, days) of the clipboard history, read each time so a changed setting applies at once. 0 = no limit
    var clipLimits: () -> Pair<Int, Int> = { 0 to 0 }

    @Synchronized
    fun init(ctx: Context) {
        if (this::file.isInitialized) return
        file = File(ctx.filesDir, "history.json")
        runCatching {
            if (file.exists()) {
                val arr = JSONArray(file.readText())
                items.addAll((0 until arr.length()).map { fromJson(arr.getJSONObject(it)) })
            }
        }
        if (pruneClips()) save()
    }

    fun add(i: Item) {
        items.add(0, i)
        if (items.size > MAX) items.removeRange(MAX, items.size)
        if (i.kind == "clip") pruneClips()
        save()
    }

    // called by the settings after the limits changed
    fun enforceClipLimits() { if (pruneClips()) save() }

    // clipboard items older than the retention or beyond the newest N are deleted. evaluated at app start, when a clipboard item
    // is added and when the limits change, not by a timer, so an item can outlive its time while the app sits idle
    private fun pruneClips(): Boolean {
        val (max, days) = clipLimits()
        val oldest = if (days > 0) System.currentTimeMillis() - days * 86_400_000L else Long.MIN_VALUE
        var kept = 0
        val before = items.size
        items.removeAll { it.kind == "clip" && (it.ts < oldest || (max > 0 && ++kept > max)) }
        return items.size != before
    }

    fun remove(id: String) {
        items.removeAll { it.id == id }
        save()
    }

    // clipboard items only, files stay
    fun clearClips() {
        items.removeAll { it.kind == "clip" }
        save()
    }

    fun clear() {
        items.clear()
        save()
    }

    private fun save() {
        val snapshot = items.toList()
        io.execute {
            runCatching {
                val arr = JSONArray()
                snapshot.forEach { arr.put(toJson(it)) }
                file.writeText(arr.toString())
            }
        }
    }

    private fun toJson(i: Item) = JSONObject()
        .put("id", i.id).put("dir", i.dir).put("kind", i.kind).put("text", i.text)
        .put("from", i.from).put("ts", i.ts).put("size", i.size).put("mime", i.mime)
        .also { o -> i.uri?.let { o.put("uri", it) } }

    private fun fromJson(o: JSONObject) = Item(
        kind = o.getString("kind"),
        text = o.getString("text"),
        from = o.optString("from"),
        ts = o.optLong("ts"),
        uri = if (o.has("uri")) o.getString("uri") else null,
        id = o.getString("id"),
        dir = o.optString("dir", "in"),
        size = o.optLong("size"),
        mime = o.optString("mime"),
    )
}
