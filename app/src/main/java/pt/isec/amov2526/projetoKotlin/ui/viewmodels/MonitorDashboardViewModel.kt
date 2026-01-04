package pt.isec.amov2526.projetoKotlin.ui.viewmodels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CarCrash
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.isec.amov2526.projetoKotlin.data.repository.RuleRepository
import pt.isec.amov2526.projetoKotlin.data.model.ProtectionRule
import pt.isec.amov2526.projetoKotlin.data.model.MonitorRuleType
import pt.isec.amov2526.projetoKotlin.utils.TimeUtils
import com.google.firebase.firestore.Query
import pt.isec.amov2526.projetoKotlin.data.model.ActivityItem
import pt.isec.amov2526.projetoKotlin.data.model.ActivityAlertStatus
import pt.isec.amov2526.projetoKotlin.data.model.ActivityAlertType
import pt.isec.amov2526.projetoKotlin.data.model.MonitorAction
import pt.isec.amov2526.projetoKotlin.data.model.MonitorActionState
import pt.isec.amov2526.projetoKotlin.data.model.AlertStat

data class GlobalScheduleInfo(
    val isEnabled: Boolean,
    val days: List<Int>,
    val start: String,
    val end: String
)

data class CategoryStatus(
    val isEnabled: Boolean,
    val pausedUntil: Long,
    val isScheduleEnabled: Boolean,
    val days: List<Int>,
    val start: String,
    val end: String
)

