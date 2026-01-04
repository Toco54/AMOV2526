package pt.isec.amov2526.projetoKotlin.data.model

/**
 * Tipos de alertas suportados pelo sistema.
 *
 * Mapeia diretamente os eventos físicos ou lógicos que podem desencadear uma notificação.
 */
enum class ActivityAlertType {
    SPEED,      // Excesso de velocidade
    FALL,       // Queda súbita (Acelerómetro)
    ACCIDENT,   // Colisão veicular (Força G extrema)
    GEOFENCE,   // Saída de zona segura
    PANIC,      // Botão SOS manual
    BATTERY,    // Bateria crítica (<15%)
    INACTIVITY  // Ausência de movimento prolongada
}

enum class ActivityAlertStatus {
    CONFIRMED,
    CANCELLED,
    PENDING
}

enum class MonitorActionState { SENT, VIEWED }

/**
 * Representa a ação/estado de um monitor em relação a um alerta.
 * Ex: "O Monitor João recebeu o alerta" ou "O Monitor João visualizou o detalhe".
 */
data class MonitorAction(
    val name: String,
    val state: MonitorActionState,
    val time: String,
    val uid: String? = null
)

/**
 * Modelo de domínio para um item de Atividade/Alerta na UI.
 * Combina dados do alerta (Firestore) com metadados de UI.
 */
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