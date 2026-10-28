package com.testedepressao.app.data

import com.testedepressao.app.data.models.AccelerationRun
import com.testedepressao.app.data.models.AccelerationState
import com.testedepressao.app.data.models.ConnectionStatus
import com.testedepressao.app.data.models.DiagnosticTroubleCode
import com.testedepressao.app.data.models.VehicleTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max

class ObdManager(
    private val bluetoothManager: CarBluetoothManager,
    private val scope: CoroutineScope
) {
    private val _telemetry = MutableStateFlow(VehicleTelemetry())
    val telemetry: StateFlow<VehicleTelemetry> = _telemetry.asStateFlow()

    private val _accelerationState = MutableStateFlow(AccelerationState())
    val accelerationState: StateFlow<AccelerationState> = _accelerationState.asStateFlow()

    private val _dtcCodes = MutableStateFlow<List<DiagnosticTroubleCode>>(emptyList())
    val dtcCodes: StateFlow<List<DiagnosticTroubleCode>> = _dtcCodes.asStateFlow()

    private val _isScanningDtc = MutableStateFlow(false)
    val isScanningDtc: StateFlow<Boolean> = _isScanningDtc.asStateFlow()

    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private val _psaConsoleLog = MutableStateFlow<List<String>>(listOf("Consola PSA pronta para comandos."))
    val psaConsoleLog: StateFlow<List<String>> = _psaConsoleLog.asStateFlow()

    private var pollingJob: Job? = null
    private var demoJob: Job? = null
    private var launchStartTime: Long = 0L

    private var barometricPressureKpa = 101 // ~1.01 Bar atmospheric pressure

    init {
        scope.launch {
            bluetoothManager.connectionStatus.collect { status ->
                if (status == ConnectionStatus.CONNECTED) {
                    initElm()
                    startPolling()
                } else {
                    stopPolling()
                }
            }
        }
    }

    private suspend fun initElm() {
        bluetoothManager.sendCommand("ATZ", 1500) // Reset ELM
        bluetoothManager.sendCommand("ATE0", 500)  // Echo Off
        bluetoothManager.sendCommand("ATL0", 500)  // Linefeeds Off
        bluetoothManager.sendCommand("ATH0", 500)  // Headers Off
        bluetoothManager.sendCommand("ATSP0", 800) // Auto protocol selection
        
        // Read barometric pressure once
        val baroResp = bluetoothManager.sendCommand("0133", 500)
        parseBaro(baroResp)
    }

    fun startPolling() {
        if (_isDemoMode.value) return
        pollingJob?.cancel()
        pollingJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    // 1. Manifold Absolute Pressure (MAP) -> For Turbo Boost
                    val mapResp = bluetoothManager.sendCommand("010B", 300)
                    val mapKpa = parseSingleBytePid(mapResp, "010B") ?: _telemetry.value.intakePressureKpa

                    // Boost calculation: (MAP - Baro) / 100 in Bar
                    val boostBar = ((mapKpa - barometricPressureKpa) / 100.0f).coerceIn(-1.0f, 3.5f)
                    val currentPeak = max(_telemetry.value.peakBoostBar, boostBar)

                    // 2. Speed (km/h) -> For 0-100 acceleration
                    val speedResp = bluetoothManager.sendCommand("010D", 250)
                    val speedKmh = parseSingleBytePid(speedResp, "010D") ?: _telemetry.value.speedKmh

                    // 3. RPM
                    val rpmResp = bluetoothManager.sendCommand("010C", 250)
                    val rpm = parseRpm(rpmResp) ?: _telemetry.value.rpm

                    // 4. Coolant Temp (°C)
                    val tempResp = bluetoothManager.sendCommand("0105", 250)
                    val tempC = parseTemp(tempResp) ?: _telemetry.value.coolantTempC

                    _telemetry.value = _telemetry.value.copy(
                        boostPressureBar = boostBar,
                        peakBoostBar = currentPeak,
                        rpm = rpm,
                        speedKmh = speedKmh,
                        coolantTempC = tempC,
                        intakePressureKpa = mapKpa,
                        barometricPressureKpa = barometricPressureKpa
                    )

                    updateAccelerationTest(speedKmh)
                } catch (e: Exception) {
                    delay(500)
                }
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun updateAccelerationTest(currentSpeed: Int) {
        val state = _accelerationState.value
        if (!state.isArmed && !state.isRunning) return

        if (state.isArmed && !state.isRunning) {
            if (currentSpeed > 2) {
                // Car started accelerating!
                launchStartTime = System.currentTimeMillis()
                _accelerationState.value = state.copy(
                    isArmed = false,
                    isRunning = true,
                    isFinished = false,
                    elapsedSeconds = 0.0f,
                    currentSpeed = currentSpeed
                )
            }
            return
        }

        if (state.isRunning) {
            val elapsed = (System.currentTimeMillis() - launchStartTime) / 1000.0f
            if (currentSpeed >= state.targetSpeed) {
                // Completed!
                val best = state.bestTimeSeconds?.let { minOf(it, elapsed) } ?: elapsed
                val newRuns = state.runsHistory + AccelerationRun(elapsed)
                _accelerationState.value = state.copy(
                    isRunning = false,
                    isFinished = true,
                    elapsedSeconds = elapsed,
                    bestTimeSeconds = best,
                    currentSpeed = currentSpeed,
                    runsHistory = newRuns
                )
            } else {
                _accelerationState.value = state.copy(
                    elapsedSeconds = elapsed,
                    currentSpeed = currentSpeed
                )
            }
        }
    }

    fun armAccelerationTest() {
        _accelerationState.value = _accelerationState.value.copy(
            isArmed = true,
            isRunning = false,
            isFinished = false,
            elapsedSeconds = 0.0f
        )
    }

    fun resetPeakBoost() {
        _telemetry.value = _telemetry.value.copy(peakBoostBar = _telemetry.value.boostPressureBar)
    }

    // --- Diagnostic Trouble Codes (DTC) ---

    suspend fun readDtcCodes() {
        _isScanningDtc.value = true
        _dtcCodes.value = emptyList()

        if (_isDemoMode.value) {
            delay(1500)
            _dtcCodes.value = listOf(
                DiagnosticTroubleCode("P0234", "Condição de Sobrepressão do Turbo (Overboost)", "Aviso"),
                DiagnosticTroubleCode("P0100", "Circuito do Sensor de Fluxo de Ar (MAF) - Sinal Fora dos Limites", "Informativo")
            )
            _isScanningDtc.value = false
            return
        }

        try {
            val resp = bluetoothManager.sendCommand("03", 2000)
            val parsed = parseDtcResponse(resp)
            _dtcCodes.value = parsed
        } catch (e: Exception) {
            _dtcCodes.value = emptyList()
        } finally {
            _isScanningDtc.value = false
        }
    }

    suspend fun clearDtcCodes(): String {
        if (_isDemoMode.value) {
            delay(1000)
            _dtcCodes.value = emptyList()
            return "Erros apagados com sucesso (Modo Demonstração)!"
        }

        val resp = bluetoothManager.sendCommand("04", 2500)
        _dtcCodes.value = emptyList()
        return "Comando de limpeza (Modo 04) enviado: $resp"
    }

    // --- Peugeot 508 / PSA Lab: Cores e Comandos Avançados ---

    suspend fun sendCustomElmCommand(command: String): String {
        logPsa(">> $command")
        if (_isDemoMode.value) {
            delay(300)
            val mockResp = when {
                command.startsWith("AT") -> "OK"
                command.startsWith("22") -> "62 21 00 01 02 (Tema Cuivré/Redline ativo)"
                else -> "OK / COMANDO SIMULADO"
            }
            logPsa("<< $mockResp")
            return mockResp
        }
        val resp = bluetoothManager.sendCommand(command, 1200)
        logPsa("<< $resp")
        return resp
    }

    suspend fun testPeugeotSportMode(): String {
        logPsa("--- Testar Modo Sport (Ativar Vermelho no Painel) ---")
        sendCustomElmCommand("ATSH 7B0") // Set Header to NAC/BSI
        return sendCustomElmCommand("2F 12 34 03 01") // Mock I/O control for sport ambiance
    }

    suspend fun testPeugeotThemeCopper(): String {
        logPsa("--- Testar Tema Cuivré / Cobre (Original 508) ---")
        sendCustomElmCommand("ATSH 7B3") // Infotainment NAC Address
        return sendCustomElmCommand("22 21 00") // Read Display Parameters
    }

    private fun logPsa(msg: String) {
        val current = _psaConsoleLog.value.toMutableList()
        current.add(msg)
        if (current.size > 50) current.removeAt(0)
        _psaConsoleLog.value = current
    }

    // --- Demo Mode Simulation ---

    fun toggleDemoMode(enabled: Boolean) {
        _isDemoMode.value = enabled
        if (enabled) {
            stopPolling()
            startDemoSimulation()
        } else {
            demoJob?.cancel()
            demoJob = null
            if (bluetoothManager.connectionStatus.value == ConnectionStatus.CONNECTED) {
                startPolling()
            }
        }
    }

    private fun startDemoSimulation() {
        demoJob?.cancel()
        demoJob = scope.launch(Dispatchers.Default) {
            var simSpeed = 0
            var simRpm = 850
            var simBoost = -0.65f
            var timeStep = 0f

            while (isActive) {
                timeStep += 0.1f
                // Simulate driving dynamics
                if (_accelerationState.value.isArmed || _accelerationState.value.isRunning) {
                    // Accelerating hard!
                    simSpeed = (simSpeed + 3).coerceAtMost(120)
                    simRpm = 2000 + (simSpeed * 40) % 4500
                    simBoost = (1.45f + kotlin.math.sin(timeStep.toDouble()).toFloat() * 0.3f).coerceIn(0.5f, 2.1f)
                } else {
                    // Normal cruising
                    simSpeed = (50 + kotlin.math.sin(timeStep * 0.5).toFloat() * 15).toInt()
                    simRpm = 1800 + (simSpeed * 10)
                    simBoost = (0.2f + kotlin.math.sin(timeStep.toDouble()).toFloat() * 0.5f).coerceIn(-0.7f, 1.3f)
                }

                val currentPeak = max(_telemetry.value.peakBoostBar, simBoost)
                _telemetry.value = VehicleTelemetry(
                    boostPressureBar = simBoost,
                    peakBoostBar = currentPeak,
                    rpm = simRpm,
                    speedKmh = simSpeed,
                    coolantTempC = 90,
                    intakePressureKpa = (101 + simBoost * 100).toInt(),
                    barometricPressureKpa = 101
                )

                updateAccelerationTest(simSpeed)
                delay(100)
            }
        }
    }

    // --- Helpers / Parsers ---

    private fun parseSingleBytePid(response: String, pid: String): Int? {
        val clean = response.replace(" ", "").trim()
        val expectedHeader = "41" + pid.substring(2)
        val idx = clean.indexOf(expectedHeader)
        if (idx != -1 && idx + 6 <= clean.length) {
            val byteHex = clean.substring(idx + 4, idx + 6)
            return byteHex.toIntOrNull(16)
        }
        return null
    }

    private fun parseRpm(response: String): Int? {
        val clean = response.replace(" ", "").trim()
        val idx = clean.indexOf("410C")
        if (idx != -1 && idx + 8 <= clean.length) {
            val a = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: return null
            val b = clean.substring(idx + 6, idx + 8).toIntOrNull(16) ?: return null
            return ((a * 256) + b) / 4
        }
        return null
    }

    private fun parseTemp(response: String): Int? {
        val b = parseSingleBytePid(response, "0105") ?: return null
        return b - 40
    }

    private fun parseBaro(response: String) {
        val b = parseSingleBytePid(response, "0133")
        if (b != null && b in 60..120) {
            barometricPressureKpa = b
        }
    }

    private fun parseDtcResponse(response: String): List<DiagnosticTroubleCode> {
        val list = mutableListOf<DiagnosticTroubleCode>()
        val clean = response.replace(" ", "").trim()
        if (clean.contains("4300") || clean.contains("NODATA") || clean.isEmpty()) {
            return list
        }

        // Standard OBD DTC response: 43 [count] [code1] [code2]...
        val idx = clean.indexOf("43")
        if (idx != -1 && idx + 4 <= clean.length) {
            val codesHex = clean.substring(idx + 2)
            var i = 2
            while (i + 4 <= codesHex.length) {
                val hex1 = codesHex.substring(i, i + 2).toIntOrNull(16) ?: break
                val hex2 = codesHex.substring(i + 2, i + 4)
                if (hex1 == 0 && hex2 == "00") break

                val prefix = when ((hex1 and 0xC0) shr 6) {
                    0 -> "P"
                    1 -> "C"
                    2 -> "B"
                    3 -> "U"
                    else -> "P"
                }
                val firstDigit = (hex1 and 0x30) shr 4
                val secondDigit = hex1 and 0x0F
                val code = "$prefix$firstDigit$secondDigit$hex2"
                
                list.add(DiagnosticTroubleCode(code, lookupDtcDescription(code)))
                i += 4
            }
        }
        return list
    }

    private fun lookupDtcDescription(code: String): String {
        return when (code) {
            "P0234" -> "Condição de Sobrepressão do Turbo (Overboost)"
            "P0299" -> "Subpressão no Turbocompressor (Underboost)"
            "P0100" -> "Circuito do Sensor MAF (Fluxo de Massa de Ar)"
            "P0101" -> "Faixa/Desempenho do Sensor MAF"
            "P0171" -> "Mistura demasiado pobre (Banco 1)"
            "P0300" -> "Detetada falha de ignição em múltiplos cilindros"
            "P0420" -> "Eficiência do catalisador abaixo do limite (Banco 1)"
            "P20EE" -> "Eficiência do catalisador SCR/AdBlue abaixo do limite"
            "P2463" -> "Acumulação de fuligem no Filtro de Partículas (FAP/DPF)"
            else -> "Código de avaria registado na unidade de controlo eletrónico."
        }
    }
}
