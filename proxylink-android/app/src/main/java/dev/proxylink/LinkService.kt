package dev.proxylink

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.net.wifi.WifiManager
import android.os.IBinder
import dev.proxylink.core.Link
import dev.proxylink.core.LinkState
import dev.proxylink.core.Notifier

// keeps the process (and the websocket) alive in the background
class LinkService : Service() {
    private var wifiLock: WifiManager.WifiLock? = null
    private val listener: (LinkState) -> Unit = { s -> refresh(); updateLock(s) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        Link.init(this)
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CH, getString(R.string.ch_link), NotificationManager.IMPORTANCE_LOW))
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
        running = false
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

    // the text follows the real link state; "disconnect" is offered only while there is a connection to end
    private fun build(): Notification {
        val text = when (Link.state) {
            LinkState.CONNECTED -> getString(if (Link.guest) R.string.svc_guest else R.string.svc_connected, Link.pcName)
            LinkState.CONNECTING -> getString(R.string.svc_connecting)
            else -> getString(R.string.svc_waiting)
        }
        val b = Notification.Builder(this, CH)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(Notifier.openApp(this))
        if (Link.state == LinkState.CONNECTED) {
            b.addAction(Notification.Action.Builder(Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel), getString(R.string.disconnect), Notifier.action(this, Notifier.ACTION_DISCONNECT, null, 3)).build())
        }
        return b.build()
    }

    companion object {
        @Volatile var running = false   // for diagnostics only
            private set
        private const val CH = "link"
        private const val ID = 1
    }
}
