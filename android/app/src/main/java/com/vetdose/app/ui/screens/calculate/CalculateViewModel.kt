package com.vetdose.app.ui.screens.calculate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vetdose.app.data.repository.DoseRuleRepository
import com.vetdose.app.data.repository.ProductRepository
import com.vetdose.app.data.repository.SpeciesRepository
import com.vetdose.app.domain.model.Product
import com.vetdose.app.domain.model.Species
import com.vetdose.app.ui.util.parseWeightInput
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalculateUiState(
    val speciesList: List<Species> = emptyList(),
    val speciesLoaded: Boolean = false,
    val selectedSpecies: Species? = null,
    val weightInput: String = "",
    val weightError: Boolean = false,
    val selectedProduct: Product? = null,
    val pendingWeightConfirmation: BigDecimal? = null,
    val noDoseRuleError: Boolean = false,
) {
    val needsFirstSync: Boolean get() = speciesLoaded && speciesList.isEmpty()
}

private data class FormState(
    val selectedSpecies: Species? = null,
    val weightInput: String = "",
    val weightError: Boolean = false,
    val selectedProduct: Product? = null,
    val pendingWeightConfirmation: BigDecimal? = null,
    val noDoseRuleError: Boolean = false,
)

sealed interface CalculateEvent {
    data class NavigateToResult(
        val speciesId: String,
        val doseRuleId: String,
        val productId: String,
        val weightKg: String,
    ) : CalculateEvent
}

@HiltViewModel
class CalculateViewModel @Inject constructor(
    speciesRepository: SpeciesRepository,
    private val doseRuleRepository: DoseRuleRepository,
    private val productRepository: ProductRepository,
) : ViewModel() {

    private val formState = MutableStateFlow(FormState())
    private val speciesListFlow: Flow<List<Species>> = speciesRepository.observeAll()

    val uiState: StateFlow<CalculateUiState> = combine(speciesListFlow, formState) { species, form ->
        CalculateUiState(
            speciesList = species,
            speciesLoaded = true,
            selectedSpecies = form.selectedSpecies,
            weightInput = form.weightInput,
            weightError = form.weightError,
            selectedProduct = form.selectedProduct,
            pendingWeightConfirmation = form.pendingWeightConfirmation,
            noDoseRuleError = form.noDoseRuleError,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalculateUiState())

    private val eventChannel = Channel<CalculateEvent>(Channel.BUFFERED)
    val events: Flow<CalculateEvent> = eventChannel.receiveAsFlow()

    fun onSpeciesSelected(species: Species) {
        formState.update { it.copy(selectedSpecies = species, selectedProduct = null, noDoseRuleError = false) }
    }

    fun onWeightInputChanged(text: String) {
        formState.update { it.copy(weightInput = text, weightError = false, noDoseRuleError = false) }
    }

    fun onProductSelected(product: Product) {
        formState.update { it.copy(selectedProduct = product, noDoseRuleError = false) }
    }

    /** Resolves a product id coming back from the search screen's saved-state
     * result and applies it, same as [onProductSelected]. */
    fun onProductIdSelected(productId: String) {
        viewModelScope.launch {
            productRepository.getById(productId)?.let { onProductSelected(it) }
        }
    }

    fun onCalculateClicked() {
        val form = formState.value
        val species = form.selectedSpecies ?: return
        val product = form.selectedProduct ?: return
        val weight = parseWeightInput(form.weightInput)
        if (weight == null) {
            formState.update { it.copy(weightError = true) }
            return
        }
        val outOfRange = weight < species.typicalMinWeightKg || weight > species.typicalMaxWeightKg
        if (outOfRange) {
            formState.update { it.copy(pendingWeightConfirmation = weight) }
            return
        }
        proceedToResult(species, product, weight)
    }

    fun onWeightConfirmed() {
        val form = formState.value
        val species = form.selectedSpecies ?: return
        val product = form.selectedProduct ?: return
        val weight = form.pendingWeightConfirmation ?: return
        formState.update { it.copy(pendingWeightConfirmation = null) }
        proceedToResult(species, product, weight)
    }

    fun onWeightConfirmationDismissed() {
        formState.update { it.copy(pendingWeightConfirmation = null) }
    }

    private fun proceedToResult(species: Species, product: Product, weight: BigDecimal) {
        viewModelScope.launch {
            val doseRule = doseRuleRepository.findForSubstanceAndSpecies(product.substanceId, species.id)
            if (doseRule == null) {
                formState.update { it.copy(noDoseRuleError = true) }
                return@launch
            }
            eventChannel.send(
                CalculateEvent.NavigateToResult(
                    speciesId = species.id,
                    doseRuleId = doseRule.id,
                    productId = product.id,
                    weightKg = weight.toString(),
                ),
            )
        }
    }
}
