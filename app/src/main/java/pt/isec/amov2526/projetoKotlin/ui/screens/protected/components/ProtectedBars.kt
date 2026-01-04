package pt.isec.amov2526.projetoKotlin.ui.screens.protected.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import pt.isec.amov2526.projetoKotlin.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectedTopBar(onProfileClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.app_name),
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
                    contentDescription = stringResource(id = R.string.nav_profile)
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
                    contentDescription = stringResource(id = R.string.nav_guard)
                )
            },
            label = { Text(stringResource(id = R.string.nav_guard)) },
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
                    contentDescription = stringResource(id = R.string.nav_monitors)
                )
            },
            label = { Text(stringResource(id = R.string.nav_monitors)) },
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
                    contentDescription = stringResource(id = R.string.nav_activity)
                )
            },
            label = { Text(stringResource(id = R.string.nav_activity)) },
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
                    contentDescription = stringResource(id = R.string.nav_profile)
                )
            },
            label = { Text(stringResource(id = R.string.nav_profile)) },
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