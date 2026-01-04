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
import kotlinx.coroutines.tasks.await
import pt.isec.amov2526.projetoKotlin.data.model.ActivityAlertStatus
import pt.isec.amov2526.projetoKotlin.data.model.ActivityAlertType
import pt.isec.amov2526.projetoKotlin.data.model.ActivityItem
import pt.isec.amov2526.projetoKotlin.data.model.AlertStat
import pt.isec.amov2526.projetoKotlin.data.model.MonitorAction
import pt.isec.amov2526.projetoKotlin.data.model.MonitorActionState
import pt.isec.amov2526.projetoKotlin.data.model.MonitorRuleType
import pt.isec.amov2526.projetoKotlin.data.model.ProtectionRule
import pt.isec.amov2526.projetoKotlin.data.repository.RuleRepository
import pt.isec.amov2526.projetoKotlin.ui.theme.RuleAccident
import pt.isec.amov2526.projetoKotlin.ui.theme.RuleFall
import pt.isec.amov2526.projetoKotlin.ui.theme.RuleGeofence
import pt.isec.amov2526.projetoKotlin.ui.theme.RuleInactivity
import pt.isec.amov2526.projetoKotlin.ui.theme.RuleSpeed
import pt.isec.amov2526.projetoKotlin.utils.TimeUtils

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
    private val protegeUid: String
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val currentMonitorId = auth.currentUser?.uid ?: ""

    private val _activities = MutableStateFlow<List<ActivityItem>>(emptyList())
    val activities: StateFlow<List<ActivityItem>> = _activities.asStateFlow()


    private val alertListeners =
        mutableMapOf<String, com.google.firebase.firestore.ListenerRegistration>()
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


        val docRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()

            .collection("users").document(currentMonitorId)

            .collection("connections").document(protegeUid)



        android.util.Log.d(
            "MonitorVM",
            "Escutando connection em: users/$currentMonitorId/connections/$protegeUid"
        )



        docRef.addSnapshotListener { snapshot, e ->

            if (e != null) {

                android.util.Log.e("MonitorVM", "Erro ao escutar connection: ${e.message}")

                return@addSnapshotListener

            }



            if (snapshot == null || !snapshot.exists()) {

                android.util.Log.w(
                    "MonitorVM",
                    "Documento de conexão não encontrado (possível revogação)."
                )

                _shouldExitDashboard.value = true

                return@addSnapshotListener

            }


            val rawMap = snapshot.get("alertRefs") as? Map<String, Any>

            val refsMap = rawMap?.mapValues { it.value.toString() } ?: emptyMap()



            android.util.Log.d("MonitorVM", "Recebido alertRefs: ${refsMap.size} itens")

            updateAlertListeners(refsMap)

        }

    }


    private fun updateAlertListeners(refsMap: Map<String, String>) {

        val currentIds = alertListeners.keys.toSet()

        val newIds = refsMap.keys


        val idsToRemove = currentIds - newIds

        idsToRemove.forEach { id ->

            android.util.Log.d("MonitorVM", "Removendo listener do alerta: $id")

            alertListeners[id]?.remove()

            alertListeners.remove(id)

            alertDataMap.remove(id)

        }


        val idsToAdd = newIds - currentIds

        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()



        idsToAdd.forEach { id ->

            val path = refsMap[id]

            android.util.Log.d(
                "MonitorVM",
                "Adicionando listener para novo alerta: $id no caminho: $path"
            )

            if (!path.isNullOrEmpty()) {

                try {

                    val docRef = db.document(path)

                    val registration = docRef.addSnapshotListener { snapshot, e ->

                        if (e != null) {

                            android.util.Log.e(
                                "MonitorVM",
                                "Erro no listener do alerta $id: ${e.message}"
                            )

                            return@addSnapshotListener

                        }



                        if (snapshot != null && snapshot.exists()) {

                            val item = parseActivityItem(snapshot)

                            if (item != null) {

                                android.util.Log.d(
                                    "MonitorVM",
                                    "Alerta atualizado: ${item.title} ($id)"
                                )

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

                monitorsList.add(
                    MonitorAction(

                        name = m["name"] as? String ?: "Unknown",

                        state = MonitorActionState.valueOf(m["state"] as? String ?: "SENT"),

                        time = m["time"] as? String ?: ""

                    )
                )

            }



            ActivityItem(

                id = doc.id,

                type = ActivityAlertType.valueOf(typeStr),

                title = doc.getString("title") ?: "Alert",

                date = doc.getString("date") ?: "",

                time = doc.getString("time") ?: "",

                status = ActivityAlertStatus.valueOf(statusStr),

                notifiedMonitors = monitorsList,


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


        val stats = listOf(
            AlertStat("Fall", items.count { it.type == ActivityAlertType.FALL }),
            AlertStat("Crash", items.count { it.type == ActivityAlertType.ACCIDENT }),
            AlertStat("Geo", items.count { it.type == ActivityAlertType.GEOFENCE }),
            AlertStat("Speed", items.count { it.type == ActivityAlertType.SPEED }),

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

                _protegeEmail.value = snapshot.getString("email") ?: ""


                val lat = snapshot.getDouble("lastLat")
                val lng = snapshot.getDouble("lastLng")
                if (lat != null && lng != null) {
                    _userLocation.value = org.osmdroid.util.GeoPoint(lat, lng)
                }


                val activeProtectionsMap =
                    snapshot.get("activeProtections") as? Map<String, Map<String, Any>>
                        ?: emptyMap()
                val parsedStatuses = activeProtectionsMap.mapValues { (_, value) ->
                    val enabled = value["enabled"] as? Boolean ?: true
                    val paused = (value["pausedUntil"] as? Number)?.toLong() ?: 0L
                    val schedEnabled = value["isScheduleEnabled"] as? Boolean ?: false
                    val schedDays =
                        (value["scheduleDays"] as? List<Number>)?.map { it.toInt() } ?: emptyList()
                    val schedStart = value["scheduleStart"] as? String ?: ""
                    val schedEnd = value["scheduleEnd"] as? String ?: ""
                    CategoryStatus(enabled, paused, schedEnabled, schedDays, schedStart, schedEnd)
                }
                _categoryStatuses.value = parsedStatuses


                val dbStatus = snapshot.getString("systemStatus") ?: "OFFLINE"
                val lastUpdate = snapshot.getLong("lastUpdate") ?: 0L
                val now = System.currentTimeMillis()


                val globalPausedUntil = snapshot.getLong("globalPausedUntil") ?: 0L
                val gDays =
                    (snapshot.get("globalScheduleDays") as? List<Number>)?.map { it.toInt() }
                        ?: listOf(1, 2, 3, 4, 5)
                val gStart = snapshot.getString("globalScheduleStart") ?: "09:00"
                val gEnd = snapshot.getString("globalScheduleEnd") ?: "18:00"
                val gEnabled = snapshot.getBoolean("isGlobalScheduleEnabled") ?: false

                _globalSchedule.value = GlobalScheduleInfo(gEnabled, gDays, gStart, gEnd)


                var finalStatus = dbStatus
                var detail: String? = null

                if (dbStatus != "OFFLINE") {
                    val calculated = TimeUtils.calculateStatus(
                        isEnabled = (dbStatus == "ACTIVE" || dbStatus == "PAUSED"),
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
                            MonitorRuleType.FALL -> RuleFall
                            MonitorRuleType.ACCIDENT -> RuleAccident
                            MonitorRuleType.GEOFENCING -> RuleGeofence
                            MonitorRuleType.SPEED -> RuleSpeed
                            MonitorRuleType.INACTIVITY -> RuleInactivity
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

    fun markAlertAsViewed(activityId: String) {
        val activity = alertDataMap[activityId] ?: return
        currentMonitorId


        activity.notifiedMonitors.find {


            true
        }

        viewModelScope.launch {


            val path = "users/$protegeUid/alerts/$activityId"
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val docRef = db.document(path)

            try {
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(docRef)
                    if (snapshot.exists()) {
                        val monitorsRaw =
                            snapshot.get("notifiedMonitors") as? List<Map<String, Any>>
                                ?: emptyList()
                        val updatedMonitors = monitorsRaw.map { m ->
                            val uid = m["uid"] as? String


                            if (uid == currentMonitorId && m["state"] != "VIEWED") {
                                m.toMutableMap().apply {
                                    put("state", "VIEWED")
                                    put("time", TimeUtils.getCurrentTime())
                                }
                            } else {
                                m
                            }
                        }
                        transaction.update(docRef, "notifiedMonitors", updatedMonitors)
                    }
                }.await()
                android.util.Log.i("MonitorVM", "Alerta $activityId marcado como VIEWED")
            } catch (e: Exception) {
                android.util.Log.e("MonitorVM", "Erro ao marcar viewed: ${e.message}")
            }
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
