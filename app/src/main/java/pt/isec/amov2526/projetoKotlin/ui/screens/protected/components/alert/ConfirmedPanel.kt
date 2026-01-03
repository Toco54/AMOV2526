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
import org.osmdroid.views.overlay.Marker // [RESTAURADO]
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.InfractionDisplayInfo
import org.osmdroid.views.overlay.Polygon 
import pt.isec.amov2526.projetoKotlin.data.model.MonitorRule

// --- Theme Colors ---
private val AlertRed = Color(0xFFDC2626)
private val AlertYellow = Color(0xFFFFC107) 
private val SafeGreen = Color(0xFF4CAF50)   
private val TabSelectedColor = Color(0xFF334155) // [RESTAURADO]
private val TabUnselectedColor = Color(0xFF1E293B) // [RESTAURADO]

@Composable
fun ConfirmedPanel(
    currentLocation: Location?,
    activeGeofences: List<MonitorRule> = emptyList(), // [NOVO]
    infractions: List<InfractionDisplayInfo> = emptyList() // [NOVO]
) {

    var selectedCameraTab by remember { mutableIntStateOf(0) } // 0 = Back, 1 = Front

    // [NOVO] Estado para controlar visibilidade das zonas por monitor
    // Inicialmente todos os monitores com infrações estão visíveis
    val visibleMonitors = remember { mutableStateListOf<String>() }

    LaunchedEffect(infractions) {
        visibleMonitors.clear()
        visibleMonitors.addAll(infractions.map { it.monitorName }.distinct())
        // Adiciona também monitores das activeGeofences que possam não estar nas infrações (opcional, mas seguro)
        activeGeofences.forEach { rule ->
            if (!visibleMonitors.contains(rule.monitorName)) {
                visibleMonitors.add(rule.monitorName)
            }
        }
    }

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
            .verticalScroll(rememberScrollState())
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
                .height(250.dp) // Altura fixa para o mapa dentro do Scroll
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE2E8F0))
        ) {
            if (currentLocation != null) {
                // Aqui usamos o MapView do OSMDroid dentro de uma AndroidView
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK) // Estilo de mapa padrão
                            setMultiTouchControls(true) // Permitir zoom com dedos
                            controller.setZoom(18.0) // Zoom inicial
                            
                            // [CORREÇÃO] Prevenir que o ScrollView pai intercepte o toque no mapa
                            setOnTouchListener { v, event ->
                                v.parent.requestDisallowInterceptTouchEvent(true)
                                false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { map ->
                        val point = GeoPoint(currentLocation.latitude, currentLocation.longitude)

                        // Centrar o mapa na localização atual
                        map.controller.setCenter(point)

                        // Limpar marcadores e overlays antigos
                        map.overlays.clear()

                        // 1. Desenhar Geofences (Apenas dos monitores visíveis)
                        Log.d("ConfirmedPanel", "Active Geofences: ${activeGeofences.size}")
                        activeGeofences.forEach { rule ->
                            // Só desenha se o monitor estiver na lista de visíveis
                            val isVisible = visibleMonitors.contains(rule.monitorName)
                            Log.d("ConfirmedPanel", "Rule for ${rule.monitorName}: isVisible=$isVisible, hasGeoData=${rule.geofenceData != null}")
                            
                            if (isVisible) {
                                rule.geofenceData?.let { (lat, lng, radius) ->
                                    val circle = Polygon(map)
                                    circle.points = Polygon.pointsAsCircle(GeoPoint(lat, lng), radius.toDouble())
                                    
                                    // Verificar se esta regra pertence a um monitor que gerou infração de Geofence
                                    val hasInfraction = infractions.any { 
                                        it.monitorName == rule.monitorName && it.ruleType == "GEOFENCING" 
                                    }

                                    Log.d("ConfirmedPanel", "  -> Drawing Zone: Lat=$lat, Lng=$lng, Radius=$radius, Color=${if(hasInfraction) "RED" else "GREEN"}")

                                    // Cor: Vermelho se houve infração (violada), Verde se está seguro
                                    val color = if (hasInfraction) AlertRed else SafeGreen
                                    
                                    circle.fillPaint.color = android.graphics.Color.argb(70, (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt()) // Transparente
                                    circle.outlinePaint.color = android.graphics.Color.argb(255, (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())
                                    circle.outlinePaint.strokeWidth = 3f

                                    map.overlays.add(circle)
                                }
                            }
                        }

                        // 2. Adicionar Marcador do User
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
        
        // 5. Infractions List
        if (infractions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Infringed Rules (Select to View on Map)",
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 8.dp)
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B))
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
                        // [NOVO] Checkbox de seleção
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
                        
                        // Monitor Icon/Name
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
                            // Primary: Monitor Name
                            Text(
                                text = inf.monitorName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            
                            // Secondary: Detail based on Type
                            val detailText = when(inf.ruleType) {
                                "SPEED" -> "Limit: ${inf.parameter ?: "?"} km/h"
                                "INACTIVITY" -> "Max Time: ${inf.parameter ?: "?"} min"
                                "GEOFENCING" -> "Left Safe Zone"
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
