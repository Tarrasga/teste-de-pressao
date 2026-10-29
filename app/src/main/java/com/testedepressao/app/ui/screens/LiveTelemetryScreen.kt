package com.testedepressao.app.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.testedepressao.app.data.models.VehicleTelemetry
import com.testedepressao.app.ui.components.CompanionGaugeMode
import com.testedepressao.app.ui.components.SportCompanionGauge
import com.testedepressao.app.ui.components.TelemetryCard
import com.testedepressao.app.ui.components.TurboGauge
import com.testedepressao.app.ui.theme.CarbonBlack
import com.testedepressao.app.ui.theme.DarkBorder
import com.testedepressao.app.ui.theme.DarkSurface
import com.testedepressao.app.ui.theme.NeedleRed
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
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        LandscapeCockpitView(
            telemetry = telemetry,
            onResetPeak = onResetPeak,
            modifier = modifier
        )
    } else {
        PortraitCockpitView(
            telemetry = telemetry,
            onResetPeak = onResetPeak,
            modifier = modifier
        )
    }
}

/**
 * Widescreen Dual Cockpit Dashboard (Horizontal / Landscape)
 * Side-by-side Turbo Gauge & RPM/Speed Gauge both with red illuminated needles.
 */
@Composable
private fun LandscapeCockpitView(
    telemetry: VehicleTelemetry,
    onResetPeak: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonBlack)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Gauge: Turbo Boost with Red Needle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            TurboGauge(
                boostBar = telemetry.boostPressureBar,
                peakBoostBar = telemetry.peakBoostBar,
                modifier = Modifier.size(240.dp)
            )
        }

        // Center Quick Bar: Essential Engine Telemetry & Peak Reset
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .width(130.dp)
                .fillMaxHeight()
                .padding(vertical = 4.dp)
        ) {
            CompactLiveMetric(
                label = "ÁGUA",
                value = "${telemetry.coolantTempC}°C",
                color = if (telemetry.coolantTempC > 105) RaceRed else if (telemetry.coolantTempC < 75) WarningAmber else NeonCyan
            )
            Spacer(modifier = Modifier.height(6.dp))
            CompactLiveMetric(
                label = "MAP",
                value = "${telemetry.intakePressureKpa} kPa",
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            CompactLiveMetric(
                label = "BATERIA",
                value = telemetry.batteryVoltage?.let { String.format("%.1fV", it) } ?: "--",
                color = if ((telemetry.batteryVoltage ?: 14f) < 12.2f) WarningAmber else NeonCyan
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onResetPeak,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Repor", fontSize = 10.sp)
            }
        }

        // Right Gauge: RPM / Speed with Red Needle (Tap to toggle)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            SportCompanionGauge(
                rpm = telemetry.rpm,
                speedKmh = telemetry.speedKmh,
                modifier = Modifier.size(240.dp),
                initialMode = CompanionGaugeMode.RPM
            )
        }
    }
}

/**
 * Vertical / Portrait Dashboard
 */
@Composable
private fun PortraitCockpitView(
    telemetry: VehicleTelemetry,
    onResetPeak: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var selectedGaugeTab by remember { mutableStateOf(0) } // 0: Turbo, 1: RPM/Speed

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonBlack)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mode Selector: Turbo Boost or RPM/Speed Gauge
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            GaugeSelectorPill(
                title = "Pressão Turbo",
                isSelected = selectedGaugeTab == 0,
                onClick = { selectedGaugeTab = 0 }
            )
            GaugeSelectorPill(
                title = "Rotações / Velocidade",
                isSelected = selectedGaugeTab == 1,
                onClick = { selectedGaugeTab = 1 }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Active Sports Gauge with Red Needle
        if (selectedGaugeTab == 0) {
            TurboGauge(
                boostBar = telemetry.boostPressureBar,
                peakBoostBar = telemetry.peakBoostBar,
                modifier = Modifier.size(280.dp)
            )
        } else {
            SportCompanionGauge(
                rpm = telemetry.rpm,
                speedKmh = telemetry.speedKmh,
                modifier = Modifier.size(280.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onResetPeak,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
            shape = RoundedCornerShape(10.dp)
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
                accentColor = if (telemetry.rpm > 4500) NeedleRed else NeonCyan,
                modifier = Modifier.weight(1f)
            )
            TelemetryCard(
                title = "Velocidade Real",
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
                accentColor = if (telemetry.coolantTempC > 105) NeedleRed else if (telemetry.coolantTempC < 75) WarningAmber else NeonCyan,
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

@Composable
private fun GaugeSelectorPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) NeedleRed else DarkSurface)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) TextPrimary else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun CompactLiveMetric(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    }
}
