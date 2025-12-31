package pt.isec.amov2526.projetoKotlin.ui.screens.protected

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import pt.isec.amov2526.projetoKotlin.R
import pt.isec.amov2526.projetoKotlin.data.model.*
import pt.isec.amov2526.projetoKotlin.utils.TimeUtils
import pt.isec.amov2526.projetoKotlin.utils.StatusResult
import kotlinx.coroutines.launch
import java.util.Calendar
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.ProtectedMainViewModel

// --- CORES ---
private val Slate50 = Color(0xFFF8FAFC)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate400 = Color(0xFF94A3B8)
private val Slate500 = Color(0xFF64748B)
private val Slate700 = Color(0xFF334155)
private val Slate900 = Color(0xFF0F172A)
private val Emerald50 = Color(0xFFECFDF5)
private val Emerald200 = Color(0xFFA7F3D0)
private val Emerald500 = Color(0xFF10B981)
private val Emerald700 = Color(0xFF047857)
private val Indigo600 = Color(0xFF4F46E5)
private val Red50 = Color(0xFFFEF2F2)
private val Red100 = Color(0xFFFEE2E2)
private val Red600 = Color(0xFFDC2626)
private val Amber500 = Color(0xFFF59E0B)

// --- MODELOS MOVED TO GuardModels.kt ---

