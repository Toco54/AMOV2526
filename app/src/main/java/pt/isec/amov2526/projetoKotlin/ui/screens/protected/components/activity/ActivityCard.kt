package pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.isec.amov2526.projetoKotlin.data.model.ActivityAlertStatus
import pt.isec.amov2526.projetoKotlin.data.model.ActivityAlertType
import pt.isec.amov2526.projetoKotlin.data.model.ActivityItem

// --- CORES ---
private val Slate100 = Color(0xFFF1F5F9)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate400 = Color(0xFF94A3B8)
private val Slate500 = Color(0xFF64748B)
private val Slate900 = Color(0xFF0F172A)
private val Red100 = Color(0xFFFEE2E2)
private val Red700 = Color(0xFFB91C1C)
private val Amber50 = Color(0xFFFFFBEB)
private val Amber700 = Color(0xFFB45309)

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
        ActivityAlertType.BATTERY -> Icons.Default.BatteryAlert
    }
}
