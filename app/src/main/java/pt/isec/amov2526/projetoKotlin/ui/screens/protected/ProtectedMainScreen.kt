package pt.isec.amov2526.projetoKotlin.ui.screens.protected

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import pt.isec.amov2526.projetoKotlin.R
import pt.isec.amov2526.projetoKotlin.ui.components.EditProfileView

@Composable
fun ProtectedMainScreen(
    navController: NavHostController = rememberNavController(),
    onSwitchToMonitor: () -> Unit = {} // Callback para troca de perfil (futuro)
) {
    var isEditingProfile by remember { mutableStateOf(false) }

    if (isEditingProfile) {
        // Fullscreen Profile Edit View
        EditProfileView(
            currentName = "Nome do Utilizador", // TODO: Get from ViewModel
            currentEmail = "email@exemplo.com",
            currentPin = "1234",
            onDismiss = { isEditingProfile = false },
            onSave = { _, _, _, _ ->
                // TODO: Save logic via ViewModel
                isEditingProfile = false
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
            bottomBar = {
                ProtectedBottomBar(navController = navController)
            }
        ) { innerPadding ->
            // Grafo de Navegação Interno (Nested Graph)
            NavHost(
                navController = navController,
                startDestination = "guard",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("guard") {
                    // Ecrã "Guard" (Botão Pânico)
                    GuardScreen()
                }
                composable("monitors") {
                    // Ecrã de Monitores
                    MonitorsScreen()
                }
                composable("activity") {
                    // Ecrã de Histórico
                    ActivityScreen()
                }
                composable("profile") {
                    // Ecrã de Perfil
                    ProfileScreen(
                        onEditProfile = { isEditingProfile = true }
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

        // Guard Button
        NavigationBarItem(
            icon = { Icon(painter = painterResource(id = R.drawable.ic_shield_check), contentDescription = "Guard") },
            label = { Text("Guard") },
            selected = currentDestination?.hierarchy?.any { it.route == "guard" } == true,
            onClick = {
                navController.navigate("guard") {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )

        // Monitors Button
        NavigationBarItem(
            icon = { Icon(imageVector = Icons.Default.SupervisorAccount, contentDescription = "Monitors") },
            label = { Text("Monitors") },
            selected = currentDestination?.hierarchy?.any { it.route == "monitors" } == true,
            onClick = {
                navController.navigate("monitors") {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )

        // Activity Button
        NavigationBarItem(
            icon = { Icon(imageVector = Icons.Default.History, contentDescription = "Activity") },
            label = { Text("Activity") },
            selected = currentDestination?.hierarchy?.any { it.route == "activity" } == true,
            onClick = {
                navController.navigate("activity") {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )

        // Profile Button
        NavigationBarItem(
            icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            selected = currentDestination?.hierarchy?.any { it.route == "profile" } == true,
            onClick = {
                navController.navigate("profile") {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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