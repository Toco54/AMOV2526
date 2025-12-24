package pt.isec.amov2526.projetoKotlin.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Cores (Partilhadas)
private val Slate50 = Color(0xFFF8FAFC)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate500 = Color(0xFF64748B)
private val Slate900 = Color(0xFF0F172A)
private val Indigo600 = Color(0xFF4F46E5)

@Composable
fun EditProfileView(
    currentName: String,
    currentEmail: String,
    currentPin: String? = null, // Se null, é Monitor (sem secção PIN)
    onDismiss: () -> Unit,
    onSave: (String, String, String?, String?) -> Unit // Name, Email, NewPass, NewPin
) {
    var name by remember { mutableStateOf(currentName) }
    var email by remember { mutableStateOf(currentEmail) }
    
    // PIN States (se aplicável)
    var pinInput by remember { mutableStateOf("") } // Input for verification (current)
    var newPinInput by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    val isPinVerified = currentPin != null && pinInput == currentPin

    // Password States
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val canChangePassword = currentPassword.isNotEmpty() // Simple validation: must enter current pass

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Slate500)
            }
            Text("Edit Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Slate900)
            Spacer(modifier = Modifier.size(40.dp))
        }

        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Name
            Text("FULL NAME", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Slate500)
            TextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Slate50,
                    unfocusedContainerColor = Slate50,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Email
            Text("EMAIL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Slate500)
            TextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Slate50,
                    unfocusedContainerColor = Slate50,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.height(24.dp))

            // --- PIN SECTION (Only if currentPin is provided) ---
            if (currentPin != null) {
                Text("SAFETY PIN (4 DIGITS)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Slate500)
                
                // 1. Verify Current PIN
                Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    TextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Slate50,
                            unfocusedContainerColor = Slate50,
                            focusedIndicatorColor = if (isPinVerified) Color(0xFF4CAF50) else Color.Transparent,
                            unfocusedIndicatorColor = if (isPinVerified) Color(0xFF4CAF50) else Color.Transparent
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp),
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = if (isPinVerified) Color(0xFF4CAF50) else Slate500) },
                        trailingIcon = {
                            IconButton(onClick = { showPin = !showPin }) {
                                Icon(if (showPin) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = Slate500)
                            }
                        },
                        placeholder = { Text("Current PIN") }
                    )
                }

                // 2. Enter New PIN
                if (isPinVerified) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPinInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Slate50,
                            unfocusedContainerColor = Slate50,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp),
                        leadingIcon = { Icon(Icons.Default.Smartphone, null, tint = Slate500) },
                        placeholder = { Text("New PIN") }
                    )
                    Text("Enter new 4-digit PIN to update.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50), fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
                } else {
                    Text("Enter current PIN to unlock changes.", style = MaterialTheme.typography.bodySmall, color = Slate500, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- PASSWORD SECTION ---
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Slate200))
            Spacer(modifier = Modifier.height(24.dp))

            Text("CHANGE PASSWORD", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Slate500)
            
            // Current Password (Required to change)
            TextField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                placeholder = { Text("Current Password") },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = Slate500)
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Slate50,
                    unfocusedContainerColor = Slate50,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            // New Password Fields (Only if current is entered)
            if (canChangePassword) {
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    placeholder = { Text("New Password") },
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
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    placeholder = { Text("Confirm New Password") },
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
            } else {
                Text("Enter current password to change it.", style = MaterialTheme.typography.bodySmall, color = Slate500, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Indigo600, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { 
                        val finalPin = if (isPinVerified && newPinInput.length == 4) newPinInput else null
                        val finalPass = if (canChangePassword && newPassword.isNotEmpty() && newPassword == confirmPassword) newPassword else null
                        onSave(name, email, finalPass, finalPin) 
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}