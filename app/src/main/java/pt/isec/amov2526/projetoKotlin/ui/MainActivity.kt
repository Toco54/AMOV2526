package pt.isec.amov2526.projetoKotlin.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import pt.isec.amov2526.projetoKotlin.ui.components.EditProfileView
import pt.isec.amov2526.projetoKotlin.ui.screens.monitor.MonitorDashboardScreen
import pt.isec.amov2526.projetoKotlin.ui.screens.monitor.MonitorMainScreen
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.ProtectedMainScreen
import pt.isec.amov2526.projetoKotlin.ui.theme.ProjetoKotlinTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProjetoKotlinTheme {
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = "landing"
                    ) {
                        // --- AUTHENTICATION ---
                        composable("landing") {
                            pt.isec.amov2526.projetoKotlin.ui.screens.auth.LandingScreen(
                                onLoginClick = { navController.navigate("login") },
                                onRegisterClick = { navController.navigate("register") }
                            )
                        }

                        composable("login") {
                            pt.isec.amov2526.projetoKotlin.ui.screens.auth.LoginScreen(
                                onBack = { navController.popBackStack() },
                                onLogin = {
                                    // TODO: Implement actual login logic
                                    navController.navigate("protected") {
                                        popUpTo("landing") { inclusive = true }
                                    }
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
                            pt.isec.amov2526.projetoKotlin.ui.screens.auth.RegisterScreen(
                                onBack = { navController.popBackStack() },
                                onSignUp = {
                                    // TODO: Implement actual signup logic
                                    navController.navigate("protected") {
                                        popUpTo("landing") { inclusive = true }
                                    }
                                },
                                onLoginClick = {
                                    navController.navigate("login") {
                                        popUpTo("landing") { inclusive = false }
                                    }
                                }
                            )
                        }

                        composable("forgot_password") {
                            pt.isec.amov2526.projetoKotlin.ui.screens.auth.ForgotPasswordScreen(
                                onBack = { navController.popBackStack() },
                                onSendOtp = {
                                    // TODO: Implement OTP logic
                                    navController.popBackStack()
                                }
                            )
                        }

                        // --- PERFIL PROTEGIDO ---
                        composable("protected") {
                            ProtectedMainScreen(
                                onLogout = {
                                    navController.navigate("monitor") {
                                        popUpTo("protected") { inclusive = true }
                                    }
                                },
                                onEditProfile = {
                                    navController.navigate("edit_profile/protected")
                                }
                            )
                        }

                        // --- PERFIL MONITOR ---
                        composable("monitor") {
                            MonitorMainScreen(
                                onLogout = {
                                    navController.navigate("protected") {
                                        popUpTo("monitor") { inclusive = true }
                                    }
                                },
                                onProtegeClick = { protegeName ->
                                    navController.navigate("monitor/dashboard/$protegeName")
                                },
                                onEditProfile = {
                                    navController.navigate("edit_profile/monitor")
                                }
                            )
                        }

                        // --- DETALHE DO MONITOR (DASHBOARD) ---
                        composable(
                            route = "monitor/dashboard/{name}",
                            arguments = listOf(
                                navArgument("name") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val name = backStackEntry.arguments?.getString("name") ?: "Unknown"
                            MonitorDashboardScreen(
                                protegeName = name,
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // --- EDIT PROFILE (FULLSCREEN) ---
                        composable(
                            route = "edit_profile/{role}",
                            arguments = listOf(
                                navArgument("role") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val role = backStackEntry.arguments?.getString("role") ?: "protected"
                            val isMonitor = role == "monitor"

                            // Dados Mockados
                            val currentName = if (isMonitor) "Alice Monitor" else "Bob Protected"
                            val currentEmail = if (isMonitor) "alice@example.com" else "bob@example.com"
                            val currentPin = if (isMonitor) null else "1234"

                            EditProfileView(
                                currentName = currentName,
                                currentEmail = currentEmail,
                                currentPin = currentPin,
                                onDismiss = {
                                    navController.popBackStack()
                                },
                                onSave = { _, _, _, _ ->
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
