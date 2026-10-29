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

    fun setKeepScreenOn(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, enabled).apply()
        _keepScreenOn.value = enabled
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

        val DEFAULT_SENSORS = setOf(
            "turbo",
            "rpm",
            "speed",
            "coolant",
            "oil_temp",
            "voltage",
            "engine_load",
            "intake_temp"
        )
    }
}
