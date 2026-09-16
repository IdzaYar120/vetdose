package com.vetdose.app.ui.screens.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vetdose.app.data.repository.FavoriteRepository
import com.vetdose.app.data.repository.ProductRepository
import com.vetdose.app.data.repository.SpeciesRepository
import com.vetdose.app.domain.model.Product
import com.vetdose.app.ui.navigation.VetDoseDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProductSearchUiState(
    val query: String = "",
    val speciesName: String? = null,
    val speciesFilterEnabled: Boolean = true,
    val favorites: List<Product> = emptyList(),
    val others: List<Product> = emptyList(),
    val loaded: Boolean = false,
) {
    val isEmpty: Boolean get() = loaded && favorites.isEmpty() && others.isEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProductSearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productRepository: ProductRepository,
    private val favoriteRepository: FavoriteRepository,
    speciesRepository: SpeciesRepository,
) : ViewModel() {

    private val speciesId: String = checkNotNull(savedStateHandle[VetDoseDestinations.ARG_SPECIES_ID])

    private val query = MutableStateFlow("")
    private val speciesFilterEnabled = MutableStateFlow(true)
    private val speciesName = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ProductSearchUiState> =
        combine(query, speciesFilterEnabled) { q, filterEnabled -> q to filterEnabled }
            .flatMapLatest { (q, filterEnabled) ->
                val effectiveSpeciesId = if (filterEnabled) speciesId else null
                combine(
                    productRepository.search(query = q.ifBlank { null }, speciesId = effectiveSpeciesId),
                    favoriteRepository.observeAll(),
                    speciesName,
                ) { products, favorites, name ->
                    val favoriteIds = favorites.map { it.productId }.toSet()
                    val (favs, others) = products.partition { it.id in favoriteIds }
                    ProductSearchUiState(
                        query = q,
                        speciesName = name,
                        speciesFilterEnabled = filterEnabled,
                        favorites = favs,
                        others = others,
                        loaded = true,
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProductSearchUiState())

    init {
        viewModelScope.launch {
            speciesName.value = speciesRepository.getById(speciesId)?.nameUk
        }
    }

    fun onQueryChanged(text: String) {
        query.value = text
    }

    fun onSpeciesFilterToggled(enabled: Boolean) {
        speciesFilterEnabled.value = enabled
    }

    fun onFavoriteToggled(productId: String, isCurrentlyFavorite: Boolean) {
        viewModelScope.launch {
            favoriteRepository.setFavorite(productId, !isCurrentlyFavorite)
        }
    }
}
