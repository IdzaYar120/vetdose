package com.vetdose.app.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.vetdose.app.domain.calculator.CalculationWarning
import com.vetdose.app.ui.theme.VetDoseTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers doc Stage 5's "базові Compose UI тести ... для попереджень": every
 * warning code DoseCalculator can produce must actually render its
 * (Ukrainian) message text on screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WarningCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(warning: CalculationWarning) {
        composeRule.setContent {
            VetDoseTheme {
                WarningCard(warning)
            }
        }
    }

    @Test
    fun `renders the absolute contraindication message`() {
        setContent(CalculationWarning("ABSOLUTE_CONTRAINDICATION", "TEST: протипоказано вагітним тваринам"))
        composeRule.onNodeWithText("TEST: протипоказано вагітним тваринам").assertExists()
    }

    @Test
    fun `renders the caution message`() {
        setContent(CalculationWarning("CAUTION", "TEST: застосовувати з обережністю"))
        composeRule.onNodeWithText("TEST: застосовувати з обережністю").assertExists()
    }

    @Test
    fun `renders the rule-not-verified message`() {
        setContent(
            CalculationWarning(
                "RULE_NOT_VERIFIED",
                "Це правило дозування ще не підтверджене лікарем. Застосовуйте з обережністю.",
            ),
        )
        composeRule.onNodeWithText(
            "Це правило дозування ще не підтверджене лікарем. Застосовуйте з обережністю.",
        ).assertExists()
    }

    @Test
    fun `renders the food-producing-animal reminder`() {
        setContent(
            CalculationWarning(
                "FOOD_PRODUCING_ANIMAL",
                "Продуктивна тварина: перед використанням продукції перевірте терміни виведення.",
            ),
        )
        composeRule.onNodeWithText(
            "Продуктивна тварина: перед використанням продукції перевірте терміни виведення.",
        ).assertExists()
    }
}
