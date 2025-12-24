package pt.isec.amov2526.projetoKotlin.ui.screens.protected

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import pt.isec.amov2526.projetoKotlin.R

// Paleta de Cores baseada no Mockup
private val Slate50 = Color(0xFFF8FAFC)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate400 = Color(0xFF94A3B8)
private val Slate500 = Color(0xFF64748B)
private val Slate700 = Color(0xFF334155)
private val Slate900 = Color(0xFF0F172A)
private val Emerald50 = Color(0xFFECFDF5)
private val Emerald200 = Color(0xFFA7F3D0)
private val Emerald500 = Color(0xFF10B981)
private val Emerald700 = Color(0xFF047857)
private val Red100 = Color(0xFFFEE2E2)
private val Red50 = Color(0xFFFEF2F2)
private val Red500 = Color(0xFFEF4444)
private val Red600 = Color(0xFFDC2626)
private val Amber500 = Color(0xFFF59E0B)

@Composable
fun GuardScreen(
    onPanic: () -> Unit = {}
) {
    val isActive = remember { mutableStateOf(true) }
    var showPauseModal by remember { mutableStateOf(false) }
    var showScheduleModal by remember { mutableStateOf(false) }
    var selectedRuleToPause by remember { mutableStateOf<String?>(null) }

    val openPauseModal = { ruleName: String? ->
        selectedRuleToPause = ruleName
        showPauseModal = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- System Status Card ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isActive.value) Color.White else Slate100
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(24.dp),
            border = if (!isActive.value) androidx.compose.foundation.BorderStroke(1.dp, Slate200) else null
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SYSTEM STATUS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Schedule Button
                        Box(
                            modifier = Modifier
                                .border(1.dp, Slate200, RoundedCornerShape(20.dp))
                                .background(Color.White, RoundedCornerShape(20.dp))
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { showScheduleModal = true }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = Slate500,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "SCHEDULE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate500,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Toggle Active Button
                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .border(
                                    width = 1.dp,
                                    color = if (isActive.value) Emerald200 else Slate200,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .background(
                                    color = if (isActive.value) Emerald50 else Color.White,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    if (isActive.value) openPauseModal(null)
                                    else isActive.value = true
                                }
                                .padding(start = 8.dp, end = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                Text(
                                    text = if (isActive.value) "ACTIVE" else "OFFLINE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isActive.value) Emerald700 else Slate400,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(width = 32.dp, height = 16.dp)
                                        .background(
                                            color = if (isActive.value) Emerald500 else Slate200,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .align(if (isActive.value) Alignment.CenterEnd else Alignment.CenterStart)
                                            .padding(if (isActive.value) PaddingValues(end = 2.dp) else PaddingValues(start = 2.dp))
                                            .background(Color.White, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Status Content
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                color = if (isActive.value) Slate900 else Slate200,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isActive.value) Color.White else Slate500,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = if (isActive.value) "Protected" else "Monitoring Disabled",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isActive.value) Slate900 else Slate500,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isActive.value) "All sensors active" else "Tap switch to resume",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate500
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // --- SOS Button ---
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.95f else 1.0f,
            label = "scale"
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .scale(scale)
                .size(260.dp)
                .background(Color.Transparent)
        ) {
            // Outer Ring
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Red100, CircleShape)
            )

            // Main Button
            Box(
                modifier = Modifier
                    .fillMaxSize(0.92f)
                    .align(Alignment.Center)
                    .shadow(
                        elevation = 20.dp,
                        shape = CircleShape,
                        ambientColor = Red500,
                        spotColor = Red600
                    )
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Red500, Red600)
                        )
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onPanic() },
                contentAlignment = Alignment.Center
            ) {
                // Inner ripple decoration (static)
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "SOS",
                        tint = Color.White,
                        modifier = Modifier
                            .size(56.dp)
                            .padding(bottom = 4.dp)
                    )

                    Text(
                        text = "SOS",
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = Color.Black.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "EMERGENCY",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // --- Active Protections Section ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header with icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_shield_check),
                        contentDescription = null,
                        tint = Emerald500,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Active Protections",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Protection Items List
                ProtectionRowItem(
                    title = "Fall",
                    subtitle = "ALWAYS ACTIVE",
                    icon = Icons.Default.MonitorHeart,
                    iconColor = Color(0xFFFF7043),
                    isEnabled = true,
                    onScheduleClick = { showScheduleModal = true },
                    onToggle = { openPauseModal("Fall Detection") }
                )

                ProtectionRowItem(
                    title = "Geofence",
                    subtitle = "RADIUS: 500M",
                    icon = Icons.Default.LocationOn,
                    iconColor = Color(0xFF7E57C2),
                    isEnabled = true,
                    onScheduleClick = { showScheduleModal = true },
                    onToggle = { openPauseModal("Geofencing") }
                )

                ProtectionRowItem(
                    title = "Inactivity",
                    subtitle = "TIME LIMIT: 60 MINS",
                    icon = Icons.Default.Schedule,
                    iconColor = Slate400,
                    isEnabled = false,
                    onScheduleClick = { showScheduleModal = true },
                    onToggle = { openPauseModal("Inactivity") }
                )

                ProtectionRowItem(
                    title = "Speed",
                    subtitle = "LIMIT: 120 KM/H",
                    icon = Icons.Default.Speed,
                    iconColor = Color(0xFF42A5F5),
                    isEnabled = true,
                    onScheduleClick = { showScheduleModal = true },
                    onToggle = { openPauseModal("Speed Limit") }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // --- MODALS ---
    if (showPauseModal) {
        PauseSystemModal(
            ruleName = selectedRuleToPause,
            onDismiss = { showPauseModal = false },
            onConfirm = {
                if (selectedRuleToPause == null) isActive.value = false
                showPauseModal = false
            }
        )
    }

    if (showScheduleModal) {
        ScheduleSystemModal(
            onDismiss = { showScheduleModal = false },
            onSave = { showScheduleModal = false }
        )
    }
}

@Composable
fun ProtectionRowItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    isEnabled: Boolean,
    onScheduleClick: () -> Unit,
    onToggle: () -> Unit
) {
    val checked = remember { mutableStateOf(isEnabled) }
    val bgColor = if (checked.value) Slate50 else Slate50.copy(alpha = 0.5f)
    val borderColor = if (checked.value) Slate100 else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(bgColor, RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconOpacity = if (checked.value) 1f else 0.5f
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, Slate100, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor.copy(alpha = iconOpacity),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (checked.value) Slate900 else Slate400
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Slate400,
                letterSpacing = 0.5.sp
            )
        }

        Icon(
            imageVector = Icons.Default.AccessTime,
            contentDescription = "Schedule",
            tint = if (checked.value) Slate400 else Slate200,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable { onScheduleClick() }
                .padding(4.dp)
        )

        Spacer(modifier = Modifier.width(4.dp))

        Switch(
            checked = checked.value,
            onCheckedChange = {
                if (checked.value) onToggle()
                else checked.value = true
            },
            modifier = Modifier.scale(0.8f),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Emerald500,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Slate200,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun PauseSystemModal(
    ruleName: String? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var pin by remember { mutableStateOf("") }

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
                        text = if (ruleName == null) "Pause System" else "Pause $ruleName",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }

                Text(
                    text = if (ruleName == null) "Select duration to pause protections."
                    else "Select duration to disable this rule.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Text(
                    text = "ENTER SAFETY PIN",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate400
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Slate50, RoundedCornerShape(12.dp))
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        decorationBox = { innerTextField ->
                            if (pin.isEmpty()) Text("****", style = MaterialTheme.typography.titleLarge, color = Slate200)
                            innerTextField()
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = Slate900,
                            letterSpacing = 8.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                val options = listOf(
                    "15 Minutes" to Icons.Default.Timer,
                    "30 Minutes" to Icons.Default.Timer,
                    "1 Hour" to Icons.Default.Timer,
                    "8 Hours" to Icons.Default.Timer,
                    "Disable Indefinitely" to Icons.Default.Close
                )

                options.forEach { (label, icon) ->
                    val isDestructive = label.contains("Indefinitely")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                color = if (isDestructive) Red50 else Slate50,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { if (pin.length == 4) onConfirm() }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            fontWeight = FontWeight.Bold,
                            color = if (isDestructive) Red600 else Slate700
                        )
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isDestructive) Red600 else Slate400,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = Slate400, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ScheduleSystemModal(
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    val days = listOf("S", "M", "T", "W", "T", "F", "S")
    val selectedDays = remember { mutableStateListOf(1, 2, 3, 4, 5) }

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
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "System Schedule",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }

                Text(
                    text = "ACTIVE DAYS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate400
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    days.forEachIndexed { index, day ->
                        val isSelected = selectedDays.contains(index)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = if (isSelected) Slate900 else Slate100,
                                    shape = CircleShape
                                )
                                .clip(CircleShape)
                                .clickable {
                                    if (isSelected) selectedDays.remove(index)
                                    else selectedDays.add(index)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day,
                                color = if (isSelected) Color.White else Slate400,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "START TIME",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate400
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Slate50, RoundedCornerShape(12.dp))
                                .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "09:00",
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "END TIME",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate400
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Slate50, RoundedCornerShape(12.dp))
                                .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "18:00",
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "ENTER SAFETY PIN TO SAVE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate400
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Slate50, RoundedCornerShape(12.dp))
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        decorationBox = { innerTextField ->
                            if (pin.isEmpty()) Text("****", style = MaterialTheme.typography.titleLarge, color = Slate200)
                            innerTextField()
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = Slate900,
                            letterSpacing = 8.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .background(Slate100, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cancel", color = Slate500, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .background(Slate900, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { if (pin.length == 4) onSave() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Save Plan", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}