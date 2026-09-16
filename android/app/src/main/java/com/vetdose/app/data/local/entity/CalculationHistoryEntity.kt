package com.vetdose.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A past calculation the vet ran, stored locally only — never synced to the
 * server. We store the *inputs*, not the computed result: DoseCalculator is
 * pure and fast, so re-running it on display keeps history entries correct
 * even if the underlying dose_rule/product changed since (and avoids
 * duplicating the result-formatting logic here).
 */
@Entity(tableName = "calculation_history")
data class CalculationHistoryEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val speciesId: String,
    val doseRuleId: String,
    val productId: String,
    val weightKg: String,
)
