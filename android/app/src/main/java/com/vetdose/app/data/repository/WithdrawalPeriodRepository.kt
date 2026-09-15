package com.vetdose.app.data.repository

import com.vetdose.app.data.local.dao.WithdrawalPeriodDao
import com.vetdose.app.data.mapper.toDomain
import com.vetdose.app.domain.model.WithdrawalPeriod
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class WithdrawalPeriodRepository @Inject constructor(private val withdrawalPeriodDao: WithdrawalPeriodDao) {
    fun observeByProduct(productId: String): Flow<List<WithdrawalPeriod>> =
        withdrawalPeriodDao.observeByProduct(productId).map { list -> list.map { it.toDomain() } }
}
