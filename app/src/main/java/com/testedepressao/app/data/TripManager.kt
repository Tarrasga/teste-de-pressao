package com.testedepressao.app.data

import android.content.Context
import android.content.SharedPreferences
import com.testedepressao.app.data.models.DrivingStyle
import com.testedepressao.app.data.models.LiveTripData
import com.testedepressao.app.data.models.TripRecord
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
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max

class TripManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("teste_de_pressao_trips", Context.MODE_PRIVATE)

    private val _liveTrip = MutableStateFlow(LiveTripData())
    val liveTrip: StateFlow<LiveTripData> = _liveTrip.asStateFlow()

    private val _tripHistory = MutableStateFlow<List<TripRecord>>(emptyList())
    val tripHistory: StateFlow<List<TripRecord>> = _tripHistory.asStateFlow()

    private var tripLoopJob: Job? = null
    private var currentTelemetryProvider: (() -> VehicleTelemetry)? = null
    private var rawEcoScoreFloat = 85.0f

    init {
        loadHistory()
    }

    fun bindTelemetryProvider(provider: () -> VehicleTelemetry) {
        currentTelemetryProvider = provider
    }

    fun startTrip() {
        if (_liveTrip.value.isRecording) return

        rawEcoScoreFloat = 85.0f
        _liveTrip.value = LiveTripData(
            isRecording = true,
            startTimeMs = System.currentTimeMillis(),
            durationSeconds = 0L,
            distanceKm = 0.0f,
            instantConsumptionL100km = 0.0f,
            instantConsumptionLh = 0.7f,
            isStopped = true,
            avgConsumptionL100km = 0.0f,
            fuelConsumedLiters = 0.0f,
            avgSpeedKmh = 0.0f,
            maxSpeedKmh = 0,
            maxBoostBar = 0.0f,
            maxRpm = 0,
            ecoScore = 85,
            currentStyle = DrivingStyle.DINAMICA
        )

        tripLoopJob?.cancel()
        tripLoopJob = scope.launch(Dispatchers.Default) {
            while (isActive && _liveTrip.value.isRecording) {
                delay(1000)
                updateTripTick()
            }
        }
    }

    fun stopTrip(): TripRecord? {
        if (!_liveTrip.value.isRecording) return null
        tripLoopJob?.cancel()
        tripLoopJob = null

        val live = _liveTrip.value
        _liveTrip.value = live.copy(isRecording = false)

        // Only save if the trip lasted more than 10 seconds or covered some distance
        if (live.durationSeconds < 10 && live.distanceKm < 0.05f) {
            return null
        }

        val record = TripRecord(
            id = UUID.randomUUID().toString(),
            timestamp = live.startTimeMs,
            formattedDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(live.startTimeMs)),
            durationFormatted = formatDuration(live.durationSeconds),
            distanceKm = live.distanceKm,
            avgConsumptionL100km = live.avgConsumptionL100km,
            fuelConsumedLiters = live.fuelConsumedLiters,
            avgSpeedKmh = live.avgSpeedKmh,
            maxSpeedKmh = live.maxSpeedKmh,
            maxBoostBar = live.maxBoostBar,
            drivingStyle = live.currentStyle,
            ecoScore = live.ecoScore
        )

        val updatedHistory = listOf(record) + _tripHistory.value
        _tripHistory.value = updatedHistory
        saveHistory(updatedHistory)
        return record
    }

    private fun updateTripTick() {
        val telem = currentTelemetryProvider?.invoke() ?: VehicleTelemetry()
        val current = _liveTrip.value

        val newDuration = current.durationSeconds + 1L
        val speedKmh = telem.speedKmh
        val isStopped = speedKmh < 3

        // Distance delta in 1 second: km/h / 3600
        val distDelta = (speedKmh / 3600.0f)
        val newDistance = current.distanceKm + distDelta

        // Fuel consumption calculation (Liters per second)
        val litersPerSec: Float
        val instantL100km: Float
        val instantLh: Float

        if (telem.mafGramsPerSec != null && telem.mafGramsPerSec > 0.5f) {
            // Using MAF: Fuel (g/s) = MAF / AFR (approx 15.0 for mixed gasoline/diesel)
            val fuelGramsSec = telem.mafGramsPerSec / 15.0f
            // Density approx 800 g/L
            litersPerSec = fuelGramsSec / 800.0f
        } else {
            // Fallback estimation using load, RPM, and boost
            val loadRatio = (telem.engineLoadPercent ?: 30).coerceIn(10, 100) / 100.0f
            val rpmRatio = telem.rpm.coerceIn(750, 6000) / 3000.0f
            val boostRatio = telem.boostPressureBar.coerceAtLeast(0.0f)

            val baseLitersPerHour = 0.7f + (loadRatio * 4.5f * rpmRatio) + (boostRatio * 6.0f)
            litersPerSec = baseLitersPerHour / 3600.0f
        }

        val fuelDelta = litersPerSec * 1.0f
        val newTotalFuel = current.fuelConsumedLiters + fuelDelta

        if (isStopped) {
            instantLh = (litersPerSec * 3600.0f).coerceIn(0.4f, 4.0f)
            instantL100km = 0.0f
        } else {
            instantLh = litersPerSec * 3600.0f
            // L/100km = (LitersPerSec * 3600 / Speed) * 100
            instantL100km = ((litersPerSec * 3600.0f / speedKmh) * 100.0f).coerceIn(1.5f, 35.0f)
        }

        val avgConsumption = if (newDistance > 0.05f) {
            ((newTotalFuel / newDistance) * 100.0f).coerceIn(2.0f, 30.0f)
        } else {
            if (isStopped) 0.0f else instantL100km
        }

        val avgSpeed = if (newDuration > 0) {
            (newDistance / (newDuration / 3600.0f))
        } else 0.0f

        val newMaxSpeed = max(current.maxSpeedKmh, speedKmh)
        val newMaxBoost = max(current.maxBoostBar, telem.boostPressureBar)
        val newMaxRpm = max(current.maxRpm, telem.rpm)

        // --- Driving Style Evaluation Algorithm ---
        val boost = telem.boostPressureBar
        val load = telem.engineLoadPercent ?: 30
        val rpm = telem.rpm

        when {
            // Aggressive Sport driving trigger
            boost > 0.9f || load > 70 || rpm > 3200 -> {
                rawEcoScoreFloat = (rawEcoScoreFloat - 0.45f).coerceAtLeast(15.0f)
            }
            // Moderate driving trigger
            boost > 0.4f || load > 45 || rpm > 2400 -> {
                rawEcoScoreFloat = if (rawEcoScoreFloat > 65f) {
                    (rawEcoScoreFloat - 0.15f).coerceAtLeast(40.0f)
                } else {
                    rawEcoScoreFloat
                }
            }
            // Smooth Economic cruising
            speedKmh > 25 && boost < 0.2f && load < 35 && rpm in 1200..2200 -> {
                rawEcoScoreFloat = (rawEcoScoreFloat + 0.15f).coerceAtMost(98.0f)
            }
        }

        val currentScore = rawEcoScoreFloat.toInt()
        val currentStyle = when {
            currentScore >= 75 -> DrivingStyle.ECONOMICA
            currentScore >= 50 -> DrivingStyle.DINAMICA
            else -> DrivingStyle.DESPORTIVA
        }

        _liveTrip.value = current.copy(
            durationSeconds = newDuration,
            distanceKm = newDistance,
            instantConsumptionL100km = instantL100km,
            instantConsumptionLh = instantLh,
            isStopped = isStopped,
            avgConsumptionL100km = avgConsumption,
            fuelConsumedLiters = newTotalFuel,
            avgSpeedKmh = avgSpeed,
            maxSpeedKmh = newMaxSpeed,
            maxBoostBar = newMaxBoost,
            maxRpm = newMaxRpm,
            ecoScore = currentScore,
            currentStyle = currentStyle
        )
    }

    fun deleteTrip(id: String) {
        val updated = _tripHistory.value.filterNot { it.id == id }
        _tripHistory.value = updated
        saveHistory(updated)
    }

    fun clearAllHistory() {
        _tripHistory.value = emptyList()
        prefs.edit().remove(KEY_TRIPS_JSON).apply()
    }

    private fun loadHistory() {
        try {
            val jsonStr = prefs.getString(KEY_TRIPS_JSON, null) ?: return
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<TripRecord>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val styleName = obj.optString("drivingStyle", DrivingStyle.DINAMICA.name)
                val style = try {
                    DrivingStyle.valueOf(styleName)
                } catch (_: Exception) {
                    DrivingStyle.DINAMICA
                }

                list.add(
                    TripRecord(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        timestamp = obj.optLong("timestamp", 0L),
                        formattedDate = obj.optString("formattedDate", "--"),
                        durationFormatted = obj.optString("durationFormatted", "00:00"),
                        distanceKm = obj.optDouble("distanceKm", 0.0).toFloat(),
                        avgConsumptionL100km = obj.optDouble("avgConsumptionL100km", 0.0).toFloat(),
                        fuelConsumedLiters = obj.optDouble("fuelConsumedLiters", 0.0).toFloat(),
                        avgSpeedKmh = obj.optDouble("avgSpeedKmh", 0.0).toFloat(),
                        maxSpeedKmh = obj.optInt("maxSpeedKmh", 0),
                        maxBoostBar = obj.optDouble("maxBoostBar", 0.0).toFloat(),
                        drivingStyle = style,
                        ecoScore = obj.optInt("ecoScore", 75)
                    )
                )
            }
            _tripHistory.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveHistory(list: List<TripRecord>) {
        try {
            val arr = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("timestamp", item.timestamp)
                    put("formattedDate", item.formattedDate)
                    put("durationFormatted", item.durationFormatted)
                    put("distanceKm", item.distanceKm.toDouble())
                    put("avgConsumptionL100km", item.avgConsumptionL100km.toDouble())
                    put("fuelConsumedLiters", item.fuelConsumedLiters.toDouble())
                    put("avgSpeedKmh", item.avgSpeedKmh.toDouble())
                    put("maxSpeedKmh", item.maxSpeedKmh)
                    put("maxBoostBar", item.maxBoostBar.toDouble())
                    put("drivingStyle", item.drivingStyle.name)
                    put("ecoScore", item.ecoScore)
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_TRIPS_JSON, arr.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%02d:%02d", m, s)
        }
    }

    companion object {
        private const val KEY_TRIPS_JSON = "key_trips_history_json"
    }
}
