package dev.proxylink.core

import android.content.Context
import android.os.Build
import java.util.UUID

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("proxylink", Context.MODE_PRIVATE)

    val id: String = sp.getString("id", null)
        ?: UUID.randomUUID().toString().also { sp.edit().putString("id", it).apply() }

    var name: String
        get() = sp.getString("name", null) ?: Build.MODEL
        set(v) = sp.edit().putString("name", v).apply()

    var palette: String
        get() = sp.getString("palette", null) ?: "navy"
        set(v) = sp.edit().putString("palette", v).apply()

    var autoSync: Boolean
        get() = sp.getBoolean("autosync", true)
        set(v) = sp.edit().putBoolean("autosync", v).apply()

    fun secret(peerId: String): String? = sp.getString("s_$peerId", null)
    fun setSecret(peerId: String, s: String) = sp.edit().putString("s_$peerId", s).apply()
}
