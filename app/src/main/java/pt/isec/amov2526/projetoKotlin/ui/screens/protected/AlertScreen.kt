package pt.isec.amov2526.projetoKotlin.ui.screens.protected

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// --- Theme Colors ---
private val AlertRed = Color(0xFFDC2626)
private val DarkBackground = Color(0xFF0F172A)
private val PinBoxColor = Color(0xFF1E293B)
private val MapColor = Color(0xFFE2E8F0)
private val MapRoadColor = Color(0xFFCBD5E1)
private val TabSelectedColor = Color(0xFF334155)
private val TabUnselectedColor = Color(0xFF1E293B)

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

@Composable
fun CountdownPanel(
    alertType: String,
    pinInput: String,
    onPinDigit: (String) -> Unit,
    onPinDelete: () -> Unit,
    onTimerFinished: () -> Unit,
    onHelpNow: () -> Unit
) {
    var timeLeft by remember { mutableIntStateOf(10) }

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
            text = "$alertType DETECTED",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = AlertRed
        )

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
fun ConfirmedPanel() {
    var selectedCameraTab by remember { mutableIntStateOf(0) } // 0 = Back, 1 = Front

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Status Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AlertRed)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "HELP REQUESTED",
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        color = Color.White
                    )
                }
                Text(
                    text = "Monitors Notified • Recording Evidence",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Camera Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(TabUnselectedColor)
        ) {
            CameraTab(
                title = "BACK CAM",
                isSelected = selectedCameraTab == 0,
                onClick = { selectedCameraTab = 0 },
                modifier = Modifier.weight(1f)
            )
            CameraTab(
                title = "FRONT CAM",
                isSelected = selectedCameraTab == 1,
                onClick = { selectedCameraTab = 1 },
                modifier = Modifier.weight(1f)
            )
        }

        // 3. Video Feed Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                .background(Color.Black)
                .border(
                    width = 1.dp,
                    color = Color.Gray,
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Placeholder Content based on Tab
            if (selectedCameraTab == 0) {
                // Mock Back Camera View
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Back Camera Feed",
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 80.dp)
                )
            } else {
                // Mock Front Camera View
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Front Camera Feed",
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 80.dp)
                )
            }

            // Universal Recording Indicator (Always on top)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.Red, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "REC",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Map View
        Text(
            text = "Live Location",
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MapColor)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                drawLine(
                    color = MapRoadColor,
                    start = Offset(width * 0.6f, 0f),
                    end = Offset(width * 0.6f, height),
                    strokeWidth = 40f
                )
                drawLine(
                    color = MapRoadColor,
                    start = Offset(0f, height * 0.4f),
                    end = Offset(width, height * 0.4f),
                    strokeWidth = 40f
                )
            }
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "My Location",
                tint = AlertRed,
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
                    .offset(y = (-24).dp)
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.Center)
                    .background(AlertRed.copy(alpha = 0.3f), CircleShape)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                    .padding(4.dp)
            ) {
                Text(
                    text = "Coimbra, PT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Keep the app open. Help is coming.",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun CameraTab(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(if (isSelected) TabSelectedColor else Color.Transparent)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(AlertRed)
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
                                imageVector = Icons.Default.Backspace,
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
