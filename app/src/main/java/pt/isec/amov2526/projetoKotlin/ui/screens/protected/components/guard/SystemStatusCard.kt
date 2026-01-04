package pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.guard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.isec.amov2526.projetoKotlin.utils.StatusResult

// --- CORES ---
private val Slate100 = Color(0xFFF1F5F9)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate400 = Color(0xFF94A3B8)
private val Slate500 = Color(0xFF64748B)
private val Slate900 = Color(0xFF0F172A)
private val Emerald50 = Color(0xFFECFDF5)
private val Emerald200 = Color(0xFFA7F3D0)
private val Emerald500 = Color(0xFF10B981)
private val Emerald700 = Color(0xFF047857)
private val Amber500 = Color(0xFFF59E0B)
private val Indigo600 = Color(0xFF4F46E5)

@Composable
fun SystemStatusCard(
    isActive: Boolean,
    isGlobalScheduleEnabled: Boolean,
    globalScheduleStatus: pt.isec.amov2526.projetoKotlin.utils.StatusResult,
    rawSystemStatus: String,
    batteryLevel: Int,
    onPlanClick: () -> Unit,
    onStatusClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive && globalScheduleStatus.isActive) Color.White else Slate100
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(24.dp),
        border = if (!isActive || !globalScheduleStatus.isActive) androidx.compose.foundation.BorderStroke(1.dp, Slate200) else null
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(id = pt.isec.amov2526.projetoKotlin.R.string.monitor_current_status),
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isScheduleActiveNow = globalScheduleStatus.label == "SCHEDULED"
                    
                    Box(
                        modifier = Modifier
                            .border(
                                1.dp, 
                                if (isScheduleActiveNow) Indigo600
                                else if (isGlobalScheduleEnabled) Indigo600.copy(alpha = 0.2f) 
                                else Slate200, 
                                RoundedCornerShape(20.dp)
                            )
                            .background(
                                if (isScheduleActiveNow) Indigo600
                                else if (isGlobalScheduleEnabled) Indigo600.copy(alpha = 0.1f) 
                                else Color.White, 
                                RoundedCornerShape(20.dp)
                            )
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onPlanClick() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = if (isScheduleActiveNow) Color.White else if (isGlobalScheduleEnabled) Indigo600 else Slate500,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PLAN",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isScheduleActiveNow) Color.White else if (isGlobalScheduleEnabled) Indigo600 else Slate500,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BatteryFull,
                            contentDescription = "Battery",
                            tint = if (batteryLevel < 20) Color.Red else Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "$batteryLevel%",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (batteryLevel < 20) Color.Red else Slate400,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                    // Status Pill
                    // [CORREÇÃO] Se estiver "SCHEDULED", o switch manual ainda deve aparecer como "ACTIVE" (Verde),
                    // porque o utilizador não o pausou manualmente. Eles não estão relacionados diretamente no switch.
                    val statusLabel = if (rawSystemStatus == "SCHEDULED") "ACTIVE" else rawSystemStatus
                    
                    val statusColor = when (statusLabel) {
                        "ACTIVE" -> Emerald700
                        "PAUSED" -> Amber500
                        else -> Slate400
                    }
                    val statusBg = when (statusLabel) {
                        "ACTIVE" -> Emerald50
                        "PAUSED" -> Color(0xFFFFFBEB) // Amber50
                        else -> Slate200
                    }
                    val statusBorder = when (statusLabel) {
                        "ACTIVE" -> Emerald200
                        "PAUSED" -> Color(0xFFFDE68A) // Amber200
                        else -> Slate200
                    }

                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .border(width = 1.dp, color = statusBorder, shape = RoundedCornerShape(20.dp))
                            .background(color = statusBg, shape = RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onStatusClick() }
                            .padding(start = 8.dp, end = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Text(
                                text = when(statusLabel) {
                                    "ACTIVE" -> androidx.compose.ui.res.stringResource(id = pt.isec.amov2526.projetoKotlin.R.string.status_active)
                                    "PAUSED" -> androidx.compose.ui.res.stringResource(id = pt.isec.amov2526.projetoKotlin.R.string.status_paused)
                                    "OFFLINE" -> androidx.compose.ui.res.stringResource(id = pt.isec.amov2526.projetoKotlin.R.string.status_offline)
                                    else -> statusLabel
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(width = 32.dp, height = 16.dp)
                                    .background(
                                        color = if (statusLabel == "ACTIVE") Emerald500 else if (statusLabel == "PAUSED") Amber500 else Slate400,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .align(if (statusLabel == "ACTIVE") Alignment.CenterEnd else Alignment.CenterStart)
                                        .padding(2.dp)
                                        .background(Color.White, CircleShape)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            color = if (isActive && globalScheduleStatus.isActive) Slate900 else Slate200,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (!globalScheduleStatus.isActive) Icons.Default.Schedule else Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (isActive && globalScheduleStatus.isActive) Color.White else Slate500,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    val titleText = when (rawSystemStatus) {
                        "PAUSED" -> androidx.compose.ui.res.stringResource(id = pt.isec.amov2526.projetoKotlin.R.string.status_paused)
                        "OFFLINE" -> androidx.compose.ui.res.stringResource(id = pt.isec.amov2526.projetoKotlin.R.string.status_offline)
                        "SCHEDULED" -> androidx.compose.ui.res.stringResource(id = pt.isec.amov2526.projetoKotlin.R.string.status_scheduled)
                        else -> androidx.compose.ui.res.stringResource(id = pt.isec.amov2526.projetoKotlin.R.string.auth_protected)
                    }

                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isActive && globalScheduleStatus.isActive) Slate900 else Slate500,
                        fontWeight = FontWeight.Bold
                    )

                    val subText = when {
                        globalScheduleStatus.label == "PAUSED" -> globalScheduleStatus.subLabel ?: androidx.compose.ui.res.stringResource(id = pt.isec.amov2526.projetoKotlin.R.string.status_paused)
                        !isActive -> androidx.compose.ui.res.stringResource(id = pt.isec.amov2526.projetoKotlin.R.string.guard_no_protections)
                        !globalScheduleStatus.isActive -> globalScheduleStatus.subLabel ?: "Outside active hours"
                        else -> globalScheduleStatus.subLabel ?: "Active security by multiple monitors"
                    }

                    Text(
                        text = subText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (!globalScheduleStatus.isActive || globalScheduleStatus.label == "PAUSED") Amber500 else Slate500,
                        fontWeight = if (!globalScheduleStatus.isActive || globalScheduleStatus.label == "PAUSED") FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
