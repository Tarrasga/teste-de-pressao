package com.testedepressao.app

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.testedepressao.app.data.CarBluetoothManager
import com.testedepressao.app.data.ObdManager
import com.testedepressao.app.data.SettingsPreferences
import com.testedepressao.app.data.UpdateManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val bluetoothManager = CarBluetoothManager(application)
    val settingsPreferences = SettingsPreferences(application)
    val updateManager = UpdateManager(application)
    val obdManager = ObdManager(bluetoothManager, settingsPreferences, viewModelScope)

    val telemetry = obdManager.telemetry
    val allSensors = obdManager.allSensors
    val accelerationState = obdManager.accelerationState
    val dtcCodes = obdManager.dtcCodes
    val isScanningDtc = obdManager.isScanningDtc
    val psaConsoleLog = obdManager.psaConsoleLog
    val isDemoMode = obdManager.isDemoMode

    val connectionStatus = bluetoothManager.connectionStatus
    val connectedDeviceName = bluetoothManager.connectedDeviceName

    val keepScreenOn = settingsPreferences.keepScreenOn
    val updateInfo = updateManager.updateInfo
    val isCheckingUpdate = updateManager.isChecking
    val isDownloadingUpdate = updateManager.isDownloading
    val downloadProgress = updateManager.downloadProgress
    val updateStatusMessage = updateManager.statusMessage
    val currentVersionName = updateManager.getCurrentVersionName()

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

    fun armAccelerationTest() {
        obdManager.armAccelerationTest()
    }

    fun scanDtc() {
        viewModelScope.launch {
            obdManager.readDtcCodes()
        }
    }

    fun clearDtc() {
        viewModelScope.launch {
            obdManager.clearDtcCodes()
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        settingsPreferences.setKeepScreenOn(enabled)
    }

    fun toggleSensor(sensorId: String, isEnabled: Boolean) {
        settingsPreferences.setSensorEnabled(sensorId, isEnabled)
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

    fun sendRawCommand(command: String) {
        viewModelScope.launch {
            obdManager.sendCustomElmCommand(command)
        }
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothManager.disconnect()
        obdManager.stopPolling()
    }
}
