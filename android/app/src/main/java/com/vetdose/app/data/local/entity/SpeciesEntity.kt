package com.vetdose.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room's local mirror of the backend's `species` table. Deleted rows are
 * removed outright rather than kept with an `is_deleted` flag — the app only
 * ever reads active rows, so there is nothing to gain from keeping tombstones
 * around locally (see [com.vetdose.app.data.repository.SyncRepository]).
 *
 * Decimal fields are stored as their exact wire-format `String` (never
 * Float/Double) and parsed to [java.math.BigDecimal] at the domain boundary.
 */
@Entity(tableName = "species")
data class SpeciesEntity(
    @PrimaryKey val id: String,
    val code: String,
    val nameUk: String,
    val isFoodProducing: Boolean,
    val typicalMinWeightKg: String,
    val typicalMaxWeightKg: String,
)
