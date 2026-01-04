package pt.isec.amov2526.projetoKotlin.ui.screens.monitor.components.dashboard

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.preference.PreferenceManager
import android.view.Surface
import android.view.TextureView
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CarCrash
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon
import pt.isec.amov2526.projetoKotlin.R
import pt.isec.amov2526.projetoKotlin.data.model.ActivityAlertType
import pt.isec.amov2526.projetoKotlin.data.model.ActivityItem
import pt.isec.amov2526.projetoKotlin.data.model.MonitorActionState
import pt.isec.amov2526.projetoKotlin.data.model.MonitorRule
import pt.isec.amov2526.projetoKotlin.ui.theme.Amber100
import pt.isec.amov2526.projetoKotlin.ui.theme.Amber700
import pt.isec.amov2526.projetoKotlin.ui.theme.Indigo500
import pt.isec.amov2526.projetoKotlin.ui.theme.Red700
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate100
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate200
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate400
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate50
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate500
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate900
import java.io.File
import java.io.FileOutputStream
import java.net.URL

@Composable
fun RecentActivityCard(activities: List<ActivityItem>, onActivityClick: (ActivityItem) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, null, tint = Slate400, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(id = R.string.history_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                activities.forEach { activity ->
                    ActivityItemRow(activity) {
                        onActivityClick(
                            activity
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityItemRow(activity: ActivityItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate50),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate100)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White, CircleShape)
                    .border(1.dp, Slate100, CircleShape), contentAlignment = Alignment.Center
            ) {
                Icon(
                    getMonitorIconForActivityType(activity.type),
                    null,
                    tint = Slate500,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    activity.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    "${activity.date} • ${activity.time}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                null,
                tint = Slate200,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun MonitorAlertDetailModal(
    activity: ActivityItem,
    onDismiss: () -> Unit,
    activeGeofences: List<MonitorRule> = emptyList(),
    onMarkAsViewed: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()


    LaunchedEffect(activity.id) {
        onMarkAsViewed(activity.id)
    }


    var localVideoFile by remember { mutableStateOf<File?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(true) }

    val videoUrl = activity.videoUrl

    LaunchedEffect(activity) {

        val file = File(context.filesDir, "alert_video_${activity.id}.mp4")
        if (file.exists() && file.length() > 0) {
            localVideoFile = file
        } else {
            localVideoFile = null
        }
    }

    LaunchedEffect(Unit) {
        Configuration.getInstance()
            .load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    fun downloadVideo() {
        if (videoUrl == null || videoUrl == "PENDING/UNAVAILABLE") {
            downloadError = "Video unavailable"
            return
        }

        scope.launch(Dispatchers.IO) {
            isDownloading = true
            downloadError = null
            try {
                val connection = URL(videoUrl).openConnection()
                connection.connect()
                val input = connection.getInputStream()
                val file = File(context.filesDir, "alert_video_${activity.id}.mp4")
                val output = FileOutputStream(file)
                input.copyTo(output)
                output.close()
                input.close()

                withContext(Dispatchers.Main) {
                    localVideoFile = file
                    isDownloading = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    downloadError = "Download failed"
                    isDownloading = false
                }
            }
        }
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
                                imageVector = getMonitorIconForActivityType(activity.type),
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
                                text = "${activity.date} • ${activity.time}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(id = R.string.action_close),
                            tint = Slate400
                        )
                    }
                }


                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(250.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .clickable(enabled = localVideoFile != null) { isPlaying = !isPlaying },
                    contentAlignment = Alignment.Center
                ) {
                    if (localVideoFile != null) {

                        val mediaPlayer = remember { MediaPlayer() }

                        DisposableEffect(Unit) {
                            onDispose {
                                try {
                                    if (mediaPlayer.isPlaying) mediaPlayer.stop()
                                    mediaPlayer.release()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }


                        LaunchedEffect(isPlaying) {
                            try {
                                if (isPlaying && !mediaPlayer.isPlaying) {
                                    mediaPlayer.start()
                                } else if (!isPlaying && mediaPlayer.isPlaying) {
                                    mediaPlayer.pause()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }


                        AndroidView(
                            factory = { ctx ->
                                TextureView(ctx).apply {
                                    surfaceTextureListener =
                                        object : TextureView.SurfaceTextureListener {
                                            override fun onSurfaceTextureAvailable(
                                                surface: SurfaceTexture,
                                                w: Int,
                                                h: Int
                                            ) {
                                                try {
                                                    mediaPlayer.setDataSource(
                                                        ctx,
                                                        Uri.fromFile(localVideoFile)
                                                    )
                                                    mediaPlayer.setSurface(Surface(surface))
                                                    mediaPlayer.isLooping = true
                                                    mediaPlayer.prepare()
                                                    if (isPlaying) mediaPlayer.start()
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }

                                            override fun onSurfaceTextureSizeChanged(
                                                surface: SurfaceTexture,
                                                w: Int,
                                                h: Int
                                            ) {
                                            }

                                            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean =
                                                true

                                            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                                        }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )


                        if (!isPlaying) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                    .padding(16.dp)
                            )
                        }
                    } else {

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (isDownloading) {
                                CircularProgressIndicator(color = Color.White)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Downloading Evidence...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "Download",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                                        .padding(12.dp)
                                        .clickable { downloadVideo() }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (downloadError != null) "Error: $downloadError" else "TAP TO DOWNLOAD VIDEO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (downloadError != null) Red700 else Color.White.copy(
                                        alpha = 0.4f
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                if (videoUrl == null || videoUrl == "PENDING/UNAVAILABLE") {
                                    Text(
                                        text = "(No video available)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.3f),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }


                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "REC",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }


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
                            text = stringResource(id = R.string.evidence_live_location).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))


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

                                    setOnTouchListener { v, _ ->
                                        v.parent.requestDisallowInterceptTouchEvent(true)
                                        false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            update = { map ->
                                map.overlays.clear()


                                val centerPoint =
                                    if (activity.latitude != null && activity.longitude != null) {
                                        GeoPoint(activity.latitude, activity.longitude)
                                    } else {

                                        val firstActive =
                                            activeGeofences.firstOrNull { it.geofenceData != null }
                                        if (firstActive != null) {
                                            val (lat, lng, _) = firstActive.geofenceData!!
                                            GeoPoint(lat, lng)
                                        } else {
                                            GeoPoint(40.2033, -8.4103)
                                        }
                                    }

                                map.controller.setCenter(centerPoint)


                                activeGeofences.forEach { rule ->
                                    val isAlertContext = activity.type == ActivityAlertType.GEOFENCE

                                    if (isAlertContext) {
                                        rule.geofenceData?.let { (lat, lng, radius) ->
                                            val circle = Polygon(map)
                                            circle.points = Polygon.pointsAsCircle(
                                                GeoPoint(lat, lng),
                                                radius.toDouble()
                                            )

                                            val color = Red700

                                            circle.fillPaint.color = android.graphics.Color.argb(
                                                70,
                                                (color.red * 255).toInt(),
                                                (color.green * 255).toInt(),
                                                (color.blue * 255).toInt()
                                            )
                                            circle.outlinePaint.color = android.graphics.Color.argb(
                                                255,
                                                (color.red * 255).toInt(),
                                                (color.green * 255).toInt(),
                                                (color.blue * 255).toInt()
                                            )
                                            circle.outlinePaint.strokeWidth = 3f

                                            map.overlays.add(circle)
                                        }
                                    }
                                }


                                if (activity.latitude != null && activity.longitude != null) {
                                    val marker = org.osmdroid.views.overlay.Marker(map)
                                    marker.position =
                                        GeoPoint(activity.latitude, activity.longitude)
                                    marker.setAnchor(
                                        org.osmdroid.views.overlay.Marker.ANCHOR_CENTER,
                                        org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM
                                    )
                                    marker.title = activity.title
                                    map.overlays.add(marker)
                                }

                                map.invalidate()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))


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
                                val (icon, color, labelRes) = when (action.state) {
                                    MonitorActionState.SENT -> Triple(
                                        Icons.Default.Notifications,
                                        Slate400,
                                        R.string.status_active
                                    )

                                    MonitorActionState.VIEWED -> Triple(
                                        Icons.Default.Visibility,
                                        Indigo500,
                                        R.string.rule_fall
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Slate50, RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {


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
                                            text = "${stringResource(labelRes)} • ${action.time}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = color,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
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
                        text = stringResource(id = R.string.action_close),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

fun getMonitorIconForActivityType(type: ActivityAlertType): ImageVector {
    return when (type) {
        ActivityAlertType.SPEED -> Icons.Default.Speed
        ActivityAlertType.FALL -> Icons.Default.MonitorHeart
        ActivityAlertType.ACCIDENT -> Icons.Default.CarCrash
        ActivityAlertType.GEOFENCE -> Icons.Default.LocationOn
        ActivityAlertType.PANIC -> Icons.Default.Warning
        ActivityAlertType.BATTERY -> Icons.Default.BatteryAlert
        ActivityAlertType.INACTIVITY -> Icons.Default.Schedule
    }
}