package com.vetdose.app.domain.model

data class WithdrawalPeriod(
    val id: String,
    val productId: String,
    val speciesId: String,
    val route: Route,
    val foodProduct: FoodProduct,
    val days: Int,
    val hours: Int?,
    val source: String,
)
