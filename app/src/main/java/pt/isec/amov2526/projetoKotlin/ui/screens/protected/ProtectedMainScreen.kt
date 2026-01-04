package pt.isec.amov2526.projetoKotlin.ui.screens.protected

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import pt.isec.amov2526.projetoKotlin.R
import pt.isec.amov2526.projetoKotlin.data.model.ActivityAlertType
import pt.isec.amov2526.projetoKotlin.data.model.InfractionDisplayInfo
import pt.isec.amov2526.projetoKotlin.services.SafetyMonitorService
import pt.isec.amov2526.projetoKotlin.ui.MainActivity
import pt.isec.amov2526.projetoKotlin.ui.components.EditProfileView
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.ProtectedBottomBar
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.ProtectedTopBar
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.AuthViewModel
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.GuardViewModel
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.GuardViewModelFactory
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.ProtectedMainViewModel
import pt.isec.amov2526.projetoKotlin.utils.LocationHelper

/**
 * Estrutura para armazenar alertas que chegam enquanto outro está a ser exibido.
 */
data class PendingAlert(
    val type: ActivityAlertType,
    val details: String?,
    val id: String,
    val infractions: List<InfractionDisplayInfo>,
    val timestamp: Long
)

/**
 * Container principal do modo "Protegido" (Utilizador monitorizado).
 *
 * Responsável por:
 * 1. Gestão da navegação inferior (Guard, Monitores, Atividade, Perfil).
 * 2. Gestão global de alertas: Escuta Intents de emergência e apresenta o `AlertScreen` em overlay.
 * 3. Fila de alertas: Gere múltiplos alertas consecutivos.
 * 4. Validação de permissões críticas (Sobreposição).
 */
