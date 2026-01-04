package pt.isec.amov2526.projetoKotlin.ui.screens.monitor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
    val systemStatus by viewModel.systemStatus.collectAsState()
    val statusDetail by viewModel.statusDetail.collectAsState()
    val pausedUntil by viewModel.pausedUntil.collectAsState()
    val globalSchedule by viewModel.globalSchedule.collectAsState()
    val categoryStatuses by viewModel.categoryStatuses.collectAsState()
    val isDisconnected by viewModel.isDisconnected.collectAsState()
    val shouldExitDashboard by viewModel.shouldExitDashboard.collectAsState()
    val protegeEmail by viewModel.protegeEmail.collectAsState()

    val activities by viewModel.activities.collectAsState()
    val alertStats by viewModel.alertStats.collectAsState()

    val context = LocalContext.current

    // Timer vivo para countdowns
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(shouldExitDashboard) {
        if (shouldExitDashboard) {
            onBack()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000L)
        }
    }

    var showAddRuleSelection by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<ProtectionRule?>(null) }
    var selectedActivity by remember { mutableStateOf<ActivityItem?>(null) }
    var ruleIdToDelete by remember { mutableStateOf<String?>(null) }
    var showStatusDetailDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Determine Effective Status for UI
    val displayStatus = if (isDisconnected) "DISCONNECTED" else systemStatus
    // Global System Active Flag for dots
    val isGlobalActive = systemStatus == "ACTIVE"
    
    val displayStatusColor = when (displayStatus) {
        "ACTIVE" -> Emerald500
        "PAUSED", "SCHEDULED" -> Color(0xFFF59E0B)
        "DISCONNECTED" -> Color(0xFFEF4444)
        else -> Slate400
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(protegeName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Slate900)
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Status Pill
                            Surface(
                                color = displayStatusColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, displayStatusColor.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    text = displayStatus,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = displayStatusColor,
                                    fontSize = 9.sp
                                )
                            }
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(id = R.string.monitor_live_dashboard), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Indigo600, letterSpacing = 0.5.sp)
                            
                            // Show details ONLY if connected
                            if (!isDisconnected) {
                                // Dynamic Countdown Logic for TopBar
                                val dynamicDetail = if (systemStatus == "PAUSED" && pausedUntil > currentTime) {
                                    context.getString(R.string.guard_resumes_in, pt.isec.amov2526.projetoKotlin.utils.TimeUtils.formatDuration(pausedUntil - currentTime))
                                } else {
                                    statusDetail
                                }

                                if (dynamicDetail != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val detailColor = if (systemStatus == "PAUSED" || systemStatus == "SCHEDULED") Color(0xFFF59E0B) else Slate500
                                    Text(
                                        text = "•  $dynamicDetail",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = detailColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.profile_logout), tint = Slate900)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showStatusDetailDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Info, stringResource(id = R.string.monitor_current_status), tint = Indigo600, modifier = Modifier.size(18.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate50)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            LiveLocationMapCard(userLocation = userLocation, rules = rules)

            AlertTrendsCard(
                stats = alertStats // <--- MUDAR ISTO (estava uma lista gigante fixa)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(id = R.drawable.ic_shield_check), null, tint = Emerald500, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(id = R.string.monitor_manage_rules), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate900)
                        }
                        IconButton(onClick = { showAddRuleSelection = true }, modifier = Modifier.size(28.dp).background(Slate100, CircleShape)) {
                            Icon(Icons.Default.Add, stringResource(id = R.string.action_add), tint = Slate900, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (rules.isEmpty()) {
                        Text(stringResource(id = R.string.monitor_no_rules), style = MaterialTheme.typography.bodySmall, color = Slate400, modifier = Modifier.padding(vertical = 12.dp))
                    } else {
                        val groupedRules = rules.groupBy { it.type }.toList().sortedBy { it.first.ordinal }
                        groupedRules.forEach { (type, typeRules) ->
                            if (type.isMultiInstance) {
                                MonitorRuleGroup(
                                    type = type,
                                    rules = typeRules,
                                    categoryStatus = categoryStatuses[type.name],
                                    currentTime = currentTime,
                                    isGlobalActive = isGlobalActive, // Pass Global Flag
                                    onToggleGroup = { enabled ->
                                        typeRules.forEach { if (it.isEnabled != enabled) viewModel.toggleRule(it.id, enabled) }
                                    },
                                    onEditRule = { if (it.deletedByProtege) viewModel.deleteRule(it.id) else editingRule = it },
                                    onToggleRule = { id, enabled -> viewModel.toggleRule(id, enabled) }
                                )
                            } else {
                                typeRules.forEach { rule ->
                                    // Calculate Category Status for Single Instance Rules (Fall/Accident)
                                    val catStatus = categoryStatuses[type.name]
                                    val catStatusResult = pt.isec.amov2526.projetoKotlin.utils.TimeUtils.calculateStatus(
                                        isEnabled = catStatus?.isEnabled ?: true,
                                        pausedUntil = catStatus?.pausedUntil ?: 0L,
                                        isScheduleEnabled = catStatus?.isScheduleEnabled ?: false,
                                        days = catStatus?.days ?: emptyList(),
                                        start = catStatus?.start ?: "",
                                        end = catStatus?.end ?: "",
                                        now = currentTime
                                    )
                                    
                                    val catStatusText = when {
                                        catStatusResult.label == "PAUSED" && (catStatus?.pausedUntil ?: 0L) > currentTime -> context.getString(R.string.guard_pause_type, context.getString(type.titleRes)) + " Paused (${pt.isec.amov2526.projetoKotlin.utils.TimeUtils.formatDuration((catStatus?.pausedUntil ?: 0L) - currentTime)})"
                                        catStatusResult.label == "SCHEDULED" -> catStatusResult.subLabel ?: stringResource(id = R.string.status_scheduled)
                                        !(catStatus?.isEnabled ?: true) || (catStatus?.pausedUntil ?: 0L) == -1L -> context.getString(R.string.guard_pause_type, context.getString(type.titleRes)) + " " + stringResource(id = R.string.monitor_rule_marked_delete)
                                        else -> ""
                                    }

                                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Slate50), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Slate100)) {
                                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(36.dp).background(rule.iconColor.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                                                Icon(rule.icon, null, tint = rule.iconColor, modifier = Modifier.size(20.dp))
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                // Calculate Effective Status for Dot
                                                val effectiveRuleActive = isGlobalActive && rule.isEnabled && !rule.deletedByProtege && !rule.isDisabledByProtege && 
                                                    (pt.isec.amov2526.projetoKotlin.utils.TimeUtils.calculateStatus(
                                                        isEnabled = true,
                                                        pausedUntil = rule.pausedUntil,
                                                        isScheduleEnabled = rule.isScheduleEnabled,
                                                        days = rule.scheduleDays,
                                                        start = rule.scheduleStart,
                                                        end = rule.scheduleEnd,
                                                        now = currentTime
                                                    ).isActive) && (catStatus?.isEnabled ?: true) && (catStatusResult.isActive)

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .background(
                                                                color = if (effectiveRuleActive) Emerald500 else Slate400,
                                                                shape = CircleShape
                                                            )
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = stringResource(id = type.titleRes),
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Slate900
                                                    )
                                                }
                                                
                                                // Header: Category Status (Separated)
                                                if (catStatusText.isNotEmpty()) {
                                                    Text(
                                                        text = catStatusText,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (catStatusText.contains("Disabled")) Red600 else Color(0xFFF59E0B),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                // Line 1: Monitor State
                                                if (!rule.isEnabled) {
                                                    Text(stringResource(id = R.string.monitor_rule_marked_delete), style = MaterialTheme.typography.labelSmall, color = Red600, fontWeight = FontWeight.Bold)
                                                }

                                                // Line 2: Protege State (Rule Only)
                                                val ruleStatus = pt.isec.amov2526.projetoKotlin.utils.TimeUtils.calculateStatus(
                                                    isEnabled = true,
                                                    pausedUntil = rule.pausedUntil,
                                                    isScheduleEnabled = rule.isScheduleEnabled,
                                                    days = rule.scheduleDays,
                                                    start = rule.scheduleStart,
                                                    end = rule.scheduleEnd,
                                                    now = currentTime
                                                )
                                                val isScheduledOff = ruleStatus.label == "SCHEDULED"

                                                val detailText = when {
                                                    rule.deletedByProtege -> stringResource(id = R.string.monitor_rule_marked_delete)
                                                    rule.isDisabledByProtege -> stringResource(id = R.string.monitor_rule_marked_delete)
                                                    rule.pausedUntil > currentTime -> "Paused (${pt.isec.amov2526.projetoKotlin.utils.TimeUtils.formatDuration(rule.pausedUntil - currentTime)})"
                                                    isScheduledOff -> ruleStatus.subLabel ?: stringResource(id = R.string.status_scheduled)
                                                    rule.isEnabled -> stringResource(id = R.string.status_active)
                                                    else -> ""
                                                }
                                                
                                                if (detailText.isNotEmpty()) {
                                                    Text(
                                                        text = detailText,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (detailText == stringResource(id = R.string.status_active)) Slate400 else Red600
                                                    )
                                                }
                                                
                                                // Line 3: Schedule Plan
                                                val schedText = pt.isec.amov2526.projetoKotlin.utils.TimeUtils.formatSchedule(rule.scheduleDays, rule.scheduleStart, rule.scheduleEnd)
                                                if (schedText.isNotEmpty()) {
                                                    Text(
                                                        text = schedText,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Slate400,
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    )
                                                }
                                            }
                                            IconButton(onClick = { if (rule.deletedByProtege) viewModel.deleteRule(rule.id) else ruleIdToDelete = rule.id }, modifier = Modifier.size(32.dp)) {
                                                Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(id = R.string.action_delete), tint = if (rule.deletedByProtege) Red600 else Slate400, modifier = Modifier.size(18.dp))
                                            }

                                            Spacer(modifier = Modifier.width(4.dp))
                                            if (rule.deletedByProtege) {
                                                Icon(Icons.Default.Warning, stringResource(id = R.string.monitor_rule_marked_delete), tint = Red600, modifier = Modifier.size(24.dp))
                                            } else {
                                                // Show status icons alongside the switch
                                                if (rule.isDisabledByProtege) {
                                                    Icon(Icons.Default.PersonOff, stringResource(id = R.string.monitor_rule_marked_delete), tint = Red600, modifier = Modifier.size(20.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                }
                                                if (rule.pausedUntil > currentTime) {
                                                    Icon(Icons.Default.PauseCircle, stringResource(id = R.string.status_paused), tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                }

                                                Switch(
                                                    checked = rule.isEnabled,
                                                    onCheckedChange = { viewModel.toggleRule(rule.id, it) },
                                                    modifier = Modifier.scale(0.6f),
                                                    colors = SwitchDefaults.colors(checkedTrackColor = Emerald500)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }

            RecentActivityCard(activities = activities, onActivityClick = { selectedActivity = it })
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showAddRuleSelection) {
        AddRuleTypeDialog(existingRules = rules, onDismiss = { showAddRuleSelection = false }, onTypeSelected = { type ->
            showAddRuleSelection = false
            val newRule = ProtectionRule(id = "NEW_${java.util.UUID.randomUUID()}", type = type, icon = when (type) { MonitorRuleType.FALL -> Icons.Default.MonitorHeart; MonitorRuleType.ACCIDENT -> Icons.Default.CarCrash; MonitorRuleType.GEOFENCING -> Icons.Default.LocationOn; MonitorRuleType.SPEED -> Icons.Default.Speed; else -> Icons.Default.Schedule }, iconColor = when (type) { MonitorRuleType.FALL -> Color(0xFFFF7043); MonitorRuleType.ACCIDENT -> Color(0xFFEF4444); MonitorRuleType.GEOFENCING -> Color(0xFF7E57C2); MonitorRuleType.SPEED -> Color(0xFF42A5F5); else -> Slate400 }, isEnabled = true)
            if (!type.isMultiInstance) viewModel.addRule(newRule) else editingRule = newRule
        })
    }

    if (editingRule != null) {
        EditRuleDialog(rule = editingRule!!, isNew = editingRule!!.id.startsWith("NEW_"), existingRules = rules, onDismiss = { editingRule = null }, onSave = { if (editingRule!!.id.startsWith("NEW_")) viewModel.addRule(it) else viewModel.updateRule(it); editingRule = null }, onDelete = { ruleIdToDelete = it })
    }

    if (ruleIdToDelete != null) {
        val ruleToDelete = rules.find { it.id == ruleIdToDelete }
        val isSoftDeleted = ruleToDelete?.deletedByProtege == true
        AlertDialog(onDismissRequest = { ruleIdToDelete = null }, title = { Text(stringResource(id = R.string.monitor_delete_rule_title), fontWeight = FontWeight.Bold, color = Slate900) }, text = { Text(if (isSoftDeleted) stringResource(id = R.string.monitor_delete_rule_soft) else stringResource(id = R.string.monitor_delete_rule_desc), color = Slate500) }, confirmButton = { Button(onClick = { viewModel.deleteRule(ruleIdToDelete!!); ruleIdToDelete = null; editingRule = null }, colors = ButtonDefaults.buttonColors(containerColor = Red600)) { Text(stringResource(id = R.string.action_delete), fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = { ruleIdToDelete = null }) { Text(stringResource(id = R.string.action_cancel), color = Slate500, fontWeight = FontWeight.Bold) } }, containerColor = Color.White, shape = RoundedCornerShape(24.dp))
    }

    if (showStatusDetailDialog) {
        ModalBottomSheet(
            onDismissRequest = { showStatusDetailDialog = false },
            sheetState = sheetState,
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
                // Protege Info Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(48.dp).background(Indigo600.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, null, tint = Indigo600, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = protegeName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = protegeEmail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate500
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider(color = Slate100)
                Spacer(modifier = Modifier.height(24.dp))

                // Section 1: Current Status
                Text(stringResource(id = R.string.monitor_current_status), style = MaterialTheme.typography.labelSmall, color = Slate400, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(displayStatusColor, CircleShape))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(displayStatus, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Slate900)
                }
                
                // Show details only if NOT disconnected
                if (!isDisconnected) {
                    if (systemStatus == "PAUSED" && pausedUntil > currentTime) {
                        val remaining = pausedUntil - currentTime
                        Text(
                            text = context.getString(R.string.guard_resumes_in, pt.isec.amov2526.projetoKotlin.utils.TimeUtils.formatDuration(remaining)),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFFF59E0B),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp, start = 24.dp)
                        )
                    } else if (statusDetail != null) {
                        Text(
                            text = statusDetail!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate500,
                            modifier = Modifier.padding(top = 4.dp, start = 24.dp)
                        )
                    }
                }

                // Section 2: Global Schedule (If enabled)
                if (globalSchedule?.isEnabled == true) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(stringResource(id = R.string.monitor_active_plan), style = MaterialTheme.typography.labelSmall, color = Slate400, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Days row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val daysLabels = listOf("S", "M", "T", "W", "T", "F", "S")
                        daysLabels.forEachIndexed { index, label ->
                            val isActive = globalSchedule!!.days.contains(index)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (isActive) Indigo600 else Slate100, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = if (isActive) Color.White else Slate400, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = Slate50,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Schedule, null, tint = Indigo600, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = context.getString(R.string.guard_active_window, globalSchedule!!.start, globalSchedule!!.end),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        }
                    }
                }

                // Section 3: Connection Warning (Extra warning)
                if (isDisconnected) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Surface(
                        color = Red600.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Red600.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = Red600, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(stringResource(id = R.string.monitor_connection_lost), color = Red600, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (selectedActivity != null) {
        // [NOVO] Converter ProtectionRules (MonitorDashboard) em MonitorRules (Shared Model) para o Mapa
        val activeGeofences = rules
            .filter { it.type == MonitorRuleType.GEOFENCING }
            .map { rule ->
                // Parse Lat,Lng from param2
                val coords = rule.param2.split(",").mapNotNull { it.trim().toDoubleOrNull() }
                val lat = coords.getOrNull(0) ?: 0.0
                val lng = coords.getOrNull(1) ?: 0.0
                val radius = rule.param1.toFloatOrNull() ?: 100f
                
                MonitorRule(
                    id = rule.id,
                    monitorId = "CURRENT_MONITOR", // Placeholder, não critico para visualização visual
                    monitorName = "Me",
                    parameter = rule.param1,
                    isEnabled = rule.isEnabled,
                    type = RuleType.GEOFENCING, // Enum Mapping
                    geofenceData = Triple(lat, lng, radius)
                )
            }

        MonitorAlertDetailModal(
            activity = selectedActivity!!, 
            onDismiss = { selectedActivity = null },
            activeGeofences = activeGeofences,
            onMarkAsViewed = { viewModel.markAlertAsViewed(it) }
        )
    }
}
