package pt.isec.amov2526.projetoKotlin.utils

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import pt.isec.amov2526.projetoKotlin.data.model.MonitorRule
import pt.isec.amov2526.projetoKotlin.data.model.ProtectionCheckResult
import pt.isec.amov2526.projetoKotlin.data.model.RuleInfraction
import pt.isec.amov2526.projetoKotlin.data.model.RuleType

class InactivityHandler(
    private val sensorHelper: SensorHelper,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val isActiveChecker: (MonitorRule) -> Boolean // [NOVO]
) {
    private val rules = mutableListOf<MonitorRule>()
    private val triggeredRulesThisSession = mutableSetOf<String>()

    private val _infractions = MutableSharedFlow<ProtectionCheckResult>()
    val infractions = _infractions.asSharedFlow()

    private var lastMotionTimestamp: Long = System.currentTimeMillis()
    private var monitoringJob: Job? = null
    private var sensorJob: Job? = null
    private var lastMinuteLogged: Long = 0

    fun addRule(rule: MonitorRule) {
        if (rule.type != RuleType.INACTIVITY) return
        synchronized(rules) { rules.removeAll { it.id == rule.id }; rules.add(rule) }
    }

    fun clearAllRules() {
        synchronized(rules) { rules.clear(); triggeredRulesThisSession.clear() }
    }

    fun startMonitoring() {
        if (monitoringJob?.isActive == true) return
        sensorHelper.startMotionDetection()
        sensorJob = scope.launch {
            sensorHelper.significantMotion.collect { hasMotion -> if (hasMotion) resetTimer() }
        }
        monitoringJob = scope.launch {
            while (true) { checkRules(); delay(5000L) }
        }
    }

    fun stopMonitoring() {
        sensorJob?.cancel()
        monitoringJob?.cancel()
        sensorHelper.stopMotionDetection()
    }

    private fun resetTimer() {
        lastMotionTimestamp = System.currentTimeMillis()
        if (triggeredRulesThisSession.isNotEmpty()) {
            synchronized(triggeredRulesThisSession) { triggeredRulesThisSession.clear() }
        }
        if (lastMinuteLogged != 0L) lastMinuteLogged = 0
    }

    private suspend fun checkRules() {
        val now = System.currentTimeMillis()
        val inactiveDurationMillis = now - lastMotionTimestamp
        val minutesInactive = inactiveDurationMillis / 60000

        if (minutesInactive > lastMinuteLogged) {
            lastMinuteLogged = minutesInactive
            Log.d("InactivityHandler", "User inactive for $minutesInactive minutes...")
        }

        val currentRules = synchronized(rules) { rules.toList() }
        val newInfractions = mutableListOf<RuleInfraction>()
        val monitorsToNotify = mutableSetOf<String>()

        for (rule in currentRules) {
            // [ATUALIZADO] Verificação Dinâmica (AND: Global && Group && Rule)
            if (!isActiveChecker(rule)) continue
            
            if (triggeredRulesThisSession.contains(rule.id)) continue

            val ruleMinutes = rule.parameter.toLongOrNull() ?: continue
            val ruleMillis = ruleMinutes * 60 * 1000

            if (inactiveDurationMillis >= ruleMillis) {
                monitorsToNotify.add(rule.monitorId)
                newInfractions.add(
                    RuleInfraction(
                        ruleId = rule.id,
                        monitorId = rule.monitorId,
                        monitorName = rule.monitorName,
                        ruleType = RuleType.INACTIVITY,
                        description = "Inactive for > $minutesInactive min",
                        valueDetected = minutesInactive,
                        ruleParameter = rule.parameter
                    )
                )
                synchronized(triggeredRulesThisSession) { triggeredRulesThisSession.add(rule.id) }
            }
        }

        if (newInfractions.isNotEmpty()) {
            _infractions.emit(ProtectionCheckResult(newInfractions, monitorsToNotify))
        }
    }
}