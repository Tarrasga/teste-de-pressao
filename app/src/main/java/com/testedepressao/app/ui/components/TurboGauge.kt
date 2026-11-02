package com.testedepressao.app.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.testedepressao.app.ui.theme.CarbonBlack
import com.testedepressao.app.ui.theme.DarkBorder
import com.testedepressao.app.ui.theme.GaugeBezelInner
import com.testedepressao.app.ui.theme.GaugeBezelOuter
import com.testedepressao.app.ui.theme.GaugeFaceBlack
import com.testedepressao.app.ui.theme.GaugeRedZone
import com.testedepressao.app.ui.theme.GaugeTickWhite
import com.testedepressao.app.ui.theme.NeedleRed
import com.testedepressao.app.ui.theme.NeedleRedGlow
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
    modifier: Modifier = Modifier
) {
    // Range: -1.0 Bar to +3.0 Bar (Total 4.0 Bar span)
    val minVal = -1.0f
    val maxVal = 3.0f
    val totalSpan = maxVal - minVal

    // Fast and responsive needle sweep
    val animatedBoost by animateFloatAsState(
        targetValue = boostBar.coerceIn(minVal, maxVal),
        animationSpec = tween(durationMillis = 75),
        label = "turbo_needle"
    )

    val startAngle = 135f
    val sweepTotal = 270f

    val currentRatio = ((animatedBoost - minVal) / totalSpan).coerceIn(0f, 1f)
    val needleAngleDeg = startAngle + (currentRatio * sweepTotal)
    val needleAngleRad = needleAngleDeg * (PI / 180f).toFloat()

    val peakRatio = ((peakBoostBar.coerceIn(minVal, maxVal) - minVal) / totalSpan).coerceIn(0f, 1f)
    val peakAngleDeg = startAngle + (peakRatio * sweepTotal)

    val density = LocalDensity.current

    val tickTextPaint = remember(density) {
        Paint().apply {
            color = android.graphics.Color.argb(220, 235, 238, 245)
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
    }

    val redZoneTextPaint = remember(density) {
        Paint().apply {
            color = android.graphics.Color.argb(240, 255, 30, 39)
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
    }

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        // Automatically adapt diameter to fill the container (phone or tablet)
        val gaugeSizeDp = minOf(maxWidth, maxHeight)
        val scale = (gaugeSizeDp / 280.dp).coerceIn(0.5f, 4.0f)

        Box(modifier = Modifier.size(gaugeSizeDp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val outerRadius = (size.minDimension / 2f) - (6.dp.toPx() * scale)
                val dialRadius = outerRadius - (14.dp.toPx() * scale)

                // 1. Outer Metallic Bezel Ring
                drawCircle(
                    color = GaugeBezelOuter,
                    radius = outerRadius,
                    center = center
                )
                drawCircle(
                    color = GaugeBezelInner,
                    radius = outerRadius - (4.dp.toPx() * scale),
                    center = center
                )

                // 2. Pure Black Dial Face
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(GaugeFaceBlack, CarbonBlack),
                        center = center,
                        radius = dialRadius
                    ),
                    radius = dialRadius,
                    center = center
                )

                // Outer dial perimeter hairline
                drawCircle(
                    color = DarkBorder,
                    radius = dialRadius,
                    center = center,
                    style = Stroke(width = 1.5f.dp.toPx() * scale)
                )

                // 3. Dial Track Arc
                val arcRadius = dialRadius - (10.dp.toPx() * scale)
                val arcStroke = 4.dp.toPx() * scale

                drawArc(
                    color = DarkBorder.copy(alpha = 0.5f),
                    startAngle = startAngle,
                    sweepAngle = sweepTotal,
                    useCenter = false,
                    topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                    size = Size(arcRadius * 2, arcRadius * 2),
                    style = Stroke(width = arcStroke, cap = StrokeCap.Round)
                )

                // Redline / Overboost warning arc (from +1.5 Bar to +3.0 Bar)
                val redZoneStartRatio = (1.5f - minVal) / totalSpan
                val redZoneStartAngle = startAngle + (redZoneStartRatio * sweepTotal)
                val redZoneSweep = (1f - redZoneStartRatio) * sweepTotal

                drawArc(
                    color = GaugeRedZone,
                    startAngle = redZoneStartAngle,
                    sweepAngle = redZoneSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                    size = Size(arcRadius * 2, arcRadius * 2),
                    style = Stroke(width = arcStroke + (2.dp.toPx() * scale), cap = StrokeCap.Round)
                )

                // 4. Dial Graduation Ticks & Numeric Labels
                tickTextPaint.textSize = with(density) { (11.5f * scale).sp.toPx() }
                redZoneTextPaint.textSize = with(density) { (11.5f * scale).sp.toPx() }

                val totalTicks = 16
                for (i in 0..totalTicks) {
                    val tickVal = minVal + (i * 0.25f)
                    val tickRatio = (tickVal - minVal) / totalSpan
                    val tickAngleDeg = startAngle + (tickRatio * sweepTotal)
                    val tickAngleRad = tickAngleDeg * (PI / 180f).toFloat()

                    val isMajor = i % 4 == 0       // -1, 0, 1, 2, 3
                    val isSemiMajor = i % 2 == 0   // -0.5, 0.5, 1.5, 2.5
                    val isRedZone = tickVal >= 1.5f

                    val tickLen = when {
                        isMajor -> 14.dp.toPx() * scale
                        isSemiMajor -> 9.dp.toPx() * scale
                        else -> 5.dp.toPx() * scale
                    }

                    val tickOuterR = dialRadius - (8.dp.toPx() * scale)
                    val tickInnerR = tickOuterR - tickLen

                    val p1 = Offset(
                        center.x + tickOuterR * cos(tickAngleRad),
                        center.y + tickOuterR * sin(tickAngleRad)
                    )
                    val p2 = Offset(
                        center.x + tickInnerR * cos(tickAngleRad),
                        center.y + tickInnerR * sin(tickAngleRad)
                    )

                    val tickColor = when {
                        isRedZone -> GaugeRedZone
                        isMajor -> GaugeTickWhite
                        else -> TextSecondary.copy(alpha = 0.6f)
                    }

                    val tickStrokeWidth = when {
                        isMajor -> 2.5f.dp.toPx() * scale
                        isSemiMajor -> 1.5f.dp.toPx() * scale
                        else -> 1.dp.toPx() * scale
                    }

                    drawLine(
                        color = tickColor,
                        start = p1,
                        end = p2,
                        strokeWidth = tickStrokeWidth,
                        cap = StrokeCap.Round
                    )

                    // Numerical text labels for major numbers: -1, 0, 1, 2, 3
                    if (isMajor) {
                        val labelR = tickInnerR - (13.dp.toPx() * scale)
                        val labelX = center.x + labelR * cos(tickAngleRad)
                        val labelY = center.y + labelR * sin(tickAngleRad) + (4.dp.toPx() * scale)

                        val text = when (tickVal.toInt()) {
                            -1 -> "-1"
                            0 -> "0"
                            1 -> "1"
                            2 -> "2"
                            3 -> "3"
                            else -> "${tickVal.toInt()}"
                        }

                        drawContext.canvas.nativeCanvas.drawText(
                            text,
                            labelX,
                            labelY,
                            if (isRedZone) redZoneTextPaint else tickTextPaint
                        )
                    }
                }

                // 5. Peak Hold Marker Bug
                if (peakBoostBar > 0f) {
                    val peakRad = peakAngleDeg * (PI / 180f).toFloat()
                    val markerOuterR = dialRadius - (6.dp.toPx() * scale)
                    val markerInnerR = markerOuterR - (16.dp.toPx() * scale)

                    drawLine(
                        color = RaceRed,
                        start = Offset(center.x + markerInnerR * cos(peakRad), center.y + markerInnerR * sin(peakRad)),
                        end = Offset(center.x + markerOuterR * cos(peakRad), center.y + markerOuterR * sin(peakRad)),
                        strokeWidth = 3.5f.dp.toPx() * scale,
                        cap = StrokeCap.Round
                    )
                }

                // 6. RED NEEDLE (PONTEIRO VERMELHO)
                val needleLength = dialRadius - (16.dp.toPx() * scale)
                val tailLength = 22.dp.toPx() * scale
                val needleBaseHalfWidth = 4.5f.dp.toPx() * scale
                val tailHalfWidth = 3.5f.dp.toPx() * scale

                val tipX = center.x + needleLength * cos(needleAngleRad)
                val tipY = center.y + needleLength * sin(needleAngleRad)

                val tailRad = needleAngleRad + PI.toFloat()
                val tailX = center.x + tailLength * cos(tailRad)
                val tailY = center.y + tailLength * sin(tailRad)

                val perpRad = needleAngleRad + (PI / 2f).toFloat()
                val base1X = center.x + needleBaseHalfWidth * cos(perpRad)
                val base1Y = center.y + needleBaseHalfWidth * sin(perpRad)
                val base2X = center.x - needleBaseHalfWidth * cos(perpRad)
                val base2Y = center.y - needleBaseHalfWidth * sin(perpRad)

                val tail1X = tailX + tailHalfWidth * cos(perpRad)
                val tail1Y = tailY + tailHalfWidth * sin(perpRad)
                val tail2X = tailX - tailHalfWidth * cos(perpRad)
                val tail2Y = tailY - tailHalfWidth * sin(perpRad)

                val needlePath = Path().apply {
                    moveTo(tipX, tipY)
                    lineTo(base1X, base1Y)
                    lineTo(tail1X, tail1Y)
                    lineTo(tail2X, tail2Y)
                    lineTo(base2X, base2Y)
                    close()
                }

                // 6a. Optical Glow Behind Needle
                drawPath(
                    path = needlePath,
                    color = NeedleRedGlow,
                    style = Stroke(width = 6.dp.toPx() * scale)
                )

                // 6b. Red Needle Body
                drawPath(
                    path = needlePath,
                    color = NeedleRed
                )

                // 6c. Needle Center Spine Highlight
                drawLine(
                    color = Color(0xFFFFB3BA),
                    start = Offset(center.x, center.y),
                    end = Offset(tipX, tipY),
                    strokeWidth = 1.2f.dp.toPx() * scale,
                    cap = StrokeCap.Round
                )

                // 7. Center Hub / Pivot Cap
                val hubRadius = 18.dp.toPx() * scale
                drawCircle(
                    color = Color(0xFF0F1014),
                    radius = hubRadius,
                    center = center
                )
                drawCircle(
                    color = NeedleRed,
                    radius = hubRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx() * scale)
                )
                drawCircle(
                    color = Color(0xFF282B35),
                    radius = 6.dp.toPx() * scale,
                    center = center
                )
                drawCircle(
                    color = NeedleRed,
                    radius = 2.5f.dp.toPx() * scale,
                    center = center
                )
            }

            // Center / Lower Digital Display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(y = 48.dp * scale)
            ) {
                val boostText = String.format(Locale.US, "%+.2f", animatedBoost)
                val isOverboost = animatedBoost >= 1.5f

                Text(
                    text = "TURBO BOOST",
                    color = TextSecondary,
                    fontSize = (10 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (1.2 * scale).sp
                )
                Text(
                    text = boostText,
                    color = if (isOverboost) NeedleRed else TextPrimary,
                    fontSize = (32 * scale).sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "BAR",
                    color = if (isOverboost) NeedleRed else TextSecondary,
                    fontSize = (11 * scale).sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "PICO: ${String.format(Locale.US, "%.2f", peakBoostBar)} BAR",
                    color = if (peakBoostBar >= 1.5f) NeedleRed else TextSecondary,
                    fontSize = (10 * scale).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
