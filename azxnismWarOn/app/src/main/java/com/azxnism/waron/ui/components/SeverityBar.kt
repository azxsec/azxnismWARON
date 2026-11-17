package com.azxnism.waron.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.azxnism.waron.ui.theme.*

@Composable
fun SeverityBar(
    critical: Int,
    high: Int,
    medium: Int,
    low: Int,
    info: Int,
    modifier: Modifier = Modifier
) {
    val total = (critical + high + medium + low + info).coerceAtLeast(1)

    val segments = listOf(
        critical to SeverityCritical,
        high to SeverityHigh,
        medium to SeverityMedium,
        low to SeverityLow,
        info to SeverityInfo
    ).filter { it.first > 0 }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(DarkBorder)
    ) {
        segments.forEach { (count, color) ->
            Box(
                modifier = Modifier
                    .weight(count.toFloat() / total)
                    .height(6.dp)
                    .background(color)
            )
        }
    }
}
