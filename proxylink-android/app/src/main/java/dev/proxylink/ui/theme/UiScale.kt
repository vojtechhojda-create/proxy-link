package dev.proxylink.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.proxylink.core.Prefs

// density of the ui: the spacing inside panels and between blocks (text and touch targets are not made smaller by it)
enum class UiDensity(val key: String, val pad: Int, val gap: Int) {
    COMPACT("compact", 12, 8),
    DEFAULT("default", 16, 12),
    COMFORTABLE("comfortable", 20, 16),
}

// size of the elements: scales dp (buttons, icons, spacing). the text size is kept as chosen, so this never changes the text
enum class ElementSize(val key: String, val scale: Float) {
    SMALL("small", 0.92f),
    DEFAULT("default", 1f),
    LARGE("large", 1.1f),
}

// monospace for technical data (ids, addresses, hashes): the bundled font or the system monospace
enum class MonoFont(val key: String) { BUNDLED("bundled"), SYSTEM("system") }

val TextScales = listOf(85, 100, 115, 130)

// global state of the visual preferences below the palette, font and corners; persisted through Prefs. an unknown stored value
// falls back to the default. the text scale multiplies the system font size, it does not replace it
object UiStore {
    var density by mutableStateOf(UiDensity.DEFAULT)
        private set
    var size by mutableStateOf(ElementSize.DEFAULT)
        private set
    var textScale by mutableIntStateOf(100)
        private set
    var mono by mutableStateOf(MonoFont.BUNDLED)
        private set

    // false when the user turned animations off in the system ("remove animations"); read when the app starts
    var animations by mutableStateOf(true)
        private set

    fun selectDensity(d: UiDensity) { density = d }
    fun selectSize(s: ElementSize) { size = s }
    fun selectTextScale(p: Int) { textScale = if (p in TextScales) p else 100 }
    fun selectMono(m: MonoFont) { mono = m }

    fun load(prefs: Prefs, ctx: Context) {
        density = UiDensity.entries.firstOrNull { it.key == prefs.uiDensity } ?: UiDensity.DEFAULT
        size = ElementSize.entries.firstOrNull { it.key == prefs.elementSize } ?: ElementSize.DEFAULT
        selectTextScale(prefs.textScale)
        mono = MonoFont.entries.firstOrNull { it.key == prefs.monoFont } ?: MonoFont.BUNDLED
        animations = Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
}
