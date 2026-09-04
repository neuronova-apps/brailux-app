package com.brailuxaprende.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AccessibilityPreferencesBackgroundRotationTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: AccessibilityPreferencesRepository

    @Before
    fun setUp() {
        dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = {
                File(temporaryFolder.root, "background_rotation.preferences_pb")
            },
        )
        repository = AccessibilityPreferencesRepository(dataStore)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun rotationModeDefaultsToFixed() = runBlocking {
        assertEquals(
            BackgroundRotationMode.Fixed,
            repository.preferences.first().backgroundRotationMode,
        )
    }

    @Test
    fun manualBackgroundSelectionReturnsToFixedMode() = runBlocking {
        repository.setBackgroundRotationMode(BackgroundRotationMode.OnAppOpen)
        repository.selectBackgroundAndFix(BrailuxBackgroundCatalog.CREMA_ONDAS_ID)

        val preferences = repository.preferences.first()

        assertEquals(
            BrailuxBackgroundCatalog.CREMA_ONDAS_ID,
            preferences.selectedBackgroundId,
        )
        assertEquals(BackgroundRotationMode.Fixed, preferences.backgroundRotationMode)
    }

    @Test
    fun onAppOpenModeMovesToNextPremiumBackground() = runBlocking {
        repository.selectBackgroundAndFix(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID)
        repository.setBackgroundRotationMode(BackgroundRotationMode.OnAppOpen)

        repository.rotatePremiumBackgroundOnForeground(
            isPremiumUnlocked = true,
            nowMillis = 10_000L,
        )

        assertEquals(
            BrailuxBackgroundCatalog.CREMA_ONDAS_ID,
            repository.preferences.first().selectedBackgroundId,
        )
    }

    @Test
    fun periodicModeWaitsThenMovesToNextPremiumBackground() = runBlocking {
        val start = 10_000L
        repository.selectBackgroundAndFix(BrailuxBackgroundCatalog.LAVANDA_NIEBLA_ID)
        repository.setBackgroundRotationMode(
            mode = BackgroundRotationMode.EverySixHours,
            nowMillis = start,
        )

        repository.rotatePremiumBackgroundOnForeground(
            isPremiumUnlocked = true,
            nowMillis = start +
                BrailuxBackgroundRotationPolicy.PERIODIC_INTERVAL_MILLIS - 1L,
        )
        assertEquals(
            BrailuxBackgroundCatalog.LAVANDA_NIEBLA_ID,
            repository.preferences.first().selectedBackgroundId,
        )

        repository.rotatePremiumBackgroundOnForeground(
            isPremiumUnlocked = true,
            nowMillis = start +
                BrailuxBackgroundRotationPolicy.PERIODIC_INTERVAL_MILLIS,
        )
        assertNotEquals(
            BrailuxBackgroundCatalog.LAVANDA_NIEBLA_ID,
            repository.preferences.first().selectedBackgroundId,
        )
    }

    @Test
    fun lockedPremiumDoesNotRotateOnForeground() = runBlocking {
        repository.selectBackgroundAndFix(BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID)
        repository.setBackgroundRotationMode(BackgroundRotationMode.OnAppOpen)

        repository.rotatePremiumBackgroundOnForeground(
            isPremiumUnlocked = false,
            nowMillis = 10_000L,
        )

        assertEquals(
            BrailuxBackgroundCatalog.CELESTE_GEOMETRICO_ID,
            repository.preferences.first().selectedBackgroundId,
        )
    }
}

