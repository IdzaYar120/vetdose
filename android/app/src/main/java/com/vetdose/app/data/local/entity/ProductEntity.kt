package com.vetdose.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "product", indices = [Index("substanceId")])
data class ProductEntity(
    @PrimaryKey val id: String,
    val tradeName: String,
    val manufacturer: String?,
    val substanceId: String,
    val form: String,
    val concentrationValue: String,
    val concentrationUnit: String,
    val tabletDivisibleBy: Int?,
)
