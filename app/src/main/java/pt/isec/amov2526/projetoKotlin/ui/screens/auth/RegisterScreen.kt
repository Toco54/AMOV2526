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
import androidx.compose.ui.res.stringResource
import pt.isec.amov2526.projetoKotlin.R
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.AuthViewModel

import pt.isec.amov2526.projetoKotlin.ui.theme.*

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onSignUp: (String) -> Unit,
    onLoginClick: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var selectedRole by remember { mutableStateOf("Protected") }
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
            text = stringResource(id = R.string.auth_create_account),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.auth_signup_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = Slate400
        )

        Spacer(modifier = Modifier.height(32.dp))


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
                        text = stringResource(id = R.string.auth_monitor),
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
                        text = stringResource(id = R.string.auth_protected),
                        fontWeight = FontWeight.Bold,
                        color = if (protectedSelected) Color.White else Slate400
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))


        Text(
            text = stringResource(id = R.string.auth_full_name),
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


        Text(

            text = stringResource(id = R.string.auth_email),
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


        Text(
            text = stringResource(id = R.string.auth_password),
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


        if (selectedRole == "Protected") {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.auth_safety_pin),
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


        Button(
            onClick = {
                val isMonitor = selectedRole == "Monitor"
                val trimmedName = name.trim()
                val trimmedEmail = email.trim()
                val trimmedPassword = password.trim()
                

                if (trimmedName.isBlank() || trimmedEmail.isBlank() || trimmedPassword.isBlank() || (!isMonitor && pin.length != 4)) {
                    viewModel.errorMsg = "Please fill in all fields correctly"
                    return@Button
                }
                
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                    viewModel.errorMsg = "Invalid Email Format"
                    return@Button
                }

                if (trimmedPassword.length < 6) {
                    viewModel.errorMsg = "Password must be at least 6 characters"
                    return@Button
                }

                val finalPin = if (isMonitor) null else pin
                viewModel.register(trimmedEmail, trimmedPassword, trimmedName, isMonitor, finalPin)
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
                    text = stringResource(id = R.string.auth_signup),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        if (errorMsg != null) {
            Spacer(modifier = Modifier.height(16.dp))
            val localizedError = when(errorMsg) {
                "Please fill in all fields correctly" -> stringResource(id = R.string.error_fill_all)
                "Invalid Email Format" -> stringResource(id = R.string.error_invalid_email)
                "Password must be at least 6 characters" -> stringResource(id = R.string.error_password_length)
                else -> errorMsg
            }
            Text(
                text = localizedError ?: "",
                color = AlertRed,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.landing_have_account),
                color = Slate400
            )
            Text(
                text = stringResource(id = R.string.landing_login),
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
