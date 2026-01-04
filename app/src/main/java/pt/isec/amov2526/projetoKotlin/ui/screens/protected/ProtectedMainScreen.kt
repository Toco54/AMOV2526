package pt.isec.amov2526.projetoKotlin.ui.screens.protected

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
import pt.isec.amov2526.projetoKotlin.ui.components.EditProfileView
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.ProtectedBottomBar
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.ProtectedTopBar
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.AuthViewModel
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.GuardViewModel
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.GuardViewModelFactory
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.ProtectedMainViewModel
import pt.isec.amov2526.projetoKotlin.utils.LocationHelper

data class PendingAlert(
    val type: ActivityAlertType,
    val details: String?,
    val id: String,
    val infractions: List<InfractionDisplayInfo>,
    val timestamp: Long
)

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
        context.getSystemService(android.content.Context.KEYGUARD_SERVICE) as android.app.KeyguardManager

    val guardViewModel: GuardViewModel = viewModel(factory = GuardViewModelFactory())

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
            if (protectedViewModel.isProtectionActive) protectedViewModel.toggleProtection()
            onLogout()
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val activity = LocalContext.current as? android.app.Activity
    var alertInitialSeconds by remember { mutableIntStateOf(10) }
    var alertDetails by remember { mutableStateOf<String?>(null) }
    var alertId by remember { mutableStateOf<String?>(null) }
    var alertInfractions by remember { mutableStateOf<List<InfractionDisplayInfo>>(emptyList()) }
    val invalidPinError = stringResource(R.string.error_invalid_pin)

    fun processNextAlert() {
        if (alertQueue.isNotEmpty()) {
            val next = alertQueue.removeAt(0)
            pendingAlertType = next.type
            alertDetails = next.details
            alertId = next.id
            alertInfractions = next.infractions

            val elapsed = (System.currentTimeMillis() - next.timestamp) / 1000
            val remaining = (10 - elapsed).toInt()
            alertInitialSeconds = if (remaining > 0) remaining else 0

            isAlertConfirmed = false
            showAlertScreen = true
        } else {
            showAlertScreen = false
            alertId = null

            val intent = android.content.Intent(
                context,
                pt.isec.amov2526.projetoKotlin.services.SafetyMonitorService::class.java
            ).apply {
                action =
                    pt.isec.amov2526.projetoKotlin.services.SafetyMonitorService.ACTION_FINISH_ALERT
            }
            context.startService(intent)

            (context as? pt.isec.amov2526.projetoKotlin.ui.MainActivity)?.setShowOnLockScreen(false)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (activity?.intent?.action == "ACTION_SHOW_ALERT") {
                    val typeStr = activity.intent.getStringExtra("ALERT_TYPE")
                    val details = activity.intent.getStringExtra("ALERT_DETAILS")
                    val incomingId = activity.intent.getStringExtra("ALERT_ID")
                    val timestamp = activity.intent.getLongExtra("ALERT_TIMESTAMP", -1L)

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
                        alertQueue.add(
                            PendingAlert(
                                type,
                                details,
                                safeId,
                                infractionsList,
                                timestamp
                            )
                        )
                        android.util.Log.d(
                            "ProtectedMainScreen",
                            "Alert enqueued. Queue size: ${alertQueue.size}"
                        )



                        if (isAlertConfirmed) {
                            processNextAlert()
                        }
                    } else {
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
        android.util.Log.d(
            "ProtectedMainScreen",
            "Showing AlertScreen. Active Geofences Count: ${activeGeofences.size}"
        )
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
                            val targetMonitors =
                                alertInfractions.mapNotNull { it.monitorId }.distinct()
                                    .ifEmpty { null }
                            guardViewModel.sendAlert(pendingAlertType, alertId, targetMonitors) { }
                            val intent = android.content.Intent(
                                context,
                                pt.isec.amov2526.projetoKotlin.services.SafetyMonitorService::class.java
                            ).apply {
                                action =
                                    pt.isec.amov2526.projetoKotlin.services.SafetyMonitorService.ACTION_STOP_TIMER
                            }
                            context.startService(intent)
                        },
                        onValidatePin = { pin ->
                            scope.launch {
                                val isValid = guardViewModel.verifyPin(pin)
                                if (isValid) {
                                    val intent = android.content.Intent(
                                        context,
                                        pt.isec.amov2526.projetoKotlin.services.SafetyMonitorService::class.java
                                    ).apply {
                                        action =
                                            pt.isec.amov2526.projetoKotlin.services.SafetyMonitorService.ACTION_FINISH_ALERT
                                    }
                                    context.startService(intent)
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

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
            !android.provider.Settings.canDrawOverlays(context)
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
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:${context.packageName}")
                    )
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
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
                            pendingAlertType = ActivityAlertType.PANIC
                            alertId = java.util.UUID.randomUUID().toString()
                            alertInitialSeconds = 10
                            alertDetails = null
                            alertInfractions = emptyList()
                            isAlertConfirmed = false
                            showAlertScreen = true

                            val intent = android.content.Intent(
                                context,
                                pt.isec.amov2526.projetoKotlin.services.SafetyMonitorService::class.java
                            ).apply {
                                action =
                                    pt.isec.amov2526.projetoKotlin.services.SafetyMonitorService.ACTION_START_ALERT
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
