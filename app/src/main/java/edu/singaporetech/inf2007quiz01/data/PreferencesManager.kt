package edu.singaporetech.inf2007quiz01.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "calbot_preferences")

class PreferencesManager(private val context: Context) {

    fun getApiToggle(calBotId: Int): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[booleanPreferencesKey("toggle_api_$calBotId")] ?: false
        }
    }

    suspend fun setApiToggle(calBotId: Int, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[booleanPreferencesKey("toggle_api_$calBotId")] = enabled
        }
    }
}
