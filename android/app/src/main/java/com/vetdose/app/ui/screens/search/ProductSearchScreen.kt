package com.vetdose.app.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vetdose.app.R
import com.vetdose.app.domain.model.Product

@Composable
fun ProductSearchScreen(
    onProductChosen: (Product) -> Unit,
    viewModel: ProductSearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = uiState.query,
            onValueChange = viewModel::onQueryChanged,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        )
        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 8.dp))
        if (uiState.speciesName != null) {
            FilterChip(
                selected = uiState.speciesFilterEnabled,
                onClick = { viewModel.onSpeciesFilterToggled(!uiState.speciesFilterEnabled) },
                label = {
                    Text(
                        if (uiState.speciesFilterEnabled) {
                            stringResource(R.string.search_filter_species, uiState.speciesName ?: "")
                        } else {
                            stringResource(R.string.search_filter_all_species)
                        },
                    )
                },
                modifier = Modifier.heightIn(min = 40.dp),
            )
        }

        if (uiState.isEmpty) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.search_no_results), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (uiState.favorites.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.search_favorites_section)) }
                    items(uiState.favorites, key = { "fav_${it.id}" }) { product ->
                        ProductRow(
                            product = product,
                            isFavorite = true,
                            onClick = { onProductChosen(product) },
                            onFavoriteToggle = { viewModel.onFavoriteToggled(product.id, true) },
                        )
                    }
                }
                if (uiState.others.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.search_all_section)) }
                    items(uiState.others, key = { "all_${it.id}" }) { product ->
                        ProductRow(
                            product = product,
                            isFavorite = false,
                            onClick = { onProductChosen(product) },
                            onFavoriteToggle = { viewModel.onFavoriteToggled(product.id, false) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun ProductRow(
    product: Product,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp).heightIn(min = 56.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.tradeName, style = MaterialTheme.typography.bodyLarge)
                if (product.manufacturer != null) {
                    Text(
                        product.manufacturer,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onFavoriteToggle) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = stringResource(
                        if (isFavorite) R.string.search_favorite_remove else R.string.search_favorite_add,
                    ),
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
