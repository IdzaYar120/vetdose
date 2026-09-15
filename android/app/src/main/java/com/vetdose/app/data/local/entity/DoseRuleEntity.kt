package com.vetdose.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dose_rule",
    indices = [Index("substanceId"), Index("speciesId")],
)
data class DoseRuleEntity(
    @PrimaryKey val id: String,
    val substanceId: String,
    val speciesId: String,
    val route: String,
    val indication: String?,
    val doseMin: String,
    val doseMax: String,
    val doseUnit: String,
    val maxTotalDose: String?,
    val maxTotalDoseUnit: String?,
    val frequency: String?,
    val duration: String?,
    val notes: String?,
    val source: String,
    val isVerified: Boolean,
)
