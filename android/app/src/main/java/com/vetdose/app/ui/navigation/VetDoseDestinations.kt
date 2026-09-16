package com.vetdose.app.ui.navigation

/** Route names and argument keys for [androidx.navigation.NavHost]. */
object VetDoseDestinations {
    const val CALCULATE = "calculate"
    const val HISTORY = "history"
    const val SETTINGS = "settings"

    const val ARG_SPECIES_ID = "speciesId"
    const val ARG_DOSE_RULE_ID = "doseRuleId"
    const val ARG_PRODUCT_ID = "productId"
    const val ARG_WEIGHT_KG = "weightKg"

    const val PRODUCT_SEARCH_ROUTE = "product_search/{$ARG_SPECIES_ID}"
    fun productSearch(speciesId: String) = "product_search/$speciesId"

    const val RESULT_ROUTE =
        "result/{$ARG_SPECIES_ID}/{$ARG_DOSE_RULE_ID}/{$ARG_PRODUCT_ID}/{$ARG_WEIGHT_KG}"

    fun result(speciesId: String, doseRuleId: String, productId: String, weightKg: String) =
        "result/$speciesId/$doseRuleId/$productId/$weightKg"
}
