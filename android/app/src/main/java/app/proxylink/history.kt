package app.proxylink

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class entry(
    val id: Long,
    val time: Long,
    val incoming: Boolean,
    val kind: String,
    val title: String,
    val data: String,
    val mime: String,
    val size: Long,
)

object history {
    private val _items = MutableStateFlow<List<entry>>(emptyList())
    val items: StateFlow<List<entry>> = _items.asStateFlow()
    private var loaded = false

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences("proxylink", Context.MODE_PRIVATE)

    @Synchronized
    fun load(ctx: Context) {
        if (loaded) return
        loaded = true
        val raw = prefs(ctx).getString("history", null) ?: return
        runCatching {
            val a = JSONArray(raw)
            _items.value = (0 until a.length()).mapNotNull { i ->
                a.optJSONObject(i)?.let { o ->
                    entry(
                        o.optLong("id"), o.optLong("time"), o.optBoolean("in"), o.optString("kind"),
                        o.optString("title"), o.optString("data"), o.optString("mime"), o.optLong("size"),
                    )
                }
            }
        }
    }

    @Synchronized
    fun add(ctx: Context, e: entry) {
        load(ctx)
        val clipped = if (e.kind == "text") e.copy(data = e.data.take(2000)) else e
        _items.value = (listOf(clipped) + _items.value).take(60)
        save(ctx)
    }

    @Synchronized
    fun clear(ctx: Context) {
        _items.value = emptyList()
        prefs(ctx).edit().remove("history").apply()
    }

    private fun save(ctx: Context) {
        val a = JSONArray()
        _items.value.forEach {
            a.put(
                JSONObject().put("id", it.id).put("time", it.time).put("in", it.incoming).put("kind", it.kind)
                    .put("title", it.title).put("data", it.data).put("mime", it.mime).put("size", it.size)
            )
        }
        prefs(ctx).edit().putString("history", a.toString()).apply()
    }
}
