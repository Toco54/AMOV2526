package pt.isec.amov2526.projetoKotlin.ui.screens.protected

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import pt.isec.amov2526.projetoKotlin.data.model.*
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.guard.*
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.ProtectedMainViewModel
import pt.isec.amov2526.projetoKotlin.utils.TimeUtils
import pt.isec.amov2526.projetoKotlin.utils.LocationHelper

// --- CORES ---
private val Slate50 = Color(0xFFF8FAFC)

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
        protectedViewModel.refreshServiceState()
    }

    val locationHelper = remember { LocationHelper(context) }

    // Init Location Tracking
    LaunchedEffect(Unit) {
        viewModel.setLocationHelper(locationHelper)
//        val locationHelper = pt.isec.amov2526.projetoKotlin.utils.LocationHelper(context)
//        viewModel.setLocationHelper(locationHelper)

        locationHelper.startLocationUpdates(2000L)
    }

    val currentLocation by locationHelper.currentLocation.collectAsState(initial = null)

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

    val isServiceRunning = protectedViewModel.isProtectionActive

    val globalScheduleStatus = TimeUtils.calculateStatus(
        isEnabled = true,
        pausedUntil = globalPausedUntil,
        isScheduleEnabled = globalSchedule.isEnabled,
        days = globalSchedule.days,
        start = globalSchedule.start,
        end = globalSchedule.end,
        now = currentTime
    )

    // Fix: Prioritize PAUSED over SCHEDULED
    val finalSystemLabel = if (!isServiceRunning) "OFFLINE" 
    else if (globalScheduleStatus.label == "PAUSED") "PAUSED" 
    else if (globalScheduleStatus.label == "SCHEDULED") "SCHEDULED"
    else "ACTIVE"

    if (showAlertScreen) {
        Dialog(
            onDismissRequest = { showAlertScreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AlertScreen(
                    onCancel = { showAlertScreen = false },
                    onConfirm = { },
                    onValidatePin = { viewModel.verifyPin(it) },
                    currentLocation = currentLocation
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
            SystemStatusCard(
                isActive = isServiceRunning,
                isGlobalScheduleEnabled = globalSchedule.isEnabled,
                globalScheduleStatus = globalScheduleStatus,
                finalSystemLabel = finalSystemLabel,
                onPlanClick = { showGlobalScheduleModal = true },
                onStatusClick = {
                    if (globalScheduleStatus.label == "PAUSED") {
                        viewModel.setSystemStatus("ACTIVE")
                    } else if (protectedViewModel.isProtectionActive) {
                        // Whether ACTIVE or SCHEDULED, clicking allows setting a manual Pause
                        selectedTypeToPause = null
                        showPauseModal = true
                    } else {
                        protectedViewModel.toggleProtection()
                    }
                }
            )

            Spacer(modifier = Modifier.height(40.dp))

            // --- SOS Button ---
            SosButton(
                onClick = {
                    showAlertScreen = true
                    onPanic()
                }
            )

            Spacer(modifier = Modifier.height(40.dp))

            ActiveProtectionsCard(
                protectionGroups = protectionGroups,
                currentTime = currentTime,
                onToggleGroupExpansion = { group, expanded ->
                    viewModel.toggleGroupExpansion(group.type, expanded)
                },
                onToggleGroupGlobal = { group, enabled ->
                    if (enabled) {
                        viewModel.setProtectionStatus(group.type, true)
                    } else {
                        selectedTypeToPause = group.type
                        showPauseModal = true
                    }
                },
                onUpdateRule = { ruleId, enabled ->
                    // Find the rule in the groups to get the object
                    var ruleFound: MonitorRule? = null
                    for (group in protectionGroups) {
                        val r = group.rules.find { it.id == ruleId }
                        if (r != null) {
                            ruleFound = r
                            break
                        }
                    }

                    if (ruleFound != null) {
                        if (enabled) {
                            viewModel.toggleRule(ruleFound, true)
                        } else {
                            selectedRuleToPause = ruleFound
                            showPauseModal = true
                        }
                    }
                },
                onRemoveRule = { rule -> ruleToRemove = rule },
                onGlobalSchedule = { group ->
                    selectedGroupToSchedule = group
                    showScheduleModal = true
                },
                onRuleSchedule = { rule ->
                    selectedRuleToSchedule = rule
                    showScheduleModal = true
                }
            )

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
                    if (duration == -1L) {
                        protectedViewModel.toggleProtection()
                    } else{
                        viewModel.pauseGlobalProtection(duration)

                    }
                }
            },
            viewModel = viewModel,

        )
    }

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
