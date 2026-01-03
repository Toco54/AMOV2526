package pt.isec.amov2526.projetoKotlin.ui.screens.protected

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import pt.isec.amov2526.projetoKotlin.data.model.ActivityAlertType
import pt.isec.amov2526.projetoKotlin.ui.components.EditProfileView
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.ProtectedBottomBar
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.ProtectedTopBar
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.AuthViewModel
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.GuardViewModel
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.GuardViewModelFactory
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.ProtectedMainViewModel
import pt.isec.amov2526.projetoKotlin.utils.LocationHelper

@Composable
fun ProtectedMainScreen(
    navController: NavHostController = rememberNavController(),
    onLogout: () -> Unit = {},
    onSwitchToMonitor: () -> Unit = {},
    viewModel: AuthViewModel = viewModel(),
    protectedViewModel: ProtectedMainViewModel = viewModel()
) {
    val context = LocalContext.current
    val keyguardManager = context.getSystemService(android.content.Context.KEYGUARD_SERVICE) as android.app.KeyguardManager

    // --- VIEWMODEL PARTILHADO ---
    // Instanciamos aqui para partilhar o mesmo LocationHelper com o GuardScreen
    val guardViewModel: GuardViewModel = viewModel(factory = GuardViewModelFactory())

    // --- ESTADOS GLOBAIS DE ALERTA ---
    var showAlertScreen by remember { mutableStateOf(false) }
    var pendingAlertType by remember { mutableStateOf(ActivityAlertType.PANIC) }
    val scope = rememberCoroutineScope()

    // Location Helper Global
    val locationHelper = remember { LocationHelper(context) }
    val currentLocation by locationHelper.currentLocation.collectAsState(initial = null)
    val activeGeofences by guardViewModel.activeGeofences.collectAsState() // [NOVO]

    LaunchedEffect(Unit) {
        guardViewModel.setLocationHelper(locationHelper)
        locationHelper.startLocationUpdates(2000L)
    }

    // Listener de Logout
    LaunchedEffect(viewModel.user) {
        if (viewModel.user == null) {
            if (protectedViewModel.isProtectionActive) protectedViewModel.toggleProtection()
            onLogout()
        }
    }

    // --- DETETOR DE ALERTAS (INTENTS) ---
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val activity = LocalContext.current as? android.app.Activity
    var alertInitialSeconds by remember { mutableIntStateOf(10) } // [NOVO]
    var alertDetails by remember { mutableStateOf<String?>(null) } // [NOVO]
    var alertInfractions by remember { mutableStateOf<List<InfractionDisplayInfo>>(emptyList()) } // [NOVO]

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
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

                    pendingAlertType = if (typeStr != null) {
                        try { ActivityAlertType.valueOf(typeStr) }
                        catch (e: Exception) { ActivityAlertType.PANIC }
                    } else { ActivityAlertType.PANIC }

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
                    activity.intent.action = null
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // --- O POPUP DE ALERTA GLOBAL ---
    if (showAlertScreen) {
        android.util.Log.d("ProtectedMainScreen", "Showing AlertScreen. Active Geofences Count: ${activeGeofences.size}")
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AlertScreen(
                    alertType = pendingAlertType.name, 
                    alertDetails = alertDetails, 
                    currentLocation = currentLocation,
                    activeGeofences = activeGeofences, // [NOVO]
                    infractions = alertInfractions, // [NOVO]
                    initialSeconds = alertInitialSeconds, 
                    onCancel = {
                        // Cancelar ANTES de confirmar: Fecha tudo
                        showAlertScreen = false
                        (context as? pt.isec.amov2526.projetoKotlin.ui.MainActivity)?.setShowOnLockScreen(false)
                    },
                    onConfirm = {
                        // Confirmar (SOS enviado):
                        // 1. Envia para a Firebase
                        guardViewModel.sendAlert(pendingAlertType)

                        // 2. Avisa o Serviço para parar a contagem
                        val intent = android.content.Intent(context, pt.isec.amov2526.projetoKotlin.services.SafetyMonitorService::class.java).apply {
                            action = pt.isec.amov2526.projetoKotlin.services.SafetyMonitorService.ACTION_CANCEL_ALERT
                        }
                        context.startService(intent)

                        // [CORREÇÃO] NÃO FECHAMOS A JANELA AQUI!
                        // Mantemos 'showAlertScreen = true' para mostrar o Mapa e Câmaras (ConfirmedPanel).
                        // Também NÃO desligamos o 'setShowOnLockScreen' para manter o ecrã acordado na emergência.
                    },
                    onValidatePin = { pin ->
                        scope.launch {
                            val isValid = guardViewModel.verifyPin(pin)
                            if (isValid) {
                                // PIN Válido: Cancela e fecha tudo
                                showAlertScreen = false

                                val intent = android.content.Intent(context, pt.isec.amov2526.projetoKotlin.services.SafetyMonitorService::class.java).apply {
                                    action = pt.isec.amov2526.projetoKotlin.services.SafetyMonitorService.ACTION_CANCEL_ALERT
                                }
                                context.startService(intent)

                                (context as? pt.isec.amov2526.projetoKotlin.ui.MainActivity)?.setShowOnLockScreen(false)

                                if (keyguardManager.isDeviceLocked) activity?.finish()
                            } else {
                                Toast.makeText(context, "Invalid PIN", Toast.LENGTH_SHORT).show()
                            }
                        }
                        false // Retorna false para impedir fecho automático pelo componente
                    }
                )
            }
        }
    }

    // --- UI NORMAL DA APP ---
    var isEditingProfile by remember { mutableStateOf(false) }
    var showGoOnlineDialog by remember { mutableStateOf(false) }
    var showOverlayPermissionDialog by remember { mutableStateOf(false) } // [NOVO] Dialogo para permissão de Overlay
    val currentUserData = viewModel.currentUserData

    // Verificar permissoes ao iniciar
    LaunchedEffect(Unit) {
        if (!protectedViewModel.isProtectionActive) showGoOnlineDialog = true

        // [NOVO] Verifica se tem permissão para sobrepor outras apps (Android 10+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
            !android.provider.Settings.canDrawOverlays(context)) {
            showOverlayPermissionDialog = true
        }
    }

    // [NOVO] Dialogo para pedir permissão de sobreposição
    if (showOverlayPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showOverlayPermissionDialog = false },
            title = { Text("Permissão Necessária") },
            text = { Text("Para garantir que os Alertas de Emergência abrem imediatamente sobre outras apps (Facebook, WhatsApp, etc.), precisa de autorizar a sobreposição.") },
            confirmButton = {
                Button(onClick = {
                    showOverlayPermissionDialog = false
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:${context.packageName}")
                    )
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }) {
                    Text("Autorizar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverlayPermissionDialog = false }) {
                    Text("Agora não")
                }
            }
        )
    }

    if (showGoOnlineDialog) {
        AlertDialog(
            onDismissRequest = { showGoOnlineDialog = false },
            title = { Text("Activate Protection?") },
            text = { Text("Do you want to go ONLINE and start monitoring your safety now?") },
            confirmButton = {
                Button(onClick = { protectedViewModel.toggleProtection(); showGoOnlineDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF10B981))) {
                    Text("Go Online")
                }
            },
            dismissButton = { TextButton(onClick = { showGoOnlineDialog = false }) { Text("Stay Offline") } }
        )
    }

    if (isEditingProfile) {
        EditProfileView(
            currentName = currentUserData?.name ?: viewModel.userName,
            currentEmail = currentUserData?.email ?: viewModel.userEmail,
            currentPin = currentUserData?.pin,
            onDismiss = { isEditingProfile = false },
            onSave = { name, email, pass, pin, currentPass ->
                viewModel.updateProfile(name, email, pass, pin, currentPass,
                    onSuccess = { isEditingProfile = false },
                    onError = { }
                )
            }
        )
    } else {
        Scaffold(
            topBar = { ProtectedTopBar(onProfileClick = { navController.navigate("profile") { launchSingleTop = true } }) },
            bottomBar = { ProtectedBottomBar(navController = navController) }
        ) { innerPadding ->
            NavHost(navController = navController, startDestination = "guard", modifier = Modifier.padding(innerPadding)) {
                composable("guard") {
                    // Passamos o ViewModel partilhado e o callback de Pânico
                    GuardScreen(
                        viewModel = guardViewModel,
                        protectedViewModel = protectedViewModel,
                        onPanic = {
                            pendingAlertType = ActivityAlertType.PANIC
                            showAlertScreen = true
                        }
                    )
                }
                composable("monitors") { MonitorsScreen() }
                composable("activity") { 
                    ActivityScreen(
                        activeGeofences = activeGeofences // [NOVO] Passa as regras de geofence para o ecrã de histórico
                    ) 
                }
                composable("profile") {
                    ProfileScreen(
                        name = viewModel.userName, email = viewModel.userEmail,
                        onEditProfile = { isEditingProfile = true },
                        onLogout = {
                            val uid = viewModel.user?.uid
                            viewModel.viewModelScope.launch {
                                try { if (uid != null) FirebaseFirestore.getInstance().collection("users").document(uid).update("systemStatus", "OFFLINE").await() } catch (_: Exception) {}
                                if (protectedViewModel.isProtectionActive) protectedViewModel.toggleProtection()
                                viewModel.logout()
                            }
                        }
                    )
                }
            }
        }
    }
}