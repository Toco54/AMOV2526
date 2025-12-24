package pt.isec.amov2526.projetoKotlin.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import pt.isec.amov2526.projetoKotlin.R

// Using the project's palette
private val Slate900 = Color(0xFF0F172A)
private val Slate800 = Color(0xFF1E293B)
private val Slate400 = Color(0xFF94A3B8)
private val Emerald500 = Color(0xFF10B981)

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onForgotPassword: () -> Unit,
    onSignUpClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
            .padding(24.dp)
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
            text = "Welcome back",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Sign in to access your dashboard.",
            style = MaterialTheme.typography.bodyLarge,
            color = Slate400
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Email
        Text(
            text = "EMAIL",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Slate400
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("name@example.com", color = Slate400.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(Icons.Default.Email, null, tint = Slate400) },
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
            text = "PASSWORD",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Slate400
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("••••••••", color = Slate400.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = Slate400) },
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

        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            TextButton(onClick = onForgotPassword) {
                Text(
                    text = "Forgot Password?",
                    color = Emerald500,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Log In",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Slate400.copy(alpha = 0.3f))
            Text(
                text = "OR CONTINUE WITH",
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

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Don't have an account? ", color = Slate400)
            Text(
                text = "Sign Up",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onSignUpClick() }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