class MonitorDashboardViewModel(
    private val ruleRepository: RuleRepository,
    private val protegeUid: String // Changed from email to UID
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val currentMonitorId = auth.currentUser?.uid ?: ""

    private val _activities = MutableStateFlow<List<ActivityItem>>(emptyList())
    val activities: StateFlow<List<ActivityItem>> = _activities.asStateFlow()

    // Maps para gerir listeners individuais
    private val alertListeners = mutableMapOf<String, com.google.firebase.firestore.ListenerRegistration>()
    private val alertDataMap = mutableMapOf<String, ActivityItem>()



    private val _alertStats = MutableStateFlow<List<AlertStat>>(emptyList())
    val alertStats: StateFlow<List<AlertStat>> = _alertStats.asStateFlow()

    private val _rules = MutableStateFlow<List<ProtectionRule>>(emptyList())
    val rules: StateFlow<List<ProtectionRule>> = _rules.asStateFlow()

    private val _userLocation = MutableStateFlow<org.osmdroid.util.GeoPoint?>(null)
    val userLocation: StateFlow<org.osmdroid.util.GeoPoint?> = _userLocation.asStateFlow()

    private val _systemStatus = MutableStateFlow("OFFLINE")
    val systemStatus: StateFlow<String> = _systemStatus.asStateFlow()

    private val _statusDetail = MutableStateFlow<String?>(null)
    val statusDetail: StateFlow<String?> = _statusDetail.asStateFlow()

    private val _pausedUntil = MutableStateFlow(0L)
    val pausedUntil: StateFlow<Long> = _pausedUntil.asStateFlow()
    
    private val _globalSchedule = MutableStateFlow<GlobalScheduleInfo?>(null)
    val globalSchedule: StateFlow<GlobalScheduleInfo?> = _globalSchedule.asStateFlow()

    private val _categoryStatuses = MutableStateFlow<Map<String, CategoryStatus>>(emptyMap())
    val categoryStatuses: StateFlow<Map<String, CategoryStatus>> = _categoryStatuses.asStateFlow()

    private val _isDisconnected = MutableStateFlow(false)
    val isDisconnected: StateFlow<Boolean> = _isDisconnected.asStateFlow()

    private val _shouldExitDashboard = MutableStateFlow(false)
    val shouldExitDashboard: StateFlow<Boolean> = _shouldExitDashboard.asStateFlow()

    private val _protegeEmail = MutableStateFlow("")
    val protegeEmail: StateFlow<String> = _protegeEmail.asStateFlow()

    init {
        if (currentMonitorId.isNotEmpty() && protegeUid.isNotEmpty()) {
            startListeningRules()
            startListeningUserStatus()
            startListeningConnectionStatus()
        }
    }

        private fun startListeningConnectionStatus() {

            // CORREÇÃO: O alertaRefs está na coleção connections do MONITOR, apontando para o protegido.

            val docRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()

                .collection("users").document(currentMonitorId)

                .collection("connections").document(protegeUid)

    

            android.util.Log.d("MonitorVM", "Escutando connection em: users/$currentMonitorId/connections/$protegeUid")

    

            docRef.addSnapshotListener { snapshot, e ->

                if (e != null) {

                    android.util.Log.e("MonitorVM", "Erro ao escutar connection: ${e.message}")

                    return@addSnapshotListener

                }

                

                if (snapshot == null || !snapshot.exists()) {

                    android.util.Log.w("MonitorVM", "Documento de conexão não encontrado (possível revogação).")

                    _shouldExitDashboard.value = true

                    return@addSnapshotListener

                }

    

                // Obtém o mapa de referências (ID -> Caminho) de forma segura

                val rawMap = snapshot.get("alertRefs") as? Map<String, Any>

                val refsMap = rawMap?.mapValues { it.value.toString() } ?: emptyMap()

                

                android.util.Log.d("MonitorVM", "Recebido alertRefs: ${refsMap.size} itens")

                updateAlertListeners(refsMap)

            }

        }

                        

                            private fun updateAlertListeners(refsMap: Map<String, String>) {

                                val currentIds = alertListeners.keys.toSet()

                                val newIds = refsMap.keys

                        

                                // 1. Remover listeners de alertas que já não estão no mapa

                                val idsToRemove = currentIds - newIds

                                idsToRemove.forEach { id ->

                                    android.util.Log.d("MonitorVM", "Removendo listener do alerta: $id")

                                    alertListeners[id]?.remove()

                                    alertListeners.remove(id)

                                    alertDataMap.remove(id)

                                }

                        

                                // 2. Adicionar listeners para novos alertas

                                val idsToAdd = newIds - currentIds

                                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

                        

                                idsToAdd.forEach { id ->

                                    val path = refsMap[id]

                                    android.util.Log.d("MonitorVM", "Adicionando listener para novo alerta: $id no caminho: $path")

                                    if (!path.isNullOrEmpty()) {

                                        try {

                                            val docRef = db.document(path)

                                            val registration = docRef.addSnapshotListener { snapshot, e ->

                                                if (e != null) {

                                                    android.util.Log.e("MonitorVM", "Erro no listener do alerta $id: ${e.message}")

                                                    return@addSnapshotListener

                                                }

                        

                                                if (snapshot != null && snapshot.exists()) {

                                                    val item = parseActivityItem(snapshot)

                                                    if (item != null) {

                                                        android.util.Log.d("MonitorVM", "Alerta atualizado: ${item.title} ($id)")

                                                        alertDataMap[id] = item

                                                    }

                                                } else {

                                                    android.util.Log.w("MonitorVM", "Documento de alerta não existe: $id")

                                                    alertDataMap.remove(id)

                                                }

                                                emitActivities()

                                            }

                                            alertListeners[id] = registration

                                        } catch (e: Exception) {

                                            android.util.Log.e("MonitorVM", "Caminho de alerta inválido: $path")

                                            e.printStackTrace()

                                        }

                                    }

                                }

                        

                                // Se houve apenas remoções ou se não há nada para adicionar, forçar atualização da UI

                                if (idsToRemove.isNotEmpty() || (idsToAdd.isEmpty() && alertDataMap.isNotEmpty())) {

                                    emitActivities()

                                }

                            }

                        

                            private fun parseActivityItem(doc: com.google.firebase.firestore.DocumentSnapshot): ActivityItem? {

                                return try {

                                    val typeStr = doc.getString("type") ?: return null

                                    val statusStr = doc.getString("status") ?: "PENDING"

                        

                                    val monitorsList = mutableListOf<MonitorAction>()

                                    val monitorsRaw = doc.get("notifiedMonitors") as? List<Map<String, Any>>

                                    monitorsRaw?.forEach { m ->

                                        monitorsList.add(MonitorAction(

                                            name = m["name"] as? String ?: "Unknown",

                                            state = MonitorActionState.valueOf(m["state"] as? String ?: "SENT"),

                                            time = m["time"] as? String ?: ""

                                        ))

                                    }

                        

                                    ActivityItem(

                                        id = doc.id,

                                        type = ActivityAlertType.valueOf(typeStr),

                                        title = doc.getString("title") ?: "Alert",

                                        date = doc.getString("date") ?: "",

                                        time = doc.getString("time") ?: "",

                                        status = ActivityAlertStatus.valueOf(statusStr),

                                        notifiedMonitors = monitorsList,
                                        
                                        // Aparentemente o ViewModel original não lia lat/lng aqui,
                                        // mas para manter consistência com o modelo atualizado:
                                        latitude = doc.getDouble("latitude"),
                                        longitude = doc.getDouble("longitude"),
                                        videoUrl = doc.getString("videoUrl")

                                    )

                                } catch (ex: Exception) {

                                    android.util.Log.e("MonitorVM", "Erro ao processar ActivityItem: ${ex.message}")

                                    null

                                }

                            }

                        

                            private fun emitActivities() {

                                val sortedList = alertDataMap.values.sortedByDescending { "${it.date} ${it.time}" }

                                android.util.Log.d("MonitorVM", "Emitindo ${sortedList.size} atividades para a UI")

                                _activities.value = sortedList

                                calculateStats(sortedList)

                            }

    private fun calculateStats(items: List<ActivityItem>) {
        // Tenta contar por tipo. Se o tipo INACTIVITY não existir no Enum,
        // ele vai cair em 'else' ou podes ajustar conforme o teu modelo.
        val stats = listOf(
            AlertStat("Fall", items.count { it.type == ActivityAlertType.FALL }),
            AlertStat("Crash", items.count { it.type == ActivityAlertType.ACCIDENT }),
            AlertStat("Geo", items.count { it.type == ActivityAlertType.GEOFENCE }),
            AlertStat("Speed", items.count { it.type == ActivityAlertType.SPEED }),
            // Se não tiveres INACTIVITY no Enum ActivityAlertType, usa FALL ou remove esta linha
            AlertStat("SOS", items.count { it.type == ActivityAlertType.PANIC })
        )
        _alertStats.value = stats
    }
    private fun startListeningUserStatus() {
        val docRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(protegeUid)
            
        docRef.addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            
            if (snapshot != null && snapshot.exists()) {
                // Email
                _protegeEmail.value = snapshot.getString("email") ?: ""

                // Location
                val lat = snapshot.getDouble("lastLat")
                val lng = snapshot.getDouble("lastLng")
                if (lat != null && lng != null) {
                    _userLocation.value = org.osmdroid.util.GeoPoint(lat, lng)
                }

                // Read Active Protections (Category Status)
                val activeProtectionsMap = snapshot.get("activeProtections") as? Map<String, Map<String, Any>> ?: emptyMap()
                val parsedStatuses = activeProtectionsMap.mapValues { (_, value) ->
                    val enabled = value["enabled"] as? Boolean ?: true
                    val paused = (value["pausedUntil"] as? Number)?.toLong() ?: 0L
                    val schedEnabled = value["isScheduleEnabled"] as? Boolean ?: false
                    val schedDays = (value["scheduleDays"] as? List<Number>)?.map { it.toInt() } ?: emptyList()
                    val schedStart = value["scheduleStart"] as? String ?: ""
                    val schedEnd = value["scheduleEnd"] as? String ?: ""
                    CategoryStatus(enabled, paused, schedEnabled, schedDays, schedStart, schedEnd)
                }
                _categoryStatuses.value = parsedStatuses

                // Status & Connectivity
                val dbStatus = snapshot.getString("systemStatus") ?: "OFFLINE"
                val lastUpdate = snapshot.getLong("lastUpdate") ?: 0L
                val now = System.currentTimeMillis()
                
                // Read Schedule & Pause Data
                val globalPausedUntil = snapshot.getLong("globalPausedUntil") ?: 0L
                val gDays = (snapshot.get("globalScheduleDays") as? List<Number>)?.map { it.toInt() } ?: listOf(1, 2, 3, 4, 5)
                val gStart = snapshot.getString("globalScheduleStart") ?: "09:00"
                val gEnd = snapshot.getString("globalScheduleEnd") ?: "18:00"
                val gEnabled = snapshot.getBoolean("isGlobalScheduleEnabled") ?: false
                
                _globalSchedule.value = GlobalScheduleInfo(gEnabled, gDays, gStart, gEnd)

                // Calculate Effective Status if not OFFLINE
                var finalStatus = dbStatus
                var detail: String? = null
                
                if (dbStatus != "OFFLINE") {
                    val calculated = TimeUtils.calculateStatus(
                        isEnabled = (dbStatus == "ACTIVE" || dbStatus == "PAUSED"), // Assuming system is technically 'enabled' if not offline
                        pausedUntil = globalPausedUntil,
                        isScheduleEnabled = gEnabled,
                        days = gDays,
                        start = gStart,
                        end = gEnd,
                        now = now
                    )
                    finalStatus = calculated.label
                    detail = calculated.subLabel
                }
                
                _systemStatus.value = finalStatus
                _statusDetail.value = detail
                _pausedUntil.value = globalPausedUntil
                
                // If status is not OFFLINE but last update was > 2 mins ago, consider DISCONNECTED
                val isActuallyDisconnected = dbStatus != "OFFLINE" && (now - lastUpdate > 120000L)
                _isDisconnected.value = isActuallyDisconnected
            }
        }
    }

    private fun startListeningRules() {
        viewModelScope.launch {
            ruleRepository.getRulesFlow(currentMonitorId, protegeUid).collect { rawRules ->
                val uiRules = rawRules.map { rule ->
                    rule.copy(
                        icon = when (rule.type) {
                            MonitorRuleType.FALL -> Icons.Default.MonitorHeart
                            MonitorRuleType.ACCIDENT -> Icons.Default.CarCrash
                            MonitorRuleType.GEOFENCING -> Icons.Default.LocationOn
                            MonitorRuleType.SPEED -> Icons.Default.Speed
                            MonitorRuleType.INACTIVITY -> Icons.Default.Schedule
                            else -> Icons.Default.Shield
                        },
                        iconColor = when (rule.type) {
                            MonitorRuleType.FALL -> Color(0xFFFF7043)
                            MonitorRuleType.ACCIDENT -> Color(0xFFEF4444)
                            MonitorRuleType.GEOFENCING -> Color(0xFF7E57C2)
                            MonitorRuleType.SPEED -> Color(0xFF42A5F5)
                            MonitorRuleType.INACTIVITY -> Color(0xFFFFA726)
                            else -> Color.Gray
                        }
                    )
                }
                _rules.value = uiRules
            }
        }
    }

    fun addRule(rule: ProtectionRule) {
        viewModelScope.launch {
            ruleRepository.addRule(rule, currentMonitorId, protegeUid)
        }
    }

    fun updateRule(rule: ProtectionRule) {
        viewModelScope.launch {
            ruleRepository.updateRule(rule, currentMonitorId, protegeUid)
        }
    }

    fun deleteRule(ruleId: String) {
        viewModelScope.launch {
            ruleRepository.deleteRule(ruleId, currentMonitorId, protegeUid)
        }
    }

    fun toggleRule(ruleId: String, isEnabled: Boolean) {
        val rule = _rules.value.find { it.id == ruleId }
        if (rule != null) {
            updateRule(rule.copy(isEnabled = isEnabled))
        }
    }
}

class MonitorDashboardViewModelFactory(
    private val protegeUid: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MonitorDashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MonitorDashboardViewModel(RuleRepository(), protegeUid) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
