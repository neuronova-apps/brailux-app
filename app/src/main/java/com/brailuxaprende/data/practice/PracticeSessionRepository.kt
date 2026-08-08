package com.brailuxaprende.data.practice

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.brailuxaprende.practice.PracticeLevel
import com.brailuxaprende.practice.PracticeSessionSnapshot
import com.brailuxaprende.practice.PracticeSessionSnapshotCodec
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

data class StoredPracticeSessions(
    val isLoaded: Boolean,
    val snapshots: Map<PracticeLevel, PracticeSessionSnapshot>,
)

class PracticeSessionRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val sessions: Flow<StoredPracticeSessions> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            StoredPracticeSessions(
                isLoaded = true,
                snapshots = buildMap {
                    PracticeLevel.entries.forEach { level ->
                        val encoded = preferences[snapshotKey(level)] ?: return@forEach
                        val snapshot = PracticeSessionSnapshotCodec.decode(encoded) ?: return@forEach
                        if (snapshot.level == level) put(level, snapshot)
                    }
                },
            )
        }

    suspend fun save(snapshot: PracticeSessionSnapshot) {
        val encoded = PracticeSessionSnapshotCodec.encode(snapshot)
        dataStore.edit { preferences ->
            preferences[snapshotKey(snapshot.level)] = encoded
        }
    }

    suspend fun clear(level: PracticeLevel) {
        dataStore.edit { preferences -> preferences.remove(snapshotKey(level)) }
    }

    suspend fun clearAll() {
        dataStore.edit { preferences ->
            PracticeLevel.entries.forEach { level -> preferences.remove(snapshotKey(level)) }
        }
    }

    private companion object {
        fun snapshotKey(level: PracticeLevel): Preferences.Key<String> = stringPreferencesKey(
            practiceSessionSnapshotKeyName(level),
        )
    }
}

internal fun practiceSessionSnapshotKeyName(level: PracticeLevel): String =
    "practice_session_snapshot_${level.name}"
