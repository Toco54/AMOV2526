package pt.isec.amov2526.projetoKotlin.ui.screens.protected

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Cores do Mockup
private val Slate50 = Color(0xFFF8FAFC)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate500 = Color(0xFF64748B)
private val Slate900 = Color(0xFF0F172A)
private val Indigo600 = Color(0xFF4F46E5)

@Composable
fun ProfileScreen(
    name: String = "Nome do Utilizador",
    email: String = "email@exemplo.com",
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
                icon = Icons.Default.ExitToApp,
                onClick = onLogout,
                isLogout = true
            )
        }
    }

    // Diálogo de Confirmação Monitor
    if (showMonitorDialog) {
        AlertDialog(
            onDismissRequest = { showMonitorDialog = false },
            title = { Text("Configure Monitor Profile") },
            text = { Text("Are you sure you want to Configure a Monitor profile? You will be able to supervise Protected users.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMonitorDialog = false
                        onConfigureMonitor()
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMonitorDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProfileActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isLogout: Boolean = false,
    customContentColor: Color? = null
) {
    val contentColor =
        customContentColor ?: if (isLogout) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        onClick = onClick, // Ripple fix nativo do Card
        colors = CardDefaults.cardColors(
            containerColor = if (isLogout) MaterialTheme.colorScheme.errorContainer else Color.White,
            contentColor = contentColor
        ),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null)
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .padding(end = 4.dp),
                tint = Slate200
            )
        }
    }
}