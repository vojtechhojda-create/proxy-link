package dev.proxylink.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import dev.proxylink.core.Link

// short haptic tick for deliberate actions, can be turned off in settings
@Composable
fun rememberHaptic(): () -> Unit {
    val h = LocalHapticFeedback.current
    return remember(h) { { if (Link.prefs.haptics) h.performHapticFeedback(HapticFeedbackType.LongPress) } }
}
