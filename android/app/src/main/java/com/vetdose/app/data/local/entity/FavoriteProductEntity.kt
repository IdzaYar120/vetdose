package com.vetdose.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Local-only "starred" marker for a product, shown atop product search. */
@Entity(tableName = "favorite_product")
data class FavoriteProductEntity(
    @PrimaryKey val productId: String,
    val addedAt: Long,
)
