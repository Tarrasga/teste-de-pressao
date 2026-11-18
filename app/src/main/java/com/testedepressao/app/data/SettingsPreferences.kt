package com.testedepressao.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("teste_de_pressao_prefs", Context.MODE_PRIVATE)

    private val _keepScreenOn = MutableStateFlow(prefs.getBoolean(KEY_KEEP_SCREEN_ON, true))
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    private val _enabledSensors = MutableStateFlow(
        prefs.getStringSet(KEY_ENABLED_SENSORS, DEFAULT_SENSORS) ?: DEFAULT_SENSORS
    )
    val enabledSensors: StateFlow<Set<String>> = _enabledSensors.asStateFlow()

    private val _gaugeScale = MutableStateFlow(prefs.getFloat(KEY_GAUGE_SCALE, 1.0f))
    val gaugeScale: StateFlow<Float> = _gaugeScale.asStateFlow()

    private val _autoConnectObd = MutableStateFlow(prefs.getBoolean(KEY_AUTO_CONNECT_OBD, true))
    val autoConnectObd: StateFlow<Boolean> = _autoConnectObd.asStateFlow()

    private val _lastObdMac = MutableStateFlow(prefs.getString(KEY_LAST_OBD_MAC, "") ?: "")
    val lastObdMac: StateFlow<String> = _lastObdMac.asStateFlow()

    private val _lastObdName = MutableStateFlow(prefs.getString(KEY_LAST_OBD_NAME, "") ?: "")
    val lastObdName: StateFlow<String> = _lastObdName.asStateFlow()

    fun setKeepScreenOn(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, enabled).apply()
        _keepScreenOn.value = enabled
    }

    fun setGaugeScale(scale: Float) {
        prefs.edit().putFloat(KEY_GAUGE_SCALE, scale).apply()
        _gaugeScale.value = scale
    }

    fun setAutoConnectObd(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CONNECT_OBD, enabled).apply()
        _autoConnectObd.value = enabled
    }

    fun setLastObdDevice(mac: String, name: String?) {
        prefs.edit()
            .putString(KEY_LAST_OBD_MAC, mac)
            .putString(KEY_LAST_OBD_NAME, name ?: "")
            .apply()
        _lastObdMac.value = mac
        _lastObdName.value = name ?: ""
    }

    fun setSensorEnabled(sensorId: String, isEnabled: Boolean) {
        val current = _enabledSensors.value.toMutableSet()
        if (isEnabled) {
            current.add(sensorId)
        } else {
            current.remove(sensorId)
        }
        prefs.edit().putStringSet(KEY_ENABLED_SENSORS, current).apply()
        _enabledSensors.value = current
    }

    companion object {
        private const val KEY_KEEP_SCREEN_ON = "pref_keep_screen_on"
        private const val KEY_ENABLED_SENSORS = "pref_enabled_sensors"
        private const val KEY_GAUGE_SCALE = "pref_gauge_scale"
        private const val KEY_AUTO_CONNECT_OBD = "pref_auto_connect_obd"
        private const val KEY_LAST_OBD_MAC = "pref_last_obd_mac"
        private const val KEY_LAST_OBD_NAME = "pref_last_obd_name"

        val DEFAULT_SENSORS = setOf(
            "turbo",
            "rpm",
            "speed",
            "coolant",
            "oil_temp",
            "voltage",
            "engine_load",
            "intake_temp",
            "runtime",
            "odometer"
        )
    }
}
