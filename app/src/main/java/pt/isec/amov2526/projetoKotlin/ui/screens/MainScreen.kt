package pt.isec.amov2526.projetoKotlin.ui.screens

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pt.isec.amov2526.projetoKotlin.ui.screens.auth.ForgotPasswordScreen
import pt.isec.amov2526.projetoKotlin.ui.screens.auth.LandingScreen
import pt.isec.amov2526.projetoKotlin.ui.screens.auth.LoginScreen
import pt.isec.amov2526.projetoKotlin.ui.screens.auth.RegisterScreen
import pt.isec.amov2526.projetoKotlin.ui.screens.monitor.MonitorDashboardScreen
import pt.isec.amov2526.projetoKotlin.ui.screens.monitor.MonitorMainScreen
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.ProtectedMainScreen
import pt.isec.amov2526.projetoKotlin.ui.theme.ProjetoKotlinTheme
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.AuthViewModel
import pt.isec.amov2526.projetoKotlin.services.MonitorNotificationService

@Composable
fun MainScreen() {
    ProjetoKotlinTheme {
        val navController = rememberNavController()


        val authViewModel: AuthViewModel = viewModel()


        val currentViewMode = authViewModel.currentViewMode
        val user = authViewModel.user


        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current


        LaunchedEffect(user) {
            val intent = Intent(
                context,
                MonitorNotificationService::class.java
            )
            if (user != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } else {
                context.stopService(intent)
            }
        }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    val activity = context as? Activity
                    val intent = activity?.intent

                    if (intent?.action == "ACTION_SHOW_ALERT") {

                        if (authViewModel.currentViewMode != "PROTECTED") {
                            authViewModel.currentViewMode = "PROTECTED"
                        }
                    } else if (intent?.action == "ACTION_MONITOR_ALERT") {

                        val targetUid = intent.getStringExtra("TARGET_PROTEGE_ID")
                        val targetName = intent.getStringExtra("TARGET_PROTEGE_NAME")


                        if (targetUid != null && targetName != null && !authViewModel.isUserInEmergency) {
                            if (authViewModel.currentViewMode != "MONITOR") {
                                authViewModel.currentViewMode = "MONITOR"
                            }



                            navController.navigate("monitor") {
                                popUpTo("landing") { inclusive = true }
                            }

                            navController.navigate("monitor/dashboard/$targetUid/$targetName") {
                                launchSingleTop = true
                            }


                            intent.action = null
                        }
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }


        LaunchedEffect(user, currentViewMode) {
            if (user != null && currentViewMode != null) {
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                val activity = context as? Activity



                if (activity?.intent?.action == "ACTION_MONITOR_ALERT") return@LaunchedEffect

                if (currentViewMode == "MONITOR") {

                    if (currentRoute?.startsWith("monitor") != true) {
                        navController.navigate("monitor") {
                            popUpTo("landing") { inclusive = true }
                        }
                    }
                } else {

                    if (currentRoute != "protected") {
                        navController.navigate("protected") {
                            popUpTo("landing") { inclusive = true }
                        }
                    }
                }
            } else if (user == null) {

                if (navController.currentBackStackEntry?.destination?.route != "landing") {
                    navController.navigate("landing") {
                        popUpTo(0)
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            NavHost(
                navController = navController,
                startDestination = "landing"
            ) {

                composable("landing") {
                    LandingScreen(


                        onLoginClick = { navController.navigate("login") },
                        onRegisterClick = { navController.navigate("register") }
                    )
                }

                composable("login") {

                    LoginScreen(
                        viewModel = authViewModel,
                        onBack = { navController.popBackStack() },
                        onLogin = { _ ->

                        },
                        onForgotPassword = { navController.navigate("forgot_password") },
                        onSignUpClick = {
                            navController.navigate("register") {
                                popUpTo("landing") { inclusive = false }
                            }
                        }
                    )
                }

                composable("register") {

                    RegisterScreen(
                        viewModel = authViewModel,
                        onBack = { navController.popBackStack() },
                        onSignUp = { _ ->

                        },
                        onLoginClick = {
                            navController.navigate("login") {
                                popUpTo("landing") { inclusive = false }
                            }
                        }
                    )
                }

                composable("forgot_password") {
                    ForgotPasswordScreen(
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }


                composable("protected") {

                    ProtectedMainScreen(
                        viewModel = authViewModel,
                        onLogout = {
                            authViewModel.logout()

                        }
                    )
                }


                composable("monitor") {

                    MonitorMainScreen(
                        viewModel = authViewModel,
                        onLogout = {
                            authViewModel.logout()
                        },
                        onProtegeClick = { protegeName, protegeUid ->
                            navController.navigate("monitor/dashboard/$protegeUid/$protegeName")
                        }
                    )
                }


                composable(
                    route = "monitor/dashboard/{uid}/{name}",
                    arguments = listOf(
                        navArgument("uid") { type = NavType.StringType },
                        navArgument("name") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val uid = backStackEntry.arguments?.getString("uid") ?: ""
                    val name = backStackEntry.arguments?.getString("name") ?: "Unknown"

                    MonitorDashboardScreen(
                        protegeUid = uid,
                        protegeName = name,
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}