package pt.isec.amov2526.projetoKotlin.ui.screens.monitor.components.dashboard

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import pt.isec.amov2526.projetoKotlin.data.model.MonitorRuleType
import pt.isec.amov2526.projetoKotlin.data.model.ProtectionRule

private val Slate50 = Color(0xFFF8FAFC)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate400 = Color(0xFF94A3B8)
private val Slate500 = Color(0xFF64748B)
private val Slate700 = Color(0xFF334155)
private val Slate900 = Color(0xFF0F172A)
private val Indigo600 = Color(0xFF4F46E5)
private val Emerald500 = Color(0xFF10B981)
private val Red600 = Color(0xFFDC2626)

@Composable
fun MonitorRuleGroup(type: MonitorRuleType, rules: List<ProtectionRule>, onToggleGroup: (Boolean) -> Unit, onEditRule: (ProtectionRule) -> Unit, onToggleRule: (String, Boolean) -> Unit) {
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
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.background(Color.White.copy(alpha = 0.5f)).padding(bottom = 8.dp)) {
                    HorizontalDivider(color = Slate100, modifier = Modifier.padding(horizontal = 12.dp))
                    rules.forEach { rule ->
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                val detail = when (rule.type) {
                                    MonitorRuleType.GEOFENCING -> if (rule.param1.isEmpty()) "Pending" else "Radius: ${rule.param1}m"
                                    MonitorRuleType.SPEED -> "Limit: ${rule.param1} km/h"
                                    MonitorRuleType.INACTIVITY -> "Limit: ${rule.param1} min"
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
fun AddRuleTypeDialog(existingRules: List<ProtectionRule>, onDismiss: () -> Unit, onTypeSelected: (MonitorRuleType) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Protection Type", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Slate900)
                Spacer(modifier = Modifier.height(16.dp))
                MonitorRuleType.entries.forEach { type ->
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
                if (rule.type == MonitorRuleType.GEOFENCING) {
                    OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Search address...") }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, Slate200, RoundedCornerShape(16.dp))) {
                        AndroidView(factory = { ctx ->
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK); setMultiTouchControls(true); controller.setZoom(15.5); mapViewRef = this
                                setOnTouchListener { v, _ ->
                                    v.parent.requestDisallowInterceptTouchEvent(true)
                                    false
                                }
                                existingRules.forEach { other ->
                                    if (other.type == MonitorRuleType.GEOFENCING && other.id != rule.id) {
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
                                        p2.value = "${it.latitude}, ${it.longitude}"
                                        currentPoly?.points = org.osmdroid.views.overlay.Polygon.pointsAsCircle(it, p1.toDoubleOrNull() ?: 500.0)
                                        map.invalidate()
                                        hasInitializedMap = true
                                    }
                                }
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
                if (rule.type.isMultiInstance && rule.type != MonitorRuleType.GEOFENCING) {
                    Text(if (rule.type == MonitorRuleType.SPEED) "MAX SPEED (KM/H)" else "TIME LIMIT (MINUTES)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Slate400)
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
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text(if (isNew) "Add" else "Save", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
