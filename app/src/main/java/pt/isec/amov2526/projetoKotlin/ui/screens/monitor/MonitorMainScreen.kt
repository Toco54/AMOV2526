package pt.isec.amov2526.projetoKotlin.ui.screens.monitor

import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.isec.amov2526.projetoKotlin.R
import pt.isec.amov2526.projetoKotlin.ui.components.EditProfileView
import pt.isec.amov2526.projetoKotlin.ui.screens.monitor.components.main.ConfirmConnectionDialog
import pt.isec.amov2526.projetoKotlin.ui.screens.monitor.components.main.EnterCodeModal
import pt.isec.amov2526.projetoKotlin.ui.screens.monitor.components.main.MonitorTopBar
import pt.isec.amov2526.projetoKotlin.ui.screens.monitor.components.main.ProfileBottomButton
import pt.isec.amov2526.projetoKotlin.ui.screens.monitor.components.main.ProfileMenuOverlay
import pt.isec.amov2526.projetoKotlin.ui.screens.monitor.components.main.ProtegeCard
import pt.isec.amov2526.projetoKotlin.ui.screens.monitor.components.main.SectionHeader
import pt.isec.amov2526.projetoKotlin.ui.theme.Indigo600
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate400
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate50
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate900
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.AuthViewModel
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.MonitorMainViewModel

/**
 * Ecrã principal do modo Monitor.
 *
 * Lista os utilizadores protegidos associados a este monitor e permite:
 * 1. Adicionar novos protegidos (via código).
 * 2. Gerir o perfil do monitor.
 * 3. Alternar para o modo Protegido (se a conta suportar ambos os papéis).
 */
@Composable
fun MonitorMainScreen(
    onLogout: () -> Unit = {},
    onProtegeClick: (String, String) -> Unit = { _, _ -> },
    viewModel: AuthViewModel = viewModel(),
    monitorViewModel: MonitorMainViewModel = viewModel()
) {
    val realName = viewModel.userName
    val realEmail = viewModel.userEmail
    val proteges = monitorViewModel.proteges


    var showProfileMenu by remember { mutableStateOf(false) }
    var showEnterCodeModal by remember { mutableStateOf(false) }
    var isEditingProfile by remember { mutableStateOf(false) }
    val connectionCandidate = monitorViewModel.connectionCandidate


    var showSetPinDialog by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }


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
    val profileUpdatedMsg = stringResource(R.string.profile_updated)
    val invalidPinError = stringResource(R.string.error_invalid_pin)

    if (isEditingProfile) {
        EditProfileView(
            currentName = viewModel.currentUserData?.name ?: realName,
            currentEmail = viewModel.currentUserData?.email ?: realEmail,
            currentPin = viewModel.currentUserData?.pin,
            currentMfaEnabled = viewModel.currentUserData?.mfaEnabled ?: false,
            showPinOptions = false,
            onDismiss = { isEditingProfile = false },
            onSave = { name, email, pass, pin, mfa, currentPass ->
                viewModel.updateProfile(
                    name = name,
                    email = email,
                    pass = pass,
                    pin = pin,
                    mfaEnabled = mfa,
                    currentPass = currentPass,
                    onSuccess = {
                        Toast.makeText(context, profileUpdatedMsg, Toast.LENGTH_SHORT).show()
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
                            contentDescription = stringResource(id = R.string.monitors_add)
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

                            if (proteges.isNotEmpty()) {
                                item { SectionHeader(stringResource(id = R.string.monitor_my_proteges)) }
                                items(items = proteges) { protege ->
                                    ProtegeCard(
                                        protege = protege,
                                        onClick = { onProtegeClick(protege.name, protege.uid) },
                                        onDelete = { monitorViewModel.deleteConnection(protege.uid) }
                                    )
                                }
                            } else {
                                item {
                                    Text(
                                        text = stringResource(id = R.string.monitor_no_proteges),
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
                        onLogout = {
                            viewModel.logout()
                        },
                        onSwitchMode = {
                            val currentPin = viewModel.currentUserData?.pin
                            if (!currentPin.isNullOrBlank()) {
                                // Se já tiver PIN configurado, alterna diretamente.
                                viewModel.switchViewMode()
                            } else {
                                // Se não tiver PIN (ex: Monitor puro), exige configuração antes de ativar o modo Protegido.
                                showProfileMenu = false
                                showSetPinDialog = true
                            }
                        },
                        canSwitchProfile = viewModel.canSwitchProfile
                    )
                }
            }
        }
    }

    if (connectionCandidate != null) {
        ConfirmConnectionDialog(
            candidate = connectionCandidate,
            errorMsg = monitorViewModel.errorMsg,
            onAccept = {
                monitorViewModel.acceptConnection {
                    Toast.makeText(context, "Protege Connected Successfully!", Toast.LENGTH_LONG)
                        .show()
                    showEnterCodeModal = false
                }
            },
            onReject = {
                monitorViewModel.rejectConnection()
            }
        )
    }

    if (showEnterCodeModal) {
        EnterCodeModal(
            viewModel = monitorViewModel,
            onDismiss = { showEnterCodeModal = false }
        )
    }


    if (showSetPinDialog) {
        Dialog(onDismissRequest = { showSetPinDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Indigo600,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(id = R.string.auth_safety_pin),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "To use Protected Mode, you must define a 4-digit PIN for cancelling alerts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = newPin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                newPin = it
                            }
                        },
                        label = { Text(stringResource(id = R.string.auth_safety_pin)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = pinError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError != null) {
                        Text(
                            text = pinError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = { showSetPinDialog = false }) {
                            Text(stringResource(id = R.string.action_cancel))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newPin.length == 4) {
                                    // Atualiza perfil com novo PIN e muda de modo.
                                    viewModel.updateProfile(
                                        name = viewModel.userName,
                                        email = viewModel.userEmail,
                                        pass = null,
                                        pin = newPin,
                                        mfaEnabled = viewModel.currentUserData?.mfaEnabled ?: false,
                                        onSuccess = {
                                            showSetPinDialog = false
                                            viewModel.switchViewMode()
                                        },
                                        onError = {
                                            pinError = it
                                        }
                                    )
                                } else {
                                    pinError = invalidPinError
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                        ) {
                            Text(stringResource(id = R.string.action_save))
                        }
                    }
                }
            }
        }
    }
}
