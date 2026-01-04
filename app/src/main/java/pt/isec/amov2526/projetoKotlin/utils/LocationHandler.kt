package pt.isec.amov2526.projetoKotlin.utils

import android.location.Location
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

/**
 * Gestor de lógica de monitorização baseada em localização (GPS).
 *
 * Responsável por processar atualizações de localização e avaliar dois tipos de regras:
 * 1. Velocidade (SPEED): Verifica se a velocidade atual excede o limite definido.
 * 2. Geofencing (GEOFENCING): Verifica se o utilizador saiu de um raio seguro definido.
 *
 * Implementa lógica de "Anti-Spam" para evitar alertas repetidos para a mesma violação de Geofence.
 */
class LocationHandler(
    private val locationHelper: LocationHelper,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val isActiveChecker: (MonitorRule) -> Boolean
) {
    private val speedRules = mutableListOf<MonitorRule>()
    private val geofenceRules = mutableListOf<MonitorRule>()

    // Canal de eventos para comunicar infrações ao Serviço principal.
    private val _infractions = MutableSharedFlow<ProtectionCheckResult>()
    val infractions = _infractions.asSharedFlow()

    // Estado para evitar spam de alertas de Geofence.
    // Armazena os IDs dos monitores para os quais já foi enviado um alerta de "Fora da Zona".
    // Só é removido quando o utilizador reentra na zona segura desse monitor.
    private val triggeredMonitors = mutableSetOf<String>()

    private var lastSpeedCheckTime = 0L
    private var monitoringJob: Job? = null

    /**
     * Limpa o estado de alertas disparados.
     * Útil quando o serviço é reiniciado ou o estado de proteção é alterado manualmente.
     */
    fun resetAlertStates() {
        synchronized(triggeredMonitors) {
            triggeredMonitors.clear()
        }
        Log.d("LocationHandler", "Alert states reset (Anti-Spam cleared).")
    }

    fun addRule(rule: MonitorRule) {
        when (rule.type) {
            RuleType.SPEED -> {
                synchronized(speedRules) {
                    speedRules.removeAll { it.id == rule.id }
                    speedRules.add(rule)
                }
            }

            RuleType.GEOFENCING -> {
                synchronized(geofenceRules) {
                    geofenceRules.removeAll { it.id == rule.id }
                    geofenceRules.add(rule)
                }
            }

            else -> {}
        }
    }

    fun clearAllRules() {
        synchronized(speedRules) { speedRules.clear() }
        synchronized(geofenceRules) { geofenceRules.clear() }
    }

    fun startMonitoring() {
        if (monitoringJob?.isActive == true) return
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

    /**
     * Avalia a localização atual contra todas as regras ativas.
     *
     * Fluxo de execução:
     * 1. Verificação de Velocidade (Throttle de 30s).
     * 2. Verificação de Geofencing (Por Monitor).
     *    - Agrupa regras por monitor (um monitor pode ter várias zonas seguras).
     *    - Se o utilizador estiver dentro de *qualquer* zona segura de um monitor, é considerado seguro para esse monitor.
     *    - Se estiver fora de *todas* as zonas desse monitor, dispara alerta.
     *
     * @param location A nova localização recebida do hardware.
     */
    private suspend fun checkRules(location: Location) {
        val detectedInfractions = mutableListOf<RuleInfraction>()
        val monitorsToNotify = mutableSetOf<String>()

        // --- Lógica de Velocidade ---
        if (location.hasSpeed()) {
            val now = System.currentTimeMillis()
            // Throttle: Verifica velocidade apenas a cada 30 segundos para evitar falsos positivos momentâneos de GPS drift.
            if (now - lastSpeedCheckTime >= 30000L) {
                lastSpeedCheckTime = now
                val currentSpeedKmh = location.speed * 3.6f

                val rules = synchronized(speedRules) { speedRules.toList() }
                val rulesByMonitor = rules.groupBy { it.monitorId }

                rulesByMonitor.forEach { (monitorId, monitorRules) ->
                    val brokenRules = monitorRules.filter { rule ->
                        val active = isActiveChecker(rule)
                        val limit = rule.parameter.toFloatOrNull() ?: Float.MAX_VALUE

                        Log.d("LocationHandler", "Rule Check -> Speed: $currentSpeedKmh | Limit: $limit | Active: $active | Monitor: ${rule.monitorName}")

                        active && currentSpeedKmh > limit
                    }

                    if (brokenRules.isNotEmpty()) {
                        // Seleciona a regra com o limite mais alto que foi quebrado (ou qualquer heurística relevante).
                        val highestLimitRule = brokenRules.maxByOrNull {
                            it.parameter.toFloatOrNull() ?: 0f
                        }
                        if (highestLimitRule != null) {
                            highestLimitRule.parameter.toFloat()
                            monitorsToNotify.add(monitorId)
                            detectedInfractions.add(
                                RuleInfraction(
                                    ruleId = highestLimitRule.id,
                                    monitorId = monitorId,
                                    monitorName = highestLimitRule.monitorName,
                                    ruleType = RuleType.SPEED,
                                    description = "${currentSpeedKmh.toInt()} km/h",
                                    valueDetected = currentSpeedKmh,
                                    ruleParameter = highestLimitRule.parameter
                                )
                            )
                        }
                    }
                }
            }
        }

        // --- Lógica de Geofencing ---
        val geoRules = synchronized(geofenceRules) { geofenceRules.toList() }

        if (geoRules.isNotEmpty()) {
            Log.d("LocationHandler", "--- CHECKING GEOFENCES (Total Rules: ${geoRules.size}) ---")
            Log.d("LocationHandler", "Current Loc: ${location.latitude}, ${location.longitude} (Acc: ${location.accuracy}m)")
        }

        // Agrupa por monitor. A lógica é: O utilizador deve estar dentro de PELO MENOS UMA zona segura definida pelo monitor.
        val geoRulesByMonitor = geoRules.groupBy { it.monitorId }

        geoRulesByMonitor.forEach { (monitorId, rules) ->
            var isSafeInThisMonitor = false
            var minDistanceToAnyZone = Float.MAX_VALUE
            var referenceRule: MonitorRule? = null // Regra de referência para o alerta (normalmente a mais próxima)

            Log.d("LocationHandler", "  > Analyzing Monitor: ${rules.firstOrNull()?.monitorName ?: monitorId}")

            for (rule in rules) {
                val isActive = isActiveChecker(rule)

                val geoData = rule.geofenceData
                if (geoData == null) {
                    Log.d("LocationHandler", "    - Rule ${rule.id}: NO GEO DATA")
                    continue
                }

                val (lat, lng, radius) = geoData

                val results = FloatArray(1)
                Location.distanceBetween(location.latitude, location.longitude, lat, lng, results)
                val distanceMeters = results[0]

                Log.d("LocationHandler", "    - Rule (${rule.parameter}): Active=$isActive | Dist=$distanceMeters / Rad=$radius | Inside=${distanceMeters <= radius}")

                if (!isActive) continue

                if (distanceMeters <= radius) {
                    // Safe! Dentro de uma zona.
                    isSafeInThisMonitor = true
                }

                // Mantém registo da distância mínima para reportar "X metros fora da zona"
                if (distanceMeters - radius < minDistanceToAnyZone) {
                    minDistanceToAnyZone = distanceMeters - radius
                    referenceRule = rule
                }
            }

            Log.d("LocationHandler", "  => Result for Monitor: isSafe=$isSafeInThisMonitor")

            if (isSafeInThisMonitor) {
                // Se estava em alerta e agora está seguro, limpa o estado de trigger.
                if (triggeredMonitors.contains(monitorId)) {
                    Log.d("LocationHandler", "     [INFO] Monitor $monitorId: User re-entered safe zone. Resetting alert trigger.")
                    synchronized(triggeredMonitors) { triggeredMonitors.remove(monitorId) }
                }
            } else if (referenceRule != null) {
                // Fora de todas as zonas deste monitor.
                
                // Anti-Spam: Verifica se já alertámos sobre esta saída.
                if (triggeredMonitors.contains(monitorId)) {
                    Log.d("LocationHandler", "     [INFO] Monitor $monitorId: Already alerted. Suppressing spam.")
                } else {
                    Log.d("LocationHandler", "     [!] INFRACTION TRIGGERED (First Time)")

                    synchronized(triggeredMonitors) { triggeredMonitors.add(monitorId) }

                    monitorsToNotify.add(monitorId)
                    detectedInfractions.add(
                        RuleInfraction(
                            ruleId = referenceRule.id,
                            monitorId = monitorId,
                            monitorName = referenceRule.monitorName,
                            ruleType = RuleType.GEOFENCING,
                            description = "Outside Safe Zones (${minDistanceToAnyZone.toInt()}m away)",
                            valueDetected = minDistanceToAnyZone,
                            ruleParameter = referenceRule.parameter
                        )
                    )
                }
            }
        }

        if (detectedInfractions.isNotEmpty()) {
            _infractions.emit(ProtectionCheckResult(detectedInfractions, monitorsToNotify))
        }
    }
}
