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
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.GuardViewModel
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.ProtectedMainViewModel
import pt.isec.amov2526.projetoKotlin.utils.TimeUtils
import pt.isec.amov2526.projetoKotlin.utils.LocationHelper

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
    var showAlertScreen by remember { mutableStateOf(false) }
    
    var ruleToRemove by remember { mutableStateOf<MonitorRule?>(null) }
    var selectedTypeToPause by remember { mutableStateOf<RuleType?>(null) }
    var selectedRuleToPause by remember { mutableStateOf<MonitorRule?>(null) }
    var selectedGroupToSchedule by remember { mutableStateOf<ProtectionGroup?>(null) }
    var selectedRuleToSchedule by remember { mutableStateOf<MonitorRule?>(null) }
    
    var pendingAlertType by remember { mutableStateOf(ActivityAlertType.PANIC) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isServiceRunning = protectedViewModel.isProtectionActive

    val globalScheduleStatus = TimeUtils.calculateStatus(
        isEnabled = true, pausedUntil = globalPausedUntil, isScheduleEnabled = globalSchedule.isEnabled,
        days = globalSchedule.days, start = globalSchedule.start, end = globalSchedule.end, now = currentTime
    )

    // [SINCRONIZAÇÃO DE TEMPO]
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val activity = LocalContext.current as? android.app.Activity
    var alertInitialSeconds by remember { mutableIntStateOf(10) } 
    var alertDetails by remember { mutableStateOf<String?>(null) } // [NOVO]
    var alertInfractions by remember { mutableStateOf<List<InfractionDisplayInfo>>(emptyList()) } // [NOVO]

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                // Executa sempre que a app abre ou volta do background
                if (activity?.intent?.action == "ACTION_SHOW_ALERT") {
                    val typeStr = activity.intent.getStringExtra("ALERT_TYPE")
                    alertDetails = activity.intent.getStringExtra("ALERT_DETAILS") // [NOVO]

                    // [NOVO] Extração das infrações
                    val infractionsStr = activity.intent.getStringExtra("ALERT_INFRACTIONS")
                    if (infractionsStr != null) {
                         alertInfractions = infractionsStr.split(";").mapNotNull { 
                             val parts = it.split("|")
                             if (parts.size >= 3) {
                                 val param = if(parts.size >= 4) parts[3].ifEmpty { null } else null
                                 InfractionDisplayInfo(parts[0], parts[1], parts[2], param)
                             } else null
                         }
                    } else {
                        alertInfractions = emptyList()
                    }

                    // Define o tipo correto (Queda/Acidente) vindo do Serviço
                    pendingAlertType = if (typeStr != null) {
                        try { ActivityAlertType.valueOf(typeStr) } catch (e: Exception) { ActivityAlertType.PANIC }
                    } else {
                        ActivityAlertType.PANIC
                    }

                    // [NOVO] Sincronização de Tempo
                    val timestamp = activity.intent.getLongExtra("ALERT_TIMESTAMP", -1L)
                    if (timestamp != -1L) {
                        val elapsed = (System.currentTimeMillis() - timestamp) / 1000
                        val remaining = (10 - elapsed).toInt()
                        alertInitialSeconds = if (remaining > 0) remaining else 0
                    } else {
                        alertInitialSeconds = 10
                    }

                    showAlertScreen = true
                    activity.intent.action = null // Limpa para não repetir
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Fix: Prioritize PAUSED over SCHEDULED
    val finalSystemLabel = if (!isServiceRunning) "OFFLINE"
    else if (globalScheduleStatus.label == "PAUSED") "PAUSED"
    else if (globalScheduleStatus.label == "SCHEDULED") "SCHEDULED"
    else "ACTIVE"

    if (showAlertScreen) {
        Dialog(
            onDismissRequest = { /* Bloquear dismiss acidental */ },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = false, // Obriga a interagir
                dismissOnClickOutside = false
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AlertScreen(
                    initialSeconds = alertInitialSeconds, 
                    alertType = pendingAlertType.name,
                    alertDetails = alertDetails, 
                    activeGeofences = activeGeofences, // [NOVO]
                    infractions = alertInfractions, // [NOVO]
                    onCancel = {
                        showAlertScreen = false
                        (context as? pt.isec.amov2526.projetoKotlin.ui.MainActivity)?.setShowOnLockScreen(false)
                               },
                    onConfirm = {
                        // 1. Enviar Alerta Manualmente
                        viewModel.sendAlert(pendingAlertType)

                        // 2. Avisar Serviço: "Pára a contagem e limpa a notificação!"
                        val intent = android.content.Intent(context, pt.isec.amov2526.projetoKotlin.services.SafetyMonitorService::class.java).apply {        
                            action = pt.isec.amov2526.projetoKotlin.services.SafetyMonitorService.ACTION_CANCEL_ALERT
                        }
                        context.startService(intent)

                        showAlertScreen = false

                        (context as? pt.isec.amov2526.projetoKotlin.ui.MainActivity)?.setShowOnLockScreen(false)
                    },
                    onValidatePin = { pin ->
                        scope.launch {
                            if (viewModel.verifyPin(pin)) {
                                // 1. Fechar UI
                                showAlertScreen = false

                                // 2. Avisar Serviço: "Cancelado com sucesso (PIN OK), pára tudo"
                                val intent = android.content.Intent(context, pt.isec.amov2526.projetoKotlin.services.SafetyMonitorService::class.java).apply {
                                    action = pt.isec.amov2526.projetoKotlin.services.SafetyMonitorService.ACTION_CANCEL_ALERT
                                }
                                context.startService(intent)

                                (context as? pt.isec.amov2526.projetoKotlin.ui.MainActivity)?.setShowOnLockScreen(false)

                                // 3. [CRÍTICO] Se estava bloqueado, fecha a Activity para voltar ao Lockscreen seguro
                                if (keyguardManager.isDeviceLocked) {
                                    activity?.finish()
                                }
                            } else {
                                snackbarHostState.showSnackbar("Invalid PIN")
                            }
                        }
                        true
                    },
                    currentLocation = currentLocation
                )
            }
        }
    }

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
                finalSystemLabel = finalSystemLabel,
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
                pendingAlertType = ActivityAlertType.PANIC
                alertInitialSeconds = 10 // [NOVO] SOS Manual sempre começa do zero
                showAlertScreen = true
                onPanic()
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

    // 3. Modal de Agendamento Global (Botão PLAN) - ESTAVA EM FALTA
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