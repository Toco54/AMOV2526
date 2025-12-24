package pt.isec.amov2526.projetoKotlin.ui.screens.protected

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

// Cores
private val Slate50 = Color(0xFFF8FAFC)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate400 = Color(0xFF94A3B8)
private val Slate500 = Color(0xFF64748B)
private val Slate900 = Color(0xFF0F172A)

private val Emerald50 = Color(0xFFECFDF5)
private val Emerald100 = Color(0xFFD1FAE5)
private val Emerald200 = Color(0xFFA7F3D0)
private val Emerald500 = Color(0xFF10B981)
private val Emerald700 = Color(0xFF047857)

private val Indigo50 = Color(0xFFEEF2FF)
private val Indigo100 = Color(0xFFE0E7FF)
private val Indigo500 = Color(0xFF6366F1)
private val Indigo600 = Color(0xFF4F46E5)

private val Red50 = Color(0xFFFEF2F2)
private val Red600 = Color(0xFFDC2626)

enum class MonitorStatus { ACTIVE, INCOMING_REQUEST, OUTGOING_REQUEST }

data class MonitorItem(
    val id: String,
    val name: String,
    val email: String,
    val status: MonitorStatus,
    val accessLevel: String = "Full Monitoring"
)

// Mock Data Initial
private val initialMonitors = listOf(
    MonitorItem("1", "Alice Monitor", "alice@example.com", MonitorStatus.ACTIVE),
    MonitorItem("2", "Dave Dual", "dave@example.com", MonitorStatus.INCOMING_REQUEST),
    MonitorItem("3", "Eve Security", "eve@example.com", MonitorStatus.OUTGOING_REQUEST)
)

private val availableMonitors = listOf(
    MonitorItem("4", "Eva Security", "eva@example.com", MonitorStatus.ACTIVE),
    MonitorItem("5", "Frank Watch", "frank@example.com", MonitorStatus.ACTIVE),
    MonitorItem("6", "George Guard", "george@example.com", MonitorStatus.ACTIVE)
)

@Composable
fun MonitorsScreen() {
    var monitors by remember { mutableStateOf(initialMonitors) }
    var showRemoveDialog by remember { mutableStateOf<MonitorItem?>(null) }
    var showAcceptDialog by remember { mutableStateOf<MonitorItem?>(null) }
    var showAddMonitorModal by remember { mutableStateOf(false) }
    var candidateMonitor by remember { mutableStateOf<MonitorItem?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Slate50)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            if (monitors.any { it.status == MonitorStatus.INCOMING_REQUEST }) {
                Text(
                    text = "RECEIVED REQUESTS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(monitors.filter { it.status == MonitorStatus.INCOMING_REQUEST }) { monitor ->
                        IncomingRequestCard(
                            monitor = monitor,
                            onAccept = { showAcceptDialog = monitor },
                            onReject = {
                                monitors = monitors.filter { it.id != monitor.id }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (monitors.any { it.status == MonitorStatus.OUTGOING_REQUEST }) {
                Text(
                    text = "SENT REQUESTS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(monitors.filter { it.status == MonitorStatus.OUTGOING_REQUEST }) { monitor ->
                        OutgoingRequestCard(
                            monitor = monitor,
                            onCancel = {
                                monitors = monitors.filter { it.id != monitor.id }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (monitors.any { it.status == MonitorStatus.ACTIVE }) {
                Text(
                    text = "ACTIVE MONITORS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(monitors.filter { it.status == MonitorStatus.ACTIVE }) { monitor ->
                        MonitorCard(
                            monitor = monitor,
                            onRemove = { showRemoveDialog = monitor }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddMonitorModal = true },
            containerColor = Indigo600,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(64.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.PersonAdd,
                contentDescription = "Add Monitor",
                modifier = Modifier.size(28.dp)
            )
        }
    }

    // --- DIALOGS ---
    
    // CUSTOM REMOVE DIALOG
    if (showRemoveDialog != null) {
        Dialog(onDismissRequest = { showRemoveDialog = null }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Red Icon Circle
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Red50, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonRemove,
                            contentDescription = null,
                            tint = Red600,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Remove Monitor?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "${showRemoveDialog!!.name} will no longer have access to your alerts or status.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate500,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showRemoveDialog = null }
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold, color = Slate500)
                        }
                        
                        Box(
                            modifier = Modifier
                                .background(Red600, RoundedCornerShape(12.dp))
                                .clickable { 
                                    monitors = monitors.filter { it.id != showRemoveDialog!!.id }
                                    showRemoveDialog = null 
                                }
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                                .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp))
                        ) {
                            Text("Remove", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showAcceptDialog != null) {
        AccessSelectionDialog(
            title = "Grant Access",
            targetName = showAcceptDialog!!.name,
            onDismiss = { showAcceptDialog = null },
            onConfirm = { level ->
                monitors = monitors.map {
                    if (it.id == showAcceptDialog!!.id) it.copy(
                        status = MonitorStatus.ACTIVE,
                        accessLevel = level
                    ) else it
                }
                showAcceptDialog = null
            }
        )
    }

    if (showAddMonitorModal) {
        AddMonitorModal(
            existingMonitors = monitors,
            onDismiss = { showAddMonitorModal = false },
            onSelectCandidate = {
                candidateMonitor = it
                showAddMonitorModal = false
            }
        )
    }

    if (candidateMonitor != null) {
        AccessSelectionDialog(
            title = "Invite Monitor",
            targetName = candidateMonitor!!.name,
            onDismiss = { candidateMonitor = null },
            onConfirm = { level ->
                monitors = monitors + candidateMonitor!!.copy(
                    status = MonitorStatus.OUTGOING_REQUEST,
                    accessLevel = level
                )
                candidateMonitor = null
            }
        )
    }
}

@Composable
fun IncomingRequestCard(monitor: MonitorItem, onAccept: () -> Unit, onReject: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Emerald50),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Emerald100),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Emerald200, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = monitor.name.first().toString(),
                    fontWeight = FontWeight.Bold,
                    color = Emerald700,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = monitor.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    text = "WANTS TO MONITOR YOU",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Emerald500,
                    fontSize = 10.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                    .clickable { onReject() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Slate400,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Emerald500, RoundedCornerShape(12.dp))
                    .clickable { onAccept() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun OutgoingRequestCard(monitor: MonitorItem, onCancel: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "blinking")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Slate100, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = monitor.name.first().toString(),
                    fontWeight = FontWeight.Bold,
                    color = Slate500,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = monitor.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.alpha(alpha)
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassEmpty,
                        contentDescription = null,
                        tint = Indigo500,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "WAITING FOR ACCEPTANCE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Indigo500,
                        fontSize = 10.sp
                    )
                }
            }

            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = Slate200
                )
            }
        }
    }
}

