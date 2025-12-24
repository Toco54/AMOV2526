package pt.isec.amov2526.projetoKotlin.ui.screens.protected

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

// --- CORES ---
private val Slate50 = Color(0xFFF8FAFC)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate400 = Color(0xFF94A3B8)
private val Slate500 = Color(0xFF64748B)
private val Slate900 = Color(0xFF0F172A)
private val Red100 = Color(0xFFFEE2E2)
private val Red700 = Color(0xFFB91C1C)
private val Amber50 = Color(0xFFFFFBEB)
private val Amber100 = Color(0xFFFEF3C7)
private val Amber700 = Color(0xFFB45309)
private val TabSelectedColor = Color(0xFF334155)
private val TabUnselectedColor = Color(0xFF1E293B)

enum class ActivityAlertType {

    SPEED,

    FALL,

    ACCIDENT,

    GEOFENCE,

    PANIC

}
enum class ActivityAlertStatus {
    CONFIRMED,
    CANCELLED,
    PENDING
}
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

private val mockActivities = listOf(
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
fun ActivityScreen() {
    var selectedActivity by remember { mutableStateOf<ActivityItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Alert History",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Slate900,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(mockActivities) { activity ->
                ActivityCard(
                    activity = activity,
                    onClick = { selectedActivity = activity }
                )
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (selectedActivity != null) {
        AlertDetailModal(
            activity = selectedActivity!!,
            onDismiss = { selectedActivity = null }
        )
    }
}

@Composable
fun ActivityCard(activity: ActivityItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Slate100, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconForActivityType(activity.type),
                    contentDescription = null,
                    tint = Slate500,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Slate400
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${activity.date} • ${activity.time}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500
                    )
                }
            }
            StatusChip(activity.status)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Slate200,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun AlertDetailModal(activity: ActivityItem, onDismiss: () -> Unit) {
    var selectedCameraTab by remember { mutableIntStateOf(0) } // 0 = Rear, 1 = Front

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                                .background(Amber100, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getIconForActivityType(activity.type),
                                contentDescription = null,
                                tint = Amber700,
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
                        .background(TabUnselectedColor)
                ) {
                    DetailCameraTab(
                        title = "REAR CAM",
                        isSelected = selectedCameraTab == 0,
                        onClick = { selectedCameraTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    DetailCameraTab(
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
                            tint = Red700,
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
                                MonitorActionState.SENT -> Triple(
                                    Icons.Default.Notifications,
                                    Slate400,
                                    "Notified"
                                )
                                MonitorActionState.VIEWED -> Triple(
                                    Icons.Default.Visibility,
                                    Color(0xFF6366F1),
                                    "Viewed"
                                )
                                MonitorActionState.CALLED_USER -> Triple(
                                    Icons.Default.Phone,
                                    Color(0xFF6366F1),
                                    "Calling..."
                                )
                                MonitorActionState.CALLED_112 -> Triple(
                                    Icons.Default.LocalPolice,
                                    Red700,
                                    "Called 112"
                                )
                                MonitorActionState.ON_THE_WAY -> Triple(
                                    Icons.Default.DirectionsRun,
                                    Amber700,
                                    "On My Way"
                                )
                                MonitorActionState.MARKED_SAFE -> Triple(
                                    Icons.Default.CheckCircle,
                                    Color(0xFF10B981),
                                    "Marked Safe"
                                )
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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(48.dp)
                        .background(Slate900, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Close Report",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun DetailCameraTab(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(if (isSelected) TabSelectedColor else Color.Transparent)
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
                    .background(Color.Red)
            )
        }
    }
}

@Composable
fun StatusChip(status: ActivityAlertStatus) {
    val (bg, txt) = when (status) {
        ActivityAlertStatus.CONFIRMED -> {
            Red100 to Red700
        }
        ActivityAlertStatus.PENDING -> {
            Amber50 to Amber700
        }
        ActivityAlertStatus.CANCELLED -> {
            Slate100 to Slate500
        }
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

fun getIconForActivityType(type: ActivityAlertType): ImageVector {
    return when (type) {
        ActivityAlertType.SPEED -> Icons.Default.Speed
        ActivityAlertType.FALL -> Icons.Default.MonitorHeart
        ActivityAlertType.ACCIDENT -> Icons.Default.CarCrash
        ActivityAlertType.GEOFENCE -> Icons.Default.LocationOn
        ActivityAlertType.PANIC -> Icons.Default.Warning
    }
}