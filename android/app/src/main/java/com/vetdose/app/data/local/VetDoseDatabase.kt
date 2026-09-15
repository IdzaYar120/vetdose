package com.vetdose.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vetdose.app.data.local.dao.ContraindicationDao
import com.vetdose.app.data.local.dao.DoseRuleDao
import com.vetdose.app.data.local.dao.ProductDao
import com.vetdose.app.data.local.dao.SpeciesDao
import com.vetdose.app.data.local.dao.SubstanceDao
import com.vetdose.app.data.local.dao.WithdrawalPeriodDao
import com.vetdose.app.data.local.entity.ContraindicationEntity
import com.vetdose.app.data.local.entity.DoseRuleEntity
import com.vetdose.app.data.local.entity.ProductEntity
import com.vetdose.app.data.local.entity.SpeciesEntity
import com.vetdose.app.data.local.entity.SubstanceEntity
import com.vetdose.app.data.local.entity.WithdrawalPeriodEntity

@Database(
    entities = [
        SpeciesEntity::class,
        SubstanceEntity::class,
        ProductEntity::class,
        DoseRuleEntity::class,
        ContraindicationEntity::class,
        WithdrawalPeriodEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class VetDoseDatabase : RoomDatabase() {
    abstract fun speciesDao(): SpeciesDao
    abstract fun substanceDao(): SubstanceDao
    abstract fun productDao(): ProductDao
    abstract fun doseRuleDao(): DoseRuleDao
    abstract fun contraindicationDao(): ContraindicationDao
    abstract fun withdrawalPeriodDao(): WithdrawalPeriodDao

    companion object {
        const val DATABASE_NAME = "vetdose.db"
    }
}
