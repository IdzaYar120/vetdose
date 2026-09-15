package com.vetdose.app.data.mapper

import com.vetdose.app.data.local.entity.ContraindicationEntity
import com.vetdose.app.data.local.entity.DoseRuleEntity
import com.vetdose.app.data.local.entity.ProductEntity
import com.vetdose.app.data.local.entity.SpeciesEntity
import com.vetdose.app.data.local.entity.SubstanceEntity
import com.vetdose.app.data.local.entity.WithdrawalPeriodEntity
import com.vetdose.app.data.remote.dto.ContraindicationDto
import com.vetdose.app.data.remote.dto.DoseRuleDto
import com.vetdose.app.data.remote.dto.ProductDto
import com.vetdose.app.data.remote.dto.SpeciesDto
import com.vetdose.app.data.remote.dto.SubstanceDto
import com.vetdose.app.data.remote.dto.WithdrawalPeriodDto

fun SpeciesDto.toEntity(): SpeciesEntity = SpeciesEntity(
    id = id,
    code = code,
    nameUk = nameUk,
    isFoodProducing = isFoodProducing,
    typicalMinWeightKg = typicalMinWeightKg,
    typicalMaxWeightKg = typicalMaxWeightKg,
)

fun SubstanceDto.toEntity(): SubstanceEntity = SubstanceEntity(
    id = id,
    name = name,
    nameUk = nameUk,
    pharmacologicalGroup = pharmacologicalGroup,
    notes = notes,
)

fun ProductDto.toEntity(): ProductEntity = ProductEntity(
    id = id,
    tradeName = tradeName,
    manufacturer = manufacturer,
    substanceId = substanceId,
    form = form,
    concentrationValue = concentrationValue,
    concentrationUnit = concentrationUnit,
    tabletDivisibleBy = tabletDivisibleBy,
)

fun DoseRuleDto.toEntity(): DoseRuleEntity = DoseRuleEntity(
    id = id,
    substanceId = substanceId,
    speciesId = speciesId,
    route = route,
    indication = indication,
    doseMin = doseMin,
    doseMax = doseMax,
    doseUnit = doseUnit,
    maxTotalDose = maxTotalDose,
    maxTotalDoseUnit = maxTotalDoseUnit,
    frequency = frequency,
    duration = duration,
    notes = notes,
    source = source,
    isVerified = isVerified,
)

fun ContraindicationDto.toEntity(): ContraindicationEntity = ContraindicationEntity(
    id = id,
    substanceId = substanceId,
    speciesId = speciesId,
    condition = condition,
    severity = severity,
    messageUk = messageUk,
    source = source,
)

fun WithdrawalPeriodDto.toEntity(): WithdrawalPeriodEntity = WithdrawalPeriodEntity(
    id = id,
    productId = productId,
    speciesId = speciesId,
    route = route,
    foodProduct = foodProduct,
    days = days,
    hours = hours,
    source = source,
)
