package dev.proxylink.core

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import okhttp3.OkHttpClient
import okhttp3.Request as HttpRequest
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

enum class LinkState { IDLE, CONNECTING, NEEDS_PAIR, CONNECTED }
data class Peer(val key: String, val id: String, val name: String, val host: String, val port: Int, val fp: String = "")
data class Other(val id: String, val name: String)   // another device connected to the same pc

// one history entry. dir = "in" received, "out" sent
data class Item(
    val kind: String,        // text | link | clip | file
    val text: String,
    val from: String,
    val ts: Long = System.currentTimeMillis(),
    val uri: String? = null,
    val id: String = UUID.randomUUID().toString(),
    val dir: String = "in",
    val size: Long = 0,
    val mime: String = "",
) {
    // filter type: text | link | file | image
    val type: String
        get() = when {
            kind == "file" && mime.startsWith("image/") -> "image"
            kind == "clip" -> "text"
            else -> kind
        }
}

data class ShareReq(
    val sid: String,
    val kind: String,
    val name: String,
    val size: Long,
    val mime: String,
    val from: String = "",   // set when relayed from another phone
)

data class Transfer(
    val sid: String,
    val name: String,
    val size: Long,
    val bytes: Long,
    val outgoing: Boolean,
    val status: String, // waiting | active | done | failed | denied
    val ts: Long = System.currentTimeMillis(),
    val who: String = "",
)

private class OutFile(
    val name: String, val size: Long, val mime: String, val stream: InputStream, val who: String, val uri: String,
)

private class InFile(val req: ShareReq, val uri: Uri, val out: OutputStream) {
    val digest: MessageDigest = MessageDigest.getInstance("SHA-256")
    var bytes = 0L
    var reported = 0L
}

object Link {
    private const val MAX_FILE = 4_000_000_000L

    lateinit var prefs: Prefs
    @Volatile var foreground = false   // set by MainActivity, notifications only while false
    private lateinit var app: Context
    private lateinit var discovery: Discovery
    private val main = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()          // incoming writes, keeps order
    private val sender = Executors.newCachedThreadPool()          // outgoing file pumps
    private val listeners = CopyOnWriteArrayList<(LinkState) -> Unit>()

    @Volatile private var expectedFp = ""

