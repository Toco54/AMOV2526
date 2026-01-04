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


    

    private val _accelerometerData = MutableStateFlow(Triple(0f, 0f, 0f))
    val accelerometerData = _accelerometerData.asStateFlow()


    private val _lastMotionTimestamp = MutableStateFlow(
        prefs.getLong("LAST_MOTION_TIMESTAMP", System.currentTimeMillis())
    )
    val lastMotionTimestamp = _lastMotionTimestamp.asStateFlow()


    private var isRawAccelerometerForMotion = false
    private var lastMotionMagnitude: Double? = null
    private val motionThreshold = 1.5
    

    private var lastEmissionTime = 0L
    private val EMISSION_COOLDOWN = 1000L



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



        
        
        sensorManager.unregisterListener(this)
        Log.d("SensorHelper", "All Sensors Stopped")
    }

    fun startMotionDetection() {
        
        var sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        isRawAccelerometerForMotion = false

        if (sensor == null) {
            
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

    

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val currentTime = System.currentTimeMillis()

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                
                _accelerometerData.value = Triple(event.values[0], event.values[1], event.values[2])

                
                if (isRawAccelerometerForMotion) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    val magnitude = sqrt((x * x + y * y + z * z).toDouble())

                    if (lastMotionMagnitude != null) {
                        val delta = abs(magnitude - lastMotionMagnitude!!)
                        if (delta > motionThreshold && (currentTime - lastEmissionTime > EMISSION_COOLDOWN)) {
                            
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
        
    }
}
