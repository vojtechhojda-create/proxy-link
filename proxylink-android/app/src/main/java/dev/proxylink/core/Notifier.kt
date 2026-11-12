package dev.proxylink.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.SystemClock
import android.text.format.Formatter
import dev.proxylink.ActionReceiver
import dev.proxylink.MainActivity
import dev.proxylink.R
import java.util.concurrent.ConcurrentHashMap

// one place for every notification of the app (the connection notification of LinkService uses the helpers here too).
// shown only while the app is not in the foreground. progress never rings again: it lives on a silent channel and updates in place
object Notifier {
    private const val CH_IN = "incoming"
    private const val CH_REQ = "requests"
    private const val CH_TRANSFER = "transfers"
    const val ACTION_ACCEPT = "accept"
    const val ACTION_DENY = "deny"
    const val ACTION_CANCEL = "cancel"
    const val ACTION_DISCONNECT = "disconnect"
    private var counter = 1000
    private val lastShown = ConcurrentHashMap<String, Long>()

    fun init(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_IN, ctx.getString(R.string.ch_received), NotificationManager.IMPORTANCE_DEFAULT))
        nm.createNotificationChannel(NotificationChannel(CH_REQ, ctx.getString(R.string.ch_requests), NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel(CH_TRANSFER, ctx.getString(R.string.ch_transfers), NotificationManager.IMPORTANCE_LOW))
    }

    // tapping opens the app (single top, so no second window); the app has no screen per transfer to jump to
    fun openApp(ctx: Context) = PendingIntent.getActivity(
        ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE,
    )

    fun action(ctx: Context, action: String, sid: String?, n: Int): PendingIntent = PendingIntent.getBroadcast(
        ctx,
        (sid?.hashCode() ?: 0) * 4 + n,
        Intent(ctx, ActionReceiver::class.java).setAction(action).also { i -> if (sid != null) i.putExtra("sid", sid) },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun icon(ctx: Context) = Icon.createWithResource(ctx, android.R.drawable.ic_menu_send)

    // lock screen: with "hide content" (default) the lock screen gets a generic text. the system setting for notification content
    // on the lock screen can still show everything, the app cannot force it off
    fun secure(ctx: Context, b: Notification.Builder, channel: String, generic: String): Notification.Builder {
        if (Link.prefs.notifHideContent) {
            b.setVisibility(Notification.VISIBILITY_PRIVATE)
            b.setPublicVersion(
                Notification.Builder(ctx, channel).setSmallIcon(android.R.drawable.stat_notify_sync)
                    .setContentTitle(ctx.getString(R.string.app_name)).setContentText(generic).build(),
            )
        } else b.setVisibility(Notification.VISIBILITY_PUBLIC)
        return b
    }

    // request that needs approval, with accept / deny buttons
    fun request(ctx: Context, r: ShareReq, pc: String) {
        if (Link.foreground) return
        val who = r.from.ifBlank { pc.ifBlank { "pc" } }
        val detail = if (r.kind == "file") "${r.name} (${Formatter.formatShortFileSize(ctx, r.size)})" else r.name
        val title = ctx.getString(if (r.kind == "file") R.string.n_wants_file else R.string.n_wants_text, who)
        val b = Notification.Builder(ctx, CH_REQ)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(detail)
            .setContentIntent(openApp(ctx))
            .setAutoCancel(true)
            .addAction(Notification.Action.Builder(icon(ctx), ctx.getString(R.string.n_accept), action(ctx, ACTION_ACCEPT, r.sid, 0)).build())
            .addAction(Notification.Action.Builder(icon(ctx), ctx.getString(R.string.n_deny), action(ctx, ACTION_DENY, r.sid, 1)).build())
        ctx.getSystemService(NotificationManager::class.java)
            .notify(r.sid.hashCode(), secure(ctx, b, CH_REQ, ctx.getString(R.string.n_generic_request)).build())
    }

    // progress and result of one transfer, one notification per transfer that is updated in place. the cancel button shows only
    // while the transfer runs or waits, and cancelling goes through Link.cancelTransfer, which ignores finished transfers
    fun transfer(ctx: Context, t: Transfer) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val id = t.sid.hashCode() + 7
        if (Link.foreground) { nm.cancel(id); lastShown.remove(t.sid); return }
        val running = t.status == "active" || t.status == "waiting"
        if (running) {
            val now = SystemClock.elapsedRealtime()
            val last = lastShown[t.sid]
            if (last != null && now - last < 600) return
            lastShown[t.sid] = now
        } else lastShown.remove(t.sid)
        // a received file is announced by received(); the progress notification is just removed
        if (t.status == "done" && !t.outgoing) { nm.cancel(id); return }
        val who = t.who.ifBlank { "pc" }
        val title = ctx.getString(
            when (t.status) {
                "waiting" -> R.string.n_tr_waiting
                "active" -> if (t.outgoing) R.string.n_tr_sending else R.string.n_tr_receiving
                "done" -> R.string.n_tr_done_out
                "denied" -> R.string.n_tr_denied
                "cancelled" -> R.string.n_tr_cancelled
                "timeout" -> R.string.n_tr_timeout
                else -> R.string.n_tr_failed
            },
            who,
        )
        val b = Notification.Builder(ctx, CH_TRANSFER)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(t.name)
            .setContentIntent(openApp(ctx))
            .setOnlyAlertOnce(true)
        if (running) {
            b.setOngoing(true).setCategory(Notification.CATEGORY_PROGRESS)
            if (t.status == "active" && t.size > 0) b.setProgress(100, ((t.bytes * 100) / t.size).toInt().coerceIn(0, 100), false)
            else b.setProgress(0, 0, true)
            b.addAction(Notification.Action.Builder(icon(ctx), ctx.getString(R.string.cancel), action(ctx, ACTION_CANCEL, t.sid, 2)).build())
        } else b.setAutoCancel(true)
        nm.notify(id, secure(ctx, b, CH_TRANSFER, ctx.getString(R.string.n_generic_transfer)).build())
    }

    // something arrived (auto accepted or accepted by the user)
    fun notifyReceived(ctx: Context, item: Item) = received(ctx, item)

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
        val title = ctx.getString(
            when (item.kind) {
                "file" -> if (item.type == "image") R.string.n_got_image else R.string.n_got_file
                "link" -> R.string.n_got_link
                else -> R.string.n_got_text
            },
            item.from,
        )
        // a received image shows its picture (small thumbnail made by the system, nothing is downloaded)
        val thumb = if (item.type == "image" && item.uri != null) {
            runCatching { ctx.contentResolver.loadThumbnail(Uri.parse(item.uri), android.util.Size(480, 480), null) }.getOrNull()
        } else null
        val style: Notification.Style =
            if (thumb != null) Notification.BigPictureStyle().bigPicture(thumb).setSummaryText(item.text)
            else Notification.BigTextStyle().bigText(item.text)
        val b = Notification.Builder(ctx, CH_IN)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(item.text)
            .setStyle(style)
            .setContentIntent(tap)
            .setAutoCancel(true)
        ctx.getSystemService(NotificationManager::class.java)
            .notify(id, secure(ctx, b, CH_IN, ctx.getString(R.string.n_generic_received)).build())
    }

    fun cancel(ctx: Context, sid: String) {
        ctx.getSystemService(NotificationManager::class.java).cancel(sid.hashCode())
    }
}
