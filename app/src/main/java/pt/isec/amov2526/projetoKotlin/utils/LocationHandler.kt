package pt.isec.amov2526.projetoKotlin.utils

import android.location.Location
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

data class GeofenceRule(
    val id: String,
    val centerLat: Double,
    val centerLng: Double,
    val radiusMeters: Float,
    val name: String = "Zone"
)

data class SpeedRule(
    val id: String,
    val maxSpeedKmh: Float,
    val name: String = "Speed Limit"
)

sealed class LocationAlert {
    data class GeofenceExit(val lastLocation: Location) : LocationAlert()
    data class SpeedLimitExceeded(val ruleId: String, val currentSpeedKmh: Float, val limitKmh: Float) : LocationAlert()
}

class LocationHandler(
    private val locationHelper: LocationHelper,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val geofences = mutableListOf<GeofenceRule>()
    private val speedRules = mutableListOf<SpeedRule>()

    private val _alerts = MutableSharedFlow<LocationAlert>()
    val alerts = _alerts.asSharedFlow()

    private var monitoringJob: Job? = null

    fun addGeofence(rule: GeofenceRule) {
        synchronized(geofences) { geofences.add(rule) }
        Log.d("LocationHandler", "Added Geofence: ${rule.name}")
    }

    fun removeGeofence(ruleId: String) {
        synchronized(geofences) { geofences.removeAll { it.id == ruleId } }
    }

    fun addSpeedRule(rule: SpeedRule) {
        synchronized(speedRules) { speedRules.add(rule) }
        Log.d("LocationHandler", "Added Speed Rule: ${rule.maxSpeedKmh} km/h")
    }

    fun removeSpeedRule(ruleId: String) {
        synchronized(speedRules) { speedRules.removeAll { it.id == ruleId } }
    }

    fun clearAllRules() {
        synchronized(geofences) { geofences.clear() }
        synchronized(speedRules) { speedRules.clear() }
    }

    fun startMonitoring() {
        if (monitoringJob?.isActive == true) return

        // Start updates (2 seconds interval)
        locationHelper.startLocationUpdates(2000L)

        monitoringJob = scope.launch {
            locationHelper.currentLocation.collect { location ->
                location?.let { checkRules(it) }
            }
        }
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        locationHelper.stopLocationUpdates()
    }

    private suspend fun checkRules(location: Location) {
        // 1. Check Geofences
        val currentGeofences = synchronized(geofences) { geofences.toList() }
        if (currentGeofences.isNotEmpty()) {
            var insideAny = false
            for (fence in currentGeofences) {
                val distance = FloatArray(1)
                Location.distanceBetween(
                    location.latitude, location.longitude,
                    fence.centerLat, fence.centerLng,
                    distance
                )
                if (distance[0] <= fence.radiusMeters) {
                    insideAny = true
                    break
                }
            }

            if (!insideAny) {
                Log.d("LocationHandler", "User is OUTSIDE all safe zones!")
                _alerts.emit(LocationAlert.GeofenceExit(location))
            }
        }

        // 2. Check Speed
        if (location.hasSpeed()) {
            val currentSpeedKmh = location.speed * 3.6f
            val currentSpeedRules = synchronized(speedRules) { speedRules.toList() }

            for (rule in currentSpeedRules) {
                if (currentSpeedKmh > rule.maxSpeedKmh) {
                    Log.d("LocationHandler", "Speed Limit Exceeded! Current: $currentSpeedKmh")
                    _alerts.emit(LocationAlert.SpeedLimitExceeded(rule.id, currentSpeedKmh, rule.maxSpeedKmh))
                }
            }
        }
    }
}