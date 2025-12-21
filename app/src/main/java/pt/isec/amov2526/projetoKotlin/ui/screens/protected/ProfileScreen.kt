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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
fun ProfileSettingsView(
    currentName: String,
    currentEmail: String,
    actualCurrentPin: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String?, String?) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var email by remember { mutableStateOf(currentEmail) }
    
    // Estados para o PIN
    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    val isPinVerified = currentPinInput == actualCurrentPin

    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Spacer(modifier = Modifier.height(16.dp)) // Adicionado padding no topo
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Slate500)
            }
            Text("Edit Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Slate900)
            Spacer(modifier = Modifier.size(40.dp))
        }

        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Name
            Text("FULL NAME", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Slate500)
            TextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Slate50,
                    unfocusedContainerColor = Slate50,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Email
            Text("EMAIL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Slate500)
            TextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Slate50,
                    unfocusedContainerColor = Slate50,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.height(24.dp))

            // --- PIN SECTION ---
            Text("SAFETY PIN (4 DIGITS)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Slate500)
            
            // 1. Verify Current PIN
            Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                TextField(
                    value = currentPinInput,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) currentPinInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Slate50,
                        unfocusedContainerColor = Slate50,
                        focusedIndicatorColor = if (isPinVerified) Color(0xFF4CAF50) else Color.Transparent,
                        unfocusedIndicatorColor = if (isPinVerified) Color(0xFF4CAF50) else Color.Transparent
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp),
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = if (isPinVerified) Color(0xFF4CAF50) else Slate500) },
                    trailingIcon = {
                        IconButton(onClick = { showPin = !showPin }) {
                            Icon(if (showPin) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = Slate500)
                        }
                    },
                    placeholder = { Text("Current PIN") }
                )
            }

            // 2. Enter New PIN (Only shows if verified)
            if (isPinVerified) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPinInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Slate50,
                            unfocusedContainerColor = Slate50,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp),
                        leadingIcon = { Icon(Icons.Default.Smartphone, null, tint = Slate500) },
                        placeholder = { Text("New PIN") }
                    )
                }
                Text("Enter new 4-digit PIN to update.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50), fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
            } else {
                Text("Enter current PIN to unlock changes.", style = MaterialTheme.typography.bodySmall, color = Slate500, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Password Section (Divider)
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Slate200))
            Spacer(modifier = Modifier.height(24.dp))

            Text("CHANGE PASSWORD (OPTIONAL)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Slate500)
            
            TextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("New Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Slate50,
                    unfocusedContainerColor = Slate50,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = { Text("Confirm New Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Slate50,
                    unfocusedContainerColor = Slate50,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            // Save Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Indigo600, RoundedCornerShape(16.dp))
                    .clickable { 
                        val finalPin = if (isPinVerified && newPinInput.length == 4) newPinInput else null
                        onSave(name, email, password.ifEmpty { null }, finalPin) 
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
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
    val contentColor = customContentColor ?: if (isLogout) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isLogout) MaterialTheme.colorScheme.errorContainer else Color.White,
            contentColor = contentColor
        ),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                modifier = Modifier.size(16.dp).padding(end = 4.dp),
                tint = Slate200
            )
        }
    }
}