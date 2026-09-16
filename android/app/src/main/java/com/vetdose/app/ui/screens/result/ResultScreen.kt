package com.vetdose.app.ui.screens.result

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vetdose.app.R
import com.vetdose.app.domain.calculator.CalculationResult
import com.vetdose.app.domain.calculator.formatNumber
import com.vetdose.app.domain.model.WithdrawalPeriod
import com.vetdose.app.ui.components.WarningCard
import com.vetdose.app.ui.theme.WarningAbsolute
import com.vetdose.app.ui.util.administrationUnitLabel
import com.vetdose.app.ui.util.label
import com.vetdose.app.ui.util.toDisplayString
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ResultScreen(viewModel: ResultViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        uiState.errorMessage != null -> ErrorContent(uiState.errorMessage!!)
        uiState.hasAbsoluteContraindication && !uiState.doseIsRevealed ->
            AbsoluteContraindicationGate(uiState, viewModel::onAbsoluteContraindicationConfirmed)
        else -> ResultContent(uiState, viewModel::onExplanationToggled)
    }
}

@Composable
private fun ErrorContent(message: String) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text(stringResource(R.string.result_error_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
internal fun AbsoluteContraindicationGate(uiState: ResultUiState, onConfirm: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = WarningAbsolute.copy(alpha = 0.15f))) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.result_absolute_contraindication_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = WarningAbsolute,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                uiState.result?.warnings?.filter { it.code == "ABSOLUTE_CONTRAINDICATION" }?.forEach { warning ->
                    Text(warning.messageUk, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(stringResource(R.string.result_absolute_contraindication_confirm))
        }
    }
}

@Composable
internal fun ResultContent(uiState: ResultUiState, onExplanationToggled: () -> Unit) {
    val result = uiState.result ?: return
    val doseRule = uiState.doseRule
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { AdministrationAmount(result) }

        if (result.doseMin != null && result.doseMax != null && result.doseAmountUnit != null) {
            item { DoseRangeRow(result) }
        }

        item {
            Column {
                if (doseRule != null) {
                    LabeledValue(stringResource(R.string.result_route_label), doseRule.route.label())
                    doseRule.frequency?.let { LabeledValue(stringResource(R.string.result_frequency_label), it) }
                    doseRule.duration?.let { LabeledValue(stringResource(R.string.result_duration_label), it) }
                }
            }
        }

        items(result.warnings, key = { it.code + it.messageUk.hashCode() }) { warning ->
            WarningCard(warning, modifier = Modifier.fillMaxWidth())
        }

        if (uiState.withdrawalPeriods.isNotEmpty()) {
            item { WithdrawalSection(uiState.withdrawalPeriods) }
        }

        item { ExplanationSection(result, uiState.explanationExpanded, onExplanationToggled) }

        if (doseRule != null) {
            item { LabeledValue(stringResource(R.string.result_source_label), doseRule.source) }
        }

        item {
            Text(
                stringResource(R.string.result_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AdministrationAmount(result: CalculationResult) {
    val range = if (result.administrationMin.compareTo(result.administrationMax) == 0) {
        result.administrationMin.toDisplayString()
    } else {
        "${result.administrationMin.toDisplayString()}–${result.administrationMax.toDisplayString()}"
    }
    Text(
        "$range ${administrationUnitLabel(result.administrationUnit)}",
        style = MaterialTheme.typography.displayLarge,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun DoseRangeRow(result: CalculationResult) {
    val doseMin = result.doseMin ?: return
    val doseMax = result.doseMax ?: return
    val unit = result.doseAmountUnit ?: return
    val range = if (doseMin.compareTo(doseMax) == 0) {
        formatNumber(doseMin)
    } else {
        "${formatNumber(doseMin)}–${formatNumber(doseMax)}"
    }
    LabeledValue(stringResource(R.string.result_dose_label), "$range ${administrationUnitLabel(unit)}")
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun WithdrawalSection(periods: List<WithdrawalPeriod>) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.result_withdrawal_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
            periods.forEach { period ->
                val safeFrom = LocalDate.now().plusDays(period.days.toLong())
                Text(
                    stringResource(
                        R.string.result_withdrawal_until,
                        period.foodProduct.label(),
                        safeFrom.format(formatter),
                        period.days,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ExplanationSection(result: CalculationResult, expanded: Boolean, onToggle: () -> Unit) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.result_explanation_toggle), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onToggle) {
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 8.dp)) {
                    result.explanation.forEach { step ->
                        Text("• $step", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }
    }
}
