package pt.isec.amov2526.projetoKotlin.ui.viewmodels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CarCrash
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

    // Rules Grouped
    private val _protectionGroups = MutableStateFlow<List<ProtectionGroup>>(emptyList())
    val protectionGroups: StateFlow<List<ProtectionGroup>> = _protectionGroups.asStateFlow()

    // Internal list to recombine
    private var allRules = mutableListOf<MonitorRule>()
    private val monitorNameMap = mutableMapOf<String, String>()
    private val listeners = mutableListOf<ListenerRegistration>()

    fun setLocationHelper(helper: LocationHelper) {
        this.locationHelper = helper
        startLocationTracking()
    }

    private fun startLocationTracking() {
        locationHelper?.startLocationUpdates(5000L) // 5 seconds
        
        viewModelScope.launch {
            locationHelper?.currentLocation?.collect { loc ->
                if (loc != null && _isActive.value) { // Only upload if active
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
        }
    }

    private fun fetchSystemStatus() {
        val docRef = db.collection("users").document(currentUserUid)
        listeners.add(docRef.addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            if (snapshot != null && snapshot.exists()) {
                val status = snapshot.getString("systemStatus") ?: "ACTIVE"
                _isActive.value = status == "ACTIVE"
                _systemStatusText.value = status
            }
        })
    }
    
    fun setSystemStatus(status: String) { // ACTIVE, PAUSED, SCHEDULED
        viewModelScope.launch {
            db.collection("users").document(currentUserUid)
                .update("systemStatus", status)
                .await()
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
        val newGroups = RuleType.values().map { type ->
            ProtectionGroup(
                type = type,
                rules = grouped[type] ?: emptyList(),
                isExpanded = _protectionGroups.value.find { it.type == type }?.isExpanded ?: true // Default expanded? or preserve state
            )
        }.filter { it.rules.isNotEmpty() } // Only show types that have rules
        
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
            
            val updates = mapOf(
                "isDisabledByProtege" to isDisabled,
                // If re-enabling, we also ensure isEnabled is true (if logic dictates monitor rule is active)
                // But Monitor controls "isEnabled". Protege controls "isDisabledByProtege" (override).
                // Let's assume enabling it clears the override.
            )
            
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
        val rulesToPause = allRules.toList() // Copy current list
        viewModelScope.launch {
            val pausedUntil = if (durationMillis == -1L) 0L else (System.currentTimeMillis() + durationMillis)
            val updates = if (durationMillis == -1L) {
                mapOf("isDisabledByProtege" to true)
            } else {
                mapOf("pausedUntil" to pausedUntil, "isDisabledByProtege" to false)
            }

            rulesToPause.forEach { rule ->
                db.collection("users").document(currentUserUid)
                    .collection("connections").document(rule.monitorId)
                    .collection("rules").document(rule.id)
                    .update(updates)
                    // We don't await each to speed up
            }
            // Also update system status for UI consistency
            db.collection("users").document(currentUserUid)
                .update("systemStatus", if (durationMillis == -1L) "OFFLINE" else "PAUSED")
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
