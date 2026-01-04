package pt.isec.amov2526.projetoKotlin.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.sqrt

class SensorHelper(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val prefs = context.getSharedPreferences("SafetySecPrefs", Context.MODE_PRIVATE)

    // --- StateFlows (The "Boxes" holding the latest data) ---
    
    // Holds (X, Y, Z) from the Accelerometer
    private val _accelerometerData = MutableStateFlow(Triple(0f, 0f, 0f))
    val accelerometerData = _accelerometerData.asStateFlow()

    // [MODIFICADO] Carrega o último timestamp guardado ou usa o tempo atual se não existir
    private val _lastMotionTimestamp = MutableStateFlow(
        prefs.getLong("LAST_MOTION_TIMESTAMP", System.currentTimeMillis())
    )
    val lastMotionTimestamp = _lastMotionTimestamp.asStateFlow()

    // --- Internal State for Logic ---
    private var isRawAccelerometerForMotion = false
    private var lastMotionMagnitude: Double? = null
    private val motionThreshold = 1.5
    
    // Controlo de tempo para evitar emissões excessivas (ms)
    private var lastEmissionTime = 0L
    private val EMISSION_COOLDOWN = 1000L // 1 segundo

    // --- Control Methods ---

    fun startAccelerometer() {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensor != null) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
            Log.d("SensorHelper", "Accelerometer Started")
        } else {
            Log.e("SensorHelper", "Accelerometer not available")
        }
    }

    fun stopAccelerometer() {
        // Note: This stops *all* listeners for this class instance. 
        // If we want to stop sensors individually, we need separate listener objects.
        // For simplicity in this project, we assume we stop everything or use separate start calls carefully.
        // To be safe, we will just unregister specific sensors if needed, 
        // but here unregisterListener(this) removes ALL sensors registered to 'this'.
        sensorManager.unregisterListener(this)
        Log.d("SensorHelper", "All Sensors Stopped")
    }

    fun startMotionDetection() {
        // Try Linear Acceleration first (Gravity removed by Android)
        var sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        isRawAccelerometerForMotion = false

        if (sensor == null) {
            // Fallback to Raw Accelerometer
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            isRawAccelerometerForMotion = true
        }

        if (sensor != null) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d("SensorHelper", "Motion Detection Started (Raw: $isRawAccelerometerForMotion)")
        } else {
            Log.e("SensorHelper", "No suitable sensor for motion detection")
        }
    }

    fun stopMotionDetection() {
        sensorManager.unregisterListener(this)
    }

    // --- Sensor Callbacks ---

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val currentTime = System.currentTimeMillis()

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                // 1. Atualização do fluxo de dados brutos
                _accelerometerData.value = Triple(event.values[0], event.values[1], event.values[2])

                // 2. Lógica de deteção de movimento (Fallback)
                if (isRawAccelerometerForMotion) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    val magnitude = sqrt((x * x + y * y + z * z).toDouble())

                    if (lastMotionMagnitude != null) {
                        val delta = abs(magnitude - lastMotionMagnitude!!)
                        if (delta > motionThreshold && (currentTime - lastEmissionTime > EMISSION_COOLDOWN)) {
                            // Atualiza o estado e persiste no disco
                            updateMotionTimestamp(currentTime)
                        }
                    }
                    lastMotionMagnitude = magnitude
                }
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt((x * x + y * y + z * z).toDouble())

                if (magnitude > motionThreshold && (currentTime - lastEmissionTime > EMISSION_COOLDOWN)) {
                    updateMotionTimestamp(currentTime)
                }
            }
        }
    }

    private fun updateMotionTimestamp(timestamp: Long) {
        _lastMotionTimestamp.value = timestamp
        lastEmissionTime = timestamp
        prefs.edit().putLong("LAST_MOTION_TIMESTAMP", timestamp).apply()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }
}
