package pt.isec.amov2526.projetoKotlin.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SensorHelper(private val context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    /**
     * Retorna um Flow que emite valores do Acelerómetro.
     * Útil para detetar quedas e acidentes (desaceleração brusca).
     */
    fun getAccelerometerData(): Flow<Triple<Float, Float, Float>> {
        return callbackFlow {
            val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    event?.let {
                        // Emite os valores X, Y, Z
                        trySend(Triple(it.values[0], it.values[1], it.values[2]))
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    // Não é necessário implementar para este caso
                }
            }

            if (sensor != null) {
                // SENSOR_DELAY_GAME ou UI é suficiente para deteção de quedas
                sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
                Log.d("SensorHelper", "Acelerómetro registado")
            } else {
                Log.e("SensorHelper", "Acelerómetro não disponível neste dispositivo")
                close() // Fecha o flow se não houver sensor
            }

            // Quando o Flow for cancelado (ex: ViewModel limpo), isto é executado
            awaitClose {
                sensorManager.unregisterListener(listener)
                Log.d("SensorHelper", "Acelerómetro parado")
            }
        }
    }

    /**
     * Exemplo para detetar inatividade (falta de movimento significativo).
     * Nota: A lógica de tempo deve ser gerida no ViewModel ou Serviço.
     * Este Flow emite 'true' se houver movimento significativo.
     */
    fun getSignificantMotion(): Flow<Boolean> {
        return callbackFlow {
            // Usa o acelerómetro linear para evitar a gravidade
            val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
                ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    event?.let {
                        val x = it.values[0]
                        val y = it.values[1]
                        val z = it.values[2]

                        // Calcula a magnitude da aceleração
                        val acceleration = kotlin.math.sqrt((x*x + y*y + z*z).toDouble())

                        // Se a aceleração for maior que um limiar (ex: 1.5), houve movimento
                        if (acceleration > 1.5) {
                            trySend(true)
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }
            }

            if (sensor != null) {
                sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            } else {
                close()
            }

            awaitClose {
                sensorManager.unregisterListener(listener)
            }
        }
    }
}