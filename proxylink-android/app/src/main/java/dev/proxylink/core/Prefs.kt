package dev.proxylink.core

import android.content.Context
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class KnownDevice(val id: String, val name: String, val lastConnected: Long)

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("proxylink", Context.MODE_PRIVATE)

    init { migrate() }

    // brings values stored by older versions to the current layout. nothing is deleted, old keys stay (a downgrade keeps working)
    private fun migrate() {
        if (sp.getInt("schema", 1) >= SCHEMA) return
        val e = sp.edit()
        // 1 -> 2: clipboard send / receive get their own stored value (they followed the old "autosync" switch),
        // users who already kept the clipboard history keep it without the new limits, an unknown image quality goes back to original
        val auto = sp.getBoolean("autosync", true)
        if (!sp.contains("clipsend")) e.putBoolean("clipsend", auto)
        if (!sp.contains("cliprecv")) e.putBoolean("cliprecv", auto)
        if (sp.getBoolean("cliphist", false)) { e.putInt("clipmax", 0); e.putInt("clipdays", 0) }
        if (sp.getString("imgq", null) !in listOf<String?>(null, "original", "high", "economy")) e.remove("imgq")
        e.putInt("schema", SCHEMA).apply()
    }

    val id: String = sp.getString("id", null)
        ?: UUID.randomUUID().toString().also { sp.edit().putString("id", it).apply() }

    var name: String
        get() = sp.getString("name", null) ?: Build.MODEL
        set(v) = sp.edit().putString("name", v).apply()

    // appearance
    var palette: String
        get() = sp.getString("palette", null) ?: "brand"
        set(v) = sp.edit().putString("palette", v).apply()

    // true = launcher icon follows the selected palette (colored), false = original brand icon
    var paletteIcon: Boolean
        get() = sp.getBoolean("paletteicon", false)
        set(v) = sp.edit().putBoolean("paletteicon", v).apply()

    // main ui font: default, inter, plex, atkinson or system (see ui/theme/Type.kt)
    var font: String
        get() = sp.getString("font", null) ?: "default"
        set(v) = sp.edit().putString("font", v).apply()

    // corner style of the whole ui: square, default or round (see ui/theme/Shape.kt)
    var shape: String
        get() = sp.getString("shape", null) ?: "default"
        set(v) = sp.edit().putString("shape", v).apply()

    // notifications show a generic text on the lock screen. the system lock screen setting can still allow full content
    var notifHideContent: Boolean
        get() = sp.getBoolean("notifhide", true)
        set(v) = sp.edit().putBoolean("notifhide", v).apply()

    var dynamicColor: Boolean
        get() = sp.getBoolean("dynamic", false)
        set(v) = sp.edit().putBoolean("dynamic", v).apply()

    // quality of images sent with "send image": original, high or economy. files are always sent as they are
    var imageQuality: String
        get() = sp.getString("imgq", "original") ?: "original"
        set(v) = sp.edit().putString("imgq", v).apply()

    // the clipboard feature as a whole. off = no automatic sync, no send button anywhere, nothing recorded; stored data stays
    var clipEnabled: Boolean
        get() = sp.getBoolean("clipon", true)
        set(v) = sp.edit().putBoolean("clipon", v).apply()

    // clipboard. send and receive are separate; the old single "autosync" value is the default for both
    var clipSend: Boolean
        get() = sp.getBoolean("clipsend", sp.getBoolean("autosync", true))
        set(v) = sp.edit().putBoolean("clipsend", v).apply()

    var clipReceive: Boolean
        get() = sp.getBoolean("cliprecv", sp.getBoolean("autosync", true))
        set(v) = sp.edit().putBoolean("cliprecv", v).apply()

    // automatic clipboard sync is paused until this time (epoch ms), Long.MAX_VALUE = until the user resumes, 0 = not paused
    var clipPausedUntil: Long
        get() = sp.getLong("clippause", 0L)
        set(v) = sp.edit().putLong("clippause", v).apply()

    // clipboard text in the history, off by default
    var clipHistory: Boolean
        get() = sp.getBoolean("cliphist", false)
        set(v) = sp.edit().putBoolean("cliphist", v).apply()

    // limits of the clipboard history: newest N items (0 = no limit) and days to keep them (0 = keep). files are not affected
    var clipHistoryMax: Int
        get() = sp.getInt("clipmax", 100)
        set(v) = sp.edit().putInt("clipmax", v).apply()

    var clipHistoryDays: Int
        get() = sp.getInt("clipdays", 30)
        set(v) = sp.edit().putInt("clipdays", v).apply()

    // open a received file right away while the app is on screen (off by default)
    var openAfterReceive: Boolean
        get() = sp.getBoolean("openrecv", false)
        set(v) = sp.edit().putBoolean("openrecv", v).apply()

    // with "accept automatically" on, files bigger than this many MB still ask first (0 = no limit)
    var autoAcceptMaxMb: Int
        get() = sp.getInt("acceptmax", 0)
        set(v) = sp.edit().putInt("acceptmax", v).apply()

    // true = accept requests from the paired pc without asking
    var autoAccept: Boolean
        get() = sp.getBoolean("autoaccept", true)
        set(v) = sp.edit().putBoolean("autoaccept", v).apply()

    // the user disconnected on purpose: no auto connect, also after the app or the phone restarts, until they connect again
    var userDisconnected: Boolean
        get() = sp.getBoolean("userdisc", false)
        set(v) = sp.edit().putBoolean("userdisc", v).apply()

    // with a preferred device auto connect and the tile go to it, other paired devices connect manually
    var preferredId: String?
        get() = sp.getString("preferred", null)
        set(v) = sp.edit().apply { if (v == null) remove("preferred") else putString("preferred", v) }.apply()

    // true = connect to a paired device as soon as it is in range
    var autoConnect: Boolean
        get() = sp.getBoolean("autoconnect", true)
        set(v) = sp.edit().putBoolean("autoconnect", v).apply()

    var startOnBoot: Boolean
        get() = sp.getBoolean("boot", true)
        set(v) = sp.edit().putBoolean("boot", v).apply()

    var haptics: Boolean
        get() = sp.getBoolean("haptics", true)
        set(v) = sp.edit().putBoolean("haptics", v).apply()

    // pinned sha-256 fingerprint of the pc certificate
    fun pin(peerId: String): String? = sp.getString("p_$peerId", null)
    fun setPin(peerId: String, fp: String) = sp.edit().putString("p_$peerId", fp).apply()

    fun secret(peerId: String): String? = sp.getString("s_$peerId", null)
    fun setSecret(peerId: String, s: String) = sp.edit().putString("s_$peerId", s).apply()

    // paired devices the phone remembers
    fun knownDevices(): List<KnownDevice> {
        val arr = runCatching { JSONArray(sp.getString("known", "[]")) }.getOrDefault(JSONArray())
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            KnownDevice(o.getString("id"), o.getString("name"), o.optLong("last"))
        }
    }

    companion object { const val SCHEMA = 2 }

    fun remember(id: String, name: String) {
        val list = knownDevices().filter { it.id != id } + KnownDevice(id, name, System.currentTimeMillis())
        saveKnown(list)
    }

    // forget = remove the secret, the pin and the entry, only paired devices are listed
    fun forget(id: String) {
        saveKnown(knownDevices().filter { it.id != id })
        sp.edit().remove("s_$id").remove("p_$id").apply()
    }

    private fun saveKnown(list: List<KnownDevice>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("id", it.id).put("name", it.name).put("last", it.lastConnected)) }
        sp.edit().putString("known", arr.toString()).apply()
    }
}
