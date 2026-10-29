package com.testedepressao.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.testedepressao.app.data.models.LiveTripData
import com.testedepressao.app.data.models.TripRecord
import com.testedepressao.app.ui.components.TelemetryCard
import com.testedepressao.app.ui.theme.CarbonBlack
import com.testedepressao.app.ui.theme.DarkBorder
import com.testedepressao.app.ui.theme.DarkSurface
import com.testedepressao.app.ui.theme.DarkSurfaceElevated
import com.testedepressao.app.ui.theme.NeonCyan
import com.testedepressao.app.ui.theme.RaceRed
import com.testedepressao.app.ui.theme.SuccessGreen
import com.testedepressao.app.ui.theme.TextPrimary
import com.testedepressao.app.ui.theme.TextSecondary
import com.testedepressao.app.ui.theme.WarningAmber
import java.util.Locale

@Composable
fun TripComputerScreen(
    liveTrip: LiveTripData,
    history: List<TripRecord>,
    onStartTrip: () -> Unit,
    onStopTrip: () -> Unit,
    onDeleteTrip: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. Ação Principal: Iniciar / Concluir Viagem ---
        item {
            Button(
                onClick = {
                    if (liveTrip.isRecording) onStopTrip() else onStartTrip()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (liveTrip.isRecording) RaceRed else NeonCyan,
                    contentColor = CarbonBlack
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (liveTrip.isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = if (liveTrip.isRecording) "CONCLUIR & GRAVAR VIAGEM" else "INICIAR NOVA VIAGEM",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // --- 2. Cartão de Avaliação de Condução (Estilo & Eco-Score) ---
        item {
            val style = liveTrip.currentStyle
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurface)
                    .border(1.5.dp, style.color.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ESTILO DE CONDUÇÃO",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "${style.emoji} ${style.displayName.uppercase()}",
                                color = style.color,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Eco-score badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(style.color.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Score: ${liveTrip.ecoScore}/100",
                            color = style.color,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = style.description,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Bar for Eco Score
                LinearProgressIndicator(
                    progress = { (liveTrip.ecoScore / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = style.color,
                    trackColor = DarkBorder,
                    strokeCap = StrokeCap.Round
                )
            }
        }

        // --- 3. Cartões de Consumo Instantâneo e Médio ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Instant Consumption
                val instantText = if (liveTrip.isStopped) {
                    String.format(Locale.US, "%.1f", liveTrip.instantConsumptionLh)
                } else {
                    String.format(Locale.US, "%.1f", liveTrip.instantConsumptionL100km)
                }
                val instantUnit = if (liveTrip.isStopped) "L/h (Parado)" else "L/100 km"

                TelemetryCard(
                    title = "Consumo Instantâneo",
                    value = instantText,
                    unit = instantUnit,
                    accentColor = if (liveTrip.instantConsumptionL100km > 12f) RaceRed else NeonCyan,
                    modifier = Modifier.weight(1f)
                )

                // Average Consumption
                TelemetryCard(
                    title = "Consumo Médio",
                    value = String.format(Locale.US, "%.1f", liveTrip.avgConsumptionL100km),
                    unit = "L/100 km",
                    accentColor = if (liveTrip.avgConsumptionL100km > 8f) WarningAmber else SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // --- 4. Distância e Duração ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelemetryCard(
                    title = "Distância Percorrida",
                    value = String.format(Locale.US, "%.1f", liveTrip.distanceKm),
                    unit = "Quilómetros",
                    accentColor = TextPrimary,
                    modifier = Modifier.weight(1f)
                )

                val durSeconds = liveTrip.durationSeconds
                val h = durSeconds / 3600
                val m = (durSeconds % 3600) / 60
                val s = durSeconds % 60
                val durFormatted = if (h > 0) {
                    String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
                } else {
                    String.format(Locale.US, "%02d:%02d", m, s)
                }

                TelemetryCard(
                    title = "Tempo de Viagem",
                    value = durFormatted,
                    unit = "Duração",
                    accentColor = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // --- 5. Litros Gastos e Velocidade Média/Máxima ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelemetryCard(
                    title = "Combustível Total",
                    value = String.format(Locale.US, "%.2f", liveTrip.fuelConsumedLiters),
                    unit = "Litros Gastos",
                    accentColor = WarningAmber,
                    modifier = Modifier.weight(1f)
                )

                TelemetryCard(
                    title = "Velocidade Média/Máx",
                    value = "${liveTrip.avgSpeedKmh.toInt()}/${liveTrip.maxSpeedKmh}",
                    unit = "km/h",
                    accentColor = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // --- 6. Histórico de Viagens Gravadas ---
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "HISTÓRICO DE VIAGENS (${history.size})",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        if (history.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Text(
                            text = "Nenhuma viagem gravada ainda",
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Clique em 'Iniciar Nova Viagem' no topo para começar a gravar o seu trajeto e consumos.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            items(history, key = { it.id }) { item ->
                TripHistoryItemCard(
                    record = item,
                    onDelete = { onDeleteTrip(item.id) }
                )
            }
        }
    }
}

@Composable
fun TripHistoryItemCard(
    record: TripRecord,
    onDelete: () -> Unit
) {
    val style = record.drivingStyle

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(1.dp, style.color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${style.emoji} ${style.displayName}",
                    color = style.color,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = " • ${record.formattedDate}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = TextSecondary.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("DISTÂNCIA", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = String.format(Locale.US, "%.1f km", record.distanceKm),
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column {
                Text("CONSUMO MÉDIO", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = String.format(Locale.US, "%.1f L/100", record.avgConsumptionL100km),
                    color = if (record.avgConsumptionL100km > 8f) WarningAmber else SuccessGreen,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column {
                Text("DURAÇÃO", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = record.durationFormatted,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column {
                Text("PICO TURBO", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = String.format(Locale.US, "%.2f Bar", record.maxBoostBar),
                    color = NeonCyan,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
