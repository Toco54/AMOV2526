package pt.isec.amov2526.projetoKotlin.ui.screens.monitor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import pt.isec.amov2526.projetoKotlin.R

// --- CORES ---
private val Slate50 = Color(0xFFF8FAFC)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate400 = Color(0xFF94A3B8)
private val Slate500 = Color(0xFF64748B)
private val Slate700 = Color(0xFF334155)
private val Slate900 = Color(0xFF0F172A)
private val Indigo600 = Color(0xFF4F46E5)
private val Emerald500 = Color(0xFF10B981)
private val Red50 = Color(0xFFFEF2F2)
private val Red600 = Color(0xFFDC2626)

// --- MODELOS ---
enum class RuleType(val title: String, val isMultiInstance: Boolean) {
    FALL("Fall", false),
    ACCIDENT("Accident", false),
    GEOFENCING("Geofencing", true),
    SPEED("Speed Control", true),
    INACTIVITY("Prolonged Inactivity", true)
}

data class ProtectionRule(
    val id: String,
    val type: RuleType,
    val icon: ImageVector,
    val iconColor: Color,
    var isEnabled: Boolean,
    var param1: String = "",
    var param2: String = ""
)

data class AlertStat(val label: String, val count: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorDashboardScreen(
    protegeName: String = "Bob Protected",
    onBack: () -> Unit = {}
) {
    var rules by remember {
        mutableStateOf(
            listOf(
                ProtectionRule(
                    id = "1",
                    type = RuleType.FALL,
                    icon = Icons.Default.MonitorHeart,
                    iconColor = Color(0xFFFF7043),
                    isEnabled = true
                ),
                ProtectionRule(
                    id = "2",
                    type = RuleType.GEOFENCING,
                    icon = Icons.Default.LocationOn,
                    iconColor = Color(0xFF7E57C2),
                    isEnabled = true,
                    param1 = "500",
                    param2 = "40.2033, -8.4103"
                )
            )
        )
    }

    var showAddRuleSelection by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<ProtectionRule?>(null) }
    
    var selectedActivity by remember { mutableStateOf<ActivityItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = protegeName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "LIVE DASHBOARD",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Indigo600,
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Slate900
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate50)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            AlertTrendsCard(
                stats = listOf(
                    AlertStat("Fall", 0),
                    AlertStat("Crash", 0),
                    AlertStat("Geo", 1),
                    AlertStat("Speed", 2),
                    AlertStat("Idle", 0),
                    AlertStat("SOS", 1)
                )
            )

            ActiveRulesManagementCard(
                rules = rules,
                onAddClick = { showAddRuleSelection = true },
                onEditClick = { editingRule = it },
                onToggleRule = { id, enabled ->
                    rules = rules.map {
                        if (it.id == id) it.copy(isEnabled = enabled) else it
                    }
                }
            )

            RecentActivityCard(
                activities = mockMonitorActivities,
                onActivityClick = { selectedActivity = it }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showAddRuleSelection) {
        AddRuleTypeDialog(
            existingRules = rules,
            onDismiss = { showAddRuleSelection = false },
            onTypeSelected = { type ->
                showAddRuleSelection = false
                editingRule = ProtectionRule(
                    id = "NEW_${java.util.UUID.randomUUID()}",
                    type = type,
                    icon = when (type) {
                        RuleType.FALL -> Icons.Default.MonitorHeart
                        RuleType.ACCIDENT -> Icons.Default.CarCrash
                        RuleType.GEOFENCING -> Icons.Default.LocationOn
                        RuleType.SPEED -> Icons.Default.Speed
                        else -> Icons.Default.Schedule
                    },
                    iconColor = when (type) {
                        RuleType.FALL -> Color(0xFFFF7043)
                        RuleType.ACCIDENT -> Color(0xFFEF4444)
                        RuleType.GEOFENCING -> Color(0xFF7E57C2)
                        RuleType.SPEED -> Color(0xFF42A5F5)
                        else -> Slate400
                    },
                    isEnabled = true
                )
            }
        )
    }

    if (editingRule != null) {
        val isNewRule = editingRule!!.id.startsWith("NEW_")
        EditRuleDialog(
            rule = editingRule!!,
            isNew = isNewRule,
            onDismiss = { editingRule = null },
            onSave = { updatedRule ->
                val finalRule = if (isNewRule) {
                    updatedRule.copy(id = java.util.UUID.randomUUID().toString())
                } else {
                    updatedRule
                }
                rules = if (!isNewRule) {
                    rules.map { if (it.id == finalRule.id) finalRule else it }
                } else {
                    rules + finalRule
                }
                editingRule = null
            },
            onDelete = { id ->
                rules = rules.filter { it.id != id }
                editingRule = null
            }
        )
    }

    if (selectedActivity != null) {
        MonitorAlertDetailModal(
            activity = selectedActivity!!,
            onDismiss = { selectedActivity = null }
        )
    }
}

