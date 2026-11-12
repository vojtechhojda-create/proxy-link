package dev.proxylink.core

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import dev.proxylink.R
import dev.proxylink.ShareActivity

// direct share targets in the android share sheet. android decides if, where and in which order they show.
// ids: "pc:<pc id>" for a paired pc (real name, stable id), "dev:<id>" for another device seen through the connected pc.
// the old id "pc" (one shortcut for the connected pc) is removed
object Shortcuts {
    private const val CAT = "dev.proxylink.category.SHARE_TARGET"

    fun publish(ctx: Context, known: List<KnownDevice>, others: List<Other>) {
        val icon = IconCompat.createWithResource(ctx, R.mipmap.ic_launcher)
        fun sc(id: String, label: String) = ShortcutInfoCompat.Builder(ctx, id)
            .setShortLabel(label.take(10).ifBlank { "pc" })
            .setLongLabel(label.take(25).ifBlank { "pc" })
            .setIcon(icon)
            .setCategories(setOf(CAT))
            .setLongLived(true)
            .setIntent(Intent(ctx, ShareActivity::class.java).setAction(Intent.ACTION_SEND))
            .build()
        val list = known.map { sc("pc:${it.id}", it.name) } + others.map { sc("dev:${it.id}", it.name) }
        runCatching {
            val max = ShortcutManagerCompat.getMaxShortcutCountPerActivity(ctx)
            ShortcutManagerCompat.setDynamicShortcuts(ctx, list.take(max))
            ShortcutManagerCompat.removeLongLivedShortcuts(ctx, listOf("pc"))
        }
    }

    // removes the cached long lived copy as well, otherwise the share sheet may keep showing it
    fun remove(ctx: Context, id: String) {
        runCatching {
            ShortcutManagerCompat.removeDynamicShortcuts(ctx, listOf(id))
            ShortcutManagerCompat.removeLongLivedShortcuts(ctx, listOf(id))
        }
    }
}
