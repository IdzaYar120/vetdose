package com.vetdose.app.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.vetdose.app.R
import com.vetdose.app.domain.model.FoodProduct
import com.vetdose.app.domain.model.Route

@Composable
fun Route.label(): String = stringResource(
    when (this) {
        Route.IV -> R.string.route_iv
        Route.IM -> R.string.route_im
        Route.SC -> R.string.route_sc
        Route.PO -> R.string.route_po
        Route.TOPICAL -> R.string.route_topical
        Route.OTHER -> R.string.route_other
    },
)

@Composable
fun FoodProduct.label(): String = stringResource(
    when (this) {
        FoodProduct.MEAT -> R.string.food_product_meat
        FoodProduct.MILK -> R.string.food_product_milk
        FoodProduct.EGGS -> R.string.food_product_eggs
        FoodProduct.HONEY -> R.string.food_product_honey
    },
)

/** Maps DoseCalculator's machine-readable unit codes ("ml", "tablets", "g",
 * "mg", "mcg", "iu") to their Ukrainian display label. */
@Composable
fun administrationUnitLabel(unit: String): String = stringResource(
    when (unit) {
        "ml" -> R.string.unit_ml
        "tablets" -> R.string.unit_tablets
        "g" -> R.string.unit_g
        "mg" -> R.string.unit_mg
        "mcg" -> R.string.unit_mcg
        "iu" -> R.string.unit_iu
        else -> R.string.unit_ml
    },
)
