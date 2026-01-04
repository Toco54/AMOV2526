package pt.isec.amov2526.projetoKotlin.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.isec.amov2526.projetoKotlin.R
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.AuthViewModel

// Using the project's palette (Mantido igual)
private val Slate900 = Color(0xFF0F172A)
private val Slate800 = Color(0xFF1E293B)
private val Slate400 = Color(0xFF94A3B8)
private val Emerald500 = Color(0xFF10B981)

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onLogin: (String) -> Unit, // Agora recebe a Role (Monitor/Protected)
    onForgotPassword: () -> Unit,
    onSignUpClick: () -> Unit,
    viewModel: AuthViewModel = viewModel() // <--- INJEÇÃO DO VIEWMODEL
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // [NOVO] Estado para o input do código MFA
    var mfaCode by remember { mutableStateOf("") }

    // --- LÓGICA DO VIEWMODEL ---
    val isLoading = viewModel.isLoading
    val errorMsg = viewModel.errorMsg
    val user = viewModel.user
    val userRole = viewModel.userRole
    val mfaRequired = viewModel.mfaRequired // Observamos se precisamos de mostrar o MFA

    // Quando o login é feito (MFA passou) e a role é descoberta, avançamos
    LaunchedEffect(user, userRole) {
        if (user != null && userRole != null && !mfaRequired) {
            onLogin(userRole)
        }
    }
    // -------------------------

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
            .padding(24.dp)
    ) {
        // Back Button
        IconButton(
            onClick = {
                if (mfaRequired) viewModel.cancelMfa() else onBack()
            },
            modifier = Modifier
                .size(48.dp)
                .background(Slate800, CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(id = R.string.profile_logout), // Just for desc
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // HEADER: Muda consoante o estado
        Text(
            text = if (mfaRequired) stringResource(id = R.string.auth_security_check) else stringResource(id = R.string.auth_welcome),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (mfaRequired) stringResource(id = R.string.auth_mfa_tagline) else stringResource(id = R.string.auth_signin_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = Slate400
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- CONTEÚDO CONDICIONAL ---
        if (mfaRequired) {
            // ===========================
            // PARTE DO CÓDIGO MFA (OTP)
            // ===========================
            Text(
                text = stringResource(id = R.string.auth_verification_code),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Slate400
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = mfaCode,
                onValueChange = { if (it.length <= 6) mfaCode = it },
                placeholder = {
                    Text(text = "123456", color = Slate400.copy(alpha = 0.5f))
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Slate400
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Slate800,
                    unfocusedContainerColor = Slate800,
                    focusedBorderColor = Emerald500,
                    unfocusedBorderColor = Slate800,
                    cursorColor = Emerald500,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // MOSTRAR ERRO (SE HOUVER)
            if (errorMsg != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMsg ?: "",
                    color = Color(0xFFEF4444),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // BOTÃO VERIFICAR
            Button(
                onClick = { viewModel.verifyMfaCode(mfaCode) },
                enabled = mfaCode.length == 6 && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = stringResource(id = R.string.auth_verify_code),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BOTÃO CANCELAR
            Button(
                onClick = { viewModel.cancelMfa() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Cancel", // Standard enough or add to strings
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

        } else {
            // ===========================
            // PARTE DO LOGIN NORMAL
            // ===========================

            // Email
            Text(
                text = stringResource(id = R.string.auth_email),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Slate400
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = {
                    Text(
                        text = "name@example.com",
                        color = Slate400.copy(alpha = 0.5f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = Slate400
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Slate800,
                    unfocusedContainerColor = Slate800,
                    focusedBorderColor = Emerald500,
                    unfocusedBorderColor = Slate800,
                    cursorColor = Emerald500,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Password
            Text(
                text = stringResource(id = R.string.auth_password),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Slate400
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = {
                    Text(
                        text = "••••••••",
                        color = Slate400.copy(alpha = 0.5f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Slate400
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Slate400
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Slate800,
                    unfocusedContainerColor = Slate800,
                    focusedBorderColor = Emerald500,
                    unfocusedBorderColor = Slate800,
                    cursorColor = Emerald500,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation()
            )

            // MOSTRAR ERRO (SE HOUVER)
            if (errorMsg != null) {
                Spacer(modifier = Modifier.height(16.dp))
                val localizedError = when(errorMsg) {
                    "Please fill in all fields" -> stringResource(id = R.string.error_fill_all)
                    "Invalid Email Format" -> stringResource(id = R.string.error_invalid_email)
                    else -> errorMsg
                }
                Text(
                    text = localizedError ?: "",
                    color = Color(0xFFEF4444), // Emerald Red
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = onForgotPassword) {
                    Text(
                        text = stringResource(id = R.string.auth_forgot_password),
                        color = Emerald500,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // O BOTÃO DE LOGIN AGORA CHAMA O VIEWMODEL
            Button(
                onClick = {
                    val trimmedEmail = email.trim()
                    val trimmedPassword = password.trim()

                    if (trimmedEmail.isBlank() || trimmedPassword.isBlank()) {
                        viewModel.errorMsg = "Please fill in all fields"
                    } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                        viewModel.errorMsg = "Invalid Email Format"
                    } else {
                        viewModel.login(trimmedEmail, trimmedPassword)
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = stringResource(id = R.string.landing_login),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.auth_no_account),
                    color = Slate400
                )
                Text(
                    text = stringResource(id = R.string.auth_signup),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onSignUpClick() }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}