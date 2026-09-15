package com.vetdose.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors `backend/app/schemas/sync.py` and friends exactly (field names via
 * [SerialName], decimal fields kept as `String` — the backend always
 * serializes `Decimal` as a JSON string, never a number, to avoid a float
 * round-trip; see `app/schemas/common.py:DecimalStr` on the backend).
 */

/** Lets SyncRepository upsert-or-delete all six entity lists with one generic helper. */
sealed interface SyncableDto {
    val id: String
    val isDeleted: Boolean
}

@Serializable
data class SyncResponseDto(
    @SerialName("server_time") val serverTime: String,
    val species: List<SpeciesDto>,
    val substances: List<SubstanceDto>,
    val products: List<ProductDto>,
    @SerialName("dose_rules") val doseRules: List<DoseRuleDto>,
    val contraindications: List<ContraindicationDto>,
    @SerialName("withdrawal_periods") val withdrawalPeriods: List<WithdrawalPeriodDto>,
)

@Serializable
data class SpeciesDto(
    override val id: String,
    val code: String,
    @SerialName("name_uk") val nameUk: String,
    @SerialName("is_food_producing") val isFoodProducing: Boolean,
    @SerialName("typical_min_weight_kg") val typicalMinWeightKg: String,
    @SerialName("typical_max_weight_kg") val typicalMaxWeightKg: String,
    @SerialName("is_deleted") override val isDeleted: Boolean,
) : SyncableDto

@Serializable
data class SubstanceDto(
    override val id: String,
    val name: String,
    @SerialName("name_uk") val nameUk: String,
    @SerialName("pharmacological_group") val pharmacologicalGroup: String? = null,
    val notes: String? = null,
    @SerialName("is_deleted") override val isDeleted: Boolean,
) : SyncableDto

@Serializable
data class ProductDto(
    override val id: String,
    @SerialName("trade_name") val tradeName: String,
    val manufacturer: String? = null,
    @SerialName("substance_id") val substanceId: String,
    val form: String,
    @SerialName("concentration_value") val concentrationValue: String,
    @SerialName("concentration_unit") val concentrationUnit: String,
    @SerialName("tablet_divisible_by") val tabletDivisibleBy: Int? = null,
    @SerialName("is_deleted") override val isDeleted: Boolean,
) : SyncableDto

@Serializable
data class DoseRuleDto(
    override val id: String,
    @SerialName("substance_id") val substanceId: String,
    @SerialName("species_id") val speciesId: String,
    val route: String,
    val indication: String? = null,
    @SerialName("dose_min") val doseMin: String,
    @SerialName("dose_max") val doseMax: String,
    @SerialName("dose_unit") val doseUnit: String,
    @SerialName("max_total_dose") val maxTotalDose: String? = null,
    @SerialName("max_total_dose_unit") val maxTotalDoseUnit: String? = null,
    val frequency: String? = null,
    val duration: String? = null,
    val notes: String? = null,
    val source: String,
    @SerialName("is_verified") val isVerified: Boolean,
    @SerialName("is_deleted") override val isDeleted: Boolean,
) : SyncableDto

@Serializable
data class ContraindicationDto(
    override val id: String,
    @SerialName("substance_id") val substanceId: String,
    @SerialName("species_id") val speciesId: String? = null,
    val condition: String? = null,
    val severity: String,
    @SerialName("message_uk") val messageUk: String,
    val source: String,
    @SerialName("is_deleted") override val isDeleted: Boolean,
) : SyncableDto

@Serializable
data class WithdrawalPeriodDto(
    override val id: String,
    @SerialName("product_id") val productId: String,
    @SerialName("species_id") val speciesId: String,
    val route: String,
    @SerialName("food_product") val foodProduct: String,
    val days: Int,
    val hours: Int? = null,
    val source: String,
    @SerialName("is_deleted") override val isDeleted: Boolean,
) : SyncableDto