@Composable
fun GuardScreen(
    onPanic: () -> Unit = {},
    protectedViewModel: ProtectedMainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val viewModel: pt.isec.amov2526.projetoKotlin.ui.viewmodels.GuardViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = pt.isec.amov2526.projetoKotlin.ui.viewmodels.GuardViewModelFactory()
    )

    LaunchedEffect(Unit) {
        protectedViewModel.refreshServiceState() // Verifica se o serviço já está a correr
    }

    // Init Location Tracking
    LaunchedEffect(Unit) {
        val locationHelper = pt.isec.amov2526.projetoKotlin.utils.LocationHelper(context)
        viewModel.setLocationHelper(locationHelper)
    }

    val isActive by viewModel.isActive.collectAsState()
    val protectionGroups by viewModel.protectionGroups.collectAsState()
    val globalSchedule by viewModel.globalSchedule.collectAsState()
    val globalPausedUntil by viewModel.globalPausedUntil.collectAsState()
    
    // UI Refresh Timer for Countdowns
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000L) // Update every second
        }
    }
    
    // Local UI states
    var showPauseModal by remember { mutableStateOf(false) }
    var showGlobalScheduleModal by remember { mutableStateOf(false) }
    var showScheduleModal by remember { mutableStateOf(false) }
    var showAlertScreen by remember { mutableStateOf(false) }
    var ruleToRemove by remember { mutableStateOf<MonitorRule?>(null) }
    var selectedTypeToPause by remember { mutableStateOf<RuleType?>(null) }
    var selectedRuleToPause by remember { mutableStateOf<MonitorRule?>(null) }
    var selectedGroupToSchedule by remember { mutableStateOf<ProtectionGroup?>(null) }
    var selectedRuleToSchedule by remember { mutableStateOf<MonitorRule?>(null) }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // --- CALCULATE GLOBAL STATUS ---
    // Check Global Pause (from isActive / systemStatus in VM, but let's check manually if we had access to pausedUntil, 
    // but VM handles 'isActive' boolean based on string. We need more detail if it's paused with timer.)
    // Note: VM exposes 'isActive' boolean. If false, we check why.
    // Ideally VM should expose 'globalPausedUntil' or we infer it. 
    // Assuming 'isActive' == false means Offline OR Paused.
    // But 'isActive' is just a boolean. 
    // We will rely on the UI logic to show "PAUSED" if explicitly set.


    val isServiceRunning = protectedViewModel.isProtectionActive

    // Actually, let's use the helper to check schedule first.
    val globalScheduleStatus = TimeUtils.calculateStatus(
        isEnabled = true, // Base system is "enabled"
        pausedUntil = globalPausedUntil, // We don't have globalPausedUntil exposed directly as StateFlow in VM yet, assuming 0 for now unless we update VM
        isScheduleEnabled = globalSchedule.isEnabled,
        days = globalSchedule.days,
        start = globalSchedule.start,
        end = globalSchedule.end,
        now = currentTime
    )

    val finalSystemLabel = if (!isServiceRunning) "OFFLINE" else if (globalScheduleStatus.label == "PAUSED") "PAUSED" else "ACTIVE"
    
    // Overrule if system is manually set to "PAUSED" or "OFFLINE" via VM status text
    // (VM should ideally expose the precise state)
    // For now we assume if isActive is false, it's manually off/paused.
    
    if (showAlertScreen) {
        Dialog(
            onDismissRequest = { showAlertScreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AlertScreen(
                    onCancel = { showAlertScreen = false },
                    onConfirm = { }
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate50)
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- System Status Card ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive && globalScheduleStatus.isActive) Color.White else Slate100
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(24.dp),
                border = if (!isActive || !globalScheduleStatus.isActive) androidx.compose.foundation.BorderStroke(1.dp, Slate200) else null
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SYSTEM STATUS",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .border(1.dp, Slate200, RoundedCornerShape(20.dp))
                                    .background(Color.White, RoundedCornerShape(20.dp))
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        showGlobalScheduleModal = true
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = Slate500,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "PLAN",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Slate500,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            
                            // Status Pill
//                            val statusLabel = when {
//                                globalScheduleStatus.label == "PAUSED" -> "PAUSED"
//                                !isActive -> "OFFLINE"
//                                else -> "ACTIVE"
//                            }
                            val statusLabel = finalSystemLabel
                            
                            val statusColor = if (statusLabel == "ACTIVE") Emerald700 else Slate400
                            val statusBg = if (statusLabel == "ACTIVE") Emerald50 else Slate200
                            val statusBorder = if (statusLabel == "ACTIVE") Emerald200 else Slate200
                            
                            Box(
                                modifier = Modifier
                                    .height(28.dp)
                                    .border(width = 1.dp, color = statusBorder, shape = RoundedCornerShape(20.dp))
                                    .background(color = statusBg, shape = RoundedCornerShape(20.dp))
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        // Se o serviço estiver ativo (a variável que definimos no passo 2)
                                        // Nota: Se ainda não definiste a variável 'isServiceRunning' do passo anterior,
                                        // usa 'protectedViewModel.isProtectionActive' aqui.
                                        if (protectedViewModel.isProtectionActive) {
                                            selectedTypeToPause = null
                                            showPauseModal = true
                                        } else {
                                            // Se estiver OFFLINE, chamamos o nosso novo metodo para ligar o Serviço!
                                            protectedViewModel.toggleProtection()
                                        }
                                    }
                                    .padding(start = 8.dp, end = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    Text(
                                        text = statusLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(width = 32.dp, height = 16.dp)
                                            .background(
                                                color = if (statusLabel == "ACTIVE") Emerald500 else Slate400,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .align(if (statusLabel == "ACTIVE") Alignment.CenterEnd else Alignment.CenterStart)
                                                .padding(2.dp)
                                                .background(Color.White, CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    color = if (isActive && globalScheduleStatus.isActive) Slate900 else Slate200,
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if(!globalScheduleStatus.isActive) Icons.Default.Schedule else Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (isActive && globalScheduleStatus.isActive) Color.White else Slate500,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
//                            val titleText = when {
//                                globalScheduleStatus.label == "PAUSED" -> "System Paused"
//                                !isActive -> "System Offline"
//                                !globalScheduleStatus.isActive -> "Scheduled Off"
//                                else -> "Protected"
//                            }

                            val titleText = when(finalSystemLabel) {
                                "PAUSED" -> "System Paused"
                                "OFFLINE" -> "System Offline" // Agora reage ao Serviço
                                else -> "Protected"
                            }
                            
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.titleLarge,
                                color = if (isActive && globalScheduleStatus.isActive) Slate900 else Slate500,
                                fontWeight = FontWeight.Bold
                            )
                            
                            val subText = when {
                                globalScheduleStatus.label == "PAUSED" -> globalScheduleStatus.subLabel ?: "Paused"
                                !isActive -> "Turn on to enable protections"
                                !globalScheduleStatus.isActive -> globalScheduleStatus.subLabel ?: "Outside active hours"
                                else -> globalScheduleStatus.subLabel ?: "Active security by multiple monitors"
                            }
                                          
                            Text(
                                text = subText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (!globalScheduleStatus.isActive || globalScheduleStatus.label == "PAUSED") Amber500 else Slate500,
                                fontWeight = if (!globalScheduleStatus.isActive || globalScheduleStatus.label == "PAUSED") FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // --- SOS Button ---
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.95f else 1.0f,
                label = "scale"
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(scale)
                    .size(260.dp)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Red100, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.92f)
                        .align(Alignment.Center)
                        .shadow(20.dp, CircleShape, ambientColor = Color.Red, spotColor = Red600)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFFEF4444), Red600)
                            )
                        )
                        .clickable(interactionSource, null) {
                            showAlertScreen = true
                            onPanic()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "SOS",
                            tint = Color.White,
                            modifier = Modifier
                                .size(56.dp)
                                .padding(bottom = 4.dp)
                        )
                        Text(
                            text = "SOS",
                            color = Color.White,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = Color.Black.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "EMERGENCY",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // --- CARTÃO PRINCIPAL: PROTEÇÕES ATIVAS ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_shield_check),
                            contentDescription = null,
                            tint = Emerald500,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Active Protections",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    if (protectionGroups.isEmpty()) {
                         Text("No protections active.", style = MaterialTheme.typography.bodyMedium, color = Slate500, modifier = Modifier.padding(vertical = 12.dp))
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            protectionGroups.forEach { group ->
                                IndividualRuleTypeSubCard(
                                    group = group,
                                    currentTime = currentTime,
                                    onToggleGroup = { expanded ->
                                        viewModel.toggleGroupExpansion(group.type, expanded)
                                    },
                                    onToggleGlobal = { enabled ->
                                        if (enabled) {
                                            viewModel.setProtectionStatus(group.type, true)
                                        } else {
                                            selectedTypeToPause = group.type
                                            showPauseModal = true
                                        }
                                    },
                                    onUpdateRule = { ruleId, enabled ->
                                        val rule = group.rules.find { it.id == ruleId }
                                        if (rule != null) {
                                            if (enabled) {
                                                viewModel.toggleRule(rule, true)
                                            } else {
                                                selectedRuleToPause = rule
                                                showPauseModal = true
                                            }
                                        }
                                    },
                                    onRemoveRule = { rule -> ruleToRemove = rule },
                                    onGlobalSchedule = {
                                        selectedGroupToSchedule = group
                                        showScheduleModal = true
                                    },
                                    onRuleSchedule = { rule ->
                                        selectedRuleToSchedule = rule
                                        showScheduleModal = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showPauseModal) {
        val title = when {
            selectedRuleToPause != null -> "Pause ${selectedRuleToPause!!.monitorName}'s Rule"
            selectedTypeToPause != null -> "Pause ${selectedTypeToPause!!.title}"
            else -> "Pause Protection"
        }
        
        PauseSystemModal(
            title = title,
            onDismiss = { 
                showPauseModal = false
                selectedTypeToPause = null
                selectedRuleToPause = null
            },
            onConfirm = { pin, duration ->
                showPauseModal = false
                if (selectedRuleToPause != null) {
                    viewModel.setRulePaused(selectedRuleToPause!!.id, selectedRuleToPause!!.monitorId, duration)
                    selectedRuleToPause = null
                } else if (selectedTypeToPause != null) {
                    viewModel.setProtectionStatus(selectedTypeToPause!!, false, duration)
                    selectedTypeToPause = null
                } else {
                    viewModel.pauseAllRules(duration)
                }
            },
            viewModel = viewModel
        )
    }
    
    // Determine context for Schedule Modal
    if (showScheduleModal) {
        val scheduleCtx = when {
            selectedRuleToSchedule != null -> Triple(
                Triple("Schedule Rule", selectedRuleToSchedule!!.isScheduleEnabled, selectedRuleToSchedule!!.scheduleDays.ifEmpty { listOf(1, 2, 3, 4, 5) }),
                selectedRuleToSchedule!!.scheduleStart.ifEmpty { "09:00" },
                selectedRuleToSchedule!!.scheduleEnd.ifEmpty { "18:00" }
            )
            selectedGroupToSchedule != null -> Triple(
                Triple("Schedule ${selectedGroupToSchedule!!.type.title}", selectedGroupToSchedule!!.isScheduleEnabled, selectedGroupToSchedule!!.scheduleDays.ifEmpty { listOf(1, 2, 3, 4, 5) }),
                selectedGroupToSchedule!!.scheduleStart.ifEmpty { "09:00" },
                selectedGroupToSchedule!!.scheduleEnd.ifEmpty { "18:00" }
            )
            else -> null
        }

        if (scheduleCtx != null) {
            val (meta, start, end) = scheduleCtx
            val (title, isEnabled, days) = meta
            ScheduleSystemModal(
                title = title,
                initialEnabled = isEnabled,
                initialDays = days,
                initialStart = start,
                initialEnd = end,
                onDismiss = { 
                    showScheduleModal = false 
                    selectedGroupToSchedule = null
                    selectedRuleToSchedule = null
                },
                onSave = { newDays, newStart, newEnd, newEnabled ->
                    if (selectedRuleToSchedule != null) {
                        viewModel.setRuleSchedule(selectedRuleToSchedule!!, newDays, newStart, newEnd, newEnabled)
                    } else if (selectedGroupToSchedule != null) {
                        viewModel.setProtectionSchedule(selectedGroupToSchedule!!.type, newDays, newStart, newEnd, newEnabled)
                    }
                    showScheduleModal = false
                    selectedGroupToSchedule = null
                    selectedRuleToSchedule = null
                },
                viewModel = viewModel
            )
        }
    }
    
    if (showGlobalScheduleModal) {
        ScheduleSystemModal(
            title = "Global Schedule",
            initialEnabled = globalSchedule.isEnabled,
            initialDays = globalSchedule.days,
            initialStart = globalSchedule.start,
            initialEnd = globalSchedule.end,
            onDismiss = { showGlobalScheduleModal = false },
            onSave = { newDays, newStart, newEnd, newEnabled ->
                viewModel.setGlobalSchedule(newDays, newStart, newEnd, newEnabled)
                showGlobalScheduleModal = false
            },
            viewModel = viewModel
        )
    }

    if (ruleToRemove != null) {
        RemoveRuleChallengeModal(
            ruleName = ruleToRemove!!.monitorName,
            onDismiss = { ruleToRemove = null },
            onConfirm = { pin ->
                 scope.launch {
                     if (viewModel.verifyPin(pin)) {
                         viewModel.deleteRule(ruleToRemove!!)
                         ruleToRemove = null
                     } else {
                         snackbarHostState.showSnackbar("Invalid PIN")
                     }
                 }
            }
        )
    }
}

@Composable
fun IndividualRuleTypeSubCard(
    group: ProtectionGroup,
    currentTime: Long,
    onToggleGroup: (Boolean) -> Unit,
    onToggleGlobal: (Boolean) -> Unit,
    onUpdateRule: (String, Boolean) -> Unit,
    onRemoveRule: (MonitorRule) -> Unit,
    onGlobalSchedule: () -> Unit,
    onRuleSchedule: (MonitorRule) -> Unit
) {
    // Check Status for the whole group
    // 1. Manually Paused (via toggle)
    val isPaused = group.pausedUntil > currentTime || group.pausedUntil == -1L
    
    // 2. Scheduled Off
    val status = TimeUtils.calculateStatus(
        isEnabled = !isPaused, // If paused, it overrides schedule check for "active" but we want to show specific status
        pausedUntil = group.pausedUntil,
        isScheduleEnabled = group.isScheduleEnabled,
        days = group.scheduleDays,
        start = group.scheduleStart,
        end = group.scheduleEnd,
        now = currentTime
    )
    
    // Determine the summary text
    val summaryText = status.subLabel ?: if (status.isActive) "${group.rules.size} monitor(s)" else status.label

    // Visuals for inactive state
    val containerColor = if (status.isActive) Slate50 else Red50
    val textColor = if (status.isActive) Slate400 else Red600

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if(status.isActive) Slate100 else Red100),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clickable area for expansion (Icon + Text)
                Row(
                    modifier = Modifier
                        .clickable { onToggleGroup(!group.isExpanded) }
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(group.type.color.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = group.type.icon,
                            contentDescription = null,
                            tint = group.type.color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        color = if (group.isEffectivelyActive) Emerald500 else Slate400,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = group.type.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        }
                        Text(
                            text = summaryText,
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor,
                            fontWeight = if(!status.isActive) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                
                // Actions (Schedule + Toggle) - Not part of expansion click
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Small Circle Schedule Button
                    IconButton(
                        onClick = { onGlobalSchedule() },
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.iconButtonColors(containerColor = if(status.isActive) Slate100 else Color.White, contentColor = Slate400)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Schedule",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Switch(
                        checked = group.isEnabled && !isPaused,
                        onCheckedChange = { onToggleGlobal(it) },
                        modifier = Modifier.scale(0.6f),
                        colors = SwitchDefaults.colors(checkedTrackColor = Emerald500)
                    )
                    Icon(
                        imageVector = if (group.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Slate200,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
            AnimatedVisibility(
                visible = group.isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.5f))
                        .padding(bottom = 8.dp)
                ) {
                    HorizontalDivider(
                        color = Slate100,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    group.rules.forEach { rule ->
                        key(rule.id) {
                            MonitorDetailRow(
                                rule = rule,
                                currentTime = currentTime,
                                onToggle = { onUpdateRule(rule.id, it) },
                                onRemove = { onRemoveRule(rule) },
                                onSchedule = { onRuleSchedule(rule) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonitorDetailRow(
    rule: MonitorRule,
    currentTime: Long,
    onToggle: (Boolean) -> Unit,
    onRemove: () -> Unit,
    onSchedule: () -> Unit
) {
    // Calculate Rule Status
    val status = TimeUtils.calculateStatus(
        isEnabled = rule.isEnabled && !rule.isDisabledByProtege,
        pausedUntil = rule.pausedUntil,
        isScheduleEnabled = rule.isScheduleEnabled,
        days = rule.scheduleDays,
        start = rule.scheduleStart,
        end = rule.scheduleEnd,
        now = currentTime
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .background(Color.Transparent)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Effective Status Indicator (Dot)
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (rule.isEffectivelyActive) Emerald500 else Slate400,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "by ${rule.monitorName}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Indigo600
                )
            }
            // Show Parameter OR Status if inactive
            if (status.isActive) {
                Text(
                    text = rule.parameter,
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500
                )
            } else {
                Text(
                    text = status.subLabel ?: status.label, // "Paused: 14m"
                    style = MaterialTheme.typography.labelSmall,
                    color = Red600,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Smaller Circular Buttons
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Red50)
                    .clickable { onRemove() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Red600.copy(alpha = 0.7f),
                    modifier = Modifier.size(13.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (rule.isScheduleEnabled) Indigo600.copy(alpha = 0.1f) else Slate100)
                    .clickable { onSchedule() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = if (rule.isScheduleEnabled) Indigo600 else Slate400,
                    modifier = Modifier.size(13.dp)
                )
            }
            
            // Switch reflects "Enabled by Monitor" generally, but Protege can toggle
            // If it's paused or scheduled, switch state:
            // The switch is the main "Toggle" control.
            // If "Paused", user can toggle to "Off" (permanently disabled).
            // If "Scheduled", user can toggle to "Off".
            // So switch state tracks 'isEnabled' && '!isDisabledByProtege'.
            // Even if paused/scheduled, the *intent* is ON.
            val switchChecked = rule.isEnabled && !rule.isDisabledByProtege && rule.pausedUntil <= currentTime
            
            Switch(
                checked = switchChecked,
                enabled = rule.isEnabled,
                onCheckedChange = { onToggle(it) },
                modifier = Modifier.scale(0.55f),
                colors = SwitchDefaults.colors(
                    checkedTrackColor = Emerald500,
                    disabledCheckedTrackColor = Emerald500.copy(alpha = 0.5f),
                    disabledUncheckedTrackColor = Slate200
                )
            )
        }
    }
}

// --- HELPER FUNCTIONS ---
// Moved to TimeUtils.kt

@Composable
fun RemoveRuleChallengeModal(
    ruleName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Red600,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Remove Protection",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }
                Text(
                    text = "Do you want to remove the rule for $ruleName? This action requires the security PIN.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                Text(
                    text = "SECURITY PIN",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate400
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Slate50, RoundedCornerShape(12.dp))
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = pin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                pin = it
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        decorationBox = { innerTextField ->
                            if (pin.isEmpty()) {
                                Text(
                                    text = "****",
                                    style = TextStyle(
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate200,
                                        textAlign = TextAlign.Center,
                                        letterSpacing = 8.sp
                                    )
                                )
                            }
                            innerTextField()
                        },
                        textStyle = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = Slate900,
                            letterSpacing = 8.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { if (pin.length == 4) { isChecking = true; onConfirm(pin) } },
                    enabled = !isChecking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Red600,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = "Confirm Removal",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = "Cancel",
                        color = Slate400,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PauseSystemModal(
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit, // Updated signature
    viewModel: pt.isec.amov2526.projetoKotlin.ui.viewmodels.GuardViewModel,
    title: String // Generic Title
) {
    var pin by remember { mutableStateOf("") }
    var selectedDuration by remember { mutableStateOf("15 Min") }
    var isChecking by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PauseCircle,
                        contentDescription = null,
                        tint = Amber500,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }
                Text(
                    text = "Select duration and enter PIN.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                listOf("1 Min","15 Min", "30 Min", "1 Hour", "8 Hours", "Indefinite").forEach { dur ->
                    val isSelected = selectedDuration == dur
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) Indigo600.copy(alpha = 0.1f) else Slate50
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Indigo600 else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedDuration = dur }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dur,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Indigo600 else Slate700
                        )
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (isSelected) Indigo600 else Slate400,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "SECURITY PIN",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate400
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Slate50, RoundedCornerShape(12.dp))
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = pin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                pin = it
                                errorText = null
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        decorationBox = { innerTextField ->
                            if (pin.isEmpty()) {
                                Text(
                                    text = "****",
                                    style = TextStyle(
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate200,
                                        textAlign = TextAlign.Center,
                                        letterSpacing = 8.sp
                                    )
                                )
                            }
                            innerTextField()
                        },
                        textStyle = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = Slate900,
                            letterSpacing = 8.sp
                        )
                    )
                }
                if (errorText != null) {
                    Text(errorText!!, color = Red600, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { 
                        if (pin.length == 4) {
                            isChecking = true
                            scope.launch {
                                if (viewModel.verifyPin(pin)) {
                                    val durationMillis = when(selectedDuration) {
                                        "1 Min" -> 1 * 60 * 1000L
                                        "15 Min" -> 15 * 60 * 1000L
                                        "30 Min" -> 30 * 60 * 1000L
                                        "1 Hour" -> 60 * 60 * 1000L
                                        "8 Hours" -> 8 * 60 * 60 * 1000L
                                        "Indefinite" -> -1L
                                        else -> 0L
                                    }
                                    onConfirm(pin, durationMillis)
                                } else {
                                    errorText = "Invalid PIN"
                                    isChecking = false
                                }
                            }
                        }
                    },
                    enabled = !isChecking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Red600,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = "Confirm Pause",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = "Cancel",
                        color = Slate400,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ScheduleSystemModal(
    title: String,
    initialEnabled: Boolean,
    initialDays: List<Int>,
    initialStart: String,
    initialEnd: String,
    onDismiss: () -> Unit,
    onSave: (List<Int>, String, String, Boolean) -> Unit,
    viewModel: pt.isec.amov2526.projetoKotlin.ui.viewmodels.GuardViewModel
) {
    var pin by remember { mutableStateOf("") }
    var scheduleEnabled by remember { mutableStateOf(initialEnabled) }
    var startTime by remember { mutableStateOf(initialStart) }
    var endTime by remember { mutableStateOf(initialEnd) }
    val days = listOf("S", "M", "T", "W", "T", "F", "S")
    val selectedDays = remember {
        mutableStateListOf<Int>().apply {
            if (initialDays.isNotEmpty()) addAll(initialDays)
            else addAll(listOf(1, 2, 3, 4, 5))
        }
    }
    
    var isChecking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = Indigo600,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = scheduleEnabled,
                        onCheckedChange = { scheduleEnabled = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = Indigo600)
                    )
                }
                
                AnimatedVisibility(visible = scheduleEnabled) {
                    Column {
                        Text(
                            text = "ACTIVE DAYS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate400
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            days.forEachIndexed { i, d ->
                                val isSelected = selectedDays.contains(i)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            color = if (isSelected) Slate900 else Slate100,
                                            shape = CircleShape
                                        )
                                        .clip(CircleShape)
                                        .clickable {
                                            if (isSelected) selectedDays.remove(i)
                                            else selectedDays.add(i)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = d,
                                        color = if (isSelected) Color.White else Slate400,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "START",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate400
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .background(Slate50, RoundedCornerShape(12.dp))
                                        .border(1.dp, Slate200, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BasicTextField(
                                        value = startTime,
                                        onValueChange = { if (it.length <= 5) startTime = it },
                                        textStyle = TextStyle(
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate900,
                                            fontSize = 16.sp
                                        ),
                                        decorationBox = { it() }
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "END",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate400
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .background(Slate50, RoundedCornerShape(12.dp))
                                        .border(1.dp, Slate200, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BasicTextField(
                                        value = endTime,
                                        onValueChange = { if (it.length <= 5) endTime = it },
                                        textStyle = TextStyle(
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate900,
                                            fontSize = 16.sp
                                        ),
                                        decorationBox = { it() }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                Text(
                    text = "SECURITY PIN TO SAVE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate400
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Slate50, RoundedCornerShape(12.dp))
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = pin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                pin = it
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        decorationBox = { innerTextField ->
                            if (pin.isEmpty()) {
                                Text(
                                    text = "****",
                                    style = TextStyle(
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate200,
                                        textAlign = TextAlign.Center,
                                        letterSpacing = 8.sp
                                    )
                                )
                            }
                            innerTextField()
                        },
                        textStyle = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = Slate900,
                            letterSpacing = 8.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Slate100,
                            contentColor = Slate500
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = {
                            if (pin.length == 4) {
                                isChecking = true
                                scope.launch {
                                    if (viewModel.verifyPin(pin)) {
                                        onSave(selectedDays.toList(), startTime, endTime, scheduleEnabled)
                                    } else {
                                        isChecking = false
                                    }
                                }
                            }
                        },
                        enabled = !isChecking,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Indigo600,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = "Save",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
