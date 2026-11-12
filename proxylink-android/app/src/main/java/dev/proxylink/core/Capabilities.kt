package dev.proxylink.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray

enum class Feature(val key: String) { TEXT("text"), FILES("files"), CLIPBOARD("clipboard") }

// what the connected pc can do, kept apart from what the user switched on (Prefs).
// a feature is available when the user wants it (Prefs) AND the peer supports it (here). a missing capability is shown as
// unavailable, never as "switched off by the user".
// the current pc sends only its protocol version ("v": 2) in hello, which has all of the features above. when a newer pc adds
// a "caps" array to hello, it is read here and decides; nothing in the pc is needed for this to work today.
object Capabilities {
    private const val BASELINE = 2
    var protocol by mutableIntStateOf(0); private set          // 0 = not connected / unknown
    private var declared by mutableStateOf<Set<String>?>(null)

    fun update(v: Int, caps: JSONArray?) {
        protocol = v
        declared = caps?.let { a -> (0 until a.length()).map { a.optString(it) }.toSet() }
    }

    fun reset() { protocol = 0; declared = null }

    fun supports(f: Feature): Boolean = declared?.contains(f.key) ?: (protocol == 0 || protocol >= BASELINE)
}
