package com.vetdose.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.vetdose.app.domain.calculator.CalculationWarning
import com.vetdose.app.ui.theme.WarningAbsolute
import com.vetdose.app.ui.theme.WarningCaution

private data class WarningStyle(val color: Color, val icon: ImageVector)

private fun styleFor(code: String): WarningStyle = when (code) {
    "ABSOLUTE_CONTRAINDICATION" -> WarningStyle(WarningAbsolute, Icons.Default.Dangerous)
    "CAUTION", "MAX_DOSE_CAPPED", "TABLET_CANNOT_MATCH_RANGE" -> WarningStyle(WarningCaution, Icons.Default.WarningAmber)
    else -> WarningStyle(WarningCaution, Icons.Default.Info)
}

/**
 * Renders one calculator warning as a colored card — red for an absolute
 * contraindication, amber/yellow for everything else (caution, unverified
 * rule, capped dose, food-producing-animal reminder, out-of-range weight).
 * The text itself ([CalculationWarning.messageUk]) already comes from
 * DoseCalculator in Ukrainian; this component only supplies color/icon.
 */
@Composable
fun WarningCard(warning: CalculationWarning, modifier: Modifier = Modifier) {
    val style = styleFor(warning.code)
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = style.color.copy(alpha = 0.12f)),
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(imageVector = style.icon, contentDescription = null, tint = style.color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text(text = warning.messageUk, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
