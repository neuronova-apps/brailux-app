package com.brailuxaprende.ui.screens

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.brailuxaprende.data.practice.PracticeSessionRepository
import com.brailuxaprende.data.practice.StoredPracticeSessions
import com.brailuxaprende.practice.PracticeLevel
import com.brailuxaprende.practice.PracticeSessionSnapshot
import com.brailuxaprende.practice.PracticeSessionSnapshotCodec
import com.brailuxaprende.practice.acceptsCreditResolution
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PracticeSessionViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: PracticeSessionRepository,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writes = Channel<PersistenceWrite>(capacity = Channel.UNLIMITED)
    private val restoredFromSavedState = PracticeLevel.entries.mapNotNull { level ->
        savedStateHandle.get<String>(savedStateKey(level))
            ?.let(PracticeSessionSnapshotCodec::decode)
            ?.takeIf { it.level == level }
            ?.let { level to it }
    }.toMap()
    private val _sessions = MutableStateFlow(
        StoredPracticeSessions(
            isLoaded = false,
            snapshots = restoredFromSavedState,
        ),
    )

    val sessions: StateFlow<StoredPracticeSessions> = _sessions.asStateFlow()

    init {
        scope.launch {
            val persisted = repository.sessions.first()
            _sessions.value = StoredPracticeSessions(
                isLoaded = true,
                snapshots = persisted.snapshots + restoredFromSavedState,
            )
            for (snapshot in restoredFromSavedState.values) {
                try {
                    repository.save(snapshot)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    // SavedStateHandle still contains the authoritative restored snapshot.
                }
            }
            processWrites()
        }
    }

    fun save(snapshot: PracticeSessionSnapshot) {
        updateImmediately(snapshot)
        writes.trySend(PersistenceWrite.Save(snapshot))
    }

    fun saveBeforeCredit(
        snapshot: PracticeSessionSnapshot,
        onPersisted: (Boolean) -> Unit,
    ) {
        updateImmediately(snapshot)
        writes.trySend(PersistenceWrite.Save(snapshot, onPersisted))
    }

    fun resolveCredit(snapshot: PracticeSessionSnapshot) {
        val current = _sessions.value.snapshots[snapshot.level]
        if (!current.acceptsCreditResolution(snapshot)) return
        save(snapshot)
    }

    fun clear(level: PracticeLevel) {
        savedStateHandle.remove<String>(savedStateKey(level))
        _sessions.update { current ->
            current.copy(snapshots = current.snapshots - level)
        }
        writes.trySend(PersistenceWrite.Clear(level))
    }

    override fun onCleared() {
        writes.close()
        scope.cancel()
    }

    private fun updateImmediately(snapshot: PracticeSessionSnapshot) {
        savedStateHandle[savedStateKey(snapshot.level)] =
            PracticeSessionSnapshotCodec.encode(snapshot)
        _sessions.update { current ->
            current.copy(snapshots = current.snapshots + (snapshot.level to snapshot))
        }
    }

    private suspend fun processWrites() {
        for (write in writes) {
            when (write) {
                is PersistenceWrite.Save -> {
                    val persisted = try {
                        repository.save(write.snapshot)
                        true
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        false
                    }
                    write.onPersisted?.let { callback ->
                        withContext(Dispatchers.Main.immediate) { callback(persisted) }
                    }
                }
                is PersistenceWrite.Clear -> {
                    try {
                        repository.clear(write.level)
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        // SavedStateHandle and the in-memory state remain authoritative.
                    }
                }
            }
        }
    }

    private sealed interface PersistenceWrite {
        data class Save(
            val snapshot: PracticeSessionSnapshot,
            val onPersisted: ((Boolean) -> Unit)? = null,
        ) : PersistenceWrite

        data class Clear(val level: PracticeLevel) : PersistenceWrite
    }
}

class PracticeSessionViewModelFactory(
    owner: SavedStateRegistryOwner,
    defaultArgs: Bundle?,
    private val repository: PracticeSessionRepository,
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle,
    ): T {
        require(modelClass.isAssignableFrom(PracticeSessionViewModel::class.java))
        return PracticeSessionViewModel(handle, repository) as T
    }
}

private fun savedStateKey(level: PracticeLevel): String =
    "practice_session_saved_state_${level.name}"
