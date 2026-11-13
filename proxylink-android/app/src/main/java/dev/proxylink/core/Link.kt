package dev.proxylink.core

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.net.ConnectivityManager
import android.net.Network
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
    val status: String, // waiting | active | done | failed | denied | cancelled | timeout
    val ts: Long = System.currentTimeMillis(),
    val who: String = "",
)

private class OutFile(
    val name: String, val size: Long, val mime: String, val stream: InputStream, val who: String, val uri: String,
)

private class InFile(val req: ShareReq, val uri: Uri, val out: OutputStream, val name: String) {
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

    // outgoing transfers cancelled on this phone; the pump and a late "share.accept" look here
    private val cancelledSids = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<String, Boolean>())
    // an outgoing request nobody answers is dropped after this time (the file stream is not held forever)
    private const val WAIT_ANSWER_MS = 300_000L
    private val io = Executors.newSingleThreadExecutor()          // incoming writes, keeps order
    private val sender = Executors.newCachedThreadPool()          // outgoing file pumps
    private val listeners = CopyOnWriteArrayList<(LinkState) -> Unit>()

    @Volatile private var expectedFp = ""

    // proven mismatch of the app signature with the configured release fingerprint: no content is sent or received (see AppIdentity).
    // devices, history and settings stay as they are
    val transfersBlocked: Boolean get() = this::app.isInitialized && AppIdentity.state(app) == IdentityState.MISMATCH

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
    // user disconnected on purpose: no auto connect until they connect again. saved, so a restart does not undo it
    private var autoSuppressed = false
    private fun suppress(v: Boolean) {
        if (autoSuppressed == v) return
        autoSuppressed = v
        if (this::prefs.isInitialized) prefs.userDisconnected = v
    }
    private val pendingText = HashMap<String, Item>()
    private val pendingFiles = HashMap<String, OutFile>()
    private val meta = HashMap<String, ShareReq>()
    private val incoming = ConcurrentHashMap<String, InFile>()

    var state by mutableStateOf(LinkState.IDLE); private set
    var pairError by mutableStateOf(false); private set
    // id of the pc the link is connected to right now, null when not connected
    val connectedPcId: String? get() = if (state == LinkState.CONNECTED) pcId.ifEmpty { null } else null
    var guest by mutableStateOf(false); private set
    var pcName by mutableStateOf(""); private set
    var clipEnabled by mutableStateOf(true); private set
    var clipSend by mutableStateOf(true); private set
    var clipReceive by mutableStateOf(true); private set
    var clipPausedUntil by mutableStateOf(0L); private set
    var clipLastSent by mutableStateOf(0L); private set       // this session, 0 = nothing yet
    var clipLastReceived by mutableStateOf(0L); private set
    var latency by mutableStateOf(-1L); private set
    var lastAttemptId by mutableStateOf<String?>(null); private set   // device of the last attempt
    var retries by mutableStateOf(0); private set                    // failed attempts in a row, 0 after success, a manual retry or the network coming back
    var preferred by mutableStateOf<String?>(null); private set
    var lastAttempt by mutableStateOf(0L); private set                // time of the last connect attempt, 0 = none yet
    var lastError by mutableStateOf<String?>(null); private set       // why the last attempt or connection failed, no secrets
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

    // one retry timer for the whole link: a new attempt always cancels the pending one, so loops cannot pile up.
    // the address is taken from discovery each time, a paired device may have a new ip after the network came back
    private val retry = Runnable {
        val p = peer ?: return@Runnable
        if (state == LinkState.IDLE) connect(peers.firstOrNull { it.id == p.id } ?: p)
    }

    private fun describe(t: Throwable): String = when {
        generateSequence(t) { it.cause }.any { it is CertificateException } -> "certificate does not match the paired device"
        t is java.net.SocketTimeoutException -> "timeout"
        t is java.net.ConnectException || t is java.net.UnknownHostException || t is java.net.NoRouteToHostException -> "device not reachable"
        else -> "connection lost"
    }

    // searching for a paired device gives up after this long, so "searching" never lasts forever
    private val giveUp = Runnable {
        if (state == LinkState.IDLE && reconnecting != null) { reconnecting = null; notifyChanged() }
    }

    private fun notifyChanged() { listeners.forEach { it(state) } }

    fun addListener(l: (LinkState) -> Unit) { listeners.add(l) }
    fun removeListener(l: (LinkState) -> Unit) { listeners.remove(l) }

    @Synchronized
    fun init(ctx: Context) {
        if (this::app.isInitialized) return
        app = ctx.applicationContext
        prefs = Prefs(app)
        autoSuppressed = prefs.userDisconnected
        preferred = prefs.preferredId
        clipEnabled = prefs.clipEnabled
        clipSend = prefs.clipSend
        clipReceive = prefs.clipReceive
        clipPausedUntil = prefs.clipPausedUntil
        History.clipLimits = { prefs.clipHistoryMax to prefs.clipHistoryDays }
        History.init(app)
        Notifier.init(app)
        discovery = Discovery(app)
        refreshKnown()
        startDiscovery()
        main.postDelayed(pinger, 5000)
        // wi-fi came back or changed: browse again and try the last device once, instead of waiting for backoff timers
        runCatching { app.getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(netCallback) }
    }

    private val netCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            main.post { main.removeCallbacks(netBack); main.postDelayed(netBack, 1500) }   // debounce, onAvailable can fire in bursts
        }
    }
    private val netBack = Runnable {
        if (state != LinkState.IDLE) return@Runnable
        retries = 0
        peers.clear()   // addresses from before the network change are stale
        discovery.stop(); startDiscovery()
        if (peer != null && !guest && !autoSuppressed && prefs.autoConnect) { main.removeCallbacks(retry); main.post(retry) }
    }

    fun setPreferredDevice(id: String?) { preferred = id; prefs.preferredId = id }

    // auto connect goes only to the preferred device, if one is set and still paired
    private fun autoAllowed(id: String) = preferred.let { it == null || it == id || known.none { k -> k.id == it } }

    private fun startDiscovery() {
        discovery.start(
            onFound = { p -> main.post {
                peers.removeAll { it.key == p.key }
                peers.add(p)
                val paired = prefs.secret(p.id) != null
                val wanted = reconnecting == p.id || (prefs.autoConnect && !autoSuppressed && autoAllowed(p.id))
                if (state == LinkState.IDLE && paired && wanted) connect(p)
                // the device we are trying to reach announced a new address: switch to it instead of retrying the old one
                else if (state == LinkState.CONNECTING && paired && code == null && guestToken == null &&
                    peer?.id == p.id && peer?.host != p.host) connect(p)
            } },
            onLost = { k -> main.post { peers.removeAll { it.key == k } } },
        )
    }

    private fun refreshKnown() {
        known.clear()
        known.addAll(prefs.knownDevices().sortedByDescending { it.lastConnected })
        publishShortcuts()
    }

    fun changeClipEnabled(v: Boolean) { clipEnabled = v; prefs.clipEnabled = v; if (this::app.isInitialized) dev.proxylink.Widgets.updateAll(app) }
    fun changeClipSend(v: Boolean) { clipSend = v; prefs.clipSend = v }
    fun changeClipReceive(v: Boolean) { clipReceive = v; prefs.clipReceive = v }
    fun clipPaused(): Boolean = clipPausedUntil > System.currentTimeMillis()
    // ms = null: until the user resumes. manual sending is never blocked by the pause
    fun pauseClip(ms: Long?) {
        clipPausedUntil = if (ms == null) Long.MAX_VALUE else System.currentTimeMillis() + ms
        prefs.clipPausedUntil = clipPausedUntil
    }
    fun resumeClip() { clipPausedUntil = 0L; prefs.clipPausedUntil = 0L }
    private fun clipAuto(enabled: Boolean) = clipEnabled && enabled && !clipPaused()

    private fun go(s: LinkState) {
        state = s
        if (s != LinkState.CONNECTED) { latency = -1; others.clear(); targetId = null }
        if (s == LinkState.IDLE) { guest = false; Capabilities.reset() }
        if (s == LinkState.CONNECTED) { reconnecting = null; retries = 0; lastError = null; discovery.stop(); peers.clear() } else startDiscovery()
        listeners.forEach { it(s) }
        publishShortcuts()
        if (this::app.isInitialized) dev.proxylink.Widgets.updateAll(app)
    }

    // direct share targets follow the paired devices and the devices seen through the pc
    private fun publishShortcuts() { if (this::app.isInitialized) Shortcuts.publish(app, known.toList(), others.toList()) }

    fun connect(p: Peer, pairCode: String? = null, guestTok: String? = null) {
        suppress(false)
        main.removeCallbacks(retry)
        lastAttempt = System.currentTimeMillis()
        lastAttemptId = p.id
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
            override fun onClosed(webSocket: WebSocket, c: Int, r: String) {
                main.post { if (webSocket === ws) { lastError = "connection closed"; dropped() } }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, r: Response?) {
                main.post { if (webSocket === ws) { lastError = describe(t); dropped() } }
            }
        })
    }

    fun disconnect() {
        suppress(true)
        main.removeCallbacks(retry)
        retries = 0
        peer = null
        ws?.close(1000, null); ws = null
        abortTransfers()
        go(LinkState.IDLE)
    }

    // ---- paired devices: reconnect and forget ----

    // connect to the remembered device as soon as it is in range
    fun reconnect(id: String) {
        if (state == LinkState.CONNECTED || state == LinkState.CONNECTING) return
        suppress(false)
        retries = 0   // manual "try again" starts a fresh series
        val p = peers.firstOrNull { it.id == id }
        if (p != null) connect(p) else {
            reconnecting = id
            main.removeCallbacks(giveUp)
            main.postDelayed(giveUp, 30_000)
            notifyChanged()   // listeners (the tile) learn that we are searching, state itself is still IDLE
        }
    }

    fun cancelReconnect() {
        main.removeCallbacks(giveUp)
        if (reconnecting != null) { reconnecting = null; notifyChanged() }
    }

    fun forget(id: String) {
        val wasSuppressed = autoSuppressed
        if (peer?.id == id || pcId == id) disconnect()
        suppress(wasSuppressed)   // forgetting a device is not "turn everything off"
        if (reconnecting == id) reconnecting = null
        if (preferred == id) setPreferredDevice(null)
        prefs.forget(id)
        if (this::app.isInitialized) Shortcuts.remove(app, "pc:$id")   // a forgotten device must not stay in the share sheet
        refreshKnown()
    }

    fun connectPaired() {
        if (state == LinkState.CONNECTED || state == LinkState.CONNECTING) return
        val pref = preferred?.takeIf { id -> known.any { it.id == id } }
        val inRange = peers.firstOrNull { prefs.secret(it.id) != null && (pref == null || it.id == pref) }
        if (inRange != null) reconnect(inRange.id) else (known.firstOrNull { it.id == pref } ?: known.firstOrNull())?.let { reconnect(it.id) }
    }

    fun toggle() {
        when {
            state == LinkState.CONNECTED || state == LinkState.CONNECTING -> disconnect()
            reconnecting != null -> cancelReconnect()   // second tap while searching = stop searching
            else -> connectPaired()
        }
    }

    // qr / nfc payloads
    //   proxylink://pair?h=<ip>&p=<port>&c=<code>&f=<sha256>&i=<pc id>&n=<name>
    //   proxylink://guest?h=<ip>&p=<port>&t=<token>&f=<sha256>&n=<name>
    // a parsed qr / nfc link. nothing is connected until connectTo is called
    class LinkInfo(
        val kind: String, val host: String, val port: Int, val fp: String, val name: String,
        val id: String, val code: String?, val token: String?,
    )

    fun parseLink(raw: String): LinkInfo? {
        val u = runCatching { Uri.parse(raw) }.getOrNull() ?: return null
        if (u.scheme != "proxylink") return null
        val h = u.getQueryParameter("h") ?: return null
        val p = u.getQueryParameter("p")?.toIntOrNull() ?: return null
        val f = u.getQueryParameter("f") ?: return null
        val n = u.getQueryParameter("n") ?: "pc"
        if (!Regex("[0-9a-fA-F]{64}").matches(f)) return null
        if (!Regex("\\d{1,3}(\\.\\d{1,3}){3}").matches(h) || p !in 1..65535) return null
        return when (u.host) {
            "pair" -> {
                val c = u.getQueryParameter("c") ?: return null
                val id = u.getQueryParameter("i") ?: return null
                if (!Regex("\\d{6}").matches(c)) return null
                LinkInfo("pair", h, p, f.lowercase(), n, id, c, null)
            }
            "guest" -> {
                val t = u.getQueryParameter("t") ?: return null
                if (!Regex("[0-9a-f]{18}").matches(t)) return null
                LinkInfo("guest", h, p, f.lowercase(), n, "", null, t)
            }
            else -> null
        }
    }

    fun connectTo(i: LinkInfo) {
        if (i.kind == "pair") connect(Peer("qr-${i.id}", i.id, i.name, i.host, i.port, i.fp), i.code)
        else connect(Peer("guest-${i.host}", "", i.name, i.host, i.port, i.fp), null, i.token)
    }

    // the qr scanner: the user pointed the camera at the code on purpose
    fun pairFromQr(raw: String): Boolean {
        val i = parseLink(raw) ?: return false
        connectTo(i)
        return true
    }

    private const val MAX_RETRIES = 8

    private fun dropped() {
        abortTransfers()
        if (guest) {                 // guest tokens are one-time, never reconnect
            peer = null
            go(LinkState.IDLE)
            return
        }
        val p = peer ?: return
        if (state != LinkState.NEEDS_PAIR) go(LinkState.IDLE)
        // backoff 3, 6, 12, 24, 30 s ... and a limit. after that discovery (auto connect) or a manual retry takes over
        if (++retries > MAX_RETRIES) return
        main.removeCallbacks(retry)
        main.postDelayed(retry, minOf(3000L shl minOf(retries - 1, 4), 30_000L))
    }

    // version name of this installed app, as sent to the pc and shown in about
    val appVersion: String by lazy {
        runCatching { app.packageManager.getPackageInfo(app.packageName, 0).versionName }.getOrNull().orEmpty()
    }

    private fun tx(m: Map<String, Any>) { ws?.send(JSONObject(m).toString()) }

    private fun handle(m: JSONObject) {
        when (m.getString("t")) {
            "hello" -> {
                nonce = m.getString("nonce"); pcId = m.getString("id")
                helloName = m.optString("name").ifBlank { pcName }
                Capabilities.update(m.optInt("v", 0), m.optJSONArray("caps"), m.optString("app"), m.optInt("min", 0))
                // this app tells the pc its protocol, version and features; the pc of today ignores these fields
                val o = mutableMapOf<String, Any>(
                    "t" to "hello", "id" to prefs.id, "name" to prefs.name,
                    "v" to Capabilities.MOBILE_PROTOCOL, "app" to appVersion, "caps" to Capabilities.MOBILE_CAPS,
                )
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
                publishShortcuts()
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
                if (!valid || transfersBlocked) tx(mapOf("t" to "share.deny", "sid" to r.sid))
                else if (prefs.autoAccept && (r.kind != "file" || prefs.autoAcceptMaxMb == 0 || r.size <= prefs.autoAcceptMaxMb * 1_048_576L)) accept(r)
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
                val pf = pendingFiles.remove(sid)
                if (pf != null) startPump(sid, pf)
                // accepted after this phone cancelled or gave up: the pc already waits for data, so it is told the file is incomplete
                else if (cancelledSids.remove(sid)) tx(mapOf("t" to "share.done", "sid" to sid, "sha256" to "cancelled"))
            }
            "share.deny" -> {
                val sid = m.getString("sid")
                cancelledSids.remove(sid)
                pendingText.remove(sid)
                pendingFiles.remove(sid)?.let {
                    runCatching { it.stream.close() }
                    setTransfer(sid, it.name, it.size, 0, true, "denied", it.who)
                }
            }
            "share.ok" -> {
                val sid = m.getString("sid")
                val ok = m.optBoolean("ok")
                transfers[sid]?.takeIf { it.status != "cancelled" && it.status != "timeout" }?.let { t ->
                    transfers[sid] = t.copy(status = if (ok) "done" else "failed", bytes = if (ok) t.size else t.bytes)
                    if (ok) outMeta.remove(sid)?.let { (uri, mime) ->
                        History.add(Item("file", t.name, prefs.name, uri = uri, dir = "out", size = t.size, mime = mime))
                    }
                }
            }
            "clip.update" -> if (clipAuto(clipReceive) && !guest && !transfersBlocked) {
                val t = m.getString("text")
                if (sha(t) != clipHash) {
                    clipHash = sha(t)
                    app.getSystemService(ClipboardManager::class.java)
                        .setPrimaryClip(ClipData.newPlainText("proxylink", t))
                    clipLastReceived = System.currentTimeMillis()
                    if (prefs.clipHistory) History.add(Item("clip", t, pcName, dir = "in", size = t.length.toLong()))
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
                    // the system keeps both files when the name is taken ("name (1).jpg"); the history shows the name really saved
                    val saved = runCatching {
                        app.contentResolver.query(uri, arrayOf(MediaStore.Downloads.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
                    }.getOrNull() ?: safe(r.name)
                    incoming[r.sid] = InFile(r, uri, out, saved)
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
                "file", f.name, f.req.from.ifBlank { pcName },
                uri = f.uri.toString(), dir = "in", size = f.req.size, mime = f.req.mime,
            )
            History.add(item)
            Notifier.notifyReceived(app, item)
            // opened by itself only while the app is on screen (android does not let a background app open windows);
            // otherwise the notification opens the file
            if (prefs.openAfterReceive && foreground) runCatching {
                app.startActivity(
                    android.content.Intent(android.content.Intent.ACTION_VIEW)
                        .setDataAndType(f.uri, f.req.mime.ifBlank { "*/*" })
                        .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    // ---- outgoing (target = null means the pc, otherwise relayed by the pc to that device) ----

    fun sendText(text: String) {
        if (state != LinkState.CONNECTED || text.isBlank() || transfersBlocked) return
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
    fun sendFile(uri: Uri, nameOverride: String? = null, mimeOverride: String? = null): Boolean {
        if (state != LinkState.CONNECTED || transfersBlocked) return false
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
        nameOverride?.let { name = it }
        val stream = runCatching { cr.openInputStream(uri) }.getOrNull() ?: return false
        if (size < 0) size = runCatching { stream.available().toLong() }.getOrDefault(0L)
        val sid = UUID.randomUUID().toString()
        val mime = mimeOverride ?: cr.getType(uri) ?: "application/octet-stream"
        val who = target?.name ?: "pc"
        pendingFiles[sid] = OutFile(name, size, mime, stream, who, uri.toString())
        outMeta[sid] = uri.toString() to mime
        setTransfer(sid, name, size, 0, true, "waiting", who)
        val m = mutableMapOf<String, Any>(
            "t" to "share.request", "sid" to sid, "kind" to "file", "name" to name, "size" to size, "mime" to mime,
        )
        target?.let { m["to"] = it.id }
        tx(m)
        main.postDelayed({ if (transfers[sid]?.status == "waiting") cancelTransfer(sid, "timeout") }, WAIT_ANSWER_MS)
        return true
    }

    private val imageExec = java.util.concurrent.Executors.newSingleThreadExecutor()

    // "send image": the chosen quality applies here only. the work runs off the main thread, any failure sends the original
    fun sendImage(uri: Uri) {
        val q = ImageOptimizer.Quality.from(prefs.imageQuality)
        if (q == ImageOptimizer.Quality.ORIGINAL) { sendFile(uri); return }
        imageExec.execute {
            val r = ImageOptimizer.optimize(app, uri, q)
            main.post {
                if (r == null) sendFile(uri) else sendFile(Uri.fromFile(r.file), r.name, "image/jpeg")
            }
        }
    }

    // send a history entry again. false = the file is gone
    fun resend(item: Item): Boolean {
        if (state != LinkState.CONNECTED) return false
        return when (item.kind) {
            "file" -> item.uri?.let { u ->
                // a resized copy lives in the app cache as a file:// uri, there the name and type come from the history item
                val p = Uri.parse(u)
                val own = p.scheme == "file"
                runCatching { sendFile(p, if (own) item.text else null, if (own) item.mime else null) }.getOrDefault(false)
            } ?: false
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
            var cancelled = false
            try {
                f.stream.use { s ->
                    while (true) {
                        if (sid in cancelledSids) { cancelled = true; break }
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
            if (cancelled) {
                // no cancel message exists in the protocol: "done" with a checksum that cannot match makes the pc drop the partial file
                cancelledSids.remove(sid)
                sock.send(JSONObject(mapOf("t" to "share.done", "sid" to sid, "sha256" to "cancelled")).toString())
                return@execute
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
            // a late progress report must not bring a cancelled transfer back
            if (old != null && (old.status == "cancelled" || old.status == "timeout") && (status == "active" || status == "waiting")) return@post
            val t = Transfer(
                sid, name, size, bytes, outgoing, status,
                old?.ts ?: System.currentTimeMillis(),
                who.ifBlank { old?.who ?: "" },
            )
            transfers[sid] = t
            Notifier.transfer(app, t)
        }
    }

    // cancels one running or waiting transfer from this phone. a finished or already cancelled one is ignored, so an old
    // notification can never control another transfer. incoming: the file is deleted and the pc is told it failed (it may still
    // send the rest of the data, which is dropped). outgoing: see cancelledSids; a request still waiting for an answer is
    // dropped here and the pc is not told until it answers
    fun cancelTransfer(sid: String, reason: String = "cancelled") {
        val t = transfers[sid] ?: return
        if (t.status != "active" && t.status != "waiting") return
        if (t.outgoing) {
            if (!cancelledSids.add(sid)) return
            pendingFiles.remove(sid)?.let { runCatching { it.stream.close() } }
            pendingText.remove(sid)
            outMeta.remove(sid)
        } else {
            val f = incoming.remove(sid) ?: return
            io.execute {
                runCatching { f.out.close() }
                runCatching { app.contentResolver.delete(f.uri, null, null) }
            }
            tx(mapOf("t" to "share.ok", "sid" to sid, "ok" to false))
        }
        setTransfer(sid, t.name, t.size, t.bytes, t.outgoing, reason, t.who)
    }

    private fun abortTransfers() {
        requests.forEach { Notifier.cancel(app, it.sid) }
        cancelledSids.clear()
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
            if (t.status == "active" || t.status == "waiting") {
                val failed = t.copy(status = "failed")
                transfers[k] = failed
                Notifier.transfer(app, failed)
            }
        }
    }

    // reading the clipboard only works while the app has focus (android 10+)
    fun pushClipboard(manual: Boolean = false) { doPushClipboard(manual) }

    // manual send from a button: true only when something was really handed to the connection (not a delivery confirmation,
    // the pc does not acknowledge clipboard updates), false when the clipboard had nothing to send
    fun sendClipboardNow(): Boolean = doPushClipboard(true)

    private fun doPushClipboard(manual: Boolean): Boolean {
        if (!clipEnabled || state != LinkState.CONNECTED || guest || transfersBlocked || (!manual && !clipAuto(clipSend))) return false
        val cm = app.getSystemService(ClipboardManager::class.java)
        val clip = cm.primaryClip?.takeIf { it.itemCount > 0 } ?: return false
        // text the source app marked as sensitive (passwords, one time codes) is never sent automatically
        if (!manual && clip.description.extras?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE, false) == true) return false
        val first = clip.getItemAt(0)
        // an image in the clipboard is sent as a file, only when the user asks
        val imageUri = first.uri
        if (imageUri != null && clip.description.hasMimeType("image/*")) {
            if (manual) { sendFile(imageUri); return true }
            return false
        }
        val t = first.coerceToText(app)?.toString().orEmpty()
        if (t.isEmpty()) return false
        val h = sha(t)
        if (!manual && h == clipHash) return false
        clipHash = h
        tx(mapOf("t" to "clip.update", "ts" to System.currentTimeMillis(), "hash" to h, "text" to t.take(100_000)))
        clipLastSent = System.currentTimeMillis()
        if (prefs.clipHistory) History.add(Item("clip", t, prefs.name, dir = "out", size = t.length.toLong()))
        return true
    }

    private fun safe(n: String) = n.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(120).ifBlank { "file" }
    private fun hex(b: ByteArray) = b.joinToString("") { "%02x".format(it) }
    private fun sha(s: String) = hex(MessageDigest.getInstance("SHA-256").digest(s.toByteArray()))
    private fun hmac(key: String, msg: String) = hex(
        Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(key.toByteArray(), "HmacSHA256")) }
            .doFinal(msg.toByteArray())
    )
}
