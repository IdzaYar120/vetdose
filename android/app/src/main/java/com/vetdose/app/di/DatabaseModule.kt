package com.vetdose.app.di

import android.content.Context
import androidx.room.Room
import com.vetdose.app.data.local.VetDoseDatabase
import com.vetdose.app.data.local.dao.ContraindicationDao
import com.vetdose.app.data.local.dao.DoseRuleDao
import com.vetdose.app.data.local.dao.ProductDao
import com.vetdose.app.data.local.dao.SpeciesDao
import com.vetdose.app.data.local.dao.SubstanceDao
import com.vetdose.app.data.local.dao.WithdrawalPeriodDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VetDoseDatabase =
        Room.databaseBuilder(context, VetDoseDatabase::class.java, VetDoseDatabase.DATABASE_NAME).build()

    @Provides
    fun provideSpeciesDao(database: VetDoseDatabase): SpeciesDao = database.speciesDao()

    @Provides
    fun provideSubstanceDao(database: VetDoseDatabase): SubstanceDao = database.substanceDao()

    @Provides
    fun provideProductDao(database: VetDoseDatabase): ProductDao = database.productDao()

    @Provides
    fun provideDoseRuleDao(database: VetDoseDatabase): DoseRuleDao = database.doseRuleDao()

    @Provides
    fun provideContraindicationDao(database: VetDoseDatabase): ContraindicationDao = database.contraindicationDao()

    @Provides
    fun provideWithdrawalPeriodDao(database: VetDoseDatabase): WithdrawalPeriodDao = database.withdrawalPeriodDao()
}
