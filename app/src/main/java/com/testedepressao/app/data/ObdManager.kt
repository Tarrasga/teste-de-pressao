package com.testedepressao.app.data

import com.testedepressao.app.data.models.AccelerationRun
import com.testedepressao.app.data.models.AccelerationState
import com.testedepressao.app.data.models.BatteryCondition
import com.testedepressao.app.data.models.BatteryHealthData
import com.testedepressao.app.data.models.ConnectionStatus
import com.testedepressao.app.data.models.DiagnosticTroubleCode
import com.testedepressao.app.data.models.SensorCategory
import com.testedepressao.app.data.models.SensorItem
import com.testedepressao.app.data.models.VehicleTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.max

class ObdManager(
    private val bluetoothManager: CarBluetoothManager,
    private val settingsPreferences: SettingsPreferences,
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

    private val _isScanningPsaDtc = MutableStateFlow(false)
    val isScanningPsaDtc: StateFlow<Boolean> = _isScanningPsaDtc.asStateFlow()

    private val _batteryHealth = MutableStateFlow(BatteryHealthData())
    val batteryHealth: StateFlow<BatteryHealthData> = _batteryHealth.asStateFlow()

    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private val _psaConsoleLog = MutableStateFlow<List<String>>(listOf("Consola PSA pronta para comandos."))
    val psaConsoleLog: StateFlow<List<String>> = _psaConsoleLog.asStateFlow()

    private var pollingJob: Job? = null
    private var demoJob: Job? = null
    private var launchStartTime: Long = 0L

    private var barometricPressureKpa = 101 // ~1.01 Bar atmospheric pressure
    private var secondarySensorIndex = 0

    // Full list of sensors combined with current telemetry and enabled state
    val allSensors: StateFlow<List<SensorItem>> = combine(
        _telemetry,
        settingsPreferences.enabledSensors
    ) { telem, enabledSet ->
        listOf(
            SensorItem(
                id = "turbo",
                name = "Pressão de Turbo (Boost)",
                pid = "010B",
                category = SensorCategory.MOTOR,
                unit = "Bar",
                value = String.format(Locale.US, "%+.2f", telem.boostPressureBar),
                numericValue = telem.boostPressureBar,
                isEnabled = enabledSet.contains("turbo"),
                description = "Calculado a partir do MAP subtraindo a pressão barométrica."
            ),
            SensorItem(
                id = "rpm",
                name = "Rotações do Motor",
                pid = "010C",
                category = SensorCategory.MOTOR,
                unit = "RPM",
                value = "${telem.rpm}",
                numericValue = telem.rpm.toFloat(),
                isEnabled = enabledSet.contains("rpm"),
                description = "Velocidade de rotação da cambota em rotações por minuto."
            ),
            SensorItem(
                id = "speed",
                name = "Velocidade Real",
                pid = "010D",
                category = SensorCategory.DINAMICA,
                unit = "km/h",
                value = "${telem.speedKmh}",
                numericValue = telem.speedKmh.toFloat(),
                isEnabled = enabledSet.contains("speed"),
                description = "Velocidade reportada pelo sensor de velocidade do veículo (VSS)."
            ),
            SensorItem(
                id = "coolant",
                name = "Líquido de Refrigeração",
                pid = "0105",
                category = SensorCategory.TEMPERATURAS,
                unit = "°C",
                value = "${telem.coolantTempC}",
                numericValue = telem.coolantTempC.toFloat(),
                isEnabled = enabledSet.contains("coolant"),
                description = "Temperatura da água/anticongelante do circuito do motor."
            ),
            SensorItem(
                id = "oil_temp",
                name = "Temperatura do Óleo",
                pid = "015C",
                category = SensorCategory.TEMPERATURAS,
                unit = "°C",
                value = telem.oilTempC?.let { "$it" } ?: "--",
                numericValue = telem.oilTempC?.toFloat() ?: 0f,
                isEnabled = enabledSet.contains("oil_temp"),
                description = "Temperatura do óleo lubrificante no cárter (essencial para motores PSA)."
            ),
            SensorItem(
                id = "voltage",
                name = "Tensão da Bateria / Alternador",
                pid = "0142",
                category = SensorCategory.ELETRICA,
                unit = "V",
                value = telem.batteryVoltage?.let { String.format(Locale.US, "%.1f", it) } ?: "--",
                numericValue = telem.batteryVoltage ?: 0f,
                isEnabled = enabledSet.contains("voltage"),
                description = "Tensão fornecida pelo alternador e módulo de controlo (13.8V - 14.7V ideal)."
            ),
            SensorItem(
                id = "engine_load",
                name = "Carga do Motor",
                pid = "0104",
                category = SensorCategory.MOTOR,
                unit = "%",
                value = telem.engineLoadPercent?.let { "$it" } ?: "--",
                numericValue = telem.engineLoadPercent?.toFloat() ?: 0f,
                isEnabled = enabledSet.contains("engine_load"),
                description = "Percentagem de binário e esforço atual do motor calculada pela centralina."
            ),
            SensorItem(
                id = "intake_temp",
                name = "Temperatura de Admissão (IAT)",
                pid = "010F",
                category = SensorCategory.TEMPERATURAS,
                unit = "°C",
                value = telem.intakeAirTempC?.let { "$it" } ?: "--",
                numericValue = telem.intakeAirTempC?.toFloat() ?: 0f,
                isEnabled = enabledSet.contains("intake_temp"),
                description = "Temperatura do ar após a passagem pelo turbocompressor e intercooler."
            ),
            SensorItem(
                id = "throttle",
                name = "Posição do Acelerador",
                pid = "0111",
                category = SensorCategory.DINAMICA,
                unit = "%",
                value = telem.throttlePercent?.let { "$it" } ?: "--",
                numericValue = telem.throttlePercent?.toFloat() ?: 0f,
                isEnabled = enabledSet.contains("throttle"),
                description = "Percentagem de pressão no pedal do acelerador."
            ),
            SensorItem(
                id = "maf",
                name = "Fluxo de Ar (MAF)",
                pid = "0110",
                category = SensorCategory.MOTOR,
                unit = "g/s",
                value = telem.mafGramsPerSec?.let { String.format(Locale.US, "%.1f", it) } ?: "--",
                numericValue = telem.mafGramsPerSec ?: 0f,
                isEnabled = enabledSet.contains("maf"),
                description = "Volume e massa de ar admitida por segundo medida pelo sensor de fluxo de ar."
            ),
            SensorItem(
                id = "fuel_level",
                name = "Nível de Combustível",
                pid = "012F",
                category = SensorCategory.DINAMICA,
                unit = "%",
                value = telem.fuelLevelPercent?.let { "$it" } ?: "--",
                numericValue = telem.fuelLevelPercent?.toFloat() ?: 0f,
                isEnabled = enabledSet.contains("fuel_level"),
                description = "Percentagem de combustível restante no depósito."
            ),
            SensorItem(
                id = "runtime",
                name = "Tempo de Motor Ligado",
                pid = "011F",
                category = SensorCategory.DINAMICA,
                unit = "min",
                value = telem.engineRunTimeSec?.let { formatRuntime(it) } ?: "--",
                numericValue = telem.engineRunTimeSec?.toFloat() ?: 0f,
                isEnabled = enabledSet.contains("runtime"),
                description = "Tempo decorrido desde o arranque do motor nesta viagem."
            ),
            SensorItem(
                id = "baro",
                name = "Pressão Atmosférica",
                pid = "0133",
                category = SensorCategory.MOTOR,
                unit = "kPa",
                value = "${telem.barometricPressureKpa}",
                numericValue = telem.barometricPressureKpa.toFloat(),
                isEnabled = enabledSet.contains("baro"),
                description = "Pressão do ar ambiente utilizada como referência barométrica."
            )
        )
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

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
                    val enabled = settingsPreferences.enabledSensors.value

                    // PRIORITY 1: Manifold Absolute Pressure (MAP) for Turbo Boost
                    var boostBar = _telemetry.value.boostPressureBar
                    var currentPeak = _telemetry.value.peakBoostBar
                    var mapKpa = _telemetry.value.intakePressureKpa

                    if (enabled.contains("turbo")) {
                        val mapResp = bluetoothManager.sendCommand("010B", 200)
                        val parsedMap = parseSingleBytePid(mapResp, "010B")
                        if (parsedMap != null) {
                            mapKpa = parsedMap
                            boostBar = ((mapKpa - barometricPressureKpa) / 100.0f).coerceIn(-1.0f, 3.5f)
                            currentPeak = max(currentPeak, boostBar)
                        }
                    }

                    // PRIORITY 2: Speed (km/h) for 0-100 acceleration
                    var speedKmh = _telemetry.value.speedKmh
                    if (enabled.contains("speed") || _accelerationState.value.isArmed || _accelerationState.value.isRunning) {
                        val speedResp = bluetoothManager.sendCommand("010D", 180)
                        val parsedSpeed = parseSingleBytePid(speedResp, "010D")
                        if (parsedSpeed != null) {
                            speedKmh = parsedSpeed
                        }
                    }

                    // PRIORITY 3: RPM
                    var rpm = _telemetry.value.rpm
                    if (enabled.contains("rpm")) {
                        val rpmResp = bluetoothManager.sendCommand("010C", 180)
                        val parsedRpm = parseRpm(rpmResp)
                        if (parsedRpm != null) {
                            rpm = parsedRpm
                        }
                    }

                    // SECONDARY SENSORS: Round-Robin (one sensor per loop to keep turbo refresh rate fast!)
                    var coolantTemp = _telemetry.value.coolantTempC
                    var oilTemp = _telemetry.value.oilTempC
                    var voltage = _telemetry.value.batteryVoltage
                    var engineLoad = _telemetry.value.engineLoadPercent
                    var intakeTemp = _telemetry.value.intakeAirTempC
                    var throttle = _telemetry.value.throttlePercent
                    var maf = _telemetry.value.mafGramsPerSec
                    var fuelLevel = _telemetry.value.fuelLevelPercent
                    var runtimeSec = _telemetry.value.engineRunTimeSec

                    val secondaryPool = listOf(
                        "coolant", "oil_temp", "voltage", "engine_load",
                        "intake_temp", "throttle", "maf", "fuel_level", "runtime"
                    ).filter { enabled.contains(it) }

                    if (secondaryPool.isNotEmpty()) {
                        secondarySensorIndex = (secondarySensorIndex + 1) % secondaryPool.size
                        val targetSensor = secondaryPool[secondarySensorIndex]

                        when (targetSensor) {
                            "coolant" -> {
                                val resp = bluetoothManager.sendCommand("0105", 200)
                                parseTemp(resp)?.let { coolantTemp = it }
                            }
                            "oil_temp" -> {
                                val resp = bluetoothManager.sendCommand("015C", 200)
                                parseSingleBytePid(resp, "015C")?.let { oilTemp = it - 40 }
                            }
                            "voltage" -> {
                                val resp = bluetoothManager.sendCommand("0142", 200)
                                parseTwoByteVoltage(resp)?.let {
                                    voltage = it
                                    updateBatteryHealth(it, rpm)
                                }
                            }
                            "engine_load" -> {
                                val resp = bluetoothManager.sendCommand("0104", 200)
                                parsePercent(resp, "0104")?.let { engineLoad = it }
                            }
                            "intake_temp" -> {
                                val resp = bluetoothManager.sendCommand("010F", 200)
                                parseSingleBytePid(resp, "010F")?.let { intakeTemp = it - 40 }
                            }
                            "throttle" -> {
                                val resp = bluetoothManager.sendCommand("0111", 200)
                                parsePercent(resp, "0111")?.let { throttle = it }
                            }
                            "maf" -> {
                                val resp = bluetoothManager.sendCommand("0110", 200)
                                parseMaf(resp)?.let { maf = it }
                            }
                            "fuel_level" -> {
                                val resp = bluetoothManager.sendCommand("012F", 200)
                                parsePercent(resp, "012F")?.let { fuelLevel = it }
                            }
                            "runtime" -> {
                                val resp = bluetoothManager.sendCommand("011F", 200)
                                parseTwoByteInt(resp, "011F")?.let { runtimeSec = it }
                            }
                        }
                    }

                    _telemetry.value = _telemetry.value.copy(
                        boostPressureBar = boostBar,
                        peakBoostBar = currentPeak,
                        rpm = rpm,
                        speedKmh = speedKmh,
                        coolantTempC = coolantTemp,
                        oilTempC = oilTemp,
                        batteryVoltage = voltage,
                        engineLoadPercent = engineLoad,
                        intakeAirTempC = intakeTemp,
                        throttlePercent = throttle,
                        mafGramsPerSec = maf,
                        fuelLevelPercent = fuelLevel,
                        engineRunTimeSec = runtimeSec,
                        intakePressureKpa = mapKpa,
                        barometricPressureKpa = barometricPressureKpa
                    )

                    updateAccelerationTest(speedKmh)
                } catch (e: Exception) {
                    delay(300)
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

    // --- Battery Health Analysis ---

    fun resetCrankTest() {
        val current = _batteryHealth.value
        _batteryHealth.value = current.copy(
            minCrankVoltage = null,
            recommendation = "Dê à chave / arranque o motor para registar a queda mínima de tensão."
        )
    }

    fun updateBatteryHealth(voltage: Float, rpm: Int) {
        val current = _batteryHealth.value
        val isEngineRunning = rpm > 500
        val isCharging = voltage >= 13.5f

        var resting = current.restingVoltage
        var minCrank = current.minCrankVoltage
        var charging = current.chargingVoltage

        if (!isEngineRunning) {
            // Engine stopped: measure resting open-circuit voltage
            resting = if (resting == null) voltage else (resting * 0.9f + voltage * 0.1f)
        } else {
            // Engine running: measure charging voltage
            charging = if (charging == null) voltage else (charging * 0.9f + voltage * 0.1f)
        }

        // Detect cranking drop: if engine starts or voltage dips sharply
        if (voltage in 6.0f..11.8f) {
            minCrank = if (minCrank == null) voltage else minOf(minCrank, voltage)
        }

        // State of Charge (SoC) estimation based on AGM battery resting voltage curve
        val refVolt = resting ?: voltage
        val soc = when {
            refVolt >= 12.80f -> 100
            refVolt >= 12.65f -> 95
            refVolt >= 12.50f -> 85
            refVolt >= 12.35f -> 70
            refVolt >= 12.20f -> 55
            refVolt >= 12.05f -> 40
            refVolt >= 11.90f -> 25
            else -> 10
        }

        val condition = when {
            minCrank != null && minCrank < 9.0f -> BatteryCondition.CRITICA
            (resting != null && resting < 12.10f) || (minCrank != null && minCrank < 9.6f) || soc < 50 -> BatteryCondition.FRACA
            soc >= 85 && (minCrank == null || minCrank >= 10.2f) -> BatteryCondition.EXCELENTE
            else -> BatteryCondition.BOA
        }

        val recommendation = when (condition) {
            BatteryCondition.CRITICA -> "Aviso Crítico: Bateria muito degradada (<9.0V no arranque). Risco de não pegar."
            BatteryCondition.FRACA -> "Bateria Fraca (<12.2V): É a causa do corte elétrico 'Modo Economia de Energia' prematuro no seu 508 e da desativação do Start&Stop."
            BatteryCondition.BOA -> "Bateria em bom estado operacional. Alternador a carregar normalmente."
            BatteryCondition.EXCELENTE -> "Bateria em estado excelente (tensão e resposta de arranque ideais)."
        }

        _batteryHealth.value = BatteryHealthData(
            liveVoltage = voltage,
            restingVoltage = resting,
            minCrankVoltage = minCrank,
            chargingVoltage = charging,
            estimatedSocPercent = soc,
            isAlternatorCharging = isCharging,
            isEngineRunning = isEngineRunning,
            condition = condition,
            recommendation = recommendation
        )
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

    suspend fun readAdvancedPsaDtcCodes() {
        _isScanningPsaDtc.value = true
        _dtcCodes.value = emptyList()

        if (_isDemoMode.value) {
            delay(1800)
            _dtcCodes.value = listOf(
                DiagnosticTroubleCode("B1805", "Inibição de paragem automática Start&Stop (Causa habitual: Bateria de 12V com carga insuficiente)", "Aviso", "BSI (Carroçaria)"),
                DiagnosticTroubleCode("B1003", "Defeito de telecodificação segura da BSI", "Informativo", "BSI (Carroçaria)"),
                DiagnosticTroubleCode("P1351", "Circuito do relé de pré-aquecimento - Velas de incandescência não alimentadas", "Aviso", "Motor PSA (CMM)"),
                DiagnosticTroubleCode("U1218", "Comunicação na rede CAN com a BSI - Dados inválidos recebidos", "Informativo", "Rede CAN")
            )
            _isScanningPsaDtc.value = false
            return
        }

        val allPsaCodes = mutableListOf<DiagnosticTroubleCode>()
        try {
            // 1. Scan BSI via UDS (ID 0x752)
            bluetoothManager.sendCommand("ATSH 752", 300)
            val bsiResp = bluetoothManager.sendCommand("19 02 FF", 2000)
            allPsaCodes.addAll(parsePsaUdsDtcResponse(bsiResp, "BSI (Carroçaria)"))

            // 2. Scan Engine ECU via UDS (ID 0x7E0)
            bluetoothManager.sendCommand("ATSH 7E0", 300)
            val engineResp = bluetoothManager.sendCommand("19 02 FF", 2000)
            allPsaCodes.addAll(parsePsaUdsDtcResponse(engineResp, "Motor PSA"))

            // 3. Scan Instrument Cluster Combine (ID 0x7B0)
            bluetoothManager.sendCommand("ATSH 7B0", 300)
            val clusterResp = bluetoothManager.sendCommand("19 02 FF", 2000)
            allPsaCodes.addAll(parsePsaUdsDtcResponse(clusterResp, "i-Cockpit"))

            // Reset header back to standard broadcast
            bluetoothManager.sendCommand("ATSH 7DF", 300)

            _dtcCodes.value = allPsaCodes
        } catch (e: Exception) {
            _dtcCodes.value = allPsaCodes
        } finally {
            _isScanningPsaDtc.value = false
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

    // --- Peugeot 508 / PSA Lab ---

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
        sendCustomElmCommand("ATSH 7B0")
        return sendCustomElmCommand("2F 12 34 03 01")
    }

    suspend fun testPeugeotThemeCopper(): String {
        logPsa("--- Testar Tema Cuivré / Cobre (Original 508) ---")
        sendCustomElmCommand("ATSH 7B3")
        return sendCustomElmCommand("22 21 00")
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
            var simRuntime = 120

            while (isActive) {
                timeStep += 0.1f
                simRuntime += 1

                if (_accelerationState.value.isArmed || _accelerationState.value.isRunning) {
                    simSpeed = (simSpeed + 3).coerceAtMost(120)
                    simRpm = 2000 + (simSpeed * 40) % 4500
                    simBoost = (1.45f + kotlin.math.sin(timeStep.toDouble()).toFloat() * 0.3f).coerceIn(0.5f, 2.1f)
                } else {
                    simSpeed = (50 + kotlin.math.sin(timeStep * 0.5).toFloat() * 15).toInt()
                    simRpm = 1800 + (simSpeed * 10)
                    simBoost = (0.2f + kotlin.math.sin(timeStep.toDouble()).toFloat() * 0.5f).coerceIn(-0.7f, 1.3f)
                }

                val currentPeak = max(_telemetry.value.peakBoostBar, simBoost)
                val simOil = 92 + (simSpeed / 20)
                val simVoltage = 14.2f + kotlin.math.sin(timeStep.toDouble()).toFloat() * 0.2f
                val simLoad = (25 + (simBoost.coerceAtLeast(0f) * 35)).toInt().coerceIn(10, 99)
                val simIat = 28 + (simBoost.coerceAtLeast(0f) * 8).toInt()
                val simThrottle = (simLoad * 0.9f).toInt()
                val simMaf = (12.0f + (simRpm / 100f) * 2.5f)

                _telemetry.value = VehicleTelemetry(
                    boostPressureBar = simBoost,
                    peakBoostBar = currentPeak,
                    rpm = simRpm,
                    speedKmh = simSpeed,
                    coolantTempC = 90,
                    oilTempC = simOil,
                    batteryVoltage = simVoltage,
                    engineLoadPercent = simLoad,
                    intakeAirTempC = simIat,
                    throttlePercent = simThrottle,
                    mafGramsPerSec = simMaf,
                    fuelLevelPercent = 68,
                    engineRunTimeSec = simRuntime,
                    intakePressureKpa = (101 + simBoost * 100).toInt(),
                    barometricPressureKpa = 101
                )

                updateBatteryHealth(simVoltage, simRpm)
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

    private fun parsePercent(response: String, pid: String): Int? {
        val b = parseSingleBytePid(response, pid) ?: return null
        return (b * 100) / 255
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

    private fun parseTwoByteVoltage(response: String): Float? {
        val clean = response.replace(" ", "").trim()
        val idx = clean.indexOf("4142")
        if (idx != -1 && idx + 8 <= clean.length) {
            val a = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: return null
            val b = clean.substring(idx + 6, idx + 8).toIntOrNull(16) ?: return null
            return ((a * 256) + b) / 1000.0f
        }
        return null
    }

    private fun parseMaf(response: String): Float? {
        val clean = response.replace(" ", "").trim()
        val idx = clean.indexOf("4110")
        if (idx != -1 && idx + 8 <= clean.length) {
            val a = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: return null
            val b = clean.substring(idx + 6, idx + 8).toIntOrNull(16) ?: return null
            return ((a * 256) + b) / 100.0f
        }
        return null
    }

    private fun parseTwoByteInt(response: String, pid: String): Int? {
        val clean = response.replace(" ", "").trim()
        val expected = "41" + pid.substring(2)
        val idx = clean.indexOf(expected)
        if (idx != -1 && idx + 8 <= clean.length) {
            val a = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: return null
            val b = clean.substring(idx + 6, idx + 8).toIntOrNull(16) ?: return null
            return (a * 256) + b
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

    private fun formatRuntime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format(Locale.US, "%02d:%02d", m, s)
    }

    private fun parseDtcResponse(response: String): List<DiagnosticTroubleCode> {
        val list = mutableListOf<DiagnosticTroubleCode>()
        val clean = response.replace(" ", "").trim()
        if (clean.contains("4300") || clean.contains("NODATA") || clean.isEmpty()) {
            return list
        }

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

    private fun parsePsaUdsDtcResponse(response: String, ecuSource: String): List<DiagnosticTroubleCode> {
        val list = mutableListOf<DiagnosticTroubleCode>()
        val clean = response.replace(" ", "").trim()

        // UDS Positive response to 19 02 FF starts with 59 02
        val idx = clean.indexOf("5902")
        if (idx != -1 && idx + 6 <= clean.length) {
            val payload = clean.substring(idx + 4) // skip 5902 and status availability mask
            // Each DTC is 3 bytes (DTC High, DTC Middle, DTC Low/Fault symptom) + 1 byte (Status) = 8 hex chars
            var i = 2 // skip availability mask byte
            while (i + 8 <= payload.length) {
                val hex1 = payload.substring(i, i + 2).toIntOrNull(16) ?: break
                val hex2 = payload.substring(i + 2, i + 4)
                val hexSub = payload.substring(i + 4, i + 6) // fault symptom
                val statusHex = payload.substring(i + 6, i + 8).toIntOrNull(16) ?: 0

                val prefix = when ((hex1 and 0xC0) shr 6) {
                    0 -> "P"
                    1 -> "C"
                    2 -> "B"
                    3 -> "U"
                    else -> "P"
                }
                val firstDigit = (hex1 and 0x30) shr 4
                val secondDigit = hex1 and 0x0F
                val baseCode = "$prefix$firstDigit$secondDigit$hex2"
                val fullCode = if (hexSub != "00") "$baseCode-$hexSub" else baseCode

                val severity = if ((statusHex and 0x08) != 0) "Ativo Confirmado" else "Intermitente"
                list.add(
                    DiagnosticTroubleCode(
                        code = fullCode,
                        description = lookupPsaDtcDescription(baseCode),
                        severity = severity,
                        ecuSource = ecuSource
                    )
                )
                i += 8
            }
        }
        return list
    }

    private fun lookupPsaDtcDescription(code: String): String {
        return when (code) {
            // Peugeot BSI (B-codes)
            "B1805" -> "BSI: Inibição do corte do motor Start & Stop (Habitualmente causado por estado de carga insuficiente da Bateria 12V)"
            "B1003" -> "BSI: Defeito de segurança ou telecodificação segura não inicializada"
            "B1624" -> "BSI: Relé de corte de alimentação do circuito de potência ou proteção"
            "B1808" -> "BSI: Reconhecimento do transponder da chave de ignição"
            "B1818" -> "BSI: Sinal do sensor de nível e qualidade do óleo do motor"
            "B1101" -> "BSI: Defeito no comando de iluminação exterior ou luz de mínimos"
            "B1153" -> "BSI: Anomalia no circuito de iluminação de presença ou médios"
            "B1328" -> "BSI: Anomalia no módulo de gestão de energia e proteção da bateria (BECB)"

            // Peugeot Engine (P1-codes / P-codes)
            "P1351" -> "Motor PSA: Circuito do relé de pré-aquecimento - Velas de incandescência comandadas mas não alimentadas"
            "P1434" -> "Motor PSA: Bomba de injeção de aditivo de combustível (Eolys/FAP) - Circuito aberto ou avaria"
            "P1435" -> "Motor PSA: Bomba de aditivação FAP - Erro de rede ou comunicação"
            "P1445" -> "Motor PSA: Aditivo no filtro de partículas - Quantidade máxima atingida no FAP"
            "P1693" -> "Motor PSA: Linha de arranque pilotado - Arranque do motor impossibilitado"
            "P20EE" -> "Motor PSA: Eficiência do catalisador SCR DeNOx/AdBlue abaixo do limite"
            "P2463" -> "Motor PSA: Sobrecarga de fuligem no Filtro de Partículas (FAP) - Regeneração necessária"
            "P0234" -> "Motor: Condição de Sobrepressão do Turbocompressor (Overboost)"
            "P0299" -> "Motor: Subpressão no Turbocompressor (Underboost)"

            // Network & Chassis (U-codes & C-codes)
            "U1218" -> "Rede CAN: Comunicação com a BSI - Recebidos dados inválidos"
            "U1118" -> "Rede CAN: Ausência temporária de comunicação com a BSI"
            "U1208" -> "Rede CAN: Comunicação com o calculador do motor - Dados inválidos"
            "C1145" -> "Chassis: Sensor de velocidade de roda ou travão de estacionamento elétrico"
            "C1555" -> "Chassis: Calibração da direção assistida elétrica"

            else -> lookupDtcDescription(code)
        }
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
