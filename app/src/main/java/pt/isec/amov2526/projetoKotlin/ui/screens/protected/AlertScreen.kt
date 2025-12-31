package pt.isec.amov2526.projetoKotlin.ui.screens.protected

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.alert.ConfirmedPanel
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.alert.CountdownPanel

// --- Theme Colors ---
private val DarkBackground = Color(0xFF0F172A)

@Composable
fun AlertScreen(
    alertType: String = "SOS",
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    var isConfirmed by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    val correctPin = "1234"

    LaunchedEffect(pinInput) {
        if (!isConfirmed && pinInput == correctPin) {
            onCancel()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        if (!isConfirmed) {
            CountdownPanel(
                alertType = alertType,
                pinInput = pinInput,
                onPinDigit = { digit ->
                    if (pinInput.length < 4) pinInput += digit
                },
                onPinDelete = {
                    if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1)
                },
                onTimerFinished = {
                    isConfirmed = true
                    onConfirm()
                },
                onHelpNow = {
                    isConfirmed = true
                    onConfirm()
                }
            )
        } else {
            ConfirmedPanel()
        }
    }
}