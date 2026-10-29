package com.testedepressao.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.testedepressao.app.data.models.BatteryCondition
import com.testedepressao.app.data.models.BatteryHealthData
import com.testedepressao.app.ui.components.TelemetryCard
import com.testedepressao.app.ui.theme.CarbonBlack
import com.testedepressao.app.ui.theme.DarkBorder
import com.testedepressao.app.ui.theme.DarkSurface
import com.testedepressao.app.ui.theme.DarkSurfaceElevated
import com.testedepressao.app.ui.theme.NeonCyan
import com.testedepressao.app.ui.theme.PeugeotCopper
import com.testedepressao.app.ui.theme.RaceRed
import com.testedepressao.app.ui.theme.SuccessGreen
import com.testedepressao.app.ui.theme.TextPrimary
import com.testedepressao.app.ui.theme.TextSecondary
import com.testedepressao.app.ui.theme.WarningAmber
import java.util.Locale

@Composable
fun BatteryHealthScreen(
    batteryHealth: BatteryHealthData,
    onResetCrankTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val conditionColor = when (batteryHealth.condition) {
        BatteryCondition.EXCELENTE -> SuccessGreen
        BatteryCondition.BOA -> NeonCyan
        BatteryCondition.FRACA -> WarningAmber
        BatteryCondition.CRITICA -> RaceRed
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. Mostrador Central de Tensão em Tempo Real ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .border(1.5.dp, conditionColor.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (batteryHealth.isAlternatorCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryAlert,
                        contentDescription = null,
                        tint = conditionColor
                    )
                    Text(
                        text = if (batteryHealth.isAlternatorCharging) "ALTERNADOR A CARREGAR" else "TENSÃO DA BATERIA (12V)",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = String.format(Locale.US, "%.2f", batteryHealth.liveVoltage),
                    color = conditionColor,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "VOLTS",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Estado de Carga (SoC %)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Estado de Carga Estimado (SoC):",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${batteryHealth.estimatedSocPercent}%",
                        color = conditionColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { (batteryHealth.estimatedSocPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = conditionColor,
                    trackColor = DarkBorder,
                    strokeCap = StrokeCap.Round
                )
            }
        }

        // --- 2. Diagnóstico & Avaliação de Saúde ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (batteryHealth.condition.isGood) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = conditionColor
                    )
                    Text(
                        text = batteryHealth.condition.title,
                        color = conditionColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = batteryHealth.condition.description,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceElevated)
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = PeugeotCopper, modifier = Modifier.padding(top = 1.dp, end = 6.dp))
                        Text(
                            text = batteryHealth.recommendation,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // --- 3. Teste de Queda no Arranque (Cranking Test) ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TESTE DE ARRANQUE (CRANKING DROP)",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Queda mínima de tensão quando dá à chave",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onResetCrankTest,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Repor", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val crankVal = batteryHealth.minCrankVoltage
                val crankColor = when {
                    crankVal == null -> TextSecondary
                    crankVal >= 10.0f -> SuccessGreen
                    crankVal >= 9.4f -> WarningAmber
                    else -> RaceRed
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = crankVal?.let { String.format(Locale.US, "%.2f V", it) } ?: "--.-- V (Aguardando arranque)",
                        color = crankColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )

                    val crankStatusText = when {
                        crankVal == null -> "Desligue e ligue o motor"
                        crankVal >= 10.0f -> "Arranque Forte"
                        crankVal >= 9.4f -> "Arranque Médio"
                        else -> "Queda Excessiva (<9.4V)"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(crankColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = crankStatusText,
                            color = crankColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- 4. Tabela de Referência das 3 Medições ---
        item {
            Text(
                text = "VALORES DE REFERÊNCIA (PEUGEOT 508)",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelemetryCard(
                    title = "Repouso (Parado)",
                    value = batteryHealth.restingVoltage?.let { String.format(Locale.US, "%.2f", it) } ?: "--",
                    unit = "Ideal: >12.5V",
                    accentColor = if ((batteryHealth.restingVoltage ?: 12.5f) < 12.2f) WarningAmber else SuccessGreen,
                    modifier = Modifier.weight(1f)
                )

                TelemetryCard(
                    title = "Alternador (Ligado)",
                    value = batteryHealth.chargingVoltage?.let { String.format(Locale.US, "%.2f", it) } ?: "--",
                    unit = "Ideal: 14.0-14.7V",
                    accentColor = if ((batteryHealth.chargingVoltage ?: 14.2f) in 13.7f..14.8f) SuccessGreen else WarningAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
