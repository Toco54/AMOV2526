package pt.isec.amov2526.projetoKotlin.utils

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * AccelerometerHandler processes raw data from SensorHelper to detect 
 * specific events like falls and accidents.
 */
class AccelerometerHandler(
    private val sensorHelper: SensorHelper,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    // Thresholds
    private val FALL_G_THRESHOLD = 25f      
    private val ACCIDENT_G_THRESHOLD = 60f  

    // Events
    private val _onFallDetected = MutableSharedFlow<Unit>()
    val onFallDetected = _onFallDetected.asSharedFlow()

    private val _onAccidentDetected = MutableSharedFlow<Unit>()
    val onAccidentDetected = _onAccidentDetected.asSharedFlow()

    private var monitoringJob: Job? = null

    fun startMonitoring() {
        if (monitoringJob?.isActive == true) return

        // Explicitly start the sensor
        sensorHelper.startAccelerometer()

        monitoringJob = scope.launch {
            sensorHelper.accelerometerData.collect { (x, y, z) ->
                val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                
                // 1. Detect Potential Accident
                if (magnitude > ACCIDENT_G_THRESHOLD) {
                    Log.d("AccelerometerHandler", "High Impact Accident Detected: $magnitude")
                    _onAccidentDetected.emit(Unit)
                } 
                // 2. Detect Potential Fall
                else if (magnitude > FALL_G_THRESHOLD) {
                    Log.d("AccelerometerHandler", "Fall Impact Detected: $magnitude")
                    _onFallDetected.emit(Unit)
                }
            }
        }
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        sensorHelper.stopAccelerometer()
    }
}