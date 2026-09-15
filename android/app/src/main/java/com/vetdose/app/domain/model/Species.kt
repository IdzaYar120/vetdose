package com.vetdose.app.domain.model

import java.math.BigDecimal

data class Species(
    val id: String,
    val code: String,
    val nameUk: String,
    val isFoodProducing: Boolean,
    val typicalMinWeightKg: BigDecimal,
    val typicalMaxWeightKg: BigDecimal,
)
