package com.vetdose.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "vetdose_settings")

/**
 * Persists the two pieces of state the sync flow needs across process death:
 * where the server is, and when we last synced successfully (the `since`
 * cursor for the next incremental `/sync` call).
 */
@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val SERVER_BASE_URL = stringPreferencesKey("server_base_url")
        val LAST_SYNC_TIME = stringPreferencesKey("last_sync_time")
    }

    val serverBaseUrlFlow: Flow<String> =
        context.dataStore.data.map { it[Keys.SERVER_BASE_URL] ?: DEFAULT_SERVER_BASE_URL }

    val lastSyncTimeFlow: Flow<String?> =
        context.dataStore.data.map { it[Keys.LAST_SYNC_TIME] }

    suspend fun currentServerBaseUrl(): String = serverBaseUrlFlow.first()

    suspend fun setServerBaseUrl(url: String) {
        context.dataStore.edit { it[Keys.SERVER_BASE_URL] = url }
    }

    suspend fun setLastSyncTime(isoTimestamp: String) {
        context.dataStore.edit { it[Keys.LAST_SYNC_TIME] = isoTimestamp }
    }

    companion object {
        // 10.0.2.2 is the Android emulator's alias for the host machine's
        // localhost, where `docker compose up` runs the backend in dev.
        const val DEFAULT_SERVER_BASE_URL = "http://10.0.2.2:8000/"
    }
}