@Composable
fun ActiveRulesManagementCard(
    rules: List<ProtectionRule>,
    onAddClick: () -> Unit,
    onEditClick: (ProtectionRule) -> Unit,
    onToggleRule: (String, Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_shield_check),
                        contentDescription = null,
                        tint = Emerald500,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Manage Protection Rules",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }
                IconButton(
                    onClick = onAddClick,
                    modifier = Modifier
                        .size(28.dp)
                        .background(Slate100, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = Slate900,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            rules.forEach { rule ->
                RuleManageItem(
                    rule = rule,
                    onEdit = { onEditClick(rule) },
                    onToggle = { onToggleRule(rule.id, it) }
                )
            }
        }
    }
}

@Composable
fun RecentActivityCard(
    activities: List<ActivityItem>,
    onActivityClick: (ActivityItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = Slate400,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Alert History",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                activities.forEach { activity ->
                    ActivityItemRow(
                        activity = activity,
                        onClick = { onActivityClick(activity) }
                    )
                }
            }
        }
    }
}

@Composable
fun ActivityItemRow(activity: ActivityItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate50),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate100)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White, CircleShape)
                    .border(1.dp, Slate100, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getMonitorIconForActivityType(activity.type),
                    contentDescription = null,
                    tint = Slate500,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    text = "${activity.date} • ${activity.time}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400
                )
            }
            MonitorStatusChip(activity.status)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Slate200,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun MonitorAlertDetailModal(activity: ActivityItem, onDismiss: () -> Unit) {
    var selectedCameraTab by remember { mutableIntStateOf(0) } // 0 = Rear, 1 = Front

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState()) // Allow scrolling
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFFFEF3C7), CircleShape), // Amber100
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getMonitorIconForActivityType(activity.type),
                                contentDescription = null,
                                tint = Color(0xFFB45309), // Amber700
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = activity.title.uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = "${activity.date} • ${activity.time}:53",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Slate400
                        )
                    }
                }

                // Camera Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .background(Slate700) // TabUnselected
                ) {
                    MonitorDetailCameraTab(
                        title = "REAR CAM",
                        isSelected = selectedCameraTab == 0,
                        onClick = { selectedCameraTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    MonitorDetailCameraTab(
                        title = "FRONT CAM",
                        isSelected = selectedCameraTab == 1,
                        onClick = { selectedCameraTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Video Feed Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(200.dp)
                        .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (selectedCameraTab == 0) Icons.Default.Videocam else Icons.Default.Face,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White.copy(alpha = 0.1f), CircleShape)
                                .padding(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedCameraTab == 0) "REAR RECORDING" else "FRONT RECORDING",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "00:30",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Content
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "INCIDENT LOCATION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(Slate50, RoundedCornerShape(12.dp))
                            .border(1.dp, Slate200, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Red600,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- NEW SECTION: NOTIFIED MONITORS ---
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NOTIFIED MONITORS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activity.notifiedMonitors.forEach { action ->
                            val (icon, color, label) = when (action.state) {
                                MonitorActionState.SENT -> Triple(Icons.Default.Notifications, Slate400, "Notified")
                                MonitorActionState.VIEWED -> Triple(Icons.Default.Visibility, Color(0xFF6366F1), "Viewed")
                                MonitorActionState.CALLED_USER -> Triple(Icons.Default.Phone, Color(0xFF6366F1), "Calling...")
                                MonitorActionState.CALLED_112 -> Triple(Icons.Default.LocalPolice, Red600, "Called 112")
                                MonitorActionState.ON_THE_WAY -> Triple(Icons.Default.DirectionsRun, Color(0xFFB45309), "On My Way")
                                MonitorActionState.MARKED_SAFE -> Triple(Icons.Default.CheckCircle, Emerald500, "Marked Safe")
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Slate50, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color.White, CircleShape)
                                        .border(1.dp, Slate200, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = action.name.first().toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate500
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = action.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900
                                    )
                                    Text(
                                        text = "$label • ${action.time}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = color,
                                        fontSize = 10.sp
                                    )
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = color,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Actions Column
                Column(modifier = Modifier.padding(16.dp)) {
                    // Row 1: Immediate Contact
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { /* TODO: Implement Call */ },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Call", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { /* TODO: Implement SOS */ },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Red600),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SOS 112", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row 2: Status Updates
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { /* TODO: Implement On My Way */ },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB45309)), // Amber700
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsRun,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "On My Way",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = { /* TODO: Implement Mark Safe */ },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Resolved",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate500)
                    ) {
                        Text("Close Report", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun MonitorDetailCameraTab(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(if (isSelected) Slate700 else Color.Transparent)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 11.sp
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color(0xFFDC2626)) // AlertRed
            )
        }
    }
}

@Composable
fun MonitorStatusChip(status: ActivityAlertStatus) {
    val (bg, txt) = when (status) {
        ActivityAlertStatus.CONFIRMED -> Red50 to Red600
        ActivityAlertStatus.PENDING -> Color(0xFFFFFBEB) to Color(0xFFB45309) // Amber50, Amber700
        ActivityAlertStatus.CANCELLED -> Slate100 to Slate500
    }
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = status.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = txt,
            fontSize = 10.sp
        )
    }
}

