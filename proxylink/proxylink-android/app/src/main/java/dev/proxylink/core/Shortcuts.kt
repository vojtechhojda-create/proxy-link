package dev.proxylink.core

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import dev.proxylink.R
import dev.proxylink.ShareActivity

// direct share targets: the pc and the other devices appear in the top row of the android share sheet
object Shortcuts {
    private const val CAT = "dev.proxylink.category.SHARE_TARGET"

    fun publish(ctx: Context, pcName: String, others: List<Other>) {
        val icon = IconCompat.createWithResource(ctx, R.mipmap.ic_launcher)
        fun sc(id: String, label: String) = ShortcutInfoCompat.Builder(ctx, id)
            .setShortLabel(label.take(10).ifBlank { "pc" })
            .setLongLabel(label.take(25).ifBlank { "pc" })
            .setIcon(icon)
            .setCategories(setOf(CAT))
            .setLongLived(true)
            .setIntent(Intent(ctx, ShareActivity::class.java).setAction(Intent.ACTION_SEND))
            .build()
        val list = mutableListOf(sc("pc", pcName.ifBlank { "pc" }))
        others.take(3).forEach { list.add(sc(it.id, it.name)) }
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(ctx, list) }
    }
}
