package pt.isec.amov2526.projetoKotlin.ui.screens.protected

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pt.isec.amov2526.projetoKotlin.R
import pt.isec.amov2526.projetoKotlin.data.model.MonitorRule
import pt.isec.amov2526.projetoKotlin.data.model.ProtectionGroup
import pt.isec.amov2526.projetoKotlin.data.model.RuleType
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.guard.ActiveProtectionsCard
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.guard.PauseSystemModal
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.guard.RemoveRuleChallengeModal
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.guard.ScheduleSystemModal
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.guard.SosButton
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.guard.SystemStatusCard
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate50
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.GuardViewModel
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.ProtectedMainViewModel
import pt.isec.amov2526.projetoKotlin.utils.LocationHelper
import pt.isec.amov2526.projetoKotlin.utils.TimeUtils

/**
 * Ecrã principal do modo "Protegido" (Guard).
 *
 * Serve como dashboard central para o utilizador que está a ser monitorizado.
 * Funcionalidades:
 * 1. Botão SOS Central: Acesso rápido a pedido de ajuda.
 * 2. Status do Sistema: Mostra se está ATIVO, PAUSADO ou OFFLINE.
 * 3. Gestão de Regras: Permite ver quem está a monitorizar e pausar regras específicas ou globais (com PIN).
 * 4. Feedback de Sensores: Nível de bateria, estado do agendamento global.
 */
@Composable
fun GuardScreen(
    viewModel: GuardViewModel,
    protectedViewModel: ProtectedMainViewModel,
    onPanic: () -> Unit = {}
) {
    val context = LocalContext.current

    // Sincroniza o estado do serviço (Foreground) com a UI ao entrar no ecrã.
    LaunchedEffect(Unit) {
        protectedViewModel.refreshServiceState()
    }

    // Inicializa a recolha de localização para atualizar a "Última Localização Conhecida" no Firestore.
    val locationHelper = remember { LocationHelper(context) }
    LaunchedEffect(Unit) {
        locationHelper.startLocationUpdates(5000L)
    }


    val protectionGroups by viewModel.protectionGroups.collectAsState()
    val globalSchedule by viewModel.globalSchedule.collectAsState()
    val globalPausedUntil by viewModel.globalPausedUntil.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()

    // Relógio local para atualizar a UI de "Tempo Restante" em pausas ou agendamentos.
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000L)
        }
    }


    // Controlo de Modais (Dialogs)
    var showPauseModal by remember { mutableStateOf(false) }
    var showGlobalScheduleModal by remember { mutableStateOf(false) }
    var showScheduleModal by remember { mutableStateOf(false) }

    // Seleção de contexto para as ações dos modais
    var ruleToRemove by remember { mutableStateOf<MonitorRule?>(null) }
    var selectedTypeToPause by remember { mutableStateOf<RuleType?>(null) }
    var selectedRuleToPause by remember { mutableStateOf<MonitorRule?>(null) }
    var selectedGroupToSchedule by remember { mutableStateOf<ProtectionGroup?>(null) }
    var selectedRuleToSchedule by remember { mutableStateOf<MonitorRule?>(null) }


    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isServiceRunning = protectedViewModel.isProtectionActive

    // Cálculo do estado atual do agendamento para exibição no cartão de status.
    val globalScheduleStatus = TimeUtils.calculateStatus(
        isEnabled = true,
        pausedUntil = globalPausedUntil,
        isScheduleEnabled = globalSchedule.isEnabled,
        days = globalSchedule.days,
        start = globalSchedule.start,
        end = globalSchedule.end,
        now = currentTime
    )

    // Resolução do status final a apresentar (Prioridade: OFFLINE > PAUSADO > AGENDADO > ATIVO).
    val rawSystemStatus = if (!isServiceRunning) "OFFLINE"
    else if (globalScheduleStatus.label == "PAUSED") "PAUSED"
    else if (globalScheduleStatus.label == "SCHEDULED") "SCHEDULED"
    else "ACTIVE"



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
                onPlanClick = { showGlobalScheduleModal = true },
                onStatusClick = {
                    // Lógica inteligente de clique no status:
                    // Se pausado -> Tenta reativar.
                    // Se ativo -> Tenta pausar (abre modal).
                    // Se offline -> Tenta ligar.
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
                onPanic()
            })

            Spacer(modifier = Modifier.height(40.dp))

            ActiveProtectionsCard(
                protectionGroups = protectionGroups,
                currentTime = currentTime,
                onToggleGroupExpansion = { group, expanded ->
                    viewModel.toggleGroupExpansion(
                        group.type,
                        expanded
                    )
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
                    var ruleFound: MonitorRule? = null
                    for (group in protectionGroups) {
                        val r = group.rules.find { it.id == ruleId }
                        if (r != null) {
                            ruleFound = r; break
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




    if (showPauseModal) {
        val title = when {
            selectedRuleToPause != null -> stringResource(
                R.string.guard_pause_rule,
                selectedRuleToPause!!.monitorName
            )

            selectedTypeToPause != null -> stringResource(
                R.string.guard_pause_type,
                stringResource(selectedTypeToPause!!.titleRes)
            )

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
                    viewModel.setRulePaused(
                        selectedRuleToPause!!.id,
                        selectedRuleToPause!!.monitorId,
                        duration
                    )
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


    if (showScheduleModal) {
        // Prepara o contexto para o modal de agendamento (Regra específica vs Grupo inteiro).
        val scheduleCtx = when {
            selectedRuleToSchedule != null -> Triple(
                Triple(
                    stringResource(id = R.string.guard_schedule_rule),
                    selectedRuleToSchedule!!.isScheduleEnabled,
                    selectedRuleToSchedule!!.scheduleDays.ifEmpty { listOf(1, 2, 3, 4, 5) }),
                selectedRuleToSchedule!!.scheduleStart.ifEmpty { "09:00" },
                selectedRuleToSchedule!!.scheduleEnd.ifEmpty { "18:00" }
            )

            selectedGroupToSchedule != null -> Triple(
                Triple(
                    stringResource(
                        R.string.guard_schedule_type,
                        stringResource(selectedGroupToSchedule!!.type.titleRes)
                    ),
                    selectedGroupToSchedule!!.isScheduleEnabled,
                    selectedGroupToSchedule!!.scheduleDays.ifEmpty { listOf(1, 2, 3, 4, 5) }),
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
                        viewModel.setRuleSchedule(
                            selectedRuleToSchedule!!,
                            newDays,
                            newStart,
                            newEnd,
                            newEnabled
                        )
                    } else if (selectedGroupToSchedule != null) {
                        viewModel.setProtectionSchedule(
                            selectedGroupToSchedule!!.type,
                            newDays,
                            newStart,
                            newEnd,
                            newEnabled
                        )
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
