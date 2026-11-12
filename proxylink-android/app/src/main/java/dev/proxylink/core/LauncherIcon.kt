package dev.proxylink.core

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

// launcher icon by palette. one enabled activity-alias per palette, only one is enabled at a time.
// the system themed (monochrome) icon is a separate thing: it comes from the monochrome layer of every alias.
object LauncherIcon {
    private const val DEFAULT = "brand"

    // palette name -> alias in the manifest. unknown palettes and system colors use the brand icon
    private val aliases = mapOf(
        "brand" to "dev.proxylink.LauncherBrand",
        "navyOrange" to "dev.proxylink.LauncherNavyOrange",
    )

    // palettes without an alias show the brand icon
    fun hasOwn(palette: String) = palette in aliases

    fun apply(ctx: Context, byPalette: Boolean, palette: String, systemColors: Boolean) {
        val key = if (byPalette && !systemColors && palette in aliases) palette else DEFAULT
        val pm = ctx.packageManager
        fun cn(name: String) = ComponentName(ctx.packageName, name)
        fun enabled(name: String) = pm.getComponentEnabledSetting(cn(name)).let {
            // DEFAULT means the manifest value: only the brand alias is enabled there
            if (it == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) name == aliases.getValue(DEFAULT)
            else it == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
        val target = aliases.getValue(key)
        // enable the new entry first so a launcher entry always exists
        if (!enabled(target)) {
            pm.setComponentEnabledSetting(cn(target), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        }
        aliases.values.filter { it != target && enabled(it) }.forEach {
            pm.setComponentEnabledSetting(cn(it), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }
    }
}
