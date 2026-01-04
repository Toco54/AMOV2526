package pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.alert


import android.graphics.SurfaceTexture
import android.location.Location
import android.media.MediaPlayer
import android.net.Uri
import android.preference.PreferenceManager
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import pt.isec.amov2526.projetoKotlin.R
import pt.isec.amov2526.projetoKotlin.data.model.InfractionDisplayInfo
import pt.isec.amov2526.projetoKotlin.data.model.MonitorRule
import pt.isec.amov2526.projetoKotlin.ui.theme.AlertGreen
import pt.isec.amov2526.projetoKotlin.ui.theme.AlertRed
import pt.isec.amov2526.projetoKotlin.ui.theme.AlertYellow
import pt.isec.amov2526.projetoKotlin.ui.theme.HeaderColor
import pt.isec.amov2526.projetoKotlin.ui.theme.PinBoxColor
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate200
import java.io.File

@Composable
fun ConfirmedPanel(
    currentLocation: Location?,
    activeGeofences: List<MonitorRule> = emptyList(),
    infractions: List<InfractionDisplayInfo> = emptyList(),
    previewView: PreviewView? = null,
    isRecordingFinished: Boolean = false,
    recordingStartTime: Long = 0L,
    hasError: Boolean = false,
    errorMessage: String? = null,
    recordedVideoFile: File? = null,
    onClose: () -> Unit = {}
) {
    var remainingSeconds by remember { mutableLongStateOf(12L) }
    val isTimeUp = remainingSeconds <= 0


    val visibleMonitors = remember { mutableStateListOf<String>() }

    LaunchedEffect(infractions) {
        visibleMonitors.clear()
        visibleMonitors.addAll(infractions.map { it.monitorName }.distinct())

        activeGeofences.forEach { rule ->
            if (!visibleMonitors.contains(rule.monitorName)) {
                visibleMonitors.add(rule.monitorName)
            }
        }
    }

    val context = LocalContext.current

    LaunchedEffect(recordingStartTime, isRecordingFinished) {
        if (!isRecordingFinished && recordingStartTime > 0) {
            while (true) {
                val elapsed = System.currentTimeMillis() - recordingStartTime
                val left = 12000L - elapsed
                remainingSeconds = (left / 1000L).coerceAtLeast(0L)
                if (left <= 0) break
                delay(500L)
            }
        } else if (isRecordingFinished) {
            remainingSeconds = 0
        }
    }


    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            PreferenceManager.getDefaultSharedPreferences(context)
        )

        Configuration.getInstance().userAgentValue = context.packageName
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val headerBg = when {
            hasError -> AlertYellow
            isRecordingFinished -> AlertGreen
            else -> AlertRed
        }
        val headerTitle = when {
            hasError -> errorMessage ?: stringResource(id = R.string.evidence_failed)
            isRecordingFinished -> stringResource(id = R.string.evidence_uploaded)
            else -> stringResource(id = R.string.evidence_help_requested)
        }
        val headerSub = when {
            hasError -> stringResource(id = R.string.evidence_failed_sub)
            isRecordingFinished -> stringResource(id = R.string.evidence_uploaded_sub)
            else -> stringResource(id = R.string.evidence_requested_sub)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(headerBg)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isRecordingFinished && !hasError) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (hasError) Color.Black else Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = headerTitle,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        color = if (hasError) Color.Black else Color.White
                    )
                }
                Text(
                    text = headerSub,
                    color = (if (hasError) Color.Black else Color.White).copy(alpha = 0.9f),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(HeaderColor)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.evidence_camera_recording),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }


        val showVideoPlayer = isRecordingFinished && !hasError && recordedVideoFile != null
        var isPlaying by remember { mutableStateOf(true) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                .background(Color.Black)
                .border(
                    width = 1.dp,
                    color = Color.Gray,
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                )
                .clickable(enabled = showVideoPlayer) { isPlaying = !isPlaying },
            contentAlignment = Alignment.Center
        ) {
            if (showVideoPlayer && recordedVideoFile != null) {

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
                        if (isPlaying && !mediaPlayer.isPlaying) mediaPlayer.start()
                        else if (!isPlaying && mediaPlayer.isPlaying) mediaPlayer.pause()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        TextureView(ctx).apply {
                            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                override fun onSurfaceTextureAvailable(
                                    surface: SurfaceTexture,
                                    w: Int,
                                    h: Int
                                ) {
                                    try {
                                        mediaPlayer.setDataSource(
                                            ctx,
                                            Uri.fromFile(recordedVideoFile)
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

                if (previewView != null) {
                    AndroidView(
                        factory = {
                            (previewView.parent as? ViewGroup)?.removeView(previewView)
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (!isRecordingFinished) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isTimeUp) {
                        Box(modifier = Modifier
                            .size(8.dp)
                            .background(Color.Red, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "REC",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${remainingSeconds}s",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    } else {
                        val uploadText =
                            if (hasError) "UPLOAD FAILED" else stringResource(id = R.string.evidence_uploading)
                        val uploadColor = if (hasError) AlertYellow else Color.White
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = uploadColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = uploadText,
                            color = uploadColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))


        Text(
            text = stringResource(id = R.string.evidence_live_location),
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Slate200)
        ) {
            if (currentLocation != null) {

                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(18.0)


                            setOnTouchListener { v, event ->
                                v.parent.requestDisallowInterceptTouchEvent(true)
                                false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { map ->
                        val point = GeoPoint(currentLocation.latitude, currentLocation.longitude)


                        map.controller.setCenter(point)


                        map.overlays.clear()


                        activeGeofences.forEach { rule ->
                            val isVisible = visibleMonitors.contains(rule.monitorName)

                            if (isVisible) {
                                rule.geofenceData?.let { (lat, lng, radius) ->

                                    val hasInfraction = infractions.any {
                                        it.monitorName == rule.monitorName && it.ruleType == "GEOFENCING"
                                    }


                                    if (hasInfraction) {
                                        val circle = Polygon(map)
                                        circle.points = Polygon.pointsAsCircle(
                                            GeoPoint(lat, lng),
                                            radius.toDouble()
                                        )

                                        val color = AlertRed
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
                        }


                        val marker = Marker(map)
                        marker.position = point
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        marker.title = "Eu estou aqui!"
                        map.overlays.add(marker)

                        map.invalidate()
                    }
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(id = R.string.evidence_waiting_gps), color = Color.Gray)
                }
            }


            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                    .padding(4.dp)
            ) {

                val locText = if (currentLocation != null) {
                    "Lat: %.4f\nLng: %.4f".format(
                        currentLocation.latitude,
                        currentLocation.longitude
                    )
                } else {
                    stringResource(R.string.evidence_acquiring_gps)
                }

                Text(
                    text = locText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }


        if (infractions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.evidence_infringed_rules),
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PinBoxColor)
                    .padding(12.dp)
            ) {
                infractions.forEachIndexed { index, inf ->
                    val isSelected = visibleMonitors.contains(inf.monitorName)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                if (isSelected) {
                                    visibleMonitors.remove(inf.monitorName)
                                } else {
                                    visibleMonitors.add(inf.monitorName)
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        androidx.compose.material3.Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                if (checked) visibleMonitors.add(inf.monitorName)
                                else visibleMonitors.remove(inf.monitorName)
                            },
                            colors = androidx.compose.material3.CheckboxDefaults.colors(
                                checkedColor = AlertRed,
                                uncheckedColor = Color.Gray,
                                checkmarkColor = Color.White
                            )
                        )


                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(AlertRed.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = inf.monitorName.firstOrNull()?.toString() ?: "M",
                                color = AlertRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {

                            Text(
                                text = inf.monitorName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )


                            val detailText = when (inf.ruleType) {
                                "SPEED" -> "Limit: ${inf.parameter ?: "?"} km/h"
                                "INACTIVITY" -> "Max Time: ${inf.parameter ?: "?"} min"
                                "GEOFENCING" -> stringResource(id = R.string.evidence_left_safe_zone)
                                "FALL" -> "Fall Detected"
                                "ACCIDENT" -> "Accident Detected"
                                else -> inf.description
                            }

                            Text(
                                text = detailText,
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                    if (index < infractions.size - 1) {
                        Divider(color = Color.Gray.copy(alpha = 0.2f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isRecordingFinished) {
            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.evidence_close_panel),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        } else {
            Text(
                text = if (isTimeUp) stringResource(id = R.string.evidence_uploading_wait) else stringResource(
                    id = R.string.evidence_help_coming
                ),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
