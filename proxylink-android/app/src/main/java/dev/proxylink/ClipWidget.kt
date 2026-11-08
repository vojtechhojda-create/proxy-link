package dev.proxylink

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

// home screen widget: one tap sends the clipboard
class ClipWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pi = PendingIntent.getActivity(
            context, 0,
            Intent(context, ClipSendActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_clip)
            views.setOnClickPendingIntent(R.id.widget_root, pi)
            manager.updateAppWidget(id, views)
        }
    }
}
