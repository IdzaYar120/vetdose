package com.vetdose.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vetdose.app.data.local.dao.CalculationHistoryDao
import com.vetdose.app.data.local.dao.ContraindicationDao
import com.vetdose.app.data.local.dao.DoseRuleDao
import com.vetdose.app.data.local.dao.FavoriteProductDao
import com.vetdose.app.data.local.dao.ProductDao
import com.vetdose.app.data.local.dao.SpeciesDao
import com.vetdose.app.data.local.dao.SubstanceDao
import com.vetdose.app.data.local.dao.WithdrawalPeriodDao
import com.vetdose.app.data.local.entity.CalculationHistoryEntity
import com.vetdose.app.data.local.entity.ContraindicationEntity
import com.vetdose.app.data.local.entity.DoseRuleEntity
import com.vetdose.app.data.local.entity.FavoriteProductEntity
import com.vetdose.app.data.local.entity.ProductEntity
import com.vetdose.app.data.local.entity.SpeciesEntity
import com.vetdose.app.data.local.entity.SubstanceEntity
import com.vetdose.app.data.local.entity.WithdrawalPeriodEntity
import androidx.room.migration.Migration

@Database(
    entities = [
        SpeciesEntity::class,
        SubstanceEntity::class,
        ProductEntity::class,
        DoseRuleEntity::class,
        ContraindicationEntity::class,
        WithdrawalPeriodEntity::class,
        CalculationHistoryEntity::class,
        FavoriteProductEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class VetDoseDatabase : RoomDatabase() {
    abstract fun speciesDao(): SpeciesDao
    abstract fun substanceDao(): SubstanceDao
    abstract fun productDao(): ProductDao
    abstract fun doseRuleDao(): DoseRuleDao
    abstract fun contraindicationDao(): ContraindicationDao
    abstract fun withdrawalPeriodDao(): WithdrawalPeriodDao
    abstract fun calculationHistoryDao(): CalculationHistoryDao
    abstract fun favoriteProductDao(): FavoriteProductDao

    companion object {
        const val DATABASE_NAME = "vetdose.db"

        /** Adds the two local-only tables (history, favorites) introduced in Stage 5. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `calculation_history` (
                        `id` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `speciesId` TEXT NOT NULL,
                        `doseRuleId` TEXT NOT NULL,
                        `productId` TEXT NOT NULL,
                        `weightKg` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `favorite_product` (
                        `productId` TEXT NOT NULL,
                        `addedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`productId`)
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}
