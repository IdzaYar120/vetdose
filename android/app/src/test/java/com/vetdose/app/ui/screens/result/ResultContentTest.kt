package com.vetdose.app.ui.screens.result

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vetdose.app.domain.calculator.CalculationResult
import com.vetdose.app.domain.calculator.CalculationWarning
import com.vetdose.app.domain.calculator.ConcentrationUnit
import com.vetdose.app.domain.calculator.DoseUnit
import com.vetdose.app.domain.model.DoseRule
import com.vetdose.app.domain.model.FoodProduct
import com.vetdose.app.domain.model.Product
import com.vetdose.app.domain.model.ProductForm
import com.vetdose.app.domain.model.Route
import com.vetdose.app.domain.model.Species
import com.vetdose.app.domain.model.WithdrawalPeriod
import com.vetdose.app.ui.theme.VetDoseTheme
import java.math.BigDecimal
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Covers doc Stage 5's "базові Compose UI тести ... для екрана результату". */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ResultContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val species = Species(
        id = "sp-1",
        code = "dog",
        nameUk = "TEST_Собака",
        isFoodProducing = true,
        typicalMinWeightKg = BigDecimal("2"),
        typicalMaxWeightKg = BigDecimal("90"),
    )

    private val doseRule = DoseRule(
        id = "rule-1",
        substanceId = "sub-1",
        speciesId = "sp-1",
        route = Route.PO,
        indication = null,
        doseMin = BigDecimal("10"),
        doseMax = BigDecimal("10"),
        doseUnit = DoseUnit.MG_PER_KG,
        maxTotalDose = null,
        maxTotalDoseUnit = null,
        frequency = "кожні 12 год",
        duration = null,
        notes = null,
        source = "TEST DATA — NOT FOR CLINICAL USE",
        isVerified = true,
    )

    private val product = Product(
        id = "prod-1",
        tradeName = "TEST_Antibiotic_Inj_A",
        manufacturer = null,
        substanceId = "sub-1",
        form = ProductForm.INJECTION_SOLUTION,
        concentrationValue = BigDecimal("50"),
        concentrationUnit = ConcentrationUnit.MG_PER_ML,
        tabletDivisibleBy = null,
    )

    private val baseResult = CalculationResult(
        doseMin = BigDecimal("52.0000"),
        doseMax = BigDecimal("52.0000"),
        doseAmountUnit = "mg",
        administrationMin = BigDecimal("1.0"),
        administrationMax = BigDecimal("1.0"),
        administrationUnit = "ml",
        maxDoseCapped = false,
        warnings = listOf(CalculationWarning("FOOD_PRODUCING_ANIMAL", "TEST: перевірте терміни виведення")),
        explanation = listOf("5,2 кг × 10 мг/кг = 52 мг", "52 мг ÷ 50 мг/мл = 1,04 мл"),
    )

    private fun state(
        result: CalculationResult = baseResult,
        withdrawalPeriods: List<WithdrawalPeriod> = emptyList(),
        explanationExpanded: Boolean = false,
    ) = ResultUiState(
        loading = false,
        species = species,
        product = product,
        doseRule = doseRule,
        result = result,
        withdrawalPeriods = withdrawalPeriods,
        explanationExpanded = explanationExpanded,
    )

    @Test
    fun `shows the big administration amount and unit`() {
        composeRule.setContent { VetDoseTheme { ResultContent(state(), {}) } }
        composeRule.onNodeWithText("1,0 мл").assertExists()
    }

    @Test
    fun `shows the dose range`() {
        composeRule.setContent { VetDoseTheme { ResultContent(state(), {}) } }
        composeRule.onNodeWithText("52 мг").assertExists()
    }

    @Test
    fun `shows the route label`() {
        composeRule.setContent { VetDoseTheme { ResultContent(state(), {}) } }
        composeRule.onNodeWithText("перорально").assertExists()
    }

    @Test
    fun `shows a warning card for each warning`() {
        composeRule.setContent { VetDoseTheme { ResultContent(state(), {}) } }
        composeRule.onNodeWithText("TEST: перевірте терміни виведення").assertExists()
    }

    @Test
    fun `shows the withdrawal period with a computed date`() {
        val withdrawal = WithdrawalPeriod(
            id = "wd-1",
            productId = "prod-1",
            speciesId = "sp-1",
            route = Route.IM,
            foodProduct = FoodProduct.MEAT,
            days = 10,
            hours = null,
            source = "TEST DATA — NOT FOR CLINICAL USE",
        )
        composeRule.setContent { VetDoseTheme { ResultContent(state(withdrawalPeriods = listOf(withdrawal)), {}) } }
        composeRule.onNodeWithText("10 дн.", substring = true).assertExists()
    }

    @Test
    fun `explanation steps are hidden until the toggle is clicked`() {
        var expanded = false
        composeRule.setContent {
            VetDoseTheme { ResultContent(state(explanationExpanded = expanded), { expanded = !expanded }) }
        }
        composeRule.onNodeWithText("5,2 кг × 10 мг/кг = 52 мг", substring = true).assertDoesNotExist()
    }

    @Test
    fun `explanation steps show once expanded`() {
        composeRule.setContent {
            VetDoseTheme { ResultContent(state(explanationExpanded = true), {}) }
        }
        composeRule.onNodeWithText("5,2 кг × 10 мг/кг = 52 мг", substring = true).assertExists()
    }

    @Test
    fun `absolute contraindication gate hides the dose until confirmed`() {
        var confirmed = false
        composeRule.setContent {
            VetDoseTheme {
                AbsoluteContraindicationGate(
                    state(
                        result = baseResult.copy(
                            warnings = listOf(
                                CalculationWarning("ABSOLUTE_CONTRAINDICATION", "TEST: протипоказано вагітним"),
                            ),
                        ),
                    ),
                    onConfirm = { confirmed = true },
                )
            }
        }
        composeRule.onNodeWithText("TEST: протипоказано вагітним").assertExists()
        composeRule.onNodeWithText("1,0 мл").assertDoesNotExist()
        composeRule.onNodeWithText("Я ознайомлений(а), показати дозу").performClick()
        assert(confirmed)
    }
}
