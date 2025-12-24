package pt.isec.amov2526.projetoKotlin.ui.screens.protected

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

// Cores (Consistente com os outros ecrãs)
private val Slate50 = Color(0xFFF8FAFC)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate400 = Color(0xFF94A3B8)
private val Slate500 = Color(0xFF64748B)
private val Slate900 = Color(0xFF0F172A)
private val Red50 = Color(0xFFFEF2F2)
private val Red100 = Color(0xFFFEE2E2)
private val Red700 = Color(0xFFB91C1C)
private val Amber50 = Color(0xFFFFFBEB)
private val Amber100 = Color(0xFFFEF3C7)
private val Amber700 = Color(0xFFB45309)

// Mock Data Models
enum class ActivityAlertType { SPEED, FALL, GEOFENCE, PANIC }
enum class ActivityAlertStatus { CONFIRMED, CANCELLED, PENDING }

data class ActivityItem(
    val id: String,
    val type: ActivityAlertType,
    val title: String,
    val date: String,
    val time: String,
    val status: ActivityAlertStatus
)

// Mock Data List
private val mockActivities = listOf(
    ActivityItem("1", ActivityAlertType.SPEED, "Speed Alert", "23/12/2025", "21:36", ActivityAlertStatus.CONFIRMED),
    ActivityItem("2", ActivityAlertType.FALL, "Fall Detected", "22/12/2025", "14:20", ActivityAlertStatus.CANCELLED),
    ActivityItem("3", ActivityAlertType.GEOFENCE, "Left Safe Zone", "20/12/2025", "09:15", ActivityAlertStatus.CONFIRMED),
    ActivityItem("4", ActivityAlertType.PANIC, "SOS Triggered", "18/12/2025", "23:45", ActivityAlertStatus.PENDING),
    ActivityItem("5", ActivityAlertType.SPEED, "Speed Alert", "15/12/2025", "08:30", ActivityAlertStatus.CONFIRMED)
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
                ActivityCard(activity, onClick = { selectedActivity = activity })
            }
            item { 
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (selectedActivity != null) {
        AlertDetailModal(activity = selectedActivity!!, onDismiss = { selectedActivity = null })
    }
}

@Composable
fun ActivityCard(activity: ActivityItem, onClick: () -> Unit) {
    Card(
        onClick = onClick, // Ripple fix
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
            // Icon Background
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

            // Text Info
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

            // Status Chip
            StatusChip(activity.status)
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Details",
                tint = Slate200,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun AlertDetailModal(activity: ActivityItem, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        ) {
            Column {
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
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                    }
                }

                // Video Placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                .padding(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "EVENT RECORDING",
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

                // Location Section
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = Slate400, modifier = Modifier.size(16.dp))
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
                            .height(160.dp)
                            .background(Slate50, RoundedCornerShape(12.dp))
                            .border(1.dp, Slate200, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(1.dp)) {
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Slate200).align(Alignment.Center))
                            Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(Slate200).align(Alignment.Center))
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Red700,
                                modifier = Modifier.size(32.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .background(Color.White, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .shadow(elevation = 2.dp)
                            ) {
                                Text("Current Location", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate900)
                            }
                        }
                    }
                }

                // Info Footer
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                        .background(Slate50, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Status", fontSize = 12.sp, color = Slate500, fontWeight = FontWeight.Medium)
                        StatusChip(activity.status)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Alert Reference", fontSize = 12.sp, color = Slate500, fontWeight = FontWeight.Medium)
                        Text("#a1", fontSize = 12.sp, color = Slate400, fontWeight = FontWeight.Bold)
                    }
                }

                // Close Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(48.dp)
                        .background(Slate900, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp)) // Ripple fix
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Close Report", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: ActivityAlertStatus) {
    val backgroundColor = when (status) {
        ActivityAlertStatus.CONFIRMED -> Red100
        ActivityAlertStatus.PENDING -> Amber50
        ActivityAlertStatus.CANCELLED -> Slate100
    }
    
    val textColor = when (status) {
        ActivityAlertStatus.CONFIRMED -> Red700
        ActivityAlertStatus.PENDING -> Amber700
        ActivityAlertStatus.CANCELLED -> Slate500
    }

    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = status.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            fontSize = 10.sp
        )
    }
}

fun getIconForActivityType(type: ActivityAlertType): ImageVector {
    return when (type) {
        ActivityAlertType.SPEED -> Icons.Default.Speed
        ActivityAlertType.FALL -> Icons.Default.MonitorHeart
        ActivityAlertType.GEOFENCE -> Icons.Default.LocationOn
        ActivityAlertType.PANIC -> Icons.Default.Warning
    }
}