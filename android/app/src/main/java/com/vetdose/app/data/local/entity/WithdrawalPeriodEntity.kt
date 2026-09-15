package com.vetdose.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "withdrawal_period",
    indices = [Index("productId"), Index("speciesId")],
)
data class WithdrawalPeriodEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val speciesId: String,
    val route: String,
    val foodProduct: String,
    val days: Int,
    val hours: Int?,
    val source: String,
)
