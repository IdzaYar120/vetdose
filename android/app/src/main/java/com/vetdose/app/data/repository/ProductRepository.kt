package com.vetdose.app.data.repository

import com.vetdose.app.data.local.dao.ProductDao
import com.vetdose.app.data.local.dao.SubstanceDao
import com.vetdose.app.data.mapper.toDomain
import com.vetdose.app.domain.model.Product
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
class ProductRepository @Inject constructor(
    private val productDao: ProductDao,
    private val substanceDao: SubstanceDao,
) {
    /**
     * Mirrors the backend's `GET /products?search=&species_id=`, but runs
     * entirely against Room: [query] matches trade name or substance name
     * (Kotlin's `contains(ignoreCase = true)`, not SQLite `LIKE`, so Cyrillic
     * case-folding actually works), [speciesId] narrows to products whose
     * substance has a dose rule for that species.
     */
    fun search(query: String? = null, speciesId: String? = null): Flow<List<Product>> {
        val productsFlow = if (speciesId != null) {
            productDao.observeBySpecies(speciesId)
        } else {
            productDao.observeAll()
        }
        return combine(productsFlow, substanceDao.observeAll()) { products, substances ->
            val substanceById = substances.associateBy { it.id }
            products
                .filter { product ->
                    if (query.isNullOrBlank()) {
                        true
                    } else {
                        val substance = substanceById[product.substanceId]
                        product.tradeName.contains(query, ignoreCase = true) ||
                            substance?.nameUk?.contains(query, ignoreCase = true) == true ||
                            substance?.name?.contains(query, ignoreCase = true) == true
                    }
                }
                .map { it.toDomain() }
        }
    }

    suspend fun getById(id: String): Product? = productDao.getById(id)?.toDomain()
}
