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

/**
 * Abstração de Hardware para sensores do dispositivo.
 *
 * Responsável por:
 * 1. Gerir o acesso ao `SensorManager` do Android.
 * 2. Fornecer fluxos reativos (`StateFlow`) para dados de Acelerómetro e Movimento.
 * 3. Normalizar dados de sensores diferentes (Acelerómetro vs Aceleração Linear) para detetar movimento.
 */
class SensorHelper(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val prefs = context.getSharedPreferences("SafetySecPrefs", Context.MODE_PRIVATE)

    // Fluxo de dados brutos (X, Y, Z) do acelerómetro.
    private val _accelerometerData = MutableStateFlow(Triple(0f, 0f, 0f))
    val accelerometerData = _accelerometerData.asStateFlow()

    // Timestamp da última deteção de movimento físico significativo.
    private val _lastMotionTimestamp = MutableStateFlow(
        prefs.getLong("LAST_MOTION_TIMESTAMP", System.currentTimeMillis())
    )
    val lastMotionTimestamp = _lastMotionTimestamp.asStateFlow()

    // Flag para indicar se estamos a usar Acelerómetro Bruto (com gravidade) ou Linear (sem gravidade).
    private var isRawAccelerometerForMotion = false
    private var lastMotionMagnitude: Double? = null
    
    // Limiar de variação de magnitude para considerar como "movimento".
    private val motionThreshold = 1.5

    // Rate limiter para atualizações de movimento.
    private var lastEmissionTime = 0L
    private val EMISSION_COOLDOWN = 1000L

    /**
     * Regista o listener para o Acelerómetro (TYPE_ACCELEROMETER).
     * Usado para deteção de quedas e acidentes.
     */
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
        // Nota: Unregister remove para todos os sensores deste listener.
        // Se usar ambos (motion e raw), isto para ambos.
        sensorManager.unregisterListener(this)
        Log.d("SensorHelper", "All Sensors Stopped")
    }

    /**
     * Inicia a deteção de movimento para reset de inatividade.
     * Tenta usar `TYPE_LINEAR_ACCELERATION` (mais preciso, sem gravidade).
     * Se indisponível, faz fallback para `TYPE_ACCELEROMETER` e aplica filtro de gravidade manual.
     */
    fun startMotionDetection() {
        // Tenta sensor de aceleração linear (hardware ou software).
        var sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        isRawAccelerometerForMotion = false

        if (sensor == null) {
            // Fallback para acelerómetro bruto.
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
                // Atualiza fluxo de dados brutos para o AccelerometerHandler (Quedas/Acidentes).
                _accelerometerData.value = Triple(event.values[0], event.values[1], event.values[2])

                // Se estivermos a usar este sensor também para deteção de movimento (Fallback):
                if (isRawAccelerometerForMotion) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    val magnitude = sqrt((x * x + y * y + z * z).toDouble())

                    if (lastMotionMagnitude != null) {
                        val delta = abs(magnitude - lastMotionMagnitude!!)
                        if (delta > motionThreshold && (currentTime - lastEmissionTime > EMISSION_COOLDOWN)) {
                            // Movimento detetado -> Atualiza timestamp.
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
        // Não utilizado neste contexto.
    }
}