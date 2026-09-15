package com.vetdose.app.domain.model

import com.vetdose.app.domain.calculator.DoseUnit
import com.vetdose.app.domain.calculator.MaxTotalDoseUnit
import java.math.BigDecimal

data class DoseRule(
    val id: String,
    val substanceId: String,
    val speciesId: String,
    val route: Route,
    val indication: String?,
    val doseMin: BigDecimal,
    val doseMax: BigDecimal,
    val doseUnit: DoseUnit,
    val maxTotalDose: BigDecimal?,
    val maxTotalDoseUnit: MaxTotalDoseUnit?,
    val frequency: String?,
    val duration: String?,
    val notes: String?,
    val source: String,
    val isVerified: Boolean,
)
