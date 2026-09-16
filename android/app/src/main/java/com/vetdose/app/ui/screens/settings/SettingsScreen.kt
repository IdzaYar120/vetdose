package com.vetdose.app.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vetdose.app.R
import com.vetdose.app.ui.theme.WarningAbsolute
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private fun formatSyncTime(iso: String): String = try {
    OffsetDateTime.parse(iso)
        .atZoneSameInstant(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
} catch (e: Exception) {
    iso
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.settings_server_address), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.serverAddress,
            onValueChange = viewModel::onServerAddressChanged,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = viewModel::onServerAddressSaved,
            modifier = Modifier.heightIn(min = 56.dp),
        ) {
            Text(stringResource(R.string.settings_server_address_save))
        }

        Spacer(Modifier.height(32.dp))
        Text(
            if (uiState.lastSyncTimeIso != null) {
                stringResource(R.string.settings_last_sync, formatSyncTime(uiState.lastSyncTimeIso!!))
            } else {
                stringResource(R.string.settings_last_sync_never)
            },
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = viewModel::onSyncNowClicked,
            enabled = !uiState.isSyncing,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) {
            if (uiState.isSyncing) {
                CircularProgressIndicator(modifier = Modifier.heightIn(max = 20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.height(0.dp))
                Text(" " + stringResource(R.string.settings_syncing))
            } else {
                Text(stringResource(R.string.settings_sync_now))
            }
        }

        uiState.lastOutcome?.let { outcome ->
            Spacer(Modifier.height(12.dp))
            val textRes = if (outcome == SyncOutcome.SUCCESS) {
                R.string.settings_sync_success
            } else {
                R.string.settings_sync_error
            }
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (outcome == SyncOutcome.ERROR) {
                        WarningAbsolute.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                ),
            ) {
                Text(stringResource(textRes), modifier = Modifier.padding(16.dp))
            }
        }

        Spacer(Modifier.height(32.dp))
        Text(
            stringResource(R.string.settings_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
