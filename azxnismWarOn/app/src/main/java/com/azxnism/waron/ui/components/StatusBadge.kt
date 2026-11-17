package com.azxnism.waron.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azxnism.waron.data.model.ScanStatus
import com.azxnism.waron.ui.theme.*

@Composable
fun StatusBadge(
    status: ScanStatus,
    modifier: Modifier = Modifier
) {
    val (color, label) = when (status) {
        ScanStatus.QUEUED -> AccentBlue to "Queued"
        ScanStatus.RUNNING -> AccentYellow to "Running"
        ScanStatus.COMPLETED -> AccentGreen to "Completed"
        ScanStatus.FAILED -> AccentRed to "Failed"
        ScanStatus.CANCELLED -> TextTertiary to "Cancelled"
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}
