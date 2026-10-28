package com.testedepressao.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.testedepressao.app.ui.theme.CarbonBlack
import com.testedepressao.app.ui.theme.DarkBorder
import com.testedepressao.app.ui.theme.NeonCyan
import com.testedepressao.app.ui.theme.RaceRed
import com.testedepressao.app.ui.theme.TextPrimary
import com.testedepressao.app.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TurboGauge(
    boostBar: Float,
    peakBoostBar: Float,
    modifier: Modifier = Modifier.size(280.dp)
) {
    // Range: -1.0 Bar to +3.0 Bar (Total 4.0 Bar span)
    val minVal = -1.0f
    val maxVal = 3.0f
    val totalSpan = maxVal - minVal

    val animatedBoost by animateFloatAsState(
        targetValue = boostBar.coerceIn(minVal, maxVal),
        animationSpec = tween(durationMillis = 80),
        label = "boost_needle"
    )

    val startAngle = 140f
    val sweepTotal = 260f

    // Calculate angles
    val currentRatio = ((animatedBoost - minVal) / totalSpan).coerceIn(0f, 1f)
    val activeSweep = currentRatio * sweepTotal

    val peakRatio = ((peakBoostBar.coerceIn(minVal, maxVal) - minVal) / totalSpan).coerceIn(0f, 1f)
    val peakAngleDeg = startAngle + (peakRatio * sweepTotal)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension / 2f) - 24.dp.toPx()
            val strokeWidth = 18.dp.toPx()

            // 1. Background Track Arc
            drawArc(
                color = DarkBorder,
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 2. Active Gradient Boost Arc
            if (activeSweep > 0f) {
                val gradient = Brush.sweepGradient(
                    0.0f to NeonCyan,
                    0.65f to NeonCyan,
                    0.85f to RaceRed,
                    1.0f to RaceRed,
                    center = center
                )

                drawArc(
                    brush = gradient,
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // 3. Ticks marks for -1, 0, 1, 2, 3 Bar
            for (i in 0..8) {
                val stepVal = minVal + (i * 0.5f)
                val stepRatio = (stepVal - minVal) / totalSpan
                val angleDeg = startAngle + (stepRatio * sweepTotal)
                val angleRad = angleDeg * (PI / 180f).toFloat()

                val isMajor = i % 2 == 0
                val tickLen = if (isMajor) 14.dp.toPx() else 8.dp.toPx()
                val tickInnerR = radius - (strokeWidth / 2f) - 6.dp.toPx()
                val tickOuterR = tickInnerR - tickLen

                val p1 = Offset(
                    center.x + tickInnerR * cos(angleRad),
                    center.y + tickInnerR * sin(angleRad)
                )
                val p2 = Offset(
                    center.x + tickOuterR * cos(angleRad),
                    center.y + tickOuterR * sin(angleRad)
                )

                val tickColor = if (stepVal >= 1.5f) RaceRed else TextSecondary
                drawLine(
                    color = tickColor,
                    start = p1,
                    end = p2,
                    strokeWidth = if (isMajor) 3.dp.toPx() else 1.5f.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 4. Peak Boost Bug Marker
            if (peakBoostBar > 0f) {
                val peakRad = peakAngleDeg * (PI / 180f).toFloat()
                val markerInnerR = radius - (strokeWidth / 2f) - 4.dp.toPx()
                val markerOuterR = radius + (strokeWidth / 2f) + 4.dp.toPx()

                drawLine(
                    color = RaceRed,
                    start = Offset(center.x + markerInnerR * cos(peakRad), center.y + markerInnerR * sin(peakRad)),
                    end = Offset(center.x + markerOuterR * cos(peakRad), center.y + markerOuterR * sin(peakRad)),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 5. Center Dial Accent
            drawCircle(
                color = CarbonBlack,
                radius = radius * 0.55f,
                center = center
            )
            drawCircle(
                color = if (animatedBoost >= 1.5f) RaceRed.copy(alpha = 0.4f) else NeonCyan.copy(alpha = 0.25f),
                radius = radius * 0.55f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Center Digital Display
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val boostText = String.format(Locale.US, "%+.2f", animatedBoost)
            val isPositive = animatedBoost >= 0f

            Text(
                text = "TURBO BOOST",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Text(
                text = boostText,
                color = if (animatedBoost >= 1.5f) RaceRed else if (isPositive) NeonCyan else TextPrimary,
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "BAR",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "PICO: ${String.format(Locale.US, "%.2f", peakBoostBar)} BAR",
                color = if (peakBoostBar >= 1.5f) RaceRed else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
