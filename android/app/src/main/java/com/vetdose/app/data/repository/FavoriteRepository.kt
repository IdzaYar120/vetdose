package com.vetdose.app.data.repository

import com.vetdose.app.data.local.dao.FavoriteProductDao
import com.vetdose.app.data.local.entity.FavoriteProductEntity
import com.vetdose.app.data.mapper.toDomain
import com.vetdose.app.domain.model.FavoriteProduct
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class FavoriteRepository @Inject constructor(private val favoriteProductDao: FavoriteProductDao) {
    fun observeAll(): Flow<List<FavoriteProduct>> =
        favoriteProductDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeIsFavorite(productId: String): Flow<Boolean> = favoriteProductDao.observeIsFavorite(productId)

    suspend fun setFavorite(productId: String, isFavorite: Boolean) {
        if (isFavorite) {
            favoriteProductDao.add(FavoriteProductEntity(productId = productId, addedAt = System.currentTimeMillis()))
        } else {
            favoriteProductDao.remove(productId)
        }
    }
}
