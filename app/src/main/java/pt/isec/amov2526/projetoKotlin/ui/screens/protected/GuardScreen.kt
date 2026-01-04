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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import pt.isec.amov2526.projetoKotlin.data.model.*
import pt.isec.amov2526.projetoKotlin.data.model.InfractionDisplayInfo
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.guard.*
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.GuardViewModel
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.ProtectedMainViewModel
import pt.isec.amov2526.projetoKotlin.utils.TimeUtils
import pt.isec.amov2526.projetoKotlin.utils.LocationHelper
import pt.isec.amov2526.projetoKotlin.R

private val Slate50 = Color(0xFFF8FAFC)

@Composable
fun GuardScreen(
    viewModel: GuardViewModel, // Recebe do Pai (ProtectedMainScreen)
    protectedViewModel: ProtectedMainViewModel,
    onPanic: () -> Unit = {}   // Callback para o Pai ativar o alerta
) {
    val context = LocalContext.current
    val keyguardManager = context.getSystemService(android.content.Context.KEYGUARD_SERVICE) as android.app.KeyguardManager

    LaunchedEffect(Unit) {
        protectedViewModel.refreshServiceState()
    }

    // Como o ViewModel é partilhado, o LocationHelper já deve ter sido setado no ProtectedMainScreen
    // mas por segurança garantimos aqui também se necessário, ou usamos o do ViewModel se exposto.
    // Aqui assumimos que o LocationHelper está a funcionar via ViewModel ou Contexto Global.
    // Para simplificar e não partir a lógica antiga, instanciamos um Helper local para o AlertScreen
    val locationHelper = remember { LocationHelper(context) }
    LaunchedEffect(Unit) {
        locationHelper.startLocationUpdates(5000L)
    }
    val currentLocation by locationHelper.currentLocation.collectAsState(initial = null)


    val isActive by viewModel.isActive.collectAsState()
    val protectionGroups by viewModel.protectionGroups.collectAsState()
    val globalSchedule by viewModel.globalSchedule.collectAsState()
    val globalPausedUntil by viewModel.globalPausedUntil.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val activeGeofences by viewModel.activeGeofences.collectAsState() // [NOVO]

    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000L)
        }
    }

    // --- ESTADOS LOCAIS DOS MODAIS ---
    var showPauseModal by remember { mutableStateOf(false) }
    var showGlobalScheduleModal by remember { mutableStateOf(false) }
    var showScheduleModal by remember { mutableStateOf(false) }
    // showAlertScreen removed (handled by parent)
    
    var ruleToRemove by remember { mutableStateOf<MonitorRule?>(null) }
    var selectedTypeToPause by remember { mutableStateOf<RuleType?>(null) }
    var selectedRuleToPause by remember { mutableStateOf<MonitorRule?>(null) }
    var selectedGroupToSchedule by remember { mutableStateOf<ProtectionGroup?>(null) }
    var selectedRuleToSchedule by remember { mutableStateOf<MonitorRule?>(null) }
    
    // pendingAlertType removed (handled by parent)

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isServiceRunning = protectedViewModel.isProtectionActive

    val globalScheduleStatus = TimeUtils.calculateStatus(
        isEnabled = true, pausedUntil = globalPausedUntil, isScheduleEnabled = globalSchedule.isEnabled,
        days = globalSchedule.days, start = globalSchedule.start, end = globalSchedule.end, now = currentTime
    )

    // [INTENT HANDLING REMOVED] - Centralized in ProtectedMainScreen

    // Fix: Prioritize PAUSED over SCHEDULED
    val rawSystemStatus = if (!isServiceRunning) "OFFLINE"
    else if (globalScheduleStatus.label == "PAUSED") "PAUSED"
    else if (globalScheduleStatus.label == "SCHEDULED") "SCHEDULED"
    else "ACTIVE"

    // [AlertScreen Dialog REMOVED] - Centralized in ProtectedMainScreen

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
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
                rawSystemStatus = rawSystemStatus,
                batteryLevel = batteryLevel,
                onPlanClick = { showGlobalScheduleModal = true }, // Agora abre o modal global
                onStatusClick = {
                    if (globalScheduleStatus.label == "PAUSED") {
                        viewModel.setSystemStatus("ACTIVE")
                    } else if (protectedViewModel.isProtectionActive) {
                        selectedTypeToPause = null
                        showPauseModal = true
                    } else {
                        protectedViewModel.toggleProtection()
                    }
                }
            )

            Spacer(modifier = Modifier.height(40.dp))

            SosButton(onClick = {
                onPanic() // Delegar apenas ao Pai
            })

            Spacer(modifier = Modifier.height(40.dp))

            ActiveProtectionsCard(
                protectionGroups = protectionGroups,
                currentTime = currentTime,
                onToggleGroupExpansion = { group, expanded -> viewModel.toggleGroupExpansion(group.type, expanded) },
                onToggleGroupGlobal = { group, enabled ->
                    if (enabled) {
                        viewModel.setProtectionStatus(group.type, true)
                    } else {
                        selectedTypeToPause = group.type
                        showPauseModal = true
                    }
                },
                onUpdateRule = { ruleId, enabled ->
                    var ruleFound: MonitorRule? = null
                    for (group in protectionGroups) {
                        val r = group.rules.find { it.id == ruleId }
                        if (r != null) { ruleFound = r; break }
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
                onRemoveRule = { ruleToRemove = it },
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

    // --- MODAIS (RESTAURADOS) ---

    // 1. Modal de Pausa
    if (showPauseModal) {
        val title = when {
            selectedRuleToPause != null -> context.getString(R.string.guard_pause_rule, selectedRuleToPause!!.monitorName)
            selectedTypeToPause != null -> context.getString(R.string.guard_pause_type, context.getString(selectedTypeToPause!!.titleRes))
            else -> stringResource(id = R.string.guard_pause_protection)
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
                    if (duration == -1L) protectedViewModel.toggleProtection()
                    else viewModel.pauseGlobalProtection(duration)
                }
            },
            viewModel = viewModel,
        )
    }

    // 2. Modal de Agendamento (Regras e Grupos) - ESTAVA EM FALTA
    if (showScheduleModal) {
        val scheduleCtx = when {
            selectedRuleToSchedule != null -> Triple(
                Triple(stringResource(id = R.string.guard_schedule_rule), selectedRuleToSchedule!!.isScheduleEnabled, selectedRuleToSchedule!!.scheduleDays.ifEmpty { listOf(1, 2, 3, 4, 5) }),
                selectedRuleToSchedule!!.scheduleStart.ifEmpty { "09:00" },
                selectedRuleToSchedule!!.scheduleEnd.ifEmpty { "18:00" }
            )
            selectedGroupToSchedule != null -> Triple(
                Triple(context.getString(R.string.guard_schedule_type, context.getString(selectedGroupToSchedule!!.type.titleRes)), selectedGroupToSchedule!!.isScheduleEnabled, selectedGroupToSchedule!!.scheduleDays.ifEmpty { listOf(1, 2, 3, 4, 5) }),
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

    // 3. Modal de Agendamento Global (Botão PLAN) - ESTAVA EM FALTA
    if (showGlobalScheduleModal) {
        ScheduleSystemModal(
            title = stringResource(id = R.string.guard_global_schedule),
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

    // 4. Modal de Remover Regra
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