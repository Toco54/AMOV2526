package pt.isec.amov2526.projetoKotlin.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import pt.isec.amov2526.projetoKotlin.data.model.ConnectedUser
import pt.isec.amov2526.projetoKotlin.data.model.MonitorRule
import pt.isec.amov2526.projetoKotlin.data.model.ProtectionGroup
import pt.isec.amov2526.projetoKotlin.data.model.RuleType
import pt.isec.amov2526.projetoKotlin.data.repository.ConnectionRepository
import pt.isec.amov2526.projetoKotlin.data.repository.RuleRepository
import pt.isec.amov2526.projetoKotlin.utils.LocationHelper
import pt.isec.amov2526.projetoKotlin.utils.TimeUtils

class GuardViewModel(
    private val ruleRepository: RuleRepository,
    private val connectionRepository: ConnectionRepository,
    // Note: Ideally inject this, but for now we instantiate or pass via factory
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val currentUserUid = auth.currentUser?.uid ?: ""
    
    // Will be initialized in init if possible, or we need context. 
    // Since ViewModel shouldn't hold context, we might need to pass LocationHelper from Factory.
    // I'll update Factory below.
    private var locationHelper: LocationHelper? = null

    // System Status
    private val _isActive = MutableStateFlow(true)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _systemStatusText = MutableStateFlow("ACTIVE")
    val systemStatusText: StateFlow<String> = _systemStatusText.asStateFlow()

    private val _globalPausedUntil = MutableStateFlow(0L)
    val globalPausedUntil: StateFlow<Long> = _globalPausedUntil.asStateFlow()

    private val _isEffectiveGlobalActive = MutableStateFlow(true)
    val isEffectiveGlobalActive: StateFlow<Boolean> = _isEffectiveGlobalActive.asStateFlow()

    // Global Schedule State
    data class GlobalSchedule(
        val days: List<Int> = emptyList(),
        val start: String = "09:00",
        val end: String = "18:00",
        val isEnabled: Boolean = false
    )
    private val _globalSchedule = MutableStateFlow(GlobalSchedule())
    val globalSchedule: StateFlow<GlobalSchedule> = _globalSchedule.asStateFlow()

    // Rules Grouped
    private val _protectionGroups = MutableStateFlow<List<ProtectionGroup>>(emptyList())
    val protectionGroups: StateFlow<List<ProtectionGroup>> = _protectionGroups.asStateFlow()

    // Internal list to recombine
    private var allRules = mutableListOf<MonitorRule>()
    private val monitorNameMap = mutableMapOf<String, String>()
    private val listeners = mutableListOf<ListenerRegistration>()
    
    data class ProtectionStatus(
        val isEnabled: Boolean, 
        val pausedUntil: Long,
        val scheduleDays: List<Int> = emptyList(),
        val scheduleStart: String = "",
        val scheduleEnd: String = "",
        val isScheduleEnabled: Boolean = false
    )
    
    // Type Status (Global switches for Protege)
    private val _typeStatus = MutableStateFlow<Map<RuleType, ProtectionStatus>>(emptyMap())

    fun setLocationHelper(helper: LocationHelper) {
        this.locationHelper = helper
        startLocationTracking()
    }

    private fun startLocationTracking() {
        locationHelper?.startLocationUpdates(5000L) // 5 seconds
        
        viewModelScope.launch {
            locationHelper?.currentLocation?.collect { loc ->
                // Check Effective Status (Schedule + Pause + Manual)
                if (loc != null && _isEffectiveGlobalActive.value) { 
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

    init {
        if (currentUserUid.isNotEmpty()) {
            fetchSystemStatus()
            fetchMonitorsAndRules()
            startScheduleChecker()
        }
    }
    
    private fun startScheduleChecker() {
        viewModelScope.launch {
            while (true) {
                updateEffectiveStatus()
                kotlinx.coroutines.delay(60000L) // Check every minute
            }
        }
    }
    
    private fun updateEffectiveStatus() {
        val schedule = _globalSchedule.value
        val now = System.currentTimeMillis()

        val currentStatusText = _systemStatusText.value
        val pausedUntil = _globalPausedUntil.value


        if (currentStatusText == "PAUSED" && pausedUntil > 0 && pausedUntil < now) {
            // O tempo de pausa acabou! Forçar o sistema a voltar a ACTIVE na Cloud.
            setSystemStatus("ACTIVE")
            return // A função será chamada novamente quando a DB atualizar
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
                
                // Fetch Global Schedule
                val gDays = (snapshot.get("globalScheduleDays") as? List<Number>)?.map { it.toInt() } ?: listOf(1, 2, 3, 4, 5)
                val gStart = snapshot.getString("globalScheduleStart") ?: "09:00"
                val gEnd = snapshot.getString("globalScheduleEnd") ?: "18:00"
                val gEnabled = snapshot.getBoolean("isGlobalScheduleEnabled") ?: false
                _globalSchedule.value = GlobalSchedule(gDays, gStart, gEnd, gEnabled)
                
                updateEffectiveStatus()
                
                // Fetch active protection types
                // Structure: activeProtections = { "FALL": { "enabled": true, "pausedUntil": 0, "scheduleDays": [], ... } }
                val rawMap = snapshot.get("activeProtections") as? Map<String, Any>
                if (rawMap != null) {
                    val newTypeStatus = mutableMapOf<RuleType, ProtectionStatus>()
                    rawMap.forEach { (key, value) ->
                        try {
                            val type = RuleType.valueOf(key)
                            if (value is Map<*, *>) {
                                val enabled = value["enabled"] as? Boolean ?: true
                                val pausedUntil = (value["pausedUntil"] as? Number)?.toLong() ?: 0L
                                val days = (value["scheduleDays"] as? List<Number>)?.map { it.toInt() } ?: emptyList()
                                val start = value["scheduleStart"] as? String ?: ""
                                val end = value["scheduleEnd"] as? String ?: ""
                                val schedEnabled = value["isScheduleEnabled"] as? Boolean ?: false
                                
                                newTypeStatus[type] = ProtectionStatus(enabled, pausedUntil, days, start, end, schedEnabled)
                            } else if (value is Boolean) {
                                // Backward compatibility for boolean-only map
                                newTypeStatus[type] = ProtectionStatus(value, 0L)
                            }
                        } catch (_: Exception) {}
                    }
                    _typeStatus.value = newTypeStatus
                    rebuildProtectionGroups()
                } else {
                     // Default to all true if not set
                     _typeStatus.value = RuleType.values().associateWith { ProtectionStatus(true, 0L) }
                     rebuildProtectionGroups()
                }
            }
        })
    }
    
    fun setSystemStatus(status: String) { // ACTIVE, PAUSED, SCHEDULED
        viewModelScope.launch {
            val updates = mutableMapOf<String, Any>("systemStatus" to status)
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
                .update(mapOf(
                    "globalScheduleDays" to days,
                    "globalScheduleStart" to start,
                    "globalScheduleEnd" to end,
                    "isGlobalScheduleEnabled" to enabled
                )).await()
        }
    }
    
    fun setProtectionStatus(type: RuleType, enabled: Boolean, duration: Long = 0L) {
        val currentStatus = _typeStatus.value[type] ?: ProtectionStatus(true, 0L)
        
        val pausedUntil = if (enabled) 0L else {
            if (duration == -1L) -1L // Indefinite
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
    
    fun setProtectionSchedule(type: RuleType, days: List<Int>, start: String, end: String, isScheduleEnabled: Boolean) {
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

    fun setRuleSchedule(rule: MonitorRule, days: List<Int>, start: String, end: String, isScheduleEnabled: Boolean) {
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

    private fun fetchMonitorsAndRules() {
        viewModelScope.launch {
            val monitorsResult = connectionRepository.getMonitors(currentUserUid)
            if (monitorsResult.isSuccess) {
                val monitors = monitorsResult.getOrDefault(emptyList())
                monitors.forEach { monitor ->
                    monitorNameMap[monitor.uid] = monitor.name
                    listenToRulesForMonitor(monitor)
                }
            }
        }
    }

    private fun listenToRulesForMonitor(monitor: ConnectedUser) {
        val query = db.collection("users").document(currentUserUid)
            .collection("connections").document(monitor.uid)
            .collection("rules")

        val registration = query.addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            if (snapshot != null) {
                // Remove old rules for this monitor
                allRules.removeAll { it.monitorId == monitor.uid }

                val newRules = snapshot.documents.mapNotNull { doc ->
                    try {
                        val typeStr = doc.getString("type") ?: return@mapNotNull null
                        val type = pt.isec.amov2526.projetoKotlin.ui.screens.monitor.RuleType.valueOf(typeStr) // Use shared enum mapping if possible
                        
                        // Map internal RuleType to UI RuleType
                        val uiType = when(type) {
                            pt.isec.amov2526.projetoKotlin.ui.screens.monitor.RuleType.FALL -> RuleType.FALL
                            pt.isec.amov2526.projetoKotlin.ui.screens.monitor.RuleType.ACCIDENT -> RuleType.ACCIDENT
                            pt.isec.amov2526.projetoKotlin.ui.screens.monitor.RuleType.GEOFENCING -> RuleType.GEOFENCING
                            pt.isec.amov2526.projetoKotlin.ui.screens.monitor.RuleType.SPEED -> RuleType.SPEED
                            pt.isec.amov2526.projetoKotlin.ui.screens.monitor.RuleType.INACTIVITY -> RuleType.INACTIVITY
                        }

                        val param1 = doc.getString("param1") ?: ""
                        val parameterDisplay = when(uiType) {
                            RuleType.GEOFENCING -> "${param1}m"
                            RuleType.SPEED -> "${param1} KM/H"
                            RuleType.INACTIVITY -> "${param1} MIN"
                            else -> "ACTIVE"
                        }

                        MonitorRule(
                            id = doc.id,
                            monitorId = monitor.uid, // Store ID for logic
                            monitorName = monitor.name,
                            parameter = parameterDisplay,
                            isEnabled = doc.getBoolean("isEnabled") ?: true,
                            type = uiType,
                            scheduleDays = (doc.get("scheduleDays") as? List<Long>)?.map { it.toInt() } ?: emptyList(),
                            scheduleStart = doc.getString("scheduleStart") ?: "",
                            scheduleEnd = doc.getString("scheduleEnd") ?: "",
                            isScheduleEnabled = doc.getBoolean("isScheduleEnabled") ?: false,
                            pausedUntil = doc.getLong("pausedUntil") ?: 0L,
                            isDisabledByProtege = doc.getBoolean("isDisabledByProtege") ?: false
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                
                allRules.addAll(newRules)
                rebuildProtectionGroups()
            }
        }
        listeners.add(registration)
    }

    private fun rebuildProtectionGroups() {
        val grouped = allRules.groupBy { it.type }
        val typeStatus = _typeStatus.value
        val now = System.currentTimeMillis()
        val isGlobalActive = _isEffectiveGlobalActive.value
        
        val newGroups = RuleType.values().map { type ->
            val status = typeStatus[type] ?: ProtectionStatus(true, 0L)
            
            // Local Group Status
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

            // Map Rules with their effective status
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

    // When Protege toggles a rule off, we set isDisabledByProtege = true.
    // If they toggle it ON, we set isDisabledByProtege = false (re-enabling it).
    // Note: 'enabled' param here comes from the Switch checked state.
    // If Switch is ON -> rule is Active. If OFF -> rule is Disabled.
    fun toggleRule(rule: MonitorRule, enabled: Boolean) {
        val isDisabled = !enabled
        
        viewModelScope.launch {
            // We use direct Firestore update here for specific field to avoid overwriting monitor params if possible,
            // but consistency is key. Since we have local copy, we can reconstruct.
            // But 'param1' and 'param2' might change from Monitor side?
            // Safer to just update the specific field for Protege control.
            
            val updates = mutableMapOf<String, Any>(
                "isDisabledByProtege" to isDisabled
            )
            
            if (enabled) {
                // If re-enabling, clear any pause timer
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
            val pausedUntil = if (durationMillis == -1L) 0L else (System.currentTimeMillis() + durationMillis)
            
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

    fun pauseAllRules(durationMillis: Long) {
        viewModelScope.launch {
            val pausedUntil = if (durationMillis == -1L) 0L else (System.currentTimeMillis() + durationMillis)
            
            // Also update system status for UI consistency
            val globalUpdates = mapOf(
                "systemStatus" to (if (durationMillis == -1L) "OFFLINE" else "PAUSED"),
                "globalPausedUntil" to pausedUntil
            )
            db.collection("users").document(currentUserUid)
                .update(globalUpdates)
        }
    }

    fun deleteRule(rule: MonitorRule) {
        viewModelScope.launch {
            ruleRepository.deleteRule(rule.id, rule.monitorId, currentUserUid)
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
            return GuardViewModel(RuleRepository(), ConnectionRepository()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}