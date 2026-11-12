package dev.proxylink

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.RemoteViews
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.toArgb
import dev.proxylink.core.Link
import dev.proxylink.core.LinkState
import dev.proxylink.core.Prefs
import dev.proxylink.ui.theme.Palettes

// home screen widgets: send clipboard (one action), three actions (file / photo / clipboard) and the connection state.
// colors follow the palette chosen in the app (or the system colors); remote views cannot use the app font, they use the system one.
// the widgets are redrawn when the palette, the clipboard switch or the connection state changes
object Widgets {
    private class Colors(val bg: Int, val fg: Int, val muted: Int, val accent: Int)

    private fun colors(ctx: Context): Colors {
        val p = Prefs(ctx)
        val night = ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val s = when {
            p.dynamicColor -> if (night) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
            else -> (Palettes.all.firstOrNull { it.name == p.palette } ?: Palettes.brand).scheme
        }
        return Colors(s.surfaceContainer.toArgb(), s.onSurface.toArgb(), s.onSurfaceVariant.toArgb(), s.primary.toArgb())
    }

    private fun activity(ctx: Context, req: Int, action: String, cls: Class<*>, kind: String? = null): PendingIntent {
        val i = Intent(ctx, cls).setAction(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        kind?.let { i.putExtra(WidgetSendActivity.KIND, it) }
        return PendingIntent.getActivity(ctx, req, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun RemoteViews.paint(c: Colors) = setInt(android.R.id.background, "setBackgroundColor", c.bg)

    fun clip(ctx: Context): RemoteViews {
        val c = colors(ctx)
        val v = RemoteViews(ctx.packageName, R.layout.widget_clip)
        v.paint(c)
        v.setTextViewText(R.id.clip_label, ctx.getString(R.string.widget_send_clipboard))
        v.setTextColor(R.id.clip_label, c.fg)
        v.setInt(R.id.clip_icon, "setColorFilter", c.accent)
        v.setOnClickPendingIntent(android.R.id.background, activity(ctx, 10, "dev.proxylink.widget.CLIP", ClipSendActivity::class.java))
        return v
    }

    // the clipboard action disappears while the clipboard feature is off in the settings; the settings still switch it on again
    fun actions(ctx: Context): RemoteViews {
        val c = colors(ctx)
        val v = RemoteViews(ctx.packageName, R.layout.widget_actions)
        v.paint(c)
        fun item(box: Int, icon: Int, label: Int, text: Int, pi: PendingIntent) {
            v.setTextViewText(label, ctx.getString(text))
            v.setTextColor(label, c.fg)
            v.setInt(icon, "setColorFilter", c.accent)
            v.setContentDescription(box, ctx.getString(text))
            v.setOnClickPendingIntent(box, pi)
        }
        item(R.id.act_file, R.id.act_file_icon, R.id.act_file_label, R.string.widget_file,
            activity(ctx, 20, "dev.proxylink.widget.FILE", WidgetSendActivity::class.java, "file"))
        item(R.id.act_photo, R.id.act_photo_icon, R.id.act_photo_label, R.string.widget_photo,
            activity(ctx, 21, "dev.proxylink.widget.PHOTO", WidgetSendActivity::class.java, "image"))
        if (Prefs(ctx).clipEnabled) {
            item(R.id.act_clip, R.id.act_clip_icon, R.id.act_clip_label, R.string.widget_clipboard,
                activity(ctx, 22, "dev.proxylink.widget.CLIP", ClipSendActivity::class.java))
        } else v.setViewVisibility(R.id.act_clip, android.view.View.GONE)
        return v
    }

    // the state is read from the link as it is: a widget of a process that was killed starts as "not connected", never as a guess
    fun status(ctx: Context): RemoteViews {
        val c = colors(ctx)
        val v = RemoteViews(ctx.packageName, R.layout.widget_status)
        v.paint(c)
        val st = Link.state
        val on = st == LinkState.CONNECTED
        val name = Link.pcName
        v.setTextViewText(R.id.st_title, if (st != LinkState.IDLE && name.isNotBlank()) name else ctx.getString(R.string.app_name))
        v.setTextViewText(
            R.id.st_sub,
            ctx.getString(
                when {
                    on && Link.guest -> R.string.widget_guest
                    on -> R.string.connected
                    st == LinkState.CONNECTING -> R.string.widget_connecting
                    else -> R.string.nc_not_connected
                },
            ),
        )
        v.setTextColor(R.id.st_title, c.fg)
        v.setTextColor(R.id.st_sub, if (on) c.accent else c.muted)
        v.setInt(R.id.st_icon, "setColorFilter", if (on) c.accent else c.muted)
        v.setOnClickPendingIntent(android.R.id.background, activity(ctx, 30, "dev.proxylink.widget.OPEN", MainActivity::class.java))
        return v
    }

    fun updateAll(ctx: Context) {
        val m = AppWidgetManager.getInstance(ctx)
        fun upd(cls: Class<*>, build: (Context) -> RemoteViews) {
            val ids = m.getAppWidgetIds(ComponentName(ctx, cls))
            if (ids.isNotEmpty()) m.updateAppWidget(ids, build(ctx))
        }
        runCatching {
            upd(ClipWidget::class.java, ::clip)
            upd(ActionsWidget::class.java, ::actions)
            upd(StatusWidget::class.java, ::status)
        }
    }
}

class ClipWidget : android.appwidget.AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = manager.updateAppWidget(ids, Widgets.clip(context))
}

class ActionsWidget : android.appwidget.AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = manager.updateAppWidget(ids, Widgets.actions(context))
}

class StatusWidget : android.appwidget.AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = manager.updateAppWidget(ids, Widgets.status(context))
}
