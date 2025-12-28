package pt.isec.amov2526.projetoKotlin.ui.screens.monitor

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.isec.amov2526.projetoKotlin.R
import pt.isec.amov2526.projetoKotlin.ui.components.EditProfileView
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.AuthViewModel
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.MonitorMainViewModel

import pt.isec.amov2526.projetoKotlin.data.model.ConnectedUser

// --- CORES ---
private val Slate50 = Color(0xFFF8FAFC)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate400 = Color(0xFF94A3B8)
private val Slate500 = Color(0xFF64748B)
private val Slate900 = Color(0xFF0F172A)
private val Emerald50 = Color(0xFFECFDF5)
private val Emerald200 = Color(0xFFA7F3D0)
private val Emerald500 = Color(0xFF10B981)
private val Emerald700 = Color(0xFF047857)
private val Indigo500 = Color(0xFF6366F1)
private val Indigo600 = Color(0xFF4F46E5)
private val Red50 = Color(0xFFFEF2F2)
private val Red600 = Color(0xFFDC2626)

@Composable
fun MonitorMainScreen(
    onLogout: () -> Unit = {},
    onProtegeClick: (String, String) -> Unit = { _, _ -> }, // name, uid
    viewModel: AuthViewModel = viewModel(),
    monitorViewModel: MonitorMainViewModel = viewModel()
) {
    val realName = viewModel.userName
    val realEmail = viewModel.userEmail
    val proteges = monitorViewModel.proteges
    var showProfileMenu by remember { mutableStateOf(false) }
    var showConfigureProtectedModal by remember { mutableStateOf(false) }
    var showEnterCodeModal by remember { mutableStateOf(false) }
    var isEditingProfile by remember { mutableStateOf(false) }
    val connectionCandidate = monitorViewModel.connectionCandidate

    // Observe user state to trigger navigation after logout
    LaunchedEffect(viewModel.user) {
        if (viewModel.user == null) {
            onLogout()
        }
    }

    LaunchedEffect(connectionCandidate) {
        if (connectionCandidate != null) {
            showEnterCodeModal = false
        }
    }
    
    val context = LocalContext.current

    if (isEditingProfile) {
        EditProfileView(
            currentName = viewModel.currentUserData?.name ?: realName,
            currentEmail = viewModel.currentUserData?.email ?: realEmail,
            currentPin = viewModel.currentUserData?.pin,
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
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    MonitorTopBar()
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showEnterCodeModal = true },
                        containerColor = Slate900,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add Protege"
                        )
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
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // 1. MY PROTEGES
                            if (proteges.isNotEmpty()) {
                                item { SectionHeader("MY PROTEGES") }
                                items(items = proteges) { protege ->
                                    ProtegeCard(
                                        protege = protege,
                                        onClick = { onProtegeClick(protege.name, protege.uid) }
                                    )
                                }
                            } else {
                                item {
                                    Text(
                                        text = "No active proteges yet.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Slate400,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }

                            item { Spacer(modifier = Modifier.height(80.dp)) }
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
                        name = realName,
                        email = realEmail,
                        onDismiss = { showProfileMenu = false },
                        onEditProfile = {
                            showProfileMenu = false
                            isEditingProfile = true
                        },
                        onConfigureProtected = {
                            showProfileMenu = false
                            showConfigureProtectedModal = true
                        },
                        onLogout = {
                            viewModel.logout()
                        }
                    )
                }
            }
        }
    }

    if (connectionCandidate != null) {
        ConfirmConnectionDialog(
            candidate = connectionCandidate,
            onAccept = {
                monitorViewModel.acceptConnection {
                    Toast.makeText(context, "Protege Connected Successfully!", Toast.LENGTH_LONG).show()
                    showEnterCodeModal = false // Close code modal too
                }
            },
            onReject = {
                monitorViewModel.rejectConnection()
            }
        )
    }

    if (showConfigureProtectedModal) {
        ConfigureProtectedModal(
            onDismiss = { showConfigureProtectedModal = false },
            onConfirm = { showConfigureProtectedModal = false }
        )
    }

    if (showEnterCodeModal) {
        EnterCodeModal(
            viewModel = monitorViewModel,
            onDismiss = { showEnterCodeModal = false }
        )
    }
}

@Composable
fun EnterCodeModal(
    viewModel: MonitorMainViewModel,
    onDismiss: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    val isLoading = viewModel.isLoading
    val errorMsg = viewModel.errorMsg

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Add Protege",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    text = "Enter the 6-digit pairing code from the Protected user.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = code,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) code = it },
                    placeholder = {
                        Text(
                            text = "000000",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Slate50,
                        unfocusedContainerColor = Slate50,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        letterSpacing = 8.sp
                    )
                )

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMsg ?: "", color = Red600, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Slate100, contentColor = Slate900),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { viewModel.validateCode(code) },
                        enabled = code.length == 6 && !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Connect")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = Slate400,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun ProtegeCard(
    protege: ConnectedUser,
    onClick: () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
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
                    text = if (protege.name.isNotEmpty()) protege.name.first().toString() else "?",
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
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Emerald500, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Active Monitoring",
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
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(28.dp),
                tint = Color.Unspecified
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
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
                Text(
                    text = "A",
                    fontWeight = FontWeight.Bold,
                    color = Slate500
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "My Profile",
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    text = "Manage Account",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
            }
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = Slate400
            )
        }
    }
}

@Composable
fun ProfileMenuOverlay(
    name: String,
    email: String,
    onDismiss: () -> Unit,
    onEditProfile: () -> Unit,
    onConfigureProtected: () -> Unit,
    onLogout: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        modifier = Modifier
            .width(320.dp)
            .border(1.dp, Slate100, RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = Slate500
            )
            Text(
                text = "Monitor Account",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate500
            )
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
                    onClick = { }
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
fun ConfirmConnectionDialog(
    candidate: pt.isec.amov2526.projetoKotlin.data.repository.ConnectionCodeInfo,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onReject,
        title = {
            Text(text = "Confirm Connection", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(text = "Do you want to connect with this user?")
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(text = "NAME", style = MaterialTheme.typography.labelSmall, color = Slate500, fontWeight = FontWeight.Bold)
                Text(text = candidate.protegeName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(text = "EMAIL", style = MaterialTheme.typography.labelSmall, color = Slate500, fontWeight = FontWeight.Bold)
                Text(text = candidate.protegeEmail, style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(8.dp))

                Text(text = "ACCESS LEVEL", style = MaterialTheme.typography.labelSmall, color = Slate500, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Shield, 
                        contentDescription = null, 
                        tint = Emerald500, 
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = candidate.accessLevel, style = MaterialTheme.typography.bodyMedium, color = Emerald700, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = Slate900)
            ) {
                Text("Accept")
            }
        },
        dismissButton = {
            TextButton(onClick = onReject) {
                Text("Reject", color = Slate500)
            }
        }
    )
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
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor
                )
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

@Composable
fun ConfigureProtectedModal(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = Emerald500,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Enable Protection",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }

                Text(
                    text = "Create a protected profile for yourself. You will need a PIN to cancel false alarms.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                Text(
                    text = "CREATE SAFETY PIN (4 DIGITS)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate400
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                            pin = it
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = {
                        Text(
                            text = "0000"
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Slate50,
                        unfocusedContainerColor = Slate50,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        letterSpacing = 8.sp
                    ),
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
                            .background(
                                color = if (pin.length == 4) Emerald500 else Slate200,
                                shape = RoundedCornerShape(16.dp)
                            )
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