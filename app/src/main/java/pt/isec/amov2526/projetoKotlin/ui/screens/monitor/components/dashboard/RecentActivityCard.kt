package pt.isec.amov2526.projetoKotlin.ui.screens.monitor.components.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import pt.isec.amov2526.projetoKotlin.data.model.*

private val Slate50 = Color(0xFFF8FAFC)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate400 = Color(0xFF94A3B8)
private val Slate500 = Color(0xFF64748B)
private val Slate700 = Color(0xFF334155)
private val Slate900 = Color(0xFF0F172A)
private val Red50 = Color(0xFFFEF2F2)
private val Red600 = Color(0xFFDC2626)

@Composable
fun RecentActivityCard(activities: List<ActivityItem>, onActivityClick: (ActivityItem) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, null, tint = Slate400, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Alert History", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate900)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                activities.forEach { activity -> ActivityItemRow(activity) { onActivityClick(activity) } }
            }
        }
    }
}

@Composable
fun ActivityItemRow(activity: ActivityItem, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Slate50), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Slate100)) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(Color.White, CircleShape).border(1.dp, Slate100, CircleShape), contentAlignment = Alignment.Center) {
                Icon(getMonitorIconForActivityType(activity.type), null, tint = Slate500, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(activity.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Slate900)
                Text("${activity.date} • ${activity.time}", style = MaterialTheme.typography.labelSmall, color = Slate400)
            }
            MonitorStatusChip(activity.status)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Slate200, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
fun MonitorAlertDetailModal(activity: ActivityItem, onDismiss: () -> Unit) {
    var selectedCameraTab by remember { mutableIntStateOf(0) }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp).background(Color(0xFFFEF3C7), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(getMonitorIconForActivityType(activity.type), null, tint = Color(0xFFB45309), modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(activity.title.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Slate900)
                            Text("${activity.date} • ${activity.time}:53", style = MaterialTheme.typography.bodySmall, color = Slate500)
                        }
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Slate400) }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(Slate700)) {
                    MonitorDetailCameraTab("REAR CAM", selectedCameraTab == 0, { selectedCameraTab = 0 }, Modifier.weight(1f))
                    MonitorDetailCameraTab("FRONT CAM", selectedCameraTab == 1, { selectedCameraTab = 1 }, Modifier.weight(1f))
                }
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(200.dp).clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(if (selectedCameraTab == 0) Icons.Default.Videocam else Icons.Default.Face, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.1f), CircleShape).padding(12.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(if (selectedCameraTab == 0) "REAR RECORDING" else "FRONT RECORDING", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, null, tint = Slate400, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("NOTIFIED MONITORS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 1.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        activity.notifiedMonitors.forEach { action ->
                            Row(modifier = Modifier.fillMaxWidth().background(Slate50, RoundedCornerShape(8.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(32.dp).background(Color.White, CircleShape).border(1.dp, Slate200, CircleShape), contentAlignment = Alignment.Center) {
                                    Text(action.name.first().toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Slate500)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(action.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Slate900)
                                    Text("Viewed • ${action.time}", style = MaterialTheme.typography.labelSmall, color = Slate400, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Slate200), colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate500)) {
                        Text("Close Report", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun MonitorDetailCameraTab(title: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Box(modifier = modifier.clickable(onClick = onClick).background(if (isSelected) Slate700 else Color.Transparent).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Text(title, color = if (isSelected) Color.White else Color.Gray, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp)
        if (isSelected) Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp).background(Color(0xFFDC2626)))
    }
}

@Composable
fun MonitorStatusChip(status: ActivityAlertStatus) {
    val (bg, txt) = when (status) {
        ActivityAlertStatus.CONFIRMED -> Red50 to Red600
        ActivityAlertStatus.PENDING -> Color(0xFFFFFBEB) to Color(0xFFB45309)
        ActivityAlertStatus.CANCELLED -> Slate100 to Slate500
    }
    Box(modifier = Modifier.background(bg, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text(status.name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = txt, fontSize = 10.sp)
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
