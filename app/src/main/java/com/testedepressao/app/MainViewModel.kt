package com.testedepressao.app

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.testedepressao.app.data.CarBluetoothManager
import com.testedepressao.app.data.ObdManager
import com.testedepressao.app.data.SettingsPreferences
import com.testedepressao.app.data.TripManager
import com.testedepressao.app.data.UpdateManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val bluetoothManager = CarBluetoothManager(application)
    val settingsPreferences = SettingsPreferences(application)
    val updateManager = UpdateManager(application)
    val obdManager = ObdManager(bluetoothManager, settingsPreferences, viewModelScope)
    val tripManager = TripManager(application, viewModelScope)

    val telemetry = obdManager.telemetry
    val allSensors = obdManager.allSensors
    val accelerationState = obdManager.accelerationState
    val dtcCodes = obdManager.dtcCodes
    val isScanningDtc = obdManager.isScanningDtc
    val isScanningPsaDtc = obdManager.isScanningPsaDtc
    val batteryHealth = obdManager.batteryHealth
    val psaConsoleLog = obdManager.psaConsoleLog
    val isDemoMode = obdManager.isDemoMode

    val liveTrip = tripManager.liveTrip
    val tripHistory = tripManager.tripHistory

    val connectionStatus = bluetoothManager.connectionStatus
    val connectedDeviceName = bluetoothManager.connectedDeviceName

    val keepScreenOn = settingsPreferences.keepScreenOn
    val gaugeScale = settingsPreferences.gaugeScale
    val updateInfo = updateManager.updateInfo
    val isCheckingUpdate = updateManager.isChecking
    val isDownloadingUpdate = updateManager.isDownloading
    val downloadProgress = updateManager.downloadProgress
    val updateStatusMessage = updateManager.statusMessage
    val currentVersionName = updateManager.getCurrentVersionName()

    init {
        tripManager.bindTelemetryProvider { obdManager.telemetry.value }

        // Automatically check for updates on startup
        viewModelScope.launch {
            updateManager.checkForUpdates()
        }

        // Auto-manage trip recording based on OBD connection
        viewModelScope.launch {
            bluetoothManager.connectionStatus.collect { status ->
                if (status == com.testedepressao.app.data.models.ConnectionStatus.CONNECTED) {
                    tripManager.startTrip()
                } else if (status == com.testedepressao.app.data.models.ConnectionStatus.DISCONNECTED ||
                    status == com.testedepressao.app.data.models.ConnectionStatus.ERROR
                ) {
                    tripManager.stopTrip()
                }
            }
        }

        // Auto-start trip recording in demo mode
        viewModelScope.launch {
            obdManager.isDemoMode.collect { demo ->
                if (demo) {
                    tripManager.startTrip()
                }
            }
        }

        // Auto-start trip recording when engine is detected running
        viewModelScope.launch {
            obdManager.telemetry.collect { telem ->
                if ((telem.rpm > 400 || telem.speedKmh > 0) && !tripManager.liveTrip.value.isRecording) {
                    tripManager.startTrip()
                }
            }
        }
    }

    fun getBondedDevices(): List<BluetoothDevice> {
        return bluetoothManager.getBondedDevices()
    }

    fun connectDevice(device: BluetoothDevice) {
        viewModelScope.launch {
            bluetoothManager.connect(device)
        }
    }

    fun disconnect() {
        bluetoothManager.disconnect()
    }

    fun toggleDemoMode(enabled: Boolean) {
        obdManager.toggleDemoMode(enabled)
    }

    fun resetPeakBoost() {
        obdManager.resetPeakBoost()
    }

    fun resetPeakSpeed() {
        obdManager.resetPeakSpeed()
    }

    fun armAccelerationTest() {
        obdManager.armAccelerationTest()
    }

    fun scanDtc() {
        viewModelScope.launch {
            obdManager.readDtcCodes()
        }
    }

    fun scanAdvancedPsaDtc() {
        viewModelScope.launch {
            obdManager.readAdvancedPsaDtcCodes()
        }
    }

    fun resetCrankTest() {
        obdManager.resetCrankTest()
    }

    fun clearDtc() {
        viewModelScope.launch {
            obdManager.clearDtcCodes()
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        settingsPreferences.setKeepScreenOn(enabled)
    }

    fun setGaugeScale(scale: Float) {
        settingsPreferences.setGaugeScale(scale)
    }

    fun cycleGaugeScale() {
        val current = gaugeScale.value
        val next = when {
            current >= 0.95f -> 0.70f  // 100% -> 70%
            current < 0.78f -> 0.85f   // 70% -> 85%
            else -> 1.0f               // 85% -> 100%
        }
        setGaugeScale(next)
    }

    fun toggleSensor(sensorId: String, isEnabled: Boolean) {
        settingsPreferences.setSensorEnabled(sensorId, isEnabled)
    }

    fun startTrip() {
        tripManager.startTrip()
    }

    fun stopTrip() {
        tripManager.stopTrip()
    }

    fun deleteTrip(id: String) {
        tripManager.deleteTrip(id)
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            updateManager.checkForUpdates()
        }
    }

    fun downloadAndInstallUpdate(apkUrl: String) {
        viewModelScope.launch {
            updateManager.downloadAndInstall(apkUrl)
        }
    }

    fun testPsaSport() {
        viewModelScope.launch {
            obdManager.testPeugeotSportMode()
        }
    }

    fun testPsaCopper() {
        viewModelScope.launch {
            obdManager.testPeugeotThemeCopper()
        }
    }

    fun restoreStandardCan() {
        viewModelScope.launch {
            obdManager.restoreStandardCanHeader()
        }
    }

    fun sendRawCommand(command: String) {
        viewModelScope.launch {
            obdManager.sendCustomElmCommand(command)
        }
    }

    override fun onCleared() {
        super.onCleared()
        tripManager.stopTrip()
        bluetoothManager.disconnect()
        obdManager.stopPolling()
    }
}
