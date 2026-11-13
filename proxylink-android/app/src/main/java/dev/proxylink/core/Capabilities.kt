package dev.proxylink.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray

// minProtocol: the lowest protocol version that has the feature. features of the baseline protocol (text, files, clipboard)
// are implied by that version; every newer feature counts only when the other side announces it by its key
enum class Feature(val key: String, val minProtocol: Int) {
    TEXT("text", 2), FILES("files", 2), CLIPBOARD("clipboard", 2),
    SCREEN("screen", 3),   // not implemented in this app; here only so it is never shown as available without an announcement
}

// how the two sides fit together, decided from what the peer announced in hello. it is not a statement about the connection
enum class Compat { UNKNOWN, IDENTICAL, PC_NEWER, PC_OLDER, PC_TOO_OLD, MOBILE_TOO_OLD }

// three separate things: the VERSION (protocol number and app version the peer announced), its CAPABILITIES (the "caps" list it
// announced) and the STATE of the link (Link.state) plus what the user switched on (Prefs). a feature is available only when the
// user wants it AND the peer supports it. a missing capability is shown as unavailable, never as "switched off by the user".
// the pc of today announces only the protocol number ("v": 2) in hello; a newer pc may also send "app" (its app version),
// "caps" (features) and "min" (the lowest mobile protocol it still serves). nothing from the pc is needed for this to work today
object Capabilities {
    // protocol this app speaks, the lowest peer protocol with the basic transfers, and the phone side features it announces
    const val MOBILE_PROTOCOL = 2
    const val MIN_PROTOCOL = 2
    val MOBILE_CAPS = listOf(Feature.TEXT.key, Feature.FILES.key, Feature.CLIPBOARD.key)

    var protocol by mutableIntStateOf(0); private set          // 0 = not connected or not announced
    var peerApp by mutableStateOf<String?>(null); private set  // app version of the peer, null = not announced
    private var peerMin by mutableIntStateOf(0)
    private var declared by mutableStateOf<Set<String>?>(null)

    fun update(v: Int, caps: JSONArray?, app: String? = null, min: Int = 0) {
        protocol = v
        peerApp = app?.takeIf { it.isNotBlank() }?.take(32)
        peerMin = min
        declared = caps?.let { a -> (0 until a.length()).map { a.optString(it) }.toSet() }
    }

    fun reset() { protocol = 0; peerApp = null; peerMin = 0; declared = null }

    val compat: Compat
        get() = when {
            protocol == 0 -> Compat.UNKNOWN
            peerMin > MOBILE_PROTOCOL -> Compat.MOBILE_TOO_OLD
            protocol < MIN_PROTOCOL -> Compat.PC_TOO_OLD
            protocol < MOBILE_PROTOCOL -> Compat.PC_OLDER
            protocol > MOBILE_PROTOCOL -> Compat.PC_NEWER
            else -> Compat.IDENTICAL
        }

    // an unknown capability is not a confirmed one: without a "caps" list only the baseline features count as supported, and
    // when the versions cannot work together nothing is sent. unequal versions with the baseline in common stay usable
    fun supports(f: Feature): Boolean {
        if (compat == Compat.PC_TOO_OLD || compat == Compat.MOBILE_TOO_OLD) return false
        if (protocol in 1 until f.minProtocol) return false
        declared?.let { return f.key in it }
        return f.minProtocol <= MIN_PROTOCOL
    }
}
