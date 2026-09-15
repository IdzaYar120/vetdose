package com.vetdose.app.domain.model

/** Mirrors the backend's `app/models/enums.py`. Kept separate from
 * `domain.calculator`'s enums, which are scoped to the calculator's own
 * narrower protocol (some values, like [ProductForm], never reach the
 * calculator at all). */
enum class ProductForm(val value: String) {
    INJECTION_SOLUTION("injection_solution"),
    ORAL_SOLUTION("oral_solution"),
    TABLET("tablet"),
    POWDER("powder"),
    SUSPENSION("suspension"),
    OTHER("other"),
    ;

    companion object {
        fun fromValue(value: String): ProductForm = entries.first { it.value == value }
    }
}

enum class Route(val value: String) {
    IV("iv"),
    IM("im"),
    SC("sc"),
    PO("po"),
    TOPICAL("topical"),
    OTHER("other"),
    ;

    companion object {
        fun fromValue(value: String): Route = entries.first { it.value == value }
    }
}

enum class FoodProduct(val value: String) {
    MEAT("meat"),
    MILK("milk"),
    EGGS("eggs"),
    HONEY("honey"),
    ;

    companion object {
        fun fromValue(value: String): FoodProduct = entries.first { it.value == value }
    }
}