@Composable
fun ProtectedMainScreen(
    navController: NavHostController = rememberNavController(),
    onLogout: () -> Unit = {},
    onSwitchToMonitor: () -> Unit = {},
    viewModel: AuthViewModel = viewModel(),
    protectedViewModel: ProtectedMainViewModel = viewModel()
) {
    val context = LocalContext.current
    val keyguardManager =
        context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

    val guardViewModel: GuardViewModel = viewModel(factory = GuardViewModelFactory())

    // Estado Mestre de Alerta: Se true, bloqueia a UI normal e mostra o ecrã de emergência.
    var showAlertScreen by remember { mutableStateOf(false) }


    LaunchedEffect(showAlertScreen) {
        viewModel.isUserInEmergency = showAlertScreen
    }

    var isAlertConfirmed by remember { mutableStateOf(false) }
    var pendingAlertType by remember { mutableStateOf(ActivityAlertType.PANIC) }
    val scope = rememberCoroutineScope()

    val alertQueue = remember { mutableStateListOf<PendingAlert>() }

    val locationHelper = remember { LocationHelper(context) }
    val currentLocation by locationHelper.currentLocation.collectAsState(initial = null)
    val activeGeofences by guardViewModel.activeGeofences.collectAsState()

    LaunchedEffect(Unit) {
        guardViewModel.setLocationHelper(locationHelper)
        locationHelper.startLocationUpdates(2000L)
    }

    LaunchedEffect(viewModel.user) {
        if (viewModel.user == null) {
            // Cleanup ao sair
            if (protectedViewModel.isProtectionActive) protectedViewModel.toggleProtection()
            onLogout()
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val activity = LocalContext.current as? Activity
    var alertInitialSeconds by remember { mutableIntStateOf(10) }
    var alertDetails by remember { mutableStateOf<String?>(null) }
    var alertId by remember { mutableStateOf<String?>(null) }
    var alertInfractions by remember { mutableStateOf<List<InfractionDisplayInfo>>(emptyList()) }
    val invalidPinError = stringResource(R.string.error_invalid_pin)

    /**
     * Processa o próximo alerta na fila ou encerra o modo de emergência se a fila estiver vazia.
     */
    fun processNextAlert() {
        if (alertQueue.isNotEmpty()) {
            val next = alertQueue.removeAt(0)
            pendingAlertType = next.type
            alertDetails = next.details
            alertId = next.id
            alertInfractions = next.infractions

            // Ajusta o tempo restante baseando-se no timestamp original do evento.
            val elapsed = (System.currentTimeMillis() - next.timestamp) / 1000
            val remaining = (10 - elapsed).toInt()
            alertInitialSeconds = if (remaining > 0) remaining else 0

            isAlertConfirmed = false
            showAlertScreen = true
        } else {
            // Fim da sequência de alertas
            showAlertScreen = false
            alertId = null

            // Notifica o serviço que a sequência terminou
            val intent = Intent(
                context,
                SafetyMonitorService::class.java
            ).apply {
                action =
                    SafetyMonitorService.ACTION_FINISH_ALERT
            }
            context.startService(intent)

            (context as? MainActivity)?.setShowOnLockScreen(false)
        }
    }

    // Escuta Intents de Alerta enviados pelo SafetyMonitorService.
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (activity?.intent?.action == "ACTION_SHOW_ALERT") {
                    val typeStr = activity.intent.getStringExtra("ALERT_TYPE")
                    val details = activity.intent.getStringExtra("ALERT_DETAILS")
                    val incomingId = activity.intent.getStringExtra("ALERT_ID")
                    val timestamp = activity.intent.getLongExtra("ALERT_TIMESTAMP", -1L)

                    // Deserialização manual da lista de infrações
                    val infractionsStr = activity.intent.getStringExtra("ALERT_INFRACTIONS")
                    val infractionsList = if (infractionsStr != null) {
                        infractionsStr.split(";").mapNotNull {
                            val parts = it.split("|")
                            if (parts.size >= 3) {
                                val param = if (parts.size >= 4) parts[3].ifEmpty { null } else null
                                val monId = if (parts.size >= 5) parts[4].ifEmpty { null } else null
                                InfractionDisplayInfo(parts[0], parts[1], parts[2], param, monId)
                            } else null
                        }
                    } else {
                        emptyList()
                    }

                    val type = if (typeStr != null) {
                        try {
                            ActivityAlertType.valueOf(typeStr)
                        } catch (e: Exception) {
                            ActivityAlertType.PANIC
                        }
                    } else {
                        ActivityAlertType.PANIC
                    }

                    val safeId = incomingId ?: java.util.UUID.randomUUID().toString()

                    if (showAlertScreen) {
                        // Se já estamos num alerta, adiciona à fila.
                        alertQueue.add(
                            PendingAlert(
                                type,
                                details,
                                safeId,
                                infractionsList,
                                timestamp
                            )
                        )
                        Log.d(
                            "ProtectedMainScreen",
                            "Alert enqueued. Queue size: ${alertQueue.size}"
                        )

                        // Se o alerta atual já foi confirmado (vídeo a gravar), não interrompe.
                        // Se ainda está em contagem, a fila será processada sequencialmente.
                        if (isAlertConfirmed) {
                            processNextAlert()
                        }
                    } else {
                        // Inicia novo alerta imediato
                        pendingAlertType = type
                        alertDetails = details
                        alertId = safeId
                        alertInfractions = infractionsList

                        if (timestamp != -1L) {
                            val elapsed = (System.currentTimeMillis() - timestamp) / 1000
                            val remaining = (10 - elapsed).toInt()
                            alertInitialSeconds = if (remaining > 0) remaining else 0
                        } else {
                            alertInitialSeconds = 10
                        }

                        isAlertConfirmed = false
                        showAlertScreen = true
                    }

                    activity.intent.action = null
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showAlertScreen) {
        Log.d(
            "ProtectedMainScreen",
            "Showing AlertScreen. Active Geofences Count: ${activeGeofences.size}"
        )
        // Dialog FullScreen que bloqueia navegação para trás
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                key(alertId) {
                    AlertScreen(
                        alertType = pendingAlertType.name,
                        alertDetails = alertDetails,
                        alertIdFromIntent = alertId,
                        currentLocation = currentLocation,
                        activeGeofences = activeGeofences,
                        infractions = alertInfractions,
                        initialSeconds = alertInitialSeconds,
                        isConfirmedState = isAlertConfirmed,
                        onCancel = { processNextAlert() },
                        onConfirm = {
                            isAlertConfirmed = true
                            // Envia sinal ao backend
                            val targetMonitors =
                                alertInfractions.mapNotNull { it.monitorId }.distinct()
                                    .ifEmpty { null }
                            guardViewModel.sendAlert(pendingAlertType, alertId, targetMonitors) { }
                            
                            // Para o temporizador de serviço
                            val intent = Intent(
                                context,
                                SafetyMonitorService::class.java
                            ).apply {
                                action =
                                    SafetyMonitorService.ACTION_STOP_TIMER
                            }
                            context.startService(intent)
                        },
                        onValidatePin = { pin ->
                            scope.launch {
                                val isValid = guardViewModel.verifyPin(pin)
                                if (isValid) {
                                    // PIN válido: Cancela alerta
                                    val intent = Intent(
                                        context,
                                        SafetyMonitorService::class.java
                                    ).apply {

                                        action =
                                            SafetyMonitorService.ACTION_FINISH_ALERT
                                    }
                                    context.startService(intent)
                                    // Se estava sobre o Lockscreen, fecha para o utilizador poder usar o telemóvel
                                    if (keyguardManager.isDeviceLocked) activity?.finish()
                                    processNextAlert()
                                } else {
                                    Toast.makeText(context, invalidPinError, Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                            false
                        }
                    )
                }
            }
        }
    }

    var isEditingProfile by remember { mutableStateOf(false) }
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }
    var showEnableMonitorDialog by remember { mutableStateOf(false) }
    val currentUserData = viewModel.currentUserData

    // Verificação de permissões de sobreposição ao iniciar.
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(context)
        ) {
            showOverlayPermissionDialog = true
        }
    }

    if (showEnableMonitorDialog) {
        AlertDialog(
            onDismissRequest = { showEnableMonitorDialog = false },
            title = { Text(stringResource(id = R.string.dual_role_title)) },
            text = { Text(stringResource(id = R.string.dual_role_desc)) },
            confirmButton = {
                Button(onClick = {
                    showEnableMonitorDialog = false
                    viewModel.enableDualRole {
                        viewModel.switchViewMode()
                    }
                }) {
                    Text(stringResource(id = R.string.dual_role_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEnableMonitorDialog = false }) {
                    Text(stringResource(id = R.string.action_cancel))
                }
            }
        )
    }

    if (showOverlayPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showOverlayPermissionDialog = false },
            title = { Text(stringResource(id = R.string.dialog_perm_title)) },
            text = { Text(stringResource(id = R.string.dialog_perm_desc)) },
            confirmButton = {
                Button(onClick = {
                    showOverlayPermissionDialog = false
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }) {
                    Text(stringResource(id = R.string.dialog_perm_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverlayPermissionDialog = false }) {
                    Text(stringResource(id = R.string.dialog_perm_dismiss))
                }
            }
        )
    }

    if (isEditingProfile) {
        EditProfileView(
            currentName = currentUserData?.name ?: viewModel.userName,
            currentEmail = currentUserData?.email ?: viewModel.userEmail,
            currentPin = currentUserData?.pin,
            currentMfaEnabled = currentUserData?.mfaEnabled ?: false,
            onDismiss = { isEditingProfile = false },
            onSave = { name, email, pass, pin, mfa, currentPass ->
                viewModel.updateProfile(
                    name, email, pass, pin, mfa, currentPass,
                    onSuccess = { isEditingProfile = false },
                    onError = { }
                )
            }
        )
    } else {
        Scaffold(
            topBar = {
                ProtectedTopBar(onProfileClick = {
                    navController.navigate("profile") {
                        launchSingleTop = true
                    }
                })
            },
            bottomBar = { ProtectedBottomBar(navController = navController) }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "guard",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("guard") {
                    GuardScreen(
                        viewModel = guardViewModel,
                        protectedViewModel = protectedViewModel,
                        onPanic = {
                            // Inicia Alerta de Pânico Manual
                            pendingAlertType = ActivityAlertType.PANIC
                            alertId = java.util.UUID.randomUUID().toString()
                            alertInitialSeconds = 10
                            alertDetails = null
                            alertInfractions = emptyList()
                            isAlertConfirmed = false
                            showAlertScreen = true

                            val intent = Intent(
                                context,
                                SafetyMonitorService::class.java
                            ).apply {
                                action =
                                    SafetyMonitorService.ACTION_START_ALERT
                            }
                            context.startService(intent)
                        }
                    )
                }
                composable("monitors") { MonitorsScreen() }
                composable("activity") {
                    ActivityScreen(
                        activeGeofences = activeGeofences
                    )
                }
                composable("profile") {
                    ProfileScreen(
                        name = viewModel.userName, email = viewModel.userEmail,
                        onEditProfile = { isEditingProfile = true },
                        onLogout = {
                            // Logout Seguro: Garante estado OFFLINE antes de sair.
                            val uid = viewModel.user?.uid
                            viewModel.viewModelScope.launch {
                                try {
                                    if (uid != null) FirebaseFirestore.getInstance()
                                        .collection("users").document(uid)
                                        .update("systemStatus", "OFFLINE").await()
                                } catch (_: Exception) {
                                }
                                if (protectedViewModel.isProtectionActive) protectedViewModel.toggleProtection()
                                viewModel.logout()
                            }
                        },
                        onSwitchMode = {
                            if (viewModel.canSwitchProfile) {
                                viewModel.switchViewMode()
                            } else {
                                showEnableMonitorDialog = true
                            }
                        },
                        canSwitchProfile = viewModel.canSwitchProfile
                    )
                }
            }
        }
    }
}