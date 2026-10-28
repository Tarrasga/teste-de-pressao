package com.testedepressao.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.testedepressao.app.data.models.VehicleTelemetry
import com.testedepressao.app.ui.components.TelemetryCard
import com.testedepressao.app.ui.components.TurboGauge
import com.testedepressao.app.ui.theme.NeonCyan
import com.testedepressao.app.ui.theme.RaceRed
import com.testedepressao.app.ui.theme.TextPrimary
import com.testedepressao.app.ui.theme.TextSecondary
import com.testedepressao.app.ui.theme.WarningAmber

@Composable
fun LiveTelemetryScreen(
    telemetry: VehicleTelemetry,
    onResetPeak: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main Turbo Gauge
        TurboGauge(
            boostBar = telemetry.boostPressureBar,
            peakBoostBar = telemetry.peakBoostBar
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onResetPeak,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
            Text("Repor Pico de Pressão", fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // First Row: RPM and Speed
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TelemetryCard(
                title = "Rotações",
                value = telemetry.rpm.toString(),
                unit = "RPM",
                accentColor = if (telemetry.rpm > 5000) RaceRed else NeonCyan,
                modifier = Modifier.weight(1f)
            )
            TelemetryCard(
                title = "Velocidade",
                value = telemetry.speedKmh.toString(),
                unit = "KM/H",
                accentColor = TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Second Row: Temperature and Intake Pressure
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TelemetryCard(
                title = "Líq. Refrigeração",
                value = telemetry.coolantTempC.toString(),
                unit = "°C",
                accentColor = if (telemetry.coolantTempC > 105) RaceRed else if (telemetry.coolantTempC < 75) WarningAmber else NeonCyan,
                modifier = Modifier.weight(1f)
            )
            TelemetryCard(
                title = "Pressão Coletor",
                value = telemetry.intakePressureKpa.toString(),
                unit = "kPa (MAP)",
                accentColor = TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Atmospheric Reference Card
        TelemetryCard(
            title = "Pressão Atmosférica (Referência Barométrica)",
            value = telemetry.barometricPressureKpa.toString(),
            unit = "kPa (~${String.format("%.2f", telemetry.barometricPressureKpa / 100f)} Bar)",
            accentColor = TextSecondary,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
