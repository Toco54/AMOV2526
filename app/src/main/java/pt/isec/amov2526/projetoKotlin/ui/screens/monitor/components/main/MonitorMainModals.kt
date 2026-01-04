package pt.isec.amov2526.projetoKotlin.ui.screens.monitor.components.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.TextStyle
import pt.isec.amov2526.projetoKotlin.R
import pt.isec.amov2526.projetoKotlin.data.repository.ConnectionCodeInfo
import pt.isec.amov2526.projetoKotlin.ui.theme.Emerald500
import pt.isec.amov2526.projetoKotlin.ui.theme.Emerald700
import pt.isec.amov2526.projetoKotlin.ui.theme.Red600
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate100
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate200
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate400
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate50
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate500
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate900
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.MonitorMainViewModel

@Composable
fun ConfirmConnectionDialog(
    candidate: ConnectionCodeInfo,
    errorMsg: String? = null,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onReject,
        title = {
            Text(
                text = stringResource(id = R.string.monitor_confirm_conn_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(text = stringResource(id = R.string.monitor_confirm_conn_desc))
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.auth_full_name),
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = candidate.protegeName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(id = R.string.auth_email),
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500,
                    fontWeight = FontWeight.Bold
                )
                Text(text = candidate.protegeEmail, style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(id = R.string.monitor_access_level),
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = Emerald500,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = candidate.accessLevel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Emerald700,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMsg,
                        color = Red600,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = Slate900)
            ) {
                Text(stringResource(id = R.string.action_accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onReject) {
                Text(stringResource(id = R.string.action_reject), color = Slate500)
            }
        }
    )
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
                        text = stringResource(id = R.string.dual_role_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }

                Text(
                    text = stringResource(id = R.string.dual_role_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                Text(
                    text = stringResource(id = R.string.auth_safety_pin),
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
                    textStyle = TextStyle(
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
                        Text(
                            stringResource(id = R.string.action_cancel),
                            color = Slate500,
                            fontWeight = FontWeight.Bold
                        )
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
                        Text(
                            stringResource(id = R.string.action_add),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
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
                    text = stringResource(id = R.string.monitors_add),
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
                    onValueChange = {
                        if (it.length <= 6 && it.all { c -> c.isDigit() }) code = it
                    },
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
                    textStyle = TextStyle(
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Slate100,
                            contentColor = Slate900
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text(stringResource(id = R.string.action_cancel))
                    }

                    Button(
                        onClick = { viewModel.validateCode(code) },
                        enabled = code.length == 6 && !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(stringResource(id = R.string.action_connect))
                        }
                    }
                }
            }
        }
    }
}
