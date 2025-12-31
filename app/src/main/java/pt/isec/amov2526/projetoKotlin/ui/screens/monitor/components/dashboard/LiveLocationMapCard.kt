package pt.isec.amov2526.projetoKotlin.ui.screens.monitor.components.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import pt.isec.amov2526.projetoKotlin.data.model.MonitorRuleType
import pt.isec.amov2526.projetoKotlin.data.model.ProtectionRule

private val Emerald500 = Color(0xFF10B981)
private val Red600 = Color(0xFFDC2626)
private val Slate900 = Color(0xFF0F172A)

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
                    rules.filter { it.type == MonitorRuleType.GEOFENCING }.forEach { rule ->
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
