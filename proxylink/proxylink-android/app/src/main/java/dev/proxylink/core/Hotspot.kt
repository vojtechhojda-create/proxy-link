package dev.proxylink.core

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// local-only hotspot: a wi-fi network owned by the phone, no internet, no router needed.
// the pc joins it, then scans the pc qr code in the app.
object Hotspot {
    var ssid by mutableStateOf<String?>(null); private set
    var pass by mutableStateOf<String?>(null); private set
    var error by mutableStateOf<String?>(null); private set
    private var reservation: WifiManager.LocalOnlyHotspotReservation? = null

    fun fail(msg: String) { error = msg }

    @SuppressLint("MissingPermission")
    fun start(ctx: Context) {
        error = null
        val wm = ctx.applicationContext.getSystemService(WifiManager::class.java)
        try {
            wm.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                override fun onStarted(r: WifiManager.LocalOnlyHotspotReservation) {
                    reservation = r
                    val c = r.softApConfiguration
                    ssid = c.wifiSsid?.bytes?.decodeToString() ?: ""
                    pass = c.passphrase ?: ""
                }
                override fun onStopped() { ssid = null; pass = null; reservation = null }
                override fun onFailed(reason: Int) { error = "hotspot failed (code $reason)" }
            }, Handler(Looper.getMainLooper()))
        } catch (e: SecurityException) {
            error = "missing permission"
        } catch (e: Exception) {
            error = e.message ?: "hotspot failed"
        }
    }

    fun stop() {
        reservation?.close()
        reservation = null
        ssid = null
        pass = null
    }
}