    // trust only the pc certificate whose sha-256 fingerprint we pinned (qr, mdns on first pairing, then stored)
    private val trust = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
            val fp = hex(MessageDigest.getInstance("SHA-256").digest(chain[0].encoded))
            if (expectedFp.isEmpty() || !fp.equals(expectedFp, ignoreCase = true)) {
                throw CertificateException("certificate pin mismatch")
            }
        }
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }
    private val http: OkHttpClient by lazy {
        val ctx = SSLContext.getInstance("TLS").apply { init(null, arrayOf<TrustManager>(trust), SecureRandom()) }
        OkHttpClient.Builder()
            .sslSocketFactory(ctx.socketFactory, trust)
            .hostnameVerifier { _, _ -> true }   // identity is checked by the pin
            .pingInterval(15, TimeUnit.SECONDS)
            .build()
    }

    @Volatile private var ws: WebSocket? = null
    private var peer: Peer? = null
    private var code: String? = null
    private var guestToken: String? = null
    private var nonce = ""
    private var pcId = ""
    private var helloName = ""
    private var clipHash = ""
    private var pingAt = 0L
    private var autoSuppressed = false      // user disconnected on purpose: no auto connect until they connect again
    private val pendingText = HashMap<String, Item>()
    private val pendingFiles = HashMap<String, OutFile>()
    private val meta = HashMap<String, ShareReq>()
    private val incoming = ConcurrentHashMap<String, InFile>()

    var state by mutableStateOf(LinkState.IDLE); private set
    var pairError by mutableStateOf(false); private set
    var guest by mutableStateOf(false); private set
    var pcName by mutableStateOf(""); private set
    var autoSync by mutableStateOf(true); private set
    var latency by mutableStateOf(-1L); private set
    var reconnecting by mutableStateOf<String?>(null); private set   // paired device id we are searching for
    var targetId by mutableStateOf<String?>(null)                    // null = the pc, otherwise another device via pc
    val peers = mutableStateListOf<Peer>()
    val others = mutableStateListOf<Other>()
    val known = mutableStateListOf<KnownDevice>()
    val requests = mutableStateListOf<ShareReq>()
    val transfers = mutableStateMapOf<String, Transfer>()

    val target: Other? get() = others.firstOrNull { it.id == targetId }

    private val pinger = object : Runnable {
        override fun run() {
            if (state == LinkState.CONNECTED) {
                pingAt = SystemClock.elapsedRealtime()
                tx(mapOf("t" to "ping"))
            }
            main.postDelayed(this, 5000)
        }
    }

    fun addListener(l: (LinkState) -> Unit) { listeners.add(l) }
    fun removeListener(l: (LinkState) -> Unit) { listeners.remove(l) }

    @Synchronized
    fun init(ctx: Context) {
        if (this::app.isInitialized) return
        app = ctx.applicationContext
        prefs = Prefs(app)
        autoSync = prefs.autoSync
        History.init(app)
        Notifier.init(app)
        discovery = Discovery(app)
        refreshKnown()
        startDiscovery()
        main.postDelayed(pinger, 5000)
    }

    private fun startDiscovery() {
        discovery.start(
            onFound = { p -> main.post {
                peers.removeAll { it.key == p.key }
                peers.add(p)
                val paired = prefs.secret(p.id) != null
                val wanted = reconnecting == p.id || (prefs.autoConnect && !autoSuppressed)
                if (state == LinkState.IDLE && paired && wanted) connect(p)
            } },
            onLost = { k -> main.post { peers.removeAll { it.key == k } } },
        )
    }

    private fun refreshKnown() {
        known.clear()
        known.addAll(prefs.knownDevices().sortedByDescending { it.lastConnected })
    }

    fun changeAutoSync(v: Boolean) { autoSync = v; prefs.autoSync = v }

    private fun go(s: LinkState) {
        state = s
        if (s != LinkState.CONNECTED) { latency = -1; others.clear(); targetId = null }
        if (s == LinkState.IDLE) guest = false
        if (s == LinkState.CONNECTED) { reconnecting = null; discovery.stop(); peers.clear() } else startDiscovery()
        listeners.forEach { it(s) }
        if (this::app.isInitialized) Shortcuts.publish(app, pcName, others.toList())
    }

    fun connect(p: Peer, pairCode: String? = null, guestTok: String? = null) {
        autoSuppressed = false
        ws?.close(1000, null)
        abortTransfers()
        peer = p; code = pairCode; guestToken = guestTok
        pairError = false; guest = false; pcName = p.name
        expectedFp = if (pairCode != null || guestTok != null) p.fp else (prefs.pin(p.id) ?: p.fp)
        go(LinkState.CONNECTING)
        ws = http.newWebSocket(HttpRequest.Builder().url("wss://${p.host}:${p.port}").build(), object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val m = runCatching { JSONObject(text) }.getOrNull() ?: return
                if (m.optString("t") == "share.done" && incoming.containsKey(m.optString("sid"))) {
                    io.execute { finishIncoming(m) }   // must run after all queued chunk writes
                } else {
                    main.post { if (webSocket === ws) runCatching { handle(m) } }
                }
            }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) { onChunk(bytes) }
            override fun onClosed(webSocket: WebSocket, c: Int, r: String) { main.post { if (webSocket === ws) dropped() } }
            override fun onFailure(webSocket: WebSocket, t: Throwable, r: Response?) { main.post { if (webSocket === ws) dropped() } }
        })
    }

    fun disconnect() {
        autoSuppressed = true
        peer = null
        ws?.close(1000, null); ws = null
        abortTransfers()
        go(LinkState.IDLE)
    }

    // ---- paired devices: reconnect and forget ----

    // connect to the remembered device as soon as it is in range
    fun reconnect(id: String) {
        if (state == LinkState.CONNECTED || state == LinkState.CONNECTING) return
        autoSuppressed = false
        val p = peers.firstOrNull { it.id == id }
        if (p != null) connect(p) else reconnecting = id
    }

    fun cancelReconnect() { reconnecting = null }

    fun forget(id: String) {
        if (peer?.id == id || pcId == id) disconnect()
        if (reconnecting == id) reconnecting = null
        prefs.forget(id)
        refreshKnown()
    }

    fun connectPaired() {
        if (state == LinkState.CONNECTED || state == LinkState.CONNECTING) return
        val inRange = peers.firstOrNull { prefs.secret(it.id) != null }
        if (inRange != null) reconnect(inRange.id) else known.firstOrNull()?.let { reconnect(it.id) }
    }

    fun toggle() {
        if (state == LinkState.CONNECTED || state == LinkState.CONNECTING) disconnect() else connectPaired()
    }

    // qr / nfc payloads
    //   proxylink://pair?h=<ip>&p=<port>&c=<code>&f=<sha256>&i=<pc id>&n=<name>
    //   proxylink://guest?h=<ip>&p=<port>&t=<token>&f=<sha256>&n=<name>
    fun pairFromQr(raw: String): Boolean {
        val u = runCatching { Uri.parse(raw) }.getOrNull() ?: return false
        if (u.scheme != "proxylink") return false
        val h = u.getQueryParameter("h") ?: return false
        val p = u.getQueryParameter("p")?.toIntOrNull() ?: return false
        val f = u.getQueryParameter("f") ?: return false
        val n = u.getQueryParameter("n") ?: "pc"
        if (!Regex("[0-9a-fA-F]{64}").matches(f)) return false
        if (!Regex("\\d{1,3}(\\.\\d{1,3}){3}").matches(h) || p !in 1..65535) return false
        return when (u.host) {
            "pair" -> {
                val c = u.getQueryParameter("c") ?: return false
                val id = u.getQueryParameter("i") ?: return false
                if (!Regex("\\d{6}").matches(c)) return false
                connect(Peer("qr-$id", id, n, h, p, f.lowercase()), c)
                true
            }
            "guest" -> {
                val t = u.getQueryParameter("t") ?: return false
                if (!Regex("[0-9a-f]{18}").matches(t)) return false
                connect(Peer("guest-$h", "", n, h, p, f.lowercase()), null, t)
                true
            }
            else -> false
        }
    }

    private fun dropped() {
        abortTransfers()
        if (guest) {                 // guest tokens are one-time, never reconnect
            peer = null
            go(LinkState.IDLE)
            return
        }
        val p = peer ?: return
        if (state != LinkState.NEEDS_PAIR) go(LinkState.IDLE)
        main.postDelayed({ if (state == LinkState.IDLE && peer == p) connect(p) }, 3000)
    }

    private fun tx(m: Map<String, Any>) { ws?.send(JSONObject(m).toString()) }

    private fun handle(m: JSONObject) {
        when (m.getString("t")) {
            "hello" -> {
                nonce = m.getString("nonce"); pcId = m.getString("id")
                helloName = m.optString("name").ifBlank { pcName }
                val o = mutableMapOf<String, Any>("t" to "hello", "id" to prefs.id, "name" to prefs.name)
                prefs.secret(pcId)?.let { o["mac"] = hmac(it, nonce) }
                tx(o)
            }
            "ready" -> when {
                m.getBoolean("trusted") -> { paired(); go(LinkState.CONNECTED); pushClipboard() }
                code != null -> tx(mapOf("t" to "pair.req", "code" to code!!))
                guestToken != null -> tx(mapOf("t" to "guest.req", "token" to guestToken!!))
                else -> go(LinkState.NEEDS_PAIR)
            }
            "pair.ok" -> {
                prefs.setSecret(pcId, m.getString("secret"))
                if (expectedFp.isNotEmpty()) prefs.setPin(pcId, expectedFp)
                code = null; paired(); go(LinkState.CONNECTED); pushClipboard()
            }
            "pair.fail" -> { code = null; pairError = true; go(LinkState.NEEDS_PAIR) }
            "guest.ok" -> { guestToken = null; guest = true; go(LinkState.CONNECTED) }
            "guest.fail" -> { guestToken = null; pairError = true; go(LinkState.NEEDS_PAIR) }
            "peers" -> {
                val arr = m.getJSONArray("list")
                others.clear()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    others.add(Other(o.getString("id"), o.getString("name")))
                }
                if (targetId != null && others.none { it.id == targetId }) targetId = null
                Shortcuts.publish(app, pcName, others.toList())
            }
            "pong" -> latency = SystemClock.elapsedRealtime() - pingAt
            "share.request" -> {
                val r = ShareReq(
                    m.getString("sid"), m.optString("kind"), m.optString("name"),
                    m.optLong("size"),
                    m.optString("mime", "application/octet-stream").ifBlank { "application/octet-stream" },
                    m.optString("from", ""),
                )
                val valid = r.sid.length == 36 && r.kind in listOf("text", "link", "file") && r.size in 0..MAX_FILE
                if (!valid) tx(mapOf("t" to "share.deny", "sid" to r.sid))
                else if (prefs.autoAccept) accept(r)
                else { requests.add(r); Notifier.request(app, r, pcName) }
            }
            "share.text" -> meta.remove(m.getString("sid"))?.let {
                val item = Item(
                    m.optString("kind", it.kind), m.getString("text"), it.from.ifBlank { pcName },
                    dir = "in", size = m.getString("text").length.toLong(),
                )
                History.add(item)
                Notifier.received(app, item)
            }
            "share.accept" -> {
                val sid = m.getString("sid")
                pendingText.remove(sid)?.let {
                    tx(mapOf("t" to "share.text", "sid" to sid, "kind" to it.kind, "text" to it.text))
                    History.add(it)
                }
                pendingFiles.remove(sid)?.let { startPump(sid, it) }
            }
            "share.deny" -> {
                val sid = m.getString("sid")
                pendingText.remove(sid)
                pendingFiles.remove(sid)?.let {
                    runCatching { it.stream.close() }
                    setTransfer(sid, it.name, it.size, 0, true, "denied", it.who)
                }
            }
            "share.ok" -> {
                val sid = m.getString("sid")
                val ok = m.optBoolean("ok")
                transfers[sid]?.let { t ->
                    transfers[sid] = t.copy(status = if (ok) "done" else "failed", bytes = if (ok) t.size else t.bytes)
                    if (ok) outMeta.remove(sid)?.let { (uri, mime) ->
                        History.add(Item("file", t.name, prefs.name, uri = uri, dir = "out", size = t.size, mime = mime))
                    }
                }
            }
            "clip.update" -> if (autoSync && !guest) {
                val t = m.getString("text")
                if (sha(t) != clipHash) {
                    clipHash = sha(t)
                    app.getSystemService(ClipboardManager::class.java)
                        .setPrimaryClip(ClipData.newPlainText("proxylink", t))
                    History.add(Item("clip", t, pcName, dir = "in", size = t.length.toLong()))
                }
            }
        }
    }

    // remembers the paired device (not guests)
    private fun paired() {
        if (guest || pcId.isEmpty()) return
        if (helloName.isNotBlank()) pcName = helloName
        prefs.remember(pcId, pcName)
        refreshKnown()
    }

    private val outMeta = HashMap<String, Pair<String, String>>()   // sid -> (uri, mime) of files sent

    // ---- incoming ----

    fun accept(r: ShareReq) {
        requests.remove(r)
        Notifier.cancel(app, r.sid)
        val who = r.from.ifBlank { "pc" }
        if (r.kind == "file") {
            io.execute {
                try {
                    val cv = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, safe(r.name))
                        put(MediaStore.Downloads.MIME_TYPE, r.mime)
                        put(MediaStore.Downloads.RELATIVE_PATH, "Download/proxylink")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val uri = app.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)!!
                    val out = app.contentResolver.openOutputStream(uri)!!
                    incoming[r.sid] = InFile(r, uri, out)
                    setTransfer(r.sid, r.name, r.size, 0, false, "active", who)
                    tx(mapOf("t" to "share.accept", "sid" to r.sid))
                } catch (e: Exception) {
                    tx(mapOf("t" to "share.deny", "sid" to r.sid))
                }
            }
        } else {
            meta[r.sid] = r
            tx(mapOf("t" to "share.accept", "sid" to r.sid))
        }
    }

    fun deny(r: ShareReq) {
        requests.remove(r)
        Notifier.cancel(app, r.sid)
        tx(mapOf("t" to "share.deny", "sid" to r.sid))
    }

    // binary frame = 36 ascii bytes sid + data
    private fun onChunk(b: ByteString) {
        if (b.size <= 36) return
        val f = incoming[b.substring(0, 36).utf8()] ?: return
        val data = b.substring(36).toByteArray()
        io.execute {
            runCatching { f.out.write(data); f.digest.update(data); f.bytes += data.size }
            if (f.bytes - f.reported >= 262_144 || f.bytes >= f.req.size) {
                f.reported = f.bytes
                setTransfer(f.req.sid, f.req.name, f.req.size, f.bytes, false, "active", f.req.from.ifBlank { "pc" })
            }
        }
    }

    private fun finishIncoming(m: JSONObject) {
        val sid = m.optString("sid")
        val f = incoming.remove(sid) ?: return
        var ok = false
        try {
            f.out.close()
            ok = hex(f.digest.digest()) == m.optString("sha256") && f.bytes == f.req.size
        } catch (_: Exception) {}
        val cr = app.contentResolver
        if (ok) cr.update(f.uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        else runCatching { cr.delete(f.uri, null, null) }
        tx(mapOf("t" to "share.ok", "sid" to sid, "ok" to ok))
        val who = f.req.from.ifBlank { "pc" }
        setTransfer(sid, f.req.name, f.req.size, f.bytes, false, if (ok) "done" else "failed", who)
        if (ok) main.post {
            val item = Item(
                "file", f.req.name, f.req.from.ifBlank { pcName },
                uri = f.uri.toString(), dir = "in", size = f.req.size, mime = f.req.mime,
            )
            History.add(item)
            Notifier.notifyReceived(app, item)
        }
    }

    // ---- outgoing (target = null means the pc, otherwise relayed by the pc to that device) ----

    fun sendText(text: String) {
        if (state != LinkState.CONNECTED || text.isBlank()) return
        val sid = UUID.randomUUID().toString()
        val kind = if (text.startsWith("http://") || text.startsWith("https://")) "link" else "text"
        pendingText[sid] = Item(kind, text, prefs.name, dir = "out", size = text.length.toLong())
        val m = mutableMapOf<String, Any>(
            "t" to "share.request", "sid" to sid, "kind" to kind, "name" to text.take(40), "size" to text.length,
        )
        target?.let { m["to"] = it.id }
        tx(m)
    }

    // opens the stream immediately so a share-sheet uri permission can expire afterwards. false = not sent
    fun sendFile(uri: Uri): Boolean {
        if (state != LinkState.CONNECTED) return false
        val cr = app.contentResolver
        var name = "file"
        var size = -1L
        runCatching {
            cr.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (ni >= 0) name = c.getString(ni) ?: name
                    val si = c.getColumnIndex(OpenableColumns.SIZE)
                    if (si >= 0 && !c.isNull(si)) size = c.getLong(si)
                }
            }
        }
        val stream = runCatching { cr.openInputStream(uri) }.getOrNull() ?: return false
        if (size < 0) size = runCatching { stream.available().toLong() }.getOrDefault(0L)
        val sid = UUID.randomUUID().toString()
        val mime = cr.getType(uri) ?: "application/octet-stream"
        val who = target?.name ?: "pc"
        pendingFiles[sid] = OutFile(name, size, mime, stream, who, uri.toString())
        outMeta[sid] = uri.toString() to mime
        setTransfer(sid, name, size, 0, true, "waiting", who)
        val m = mutableMapOf<String, Any>(
            "t" to "share.request", "sid" to sid, "kind" to "file", "name" to name, "size" to size, "mime" to mime,
        )
        target?.let { m["to"] = it.id }
        tx(m)
        return true
    }

    // send a history entry again. false = the file is gone
    fun resend(item: Item): Boolean {
        if (state != LinkState.CONNECTED) return false
        return when (item.kind) {
            "file" -> item.uri?.let { runCatching { sendFile(Uri.parse(it)) }.getOrDefault(false) } ?: false
            else -> { sendText(item.text); true }
        }
    }

    private fun startPump(sid: String, f: OutFile) {
        val sock = ws ?: return
        sender.execute {
            val digest = MessageDigest.getInstance("SHA-256")
            val sidB = sid.toByteArray(Charsets.US_ASCII)
            val buf = ByteArray(65_536)
            var sentBytes = 0L
            var reported = 0L
            var failed = false
            try {
                f.stream.use { s ->
                    while (true) {
                        val n = s.read(buf)
                        if (n <= 0) break
                        digest.update(buf, 0, n)
                        val frame = ByteArray(36 + n)
                        System.arraycopy(sidB, 0, frame, 0, 36)
                        System.arraycopy(buf, 0, frame, 36, n)
                        while (sock.queueSize() > 4_000_000L) Thread.sleep(15)
                        if (!sock.send(frame.toByteString())) { failed = true; break }
                        sentBytes += n
                        if (sentBytes - reported >= 262_144) {
                            reported = sentBytes
                            setTransfer(sid, f.name, f.size, sentBytes, true, "active", f.who)
                        }
                    }
                }
            } catch (_: Exception) {
                failed = true
            }
            if (failed) {
                setTransfer(sid, f.name, f.size, sentBytes, true, "failed", f.who)
            } else {
                setTransfer(sid, f.name, f.size, sentBytes, true, "active", f.who)
                sock.send(JSONObject(mapOf("t" to "share.done", "sid" to sid, "sha256" to hex(digest.digest()))).toString())
            }
        }
    }

    private fun setTransfer(
        sid: String, name: String, size: Long, bytes: Long, outgoing: Boolean, status: String, who: String = "",
    ) {
        main.post {
            val old = transfers[sid]
            transfers[sid] = Transfer(
                sid, name, size, bytes, outgoing, status,
                old?.ts ?: System.currentTimeMillis(),
                who.ifBlank { old?.who ?: "" },
            )
        }
    }

    private fun abortTransfers() {
        pendingFiles.values.forEach { runCatching { it.stream.close() } }
        pendingFiles.clear(); pendingText.clear(); meta.clear(); requests.clear(); outMeta.clear()
        val live = incoming.values.toList()
        incoming.clear()
        if (live.isNotEmpty()) io.execute {
            live.forEach {
                runCatching { it.out.close() }
                runCatching { app.contentResolver.delete(it.uri, null, null) }
            }
        }
        transfers.keys.toList().forEach { k ->
            val t = transfers[k] ?: return@forEach
            if (t.status == "active" || t.status == "waiting") transfers[k] = t.copy(status = "failed")
        }
    }

    // reading the clipboard only works while the app has focus (android 10+)
    fun pushClipboard(manual: Boolean = false) {
        if (state != LinkState.CONNECTED || guest || (!manual && !autoSync)) return
        val cm = app.getSystemService(ClipboardManager::class.java)
        val clip = cm.primaryClip?.takeIf { it.itemCount > 0 } ?: return
        val first = clip.getItemAt(0)
        // an image in the clipboard is sent as a file, only when the user asks
        val imageUri = first.uri
        if (imageUri != null && clip.description.hasMimeType("image/*")) {
            if (manual) sendFile(imageUri)
            return
        }
        val t = first.coerceToText(app)?.toString().orEmpty()
        if (t.isEmpty()) return
        val h = sha(t)
        if (!manual && h == clipHash) return
        clipHash = h
        tx(mapOf("t" to "clip.update", "ts" to System.currentTimeMillis(), "hash" to h, "text" to t.take(100_000)))
        History.add(Item("clip", t, prefs.name, dir = "out", size = t.length.toLong()))
    }

    private fun safe(n: String) = n.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(120).ifBlank { "file" }
    private fun hex(b: ByteArray) = b.joinToString("") { "%02x".format(it) }
    private fun sha(s: String) = hex(MessageDigest.getInstance("SHA-256").digest(s.toByteArray()))
    private fun hmac(key: String, msg: String) = hex(
        Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(key.toByteArray(), "HmacSHA256")) }
            .doFinal(msg.toByteArray())
    )
}
