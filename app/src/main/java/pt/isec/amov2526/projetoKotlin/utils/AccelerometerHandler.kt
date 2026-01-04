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

/**
 * Gestor responsável pela interpretação de dados brutos do acelerómetro para deteção de eventos de impacto.
 *
 * Analisa o fluxo contínuo de dados do sensor para identificar padrões compatíveis com:
 * 1. Quedas (FALL): Impactos súbitos de magnitude moderada a alta.
 * 2. Acidentes (ACCIDENT): Forças G extremas tipicamente associadas a colisões de veículos.
 *
 * Implementa lógica de "cooldown" para prevenir múltiplos alertas para o mesmo evento físico.
 *
 * @param sensorHelper Instância do helper que fornece o fluxo de dados brutos do sensor.
 * @param scope Escopo de corrotinas para execução assíncrona da monitorização.
 * @param isActiveChecker Função de callback para validar se uma regra específica está ativa no momento do evento.
 */
class AccelerometerHandler(
    private val sensorHelper: SensorHelper,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val isActiveChecker: (MonitorRule) -> Boolean
) {
    // Limiares empíricos para deteção de eventos.
    // NOTE: Estes valores são aproximações genéricas e podem gerar falsos positivos/negativos dependendo do hardware.
    private val FALL_G_THRESHOLD = 45f
    private val ACCIDENT_G_THRESHOLD = 90f
    
    // Período de espera entre eventos consecutivos para evitar spam de alertas.
    private val ALERT_COOLDOWN_MS = 5000L
    private var lastAlertTime = 0L

    // Armazenamento local das regras ativas, separadas por tipo para otimização da verificação.
    private val fallRules = mutableListOf<MonitorRule>()
    private val accidentRules = mutableListOf<MonitorRule>()

    // Fluxo partilhado para emissão de infrações detetadas para o serviço central.
    private val _infractions = MutableSharedFlow<ProtectionCheckResult>()
    val infractions = _infractions.asSharedFlow()

    private var monitoringJob: Job? = null

    /**
     * Adiciona ou atualiza uma regra de monitorização baseada em acelerómetro.
     * Separa as regras em listas específicas (Queda vs Acidente) para acesso rápido durante o evento.
     *
     * @param rule A regra a ser gerida.
     */
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

    /**
     * Remove todas as regras registadas.
     * Utilizado durante a reinicialização do serviço ou limpeza de estado.
     */
    fun clearAllRules() {
        synchronized(fallRules) { fallRules.clear() }
        synchronized(accidentRules) { accidentRules.clear() }
    }

    /**
     * Inicia a corrotina de monitorização.
     * Subscreve ao fluxo de dados do `SensorHelper` e aplica a lógica de limiares.
     */
    fun startMonitoring() {
        if (monitoringJob?.isActive == true) return
        sensorHelper.startAccelerometer()
        monitoringJob = scope.launch {
            sensorHelper.accelerometerData.collect { (x, y, z) ->
                // Cálculo da magnitude do vetor de aceleração (teorema de Pitágoras).
                val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

                // Logging de debug para calibração de sensores em desenvolvimento.
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
                // Verificação de cooldown temporal para evitar alertas repetidos para o mesmo impacto.
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

    /**
     * Processa um evento detetado (Queda ou Acidente).
     * Verifica quais as regras ativas que correspondem ao tipo de evento e prepara o payload de infração.
     *
     * @param type O tipo de evento detetado (FALL ou ACCIDENT).
     * @param value O valor da magnitude detetada.
     */
    private suspend fun handleEvent(type: RuleType, value: Float) {
        // Seleção das regras relevantes de forma thread-safe.
        val rawRules = if (type == RuleType.FALL) synchronized(fallRules) { fallRules.toList() }
        else synchronized(accidentRules) { accidentRules.toList() }

        Log.d("AccelerometerHandler", "Avaliar evento $type. Regras totais: ${rawRules.size}")

        // Filtragem final usando o callback externo para garantir que a regra não está pausada ou fora de horário.
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

        // Construção do conjunto de monitores a notificar e da lista de infrações detalhadas.
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

    /**
     * Para a monitorização e liberta os recursos do sensor.
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        sensorHelper.stopAccelerometer()
    }
}
