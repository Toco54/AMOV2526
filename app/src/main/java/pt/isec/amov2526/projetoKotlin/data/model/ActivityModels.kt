package pt.isec.amov2526.projetoKotlin.data.model

enum class ActivityAlertType {
    SPEED, FALL, ACCIDENT, GEOFENCE, PANIC, BATTERY
}

enum class ActivityAlertStatus {
    CONFIRMED, CANCELLED, PENDING
}

enum class MonitorActionState { SENT, VIEWED, CALLED_USER, CALLED_112, ON_THE_WAY, MARKED_SAFE }

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
    val notifiedMonitors: List<MonitorAction> = emptyList()
)
