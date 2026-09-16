package com.vetdose.app.ui.util

import java.math.BigDecimal

/** Accepts weight typed with either a decimal comma or a decimal point
 * (doc UX requirement) and returns null for anything not a valid positive
 * number — the caller decides how to surface that (disabled button, etc.). */
fun parseWeightInput(text: String): BigDecimal? {
    val normalized = text.trim().replace(',', '.')
    if (normalized.isEmpty()) return null
    val value = normalized.toBigDecimalOrNull() ?: return null
    return if (value > BigDecimal.ZERO) value else null
}

/**
 * For values DoseCalculator already rounded to a deliberate display scale
 * (`administrationMin`/`administrationMax` — see `roundVolumeForDisplay` and
 * doc rule 8), formats with a comma but WITHOUT trimming trailing zeros:
 * "1.0" must stay "1,0", not become "1", since the trailing zero is exactly
 * what tells the vet this was rounded to one decimal place. Contrast with
 * [com.vetdose.app.domain.calculator.formatNumber], which trims trailing
 * zeros and is for values that don't carry a meaningful fixed scale (e.g.
 * `doseMin`/`doseMax`, always stored at 4 decimal places internally).
 */
fun BigDecimal.toDisplayString(): String = toPlainString().replace('.', ',')
