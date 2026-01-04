package pt.isec.amov2526.projetoKotlin.ui.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.isec.amov2526.projetoKotlin.R
import pt.isec.amov2526.projetoKotlin.ui.theme.Indigo600
import pt.isec.amov2526.projetoKotlin.ui.theme.Red600
import pt.isec.amov2526.projetoKotlin.ui.theme.SafeGreen
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate200
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate50
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate500
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate900

@Composable
fun EditProfileView(
    currentName: String,
    currentEmail: String,
    currentPin: String? = null,
    currentMfaEnabled: Boolean = false,
    showPinOptions: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (String, String, String?, String?, Boolean, String?) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(currentName) }
    var email by remember { mutableStateOf(currentEmail) }
    var mfaEnabled by remember { mutableStateOf(currentMfaEnabled) }

    var pinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    val isPinVerified = currentPin != null && pinInput == currentPin

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val canSave = currentPassword.isNotEmpty()
    val confirmPasswordError = stringResource(R.string.error_mismatch_pass)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Slate500
                )
            }
            Text(
                text = stringResource(id = R.string.profile_edit_profile),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
            Spacer(modifier = Modifier.size(40.dp))
        }

        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {

            Text(
                text = stringResource(id = R.string.auth_full_name),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Slate500
            )
            TextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Slate50,
                    unfocusedContainerColor = Slate50,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.height(16.dp))


            Text(
                text = stringResource(id = R.string.auth_email),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Slate500
            )
            TextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Slate50,
                    unfocusedContainerColor = Slate50,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.height(24.dp))


            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.profile_mfa),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate500
                    )
                    Text(
                        text = stringResource(id = R.string.profile_mfa_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500
                    )
                }
                Switch(
                    checked = mfaEnabled,
                    onCheckedChange = { mfaEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Indigo600
                    )
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            if (showPinOptions && currentPin != null) {
                Text(
                    text = stringResource(id = R.string.auth_safety_pin),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate500
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    TextField(
                        value = pinInput,
                        onValueChange = {
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                pinInput = it
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = if (showPin) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Slate50,
                            unfocusedContainerColor = Slate50,
                            focusedIndicatorColor = if (isPinVerified) SafeGreen else Color.Transparent,
                            unfocusedIndicatorColor = if (isPinVerified) SafeGreen else Color.Transparent
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isPinVerified) SafeGreen else Slate500
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPin = !showPin }) {
                                Icon(
                                    imageVector = if (showPin) {
                                        Icons.Default.VisibilityOff
                                    } else {
                                        Icons.Default.Visibility
                                    },
                                    contentDescription = null,
                                    tint = Slate500
                                )
                            }
                        },
                        placeholder = { Text(stringResource(id = R.string.profile_current_pin)) }
                    )
                }
                if (isPinVerified) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = newPinInput,
                        onValueChange = {
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                newPinInput = it
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = if (showPin) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Slate50,
                            unfocusedContainerColor = Slate50,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = Slate500
                            )
                        },
                        placeholder = { Text(stringResource(id = R.string.profile_new_pin)) }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Slate200)
            )
            Spacer(modifier = Modifier.height(24.dp))


            Text(
                text = stringResource(id = R.string.profile_new_password).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Slate500
            )

            TextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                placeholder = { Text(stringResource(id = R.string.profile_new_password)) },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Slate500
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Slate50,
                    unfocusedContainerColor = Slate50,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            if (newPassword.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    placeholder = { Text(stringResource(id = R.string.profile_confirm_password)) },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Slate50,
                        unfocusedContainerColor = Slate50,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Slate200)
            )
            Spacer(modifier = Modifier.height(24.dp))


            Text(
                text = stringResource(id = R.string.profile_current_password_req),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Red600
            )
            TextField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                placeholder = { Text(stringResource(id = R.string.profile_enter_current_pass)) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Slate50,
                    unfocusedContainerColor = Slate50,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.weight(1f))


            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(
                        if (canSave) Indigo600 else Slate200,
                        RoundedCornerShape(16.dp)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = canSave) {
                        val finalPin =
                            if (isPinVerified && newPinInput.length == 4) newPinInput else null


                        val finalPass = if (newPassword.isNotEmpty()) {
                            if (newPassword == confirmPassword) newPassword else null
                        } else null


                        if (newPassword.isNotEmpty() && newPassword != confirmPassword) {
                            Toast.makeText(context, confirmPasswordError, Toast.LENGTH_SHORT).show()
                            return@clickable
                        }

                        onSave(name, email, finalPass, finalPin, mfaEnabled, currentPassword)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.profile_save_changes),
                    color = if (canSave) Color.White else Slate500,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
