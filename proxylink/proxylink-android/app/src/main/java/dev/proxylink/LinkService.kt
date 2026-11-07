package dev.proxylink

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.IBinder
import dev.proxylink.core.Link
import dev.proxylink.core.LinkState

// keeps the process (and the websocket) alive in the background
class LinkService : Service() {
    private var wifiLock: WifiManager.WifiLock? = null
    private val listener: (LinkState) -> Unit = { s -> refresh(); updateLock(s) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Link.init(this)
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CH, "connection", NotificationManager.IMPORTANCE_LOW))
        wifiLock = applicationContext.getSystemService(WifiManager::class.java)
            .createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "proxylink")
            .apply { setReferenceCounted(false) }
        Link.addListener(listener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(ID, build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        return START_STICKY
    }

    override fun onDestroy() {
        Link.removeListener(listener)
        wifiLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    // keep wi-fi awake only while a pc is connected
    private fun updateLock(s: LinkState) {
        val l = wifiLock ?: return
        if (s == LinkState.CONNECTED) { if (!l.isHeld) l.acquire() } else if (l.isHeld) l.release()
    }

    private fun refresh() {
        getSystemService(NotificationManager::class.java).notify(ID, build())
    }

    private fun build(): Notification {
        val text = if (Link.state == LinkState.CONNECTED) "connected to ${Link.pcName}" else "waiting for pc"
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CH)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("proxylink")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(open)
            .build()
    }

    private companion object {
        const val CH = "link"
        const val ID = 1
    }
}
