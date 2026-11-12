package dev.proxylink

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.proxylink.core.Link
import dev.proxylink.core.Notifier

// buttons of the notifications. every action looks the target up first: a request or transfer that is already handled,
// finished or cancelled is not found, so an old notification does nothing (and never controls another transfer)
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Link.init(ctx)
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (intent.action == Notifier.ACTION_DISCONNECT) { Link.disconnect(); return }
        val sid = intent.getStringExtra("sid") ?: return
        when (intent.action) {
            Notifier.ACTION_ACCEPT -> Link.requests.firstOrNull { it.sid == sid }?.let { Link.accept(it) }
            Notifier.ACTION_DENY -> Link.requests.firstOrNull { it.sid == sid }?.let { Link.deny(it) }
            Notifier.ACTION_CANCEL -> Link.cancelTransfer(sid)
        }
        nm.cancel(sid.hashCode())
    }
}
