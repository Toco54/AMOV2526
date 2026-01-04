package pt.isec.amov2526.projetoKotlin.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

// --- CORES ---
private val Slate400 = Color(0xFF94A3B8)

enum class RuleType(val title: String, val icon: ImageVector, val color: Color) {
    FALL("Fall", Icons.Default.MonitorHeart, Color(0xFFFF7043)),
    ACCIDENT("Accident", Icons.Default.CarCrash, Color(0xFFEF4444)),
    GEOFENCING("Geofencing", Icons.Default.LocationOn, Color(0xFF7E57C2)),
    SPEED("Speed Control", Icons.Default.Speed, Color(0xFF42A5F5)),
    INACTIVITY("Inactivity", Icons.Default.Schedule, Slate400)
}

data class MonitorRule(
    val id: String,
    val monitorId: String,
    val monitorName: String,
    val parameter: String,
    val isEnabled: Boolean,
    val type: RuleType,
    val scheduleDays: List<Int> = emptyList(), // 0=Sunday, 1=Monday...
    val scheduleStart: String = "", // "HH:mm"
    val scheduleEnd: String = "",   // "HH:mm"
    val isScheduleEnabled: Boolean = false,
    val pausedUntil: Long = 0L,     // Timestamp in millis
    val isDisabledByProtege: Boolean = false,
    val isEffectivelyActive: Boolean = true, // Added
    val geofenceData: Triple<Double, Double, Float>? = null // [NOVO] (Lat, Lng, Radius)
)

// [NOVO] Estrutura para representar uma infração detetada
data class RuleInfraction(
    val ruleId: String,
    val monitorId: String,
    val monitorName: String, // [ADDED]
    val ruleType: RuleType,
    val description: String,
    val valueDetected: Any? = null, // Ex: a velocidade atual (120.0) ou tempo inatividade
    val ruleParameter: String? = null // [NOVO] O parâmetro original (ex: "120", "10")
)

// [NOVO] Resultado de uma verificação de sensores
data class ProtectionCheckResult(
    val infractions: List<RuleInfraction> = emptyList(),
    val activeMonitorsToNotify: Set<String> = emptySet() // IDs dos monitores que devem receber alerta
)

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
    val isEffectivelyActive: Boolean = true // Added
)

// [NOVO] Classe auxiliar para passar infrações para a UI via Intent
data class InfractionDisplayInfo(
    val monitorName: String,
    val ruleType: String,
    val description: String,
    val parameter: String? = null,
    val monitorId: String? = null // [NOVO] Adicionado para targeting
)