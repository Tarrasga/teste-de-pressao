package com.testedepressao.app.data.models

import androidx.compose.ui.graphics.Color
import com.testedepressao.app.ui.theme.NeonCyan
import com.testedepressao.app.ui.theme.RaceRed
import com.testedepressao.app.ui.theme.SuccessGreen

enum class DrivingStyle(
    val displayName: String,
    val emoji: String,
    val description: String,
    val color: Color
) {
    ECONOMICA(
        displayName = "Económica",
        emoji = "🍃",
        description = "Condução suave e eficiente, foco em baixos consumos e rotações contidas.",
        color = SuccessGreen
    ),
    DINAMICA(
        displayName = "Dinâmica",
        emoji = "⚡",
        description = "Ritmo fluído de dia a dia, acelerações equilibradas e boa agilidade.",
        color = NeonCyan
    ),
    DESPORTIVA(
        displayName = "Desportiva",
        emoji = "🏎️",
        description = "Condução desportiva com forte pressão de turbo, acelerações a fundo e altas rotações.",
        color = RaceRed
    )
}

data class LiveTripData(
    val isRecording: Boolean = false,
    val startTimeMs: Long = 0L,
    val durationSeconds: Long = 0L,
    val distanceKm: Float = 0.0f,
    val instantConsumptionL100km: Float = 0.0f,
    val instantConsumptionLh: Float = 0.7f,
    val isStopped: Boolean = true,
    val avgConsumptionL100km: Float = 0.0f,
    val fuelConsumedLiters: Float = 0.0f,
    val avgSpeedKmh: Float = 0.0f,
    val maxSpeedKmh: Int = 0,
    val maxBoostBar: Float = 0.0f,
    val maxRpm: Int = 0,
    val ecoScore: Int = 85,
    val currentStyle: DrivingStyle = DrivingStyle.DINAMICA
)

data class TripRecord(
    val id: String,
    val timestamp: Long,
    val formattedDate: String,
    val durationFormatted: String,
    val distanceKm: Float,
    val avgConsumptionL100km: Float,
    val fuelConsumedLiters: Float,
    val avgSpeedKmh: Float,
    val maxSpeedKmh: Int,
    val maxBoostBar: Float,
    val drivingStyle: DrivingStyle,
    val ecoScore: Int
)
