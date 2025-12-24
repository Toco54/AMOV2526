package pt.isec.amov2526.projetoKotlin.ui.screens.monitor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import pt.isec.amov2526.projetoKotlin.R
import pt.isec.amov2526.projetoKotlin.ui.components.EditProfileView

// Cores (Consistente com a app)
private val Slate50 = Color(0xFFF8FAFC)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate400 = Color(0xFF94A3B8)
private val Slate500 = Color(0xFF64748B)
private val Slate900 = Color(0xFF0F172A)
private val Emerald50 = Color(0xFFECFDF5)
private val Emerald500 = Color(0xFF10B981)
private val Indigo600 = Color(0xFF4F46E5)
private val Red50 = Color(0xFFFEF2F2)
private val Red600 = Color(0xFFDC2626)

data class ProtegeItem(
    val id: String,
    val name: String,
    val status: String = "Active Monitoring"
)

private val mockProteges = listOf(
    ProtegeItem("1", "Bob Protected"),
    ProtegeItem("2", "Charlie Senior")
)

@Composable
fun MonitorMainScreen(
    onLogout: () -> Unit = {}
) {
    var showProfileMenu by remember { mutableStateOf(false) }
    var isEditingProfile by remember { mutableStateOf(false) }
    var showConfigureProtectedModal by remember { mutableStateOf(false) }

    if (isEditingProfile) {
        EditProfileView(
            currentName = "Alice Monitor",
            currentEmail = "alice@example.com",
            currentPin = null, // Monitor não tem PIN
            onDismiss = { isEditingProfile = false },
            onSave = { name, email, pass, pin -> 
                isEditingProfile = false 
            }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = { MonitorTopBar() },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { /* Add Protege Logic */ },
                        containerColor = Slate900,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = "Add Protege")
                    }
                },
                bottomBar = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        ProfileBottomButton(
                            onClick = { showProfileMenu = !showProfileMenu }
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Slate50)
                        .padding(innerPadding)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "MY PROTEGES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(mockProteges) { protege ->
                                ProtegeCard(protege)
                            }
                        }
                    }
                }
            }

            // Menu Overlay
            if (showProfileMenu) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f))
                        .clickable { showProfileMenu = false }
                )
                
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, end = 16.dp, bottom = 100.dp)
                ) {
                    ProfileMenuOverlay(
                        onDismiss = { showProfileMenu = false },
                        onEditProfile = { 
                            showProfileMenu = false
                            isEditingProfile = true 
                        },
                        onConfigureProtected = {
                            showProfileMenu = false
                            showConfigureProtectedModal = true
                        },
                        onLogout = onLogout
                    )
                }
            }
        }
    }

    if (showConfigureProtectedModal) {
        ConfigureProtectedModal(
            onDismiss = { showConfigureProtectedModal = false },
            onConfirm = { pin ->
                showConfigureProtectedModal = false
            }
        )
    }
}

@Composable
fun ConfigureProtectedModal(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Shield, null, tint = Emerald500, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Enable Protection", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Slate900)
                }
                
                Text(
                    "Create a protected profile for yourself. You will need a PIN to cancel false alarms.", 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = Slate500, 
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                Text("CREATE SAFETY PIN (4 DIGITS)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Slate400)
                Spacer(modifier = Modifier.height(8.dp))
                
                TextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("0000") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Slate50,
                        unfocusedContainerColor = Slate50,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center, letterSpacing = 8.sp),
                    visualTransformation = PasswordVisualTransformation()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .background(Slate100, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cancel", color = Slate500, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .background(if (pin.length == 4) Emerald500 else Slate200, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = pin.length == 4) { onConfirm(pin) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Create Profile", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorTopBar() {
    TopAppBar(
        title = {
            Text(
                text = "SafetYSec Monitor",
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
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
fun ProtegeCard(protege: ProtegeItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { /* Navigate to Detail */ }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Slate100, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = protege.name.first().toString(),
                    fontWeight = FontWeight.Bold,
                    color = Slate500,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = protege.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Emerald500, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = protege.status,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Slate200,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ProfileBottomButton(onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Slate100, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("A", fontWeight = FontWeight.Bold, color = Slate500)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("My Profile", fontWeight = FontWeight.Bold, color = Slate900)
                Text("Manage Account", style = MaterialTheme.typography.bodySmall, color = Slate500)
            }
            Icon(Icons.Default.Settings, null, tint = Slate400)
        }
    }
}

@Composable
fun ProfileMenuOverlay(onDismiss: () -> Unit, onEditProfile: () -> Unit, onConfigureProtected: () -> Unit, onLogout: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        modifier = Modifier
            .width(320.dp)
            .border(1.dp, Slate100, RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Alice Monitor", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Slate900)
            Text("Monitor Account", style = MaterialTheme.typography.bodyMedium, color = Slate500)
            
            Spacer(modifier = Modifier.height(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuOptionButton(
                    icon = Icons.Outlined.ManageAccounts, 
                    text = "Edit Profile", 
                    onClick = onEditProfile
                )
                MenuOptionButton(
                    icon = Icons.Outlined.Shield, 
                    text = "Configure Protected Profile", 
                    iconColor = Emerald500,
                    textColor = Emerald500,
                    onClick = onConfigureProtected
                )
                MenuOptionButton(
                    icon = Icons.Outlined.Settings, 
                    text = "App Settings", 
                    onClick = { /* Navigate */ }
                )
                
                MenuOptionButton(
                    icon = Icons.AutoMirrored.Filled.Logout, 
                    text = "Logout", 
                    textColor = Red600,
                    iconColor = Red600,
                    isDestructive = true,
                    onClick = onLogout
                )
            }
        }
    }
}

@Composable
fun MenuOptionButton(
    icon: ImageVector, 
    text: String, 
    textColor: Color = Slate900, 
    iconColor: Color = Slate500, 
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isDestructive) Red50 else Color.White,
            contentColor = if (isDestructive) Red600 else MaterialTheme.colorScheme.onSurfaceVariant
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
                Icon(imageVector = icon, contentDescription = null, tint = iconColor)
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    fontSize = 14.sp
                )
            }
            if (!isDestructive) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Slate200
                )
            }
        }
    }
}