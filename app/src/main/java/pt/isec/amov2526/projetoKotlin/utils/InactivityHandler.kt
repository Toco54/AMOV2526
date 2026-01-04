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

/**
 * Gestor de lógica para deteção de inatividade do utilizador.
 *
 * Monitoriza a ausência de movimento físico significativo durante períodos prolongados.
 * Funciona através de um sistema de "Watchdog" que é reiniciado sempre que o sensor de movimento
 * deteta atividade. Se o temporizador exceder o limite definido por uma regra, um alerta é gerado.
 *
 * @param sensorHelper Instância para acesso aos dados do sensor de movimento.
 * @param scope Escopo de corrotinas para execução das tarefas de verificação.
 * @param isActiveChecker Callback para validação de estado das regras.
 */
class InactivityHandler(
    private val sensorHelper: SensorHelper,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val isActiveChecker: (MonitorRule) -> Boolean
) {
    private val rules = mutableListOf<MonitorRule>()
    
    // Conjunto para evitar alertas repetidos para a mesma regra na mesma sessão de inatividade.
    private val triggeredRulesThisSession = mutableSetOf<String>()

    private val _infractions = MutableSharedFlow<ProtectionCheckResult>()
    val infractions = _infractions.asSharedFlow()

    // Timestamp da última deteção de movimento validada.
    private var lastMotionTimestamp: Long = sensorHelper.lastMotionTimestamp.value
    
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

    /**
     * Inicia o ciclo de monitorização.
     * 1. Subscreve ao sensor para reiniciar o timer em caso de movimento.
     * 2. Inicia um loop periódico (5s) para verificar se algum limite de tempo foi excedido.
     */
    fun startMonitoring() {
        if (monitoringJob?.isActive == true) return
        sensorHelper.startMotionDetection()

        // Job de reação ao sensor: Reinicia o contador de inatividade.
        sensorJob = scope.launch {
            sensorHelper.lastMotionTimestamp.collect { timestamp ->
                // Filtra atualizações redundantes.
                if (timestamp > lastMotionTimestamp) {
                    Log.d("InactivityHandler", "Movimento detetado em: $timestamp")
                    resetTimer(timestamp)
                }
            }
        }

        // Job de verificação periódica: Avalia regras contra o tempo decorrido.
        monitoringJob = scope.launch {
            while (true) {
                checkRules(); delay(5000L)
            }
        }
    }

    fun stopMonitoring() {
        sensorJob?.cancel()
        monitoringJob?.cancel()
        sensorHelper.stopMotionDetection()
    }

    /**
     * Reinicia o estado de inatividade.
     * Chamado quando há movimento físico ou quando o serviço é retomado.
     * Limpa o registo de regras já disparadas para permitir novos alertas.
     */
    fun resetTimer(timestamp: Long = System.currentTimeMillis()) {
        Log.d("InactivityHandler", "Temporizador reiniciado. Referência: $timestamp")
        lastMotionTimestamp = timestamp
        if (triggeredRulesThisSession.isNotEmpty()) {
            synchronized(triggeredRulesThisSession) { triggeredRulesThisSession.clear() }
        }
        if (lastMinuteLogged != 0L) lastMinuteLogged = 0
    }

    /**
     * Compara o tempo de inatividade atual com os limites definidos nas regras ativas.
     * Gera infrações se o tempo limite for excedido e a regra ainda não tiver disparado nesta sessão.
     */
    private suspend fun checkRules() {
        val now = System.currentTimeMillis()
        val inactiveDurationMillis = now - lastMotionTimestamp
        val minutesInactive = inactiveDurationMillis / 60000

        // Log de progresso para debug (apenas quando muda o minuto).
        if (minutesInactive > lastMinuteLogged) {
            lastMinuteLogged = minutesInactive
            Log.d("InactivityHandler", "User inactive for $minutesInactive minutes...")
        }

        val currentRules = synchronized(rules) { rules.toList() }
        val newInfractions = mutableListOf<RuleInfraction>()
        val monitorsToNotify = mutableSetOf<String>()

        for (rule in currentRules) {
            // Verifica se a regra está ativa no sistema global (horário, pausa, etc.).
            if (!isActiveChecker(rule)) continue

            // Anti-Spam: Se já alertou para esta regra desde o último movimento, ignora.
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
                // Marca a regra como disparada para evitar alertas a cada 5 segundos.
                synchronized(triggeredRulesThisSession) { triggeredRulesThisSession.add(rule.id) }
            }
        }

        if (newInfractions.isNotEmpty()) {
            _infractions.emit(ProtectionCheckResult(newInfractions, monitorsToNotify))
        }
    }
}
