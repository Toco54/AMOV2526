package pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.activity

import android.preference.PreferenceManager
import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon
import pt.isec.amov2526.projetoKotlin.data.model.ActivityItem
import pt.isec.amov2526.projetoKotlin.data.model.MonitorActionState
import pt.isec.amov2526.projetoKotlin.data.model.MonitorRule

// --- CORES ---
private val Slate50 = Color(0xFFF8FAFC)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate400 = Color(0xFF94A3B8)
private val Slate500 = Color(0xFF64748B)
private val Slate900 = Color(0xFF0F172A)
private val Red700 = Color(0xFFB91C1C)
private val Amber100 = Color(0xFFFEF3C7)
private val Amber700 = Color(0xFFB45309)
private val TabSelectedColor = Color(0xFF334155)
private val TabUnselectedColor = Color(0xFF1E293B)
private val SafeGreen = Color(0xFF4CAF50)

@Composable
fun AlertDetailModal(
    activity: ActivityItem,
    onDismiss: () -> Unit,
    activeGeofences: List<MonitorRule> = emptyList() // [NOVO]
) {
    var selectedCameraTab by remember { mutableIntStateOf(0) } // 0 = Rear, 1 = Front
    val context = LocalContext.current

    // [NOVO] Estado para controlar visibilidade das zonas por monitor
    val visibleMonitors = remember { mutableStateListOf<String>() }

    LaunchedEffect(activity) {
        visibleMonitors.clear()
        visibleMonitors.addAll(activity.notifiedMonitors.map { it.name }.distinct())
        activeGeofences.forEach { rule ->
             if (!visibleMonitors.contains(rule.monitorName)) {
                 visibleMonitors.add(rule.monitorName)
             }
        }
    }

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = context.packageName
    }

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
                    
                    // [MODIFICADO] MapView Interativo com Geofences
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Slate50)
                            .border(1.dp, Slate200, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                MapView(ctx).apply {
                                    setTileSource(TileSourceFactory.MAPNIK)
                                    setMultiTouchControls(true)
                                    controller.setZoom(18.0)
                                    
                                    // [CORREÇÃO] Prevenir conflito de scroll
                                    setOnTouchListener { v, event ->
                                        v.parent.requestDisallowInterceptTouchEvent(true)
                                        false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            update = { map ->
                                map.overlays.clear()
                                
                                // 1. Determine Map Center
                                // Priority: Incident Location -> First Active Geofence -> Default Coimbra
                                val centerPoint = if (activity.latitude != null && activity.longitude != null) {
                                    GeoPoint(activity.latitude, activity.longitude)
                                } else {
                                    // Fallback to first visible geofence center if available
                                    val firstVisible = activeGeofences.firstOrNull { visibleMonitors.contains(it.monitorName) && it.geofenceData != null }
                                    if (firstVisible != null) {
                                        val (lat, lng, _) = firstVisible.geofenceData!!
                                        GeoPoint(lat, lng)
                                    } else {
                                        GeoPoint(40.2033, -8.4103) // Default (Coimbra)
                                    }
                                }
                                
                                map.controller.setCenter(centerPoint)

                                // 2. Draw Geofences
                                activeGeofences.forEach { rule ->
                                    if (visibleMonitors.contains(rule.monitorName)) {
                                        rule.geofenceData?.let { (lat, lng, radius) ->
                                            val circle = Polygon(map)
                                            circle.points = Polygon.pointsAsCircle(GeoPoint(lat, lng), radius.toDouble())
                                            
                                            // Check infraction logic
                                            val isAlertContext = activity.type == pt.isec.amov2526.projetoKotlin.data.model.ActivityAlertType.GEOFENCE
                                            val wasNotified = activity.notifiedMonitors.any { it.name == rule.monitorName }
                                            val color = if (isAlertContext && wasNotified) Red700 else SafeGreen
                                            
                                            circle.fillPaint.color = android.graphics.Color.argb(70, (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())
                                            circle.outlinePaint.color = android.graphics.Color.argb(255, (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())
                                            circle.outlinePaint.strokeWidth = 3f

                                            map.overlays.add(circle)
                                        }
                                    }
                                }
                                
                                // 3. Draw Incident Marker (if location available)
                                if (activity.latitude != null && activity.longitude != null) {
                                    val marker = org.osmdroid.views.overlay.Marker(map)
                                    marker.position = GeoPoint(activity.latitude, activity.longitude)
                                    marker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM)
                                    marker.title = activity.title
                                    // marker.icon = ... (Optional custom icon)
                                    map.overlays.add(marker)
                                }
                                
                                map.invalidate()
                            }
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
                    
                    if (activity.notifiedMonitors.isEmpty()) {
                        Text(
                            text = "No monitors notified for this alert.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate500,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
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
                                        Icons.AutoMirrored.Filled.DirectionsRun,
                                        Amber700,
                                        "On My Way"
                                    )
                                    MonitorActionState.MARKED_SAFE -> Triple(
                                        Icons.Default.CheckCircle,
                                        Color(0xFF10B981),
                                        "Marked Safe"
                                    )
                                }
                                
                                val isSelected = visibleMonitors.contains(action.name)
    
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Slate50, RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                        .clickable {
                                            if (isSelected) visibleMonitors.remove(action.name)
                                            else visibleMonitors.add(action.name)
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // [NOVO] Checkbox
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            if (checked) visibleMonitors.add(action.name)
                                            else visibleMonitors.remove(action.name)
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Red700,
                                            uncheckedColor = Slate400,
                                            checkmarkColor = Color.White
                                        ),
                                        modifier = Modifier.scale(0.8f)
                                    )
    
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
