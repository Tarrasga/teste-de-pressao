package com.testedepressao.app.data.models

data class VehicleTelemetry(
    val boostPressureBar: Float = 0.0f,
    val peakBoostBar: Float = 0.0f,
    val rpm: Int = 0,
    val speedKmh: Int = 0,
    val coolantTempC: Int = 0,
    val oilTempC: Int? = null,
    val batteryVoltage: Float? = null,
    val engineLoadPercent: Int? = null,
    val intakeAirTempC: Int? = null,
    val throttlePercent: Int? = null,
    val mafGramsPerSec: Float? = null,
    val fuelLevelPercent: Int? = null,
    val engineRunTimeSec: Int? = null,
    val intakePressureKpa: Int = 101,
    val barometricPressureKpa: Int = 101
)

data class AccelerationRun(
    val timeSeconds: Float,
    val timestamp: Long = System.currentTimeMillis()
)

data class AccelerationState(
    val isArmed: Boolean = false,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val currentSpeed: Int = 0,
    val targetSpeed: Int = 100,
    val elapsedSeconds: Float = 0.0f,
    val bestTimeSeconds: Float? = null,
    val runsHistory: List<AccelerationRun> = emptyList()
)

data class DiagnosticTroubleCode(
    val code: String,
    val description: String,
    val severity: String = "Aviso"
)

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}
