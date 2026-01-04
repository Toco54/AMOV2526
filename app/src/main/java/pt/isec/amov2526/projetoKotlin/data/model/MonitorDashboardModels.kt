package pt.isec.amov2526.projetoKotlin.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import pt.isec.amov2526.projetoKotlin.R

/**
 * Tipos de Regras de Monitorização disponíveis para configuração.
 *
 * @property isMultiInstance Define se podem existir múltiplas regras deste tipo simultaneamente para o mesmo monitor
 *                         (ex: Várias Geofences vs Apenas uma regra de Queda).
 */
enum class MonitorRuleType(val titleRes: Int, val isMultiInstance: Boolean) {
    FALL(R.string.rule_fall, false),
    ACCIDENT(R.string.rule_accident, false),
    GEOFENCING(R.string.rule_geofencing, true),
    SPEED(R.string.rule_speed, true),
    INACTIVITY(R.string.rule_inactivity, true)
}

/**
 * Modelo completo de uma regra de proteção para exibição e edição no Dashboard do Monitor.
 *
 * Agrega estado lógico, configuração de agendamento e metadados de UI.
 *
 * @property pausedUntil Timestamp (epoch) até ao qual a regra está suspensa temporariamente.
 * @property isDisabledByProtege Se a regra foi desativada manualmente pelo utilizador protegido.
 * @property deletedByProtege Se a regra foi removida (Soft Delete) pelo utilizador protegido.
 */
data class ProtectionRule(
    val id: String,
    val type: MonitorRuleType,
    val icon: ImageVector,
    val iconColor: Color,
    var isEnabled: Boolean,
    var param1: String = "",
    var param2: String = "",

    val scheduleDays: List<Int> = emptyList(),
    val scheduleStart: String = "",
    val scheduleEnd: String = "",
    val isScheduleEnabled: Boolean = false,
    val pausedUntil: Long = 0L,
    val isDisabledByProtege: Boolean = false,
    val deletedByProtege: Boolean = false
)

data class AlertStat(val label: String, val count: Int)