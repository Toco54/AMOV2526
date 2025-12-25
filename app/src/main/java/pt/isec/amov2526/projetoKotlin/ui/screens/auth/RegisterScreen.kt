package pt.isec.amov2526.projetoKotlin.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import pt.isec.amov2526.projetoKotlin.R
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.AuthViewModel

// Using the project's palette
private val Slate900 = Color(0xFF0F172A)
private val Slate800 = Color(0xFF1E293B)
private val Slate400 = Color(0xFF94A3B8)
private val Emerald500 = Color(0xFF10B981)

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onSignUp: (String) -> Unit,
    onLoginClick: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var selectedRole by remember { mutableStateOf("Protected") } // "Monitor" or "Protected"
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    val isLoading = viewModel.isLoading
    val errorMsg = viewModel.errorMsg
    val user = viewModel.user
    val userRole = viewModel.userRole

    LaunchedEffect(user, userRole) {
        if (user != null && userRole != null) {
            onSignUp(userRole)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
            .padding(24.dp)
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Back Button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .background(Slate800, CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Create Account",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Join SafetySec for advanced protection.",
            style = MaterialTheme.typography.bodyLarge,
            color = Slate400
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Role Toggle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Slate800, RoundedCornerShape(16.dp))
                .padding(4.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                val monitorSelected = selectedRole == "Monitor"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (monitorSelected) Emerald500 else Color.Transparent)
                        .clickable { selectedRole = "Monitor" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Monitor",
                        fontWeight = FontWeight.Bold,
                        color = if (monitorSelected) Color.White else Slate400
                    )
                }
                val protectedSelected = selectedRole == "Protected"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (protectedSelected) Emerald500 else Color.Transparent)
                        .clickable { selectedRole = "Protected" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Protected",
                        fontWeight = FontWeight.Bold,
                        color = if (protectedSelected) Color.White else Slate400
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Full Name
        Text(
            text = "FULL NAME",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Slate400
        )
        Spacer(modifier = Modifier.height(8.dp))
        AuthTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = "John Doe"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Email
        Text(

            text = "EMAIL",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Slate400
        )
        Spacer(modifier = Modifier.height(8.dp))
        AuthTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = "name@example.com",
            keyboardType = KeyboardType.Email
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password
        Text(
            text = "PASSWORD",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Slate400
        )
        Spacer(modifier = Modifier.height(8.dp))
        AuthTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = "Create a password",
            isPassword = true
        )

        // Conditional PIN
        if (selectedRole == "Protected") {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SAFETY PIN (4 DIGITS)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Slate400
            )
            Spacer(modifier = Modifier.height(8.dp))
            AuthTextField(
                value = pin,
                onValueChange = {
                    if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                        pin = it
                    }
                },
                placeholder = "0 0 0 0",
                keyboardType = KeyboardType.Number,
                centerText = true
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- Actions (Now inside the scroll) ---
        Button(
            onClick = {
                // Lógica para decidir se enviamos o PIN (só se for Protegido)
                val isMonitor = selectedRole == "Monitor"
                val finalPin = if (isMonitor) null else pin

                // Chama a função de registo do ViewModel
                viewModel.register(email, password, name, isMonitor, finalPin)
            },
            enabled = !isLoading, // Bloqueia o botão se estiver a carregar
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isLoading) {
                // Mostra a rodinha de loading
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = "Sign Up",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        // Mostra mensagem de erro se o registo falhar (adiciona isto logo abaixo do botão)
        if (errorMsg != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMsg ?: "",
                color = Color(0xFFEF4444), // Vermelho de erro
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Slate400.copy(alpha = 0.3f))
            Text(
                text = "OR JOIN WITH",
                color = Slate400,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp),
                fontSize = 10.sp
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = Slate400.copy(alpha = 0.3f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { /* TODO: Google Auth */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Slate800),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.android_light_rd_na),
                contentDescription = "Google",
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Google",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already have an account? ",
                color = Slate400
            )
            Text(
                text = "Log In",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onLoginClick() }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    centerText: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                color = Slate400.copy(alpha = 0.5f),
                modifier = if (centerText) Modifier.fillMaxWidth() else Modifier,
                textAlign = if (centerText) TextAlign.Center else TextAlign.Start,
                letterSpacing = if (centerText) 8.sp else 0.sp
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
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        textStyle = LocalTextStyle.current.copy(
            textAlign = if (centerText) TextAlign.Center else TextAlign.Start,
            letterSpacing = if (centerText) 8.sp else 0.sp,
            fontSize = if (centerText) 20.sp else 16.sp,
            fontWeight = if (centerText) FontWeight.Bold else FontWeight.Normal
        )
    )
}
