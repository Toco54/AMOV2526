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
    // Thresholds (Mantidos como pediste)
    private val FALL_G_THRESHOLD = 25f
    private val ACCIDENT_G_THRESHOLD = 60f

    // [NOVO] Tempo de espera entre alertas (5 segundos)
    private val ALERT_COOLDOWN_MS = 5000L
    private var lastAlertTime = 0L

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

                // [NOVO] Verifica se já passaram 5 segundos desde o último alerta
                val currentTime = System.currentTimeMillis()

                if (currentTime - lastAlertTime >= ALERT_COOLDOWN_MS) {

                    // 1. Detect Potential Accident
                    if (magnitude > ACCIDENT_G_THRESHOLD) {
                        Log.d("AccelerometerHandler", "High Impact Accident Detected: $magnitude")
                        lastAlertTime = currentTime // Atualiza o tempo do último alerta
                        _onAccidentDetected.emit(Unit)
                    }
                    // 2. Detect Potential Fall
                    else if (magnitude > FALL_G_THRESHOLD) {
                        Log.d("AccelerometerHandler", "Fall Impact Detected: $magnitude")
                        lastAlertTime = currentTime // Atualiza o tempo do último alerta
                        _onFallDetected.emit(Unit)
                    }
                }
            }
        }
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        sensorHelper.stopAccelerometer()
    }
}