package com.vetdose.app.domain.model

import java.time.Instant

data class FavoriteProduct(
    val productId: String,
    val addedAt: Instant,
)
