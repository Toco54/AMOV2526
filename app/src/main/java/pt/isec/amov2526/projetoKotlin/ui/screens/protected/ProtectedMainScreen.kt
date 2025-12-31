package pt.isec.amov2526.projetoKotlin.ui.screens.protected

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.google.firebase.firestore.FirebaseFirestore
import pt.isec.amov2526.projetoKotlin.ui.components.EditProfileView
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.ProtectedBottomBar
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.ProtectedTopBar
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.AuthViewModel
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.ProtectedMainViewModel

@Composable
fun ProtectedMainScreen(
    navController: NavHostController = rememberNavController(),
    onLogout: () -> Unit = {},
    onSwitchToMonitor: () -> Unit = {},
    viewModel: AuthViewModel = viewModel(),
    protectedViewModel: ProtectedMainViewModel = viewModel()
) {
    // Observe user state to trigger navigation after logout
    LaunchedEffect(viewModel.user) {
        if (viewModel.user == null) {
            if (protectedViewModel.isProtectionActive) {
                protectedViewModel.toggleProtection()
            }
            onLogout()
        }
    }
    
    val context = LocalContext.current
    var isEditingProfile by remember { mutableStateOf(false) }
    val currentUserData = viewModel.currentUserData

    if (isEditingProfile) {
        EditProfileView(
            currentName = currentUserData?.name ?: viewModel.userName,
            currentEmail = currentUserData?.email ?: viewModel.userEmail,
            currentPin = currentUserData?.pin, // Protected user should have a PIN
            onDismiss = { isEditingProfile = false },
            onSave = { name, email, pass, pin, currentPass -> 
                viewModel.updateProfile(
                    name = name,
                    email = email,
                    pass = pass,
                    pin = pin,
                    currentPass = currentPass,
                    onSuccess = {
                        Toast.makeText(context, "Profile Updated Successfully", Toast.LENGTH_SHORT).show()
                        isEditingProfile = false 
                    },
                    onError = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    } else {
        Scaffold(
            topBar = {
                ProtectedTopBar(
                    onProfileClick = {
                        navController.navigate("profile") {
                            launchSingleTop = true
                        }
                    }
                )
            },
            bottomBar = {
                ProtectedBottomBar(navController = navController)
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "guard",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("guard") {
                    GuardScreen()
                }
                composable("monitors") {
                    MonitorsScreen()
                }
                composable("activity") {
                    ActivityScreen()
                }
                composable("profile") {
                    ProfileScreen(
                        name = viewModel.userName,
                        email = viewModel.userEmail,
                        onEditProfile = { isEditingProfile = true },
                        onLogout = {
                            val uid = viewModel.user?.uid
                            if (uid != null) {
                                FirebaseFirestore.getInstance().collection("users").document(uid)
                                    .update("systemStatus", "OFFLINE")
                            }

                            // 2. Desliga o Serviço localmente (mata a notificação e sensores)
                            if (protectedViewModel.isProtectionActive) {
                                protectedViewModel.toggleProtection()
                            }

                            // 3. Faz o Logout da conta (Auth)
                            viewModel.logout()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PlaceholderScreen(text: String) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(text = text)
    }
}