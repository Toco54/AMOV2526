package pt.isec.amov2526.projetoKotlin.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import pt.isec.amov2526.projetoKotlin.R
import pt.isec.amov2526.projetoKotlin.data.repository.AlertRepository
import pt.isec.amov2526.projetoKotlin.data.model.ActivityAlertType
import pt.isec.amov2526.projetoKotlin.data.model.MonitorAction
import pt.isec.amov2526.projetoKotlin.data.model.MonitorActionState
import pt.isec.amov2526.projetoKotlin.data.model.MonitorRule
import pt.isec.amov2526.projetoKotlin.data.model.RuleType
import pt.isec.amov2526.projetoKotlin.utils.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class SafetyMonitorService : Service() {

    companion object {
        var isServiceRunning = false
        const val ACTION_CANCEL_ALERT = "pt.isec.amov2526.projetoKotlin.services.ACTION_CANCEL_ALERT"
        const val ACTION_CONFIRM_ALERT = "pt.isec.amov2526.projetoKotlin.services.ACTION_CONFIRM_ALERT"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var isProtectionActive = true

    // Variáveis de Estado Interno (para Agendamento)
    private var systemStatus = "ACTIVE"
    private var globalScheduleDays: List<Int> = listOf(1, 2, 3, 4, 5)
    private var globalScheduleStart: String = "09:00"
    private var globalScheduleEnd: String = "18:00"
    private var isGlobalScheduleEnabled: Boolean = false

    // Estados dos Grupos
    private var groupStatuses = mutableMapOf<RuleType, pt.isec.amov2526.projetoKotlin.ui.viewmodels.GuardViewModel.ProtectionStatus>()

    // Cache para monitorizar alterações de estado e evitar spam de logs
    private val ruleStatusCache = mutableMapOf<String, Boolean>()

    private lateinit var locationHelper: LocationHelper
    private lateinit var sensorHelper: SensorHelper
    private lateinit var locationHandler: LocationHandler
    private lateinit var accelerometerHandler: AccelerometerHandler
    private lateinit var inactivityHandler: InactivityHandler

    private var pausedUntil: Long = 0L
    private val alertRepository = AlertRepository()
    private val ruleListeners = mutableListOf<ListenerRegistration>()

    private var alertJob: Job? = null
    private var lastBatteryAlertTime = 0L // [RESTAURADO]

    private val batteryReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            val level = intent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level != -1 && scale != -1) {
                val pct = (level * 100 / scale.toFloat()).toInt()
                updateBatteryLevel(pct)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        startForegroundService()

        // Inicialização dos Helpers
        locationHelper = LocationHelper(this)
        sensorHelper = SensorHelper(this)

        // Instanciar Handlers com o verificador dinâmico (Lógica AND)
        val checker = { rule: MonitorRule -> isRuleEffectivelyActive(rule) }
        
        locationHandler = LocationHandler(locationHelper, serviceScope, checker)
        accelerometerHandler = AccelerometerHandler(sensorHelper, serviceScope, checker)
        inactivityHandler = InactivityHandler(sensorHelper, serviceScope, checker)

        // Iniciar monitorização
        locationHelper.startLocationUpdates(5000L)
        accelerometerHandler.startMonitoring()
        inactivityHandler.startMonitoring()
        locationHandler.startMonitoring()

        registerReceiver(batteryReceiver, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        observeSensors()
        observeSystemStatus()
        startScheduleChecker()
    }

    // Função central que implementa o AND lógico dos 3 níveis de ativação com Logging
    private fun isRuleEffectivelyActive(rule: MonitorRule): Boolean {
        val now = System.currentTimeMillis()
        
        // 1. NÍVEL GLOBAL (Sistema)
        val globalActive = isProtectionActive

        // 2. NÍVEL DE GRUPO (Tipo de Regra)
        val groupStatus = groupStatuses[rule.type]
        val groupActive = if (groupStatus != null) {
            TimeUtils.calculateStatus(
                isEnabled = groupStatus.isEnabled,
                pausedUntil = groupStatus.pausedUntil,
                isScheduleEnabled = groupStatus.isScheduleEnabled,
                days = groupStatus.scheduleDays,
                start = groupStatus.scheduleStart,
                end = groupStatus.scheduleEnd,
                now = now
            ).isActive
        } else true

        // 3. NÍVEL INDIVIDUAL (Regra)
        val localActive = TimeUtils.calculateStatus(
            isEnabled = rule.isEnabled && !rule.isDisabledByProtege,
            pausedUntil = rule.pausedUntil,
            isScheduleEnabled = rule.isScheduleEnabled,
            days = rule.scheduleDays,
            start = rule.scheduleStart,
            end = rule.scheduleEnd,
            now = now
        ).isActive
        
        val effectivelyActive = globalActive && groupActive && localActive

        // [LOG DE ALTERAÇÃO]
        val previousStatus = ruleStatusCache[rule.id]
        if (previousStatus != effectivelyActive) {
            Log.d("SafetyMonitorService", "ALTERAÇÃO DE ESTADO - Regra: ${rule.type} (Monitor: ${rule.monitorName})")
            Log.d("SafetyMonitorService", "  > Global Protection: $globalActive")
            Log.d("SafetyMonitorService", "  > Group Status (${rule.type}): $groupActive")
            Log.d("SafetyMonitorService", "  > Individual Rule (Final Local): $localActive")
            
            // Log detalhado da razão local
            if (!rule.isEnabled) Log.d("SafetyMonitorService", "    [!] DISABLED BY MONITOR")
            if (rule.isDisabledByProtege) Log.d("SafetyMonitorService", "    [!] DISABLED BY PROTEGE")
            
            Log.d("SafetyMonitorService", "  > RESULTADO FINAL (AND): $effectivelyActive")
            ruleStatusCache[rule.id] = effectivelyActive
        }
        
        return effectivelyActive
    }

    private fun startScheduleChecker() {
        serviceScope.launch {
            while (isActive) {
                evaluateProtectionStatus()
                delay(60000L) // Verifica a cada 60 segundos
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL_ALERT -> {
                alertJob?.cancel()
                alertJob = null
                getSystemService(NotificationManager::class.java).cancel(999)
            }
            ACTION_CONFIRM_ALERT -> { }
        }
        return START_NOT_STICKY
    }

    private fun observeSystemStatus() {
        val uid = auth.currentUser?.uid ?: return
        startHeartbeat(uid)

        startListeningToRules(uid)

        serviceScope.launch {
            db.collection("users").document(uid).addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    systemStatus = snapshot.getString("systemStatus") ?: "ACTIVE"

                    if (systemStatus == "OFFLINE") {
                        isProtectionActive = false
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        getSystemService(NotificationManager::class.java)?.cancel(1)
                        stopSelf()
                        return@addSnapshotListener
                    }

                    pausedUntil = snapshot.getLong("globalPausedUntil") ?: 0L
                    isGlobalScheduleEnabled = snapshot.getBoolean("isGlobalScheduleEnabled") ?: false
                    globalScheduleStart = snapshot.getString("globalScheduleStart") ?: "09:00"
                    globalScheduleEnd = snapshot.getString("globalScheduleEnd") ?: "18:00"
                    val daysList = snapshot.get("globalScheduleDays") as? List<Number>
                    globalScheduleDays = daysList?.map { it.toInt() } ?: listOf(1, 2, 3, 4, 5)

                    // Ler estados dos Grupos
                    val rawGroups = snapshot.get("activeProtections") as? Map<String, Any>
                    if (rawGroups != null) {
                        val newMap = mutableMapOf<RuleType, pt.isec.amov2526.projetoKotlin.ui.viewmodels.GuardViewModel.ProtectionStatus>()
                        rawGroups.forEach { (key, value) ->
                            try {
                                val type = RuleType.valueOf(key)
                                if (value is Map<*, *>) {
                                    newMap[type] = pt.isec.amov2526.projetoKotlin.ui.viewmodels.GuardViewModel.ProtectionStatus(
                                        isEnabled = value["enabled"] as? Boolean ?: true,
                                        pausedUntil = (value["pausedUntil"] as? Number)?.toLong() ?: 0L,
                                        scheduleDays = (value["scheduleDays"] as? List<Number>)?.map { it.toInt() } ?: emptyList(),
                                        scheduleStart = value["scheduleStart"] as? String ?: "",
                                        scheduleEnd = value["scheduleEnd"] as? String ?: "",
                                        isScheduleEnabled = value["isScheduleEnabled"] as? Boolean ?: false
                                    )
                                }
                            } catch (e: Exception) {}
                        }
                        groupStatuses = newMap
                    }

                    evaluateProtectionStatus()
                }
            }
        }

        serviceScope.launch {
            locationHelper.currentLocation.collect { loc ->
                if (loc != null && isProtectionActive) {
                    val updates = mapOf(
                        "lastLat" to loc.latitude,
                        "lastLng" to loc.longitude,
                        "lastUpdate" to System.currentTimeMillis()
                    )
                    db.collection("users").document(uid).update(updates)
                }
            }
        }
    }

    private fun startListeningToRules(uid: String) {
        ruleListeners.forEach { it.remove() }
        ruleListeners.clear()

        val connListener = db.collection("users").document(uid).collection("connections")
            .whereEqualTo("role", "Monitor")
            .addSnapshotListener { connections, _ ->
                if (connections == null) return@addSnapshotListener

                locationHandler.clearAllRules()
                accelerometerHandler.clearAllRules()
                inactivityHandler.clearAllRules()

                connections.documents.forEach { connDoc ->
                    val monitorUid = connDoc.getString("uid") ?: return@forEach
                    
                    // [CORREÇÃO] Se o nome não estiver no doc de conexão, tentamos buscar ao perfil do monitor
                    // (O ideal é estar no doc de conexão para performance, mas isto garante que mostra o nome real)
                    serviceScope.launch {
                        var monitorName = connDoc.getString("name")
                        if (monitorName == null || monitorName == "Monitor") {
                            try {
                                val monitorDoc = db.collection("users").document(monitorUid).get().await()
                                monitorName = monitorDoc.getString("name") ?: "Monitor"
                                // Opcional: Atualizar o doc de conexão local para futuras leituras rápidas
                                db.collection("users").document(uid).collection("connections").document(monitorUid).update("name", monitorName)
                            } catch (e: Exception) {
                                monitorName = "Monitor"
                            }
                        }

                        val ruleListener = db.collection("users").document(uid)
                            .collection("connections").document(monitorUid)
                            .collection("rules")
                            .addSnapshotListener { rules, _ ->
                                if (rules == null) return@addSnapshotListener

                                // Limpar regras antigas deste monitor antes de adicionar as novas (evita duplicados no monitoramento)
                                // Nota: Para simplificar, como os handlers usam lista e removem por ID, o addRule trata disso.

                                rules.documents.forEach { doc ->
                                    if (doc.getBoolean("isDisabledByProtege") == true) return@forEach
                                    if (doc.getBoolean("deletedByProtege") == true) return@forEach

                                    val typeStr = doc.getString("type") ?: ""
                                    val param1 = doc.getString("param1") ?: ""
                                    val param2 = doc.getString("param2") ?: ""
                                    val ruleId = doc.id
                                    val type = try { RuleType.valueOf(typeStr) } catch(e:Exception) { null }

                                    if (type != null) {
                                        // ... (rest of the logic remains same, but uses the fetched monitorName)
                                        val isEnabled = doc.getBoolean("isEnabled") ?: true
                                        val isScheduleEnabled = doc.getBoolean("isScheduleEnabled") ?: false
                                        val scheduleStart = doc.getString("scheduleStart") ?: "09:00"
                                        val scheduleEnd = doc.getString("scheduleEnd") ?: "18:00"
                                        val pausedUntil = doc.getLong("pausedUntil") ?: 0L
                                        val daysList = doc.get("scheduleDays") as? List<Number>
                                        val scheduleDays = daysList?.map { it.toInt() } ?: listOf(1,2,3,4,5)

                                        val statusResult = TimeUtils.calculateStatus(
                                            isEnabled = isEnabled,
                                            pausedUntil = pausedUntil,
                                            isScheduleEnabled = isScheduleEnabled,
                                            days = scheduleDays,
                                            start = scheduleStart,
                                            end = scheduleEnd,
                                            now = System.currentTimeMillis()
                                        )

                                        var geofenceData: Triple<Double, Double, Float>? = null
                                        if (type == RuleType.GEOFENCING) {
                                            val radius = param1.toFloatOrNull()
                                            val coordParts = param2.split(Regex("\\s*[,;]\\s*"))
                                            if (radius != null && coordParts.size >= 2) {
                                                val lat = coordParts[0].toDoubleOrNull()
                                                val lng = coordParts[1].toDoubleOrNull()
                                                if (lat != null && lng != null) {
                                                    geofenceData = Triple(lat, lng, radius)
                                                }
                                            }
                                        }

                                        val rule = MonitorRule(
                                            id = ruleId,
                                            monitorId = monitorUid,
                                            monitorName = monitorName!!, // <--- NOME REAL RECUPERADO
                                            parameter = param1,
                                            isEnabled = isEnabled,
                                            type = type,
                                            scheduleDays = scheduleDays,
                                            scheduleStart = scheduleStart,
                                            scheduleEnd = scheduleEnd,
                                            isScheduleEnabled = isScheduleEnabled,
                                            pausedUntil = pausedUntil,
                                            isEffectivelyActive = statusResult.isActive,
                                            geofenceData = geofenceData
                                        )

                                        when(type) {
                                            RuleType.SPEED, RuleType.GEOFENCING -> locationHandler.addRule(rule)
                                            RuleType.FALL, RuleType.ACCIDENT -> accelerometerHandler.addRule(rule)
                                            RuleType.INACTIVITY -> inactivityHandler.addRule(rule)
                                            else -> {}
                                        }
                                    }
                                }
                            }
                        synchronized(ruleListeners) {
                            ruleListeners.add(ruleListener)
                        }
                    }
                }
            }
        ruleListeners.add(connListener)
    }

    private fun evaluateProtectionStatus() {
        val now = System.currentTimeMillis()
        if (systemStatus == "PAUSED" && pausedUntil != -1L && pausedUntil < now && pausedUntil != 0L) {
            systemStatus = "ACTIVE"
            pausedUntil = 0L
            auth.currentUser?.uid?.let { uid ->
                db.collection("users").document(uid).update(mapOf("systemStatus" to "ACTIVE", "globalPausedUntil" to 0L))
            }
        }
        val statusResult = TimeUtils.calculateStatus(
            isEnabled = (systemStatus == "ACTIVE"),
            pausedUntil = pausedUntil,
            isScheduleEnabled = isGlobalScheduleEnabled,
            days = globalScheduleDays,
            start = globalScheduleStart,
            end = globalScheduleEnd,
            now = now
        )
        val shouldBeActive = statusResult.isActive
        if (isProtectionActive != shouldBeActive) {
            isProtectionActive = shouldBeActive
            
            // [NOVO] Se o sistema reativou, limpamos o anti-spam para garantir que infrações persistentes sejam reportadas novamente
            if (isProtectionActive) {
                locationHandler.resetAlertStates()
            }
            
            updateNotification(isPaused = !isProtectionActive)
        } else {
            updateNotification(isPaused = !isProtectionActive)
        }
    }

    private fun startHeartbeat(uid: String) {
        serviceScope.launch {
            while (isActive) {
                try { db.collection("users").document(uid).update("lastUpdate", System.currentTimeMillis()) } catch (e: Exception) {}
                delay(60000L)
            }
        }
    }

    private fun observeSensors() {
        serviceScope.launch {
            accelerometerHandler.infractions.collect { result ->
                if (isProtectionActive && result.infractions.isNotEmpty()) {
                    val firstInfraction = result.infractions.first()
                    val alertType = when(firstInfraction.ruleType) {
                        RuleType.FALL -> ActivityAlertType.FALL
                        RuleType.ACCIDENT -> ActivityAlertType.ACCIDENT
                        else -> ActivityAlertType.ACCIDENT
                    }
                    triggerAlertSequence(
                        type = alertType,
                        details = result.infractions.joinToString(", ") { it.description },
                        monitors = result.activeMonitorsToNotify.toList(),
                        infractions = result.infractions
                    )
                }
            }
        }
        serviceScope.launch {
            inactivityHandler.infractions.collect { result ->
                if (isProtectionActive && result.infractions.isNotEmpty()) {
                    triggerAlertSequence(
                        type = ActivityAlertType.INACTIVITY,
                        details = result.infractions.joinToString(", ") { it.description },
                        monitors = result.activeMonitorsToNotify.toList(),
                        infractions = result.infractions
                    )
                }
            }
        }
        serviceScope.launch {
            locationHandler.infractions.collect { result ->
                if (isProtectionActive && result.infractions.isNotEmpty()) {
                    val firstInfraction = result.infractions.first()
                    val alertType = when(firstInfraction.ruleType) {
                        RuleType.GEOFENCING -> ActivityAlertType.GEOFENCE
                        RuleType.SPEED -> ActivityAlertType.SPEED
                        else -> ActivityAlertType.GEOFENCE
                    }
                    triggerAlertSequence(
                        type = alertType, 
                        details = result.infractions.joinToString(", ") { it.description },
                        monitors = result.activeMonitorsToNotify.toList(),
                        infractions = result.infractions
                    )
                }
            }
        }
    }

    private fun triggerAlertSequence(
        type: ActivityAlertType, 
        details: String? = null,
        monitors: List<String>? = null,
        infractions: List<pt.isec.amov2526.projetoKotlin.data.model.RuleInfraction>? = null
    ) {
        if (alertJob?.isActive == true) return
        
        // [NOVO] Gerar ID do alerta IMEDIATAMENTE para sincronizar com o vídeo
        val generatedAlertId = java.util.UUID.randomUUID().toString()
        
        launchAlertUI(type, details, infractions, generatedAlertId)
        
        alertJob = serviceScope.launch {
            delay(10000L)
            if (isActive) {
                val title = when(type) {
                    ActivityAlertType.FALL -> "Queda Detetada"
                    ActivityAlertType.ACCIDENT -> "Acidente de Viação"
                    ActivityAlertType.BATTERY -> "Bateria Fraca"
                    ActivityAlertType.GEOFENCE -> "Saiu da Zona Segura"
                    ActivityAlertType.SPEED -> "Velocidade Excessiva"
                    ActivityAlertType.INACTIVITY -> "Inatividade Detetada"
                    else -> "Alerta Automático"
                }
                createAlert(type, if (details != null) "$title: $details" else title, monitors, infractions, generatedAlertId)
                getSystemService(NotificationManager::class.java).cancel(999)
            }
            alertJob = null
        }
    }

    private fun launchAlertUI(
        type: ActivityAlertType, 
        details: String? = null,
        infractions: List<pt.isec.amov2526.projetoKotlin.data.model.RuleInfraction>? = null,
        alertId: String? = null // [NOVO]
    ) {
        val alertTime = System.currentTimeMillis()
        val intent = Intent(this, pt.isec.amov2526.projetoKotlin.ui.MainActivity::class.java).apply {
            action = "ACTION_SHOW_ALERT"
            putExtra("ALERT_TYPE", type.name)
            putExtra("ALERT_TIMESTAMP", alertTime)
            putExtra("ALERT_ID", alertId) // [NOVO] Passar o ID para a UI
            if (details != null) putExtra("ALERT_DETAILS", details)
            // [RESTAURADO] Serialização das infrações
            if (infractions != null) {
                val serialized = infractions.joinToString(";") { 
                    val safeName = it.monitorName.replace("|", "").replace(";", "")
                    val safeDesc = it.description.replace("|", "").replace(";", "")
                    val safeParam = it.ruleParameter?.replace("|", "")?.replace(";", "") ?: ""
                    "$safeName|${it.ruleType.name}|$safeDesc|$safeParam" 
                }
                putExtra("ALERT_INFRACTIONS", serialized)
            }

            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notificationManager = getSystemService(NotificationManager::class.java)
        val channelId = "SafetySecAlertChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Alertas de Emergência", NotificationManager.IMPORTANCE_HIGH).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_shield_check).setContentTitle("ALERTA DE SEGURANÇA").setContentText("A enviar ajuda em 10s...")
            .setPriority(NotificationCompat.PRIORITY_MAX).setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true).setAutoCancel(false).setOngoing(true).build()
        notificationManager.notify(999, notification)
        try { 
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || android.provider.Settings.canDrawOverlays(this)) {
                startActivity(intent) 
            } else {
                startActivity(intent)
            }
        } catch (e: Exception) {}
    }

    private suspend fun createAlert(type: ActivityAlertType, title: String, monitors: List<String>? = null, infractions: List<pt.isec.amov2526.projetoKotlin.data.model.RuleInfraction>? = null, forcedId: String? = null) {
        if (!isProtectionActive) return
        val loc = locationHelper.currentLocation.value
        
        // [MODIFICADO] Lógica mais robusta para identificar quem notificar
        var initialActions: List<MonitorAction>? = null

        if (infractions != null && infractions.isNotEmpty()) {
            // Se temos infrações específicas, notificamos esses monitores
            initialActions = infractions.map { inf ->
                MonitorAction(
                    name = inf.monitorName,
                    state = MonitorActionState.SENT,
                    time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                )
            }
        } else if (monitors != null && monitors.isNotEmpty()) {
            // Se temos IDs de monitores mas não a infração (ex: bateria), tentamos obter os nomes
            try {
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    val snapshot = db.collection("users").document(uid)
                        .collection("connections")
                        .whereIn("uid", monitors)
                        .get().await()
                    
                    initialActions = snapshot.documents.map { doc ->
                        MonitorAction(
                            name = doc.getString("name") ?: "Monitor",
                            state = MonitorActionState.SENT,
                            time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                        )
                    }
                }
            } catch (e: Exception) {
                // Fallback simples se a query falhar
                initialActions = monitors.map { MonitorAction("Monitor", MonitorActionState.SENT, "") }
            }
        }

        // Se ainda for nulo (Alerta Global/Panico), buscamos todos os monitores
        if (initialActions == null) {
            try {
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    val snapshot = db.collection("users").document(uid)
                        .collection("connections").whereEqualTo("role", "Monitor").get().await()
                    
                    initialActions = snapshot.documents.map { doc ->
                        MonitorAction(
                            name = doc.getString("name") ?: "Monitor",
                            state = MonitorActionState.SENT,
                            time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("SafetyMonitorService", "Error fetching monitors for alert", e)
            }
        }

        Log.d("SafetyMonitorService", "Creating Alert: $title | Notified: ${initialActions?.size ?: 0} monitors")
        initialActions?.forEach { Log.d("SafetyMonitorService", "  -> Monitor: ${it.name}") }

        alertRepository.createAlert(
            type = type, 
            title = title, 
            latitude = loc?.latitude, 
            longitude = loc?.longitude, 
            visibleToMonitors = monitors,
            initialActions = initialActions,
            forcedId = forcedId // [NOVO]
        )
    }

    private fun startForegroundService() {
        val channelId = "SafetySecChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Monitorização SafetySec", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("SafetySec Ativo").setContentText("Estamos a proteger-te em segundo plano.")
            .setSmallIcon(R.drawable.ic_shield_check).setPriority(NotificationCompat.PRIORITY_LOW).setOngoing(true).build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        else startForeground(1, notification)
    }

    private fun updateNotification(isPaused: Boolean) {
        val channelId = "SafetySecChannel"
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(if (isPaused) "SafetySec em Pausa" else "SafetySec Ativo")
            .setContentText(if (isPaused) "A proteção está suspensa." else "Proteção ativa.")
            .setSmallIcon(R.drawable.ic_shield_check).setPriority(NotificationCompat.PRIORITY_LOW).setOngoing(true).setOnlyAlertOnce(true).build()
        getSystemService(NotificationManager::class.java).notify(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        getSystemService(NotificationManager::class.java)?.cancel(1)
        try { unregisterReceiver(batteryReceiver) } catch (e: Exception) {}
        ruleListeners.forEach { it.remove() }
        locationHelper.stopLocationUpdates()
        accelerometerHandler.stopMonitoring()
        inactivityHandler.stopMonitoring()
        locationHandler.stopMonitoring()
        serviceScope.cancel()
    }

    // [RESTAURADO] Lógica de cooldown da bateria
    private fun updateBatteryLevel(pct: Int) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update("batteryLevel", pct)
        
        if (pct <= 15 && isProtectionActive) {
            val now = System.currentTimeMillis()
            if (now - lastBatteryAlertTime > 900000L) { // 15 minutos de cooldown
                lastBatteryAlertTime = now
                serviceScope.launch { createAlert(ActivityAlertType.BATTERY, "Bateria Fraca ($pct%)", null) }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}