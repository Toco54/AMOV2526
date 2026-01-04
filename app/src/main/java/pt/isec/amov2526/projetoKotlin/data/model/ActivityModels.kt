package pt.isec.amov2526.projetoKotlin.data.model

enum class ActivityAlertType {
    SPEED, FALL, ACCIDENT, GEOFENCE, PANIC, BATTERY, INACTIVITY
}

enum class ActivityAlertStatus {
    CONFIRMED, CANCELLED, PENDING
}

enum class MonitorActionState { SENT, VIEWED }

data class MonitorAction(
    val name: String,
    val state: MonitorActionState,
    val time: String
)

data class ActivityItem(
    val id: String,
    val type: ActivityAlertType,
    val title: String,
    val date: String,
    val time: String,
    val status: ActivityAlertStatus,
    val notifiedMonitors: List<MonitorAction> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val videoUrl: String? = null
)
