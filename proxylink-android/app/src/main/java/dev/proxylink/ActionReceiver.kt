package dev.proxylink

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.proxylink.core.Link

// accept / deny buttons of the request notification
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Link.init(ctx)
        val sid = intent.getStringExtra("sid") ?: return
        val r = Link.requests.firstOrNull { it.sid == sid }
        when (intent.action) {
            "accept" -> r?.let { Link.accept(it) }
            "deny" -> r?.let { Link.deny(it) }
        }
        ctx.getSystemService(NotificationManager::class.java).cancel(sid.hashCode())
    }
}
