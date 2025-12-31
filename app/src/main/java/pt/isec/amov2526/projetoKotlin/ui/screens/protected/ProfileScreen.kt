package pt.isec.amov2526.projetoKotlin.ui.screens.protected

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.profile.ProfileActionButton

// Cores do Mockup
private val Slate50 = Color(0xFFF8FAFC)
private val Slate500 = Color(0xFF64748B)
private val Slate900 = Color(0xFF0F172A)
private val Indigo600 = Color(0xFF4F46E5)

@Composable
fun ProfileScreen(
    name: String = "User Name",
    email: String = "email@example.com",
    currentPin: String = "1234",
    onEditProfile: () -> Unit = {},
    onConfigureMonitor: () -> Unit = {},
    onAppSettings: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var showMonitorDialog by remember { mutableStateOf(false) }

    // Vista Principal do Perfil
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Cartão de Informação do Utilizador
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.size(100.dp),
                    tint = Indigo600
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )

                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Botões de Ação
        Column(
            modifier = Modifier.fillMaxWidth(0.95f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileActionButton(
                text = "Edit Profile",
                icon = Icons.Default.ManageAccounts,
                onClick = onEditProfile
            )

            ProfileActionButton(
                text = "Configure Monitor Profile",
                icon = Icons.Default.Visibility,
                onClick = { showMonitorDialog = true },
                customContentColor = Color(0xFF4CAF50)
            )

            ProfileActionButton(
                text = "App Settings",
                icon = Icons.Default.Settings,
                onClick = onAppSettings
            )

            ProfileActionButton(
                text = "Logout",
                icon = Icons.AutoMirrored.Filled.Logout,
                onClick = onLogout,
                isLogout = true,
                showArrow = false
            )
        }
    }

    // Diálogo de Confirmação Monitor
    if (showMonitorDialog) {
        AlertDialog(
            onDismissRequest = { showMonitorDialog = false },
            title = {
                Text(text = "Configure Monitor Profile")
            },
            text = {
                Text(
                    text = "Are you sure you want to Configure a Monitor profile? " +
                            "You will be able to supervise Protected users."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMonitorDialog = false
                        onConfigureMonitor()
                    }
                ) {
                    Text(text = "Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showMonitorDialog = false }
                ) {
                    Text(text = "Cancel")
                }
            }
        )
    }
}
