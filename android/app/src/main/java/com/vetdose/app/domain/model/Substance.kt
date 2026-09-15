package com.vetdose.app.domain.model

data class Substance(
    val id: String,
    val name: String,
    val nameUk: String,
    val pharmacologicalGroup: String?,
    val notes: String?,
)
