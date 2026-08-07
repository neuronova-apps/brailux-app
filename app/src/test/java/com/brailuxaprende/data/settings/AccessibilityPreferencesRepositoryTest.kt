package com.brailuxaprende.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AccessibilityPreferencesRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var dataStoreFile: File
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: AccessibilityPreferencesRepository

    @Before
    fun setUp() {
        dataStoreFile = File(temporaryFolder.root, "accessibility.preferences_pb")
        createRepository()
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun highContrastDefaultsToDisabled() = runBlocking {
        assertFalse(repository.preferences.first().highContrastEnabled)
    }

    @Test
    fun highContrastChangeIsPersisted() = runBlocking {
        repository.setHighContrastEnabled(true)

        val reopenedRepository = reopenRepository()

        assertTrue(reopenedRepository.preferences.first().highContrastEnabled)
    }

    @Test
    fun textSizeChangeIsPersisted() = runBlocking {
        repository.setTextSize(TextSizePreference.VeryLarge)

        val reopenedRepository = reopenRepository()

        assertEquals(TextSizePreference.VeryLarge, reopenedRepository.preferences.first().textSize)
    }

    @Test
    fun soundChangeIsPersisted() = runBlocking {
        repository.setSoundEnabled(false)

        val reopenedRepository = reopenRepository()

        assertFalse(reopenedRepository.preferences.first().soundEnabled)
    }

    @Test
    fun vibrationChangeIsPersisted() = runBlocking {
        repository.setVibrationEnabled(false)

        val reopenedRepository = reopenRepository()

        assertFalse(reopenedRepository.preferences.first().vibrationEnabled)
    }

    @Test
    fun unrecognizedValuesUseSafeDefaults() = runBlocking {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(SoundEnabledKeyName)] = "unknown"
            preferences[stringPreferencesKey(VibrationEnabledKeyName)] = "unknown"
            preferences[stringPreferencesKey(HighContrastEnabledKeyName)] = "unknown"
            preferences[stringPreferencesKey(TextSizeKeyName)] = "enormous"
        }

        val preferences = repository.preferences.first()

        assertTrue(preferences.soundEnabled)
        assertTrue(preferences.vibrationEnabled)
        assertFalse(preferences.highContrastEnabled)
        assertEquals(TextSizePreference.Normal, preferences.textSize)
    }

    private fun createRepository() {
        dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { dataStoreFile },
        )
        repository = AccessibilityPreferencesRepository(dataStore)
    }

    private suspend fun reopenRepository(): AccessibilityPreferencesRepository {
        dataStoreScope.coroutineContext[Job]?.cancelAndJoin()
        createRepository()
        return repository
    }
}
