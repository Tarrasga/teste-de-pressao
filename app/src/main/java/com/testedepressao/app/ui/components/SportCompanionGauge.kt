package com.testedepressao.app.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.testedepressao.app.ui.theme.TextPrimary
import com.testedepressao.app.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class CompanionGaugeMode {
    RPM,
    SPEED
}

@Composable
fun SportCompanionGauge(
    rpm: Int,
    speedKmh: Int,
    modifier: Modifier = Modifier.size(280.dp),
    initialMode: CompanionGaugeMode = CompanionGaugeMode.RPM
) {
    var mode by remember { mutableStateOf(initialMode) }

    val isRpm = mode == CompanionGaugeMode.RPM

    // RPM scale: 0 to 6000 (0 to 6 x1000)
    // Speed scale: 0 to 240 km/h
    val maxVal = if (isRpm) 6000f else 240f
    val currentVal = if (isRpm) rpm.toFloat() else speedKmh.toFloat()

    val animatedVal by animateFloatAsState(
        targetValue = currentVal.coerceIn(0f, maxVal),
        animationSpec = tween(durationMillis = 80),
        label = "companion_needle"
    )

    val startAngle = 135f
    val sweepTotal = 270f

    val ratio = (animatedVal / maxVal).coerceIn(0f, 1f)
    val needleAngleDeg = startAngle + (ratio * sweepTotal)
    val needleAngleRad = needleAngleDeg * (PI / 180f).toFloat()

    val density = LocalDensity.current

    val tickTextPaint = remember(density) {
        Paint().apply {
            color = android.graphics.Color.argb(220, 235, 238, 245)
            textSize = with(density) { 11.sp.toPx() }
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
    }

    val redZoneTextPaint = remember(density) {
        Paint().apply {
            color = android.graphics.Color.argb(240, 255, 30, 39)
            textSize = with(density) { 11.sp.toPx() }
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
    }

    Box(
        modifier = modifier.clickable {
            mode = if (mode == CompanionGaugeMode.RPM) CompanionGaugeMode.SPEED else CompanionGaugeMode.RPM
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = (size.minDimension / 2f) - 6.dp.toPx()
            val dialRadius = outerRadius - 14.dp.toPx()

            // 1. Outer Metallic Bezel Ring
            drawCircle(
                color = GaugeBezelOuter,
                radius = outerRadius,
                center = center
            )
            drawCircle(
                color = GaugeBezelInner,
                radius = outerRadius - 4.dp.toPx(),
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

            drawCircle(
                color = DarkBorder,
                radius = dialRadius,
                center = center,
                style = Stroke(width = 1.5f.dp.toPx())
            )

            // 3. Dial Track Arc
            val arcRadius = dialRadius - 10.dp.toPx()
            val arcStroke = 4.dp.toPx()

            drawArc(
                color = DarkBorder.copy(alpha = 0.5f),
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                size = Size(arcRadius * 2, arcRadius * 2),
                style = Stroke(width = arcStroke, cap = StrokeCap.Round)
            )

            // Redline Zone
            // For RPM: from 4500 to 6000 (ratio 0.75)
            // For Speed: from 180 to 240 (ratio 0.75)
            val redlineStartRatio = 0.75f
            val redlineStartAngle = startAngle + (redlineStartRatio * sweepTotal)
            val redlineSweep = (1f - redlineStartRatio) * sweepTotal

            drawArc(
                color = GaugeRedZone,
                startAngle = redlineStartAngle,
                sweepAngle = redlineSweep,
                useCenter = false,
                topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                size = Size(arcRadius * 2, arcRadius * 2),
                style = Stroke(width = arcStroke + 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // 4. Dial Graduation Ticks & Numeric Labels
            // 12 major intervals:
            // If RPM: 0, 500, 1000, 1500, 2000, 2500, 3000, 3500, 4000, 4500, 5000, 5500, 6000
            // If Speed: 0, 20, 40, 60, 80, 100, 120, 140, 160, 180, 200, 220, 240
            val totalTicks = 24
            for (i in 0..totalTicks) {
                val tickRatio = i.toFloat() / totalTicks
                val tickAngleDeg = startAngle + (tickRatio * sweepTotal)
                val tickAngleRad = tickAngleDeg * (PI / 180f).toFloat()

                val isMajor = i % 4 == 0       // Numbers
                val isSemiMajor = i % 2 == 0   // Intermediate ticks
                val isRedline = tickRatio >= redlineStartRatio

                val tickLen = when {
                    isMajor -> 14.dp.toPx()
                    isSemiMajor -> 9.dp.toPx()
                    else -> 5.dp.toPx()
                }

                val tickOuterR = dialRadius - 8.dp.toPx()
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
                    isRedline -> GaugeRedZone
                    isMajor -> GaugeTickWhite
                    else -> TextSecondary.copy(alpha = 0.6f)
                }

                drawLine(
                    color = tickColor,
                    start = p1,
                    end = p2,
                    strokeWidth = if (isMajor) 2.5f.dp.toPx() else 1.2f.dp.toPx(),
                    cap = StrokeCap.Round
                )

                if (isMajor) {
                    val labelR = tickInnerR - 12.dp.toPx()
                    val labelX = center.x + labelR * cos(tickAngleRad)
                    val labelY = center.y + labelR * sin(tickAngleRad) + 4.dp.toPx()

                    val text = if (isRpm) {
                        // Display 0, 1, 2, 3, 4, 5, 6
                        "${(i * 6 / totalTicks)}"
                    } else {
                        // Display 0, 40, 80, 120, 160, 200, 240
                        "${(i * 240 / totalTicks)}"
                    }

                    drawContext.canvas.nativeCanvas.drawText(
                        text,
                        labelX,
                        labelY,
                        if (isRedline) redZoneTextPaint else tickTextPaint
                    )
                }
            }

            // 5. RED NEEDLE (PONTEIRO VERMELHO)
            val needleLength = dialRadius - 16.dp.toPx()
            val tailLength = 22.dp.toPx()
            val needleBaseHalfWidth = 4.5f.dp.toPx()
            val tailHalfWidth = 3.5f.dp.toPx()

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

            // 5a. Luminous Red Optical Glow Behind Needle
            drawPath(
                path = needlePath,
                color = NeedleRedGlow,
                style = Stroke(width = 6.dp.toPx())
            )

            // 5b. Red Needle Body
            drawPath(
                path = needlePath,
                color = NeedleRed
            )

            // 5c. Needle Spine Highlight
            drawLine(
                color = Color(0xFFFFB3BA),
                start = Offset(center.x, center.y),
                end = Offset(tipX, tipY),
                strokeWidth = 1.2f.dp.toPx(),
                cap = StrokeCap.Round
            )

            // 6. Center Hub / Pivot Cap
            val hubRadius = 18.dp.toPx()
            drawCircle(
                color = Color(0xFF0F1014),
                radius = hubRadius,
                center = center
            )
            drawCircle(
                color = NeedleRed,
                radius = hubRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF282B35),
                radius = 6.dp.toPx(),
                center = center
            )
            drawCircle(
                color = NeedleRed,
                radius = 2.5f.dp.toPx(),
                center = center
            )
        }

        // Center / Lower Digital Display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = 48.dp)
        ) {
            val titleText = if (isRpm) "MOTOR x1000 RPM" else "VELOCIDADE REAL"
            val digitalValText = if (isRpm) "${animatedVal.toInt()}" else "${animatedVal.toInt()}"
            val unitText = if (isRpm) "RPM (Tocar p/ KM/H)" else "KM/H (Tocar p/ RPM)"
            val isHigh = if (isRpm) animatedVal >= 4500 else animatedVal >= 180

            Text(
                text = titleText,
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Text(
                text = digitalValText,
                color = if (isHigh) NeedleRed else TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = unitText,
                color = if (isHigh) NeedleRed else TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
