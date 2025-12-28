package pt.isec.amov2526.projetoKotlin.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import pt.isec.amov2526.projetoKotlin.ui.screens.monitor.MonitorDashboardScreen
import pt.isec.amov2526.projetoKotlin.ui.screens.monitor.MonitorMainScreen
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.ProtectedMainScreen
import pt.isec.amov2526.projetoKotlin.ui.theme.ProjetoKotlinTheme

@Composable
fun MainScreen() {
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

                    // Obtemos o estado do ViewModel (que é injetado automaticamente)
                    val viewModel: pt.isec.amov2526.projetoKotlin.ui.viewmodels.AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                    val userRole = viewModel.userRole
                    val user = viewModel.user

                    // Se já houver User e Role carregados, salta para a homepage
                    LaunchedEffect(key1 = user, key2 = userRole) {
                        if (user != null && userRole != null) {
                            val destination = if (userRole == "MONITOR") "monitor" else "protected"
                            navController.navigate(destination) {
                                popUpTo("landing") { inclusive = true }
                            }
                        }
                    }


                    pt.isec.amov2526.projetoKotlin.ui.screens.auth.LandingScreen(
                        onLoginClick = { navController.navigate("login") },
                        onRegisterClick = { navController.navigate("register") }
                    )
                }

                composable("login") {
                    pt.isec.amov2526.projetoKotlin.ui.screens.auth.LoginScreen(
                        onBack = { navController.popBackStack() },
                        onLogin = { role ->
                            val destination = if(role == "MONITOR") "monitor" else "protected"
                            navController.navigate(destination) {
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
                        onSignUp = { role ->
                            val destination = if (role == "MONITOR") "monitor" else "protected"

                            navController.navigate(destination) {
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
                        onBack = { navController.popBackStack() }
                    )
                }

                // --- PERFIL PROTEGIDO ---
                composable("protected") {
                    ProtectedMainScreen(
                        onLogout = {
                            navController.navigate("landing") {
                                popUpTo("protected") { inclusive = true }
                            }
                        }
                    )
                }

                // --- PERFIL MONITOR ---
                composable("monitor") {
                    MonitorMainScreen(
                        onLogout = {
                            navController.navigate("landing") {
                                popUpTo("monitor") { inclusive = true }
                            }
                        },
                        onProtegeClick = { protegeName, protegeUid ->
                            navController.navigate("monitor/dashboard/$protegeUid/$protegeName")
                        }
                    )
                }

                // --- DETALHE DO MONITOR (DASHBOARD) ---
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