package com.vetdose.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.vetdose.app.data.settings.SettingsRepository
import com.vetdose.app.data.sync.SyncScheduler
import com.vetdose.app.data.sync.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SyncOutcome { SUCCESS, ERROR }

data class SettingsUiState(
    val serverAddress: String = "",
    val lastSyncTimeIso: String? = null,
    val isSyncing: Boolean = false,
    val lastOutcome: SyncOutcome? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val syncScheduler: SyncScheduler,
    workManager: WorkManager,
) : ViewModel() {

    private val addressOverride = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.serverBaseUrlFlow,
        settingsRepository.lastSyncTimeFlow,
        workManager.getWorkInfosForUniqueWorkFlow(SyncWorker.ONE_TIME_WORK_NAME),
        addressOverride,
    ) { storedAddress, lastSync, workInfos, override ->
        // WorkManager keeps the most recent run's WorkInfo around for a unique
        // work name until the next one is enqueued, so this naturally shows
        // "still running" while syncing and the last run's outcome after.
        val latestWork = workInfos.maxByOrNull { it.id.hashCode() }
        SettingsUiState(
            serverAddress = override ?: storedAddress,
            lastSyncTimeIso = lastSync,
            isSyncing = latestWork?.state == WorkInfo.State.ENQUEUED || latestWork?.state == WorkInfo.State.RUNNING,
            lastOutcome = when (latestWork?.state) {
                WorkInfo.State.SUCCEEDED -> SyncOutcome.SUCCESS
                WorkInfo.State.FAILED -> SyncOutcome.ERROR
                else -> null
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun onServerAddressChanged(text: String) {
        addressOverride.value = text
    }

    fun onServerAddressSaved() {
        val address = addressOverride.value ?: return
        viewModelScope.launch {
            settingsRepository.setServerBaseUrl(address)
            addressOverride.value = null
        }
    }

    fun onSyncNowClicked() {
        syncScheduler.triggerImmediateSync()
    }
}
