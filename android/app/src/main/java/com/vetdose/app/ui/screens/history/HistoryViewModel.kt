package com.vetdose.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vetdose.app.data.repository.HistoryRepository
import com.vetdose.app.data.repository.ProductRepository
import com.vetdose.app.data.repository.SpeciesRepository
import com.vetdose.app.domain.model.CalculationHistoryEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HistoryItemUi(
    val entry: CalculationHistoryEntry,
    val speciesName: String,
    val productName: String,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    historyRepository: HistoryRepository,
    private val speciesRepository: SpeciesRepository,
    private val productRepository: ProductRepository,
) : ViewModel() {

    val items: StateFlow<List<HistoryItemUi>> = historyRepository.observeRecent()
        .map { entries ->
            entries.mapNotNull { entry ->
                val species = speciesRepository.getById(entry.speciesId) ?: return@mapNotNull null
                val product = productRepository.getById(entry.productId) ?: return@mapNotNull null
                HistoryItemUi(entry = entry, speciesName = species.nameUk, productName = product.tradeName)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
