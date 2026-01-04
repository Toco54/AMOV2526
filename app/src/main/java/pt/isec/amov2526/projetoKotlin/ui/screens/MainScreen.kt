package pt.isec.amov2526.projetoKotlin.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
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

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner

@Composable
fun MainScreen() {
    ProjetoKotlinTheme {
        val navController = rememberNavController()

        // [CORREÇÃO CRÍTICA] Instanciamos o ViewModel AQUI (no topo).
        // Esta instância será passada para todos os ecrãs filhos para garantir consistência.
        val authViewModel: AuthViewModel = viewModel()

        // Observar estado para navegação automática
        val currentViewMode = authViewModel.currentViewMode
        val user = authViewModel.user
        
        // Observador Global de Alertas para Utilizadores "BOTH"
        // Se o utilizador estiver no modo MONITOR e receber um alerta, forçamos a ida para PROTECTED
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        
        // [NOVO] Iniciar Serviço de Notificações do Monitor
        LaunchedEffect(user) {
            val intent = android.content.Intent(context, pt.isec.amov2526.projetoKotlin.services.MonitorNotificationService::class.java)
            if (user != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
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
                    val activity = context as? android.app.Activity
                    val intent = activity?.intent
                    
                    if (intent?.action == "ACTION_SHOW_ALERT") {
                        // Se temos um alerta e não estamos no modo protegido, muda para lá
                        if (authViewModel.currentViewMode != "PROTECTED") {
                            authViewModel.currentViewMode = "PROTECTED"
                        }
                    } else if (intent?.action == "ACTION_MONITOR_ALERT") {
                        // [NOVO] Navegação direta para o Dashboard do Monitor
                        val targetUid = intent.getStringExtra("TARGET_PROTEGE_ID")
                        val targetName = intent.getStringExtra("TARGET_PROTEGE_NAME")
                        
                        // [CORREÇÃO] Se o utilizador estiver em emergência (AlertScreen aberto), IGNORA a navegação
                        if (targetUid != null && targetName != null && !authViewModel.isUserInEmergency) {
                            if (authViewModel.currentViewMode != "MONITOR") {
                                authViewModel.currentViewMode = "MONITOR"
                            }
                            // Navega diretamente, limpando backstack relativo ao monitor se necessário
                            navController.navigate("monitor/dashboard/$targetUid/$targetName") {
                                // Força a recriação do ecrã de destino se necessário
                                launchSingleTop = false 
                            }
                            // Limpa a action para não re-navegar ao rodar o ecrã
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

        // Observador Global de Mudança de Modo e Login
        LaunchedEffect(user, currentViewMode) {
            if (user != null && currentViewMode != null) {
                val currentRoute = navController.currentBackStackEntry?.destination?.route

                if (currentViewMode == "MONITOR") {
                    // [CORREÇÃO] Permite qualquer rota que comece por "monitor" (ex: dashboard)
                    // para não expulsar o utilizador do dashboard ao mudar o modo ou ao navegar via notificação.
                    if (currentRoute?.startsWith("monitor") != true) {
                        navController.navigate("monitor") {
                            popUpTo("landing") { inclusive = true }
                        }
                    }
                } else {
                    // Modo PROTECTED
                    if (currentRoute != "protected") {
                        navController.navigate("protected") {
                            popUpTo("landing") { inclusive = true }
                        }
                    }
                }
            } else if (user == null) {
                // Se o user for null (logout), volta forçosamente ao landing
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
                // --- AUTHENTICATION ---
                composable("landing") {
                    LandingScreen(
                        // Passamos o VM se necessário, ou deixamos o Landing criar o seu,
                        // pois a navegação é gerida pelo LaunchedEffect acima.
                        onLoginClick = { navController.navigate("login") },
                        onRegisterClick = { navController.navigate("register") }
                    )
                }

                composable("login") {
                    // [CORREÇÃO] Passamos a MESMA instância do ViewModel
                    LoginScreen(
                        viewModel = authViewModel,
                        onBack = { navController.popBackStack() },
                        onLogin = { _ ->
                            // A navegação é automática pelo LaunchedEffect no MainScreen
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
                    // [CORREÇÃO] Passamos a MESMA instância do ViewModel
                    RegisterScreen(
                        viewModel = authViewModel,
                        onBack = { navController.popBackStack() },
                        onSignUp = { _ ->
                            // Navegação automática
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

                // --- PERFIL PROTEGIDO ---
                composable("protected") {
                    // [CORREÇÃO] Passamos a instância para permitir o Switch Mode e Logout
                    ProtectedMainScreen(
                        viewModel = authViewModel, // Usa o parametro viewModel do ProtectedMainScreen
                        onLogout = {
                            authViewModel.logout()
                            // Navegação tratada pelo LaunchedEffect
                        }
                    )
                }

                // --- PERFIL MONITOR ---
                composable("monitor") {
                    // [CORREÇÃO] Passamos a instância para permitir o Switch Mode e Logout
                    MonitorMainScreen(
                        viewModel = authViewModel, // Passamos como viewModel (AuthViewModel)
                        onLogout = {
                            authViewModel.logout()
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