package dev.proxylink

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.proxylink.core.Prefs

// starts the background service after a reboot or an app update
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        if (!Prefs(ctx).startOnBoot) return
        ctx.startForegroundService(Intent(ctx, LinkService::class.java))
    }
}
