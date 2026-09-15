package com.vetdose.app.domain.model

import com.vetdose.app.domain.calculator.ConcentrationUnit
import java.math.BigDecimal

// Reuses domain.calculator.ConcentrationUnit rather than duplicating it: the
// value read from Room/the API is the exact same thing later fed straight
// into DoseCalculator's ProductInput.
data class Product(
    val id: String,
    val tradeName: String,
    val manufacturer: String?,
    val substanceId: String,
    val form: ProductForm,
    val concentrationValue: BigDecimal,
    val concentrationUnit: ConcentrationUnit,
    val tabletDivisibleBy: Int?,
)
