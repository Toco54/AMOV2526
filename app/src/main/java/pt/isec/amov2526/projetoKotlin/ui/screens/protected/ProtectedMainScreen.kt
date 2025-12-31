package pt.isec.amov2526.projetoKotlin.ui.screens.protected

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import pt.isec.amov2526.projetoKotlin.R

import com.google.firebase.firestore.FirebaseFirestore

import pt.isec.amov2526.projetoKotlin.ui.viewmodels.AuthViewModel
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.ProtectedMainViewModel

import androidx.lifecycle.viewmodel.compose.viewModel

import pt.isec.amov2526.projetoKotlin.ui.components.EditProfileView

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectedTopBar(onProfileClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "SafetYSec",
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_shield_check),
                contentDescription = "App Logo",
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp)
                    .size(28.dp),
                tint = Color.Unspecified
            )
        },
        actions = {
            IconButton(onClick = onProfileClick) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Go to Profile"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
fun ProtectedBottomBar(
    navController: NavHostController
) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_shield_check),
                    contentDescription = "Guard"
                )
            },
            label = { Text("Guard") },
            selected = currentDestination?.hierarchy?.any { it.route == "guard" } == true,
            onClick = {
                navController.navigate("guard") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )

        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.SupervisorAccount,
                    contentDescription = "Monitors"
                )
            },
            label = { Text("Monitors") },
            selected = currentDestination?.hierarchy?.any { it.route == "monitors" } == true,
            onClick = {
                navController.navigate("monitors") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )

        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Activity"
                )
            },
            label = { Text("Activity") },
            selected = currentDestination?.hierarchy?.any { it.route == "activity" } == true,
            onClick = {
                navController.navigate("activity") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )

        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile"
                )
            },
            label = { Text("Profile") },
            selected = currentDestination?.hierarchy?.any { it.route == "profile" } == true,
            onClick = {
                navController.navigate("profile") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
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
