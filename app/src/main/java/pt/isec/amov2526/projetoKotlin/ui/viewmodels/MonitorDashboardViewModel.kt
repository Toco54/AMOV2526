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

data class GlobalScheduleInfo(
    val isEnabled: Boolean,
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

    private val _isDisconnected = MutableStateFlow(false)
    val isDisconnected: StateFlow<Boolean> = _isDisconnected.asStateFlow()

    private val _protegeEmail = MutableStateFlow("")
    val protegeEmail: StateFlow<String> = _protegeEmail.asStateFlow()

    init {
        if (currentMonitorId.isNotEmpty() && protegeUid.isNotEmpty()) {
            startListeningRules()
            startListeningUserStatus()
        }
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
