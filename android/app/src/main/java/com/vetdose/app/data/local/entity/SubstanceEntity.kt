package com.vetdose.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "substance")
data class SubstanceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nameUk: String,
    val pharmacologicalGroup: String?,
    val notes: String?,
)