fun getMonitorIconForActivityType(type: ActivityAlertType): ImageVector {
    return when (type) {
        ActivityAlertType.SPEED -> Icons.Default.Speed
        ActivityAlertType.FALL -> Icons.Default.MonitorHeart
        ActivityAlertType.ACCIDENT -> Icons.Default.CarCrash
        ActivityAlertType.GEOFENCE -> Icons.Default.LocationOn
        ActivityAlertType.PANIC -> Icons.Default.Warning
    }
}

// --- Activity Data Classes (Mirrored from Protected for independence) ---
enum class ActivityAlertType { SPEED, FALL, ACCIDENT, GEOFENCE, PANIC }
enum class ActivityAlertStatus { CONFIRMED, CANCELLED, PENDING }
enum class MonitorActionState { SENT, VIEWED, CALLED_USER, CALLED_112, ON_THE_WAY, MARKED_SAFE }

data class MonitorAction(
    val name: String,
    val state: MonitorActionState,
    val time: String
)

data class ActivityItem(
    val id: String,
    val type: ActivityAlertType,
    val title: String,
    val date: String,
    val time: String,
    val status: ActivityAlertStatus,
    val notifiedMonitors: List<MonitorAction> = emptyList()
)

private val mockMonitorActivities = listOf(
    ActivityItem(
        "1",
        ActivityAlertType.SPEED,
        "Speed Alert",
        "23/12/2025",
        "21:36",
        ActivityAlertStatus.CONFIRMED,
        listOf(
            MonitorAction("Alice Monitor", MonitorActionState.CALLED_USER, "21:37"),
            MonitorAction("Bob Supervisor", MonitorActionState.VIEWED, "21:38")
        )
    ),
    ActivityItem(
        "2",
        ActivityAlertType.FALL,
        "Fall Detected",
        "22/12/2025",
        "14:20",
        ActivityAlertStatus.CANCELLED,
        listOf(
            MonitorAction("Alice Monitor", MonitorActionState.MARKED_SAFE, "14:22")
        )
    ),
    ActivityItem(
        "3",
        ActivityAlertType.GEOFENCE,
        "Safe Zone Exit",
        "20/12/2025",
        "09:15",
        ActivityAlertStatus.CONFIRMED,
        listOf(
            MonitorAction("Alice Monitor", MonitorActionState.ON_THE_WAY, "09:20"),
            MonitorAction("Charlie Guard", MonitorActionState.SENT, "09:15")
        )
    ),
    ActivityItem(
        "4",
        ActivityAlertType.PANIC,
        "SOS Activated",
        "18/12/2025",
        "23:45",
        ActivityAlertStatus.PENDING,
        listOf(
            MonitorAction("Alice Monitor", MonitorActionState.CALLED_112, "23:46"),
            MonitorAction("Bob Supervisor", MonitorActionState.ON_THE_WAY, "23:50"),
            MonitorAction("Charlie Guard", MonitorActionState.VIEWED, "23:47")
        )
    )
)


