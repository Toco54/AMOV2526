package pt.isec.amov2526.projetoKotlin.ui.viewmodels

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import pt.isec.amov2526.projetoKotlin.data.model.ActivityAlertType
import pt.isec.amov2526.projetoKotlin.data.model.MonitorAction
import pt.isec.amov2526.projetoKotlin.data.model.MonitorActionState
import pt.isec.amov2526.projetoKotlin.data.model.MonitorRule
import pt.isec.amov2526.projetoKotlin.data.model.MonitorRuleType
import pt.isec.amov2526.projetoKotlin.data.model.ProtectionGroup
import pt.isec.amov2526.projetoKotlin.data.model.RuleType
import pt.isec.amov2526.projetoKotlin.data.repository.AlertRepository
import pt.isec.amov2526.projetoKotlin.data.repository.ConnectionRepository
import pt.isec.amov2526.projetoKotlin.data.repository.RuleRepository
import pt.isec.amov2526.projetoKotlin.utils.LocationHelper
import pt.isec.amov2526.projetoKotlin.utils.TimeUtils

class GuardViewModel(
    private val ruleRepository: RuleRepository,
    private val connectionRepository: ConnectionRepository,
    private val alertRepository: AlertRepository
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val currentUserUid = auth.currentUser?.uid ?: ""

    private var locationHelper: LocationHelper? = null
    private var lastLocation: Location? = null


    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _systemStatusText = MutableStateFlow("OFFLINE")
    val systemStatusText: StateFlow<String> = _systemStatusText.asStateFlow()

    private val _globalPausedUntil = MutableStateFlow(0L)
    val globalPausedUntil: StateFlow<Long> = _globalPausedUntil.asStateFlow()

    private val _isEffectiveGlobalActive = MutableStateFlow(false)
    val isEffectiveGlobalActive: StateFlow<Boolean> = _isEffectiveGlobalActive.asStateFlow()

    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()


    data class GlobalSchedule(
        val days: List<Int> = emptyList(),
        val start: String = "09:00",
        val end: String = "18:00",
        val isEnabled: Boolean = false
    )

    private val _globalSchedule = MutableStateFlow(GlobalSchedule())
    val globalSchedule: StateFlow<GlobalSchedule> = _globalSchedule.asStateFlow()


    private val _protectionGroups = MutableStateFlow<List<ProtectionGroup>>(emptyList())
    val protectionGroups: StateFlow<List<ProtectionGroup>> = _protectionGroups.asStateFlow()


    private var allRules = mutableListOf<MonitorRule>()
    private val monitorNameMap = mutableMapOf<String, String>()
    private val listeners = mutableListOf<ListenerRegistration>()


    val activeGeofences: StateFlow<List<MonitorRule>> = _protectionGroups
        .map { groups ->
            groups.find { it.type == RuleType.GEOFENCING }?.rules?.filter {
                it.isEffectivelyActive && it.geofenceData != null
            } ?: emptyList()
        }
        .stateIn(
            viewModelScope,
            kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    data class ProtectionStatus(
        val isEnabled: Boolean,
        val pausedUntil: Long,
        val scheduleDays: List<Int> = emptyList(),
        val scheduleStart: String = "",
        val scheduleEnd: String = "",
        val isScheduleEnabled: Boolean = false
    )


    private val _typeStatus = MutableStateFlow<Map<RuleType, ProtectionStatus>>(emptyMap())

    fun setLocationHelper(helper: LocationHelper) {
        this.locationHelper = helper
        startLocationTracking()
    }

    private fun startLocationTracking() {
        locationHelper?.startLocationUpdates(5000L)

        viewModelScope.launch {
            locationHelper?.currentLocation?.collect { loc ->
                if (loc != null) {

                    lastLocation = loc

                    if (_isEffectiveGlobalActive.value) {
                        db.collection("users").document(currentUserUid)
                            .update(
                                mapOf(
                                    "lastLat" to loc.latitude,
                                    "lastLng" to loc.longitude,
                                    "lastUpdate" to System.currentTimeMillis()
                                )
                            )
                    }
                }
            }
        }
    }

    fun sendAlert(
        type: ActivityAlertType,
        forcedId: String? = null,
        targetMonitorIds: List<String>? = null,
        onSuccess: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val lat = lastLocation?.latitude
            val lng = lastLocation?.longitude


            var initialActions: List<MonitorAction>? =
                null
            var monitorIds: List<String>? = targetMonitorIds


            if (monitorIds == null || monitorIds.isEmpty()) {
                try {
                    val snapshot = db.collection("users").document(currentUserUid)
                        .collection("connections")
                        .whereEqualTo("role", "Monitor")
                        .get().await()

                    monitorIds = snapshot.documents.mapNotNull { it.getString("uid") }

                    initialActions = snapshot.documents.map { doc ->
                        MonitorAction(
                            name = doc.getString("name") ?: "Monitor",
                            state = MonitorActionState.SENT,
                            time = java.text.SimpleDateFormat(
                                "HH:mm",
                                java.util.Locale.getDefault()
                            ).format(java.util.Date())
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e(
                        "GuardViewModel",
                        "Error fetching monitors for manual SOS",
                        e
                    )
                }
            } else {

                try {
                    val snapshot = db.collection("users").document(currentUserUid)
                        .collection("connections")
                        .whereIn("uid", monitorIds)
                        .get().await()

                    initialActions = snapshot.documents.map { doc ->
                        MonitorAction(
                            name = doc.getString("name") ?: "Monitor",
                            state = MonitorActionState.SENT,
                            time = java.text.SimpleDateFormat(
                                "HH:mm",
                                java.util.Locale.getDefault()
                            ).format(java.util.Date())
                        )
                    }
                } catch (e: Exception) {

                    initialActions = monitorIds.map {
                        MonitorAction(
                            "Monitor",
                            MonitorActionState.SENT,
                            ""
                        )
                    }
                }
            }

            val alertTitle = when (type) {
                ActivityAlertType.PANIC -> "SOS Manual"
                ActivityAlertType.FALL -> "Queda Detetada"
                ActivityAlertType.ACCIDENT -> "Acidente de Viação"
                ActivityAlertType.GEOFENCE -> "Saiu da Zona Segura"
                ActivityAlertType.SPEED -> "Velocidade Excessiva"
                ActivityAlertType.INACTIVITY -> "Inatividade Detetada"
                else -> "Alerta de Segurança"
            }

            val newId = alertRepository.createAlert(
                type = type,
                title = alertTitle,
                latitude = lat,
                longitude = lng,
                visibleToMonitors = monitorIds,
                initialActions = initialActions,
                forcedId = forcedId
            )

            if (newId != null) {
                onSuccess?.invoke(newId)
            }
        }
    }

    init {
        if (currentUserUid.isNotEmpty()) {
            fetchSystemStatus()
            startListeningMonitorsAndRules()
            startScheduleChecker()
        }
    }

    private fun startScheduleChecker() {
        viewModelScope.launch {
            while (true) {
                updateEffectiveStatus()
                kotlinx.coroutines.delay(60000L)
            }
        }
    }

    private fun updateEffectiveStatus() {
        val schedule = _globalSchedule.value
        val now = System.currentTimeMillis()

        val currentStatusText = _systemStatusText.value
        val pausedUntil = _globalPausedUntil.value

        if (currentStatusText == "PAUSED" && pausedUntil > 0 && pausedUntil < now) {
            setSystemStatus("ACTIVE")
            return
        }

        val status = TimeUtils.calculateStatus(
            isEnabled = _isActive.value,
            pausedUntil = pausedUntil,
            isScheduleEnabled = schedule.isEnabled,
            days = schedule.days,
            start = schedule.start,
            end = schedule.end,
            now = now
        )
        _isEffectiveGlobalActive.value = status.isActive
        rebuildProtectionGroups()
    }

    private fun fetchSystemStatus() {
        val docRef = db.collection("users").document(currentUserUid)
        listeners.add(docRef.addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            if (snapshot != null && snapshot.exists()) {
                val status = snapshot.getString("systemStatus") ?: "ACTIVE"
                _isActive.value = status == "ACTIVE"
                _systemStatusText.value = status
                _globalPausedUntil.value = snapshot.getLong("globalPausedUntil") ?: 0L

                val bat = snapshot.getLong("batteryLevel")?.toInt() ?: 100
                _batteryLevel.value = bat
                val gDays =
                    (snapshot.get("globalScheduleDays") as? List<Number>)?.map { it.toInt() }
                        ?: listOf(1, 2, 3, 4, 5)
                val gStart = snapshot.getString("globalScheduleStart") ?: "09:00"
                val gEnd = snapshot.getString("globalScheduleEnd") ?: "18:00"
                val gEnabled = snapshot.getBoolean("isGlobalScheduleEnabled") ?: false
                _globalSchedule.value = GlobalSchedule(gDays, gStart, gEnd, gEnabled)

                updateEffectiveStatus()

                val rawMap = snapshot.get("activeProtections") as? Map<String, Any>
                if (rawMap != null) {
                    val newTypeStatus = mutableMapOf<RuleType, ProtectionStatus>()
                    rawMap.forEach { (key, value) ->
                        try {
                            val type = RuleType.valueOf(key)
                            if (value is Map<*, *>) {
                                val enabled = value["enabled"] as? Boolean ?: true
                                val pausedUntil = (value["pausedUntil"] as? Number)?.toLong() ?: 0L
                                val days =
                                    (value["scheduleDays"] as? List<Number>)?.map { it.toInt() }
                                        ?: emptyList()
                                val start = value["scheduleStart"] as? String ?: ""
                                val end = value["scheduleEnd"] as? String ?: ""
                                val schedEnabled = value["isScheduleEnabled"] as? Boolean ?: false

                                newTypeStatus[type] = ProtectionStatus(
                                    enabled,
                                    pausedUntil,
                                    days,
                                    start,
                                    end,
                                    schedEnabled
                                )
                            } else if (value is Boolean) {
                                newTypeStatus[type] = ProtectionStatus(value, 0L)
                            }
                        } catch (_: Exception) {
                        }
                    }
                    _typeStatus.value = newTypeStatus
                    rebuildProtectionGroups()
                } else {
                    _typeStatus.value =
                        RuleType.values().associateWith { ProtectionStatus(true, 0L) }
                    rebuildProtectionGroups()
                }
            }
        })
    }

    fun setSystemStatus(status: String) {
        viewModelScope.launch {
            val updates = mutableMapOf<String, Any>(
                "systemStatus" to status,
                "lastUpdate" to System.currentTimeMillis()
            )
            if (status == "ACTIVE") {
                updates["globalPausedUntil"] = 0L
            }
            db.collection("users").document(currentUserUid)
                .update(updates)
                .await()
        }
    }

    fun setGlobalSchedule(days: List<Int>, start: String, end: String, enabled: Boolean) {
        viewModelScope.launch {
            db.collection("users").document(currentUserUid)
                .update(
                    mapOf(
                        "globalScheduleDays" to days,
                        "globalScheduleStart" to start,
                        "globalScheduleEnd" to end,
                        "isGlobalScheduleEnabled" to enabled
                    )
                ).await()
        }
    }

    fun setProtectionStatus(type: RuleType, enabled: Boolean, duration: Long = 0L) {
        val currentStatus = _typeStatus.value[type] ?: ProtectionStatus(true, 0L)

        val pausedUntil = if (enabled) 0L else {
            if (duration == -1L) -1L
            else System.currentTimeMillis() + duration
        }

        val newStatus = mapOf(
            "enabled" to enabled,
            "pausedUntil" to pausedUntil,
            "scheduleDays" to currentStatus.scheduleDays,
            "scheduleStart" to currentStatus.scheduleStart,
            "scheduleEnd" to currentStatus.scheduleEnd,
            "isScheduleEnabled" to currentStatus.isScheduleEnabled
        )

        viewModelScope.launch {
            db.collection("users").document(currentUserUid)
                .update("activeProtections.${type.name}", newStatus)
                .await()
        }
    }

    fun setProtectionSchedule(
        type: RuleType,
        days: List<Int>,
        start: String,
        end: String,
        isScheduleEnabled: Boolean
    ) {
        val currentStatus = _typeStatus.value[type] ?: ProtectionStatus(true, 0L)

        val newStatus = mapOf(
            "enabled" to currentStatus.isEnabled,
            "pausedUntil" to currentStatus.pausedUntil,
            "scheduleDays" to days,
            "scheduleStart" to start,
            "scheduleEnd" to end,
            "isScheduleEnabled" to isScheduleEnabled
        )

        viewModelScope.launch {
            db.collection("users").document(currentUserUid)
                .update("activeProtections.${type.name}", newStatus)
                .await()
        }
    }

    fun setRuleSchedule(
        rule: MonitorRule,
        days: List<Int>,
        start: String,
        end: String,
        isScheduleEnabled: Boolean
    ) {
        viewModelScope.launch {
            val updates = mapOf(
                "scheduleDays" to days,
                "scheduleStart" to start,
                "scheduleEnd" to end,
                "isScheduleEnabled" to isScheduleEnabled
            )

            db.collection("users").document(currentUserUid)
                .collection("connections").document(rule.monitorId)
                .collection("rules").document(rule.id)
                .update(updates).await()
        }
    }

    private fun startListeningMonitorsAndRules() {
        val query = db.collection("users").document(currentUserUid)
            .collection("connections")
            .whereEqualTo("role", "Monitor")

        listeners.add(query.addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            if (snapshot != null) {

                val currentMonitorUids =
                    snapshot.documents.mapNotNull { it.getString("uid") }.toSet()


                val monitorsToRemove = monitorNameMap.keys.filter { it !in currentMonitorUids }
                monitorsToRemove.forEach { uid ->

                    monitorNameMap.remove(uid)
                    allRules.removeAll { it.monitorId == uid }


                }


                viewModelScope.launch {
                    snapshot.documents.forEach { doc ->
                        val uid = doc.getString("uid") ?: return@forEach
                        if (!monitorNameMap.containsKey(uid)) {

                            val userDoc = db.collection("users").document(uid).get().await()
                            val name = userDoc.getString("name") ?: "Unknown"
                            monitorNameMap[uid] = name
                            listenToRulesForMonitor(uid, name)
                        }
                    }
                    rebuildProtectionGroups()
                }
            }
        })
    }

    private val monitorRuleListeners = mutableMapOf<String, ListenerRegistration>()

    private fun listenToRulesForMonitor(monitorUid: String, monitorName: String) {

        monitorRuleListeners[monitorUid]?.remove()

        val query = db.collection("users").document(currentUserUid)
            .collection("connections").document(monitorUid)
            .collection("rules")

        val registration = query.addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            if (snapshot != null) {
                allRules.removeAll { it.monitorId == monitorUid }

                val newRules = snapshot.documents.mapNotNull { doc ->
                    try {

                        if (doc.getBoolean("deletedByProtege") == true) return@mapNotNull null

                        val typeStr = doc.getString("type") ?: return@mapNotNull null
                        val type =
                            MonitorRuleType.valueOf(
                                typeStr
                            )

                        val uiType = when (type) {
                            MonitorRuleType.FALL -> RuleType.FALL
                            MonitorRuleType.ACCIDENT -> RuleType.ACCIDENT
                            MonitorRuleType.GEOFENCING -> RuleType.GEOFENCING
                            MonitorRuleType.SPEED -> RuleType.SPEED
                            MonitorRuleType.INACTIVITY -> RuleType.INACTIVITY
                            else -> RuleType.FALL // Should not happen
                        }

                        val param1 = doc.getString("param1") ?: ""
                        val param2 = doc.getString("param2") ?: ""
                        var geoData: Triple<Double, Double, Float>? = null

                        val parameterDisplay = when (uiType) {
                            RuleType.GEOFENCING -> {

                                val radius = param1.toFloatOrNull()
                                val coordParts = param2.split(Regex("[,;]"))

                                if (radius != null && coordParts.size >= 2) {
                                    val lat = coordParts[0].trim().toDoubleOrNull()
                                    val lng = coordParts[1].trim().toDoubleOrNull()

                                    if (lat != null && lng != null) {
                                        geoData = Triple(lat, lng, radius)
                                    }
                                }
                                "${param1}m"
                            }

                            RuleType.SPEED -> "${param1} KM/H"
                            RuleType.INACTIVITY -> "${param1} MIN"
                            else -> "ACTIVE"
                        }

                        MonitorRule(
                            id = doc.id,
                            monitorId = monitorUid,
                            monitorName = monitorName,
                            parameter = parameterDisplay,
                            isEnabled = doc.getBoolean("isEnabled") ?: true,
                            type = uiType,
                            scheduleDays = (doc.get("scheduleDays") as? List<Long>)?.map { it.toInt() }
                                ?: emptyList(),
                            scheduleStart = doc.getString("scheduleStart") ?: "",
                            scheduleEnd = doc.getString("scheduleEnd") ?: "",
                            isScheduleEnabled = doc.getBoolean("isScheduleEnabled") ?: false,
                            pausedUntil = doc.getLong("pausedUntil") ?: 0L,
                            isDisabledByProtege = doc.getBoolean("isDisabledByProtege") ?: false,
                            geofenceData = geoData
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                allRules.addAll(newRules)
                rebuildProtectionGroups()
            }
        }
        monitorRuleListeners[monitorUid] = registration
    }

    private fun rebuildProtectionGroups() {
        val grouped = allRules.groupBy { it.type }
        val typeStatus = _typeStatus.value
        val now = System.currentTimeMillis()
        val isGlobalActive = _isEffectiveGlobalActive.value

        val newGroups = RuleType.values().map { type ->
            val status = typeStatus[type] ?: ProtectionStatus(true, 0L)

            val localGroupStatus = TimeUtils.calculateStatus(
                isEnabled = status.isEnabled,
                pausedUntil = status.pausedUntil,
                isScheduleEnabled = status.isScheduleEnabled,
                days = status.scheduleDays,
                start = status.scheduleStart,
                end = status.scheduleEnd,
                now = now
            )

            val isGroupEffectivelyActive = isGlobalActive && localGroupStatus.isActive

            val rulesForType = (grouped[type] ?: emptyList()).map { rule ->
                val localRuleStatus = TimeUtils.calculateStatus(
                    isEnabled = rule.isEnabled && !rule.isDisabledByProtege,
                    pausedUntil = rule.pausedUntil,
                    isScheduleEnabled = rule.isScheduleEnabled,
                    days = rule.scheduleDays,
                    start = rule.scheduleStart,
                    end = rule.scheduleEnd,
                    now = now
                )
                rule.copy(isEffectivelyActive = isGroupEffectivelyActive && localRuleStatus.isActive)
            }

            ProtectionGroup(
                type = type,
                rules = rulesForType,
                isExpanded = _protectionGroups.value.find { it.type == type }?.isExpanded ?: true,
                isEnabled = status.isEnabled,
                pausedUntil = status.pausedUntil,
                scheduleDays = status.scheduleDays,
                scheduleStart = status.scheduleStart,
                scheduleEnd = status.scheduleEnd,
                isScheduleEnabled = status.isScheduleEnabled,
                isEffectivelyActive = isGroupEffectivelyActive
            )
        }.filter { it.rules.isNotEmpty() }

        _protectionGroups.value = newGroups
    }

    fun toggleGroupExpansion(type: RuleType, expanded: Boolean) {
        val current = _protectionGroups.value.toMutableList()
        val index = current.indexOfFirst { it.type == type }
        if (index != -1) {
            current[index] = current[index].copy(isExpanded = expanded)
            _protectionGroups.value = current
        }
    }

    fun toggleRule(rule: MonitorRule, enabled: Boolean) {
        val isDisabled = !enabled

        viewModelScope.launch {
            val updates = mutableMapOf<String, Any>(
                "isDisabledByProtege" to isDisabled
            )

            if (enabled) {
                updates["pausedUntil"] = 0L
            }

            db.collection("users").document(currentUserUid)
                .collection("connections").document(rule.monitorId)
                .collection("rules").document(rule.id)
                .update(updates).await()
        }
    }

    fun setRulePaused(ruleId: String, monitorId: String, durationMillis: Long) {
        viewModelScope.launch {
            val pausedUntil =
                if (durationMillis == -1L) 0L else (System.currentTimeMillis() + durationMillis)

            val updates = if (durationMillis == -1L) {
                mapOf("isDisabledByProtege" to true)
            } else {
                mapOf("pausedUntil" to pausedUntil, "isDisabledByProtege" to false)
            }

            db.collection("users").document(currentUserUid)
                .collection("connections").document(monitorId)
                .collection("rules").document(ruleId)
                .update(updates).await()
        }
    }

    fun pauseGlobalProtection(durationMillis: Long) {
        viewModelScope.launch {
            if (durationMillis == -1L) {

                db.collection("users").document(currentUserUid)
                    .update(
                        mapOf(
                            "systemStatus" to "OFFLINE",
                            "globalPausedUntil" to 0L,
                            "storedRemainingPause" to -1L
                        )
                    )
            } else {
                val pausedUntil = System.currentTimeMillis() + durationMillis
                val globalUpdates = mapOf(
                    "systemStatus" to "PAUSED",
                    "globalPausedUntil" to pausedUntil
                )
                db.collection("users").document(currentUserUid)
                    .update(globalUpdates)
            }
        }
    }

    fun deleteRule(rule: MonitorRule) {
        viewModelScope.launch {
            db.collection("users").document(currentUserUid)
                .collection("connections").document(rule.monitorId)
                .collection("rules").document(rule.id)
                .update("deletedByProtege", true)
                .await()
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        return try {
            val doc = db.collection("users").document(currentUserUid).get().await()
            val storedPin = doc.getString("pin")
            storedPin == pin
        } catch (e: Exception) {
            false
        }
    }

    override fun onCleared() {
        super.onCleared()
        listeners.forEach { it.remove() }
    }
}

class GuardViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GuardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GuardViewModel(RuleRepository(), ConnectionRepository(), AlertRepository()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}