package pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.alert


import android.location.Location
import android.preference.PreferenceManager
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.ui.geometry.Offset

import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

// --- Theme Colors ---
private val AlertRed = Color(0xFFDC2626)
private val MapColor = Color(0xFFE2E8F0)
private val MapRoadColor = Color(0xFFCBD5E1)
private val TabSelectedColor = Color(0xFF334155)
private val TabUnselectedColor = Color(0xFF1E293B)

@Composable
fun ConfirmedPanel(currentLocation: Location?) {

    var selectedCameraTab by remember { mutableIntStateOf(0) } // 0 = Back, 1 = Front

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    //(tiles) da internet
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            PreferenceManager.getDefaultSharedPreferences(context)
        )
        // Opcional: Define um UserAgent para evitar bloqueios dos servidores do OSM
        Configuration.getInstance().userAgentValue = context.packageName
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Status Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AlertRed)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "HELP REQUESTED",
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        color = Color.White
                    )
                }
                Text(
                    text = "Monitors Notified • Recording Evidence",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Camera Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(TabUnselectedColor)
        ) {
            CameraTab(
                title = "BACK CAM",
                isSelected = selectedCameraTab == 0,
                onClick = { selectedCameraTab = 0 },
                modifier = Modifier.weight(1f)
            )
            CameraTab(
                title = "FRONT CAM",
                isSelected = selectedCameraTab == 1,
                onClick = { selectedCameraTab = 1 },
                modifier = Modifier.weight(1f)
            )
        }

        // 3. Video Feed Area
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
                ),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    previewView.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    previewView
                },
                modifier = Modifier.fillMaxSize(),
                update =  {previewView ->
                    val cameraSelector = if (selectedCameraTab == 0) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build()
                        preview.setSurfaceProvider(previewView.surfaceProvider)

                        try {
                            // Desliga casos de uso anteriores e liga a nova câmara
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview
                            )
                        } catch (e: Exception) {
                            Log.e("CameraX", "Binding failed", e)
                        }
                    }, ContextCompat.getMainExecutor(context))

                }
            )

            Row (
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically

            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.Red, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "REC",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Map View
        Text(
            text = "Live Location",
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE2E8F0))
                //.background(MapColor)
        ) {
//            Canvas(modifier = Modifier.fillMaxSize()) {
//                val width = size.width
//                val height = size.height
//                drawLine(
//                    color = MapRoadColor,
//                    start = Offset(width * 0.6f, 0f),
//                    end = Offset(width * 0.6f, height),
//                    strokeWidth = 40f
//                )
//                drawLine(
//                    color = MapRoadColor,
//                    start = Offset(0f, height * 0.4f),
//                    end = Offset(width, height * 0.4f),
//                    strokeWidth = 40f
//                )
//            }
//            Icon(
//                imageVector = Icons.Default.LocationOn,
//                contentDescription = "My Location",
//                tint = AlertRed,
//                modifier = Modifier
//                    .size(48.dp)
//                    .align(Alignment.Center)
//                    .offset(y = (-24).dp)
//            )
//            Box(
//                modifier = Modifier
//                    .size(16.dp)
//                    .align(Alignment.Center)
//                    .background(AlertRed.copy(alpha = 0.3f), CircleShape)
//            )

            if (currentLocation != null) {
                // Aqui usamos o MapView do OSMDroid dentro de uma AndroidView
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK) // Estilo de mapa padrão
                            setMultiTouchControls(true) // Permitir zoom com dedos
                            controller.setZoom(18.0) // Zoom inicial
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { map ->
                        val point = GeoPoint(currentLocation.latitude, currentLocation.longitude)

                        // Centrar o mapa na localização atual
                        map.controller.setCenter(point)

                        // Limpar marcadores antigos e adicionar o novo
                        map.overlays.clear()
                        val marker = Marker(map)
                        marker.position = point
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        marker.title = "Eu estou aqui!"
                        map.overlays.add(marker)

                        map.invalidate() // Forçar atualização visual
                    }
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("A aguardar sinal de GPS...", color = Color.Gray)
                }
            }


            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                    .padding(4.dp)
            ) {
                // Se tivermos localização, mostramos Lat/Lng. Se não, "A adquirir..."
                val locText = if (currentLocation != null) {
                    "Lat: %.4f\nLng: %.4f".format(currentLocation.latitude, currentLocation.longitude)
                } else {
                    "A adquirir GPS..."
                }

                Text(
                    text = locText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Keep the app open. Help is coming.",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun CameraTab(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(if (isSelected) TabSelectedColor else Color.Transparent)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(AlertRed)
            )
        }
    }
}
