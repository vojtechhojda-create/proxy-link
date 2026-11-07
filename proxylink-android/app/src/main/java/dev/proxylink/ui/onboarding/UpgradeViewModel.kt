package dev.proxylink.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.proxylink.core.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UpgradeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SettingsRepository(app)

    // null while the stored value is still loading, so neither screen flashes at startup
    val hasSeen: StateFlow<Boolean?> = repo.hasSeenUpgradeScreen
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun continueFree() {
        viewModelScope.launch { repo.setHasSeenUpgradeScreen(true) }
    }
}
