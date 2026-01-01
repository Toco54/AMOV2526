package pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.guard

import androidx.compose.animation.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pt.isec.amov2526.projetoKotlin.R
import pt.isec.amov2526.projetoKotlin.data.model.MonitorRule
import pt.isec.amov2526.projetoKotlin.data.model.ProtectionGroup
import pt.isec.amov2526.projetoKotlin.utils.TimeUtils

// --- CORES ---
private val Slate50 = Color(0xFFF8FAFC)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate400 = Color(0xFF94A3B8)
private val Slate500 = Color(0xFF64748B)
private val Slate900 = Color(0xFF0F172A)
private val Emerald500 = Color(0xFF10B981)
private val Red50 = Color(0xFFFEF2F2)
private val Red100 = Color(0xFFFEE2E2)
private val Red600 = Color(0xFFDC2626)
private val Indigo600 = Color(0xFF4F46E5)

@Composable
fun ActiveProtectionsCard(
    protectionGroups: List<ProtectionGroup>,
    currentTime: Long,
    onToggleGroupExpansion: (ProtectionGroup, Boolean) -> Unit,
    onToggleGroupGlobal: (ProtectionGroup, Boolean) -> Unit,
    onUpdateRule: (String, Boolean) -> Unit, // Using Rule ID
    onRemoveRule: (MonitorRule) -> Unit,
    onGlobalSchedule: (ProtectionGroup) -> Unit,
    onRuleSchedule: (MonitorRule) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
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
            Spacer(modifier = Modifier.height(20.dp))

            if (protectionGroups.isEmpty()) {
                Text(
                    "No protections active.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    protectionGroups.forEach { group ->
                        IndividualRuleTypeSubCard(
                            group = group,
                            currentTime = currentTime,
                            onToggleGroup = { expanded -> onToggleGroupExpansion(group, expanded) },
                            onToggleGlobal = { enabled -> onToggleGroupGlobal(group, enabled) },
                            onUpdateRule = onUpdateRule,
                            onRemoveRule = onRemoveRule,
                            onGlobalSchedule = { onGlobalSchedule(group) },
                            onRuleSchedule = onRuleSchedule
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IndividualRuleTypeSubCard(
    group: ProtectionGroup,
    currentTime: Long,
    onToggleGroup: (Boolean) -> Unit,
    onToggleGlobal: (Boolean) -> Unit,
    onUpdateRule: (String, Boolean) -> Unit,
    onRemoveRule: (MonitorRule) -> Unit,
    onGlobalSchedule: () -> Unit,
    onRuleSchedule: (MonitorRule) -> Unit
) {
    // Check Status for the whole group
    // 1. Manually Paused (via toggle)
    val isPaused = group.pausedUntil > currentTime || group.pausedUntil == -1L

    // 2. Scheduled Off
    val status = TimeUtils.calculateStatus(
        isEnabled = !isPaused, // If paused, it overrides schedule check for "active" but we want to show specific status
        pausedUntil = group.pausedUntil,
        isScheduleEnabled = group.isScheduleEnabled,
        days = group.scheduleDays,
        start = group.scheduleStart,
        end = group.scheduleEnd,
        now = currentTime
    )

    // Determine the summary text with strict hierarchy:
    // 1. Live Countdown (Pause)
    // 2. Schedule Expiration (Schedule)
    // 3. Active Monitor Count (Active)
    // 4. Status Label (Inactive)
    val summaryText = when {
        group.pausedUntil > currentTime -> {
            val remaining = group.pausedUntil - currentTime
            "Paused (${TimeUtils.formatDuration(remaining)})"
        }
        status.label == "SCHEDULED" -> status.subLabel ?: "Scheduled Off"
        status.isActive -> "${group.rules.size} monitor(s)"
        else -> status.label
    }

    // Visuals for inactive state
    val containerColor = if (status.isActive) Slate50 else Red50
    val textColor = if (status.isActive) Slate400 else Red600

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (status.isActive) Slate100 else Red100),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clickable area for expansion (Icon + Text)
                Row(
                    modifier = Modifier
                        .clickable { onToggleGroup(!group.isExpanded) }
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(group.type.color.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = group.type.icon,
                            contentDescription = null,
                            tint = group.type.color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        color = if (group.isEffectivelyActive) Emerald500 else Slate400,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = group.type.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        }
                        Text(
                            text = summaryText,
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor,
                            fontWeight = if (!status.isActive) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                // Actions (Schedule + Toggle) - Not part of expansion click
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Small Circle Schedule Button
                    val isScheduleEnabled = group.isScheduleEnabled
                    val isScheduleActiveNow = status.label == "SCHEDULED"
                    
                    IconButton(
                        onClick = { onGlobalSchedule() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (isScheduleActiveNow) Indigo600
                                        else if (isScheduleEnabled) Indigo600.copy(alpha = 0.1f)
                                        else if (status.isActive) Slate100 else Color.White,
                                shape = CircleShape
                            ),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Transparent, // Managed by background modifier
                            contentColor = if (isScheduleActiveNow) Color.White 
                                           else if (isScheduleEnabled) Indigo600 
                                           else Slate400
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Schedule",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Switch(
                        checked = group.isEnabled && !isPaused,
                        onCheckedChange = { onToggleGlobal(it) },
                        modifier = Modifier.scale(0.6f),
                        colors = SwitchDefaults.colors(checkedTrackColor = Emerald500)
                    )
                    Icon(
                        imageVector = if (group.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Slate200,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
            AnimatedVisibility(
                visible = group.isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.5f))
                        .padding(bottom = 8.dp)
                ) {
                    HorizontalDivider(
                        color = Slate100,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    group.rules.forEach { rule ->
                        key(rule.id) {
                            MonitorDetailRow(
                                rule = rule,
                                currentTime = currentTime,
                                onToggle = { onUpdateRule(rule.id, it) },
                                onRemove = { onRemoveRule(rule) },
                                onSchedule = { onRuleSchedule(rule) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonitorDetailRow(
    rule: MonitorRule,
    currentTime: Long,
    onToggle: (Boolean) -> Unit,
    onRemove: () -> Unit,
    onSchedule: () -> Unit
) {
    // Calculate Rule Status
    val status = TimeUtils.calculateStatus(
        isEnabled = true, // Check schedule/pause regardless of Monitor/User disabled state for display
        pausedUntil = rule.pausedUntil,
        isScheduleEnabled = rule.isScheduleEnabled,
        days = rule.scheduleDays,
        start = rule.scheduleStart,
        end = rule.scheduleEnd,
        now = currentTime
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .background(Color.Transparent)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Effective Status Indicator (Dot)
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (rule.isEffectivelyActive) Emerald500 else Slate400,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "by ${rule.monitorName}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Indigo600
                )
            }
            
            // LINE 1: Parameter (Always visible)
            Text(
                text = rule.parameter,
                style = MaterialTheme.typography.labelSmall,
                color = Slate500
            )

            // LINE 2: Monitor State (if disabled by monitor)
            if (!rule.isEnabled) {
                Text(
                    text = "Disabled by Monitor",
                    style = MaterialTheme.typography.labelSmall,
                    color = Red600,
                    fontWeight = FontWeight.Bold
                )
            }

            // LINE 3: Protege State (Hierarchy)
            val protegeStateText = when {
                rule.isDisabledByProtege -> "Disabled by User"
                rule.pausedUntil > currentTime -> "Paused (${TimeUtils.formatDuration(rule.pausedUntil - currentTime)})"
                status.label == "SCHEDULED" -> status.subLabel ?: "Scheduled Off"
                rule.isEffectivelyActive -> "Active"
                else -> ""
            }
            
            if (protegeStateText.isNotEmpty()) {
                Text(
                    text = protegeStateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (rule.isEffectivelyActive) Emerald500 else Red600,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Show Schedule Info (Extra Info - maybe kept as line 4 effectively or just auxiliary)
            val schedText = TimeUtils.formatSchedule(rule.scheduleDays, rule.scheduleStart, rule.scheduleEnd)
            if (schedText.isNotEmpty()) {
                Text(
                    text = schedText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Smaller Circular Buttons
            val isRuleScheduleActive = status.label == "SCHEDULED"
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Red50)
                    .clickable { onRemove() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Red600.copy(alpha = 0.7f),
                    modifier = Modifier.size(13.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRuleScheduleActive) Indigo600
                        else if (rule.isScheduleEnabled) Indigo600.copy(alpha = 0.1f) 
                        else Slate100
                    )
                    .clickable { onSchedule() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = if (isRuleScheduleActive) Color.White 
                           else if (rule.isScheduleEnabled) Indigo600 
                           else Slate400,
                    modifier = Modifier.size(13.dp)
                )
            }

            // Switch logic for Protege side:
            // Controls 'isDisabledByProtege'. If checked (ON) -> isDisabledByProtege = false.
            // Also reflects PAUSE state: If paused (temporarily or indefinitely), switch appears OFF.
            val switchChecked = !rule.isDisabledByProtege && rule.pausedUntil <= currentTime

            // Status warning if Monitor disabled it
            if (!rule.isEnabled) {
                 Icon(
                     imageVector = Icons.Default.AdminPanelSettings, 
                     contentDescription = "Disabled by Monitor", 
                     tint = Red600, 
                     modifier = Modifier.size(20.dp)
                 )
                 Spacer(modifier = Modifier.width(4.dp))
            }

            Switch(
                checked = switchChecked,
                enabled = true, // Always enabled so Protege can set their preference
                onCheckedChange = { onToggle(it) },
                modifier = Modifier.scale(0.55f),
                colors = SwitchDefaults.colors(
                    checkedTrackColor = Emerald500,
                    disabledCheckedTrackColor = Emerald500.copy(alpha = 0.5f),
                    disabledUncheckedTrackColor = Slate200
                )
            )
        }
    }
}
