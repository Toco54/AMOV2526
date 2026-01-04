package pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.alert

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// --- Theme Colors ---
private val AlertRed = Color(0xFFDC2626)
private val PinBoxColor = Color(0xFF1E293B)

@Composable
fun CountdownPanel(
    alertType: String,
    alertDetails: String? = null, // [NOVO]
    pinInput: String,
    initialSeconds: Int = 10, 
    onPinDigit: (String) -> Unit,
    onPinDelete: () -> Unit,
    onTimerFinished: () -> Unit,
    onHelpNow: () -> Unit
) {
    var timeLeft by remember { mutableIntStateOf(initialSeconds) }

    // [NOVO] Formatação do Título
    val displayTitle = remember(alertType) {
        when (alertType) {
            "FALL" -> "FALL DETECTED"
            "ACCIDENT" -> "ACCIDENT DETECTED"
            "GEOFENCE" -> "SAFE ZONE EXIT"
            "SPEED" -> "HIGH SPEED"
            "INACTIVITY" -> "INACTIVITY ALERT"
            "BATTERY" -> "LOW BATTERY"
            "PANIC" -> "SOS ALERT"
            else -> "$alertType ALERT"
        }
    }
    
    // [CORREÇÃO] Remove redundância se os detalhes começarem com o nome do alerta (vários idiomas)
    val cleanDetails = remember(alertDetails, alertType) {
        if (alertDetails == null) return@remember null
        
        // Lista de prefixos possíveis vindos do Service (SafetyMonitorService.kt)
        val prefixes = listOf(
            "Queda Detetada", "Acidente de Viação", "Bateria Fraca", 
            "Saiu da Zona Segura", "Velocidade Excessiva", "Inatividade Detetada", "Alerta Automático",
            // Inglês (caso futuro)
            "Fall Detected", "Car Accident", "Low Battery", "Safe Zone Exit", "High Speed", "Inactivity Detected"
        )
        
        var d = alertDetails
        for (p in prefixes) {
            if (d?.startsWith(p, ignoreCase = true) == true) {
                d = d?.removePrefix(p)?.trim()?.removePrefix(":")?.trim()
            }
        }
        if (d.isNullOrEmpty()) null else d
    }

    LaunchedEffect(timeLeft) {
        if (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        } else {
            onTimerFinished()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = displayTitle, // Usa o título formatado
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = AlertRed
        )

        // [NOVO] Mostrar detalhes se existirem
        if (cleanDetails != null) {
            Text(
                text = cleanDetails,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 20.dp)
        ) {
            CircularProgressIndicator(
                progress = { timeLeft / 10f },
                modifier = Modifier.size(100.dp),
                color = AlertRed,
                strokeWidth = 8.dp,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
            Text(
                text = "$timeLeft",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Text(
            text = "Enter PIN to Cancel",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        PinDotsDisplay(pinInput)
        Spacer(modifier = Modifier.height(24.dp))
        SecurityKeypad(
            onDigitClick = onPinDigit,
            onDeleteClick = onPinDelete
        )
        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onHelpNow,
            colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "HELP NOW",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
    }
}

@Composable
fun PinDotsDisplay(input: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(4) { index ->
            val isFilled = index < input.length
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = if (isFilled) Color.White else PinBoxColor,
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = if (isFilled) Color.White else Color.Gray,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
fun SecurityKeypad(
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "DEL")
    )

    val buttonSize = 72.dp

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        keys.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                row.forEach { key ->
                    if (key.isEmpty()) {
                        Spacer(modifier = Modifier.size(buttonSize))
                    } else if (key == "DEL") {
                        Box(
                            modifier = Modifier
                                .size(buttonSize)
                                .clickable { onDeleteClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Delete",
                                tint = Color.White
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(buttonSize)
                                .clip(CircleShape)
                                .background(PinBoxColor)
                                .clickable { onDigitClick(key) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = key,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
