package com.vetdose.app.data.mapper

import com.vetdose.app.data.local.entity.ContraindicationEntity
import com.vetdose.app.data.local.entity.DoseRuleEntity
import com.vetdose.app.data.local.entity.ProductEntity
import com.vetdose.app.data.local.entity.SpeciesEntity
import com.vetdose.app.data.local.entity.SubstanceEntity
import com.vetdose.app.data.local.entity.WithdrawalPeriodEntity
import com.vetdose.app.domain.calculator.ConcentrationUnit
import com.vetdose.app.domain.calculator.DoseUnit
import com.vetdose.app.domain.calculator.MaxTotalDoseUnit
import com.vetdose.app.domain.calculator.Severity
import com.vetdose.app.domain.model.Contraindication
import com.vetdose.app.domain.model.DoseRule
import com.vetdose.app.domain.model.FoodProduct
import com.vetdose.app.domain.model.Product
import com.vetdose.app.domain.model.ProductForm
import com.vetdose.app.domain.model.Route
import com.vetdose.app.domain.model.Species
import com.vetdose.app.domain.model.Substance
import com.vetdose.app.domain.model.WithdrawalPeriod
import java.math.BigDecimal

fun SpeciesEntity.toDomain(): Species = Species(
    id = id,
    code = code,
    nameUk = nameUk,
    isFoodProducing = isFoodProducing,
    typicalMinWeightKg = BigDecimal(typicalMinWeightKg),
    typicalMaxWeightKg = BigDecimal(typicalMaxWeightKg),
)

fun SubstanceEntity.toDomain(): Substance = Substance(
    id = id,
    name = name,
    nameUk = nameUk,
    pharmacologicalGroup = pharmacologicalGroup,
    notes = notes,
)

fun ProductEntity.toDomain(): Product = Product(
    id = id,
    tradeName = tradeName,
    manufacturer = manufacturer,
    substanceId = substanceId,
    form = ProductForm.fromValue(form),
    concentrationValue = BigDecimal(concentrationValue),
    concentrationUnit = ConcentrationUnit.fromValue(concentrationUnit),
    tabletDivisibleBy = tabletDivisibleBy,
)

fun DoseRuleEntity.toDomain(): DoseRule = DoseRule(
    id = id,
    substanceId = substanceId,
    speciesId = speciesId,
    route = Route.fromValue(route),
    indication = indication,
    doseMin = BigDecimal(doseMin),
    doseMax = BigDecimal(doseMax),
    doseUnit = DoseUnit.fromValue(doseUnit),
    maxTotalDose = maxTotalDose?.let { BigDecimal(it) },
    maxTotalDoseUnit = maxTotalDoseUnit?.let { MaxTotalDoseUnit.fromValue(it) },
    frequency = frequency,
    duration = duration,
    notes = notes,
    source = source,
    isVerified = isVerified,
)

fun ContraindicationEntity.toDomain(): Contraindication = Contraindication(
    id = id,
    substanceId = substanceId,
    speciesId = speciesId,
    condition = condition,
    severity = Severity.fromValue(severity),
    messageUk = messageUk,
    source = source,
)

fun WithdrawalPeriodEntity.toDomain(): WithdrawalPeriod = WithdrawalPeriod(
    id = id,
    productId = productId,
    speciesId = speciesId,
    route = Route.fromValue(route),
    foodProduct = FoodProduct.fromValue(foodProduct),
    days = days,
    hours = hours,
    source = source,
)
