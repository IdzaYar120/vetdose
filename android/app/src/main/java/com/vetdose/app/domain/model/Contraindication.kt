package com.vetdose.app.domain.model

import com.vetdose.app.domain.calculator.Severity

data class Contraindication(
    val id: String,
    val substanceId: String,
    val speciesId: String?,
    val condition: String?,
    val severity: Severity,
    val messageUk: String,
    val source: String,
)
