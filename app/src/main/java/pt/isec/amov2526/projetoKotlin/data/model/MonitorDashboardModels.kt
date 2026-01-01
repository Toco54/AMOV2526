package pt.isec.amov2526.projetoKotlin.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class MonitorRuleType(val title: String, val isMultiInstance: Boolean) {
    FALL("Fall", false),
    ACCIDENT("Accident", false),
    GEOFENCING("Geofencing", true),
    SPEED("Speed Control", true),
    INACTIVITY("Prolonged Inactivity", true)
}

data class ProtectionRule(
    val id: String,
    val type: MonitorRuleType,
    val icon: ImageVector,
    val iconColor: Color,
    var isEnabled: Boolean,
    var param1: String = "",
    var param2: String = "",
    // New fields
    val scheduleDays: List<Int> = emptyList(),
    val scheduleStart: String = "",
    val scheduleEnd: String = "",
    val isScheduleEnabled: Boolean = false,
    val pausedUntil: Long = 0L,
    val isDisabledByProtege: Boolean = false,
    val deletedByProtege: Boolean = false
)

data class AlertStat(val label: String, val count: Int)
