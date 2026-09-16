package com.vetdose.app.domain.model

import java.math.BigDecimal
import java.time.Instant

data class CalculationHistoryEntry(
    val id: String,
    val timestamp: Instant,
    val speciesId: String,
    val doseRuleId: String,
    val productId: String,
    val weightKg: BigDecimal,
)
