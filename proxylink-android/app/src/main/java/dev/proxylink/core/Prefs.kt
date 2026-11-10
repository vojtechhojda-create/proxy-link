package dev.proxylink.core

import android.content.Context
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class KnownDevice(val id: String, val name: String, val lastConnected: Long)

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("proxylink", Context.MODE_PRIVATE)

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

    var dynamicColor: Boolean
        get() = sp.getBoolean("dynamic", false)
        set(v) = sp.edit().putBoolean("dynamic", v).apply()

    // behavior
    var autoSync: Boolean
        get() = sp.getBoolean("autosync", true)
        set(v) = sp.edit().putBoolean("autosync", v).apply()

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
