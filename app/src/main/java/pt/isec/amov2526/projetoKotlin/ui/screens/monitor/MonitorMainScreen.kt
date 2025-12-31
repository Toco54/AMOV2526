package pt.isec.amov2526.projetoKotlin.ui.screens.monitor

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.isec.amov2526.projetoKotlin.ui.components.EditProfileView
import pt.isec.amov2526.projetoKotlin.ui.screens.monitor.components.main.*
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.AuthViewModel
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.MonitorMainViewModel

// --- CORES ---
private val Slate50 = Color(0xFFF8FAFC)
private val Slate400 = Color(0xFF94A3B8)
private val Slate900 = Color(0xFF0F172A)

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