@Composable
fun RuleManageItem(
    rule: ProtectionRule,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    val subtitle = when (rule.type) {
        RuleType.FALL, RuleType.ACCIDENT -> "ALWAYS ACTIVE"
        RuleType.GEOFENCING -> if (rule.param1.isEmpty()) "PENDING" else "RADIUS: ${rule.param1}M"
        RuleType.SPEED -> if (rule.param1.isEmpty()) "PENDING" else "LIMIT: ${rule.param1}KM/H"
        RuleType.INACTIVITY -> if (rule.param1.isEmpty()) "PENDING" else "LIMIT: ${rule.param1}MIN"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(
                color = if (rule.isEnabled) Slate50 else Slate50.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = if (rule.isEnabled) Slate100 else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, Slate100, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = rule.icon,
                contentDescription = null,
                tint = rule.iconColor.copy(alpha = if (rule.isEnabled) 1f else 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = rule.type.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (rule.isEnabled) Slate900 else Slate400
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Slate400,
                letterSpacing = 0.5.sp
            )
        }
        IconButton(
            onClick = onEdit,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = Slate400,
                modifier = Modifier.size(18.dp)
            )
        }
        Switch(
            checked = rule.isEnabled,
            onCheckedChange = onToggle,
            modifier = Modifier.scale(0.7f),
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
fun AddRuleTypeDialog(
    existingRules: List<ProtectionRule>,
    onDismiss: () -> Unit,
    onTypeSelected: (RuleType) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Protection Type",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Spacer(modifier = Modifier.height(16.dp))
                RuleType.entries.forEach { type ->
                    val isAlreadyDefined = !type.isMultiInstance && existingRules.any { it.type == type }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                color = if (isAlreadyDefined) Slate100 else Slate50,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isAlreadyDefined) { onTypeSelected(type) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = type.title,
                            fontWeight = FontWeight.Bold,
                            color = if (isAlreadyDefined) Slate400 else Slate700
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (isAlreadyDefined) {
                            Text(
                                text = "DEFINED",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate400
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Slate400
                            )
                        }
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 16.dp)
                ) {
                    Text(
                        text = "Cancel",
                        color = Slate400,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun EditRuleDialog(
    rule: ProtectionRule,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (ProtectionRule) -> Unit,
    onDelete: (String) -> Unit
) {
    var p1 by remember { mutableStateOf(rule.param1) }
    var p2 by remember { mutableStateOf(rule.param2) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(rule.iconColor.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = rule.icon,
                            contentDescription = null,
                            tint = rule.iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = rule.type.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))

                if (rule.type == RuleType.GEOFENCING) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Slate100),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawLine(
                                color = Color(0xFFCBD5E1),
                                start = Offset(size.width * 0.6f, 0f),
                                end = Offset(size.width * 0.6f, size.height),
                                strokeWidth = 20f
                            )
                            drawLine(
                                color = Color(0xFFCBD5E1),
                                start = Offset(0f, size.height * 0.4f),
                                end = Offset(size.width, size.height * 0.4f),
                                strokeWidth = 20f
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(rule.iconColor.copy(alpha = 0.2f), CircleShape)
                                .border(2.dp, rule.iconColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = rule.iconColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "GPS COORDINATES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate400
                    )
                    TextField(
                        value = p2,
                        onValueChange = { p2 = it },
                        placeholder = { Text("0.0000, 0.0000") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Slate50,
                            unfocusedContainerColor = Slate50
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (rule.type.isMultiInstance) {
                    val label = when (rule.type) {
                        RuleType.GEOFENCING -> "RADIUS (METERS)"
                        RuleType.SPEED -> "MAX SPEED (KM/H)"
                        else -> "TIME LIMIT (MINUTES)"
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate400
                    )
                    TextField(
                        value = p1,
                        onValueChange = { p1 = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Slate50,
                            unfocusedContainerColor = Slate50
                        )
                    )
                } else {
                    Text(
                        text = "This rule automatically detects events and notifies you immediately. No extra parameters required.",
                        color = Slate500,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Botão Esquerdo: Cancelar (se novo) ou Eliminar (se existente)
                    Button(
                        onClick = { if (isNew) onDismiss() else onDelete(rule.id) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isNew) Slate100 else Red600,
                            contentColor = if (isNew) Slate500 else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isNew) "Cancel" else "Delete",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Botão Direito: Sempre visível se for novo ou se tiver parâmetros
                    if (isNew || rule.type.isMultiInstance) {
                        Button(
                            onClick = { onSave(rule.copy(param1 = p1, param2 = p2, isEnabled = true)) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Indigo600,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isNew) "Add" else "Save",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlertTrendsCard(stats: List<AlertStat>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = Slate400,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Alert Trends",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("4", "3", "2", "1", "0").forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400,
                            fontSize = 10.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        repeat(5) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Slate100)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        stats.forEach { stat ->
                            val barHeightFactor = stat.count / 4f
                            Box(
                                modifier = Modifier
                                    .width(16.dp)
                                    .fillMaxHeight(barHeightFactor.coerceAtLeast(0.02f))
                                    .background(
                                        color = Emerald500,
                                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                stats.forEach { stat ->
                    Text(
                        text = stat.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500,
                        textAlign = TextAlign.Center,
                        fontSize = 9.sp,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }
        }
    }
}
