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
import androidx.compose.ui.platform.LocalContext // Added
import pt.isec.amov2526.projetoKotlin.R
import pt.isec.amov2526.projetoKotlin.data.model.*
import kotlinx.coroutines.launch

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
    onPanic: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: pt.isec.amov2526.projetoKotlin.ui.viewmodels.GuardViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = pt.isec.amov2526.projetoKotlin.ui.viewmodels.GuardViewModelFactory()
    )

    // Init Location Tracking
    LaunchedEffect(Unit) {
        val locationHelper = pt.isec.amov2526.projetoKotlin.utils.LocationHelper(context)
        viewModel.setLocationHelper(locationHelper)
    }

    val isActive by viewModel.isActive.collectAsState()
    val protectionGroups by viewModel.protectionGroups.collectAsState()
    
    // Local UI states
    var showPauseModal by remember { mutableStateOf(false) }
    var showScheduleModal by remember { mutableStateOf(false) }
    var showAlertScreen by remember { mutableStateOf(false) }
    var ruleToRemove by remember { mutableStateOf<MonitorRule?>(null) }
    
    // Coroutine scope for async operations (PIN verification)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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
                    containerColor = if (isActive) Color.White else Slate100
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(24.dp),
                border = if (!isActive) androidx.compose.foundation.BorderStroke(1.dp, Slate200) else null
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
                                    .clickable { showScheduleModal = true }
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
                            Box(
                                modifier = Modifier
                                    .height(28.dp)
                                    .border(
                                        width = 1.dp,
                                        color = if (isActive) Emerald200 else Slate200,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .background(
                                        color = if (isActive) Emerald50 else Color.White,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        if (isActive) showPauseModal = true else viewModel.setSystemStatus("ACTIVE")
                                    }
                                    .padding(start = 8.dp, end = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    Text(
                                        text = if (isActive) "ACTIVE" else "OFFLINE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isActive) Emerald700 else Slate400,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(width = 32.dp, height = 16.dp)
                                            .background(
                                                color = if (isActive) Emerald500 else Slate200,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .align(if (isActive) Alignment.CenterEnd else Alignment.CenterStart)
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
                                    color = if (isActive) Slate900 else Slate200,
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (isActive) Color.White else Slate500,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = if (isActive) "Protected" else "System Paused",
                                style = MaterialTheme.typography.titleLarge,
                                color = if (isActive) Slate900 else Slate500,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Active security by multiple monitors",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Slate500
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
                                    onToggleGroup = { expanded ->
                                        viewModel.toggleGroupExpansion(group.type, expanded)
                                    },
                                    onToggleGlobal = { enabled ->
                                        if (!enabled) showPauseModal = true
                                        else {
                                            // Enable all rules in group
                                            group.rules.forEach { viewModel.toggleRule(it, true) }
                                        }
                                    },
                                    onUpdateRule = { ruleId, enabled ->
                                        val rule = group.rules.find { it.id == ruleId }
                                        if (rule != null) {
                                            if (!enabled) {
                                                 // Check if we want pin for single rule disable? Mockup implies yes for "Remove", maybe not for toggle?
                                                 // The logic: if (!enabled) showPauseModal = true -> This pauses SYSTEM.
                                                 // Let's assume toggle simply toggles for now, unless we want strict mode.
                                                 // Mockup logic was: onToggleGlobal(!enabled) -> showPause.
                                                 // For single rule:
                                                 viewModel.toggleRule(rule, enabled)
                                            } else {
                                                 viewModel.toggleRule(rule, enabled)
                                            }
                                        }
                                    },
                                    onRemoveRule = { rule -> ruleToRemove = rule },
                                    onGlobalSchedule = { showScheduleModal = true }
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
        PauseSystemModal(
            onDismiss = { showPauseModal = false },
            onConfirm = { pin ->
                showPauseModal = false
            },
            viewModel = viewModel // Pass VM to modal
        )
    }
    if (showScheduleModal) {
        ScheduleSystemModal(
            onDismiss = { showScheduleModal = false },
            onSave = { showScheduleModal = false }
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
    onToggleGroup: (Boolean) -> Unit,
    onToggleGlobal: (Boolean) -> Unit,
    onUpdateRule: (String, Boolean) -> Unit,
    onRemoveRule: (MonitorRule) -> Unit,
    onGlobalSchedule: () -> Unit
) {
    val isAnyActive = group.rules.any { it.isEnabled }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate50),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleGroup(!group.isExpanded) }
                    .padding(12.dp),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.type.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = "${group.rules.size} monitor(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Small Circle Schedule Button
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Slate100)
                            .clickable { onGlobalSchedule() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Switch(
                        checked = isAnyActive,
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
                        MonitorDetailRow(
                            rule = rule,
                            onToggle = { onUpdateRule(rule.id, it) },
                            onRemove = { onRemoveRule(rule) },
                            onSchedule = onGlobalSchedule
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonitorDetailRow(
    rule: MonitorRule,
    onToggle: (Boolean) -> Unit,
    onRemove: () -> Unit,
    onSchedule: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .background(Color.Transparent)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "by ${rule.monitorName}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Indigo600
            )
            Text(
                text = rule.parameter,
                style = MaterialTheme.typography.labelSmall,
                color = Slate500
            )
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
                    .background(Slate100)
                    .clickable { onSchedule() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = Slate400,
                    modifier = Modifier.size(13.dp)
                )
            }
            Switch(
                checked = rule.isEnabled,
                onCheckedChange = { onToggle(it) },
                modifier = Modifier.scale(0.55f),
                colors = SwitchDefaults.colors(checkedTrackColor = Emerald500)
            )
        }
    }
}

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
    onConfirm: (String) -> Unit,
    viewModel: pt.isec.amov2526.projetoKotlin.ui.viewmodels.GuardViewModel
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
                        text = "Pause Protection",
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
                listOf("15 Min", "30 Min", "1 Hour", "8 Hours", "Indefinite").forEach { dur ->
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
                                        "15 Min" -> 15 * 60 * 1000L
                                        "30 Min" -> 30 * 60 * 1000L
                                        "1 Hour" -> 60 * 60 * 1000L
                                        "8 Hours" -> 8 * 60 * 60 * 1000L
                                        "Indefinite" -> -1L
                                        else -> 0L
                                    }
                                    viewModel.pauseAllRules(durationMillis)
                                    onConfirm(pin)
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
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("18:00") }
    val days = listOf("S", "M", "T", "W", "T", "F", "S")
    val selectedDays = remember { mutableStateListOf(1, 2, 3, 4, 5) }
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
                        text = "Schedule Plan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }
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
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = if (selectedDays.contains(i)) Slate900 else Slate100,
                                    shape = CircleShape
                                )
                                .clip(CircleShape)
                                .clickable {
                                    if (selectedDays.contains(i)) selectedDays.remove(i)
                                    else selectedDays.add(i)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = d,
                                color = if (selectedDays.contains(i)) Color.White else Slate400,
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
                                onValueChange = { startTime = it },
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
                                onValueChange = { endTime = it },
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
                        onClick = { if (pin.length == 4) onSave() },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Indigo600,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
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