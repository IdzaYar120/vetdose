package com.vetdose.app.ui.screens.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vetdose.app.data.repository.ContraindicationRepository
import com.vetdose.app.data.repository.DoseRuleRepository
import com.vetdose.app.data.repository.HistoryRepository
import com.vetdose.app.data.repository.ProductRepository
import com.vetdose.app.data.repository.SpeciesRepository
import com.vetdose.app.data.repository.WithdrawalPeriodRepository
import com.vetdose.app.domain.calculator.CalculationInput
import com.vetdose.app.domain.calculator.CalculationResult
import com.vetdose.app.domain.calculator.CalculatorError
import com.vetdose.app.domain.calculator.ContraindicationInput
import com.vetdose.app.domain.calculator.DoseRuleInput
import com.vetdose.app.domain.calculator.ProductInput
import com.vetdose.app.domain.calculator.SpeciesInput
import com.vetdose.app.domain.calculator.calculateDose
import com.vetdose.app.domain.model.DoseRule
import com.vetdose.app.domain.model.Product
import com.vetdose.app.domain.model.Species
import com.vetdose.app.domain.model.WithdrawalPeriod
import com.vetdose.app.ui.navigation.VetDoseDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResultUiState(
    val loading: Boolean = true,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val species: Species? = null,
    val product: Product? = null,
    val doseRule: DoseRule? = null,
    val result: CalculationResult? = null,
    val withdrawalPeriods: List<WithdrawalPeriod> = emptyList(),
    val absoluteContraindicationConfirmed: Boolean = false,
    val explanationExpanded: Boolean = false,
) {
    val hasAbsoluteContraindication: Boolean
        get() = result?.warnings.orEmpty().any { it.code == "ABSOLUTE_CONTRAINDICATION" }

    val doseIsRevealed: Boolean
        get() = !hasAbsoluteContraindication || absoluteContraindicationConfirmed
}

@HiltViewModel
class ResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val speciesRepository: SpeciesRepository,
    private val doseRuleRepository: DoseRuleRepository,
    private val productRepository: ProductRepository,
    private val contraindicationRepository: ContraindicationRepository,
    private val withdrawalPeriodRepository: WithdrawalPeriodRepository,
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    private val speciesId: String = checkNotNull(savedStateHandle[VetDoseDestinations.ARG_SPECIES_ID])
    private val doseRuleId: String = checkNotNull(savedStateHandle[VetDoseDestinations.ARG_DOSE_RULE_ID])
    private val productId: String = checkNotNull(savedStateHandle[VetDoseDestinations.ARG_PRODUCT_ID])
    private val weightKg: BigDecimal = BigDecimal(checkNotNull(savedStateHandle[VetDoseDestinations.ARG_WEIGHT_KG] as String?))

    private val _uiState = MutableStateFlow(ResultUiState())
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val species = speciesRepository.getById(speciesId)
            val doseRule = doseRuleRepository.getById(doseRuleId)
            val product = productRepository.getById(productId)

            if (species == null || doseRule == null || product == null) {
                _uiState.update {
                    it.copy(loading = false, errorCode = "NOT_FOUND", errorMessage = "Дані не знайдено. Спробуйте синхронізувати базу ще раз.")
                }
                return@launch
            }

            val contraindications = contraindicationRepository.observeBySubstance(doseRule.substanceId).first()
                .filter { it.speciesId == null || it.speciesId == species.id }
            val withdrawalPeriods = withdrawalPeriodRepository.observeByProduct(product.id).first()
                .filter { it.speciesId == species.id }

            val input = CalculationInput(
                weightKg = weightKg,
                species = SpeciesInput(
                    isFoodProducing = species.isFoodProducing,
                    typicalMinWeightKg = species.typicalMinWeightKg,
                    typicalMaxWeightKg = species.typicalMaxWeightKg,
                ),
                doseRule = DoseRuleInput(
                    doseMin = doseRule.doseMin,
                    doseMax = doseRule.doseMax,
                    doseUnit = doseRule.doseUnit,
                    isVerified = doseRule.isVerified,
                    maxTotalDose = doseRule.maxTotalDose,
                    maxTotalDoseUnit = doseRule.maxTotalDoseUnit,
                ),
                product = ProductInput(
                    concentrationValue = product.concentrationValue,
                    concentrationUnit = product.concentrationUnit,
                    tabletDivisibleBy = product.tabletDivisibleBy,
                ),
                contraindications = contraindications.map {
                    ContraindicationInput(severity = it.severity, messageUk = it.messageUk)
                },
            )

            try {
                val result = calculateDose(input)
                _uiState.update {
                    it.copy(
                        loading = false,
                        species = species,
                        product = product,
                        doseRule = doseRule,
                        result = result,
                        withdrawalPeriods = withdrawalPeriods,
                    )
                }
                historyRepository.record(species.id, doseRule.id, product.id, weightKg)
            } catch (e: CalculatorError) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        errorCode = e.code,
                        errorMessage = e.message,
                        species = species,
                        product = product,
                        doseRule = doseRule,
                    )
                }
            }
        }
    }

    fun onAbsoluteContraindicationConfirmed() {
        _uiState.update { it.copy(absoluteContraindicationConfirmed = true) }
    }

    fun onExplanationToggled() {
        _uiState.update { it.copy(explanationExpanded = !it.explanationExpanded) }
    }
}