@Composable
fun MonitorCard(monitor: MonitorItem, onRemove: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Slate100, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = monitor.name.first().toString(),
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = monitor.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = Indigo500,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = monitor.accessLevel,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500
                    )
                }
            }

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Outlined.PersonRemove,
                    contentDescription = "Remove",
                    tint = Slate200
                )
            }
        }
    }
}

@Composable
fun AccessSelectionDialog(
    title: String,
    targetName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = Emerald500,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }
                Text(
                    text = "Choose what type of monitoring $targetName can perform.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                )

                AccessOptionCard(
                    title = "Full Monitoring",
                    subtitle = "RECOMMENDED",
                    icon = Icons.Outlined.Shield,
                    color = Emerald500,
                    bgColor = Emerald50,
                    borderColor = Emerald100,
                    onClick = { onConfirm("Full Monitoring") }
                )
                Spacer(modifier = Modifier.height(12.dp))
                AccessOptionCard(
                    title = "Alerts Only",
                    subtitle = "LIMITED ACCESS",
                    icon = Icons.Outlined.Notifications,
                    color = Slate500,
                    bgColor = Slate50,
                    borderColor = Slate100,
                    onClick = { onConfirm("Alerts Only") }
                )
                Spacer(modifier = Modifier.height(12.dp))
                AccessOptionCard(
                    title = "Location Only",
                    subtitle = "LIMITED ACCESS",
                    icon = Icons.Default.LocationOn,
                    color = Indigo500,
                    bgColor = Indigo50,
                    borderColor = Indigo100,
                    onClick = { onConfirm("Location Only") }
                )

                Spacer(modifier = Modifier.height(24.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Back", color = Slate400, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AccessOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    bgColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun AddMonitorModal(
    existingMonitors: List<MonitorItem>,
    onDismiss: () -> Unit,
    onSelectCandidate: (MonitorItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredMonitors = availableMonitors.filter { candidate ->
        existingMonitors.none { it.id == candidate.id || it.email == candidate.email }
    }.filter {
        searchQuery.isBlank() ||
                it.name.contains(searchQuery, true) ||
                it.email.contains(searchQuery, true)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Add Monitor",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    text = "Search for a user to invite.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name or email") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Slate400) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Slate50,
                        unfocusedContainerColor = Slate50,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredMonitors) { monitor ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Slate50, RoundedCornerShape(12.dp))
                                .clickable { onSelectCandidate(monitor) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarPlaceholder(monitor.name, 40)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = monitor.name, fontWeight = FontWeight.Bold, color = Slate900)
                                Text(
                                    text = monitor.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate500
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Indigo600)
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close", color = Slate500)
                }
            }
        }
    }
}

@Composable
fun AvatarPlaceholder(name: String, size: Int = 48) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(Slate100, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.first().toString(),
            fontWeight = FontWeight.Bold,
            color = Slate500,
            fontSize = (size * 0.4).sp
        )
    }
}