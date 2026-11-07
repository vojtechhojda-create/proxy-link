package dev.proxylink.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.text.format.Formatter
import dev.proxylink.ActionReceiver
import dev.proxylink.MainActivity

// notifications for incoming content, only shown while the app is not in the foreground
object Notifier {
    private const val CH_IN = "incoming"
    private const val CH_REQ = "requests"
    private var counter = 1000

    fun init(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_IN, "received items", NotificationManager.IMPORTANCE_DEFAULT))
        nm.createNotificationChannel(NotificationChannel(CH_REQ, "incoming requests", NotificationManager.IMPORTANCE_HIGH))
    }

    private fun openApp(ctx: Context) =
        PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

    // request that needs approval, with accept / deny buttons
    fun request(ctx: Context, r: ShareReq, pc: String) {
        if (Link.foreground) return
        val who = r.from.ifBlank { pc.ifBlank { "pc" } }
        val what = if (r.kind == "file") "a file" else r.kind
        val detail = if (r.kind == "file") "${r.name} (${Formatter.formatShortFileSize(ctx, r.size)})" else r.name
        fun act(a: String, n: Int) = PendingIntent.getBroadcast(
            ctx,
            r.sid.hashCode() * 2 + n,
            Intent(ctx, ActionReceiver::class.java).setAction(a).putExtra("sid", r.sid),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val icon = Icon.createWithResource(ctx, android.R.drawable.ic_menu_send)
        val n = Notification.Builder(ctx, CH_REQ)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("$who wants to send $what")
            .setContentText(detail)
            .setContentIntent(openApp(ctx))
            .setAutoCancel(true)
            .addAction(Notification.Action.Builder(icon, "accept", act("accept", 0)).build())
            .addAction(Notification.Action.Builder(icon, "deny", act("deny", 1)).build())
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(r.sid.hashCode(), n)
    }

    // something arrived (auto accepted or accepted by the user)
    fun received(ctx: Context, item: Item) {
        if (Link.foreground || item.kind == "clip") return
        val id = counter++
        val tap = when {
            item.uri != null -> {
                val uri = Uri.parse(item.uri)
                PendingIntent.getActivity(
                    ctx, id,
                    Intent(Intent.ACTION_VIEW)
                        .setDataAndType(uri, ctx.contentResolver.getType(uri) ?: "*/*")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_IMMUTABLE,
                )
            }
            item.kind == "link" && (item.text.startsWith("http://") || item.text.startsWith("https://")) ->
                PendingIntent.getActivity(
                    ctx, id,
                    Intent(Intent.ACTION_VIEW, Uri.parse(item.text)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_IMMUTABLE,
                )
            else -> openApp(ctx)
        }
        val title = when (item.kind) {
            "file" -> "file received from ${item.from}"
            "link" -> "link from ${item.from}"
            else -> "text from ${item.from}"
        }
        val n = Notification.Builder(ctx, CH_IN)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(item.text)
            .setStyle(Notification.BigTextStyle().bigText(item.text))
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(id, n)
    }

    fun cancel(ctx: Context, sid: String) {
        ctx.getSystemService(NotificationManager::class.java).cancel(sid.hashCode())
    }
}
