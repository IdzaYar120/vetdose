package com.vetdose.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contraindication",
    indices = [Index("substanceId"), Index("speciesId")],
)
data class ContraindicationEntity(
    @PrimaryKey val id: String,
    val substanceId: String,
    val speciesId: String?,
    val condition: String?,
    val severity: String,
    val messageUk: String,
    val source: String,
)
