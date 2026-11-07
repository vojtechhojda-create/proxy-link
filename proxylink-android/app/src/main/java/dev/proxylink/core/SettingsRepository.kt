package dev.proxylink.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// one datastore per process, declared once at top level
private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "proxylink_settings")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsStore

    // false until the user presses "continue free" on the first-launch screen
    val hasSeenUpgradeScreen: Flow<Boolean> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_SEEN_UPGRADE] ?: false }

    suspend fun setHasSeenUpgradeScreen(value: Boolean) {
        store.edit { it[KEY_SEEN_UPGRADE] = value }
    }

    private companion object {
        val KEY_SEEN_UPGRADE = booleanPreferencesKey("hasSeenUpgradeScreen")
    }
}
