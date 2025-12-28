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
import androidx.compose.material.icons.automirrored.filled.ShowChart
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import pt.isec.amov2526.projetoKotlin.R
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.MonitorDashboardViewModel
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.MonitorDashboardViewModelFactory

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
    var param2: String = "",
    // New fields
    val scheduleDays: List<Int> = emptyList(),
    val scheduleStart: String = "",
    val scheduleEnd: String = "",
    val pausedUntil: Long = 0L,
    val isDisabledByProtege: Boolean = false
)

data class AlertStat(val label: String, val count: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorDashboardScreen(
    protegeName: String = "Bob Protected",
    protegeUid: String = "",
    onBack: () -> Unit = {}
) {
    val viewModel: MonitorDashboardViewModel = viewModel(
        factory = MonitorDashboardViewModelFactory(protegeUid)
    )
    val rules by viewModel.rules.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()

    var showAddRuleSelection by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<ProtectionRule?>(null) }
    var selectedActivity by remember { mutableStateOf<ActivityItem?>(null) }
    var ruleIdToDelete by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(protegeName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Slate900)
                        Text("LIVE DASHBOARD", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Indigo600, letterSpacing = 0.5.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Slate900)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().background(Slate50).padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            LiveLocationMapCard(userLocation = userLocation, rules = rules)

            AlertTrendsCard(
                stats = listOf(
                    AlertStat("Fall", 0), AlertStat("Crash", 0), AlertStat("Geo", 1),
                    AlertStat("Speed", 2), AlertStat("Idle", 0), AlertStat("SOS", 1)
                )
            )

            // Rules Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(id = R.drawable.ic_shield_check), null, tint = Emerald500, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Manage Protection Rules", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate900)
                        }
                        IconButton(onClick = { showAddRuleSelection = true }, modifier = Modifier.size(28.dp).background(Slate100, CircleShape)) {
                            Icon(Icons.Default.Add, "Add", tint = Slate900, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (rules.isEmpty()) {
                        Text("No active rules. Tap + to add.", style = MaterialTheme.typography.bodySmall, color = Slate400, modifier = Modifier.padding(vertical = 12.dp))
                    } else {
                        // Group rules by Type and sort by enum ordinal
                        val groupedRules = rules.groupBy { it.type }.toList().sortedBy { it.first.ordinal }
                        
                        groupedRules.forEach { (type, typeRules) ->
                            if (type.isMultiInstance) {
                                // Multi-instance: Use Expandable Group
                                MonitorRuleGroup(
                                    type = type,
                                    rules = typeRules,
                                    onToggleGroup = { enabled ->
                                        typeRules.forEach { if (it.isEnabled != enabled) viewModel.toggleRule(it.id, enabled) }
                                    },
                                    onEditRule = { editingRule = it },
                                    onToggleRule = { id, enabled -> viewModel.toggleRule(id, enabled) }
                                )
                            } else {
                                // Single-instance: Render standalone card
                                val rule = typeRules.firstOrNull()
                                if (rule != null) {
                                    // Use a simplified version of the row logic wrapped in a Card
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Slate50),
                                        shape = RoundedCornerShape(16.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Icon
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(rule.iconColor.copy(alpha = 0.1f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(rule.icon, null, tint = rule.iconColor, modifier = Modifier.size(20.dp))
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            
                                            // Text
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = type.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Slate900
                                                )
                                                Text(
                                                    text = if(rule.isEnabled) "Monitoring Active" else "Disabled",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Slate400
                                                )
                                            }
                                            
                                            // Delete (optional for singleton? Maybe just toggle is enough, but keeping consistency)
                                            // Actually, for singleton, usually you don't delete, you just disable.
                                            // But if we want to allow removing the capability entirely:
                                            IconButton(onClick = { ruleIdToDelete = rule.id }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.Delete, null, tint = Slate400, modifier = Modifier.size(18.dp))
                                            }

                                            Spacer(modifier = Modifier.width(4.dp))

                                            // Switch
                                            if (rule.isDisabledByProtege) {
                                                Icon(Icons.Default.PersonOff, "Disabled by User", tint = Red600, modifier = Modifier.size(24.dp))
                                            } else if (rule.pausedUntil > System.currentTimeMillis()) {
                                                Icon(Icons.Default.PauseCircle, "Paused", tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                                            } else {
                                                Switch(
                                                    checked = rule.isEnabled,
                                                    onCheckedChange = { viewModel.toggleRule(rule.id, it) },
                                                    modifier = Modifier.scale(0.6f),
                                                    colors = SwitchDefaults.colors(checkedTrackColor = Emerald500)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }

            RecentActivityCard(activities = mockMonitorActivities, onActivityClick = { selectedActivity = it })
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showAddRuleSelection) {
        AddRuleTypeDialog(existingRules = rules, onDismiss = { showAddRuleSelection = false }, onTypeSelected = { type ->
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
        })
    }

    if (editingRule != null) {
        EditRuleDialog(
            rule = editingRule!!,
            isNew = editingRule!!.id.startsWith("NEW_"),
            existingRules = rules,
            onDismiss = { editingRule = null },
            onSave = { if (editingRule!!.id.startsWith("NEW_")) viewModel.addRule(it) else viewModel.updateRule(it); editingRule = null },
            onDelete = { ruleIdToDelete = it }
        )
    }

    if (ruleIdToDelete != null) {
        AlertDialog(
            onDismissRequest = { ruleIdToDelete = null },
            title = { Text("Delete Rule", fontWeight = FontWeight.Bold, color = Slate900) },
            text = { Text("Are you sure you want to remove this protection rule? This action cannot be undone.", color = Slate500) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRule(ruleIdToDelete!!)
                        ruleIdToDelete = null
                        editingRule = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red600)
                ) { Text("Delete", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { ruleIdToDelete = null }) { Text("Cancel", color = Slate500, fontWeight = FontWeight.Bold) }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (selectedActivity != null) {
        MonitorAlertDetailModal(activity = selectedActivity!!, onDismiss = { selectedActivity = null })
    }
}

@Composable
fun MonitorRuleGroup(type: RuleType, rules: List<ProtectionRule>, onToggleGroup: (Boolean) -> Unit, onEditRule: (ProtectionRule) -> Unit, onToggleRule: (String, Boolean) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    val isAnyActive = rules.any { it.isEnabled }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Slate50), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Slate100)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                val iconColor = rules.firstOrNull()?.iconColor ?: Slate400
                val iconVector = rules.firstOrNull()?.icon ?: Icons.Default.Shield
                Box(modifier = Modifier.size(36.dp).background(iconColor.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(iconVector, null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(type.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate900)
                    Text("${rules.count { it.isEnabled }} active / ${rules.size} total", style = MaterialTheme.typography.labelSmall, color = Slate400)
                }
                Switch(checked = isAnyActive, onCheckedChange = onToggleGroup, modifier = Modifier.scale(0.6f), colors = SwitchDefaults.colors(checkedTrackColor = Emerald500))
                Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = Slate200)
            }
            androidx.compose.animation.AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.background(Color.White.copy(alpha = 0.5f)).padding(bottom = 8.dp)) {
                    HorizontalDivider(color = Slate100, modifier = Modifier.padding(horizontal = 12.dp))
                    rules.forEach { rule ->
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                val detail = when (rule.type) {
                                    RuleType.GEOFENCING -> if (rule.param1.isEmpty()) "Pending" else "Radius: ${rule.param1}m"
                                    RuleType.SPEED -> "Limit: ${rule.param1} km/h"
                                    RuleType.INACTIVITY -> "Limit: ${rule.param1} min"
                                    else -> "Always Active"
                                }
                                Text(if(rule.type.isMultiInstance) "Custom Rule" else "Standard Rule", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Indigo600)
                                Text(detail, style = MaterialTheme.typography.labelSmall, color = Slate500)
                            }
                            IconButton(onClick = { onEditRule(rule) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Settings, null, tint = Slate400, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            if (rule.isDisabledByProtege) {
                                Icon(Icons.Default.PersonOff, "Disabled by User", tint = Red600, modifier = Modifier.size(20.dp))
                            } else if (rule.pausedUntil > System.currentTimeMillis()) {
                                Icon(Icons.Default.PauseCircle, "Paused", tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                            } else {
                                Switch(
                                    checked = rule.isEnabled,
                                    onCheckedChange = { onToggleRule(rule.id, it) },
                                    modifier = Modifier.scale(0.55f),
                                    colors = SwitchDefaults.colors(checkedTrackColor = Emerald500)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddRuleTypeDialog(existingRules: List<ProtectionRule>, onDismiss: () -> Unit, onTypeSelected: (RuleType) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Protection Type", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Slate900)
                Spacer(modifier = Modifier.height(16.dp))
                RuleType.entries.forEach { type ->
                    val isAlreadyDefined = !type.isMultiInstance && existingRules.any { it.type == type }
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(if (isAlreadyDefined) Slate100 else Slate50, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)).clickable(enabled = !isAlreadyDefined) { onTypeSelected(type) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(type.title, fontWeight = FontWeight.Bold, color = if (isAlreadyDefined) Slate400 else Slate700)
                        Spacer(modifier = Modifier.weight(1f))
                        if (isAlreadyDefined) Text("DEFINED", style = MaterialTheme.typography.labelSmall, color = Slate400) else Icon(Icons.Default.ChevronRight, null, tint = Slate400)
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp)) { Text("Cancel", color = Slate400, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun EditRuleDialog(rule: ProtectionRule, isNew: Boolean, existingRules: List<ProtectionRule>, onDismiss: () -> Unit, onSave: (ProtectionRule) -> Unit, onDelete: (String) -> Unit) {
    var p1 by remember { mutableStateOf(if(rule.param1.isEmpty()) "500" else rule.param1) }
    // FIX: Use a mutable object that doesn't trigger recomposition for map updates
    val p2 = remember { object { var value = rule.param2 } }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val locationHelper = remember { pt.isec.amov2526.projetoKotlin.utils.LocationHelper(context) }
    val userLocation by locationHelper.currentLocation.collectAsState()
    var hasInitializedMap by remember { mutableStateOf(false) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var currentPoly by remember { mutableStateOf<org.osmdroid.views.overlay.Polygon?>(null) }

    fun zoomToRadius(map: MapView, center: GeoPoint, radiusMeters: Double) {
        val delta = (radiusMeters * 1.5) / 111000.0
        map.zoomToBoundingBox(org.osmdroid.util.BoundingBox(center.latitude+delta, center.longitude+delta, center.latitude-delta, center.longitude-delta), true)
    }

    LaunchedEffect(Unit) { Configuration.getInstance().userAgentValue = context.packageName; locationHelper.getLastLocation() }
    LaunchedEffect(p1) {
        val r = p1.toDoubleOrNull() ?: 100.0
        mapViewRef?.let { map ->
            currentPoly?.points = org.osmdroid.views.overlay.Polygon.pointsAsCircle(map.mapCenter as GeoPoint, r)
            zoomToRadius(map, map.mapCenter as GeoPoint, r)
            map.invalidate()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(44.dp).background(rule.iconColor.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(rule.icon, null, tint = rule.iconColor, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(rule.type.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Slate900)
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (rule.type == RuleType.GEOFENCING) {
                    OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Search address...") }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, Slate200, RoundedCornerShape(16.dp))) {
                        AndroidView(factory = { ctx ->
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK); setMultiTouchControls(true); controller.setZoom(15.5); mapViewRef = this
                                // Fix vertical scroll conflict
                                setOnTouchListener { v, _ ->
                                    v.parent.requestDisallowInterceptTouchEvent(true)
                                    false
                                }
                                existingRules.forEach { other ->
                                    if (other.type == RuleType.GEOFENCING && other.id != rule.id) {
                                        val parts = other.param2.split(",").map { it.trim().toDoubleOrNull() }
                                        if (parts.size == 2 && parts[0] != null && parts[1] != null) {
                                            val otherPoly = org.osmdroid.views.overlay.Polygon(this)
                                            otherPoly.points = org.osmdroid.views.overlay.Polygon.pointsAsCircle(GeoPoint(parts[0]!!, parts[1]!!), other.param1.toDoubleOrNull() ?: 100.0)
                                            val c = if (other.isEnabled) Color(0xFF059669) else Color(0xFFDC2626)
                                            otherPoly.fillColor = android.graphics.Color.argb(50, (c.red*255).toInt(), (c.green*255).toInt(), (c.blue*255).toInt())
                                            otherPoly.strokeColor = android.graphics.Color.argb(180, (c.red*255).toInt(), (c.green*255).toInt(), (c.blue*255).toInt())
                                            otherPoly.strokeWidth = 3f; overlays.add(otherPoly)
                                        }
                                    }
                                }
                                val poly = org.osmdroid.views.overlay.Polygon(this)
                                poly.fillColor = android.graphics.Color.argb(60, 79, 70, 229); poly.strokeColor = android.graphics.Color.argb(200, 79, 70, 229); poly.strokeWidth = 4f
                                overlays.add(poly); currentPoly = poly
                                addMapListener(object : org.osmdroid.events.MapListener {
                                    override fun onScroll(e: org.osmdroid.events.ScrollEvent?): Boolean {
                                        val center = mapCenter as GeoPoint; p2.value = "${center.latitude}, ${center.longitude}"
                                        poly.points = org.osmdroid.views.overlay.Polygon.pointsAsCircle(center, p1.toDoubleOrNull() ?: 100.0); invalidate(); return true
                                    }
                                    override fun onZoom(e: org.osmdroid.events.ZoomEvent?): Boolean {
                                        poly.points = org.osmdroid.views.overlay.Polygon.pointsAsCircle(mapCenter as GeoPoint, p1.toDoubleOrNull() ?: 100.0); return true
                                    }
                                })
                            }
                        }, modifier = Modifier.fillMaxSize(),
                            update = { map ->
                                if (!hasInitializedMap) {
                                    val start = if (isNew && userLocation != null) GeoPoint(userLocation!!.latitude, userLocation!!.longitude)
                                                else if (p2.value.isNotBlank()) {
                                                    val parts = p2.value.split(",").map { it.trim().toDoubleOrNull() }
                                                    if(parts.size == 2 && parts[0] != null && parts[1] != null) GeoPoint(parts[0]!!, parts[1]!!) else null
                                                } else if (!isNew) GeoPoint(40.2033, -8.4103) else null
                                    
                                    start?.let {
                                        map.controller.setCenter(it)
                                        // Sync p2 with initial center
                                        p2.value = "${it.latitude}, ${it.longitude}"
                                        
                                        currentPoly?.points = org.osmdroid.views.overlay.Polygon.pointsAsCircle(it, p1.toDoubleOrNull() ?: 500.0)
                                        map.invalidate()
                                        hasInitializedMap = true
                                    }
                                }
                                // Crucial Fix: Do NOT setCenter here based on p2. 
                                // The map drives p2, not the other way around during drag.
                            }
                        )
                        Box(modifier = Modifier.align(Alignment.Center)) {
                            Icon(Icons.Default.LocationOn, null, tint = Indigo600, modifier = Modifier.size(44.dp).offset(y = (-22).dp))
                        }
                        SmallFloatingActionButton(onClick = { locationHelper.getLastLocation(); userLocation?.let { mapViewRef?.controller?.animateTo(GeoPoint(it.latitude, it.longitude)) } }, modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp), containerColor = Color.White, contentColor = Indigo600) {
                            Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("RADIUS: ${p1} METERS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Slate500)
                    Slider(value = p1.toFloatOrNull() ?: 500f, onValueChange = { p1 = it.toInt().toString() }, valueRange = 100f..5000f, steps = 49, colors = SliderDefaults.colors(thumbColor = Indigo600, activeTrackColor = Indigo600))
                }
                if (rule.type.isMultiInstance && rule.type != RuleType.GEOFENCING) {
                    Text(if (rule.type == RuleType.SPEED) "MAX SPEED (KM/H)" else "TIME LIMIT (MINUTES)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Slate400)
                    TextField(value = p1, onValueChange = { p1 = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth().padding(top = 4.dp), shape = RoundedCornerShape(12.dp), colors = TextFieldDefaults.colors(focusedContainerColor = Slate50, unfocusedContainerColor = Slate50))
                } else if (!rule.type.isMultiInstance) {
                    Text("This rule automatically detects events and notifies you. No parameters required.", color = Slate500, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { if (isNew) onDismiss() else onDelete(rule.id) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isNew) Slate100 else Red600,
                            contentColor = if (isNew) Slate500 else Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text(if (isNew) "Cancel" else "Delete", fontWeight = FontWeight.Bold) }
                    
                    Button(
                        onClick = { onSave(rule.copy(param1 = p1, param2 = p2.value, isEnabled = true)) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Indigo600,
                            contentColor = Color.White // Fix: Added white text for indigo button
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text(if (isNew) "Add" else "Save", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
fun AlertTrendsCard(stats: List<AlertStat>) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.ShowChart, null, tint = Slate400, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Alert Trends", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate900)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth().height(160.dp), verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    listOf("4", "3", "2", "1", "0").forEach { label -> Text(label, style = MaterialTheme.typography.labelSmall, color = Slate400, fontSize = 10.sp) }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        repeat(5) { Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Slate100)) }
                    }
                    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.Bottom) {
                        stats.forEach { stat ->
                            val barHeightFactor = stat.count / 4f
                            Box(modifier = Modifier.width(16.dp).fillMaxHeight(barHeightFactor.coerceAtLeast(0.02f)).background(Emerald500, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(start = 18.dp, top = 8.dp), horizontalArrangement = Arrangement.SpaceAround) {
                stats.forEach { stat -> Text(stat.label, style = MaterialTheme.typography.labelSmall, color = Slate500, textAlign = TextAlign.Center, fontSize = 9.sp, modifier = Modifier.width(36.dp)) }
            }
        }
    }
}

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

enum class ActivityAlertType { SPEED, FALL, ACCIDENT, GEOFENCE, PANIC }
enum class ActivityAlertStatus { CONFIRMED, CANCELLED, PENDING }
enum class MonitorActionState { SENT, VIEWED, CALLED_USER, CALLED_112, ON_THE_WAY, MARKED_SAFE }
data class MonitorAction(val name: String, val state: MonitorActionState, val time: String)
data class ActivityItem(val id: String, val type: ActivityAlertType, val title: String, val date: String, val time: String, val status: ActivityAlertStatus, val notifiedMonitors: List<MonitorAction> = emptyList())

private val mockMonitorActivities = listOf(
    ActivityItem("1", ActivityAlertType.SPEED, "Speed Alert", "23/12/2025", "21:36", ActivityAlertStatus.CONFIRMED, listOf(MonitorAction("Alice Monitor", MonitorActionState.CALLED_USER, "21:37"))),
    ActivityItem("2", ActivityAlertType.FALL, "Fall Detected", "22/12/2025", "14:20", ActivityAlertStatus.CANCELLED, listOf(MonitorAction("Alice Monitor", MonitorActionState.MARKED_SAFE, "14:22"))),
    ActivityItem("3", ActivityAlertType.GEOFENCE, "Safe Zone Exit", "20/12/2025", "09:15", ActivityAlertStatus.CONFIRMED),
    ActivityItem("4", ActivityAlertType.PANIC, "SOS Activated", "18/12/2025", "23:45", ActivityAlertStatus.PENDING)
)

@Composable
fun LiveLocationMapCard(userLocation: GeoPoint?, rules: List<ProtectionRule>) {
    Card(
        modifier = Modifier.fillMaxWidth().height(300.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)
                        
                        // Fix for parent scroll
                        setOnTouchListener { v, _ ->
                            v.parent.requestDisallowInterceptTouchEvent(true)
                            false
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { map ->
                    map.overlays.clear()

                    // Draw Geofences
                    rules.filter { it.type == RuleType.GEOFENCING }.forEach { rule ->
                        val parts = rule.param2.split(",").map { it.trim().toDoubleOrNull() }
                        if (parts.size == 2 && parts[0] != null && parts[1] != null) {
                            val poly = org.osmdroid.views.overlay.Polygon(map)
                            poly.points = org.osmdroid.views.overlay.Polygon.pointsAsCircle(
                                GeoPoint(parts[0]!!, parts[1]!!), 
                                rule.param1.toDoubleOrNull() ?: 100.0
                            )
                            
                            val c = if (rule.isEnabled) Color(0xFF059669) else Color(0xFFDC2626)
                            poly.fillColor = android.graphics.Color.argb(50, (c.red*255).toInt(), (c.green*255).toInt(), (c.blue*255).toInt())
                            poly.strokeColor = android.graphics.Color.argb(180, (c.red*255).toInt(), (c.green*255).toInt(), (c.blue*255).toInt())
                            poly.strokeWidth = 3f
                            map.overlays.add(poly)
                        }
                    }

                    // Draw User Location
                    if (userLocation != null) {
                        val marker = org.osmdroid.views.overlay.Marker(map)
                        marker.position = userLocation
                        marker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM)
                        marker.title = "Protege Location"
                        // You can set a custom icon here if needed
                        // marker.icon = ...
                        map.overlays.add(marker)
                        map.controller.setCenter(userLocation)
                    } else {
                        // Default center if no location
                        if (map.mapCenter.latitude == 0.0 && map.mapCenter.longitude == 0.0) {
                            map.controller.setCenter(GeoPoint(40.2033, -8.4103))
                        }
                    }
                    map.invalidate()
                }
            )
            
            // Overlay Label
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(if (userLocation != null) Emerald500 else Red600, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (userLocation != null) "LIVE LOCATION" else "OFFLINE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
