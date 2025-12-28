package pt.isec.amov2526.projetoKotlin.utils

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

data class InactivityRule(
    val id: String,
    val maxDurationMillis: Long,
    val name: String = "Inactivity Rule"
)

class InactivityHandler(
    private val sensorHelper: SensorHelper,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val rules = mutableListOf<InactivityRule>()
    private val triggeredRules = mutableSetOf<String>()

    private val _inactivityAlerts = MutableSharedFlow<InactivityRule>()
    val inactivityAlerts = _inactivityAlerts.asSharedFlow()

    private var lastMotionTimestamp: Long = System.currentTimeMillis()
    private var monitoringJob: Job? = null
    private var sensorJob: Job? = null

    fun addRule(rule: InactivityRule) {
        synchronized(rules) { rules.add(rule) }
        Log.d("InactivityHandler", "Added rule: ${rule.name}")
    }

    fun removeRule(ruleId: String) {
        synchronized(rules) {
            rules.removeAll { it.id == ruleId }
            triggeredRules.remove(ruleId)
        }
    }

    fun startMonitoring() {
        if (monitoringJob?.isActive == true) return

        sensorHelper.startMotionDetection()

        // 1. Listen for Motion to reset the timer
        sensorJob = scope.launch {
            sensorHelper.significantMotion.collect { hasMotion ->
                if (hasMotion) {
                    resetTimer()
                }
            }
        }

        // 2. Periodic check loop (every 5 seconds)
        monitoringJob = scope.launch {
            while (true) {
                checkRules()
                delay(5000L) 
            }
        }
    }

    fun stopMonitoring() {
        sensorJob?.cancel()
        monitoringJob?.cancel()
        sensorHelper.stopMotionDetection()
    }

    private fun resetTimer() {
        lastMotionTimestamp = System.currentTimeMillis()
        if (triggeredRules.isNotEmpty()) {
            Log.d("InactivityHandler", "Motion detected! Resetting all timers.")
            synchronized(triggeredRules) {
                triggeredRules.clear()
            }
        }
    }

    private suspend fun checkRules() {
        val now = System.currentTimeMillis()
        val inactiveDuration = now - lastMotionTimestamp
        
        val currentRules = synchronized(rules) { rules.toList() }

        for (rule in currentRules) {
            if (inactiveDuration > rule.maxDurationMillis) {
                if (!triggeredRules.contains(rule.id)) {
                    Log.d("InactivityHandler", "ALERT: Inactivity Rule '${rule.name}' triggered!")
                    _inactivityAlerts.emit(rule)
                    synchronized(triggeredRules) { triggeredRules.add(rule.id) }
                }
            }
        }
    }
}