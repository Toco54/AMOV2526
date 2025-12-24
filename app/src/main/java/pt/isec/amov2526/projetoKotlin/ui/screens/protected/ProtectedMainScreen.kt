package pt.isec.amov2526.projetoKotlin.ui.screens.protected

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import pt.isec.amov2526.projetoKotlin.R

@Composable
fun ProtectedMainScreen(
    navController: NavHostController = rememberNavController(),
    onLogout: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onSwitchToMonitor: () -> Unit = {}
) {
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
                    onEditProfile = onEditProfile,
                    onLogout = onLogout
                )
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
