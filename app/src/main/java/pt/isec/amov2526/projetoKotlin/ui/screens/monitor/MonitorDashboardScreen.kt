package pt.isec.amov2526.projetoKotlin.ui.screens.monitor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CarCrash
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.isec.amov2526.projetoKotlin.R
import pt.isec.amov2526.projetoKotlin.data.model.*
import pt.isec.amov2526.projetoKotlin.ui.screens.monitor.components.dashboard.*
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.MonitorDashboardViewModel
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.MonitorDashboardViewModelFactory

// --- CORES ---
private val Slate50 = Color(0xFFF8FAFC)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate400 = Color(0xFF94A3B8)
private val Slate500 = Color(0xFF64748B)
private val Slate900 = Color(0xFF0F172A)
private val Indigo600 = Color(0xFF4F46E5)
private val Emerald500 = Color(0xFF10B981)
private val Red600 = Color(0xFFDC2626)

private val mockMonitorActivities = listOf(
    ActivityItem("1", ActivityAlertType.SPEED, "Speed Alert", "23/12/2025", "21:36", ActivityAlertStatus.CONFIRMED, listOf(MonitorAction("Alice Monitor", MonitorActionState.CALLED_USER, "21:37"))),
    ActivityItem("2", ActivityAlertType.FALL, "Fall Detected", "22/12/2025", "14:20", ActivityAlertStatus.CANCELLED, listOf(MonitorAction("Alice Monitor", MonitorActionState.MARKED_SAFE, "14:22"))),
    ActivityItem("3", ActivityAlertType.GEOFENCE, "Safe Zone Exit", "20/12/2025", "09:15", ActivityAlertStatus.CONFIRMED),
    ActivityItem("4", ActivityAlertType.PANIC, "SOS Activated", "18/12/2025", "23:45", ActivityAlertStatus.PENDING)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorDashboardScreen(
    protegeName: String = "Bob Protected",
    protegeUid: String = "",
    onBack: () -> Unit = {}
) {
    val viewModel: MonitorDashboardViewModel = viewModel(
        factory = MonitorDashboardViewModelFactory(protegeUid)
    )
    val rules by viewModel.rules.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()

    var showAddRuleSelection by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<ProtectionRule?>(null) }
    var selectedActivity by remember { mutableStateOf<ActivityItem?>(null) }
    var ruleIdToDelete by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(protegeName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Slate900)
                        Text("LIVE DASHBOARD", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Indigo600, letterSpacing = 0.5.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Slate900)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().background(Slate50).padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            LiveLocationMapCard(userLocation = userLocation, rules = rules)

            AlertTrendsCard(
                stats = listOf(
                    AlertStat("Fall", 0), AlertStat("Crash", 0), AlertStat("Geo", 1),
                    AlertStat("Speed", 2), AlertStat("Idle", 0), AlertStat("SOS", 1)
                )
            )

            // Rules Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(id = R.drawable.ic_shield_check), null, tint = Emerald500, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Manage Protection Rules", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate900)
                        }
                        IconButton(onClick = { showAddRuleSelection = true }, modifier = Modifier.size(28.dp).background(Slate100, CircleShape)) {
                            Icon(Icons.Default.Add, "Add", tint = Slate900, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (rules.isEmpty()) {
                        Text("No active rules. Tap + to add.", style = MaterialTheme.typography.bodySmall, color = Slate400, modifier = Modifier.padding(vertical = 12.dp))
                    } else {
                        // Group rules by Type and sort by enum ordinal
                        val groupedRules = rules.groupBy { it.type }.toList().sortedBy { it.first.ordinal }
                        
                        groupedRules.forEach { (type, typeRules) ->
                            if (type.isMultiInstance) {
                                // Multi-instance: Use Expandable Group
                                MonitorRuleGroup(
                                    type = type,
                                    rules = typeRules,
                                    onToggleGroup = { enabled ->
                                        typeRules.forEach { if (it.isEnabled != enabled) viewModel.toggleRule(it.id, enabled) }
                                    },
                                    onEditRule = { editingRule = it },
                                    onToggleRule = { id, enabled -> viewModel.toggleRule(id, enabled) }
                                )
                            } else {
                                // Single-instance: Render standalone card
                                val rule = typeRules.firstOrNull()
                                if (rule != null) {
                                    // Use a simplified version of the row logic wrapped in a Card
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Slate50),
                                        shape = RoundedCornerShape(16.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Icon
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(rule.iconColor.copy(alpha = 0.1f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(rule.icon, null, tint = rule.iconColor, modifier = Modifier.size(20.dp))
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            
                                            // Text
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = type.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Slate900
                                                )
                                                Text(
                                                    text = if(rule.isEnabled) "Monitoring Active" else "Disabled",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Slate400
                                                )
                                            }
                                            
                                            // Delete (optional for singleton? Maybe just toggle is enough, but keeping consistency)
                                            IconButton(onClick = { ruleIdToDelete = rule.id }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.Delete, null, tint = Slate400, modifier = Modifier.size(18.dp))
                                            }

                                            Spacer(modifier = Modifier.width(4.dp))

                                            // Switch
                                            if (rule.isDisabledByProtege) {
                                                Icon(Icons.Default.PersonOff, "Disabled by User", tint = Red600, modifier = Modifier.size(24.dp))
                                            } else if (rule.pausedUntil > System.currentTimeMillis()) {
                                                Icon(Icons.Default.PauseCircle, "Paused", tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                                            } else {
                                                Switch(
                                                    checked = rule.isEnabled,
                                                    onCheckedChange = { viewModel.toggleRule(rule.id, it) },
                                                    modifier = Modifier.scale(0.6f),
                                                    colors = SwitchDefaults.colors(checkedTrackColor = Emerald500)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }

            RecentActivityCard(activities = mockMonitorActivities, onActivityClick = { selectedActivity = it })
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showAddRuleSelection) {
        AddRuleTypeDialog(existingRules = rules, onDismiss = { showAddRuleSelection = false }, onTypeSelected = { type ->
            showAddRuleSelection = false
            editingRule = ProtectionRule(
                id = "NEW_${java.util.UUID.randomUUID()}",
                type = type,
                icon = when (type) {
                    MonitorRuleType.FALL -> Icons.Default.MonitorHeart
                    MonitorRuleType.ACCIDENT -> Icons.Default.CarCrash
                    MonitorRuleType.GEOFENCING -> Icons.Default.LocationOn
                    MonitorRuleType.SPEED -> Icons.Default.Speed
                    else -> Icons.Default.Schedule
                },
                iconColor = when (type) {
                    MonitorRuleType.FALL -> Color(0xFFFF7043)
                    MonitorRuleType.ACCIDENT -> Color(0xFFEF4444)
                    MonitorRuleType.GEOFENCING -> Color(0xFF7E57C2)
                    MonitorRuleType.SPEED -> Color(0xFF42A5F5)
                    else -> Slate400
                },
                isEnabled = true
            )
        })
    }

    if (editingRule != null) {
        EditRuleDialog(
            rule = editingRule!!,
            isNew = editingRule!!.id.startsWith("NEW_"),
            existingRules = rules,
            onDismiss = { editingRule = null },
            onSave = { if (editingRule!!.id.startsWith("NEW_")) viewModel.addRule(it) else viewModel.updateRule(it); editingRule = null },
            onDelete = { ruleIdToDelete = it }
        )
    }

    if (ruleIdToDelete != null) {
        AlertDialog(
            onDismissRequest = { ruleIdToDelete = null },
            title = { Text("Delete Rule", fontWeight = FontWeight.Bold, color = Slate900) },
            text = { Text("Are you sure you want to remove this protection rule? This action cannot be undone.", color = Slate500) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRule(ruleIdToDelete!!)
                        ruleIdToDelete = null
                        editingRule = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red600)
                ) { Text("Delete", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { ruleIdToDelete = null }) { Text("Cancel", color = Slate500, fontWeight = FontWeight.Bold) }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (selectedActivity != null) {
        MonitorAlertDetailModal(activity = selectedActivity!!, onDismiss = { selectedActivity = null })
    }
}