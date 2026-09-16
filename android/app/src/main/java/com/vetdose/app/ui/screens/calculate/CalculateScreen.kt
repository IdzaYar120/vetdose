package com.vetdose.app.ui.screens.calculate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vetdose.app.R
import com.vetdose.app.domain.calculator.formatNumber
import com.vetdose.app.domain.model.Species
import com.vetdose.app.ui.theme.WarningCaution
import androidx.hilt.navigation.compose.hiltViewModel

private fun speciesEmoji(code: String): String = when (code) {
    "cat" -> "🐱"
    "dog" -> "🐶"
    "cattle" -> "🐄"
    "pig" -> "🐷"
    "horse" -> "🐴"
    "sheep" -> "🐑"
    "goat" -> "🐐"
    "poultry" -> "🐔"
    "rabbit" -> "🐰"
    else -> "🐾"
}

@Composable
fun CalculateScreen(
    onNavigateToProductSearch: (speciesId: String) -> Unit,
    onNavigateToResult: (speciesId: String, doseRuleId: String, productId: String, weightKg: String) -> Unit,
    onNavigateToSettings: () -> Unit,
    savedStateHandle: SavedStateHandle?,
    viewModel: CalculateViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val selectedProductId = savedStateHandle
        ?.getStateFlow<String?>("selected_product_id", null)
        ?.collectAsState()
    LaunchedEffect(selectedProductId?.value) {
        val productId = selectedProductId?.value
        if (productId != null) {
            viewModel.onProductIdSelected(productId)
            savedStateHandle?.set<String?>("selected_product_id", null)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CalculateEvent.NavigateToResult ->
                    onNavigateToResult(event.speciesId, event.doseRuleId, event.productId, event.weightKg)
            }
        }
    }

    if (uiState.needsFirstSync) {
        FirstSyncNeeded(onNavigateToSettings)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.calculate_select_species), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        SpeciesRow(
            species = uiState.speciesList,
            selected = uiState.selectedSpecies,
            onSelected = viewModel::onSpeciesSelected,
        )

        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = uiState.weightInput,
            onValueChange = viewModel::onWeightInputChanged,
            label = { Text(stringResource(R.string.calculate_weight_label)) },
            placeholder = { Text(stringResource(R.string.calculate_weight_placeholder)) },
            isError = uiState.weightError,
            supportingText = {
                if (uiState.weightError) Text(stringResource(R.string.calculate_weight_error))
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        )

        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.calculate_product_label), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        val species = uiState.selectedSpecies
        val product = uiState.selectedProduct
        if (product == null) {
            OutlinedButton(
                onClick = { species?.let { onNavigateToProductSearch(it.id) } },
                enabled = species != null,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Text(
                    if (species == null) {
                        stringResource(R.string.calculate_select_species_first)
                    } else {
                        stringResource(R.string.calculate_choose_product)
                    },
                )
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(product.tradeName, style = MaterialTheme.typography.bodyLarge)
                    OutlinedButton(onClick = { species?.let { onNavigateToProductSearch(it.id) } }) {
                        Text(stringResource(R.string.calculate_change_product))
                    }
                }
            }
        }

        if (uiState.noDoseRuleError) {
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = WarningCaution.copy(alpha = 0.15f))) {
                Text(
                    stringResource(R.string.calculate_no_dose_rule),
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = viewModel::onCalculateClicked,
            enabled = uiState.selectedSpecies != null && uiState.selectedProduct != null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) {
            Text(stringResource(R.string.calculate_button), style = MaterialTheme.typography.titleMedium)
        }
    }

    val pendingWeight = uiState.pendingWeightConfirmation
    val species = uiState.selectedSpecies
    if (pendingWeight != null && species != null) {
        AlertDialog(
            onDismissRequest = viewModel::onWeightConfirmationDismissed,
            title = { Text(stringResource(R.string.calculate_weight_out_of_range_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.calculate_weight_out_of_range_body,
                        formatNumber(pendingWeight),
                        species.nameUk,
                        formatNumber(species.typicalMinWeightKg),
                        formatNumber(species.typicalMaxWeightKg),
                    ),
                )
            },
            confirmButton = {
                Button(onClick = viewModel::onWeightConfirmed) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                OutlinedButton(onClick = viewModel::onWeightConfirmationDismissed) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun SpeciesRow(species: List<Species>, selected: Species?, onSelected: (Species) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(species, key = { it.id }) { item ->
            FilterChip(
                selected = selected?.id == item.id,
                onClick = { onSelected(item) },
                label = { Text("${speciesEmoji(item.code)} ${item.nameUk}") },
                modifier = Modifier.heightIn(min = 56.dp).wrapContentHeight(),
            )
        }
    }
}

@Composable
private fun FirstSyncNeeded(onNavigateToSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.calculate_first_sync_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.calculate_first_sync_body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onNavigateToSettings, modifier = Modifier.heightIn(min = 56.dp)) {
            Text(stringResource(R.string.calculate_first_sync_go_to_settings))
        }
    }
}
