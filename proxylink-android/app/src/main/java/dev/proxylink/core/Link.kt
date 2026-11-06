package dev.proxylink.core

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

enum class LinkState { IDLE, CONNECTING, NEEDS_PAIR, CONNECTED }
data class Peer(val key: String, val id: String, val name: String, val host: String, val port: Int)
data class Item(val kind: String, val text: String, val from: String, val ts: Long = System.currentTimeMillis())

object Link {
    lateinit var prefs: Prefs
    private lateinit var app: Context
    private val main = Handler(Looper.getMainLooper())
    private val http = OkHttpClient.Builder().pingInterval(15, TimeUnit.SECONDS).build()

    private var ws: WebSocket? = null
    private var peer: Peer? = null
    private var code: String? = null
    private var nonce = ""
    private var pcId = ""
    private var clipHash = ""
    private val outgoing = HashMap<String, Item>()
    private val allowed = HashSet<String>()

    var state by mutableStateOf(LinkState.IDLE); private set
    var pairError by mutableStateOf(false); private set
    var pcName by mutableStateOf(""); private set
    var autoSync by mutableStateOf(true); private set
    val peers = mutableStateListOf<Peer>()
    val received = mutableStateListOf<Item>()
    val sent = mutableStateListOf<Item>()

    fun init(ctx: Context) {
        if (this::app.isInitialized) return
        app = ctx.applicationContext
        prefs = Prefs(app)
        autoSync = prefs.autoSync
        Discovery(app).start(
            onFound = { p -> main.post {
                peers.removeAll { it.key == p.key }
                peers.add(p)
                if (state == LinkState.IDLE && prefs.secret(p.id) != null) connect(p)
            } },
            onLost = { k -> main.post { peers.removeAll { it.key == k } } },
        )
    }

    fun setAutoSync(v: Boolean) { autoSync = v; prefs.autoSync = v }

    fun connect(p: Peer, pairCode: String? = null) {
        ws?.close(1000, null)
        peer = p; code = pairCode; pairError = false; pcName = p.name
        state = LinkState.CONNECTING
        ws = http.newWebSocket(Request.Builder().url("ws://${p.host}:${p.port}").build(), object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                main.post { if (webSocket === ws) runCatching { handle(JSONObject(text)) } }
            }
            override fun onClosed(webSocket: WebSocket, c: Int, r: String) { main.post { if (webSocket === ws) dropped() } }
            override fun onFailure(webSocket: WebSocket, t: Throwable, r: Response?) { main.post { if (webSocket === ws) dropped() } }
        })
    }

    fun disconnect() {
        peer = null
        ws?.close(1000, null); ws = null
        state = LinkState.IDLE
    }

    private fun dropped() {
        val p = peer ?: return
        if (state != LinkState.NEEDS_PAIR) state = LinkState.IDLE
        main.postDelayed({ if (state == LinkState.IDLE && peer == p) connect(p) }, 3000)
    }

    private fun tx(m: Map<String, Any>) { ws?.send(JSONObject(m).toString()) }

    private fun handle(m: JSONObject) {
        when (m.getString("t")) {
            "hello" -> {
                nonce = m.getString("nonce"); pcId = m.getString("id")
                val o = mutableMapOf<String, Any>("t" to "hello", "id" to prefs.id, "name" to prefs.name)
                prefs.secret(pcId)?.let { o["mac"] = hmac(it, nonce) }
                tx(o)
            }
            "ready" -> when {
                m.getBoolean("trusted") -> { state = LinkState.CONNECTED; pushClipboard() }
                code != null -> tx(mapOf("t" to "pair.req", "code" to code!!))
                else -> state = LinkState.NEEDS_PAIR
            }
            "pair.ok" -> {
                prefs.setSecret(pcId, m.getString("secret"))
                code = null; state = LinkState.CONNECTED; pushClipboard()
            }
            "pair.fail" -> { code = null; pairError = true; state = LinkState.NEEDS_PAIR }
            "share.request" -> {
                allowed.add(m.getString("sid"))
                tx(mapOf("t" to "share.accept", "sid" to m.getString("sid")))
            }
            "share.text" -> if (allowed.remove(m.getString("sid"))) {
                received.add(0, Item(m.getString("kind"), m.getString("text"), pcName))
            }
            "share.accept" -> outgoing.remove(m.getString("sid"))?.let {
                tx(mapOf("t" to "share.text", "sid" to m.getString("sid"), "kind" to it.kind, "text" to it.text))
                sent.add(0, it)
            }
            "share.deny" -> outgoing.remove(m.getString("sid"))
            "clip.update" -> if (autoSync) {
                val t = m.getString("text")
                if (sha(t) != clipHash) {
                    clipHash = sha(t)
                    app.getSystemService(ClipboardManager::class.java)
                        .setPrimaryClip(ClipData.newPlainText("proxylink", t))
                    received.add(0, Item("clip", t, pcName))
                }
            }
        }
    }

    fun sendText(text: String) {
        if (state != LinkState.CONNECTED || text.isBlank()) return
        val sid = UUID.randomUUID().toString()
        val kind = if (text.startsWith("http://") || text.startsWith("https://")) "link" else "text"
        outgoing[sid] = Item(kind, text, prefs.name)
        tx(mapOf("t" to "share.request", "sid" to sid, "kind" to kind, "name" to text.take(40), "size" to text.length))
    }

    // reading the clipboard only works while the app has focus (android 10+)
    fun pushClipboard(manual: Boolean = false) {
        if (state != LinkState.CONNECTED || (!manual && !autoSync)) return
        val cm = app.getSystemService(ClipboardManager::class.java)
        val clip = cm.primaryClip?.takeIf { it.itemCount > 0 } ?: return
        val t = clip.getItemAt(0).coerceToText(app)?.toString().orEmpty()
        if (t.isEmpty()) return
        val h = sha(t)
        if (!manual && h == clipHash) return
        clipHash = h
        tx(mapOf("t" to "clip.update", "ts" to System.currentTimeMillis(), "hash" to h, "text" to t.take(100_000)))
        sent.add(0, Item("clip", t, prefs.name))
    }

    private fun hex(b: ByteArray) = b.joinToString("") { "%02x".format(it) }
    private fun sha(s: String) = hex(MessageDigest.getInstance("SHA-256").digest(s.toByteArray()))
    private fun hmac(key: String, msg: String) = hex(
        Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(key.toByteArray(), "HmacSHA256")) }
            .doFinal(msg.toByteArray())
    )
}
