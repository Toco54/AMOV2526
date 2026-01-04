package pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.guard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import pt.isec.amov2526.projetoKotlin.ui.theme.Amber500
import pt.isec.amov2526.projetoKotlin.ui.theme.Indigo600
import pt.isec.amov2526.projetoKotlin.ui.theme.Red600
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate200
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate400
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate50
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate500
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate700
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate900
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.GuardViewModel

@Composable
fun PauseSystemModal(
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit,
    viewModel: GuardViewModel,
    title: String,
) {
    var pin by remember { mutableStateOf("") }
    var selectedDuration by remember { mutableStateOf("15 Min") }
    var isChecking by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PauseCircle,
                        contentDescription = null,
                        tint = Amber500,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }

                Text(
                    text = "Select duration and enter PIN.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                listOf(
                    "1 Min", "15 Min", "30 Min", "1 Hour", "8 Hours", "Indefinite"
                ).forEach { dur ->
                    val isSelected = selectedDuration == dur
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) Indigo600.copy(alpha = 0.1f) else Slate50
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Indigo600 else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedDuration = dur }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = dur,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Indigo600 else Slate700
                        )
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (isSelected) Indigo600 else Slate400,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "SECURITY PIN",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate400
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Slate50, RoundedCornerShape(12.dp))
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = pin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                pin = it
                                errorText = null
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        decorationBox = { innerTextField ->
                            if (pin.isEmpty()) {
                                Text(
                                    text = "****", style = TextStyle(
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate200,
                                        textAlign = TextAlign.Center,
                                        letterSpacing = 8.sp
                                    )
                                )
                            }
                            innerTextField()
                        },
                        textStyle = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = Slate900,
                            letterSpacing = 8.sp
                        )
                    )
                }
                if (errorText != null) {
                    Text(
                        errorText!!,
                        color = Red600,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (pin.length == 4) {
                            isChecking = true
                            scope.launch {
                                if (viewModel.verifyPin(pin)) {
                                    val durationMillis = when (selectedDuration) {
                                        "1 Min" -> 1 * 60 * 1000L
                                        "15 Min" -> 15 * 60 * 1000L
                                        "30 Min" -> 30 * 60 * 1000L
                                        "1 Hour" -> 60 * 60 * 1000L
                                        "8 Hours" -> 8 * 60 * 60 * 1000L
                                        "Indefinite" -> -1L
                                        else -> 0L
                                    }
                                    onConfirm(pin, durationMillis)
                                } else {
                                    errorText = "Invalid PIN"
                                    isChecking = false
                                }
                            }
                        }
                    },
                    enabled = !isChecking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Red600, contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            color = Color.White, modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = if (selectedDuration == "Indefinite") "Turn Off System" else "Confirm Pause",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                TextButton(
                    onClick = onDismiss, modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = "Cancel", color = Slate400, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
