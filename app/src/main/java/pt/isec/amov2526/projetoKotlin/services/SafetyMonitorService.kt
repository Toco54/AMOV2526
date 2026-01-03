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
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import pt.isec.amov2526.projetoKotlin.R
import pt.isec.amov2526.projetoKotlin.data.repository.AlertRepository
import pt.isec.amov2526.projetoKotlin.data.model.ActivityAlertType
import pt.isec.amov2526.projetoKotlin.data.model.MonitorRuleType
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

    private lateinit var locationHelper: LocationHelper
    private lateinit var sensorHelper: SensorHelper
    private lateinit var locationHandler: LocationHandler // [NOVO] Handler de Regras
    private lateinit var accelerometerHandler: AccelerometerHandler
    private lateinit var inactivityHandler: InactivityHandler

    private var pausedUntil: Long = 0L
    private val alertRepository = AlertRepository()
    private val ruleListeners = mutableListOf<ListenerRegistration>()

    private var alertJob: Job? = null

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

        // [NOVO] Instanciar LocationHandler com o mesmo locationHelper
        locationHandler = LocationHandler(locationHelper, serviceScope)

        accelerometerHandler = AccelerometerHandler(sensorHelper, serviceScope)
        inactivityHandler = InactivityHandler(sensorHelper, serviceScope)

        // Iniciar monitorização
        locationHelper.startLocationUpdates(5000L)
        accelerometerHandler.startMonitoring()
        inactivityHandler.startMonitoring()
        locationHandler.startMonitoring() // [NOVO] Iniciar verificação de regras

        registerReceiver(batteryReceiver, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        observeSensors()
        observeSystemStatus()
        startScheduleChecker()
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

        // [NOVO] Começar a escutar as regras da BD para alimentar o LocationHandler
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

    // [NOVO] Função para ler regras da BD e injetar no LocationHandler
    private fun startListeningToRules(uid: String) {
        // Limpar listeners antigos se houver recriação
        ruleListeners.forEach { it.remove() }
        ruleListeners.clear()

        // 1. Escutar Conexões (Monitores)
        val connListener = db.collection("users").document(uid).collection("connections")
            .whereEqualTo("role", "Monitor")
            .addSnapshotListener { connections, _ ->
                if (connections == null) return@addSnapshotListener

                // Reiniciar regras no Handler para evitar duplicados/lixo
                locationHandler.clearAllRules()

                connections.documents.forEach { connDoc ->
                    val monitorUid = connDoc.getString("uid") ?: return@forEach

                    // 2. Escutar Regras de cada Monitor
                    val ruleListener = db.collection("users").document(uid)
                        .collection("connections").document(monitorUid)
                        .collection("rules")
                        .whereEqualTo("isEnabled", true) // Apenas regras ativas
                        .addSnapshotListener { rules, _ ->
                            if (rules == null) return@addSnapshotListener

                            rules.documents.forEach { doc ->
                                // Ignorar se estiver desativada pelo Protegido
                                if (doc.getBoolean("isDisabledByProtege") == true) return@forEach
                                if (doc.getBoolean("deletedByProtege") == true) return@forEach

                                val typeStr = doc.getString("type") ?: ""
                                val param1 = doc.getString("param1") ?: ""
                                val ruleId = doc.id

                                try {
                                    when (typeStr) {
                                        "INACTIVITY" -> {
                                            // param1 esperado em minutos (ex: "10")
                                            val minutes = param1.toLongOrNull()
                                            if (minutes != null) {
                                                // Converte minutos para milissegundos para o Handler
                                                inactivityHandler.addRule(InactivityRule(ruleId, minutes * 60 * 1000))
                                            }
                                        }
                                        "SPEED" -> {
                                            val limit = param1.toFloatOrNull()
                                            if (limit != null) {
                                                locationHandler.addSpeedRule(SpeedRule(ruleId, limit))
                                            }
                                        }
                                        "GEOFENCING" -> {
                                            // Formato esperado: "lat,lng,radius" ou "lat;lng;radius"
                                            val parts = param1.split(Regex("[,;]"))
                                            if (parts.size >= 3) {
                                                val lat = parts[0].toDoubleOrNull()
                                                val lng = parts[1].toDoubleOrNull()
                                                val rad = parts[2].toFloatOrNull()
                                                if (lat != null && lng != null && rad != null) {
                                                    locationHandler.addGeofence(GeofenceRule(ruleId, lat, lng, rad))
                                                }
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Erro no parse da regra, ignora
                                }
                            }
                        }
                    ruleListeners.add(ruleListener)
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
                db.collection("users").document(uid).update(
                    mapOf("systemStatus" to "ACTIVE", "globalPausedUntil" to 0L)
                )
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
            updateNotification(isPaused = !isProtectionActive)
        } else {
            updateNotification(isPaused = !isProtectionActive)
        }
    }

    private fun startHeartbeat(uid: String) {
        serviceScope.launch {
            while (isActive) {
                try {
                    db.collection("users").document(uid).update("lastUpdate", System.currentTimeMillis())
                } catch (e: Exception) {}
                delay(60000L)
            }
        }
    }

    private fun observeSensors() {
        // [EXISTENTE] Acelerómetro (Queda/Acidente)
        serviceScope.launch {
            accelerometerHandler.onFallDetected.collect {
                if (isProtectionActive) triggerAlertSequence(ActivityAlertType.FALL)
            }
        }

        serviceScope.launch {
            accelerometerHandler.onAccidentDetected.collect {
                if (isProtectionActive) triggerAlertSequence(ActivityAlertType.ACCIDENT)
            }
        }

        serviceScope.launch {
            inactivityHandler.inactivityAlerts.collect { rule ->
                if (isProtectionActive) {
                    val minutes = rule.maxDurationMillis / 60000
                    triggerAlertSequence(ActivityAlertType.INACTIVITY, "> $minutes min inactive")
                }
            }
        }

        // [NOVO] LocationHandler (Geofencing/Velocidade)
        serviceScope.launch {
            locationHandler.alerts.collect { alert ->
                if (isProtectionActive) {
                    when (alert) {
                        is LocationAlert.GeofenceExit -> {
                            triggerAlertSequence(ActivityAlertType.GEOFENCE)
                        }
                        is LocationAlert.SpeedLimitExceeded -> {
                            val current = alert.currentSpeedKmh.toInt()
                            val limit = alert.limitKmh.toInt()
                            triggerAlertSequence(ActivityAlertType.SPEED, "$current km/h (Limit: $limit)")
                        }
                    }
                }
            }
        }
    }

    private fun triggerAlertSequence(type: ActivityAlertType, details: String? = null) {
        if (alertJob?.isActive == true) return

        launchAlertUI(type, details)

        alertJob = serviceScope.launch {
            delay(10000L)

            if (isActive) {
                // [ATUALIZADO] Títulos corretos para os novos tipos
                val title = when(type) {
                    ActivityAlertType.FALL -> "Queda Detetada"
                    ActivityAlertType.ACCIDENT -> "Acidente de Viação"
                    ActivityAlertType.BATTERY -> "Bateria Fraca"
                    ActivityAlertType.GEOFENCE -> "Saiu da Zona Segura"
                    ActivityAlertType.SPEED -> "Velocidade Excessiva"
                    ActivityAlertType.INACTIVITY -> "Inatividade Detetada"
                    else -> "Alerta Automático"
                }
                // Adiciona detalhes ao título do alerta se existirem
                val finalTitle = if (details != null) "$title: $details" else title
                createAlert(type, finalTitle)
                getSystemService(NotificationManager::class.java).cancel(999)
            }
            alertJob = null
        }
    }

    private fun launchAlertUI(type: ActivityAlertType, details: String? = null) {
        val alertTime = System.currentTimeMillis() // [NOVO] Hora de início do alerta
        val intent = Intent(this, pt.isec.amov2526.projetoKotlin.ui.MainActivity::class.java).apply {
            action = "ACTION_SHOW_ALERT"
            putExtra("ALERT_TYPE", type.name)
            putExtra("ALERT_TIMESTAMP", alertTime)
            if (details != null) {
                putExtra("ALERT_DETAILS", details)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = getSystemService(NotificationManager::class.java)
        val channelId = "SafetySecAlertChannel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Alertas de Emergência", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_shield_check)
            .setContentTitle("ALERTA DE SEGURANÇA")
            .setContentText("A enviar ajuda em 10s...")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        notificationManager.notify(999, notification)

        // [IMPORTANTE] Tenta abrir a Activity diretamente (útil se a app já estiver aberta ou tiver permissão de Overlay)
        try {
            // Se tivermos permissão de Overlay (ou for Android < 10), isto vai forçar a abertura
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || android.provider.Settings.canDrawOverlays(this)) {
                startActivity(intent)
            } else {
                // Mesmo sem permissão, tentamos (alguns fabricantes permitem, ou se a app já estiver em foreground funciona)
                startActivity(intent)
            }
        } catch (e: Exception) {
            // Se falhar, o FullScreenIntent da notificação acima serve como backup
        }
    }

    private suspend fun createAlert(type: ActivityAlertType, title: String) {
        if (!isProtectionActive) return

        val loc = locationHelper.currentLocation.value
        alertRepository.createAlert(
            type = type,
            title = title,
            latitude = loc?.latitude,
            longitude = loc?.longitude
        )
    }

    private fun startForegroundService() {
        val channelId = "SafetySecChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Monitorização SafetySec", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("SafetySec Ativo")
            .setContentText("Estamos a proteger-te em segundo plano.")
            .setSmallIcon(R.drawable.ic_shield_check)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1, notification)
        }
    }

    private fun updateNotification(isPaused: Boolean) {
        val channelId = "SafetySecChannel"
        val title = if (isPaused) "SafetySec em Pausa" else "SafetySec Ativo"
        val text = if (isPaused) "A proteção está suspensa." else "Proteção ativa."
        val icon = if (isPaused) R.drawable.ic_shield_check else R.drawable.ic_shield_check

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(icon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        getSystemService(NotificationManager::class.java).notify(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        getSystemService(NotificationManager::class.java)?.cancel(1)

        try { unregisterReceiver(batteryReceiver) } catch (e: Exception) {}

        // Limpar listeners de regras
        ruleListeners.forEach { it.remove() }

        locationHelper.stopLocationUpdates()
        accelerometerHandler.stopMonitoring()
        inactivityHandler.stopMonitoring()
        locationHandler.stopMonitoring() // [NOVO]
        serviceScope.cancel()
    }

    private var lastBatteryAlertTime = 0L

    private fun updateBatteryLevel(pct: Int) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update("batteryLevel", pct)

        if (pct <= 15 && isProtectionActive) {
            val now = System.currentTimeMillis()
            if (now - lastBatteryAlertTime > 900000L) {
                lastBatteryAlertTime = now
                serviceScope.launch {
                    createAlert(ActivityAlertType.BATTERY, "Bateria Fraca ($pct%)")
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}