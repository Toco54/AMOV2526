package pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.alert

import android.location.Location
import android.preference.PreferenceManager
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlinx.coroutines.delay

private val AlertRed = Color(0xFFDC2626)
private val AlertGreen = Color(0xFF16A34A)
private val HeaderColor = Color(0xFF334155)

@Composable
fun ConfirmedPanel(
    currentLocation: Location?,
    alertId: String?,
    previewView: PreviewView,
    isRecordingFinished: Boolean = false,
    recordingStartTime: Long = 0L,
    onClose: () -> Unit = {}
) {
    val context = LocalContext.current
    // Estado local para o countdown visual
    var remainingSeconds by remember { mutableLongStateOf(12L) }
    val isTimeUp = remainingSeconds <= 0

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    // Timer Loop para atualizar o UI de forma sincronizada com o início real
    LaunchedEffect(recordingStartTime, isRecordingFinished) {
        if (!isRecordingFinished && recordingStartTime > 0) {
            while (true) {
                val elapsed = System.currentTimeMillis() - recordingStartTime
                val left = 12000L - elapsed
                remainingSeconds = (left / 1000L).coerceAtLeast(0L)
                if (left <= 0) break
                delay(500L) // Atualização mais frequente para maior precisão visual
            }
        } else if (isRecordingFinished) {
            remainingSeconds = 0
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Status Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isRecordingFinished) AlertGreen else AlertRed)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = if (isRecordingFinished) Icons.Default.CheckCircle else Icons.Default.Warning, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (isRecordingFinished) "EVIDENCE UPLOADED" else "HELP REQUESTED", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color.White)
                }
                Text(text = if (isRecordingFinished) "Monitors received your data." else "Monitors Notified • Recording Evidence", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Camera Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(HeaderColor)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "BACK CAMERA RECORDING", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        // 3. Video Feed Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                .background(Color.Black)
                .border(width = 1.dp, color = Color.Gray, shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { 
                    (previewView.parent as? ViewGroup)?.removeView(previewView)
                    previewView 
                },
                modifier = Modifier.fillMaxSize()
            )

            if (!isRecordingFinished) {
                Row (
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isTimeUp) {
                        Box(modifier = Modifier.size(8.dp).background(Color.Red, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "REC", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "${remainingSeconds}s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    } else {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "UPLOADING...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Map View
        Text(text = "Live Location", color = Color.White.copy(alpha = 0.7f), modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp))
        Box(modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(12.dp)).background(Color(0xFFE2E8F0))) {
            if (currentLocation != null) {
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(18.0)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { map ->
                        val point = GeoPoint(currentLocation.latitude, currentLocation.longitude)
                        map.controller.setCenter(point)
                        map.overlays.clear()
                        val marker = Marker(map)
                        marker.position = point
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        marker.title = "Eu estou aqui!"
                        map.overlays.add(marker)
                        map.invalidate()
                    }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "A aguardar sinal de GPS...", color = Color.Gray)
                }
            }
            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(4.dp)).padding(4.dp)) {
                val locText = if (currentLocation != null) "Lat: %.4f\nLng: %.4f".format(currentLocation.latitude, currentLocation.longitude) else "A adquirir GPS..."
                Text(text = locText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        if (isRecordingFinished) {
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                Text(text = "CLOSE ALERT PANEL", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        } else {
            Text(text = if (isTimeUp) "Uploading evidence... please wait." else "Keep the app open. Help is coming.", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}