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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import com.testedepressao.app.data.models.AccelerationState
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
fun AccelerationScreen(
    state: AccelerationState,
    onArmTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Tag
        val (statusText, statusBg, statusColor) = when {
            state.isRunning -> Triple("EM CURSO - A ACELERAR!", RaceRed.copy(alpha = 0.2f), RaceRed)
            state.isArmed -> Triple("PRONTO! PARADO A 0 KM/H (Arranque quando quiser)", SuccessGreen.copy(alpha = 0.2f), SuccessGreen)
            state.isFinished -> Triple("TESTE CONCLUÍDO COM SUCESSO!", NeonCyan.copy(alpha = 0.2f), NeonCyan)
            else -> Triple("TESTE DESARMADO - CLIQUE ABAIXO", DarkSurfaceElevated, TextSecondary)
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(statusBg)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Big Stopwatch Display
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurface)
                .border(1.dp, if (state.isRunning) RaceRed else DarkBorder, RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "0 A 100 KM/H",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            
            Text(
                text = String.format(Locale.US, "%05.2f", state.elapsedSeconds),
                color = if (state.isRunning) RaceRed else if (state.isFinished) SuccessGreen else TextPrimary,
                fontSize = 64.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "SEGUNDOS",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Speed gauge progress to 100 km/h
            val progress = (state.currentSpeed / 100f).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = if (progress >= 1f) SuccessGreen else NeonCyan,
                trackColor = DarkBorder,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0 km/h", color = TextSecondary, fontSize = 11.sp)
                Text(
                    "${state.currentSpeed} km/h",
                    color = NeonCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("100 km/h", color = TextSecondary, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Best Record Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceElevated)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = WarningAmber)
                Text(
                    text = "  MELHOR TEMPO:",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = state.bestTimeSeconds?.let { String.format(Locale.US, "%.2f s", it) } ?: "--.-- s",
                color = WarningAmber,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Arm / Start Button
        Button(
            onClick = onArmTest,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isArmed) SuccessGreen else RaceRed,
                contentColor = CarbonBlack
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Text(
                text = if (state.isArmed) "PRONTO (AGUARDANDO ARRANQUE)" else "ARMAR NOVO TESTE (0-100)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // History of Runs
        if (state.runsHistory.isNotEmpty()) {
            Text(
                text = "HISTÓRICO DE ARRANQUES",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(state.runsHistory.reversed()) { index, run ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurface)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Arranque #${state.runsHistory.size - index}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = String.format(Locale.US, "%.2f s", run.timeSeconds),
                            color = NeonCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
