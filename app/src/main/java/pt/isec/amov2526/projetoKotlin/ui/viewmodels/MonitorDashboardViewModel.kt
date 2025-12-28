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
import pt.isec.amov2526.projetoKotlin.ui.screens.monitor.ProtectionRule
import pt.isec.amov2526.projetoKotlin.ui.screens.monitor.RuleType

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

    init {
        if (currentMonitorId.isNotEmpty() && protegeUid.isNotEmpty()) {
            startListeningRules()
            startListeningLocation()
        }
    }

    private fun startListeningLocation() {
        val docRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(protegeUid)
            
        docRef.addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            
            if (snapshot != null && snapshot.exists()) {
                val lat = snapshot.getDouble("lastLat")
                val lng = snapshot.getDouble("lastLng")
                if (lat != null && lng != null) {
                    _userLocation.value = org.osmdroid.util.GeoPoint(lat, lng)
                }
            }
        }
    }

    private fun startListeningRules() {
        viewModelScope.launch {
            ruleRepository.getRulesFlow(currentMonitorId, protegeUid).collect { rawRules ->
                val uiRules = rawRules.map { rule ->
                    rule.copy(
                        icon = when (rule.type) {
                            RuleType.FALL -> Icons.Default.MonitorHeart
                            RuleType.ACCIDENT -> Icons.Default.CarCrash
                            RuleType.GEOFENCING -> Icons.Default.LocationOn
                            RuleType.SPEED -> Icons.Default.Speed
                            RuleType.INACTIVITY -> Icons.Default.Schedule
                            else -> Icons.Default.Shield
                        },
                        iconColor = when (rule.type) {
                            RuleType.FALL -> Color(0xFFFF7043)
                            RuleType.ACCIDENT -> Color(0xFFEF4444)
                            RuleType.GEOFENCING -> Color(0xFF7E57C2)
                            RuleType.SPEED -> Color(0xFF42A5F5)
                            RuleType.INACTIVITY -> Color(0xFFFFA726)
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