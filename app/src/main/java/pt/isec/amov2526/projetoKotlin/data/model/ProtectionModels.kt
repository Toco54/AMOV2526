package pt.isec.amov2526.projetoKotlin.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CarCrash
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import pt.isec.amov2526.projetoKotlin.R
import pt.isec.amov2526.projetoKotlin.ui.theme.RuleAccident
import pt.isec.amov2526.projetoKotlin.ui.theme.RuleFall
import pt.isec.amov2526.projetoKotlin.ui.theme.RuleGeofence
import pt.isec.amov2526.projetoKotlin.ui.theme.RuleSpeed
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate400

/**
 * Definição central dos tipos de regras com mapeamento para recursos visuais (Ícone, Cor).
 */
enum class RuleType(val titleRes: Int, val icon: ImageVector, val color: Color) {
    FALL(R.string.rule_fall, Icons.Default.MonitorHeart, RuleFall),
    ACCIDENT(R.string.rule_accident, Icons.Default.CarCrash, RuleAccident),
    GEOFENCING(R.string.rule_geofencing, Icons.Default.LocationOn, RuleGeofence),
    SPEED(R.string.rule_speed, Icons.Default.Speed, RuleSpeed),
    INACTIVITY(R.string.rule_inactivity, Icons.Default.Schedule, Slate400)
}

/**
 * Modelo interno de regra para processamento lógico nos ViewModels e Serviços.
 *
 * Diferente de [ProtectionRule], este modelo é otimizado para avaliação de estado
 * (`isEffectivelyActive`) e não contém metadados puramente visuais.
 *
 * @property isEffectivelyActive Resultado calculado da hierarquia de estados (Global && Grupo && Regra).
 * @property geofenceData Tuplo (Lat, Lng, Raio) pré-parseado para otimização de verificações de localização.
 */
data class MonitorRule(
    val id: String,
    val monitorId: String,
    val monitorName: String,
    val parameter: String,
    val isEnabled: Boolean,
    val type: RuleType,
    val scheduleDays: List<Int> = emptyList(),
    val scheduleStart: String = "",
    val scheduleEnd: String = "",
    val isScheduleEnabled: Boolean = false,
    val pausedUntil: Long = 0L,
    val isDisabledByProtege: Boolean = false,
    val isEffectivelyActive: Boolean = true,
    val geofenceData: Triple<Double, Double, Float>? = null
)

/**
 * Representa uma violação detetada de uma regra específica.
 */
data class RuleInfraction(
    val ruleId: String,
    val monitorId: String,
    val monitorName: String,
    val ruleType: RuleType,
    val description: String,
    val valueDetected: Any? = null,
    val ruleParameter: String? = null
)

/**
 * Payload de resultado da verificação de regras pelos Handlers.
 * Agrega todas as infrações de um ciclo de verificação e o conjunto de monitores a notificar.
 */
data class ProtectionCheckResult(
    val infractions: List<RuleInfraction> = emptyList(),
    val activeMonitorsToNotify: Set<String> = emptySet()
)

/**
 * Agrupamento lógico de regras para a UI do Guard.
 * Permite controlo em massa (ex: Pausar todas as regras de "Queda").
 */
data class ProtectionGroup(
    val type: RuleType,
    val rules: List<MonitorRule>,
    val isExpanded: Boolean = false,
    val isEnabled: Boolean = true,
    val pausedUntil: Long = 0L,
    val scheduleDays: List<Int> = emptyList(),
    val scheduleStart: String = "",
    val scheduleEnd: String = "",
    val isScheduleEnabled: Boolean = false,
    val isEffectivelyActive: Boolean = true
)

/**
 * Modelo simplificado para exibir detalhes de infração na UI de Alerta.
 */
data class InfractionDisplayInfo(
    val monitorName: String,
    val ruleType: String,
    val description: String,
    val parameter: String? = null,
    val monitorId: String? = null
)
