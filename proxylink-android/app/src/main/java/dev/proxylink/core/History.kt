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
    }

    fun add(i: Item) {
        items.add(0, i)
        if (items.size > MAX) items.removeRange(MAX, items.size)
        save()
    }

    fun remove(id: String) {
        items.removeAll { it.id == id }
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
