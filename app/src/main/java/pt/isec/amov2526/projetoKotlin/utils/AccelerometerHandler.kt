package pt.isec.amov2526.projetoKotlin.utils

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import pt.isec.amov2526.projetoKotlin.data.model.MonitorRule
import pt.isec.amov2526.projetoKotlin.data.model.ProtectionCheckResult
import pt.isec.amov2526.projetoKotlin.data.model.RuleInfraction
import pt.isec.amov2526.projetoKotlin.data.model.RuleType
import kotlin.math.sqrt

class AccelerometerHandler(
    private val sensorHelper: SensorHelper,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val isActiveChecker: (MonitorRule) -> Boolean
) {
    private val FALL_G_THRESHOLD = 45f
    private val ACCIDENT_G_THRESHOLD = 90f
    private val ALERT_COOLDOWN_MS = 5000L
    private var lastAlertTime = 0L

    private val fallRules = mutableListOf<MonitorRule>()
    private val accidentRules = mutableListOf<MonitorRule>()

    private val _infractions = MutableSharedFlow<ProtectionCheckResult>()
    val infractions = _infractions.asSharedFlow()

    private var monitoringJob: Job? = null

    fun addRule(rule: MonitorRule) {
        when (rule.type) {
            RuleType.FALL -> synchronized(fallRules) {
                fallRules.removeAll { it.id == rule.id }; fallRules.add(
                rule
            )
            }

            RuleType.ACCIDENT -> synchronized(accidentRules) {
                accidentRules.removeAll { it.id == rule.id }; accidentRules.add(
                rule
            )
            }

            else -> {}
        }
    }

    fun clearAllRules() {
        synchronized(fallRules) { fallRules.clear() }
        synchronized(accidentRules) { accidentRules.clear() }
    }

    fun startMonitoring() {
        if (monitoringJob?.isActive == true) return
        sensorHelper.startAccelerometer()
        monitoringJob = scope.launch {
            sensorHelper.accelerometerData.collect { (x, y, z) ->
                val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()


                if (magnitude > 15f) {
                    Log.d(
                        "AccelerometerHandler",
                        "Magnitude detetada: ${
                            String.format(
                                "%.2f",
                                magnitude
                            )
                        } m/s² (~${String.format("%.2f", magnitude / 9.81f)}G)"
                    )
                }

                val currentTime = System.currentTimeMillis()
                if (currentTime - lastAlertTime >= ALERT_COOLDOWN_MS) {
                    if (magnitude > ACCIDENT_G_THRESHOLD) {
                        Log.i(
                            "AccelerometerHandler",
                            "!!! LIMIAR DE ACIDENTE EXCEDIDO: $magnitude !!!"
                        )
                        handleEvent(RuleType.ACCIDENT, magnitude)
                        lastAlertTime = currentTime
                    } else if (magnitude > FALL_G_THRESHOLD) {
                        Log.i(
                            "AccelerometerHandler",
                            "!!! LIMIAR DE QUEDA EXCEDIDO: $magnitude !!!"
                        )
                        handleEvent(RuleType.FALL, magnitude)
                        lastAlertTime = currentTime
                    }
                }
            }
        }
    }

    private suspend fun handleEvent(type: RuleType, value: Float) {
        val rawRules = if (type == RuleType.FALL) synchronized(fallRules) { fallRules.toList() }
        else synchronized(accidentRules) { accidentRules.toList() }

        Log.d("AccelerometerHandler", "Avaliar evento $type. Regras totais: ${rawRules.size}")


        val activeRules = rawRules.filter { rule ->
            val active = isActiveChecker(rule)
            Log.v(
                "AccelerometerHandler",
                "Regra ${rule.id} (Monitor: ${rule.monitorName}) ativa: $active"
            )
            active
        }

        if (activeRules.isEmpty()) {
            Log.w("AccelerometerHandler", "Evento descartado: Nenhuma regra ativa para $type")
            return
        }

        val monitorsToNotify = activeRules.map { it.monitorId }.toSet()
        val infractionsList = activeRules.map { rule ->
            RuleInfraction(
                ruleId = rule.id,
                monitorId = rule.monitorId,
                monitorName = rule.monitorName,
                ruleType = type,
                description = if (type == RuleType.FALL) "Fall Detected (${value.toInt()} G)" else "Accident Detected (${value.toInt()} G)",
                valueDetected = value,
                ruleParameter = rule.parameter
            )
        }

        _infractions.emit(ProtectionCheckResult(infractionsList, monitorsToNotify))
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        sensorHelper.stopAccelerometer()
    }
}