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
import pt.isec.amov2526.projetoKotlin.utils.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SafetyMonitorService : Service() {

    companion object {
        var isServiceRunning = false
        // [ALTERAÇÃO 1] Constantes para comunicação com o Ecrã
        const val ACTION_CANCEL_ALERT = "pt.isec.amov2526.projetoKotlin.services.ACTION_CANCEL_ALERT"
        const val ACTION_CONFIRM_ALERT = "pt.isec.amov2526.projetoKotlin.services.ACTION_CONFIRM_ALERT"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var isProtectionActive = true

    private lateinit var locationHelper: LocationHelper
    private lateinit var sensorHelper: SensorHelper
    private lateinit var accelerometerHandler: AccelerometerHandler
    private lateinit var inactivityHandler: InactivityHandler

    private var pausedUntil: Long = 0L
    private val alertRepository = AlertRepository()

    // [ALTERAÇÃO 2] Variável para controlar o temporizador de 10s
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

        locationHelper = LocationHelper(this)
        sensorHelper = SensorHelper(this)
        accelerometerHandler = AccelerometerHandler(sensorHelper, serviceScope)
        inactivityHandler = InactivityHandler(sensorHelper, serviceScope)

        locationHelper.startLocationUpdates(5000L)
        accelerometerHandler.startMonitoring()
        inactivityHandler.startMonitoring()

        registerReceiver(batteryReceiver, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        observeSensors()
        observeSystemStatus()
    }

    // [ALTERAÇÃO 3] Receber ordens do GuardScreen (Cancelar/Confirmar)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL_ALERT -> {
                // Cancelar contagem e limpar notificação
                alertJob?.cancel()
                alertJob = null
                getSystemService(NotificationManager::class.java).cancel(999)
            }
            ACTION_CONFIRM_ALERT -> {
                // Confirmar envio (o job pode continuar ou ser cancelado se o envio for manual imediato)
                // A lógica de envio manual já trata disto chamando ACTION_CANCEL_ALERT a seguir
            }
        }
        // Garante que o serviço não renasce sozinho se morrer, mas processa intents
        return START_NOT_STICKY
    }

    private fun observeSystemStatus() {
        val uid = auth.currentUser?.uid ?: return
        startHeartbeat(uid)

        serviceScope.launch {
            db.collection("users").document(uid).addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val status = snapshot.getString("systemStatus") ?: "ACTIVE"

                    if (status == "OFFLINE") {
                        isProtectionActive = false
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        getSystemService(NotificationManager::class.java)?.cancel(1)
                        stopSelf()
                        return@addSnapshotListener
                    }

                    pausedUntil = snapshot.getLong("globalPausedUntil") ?: 0L
                    val now = System.currentTimeMillis()

                    if (status == "PAUSED" && (pausedUntil == -1L || pausedUntil > now)) {
                        isProtectionActive = false
                        updateNotification(isPaused = true)
                    } else {
                        isProtectionActive = true
                        updateNotification(isPaused = false)
                    }
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

    // [ALTERAÇÃO 4] Usar triggerAlertSequence em vez de launchAlertUI direto
    private fun observeSensors() {
        serviceScope.launch {
            accelerometerHandler.onFallDetected.collect {
                if (isProtectionActive) {
                    triggerAlertSequence(ActivityAlertType.FALL)
                }
            }
        }

        serviceScope.launch {
            accelerometerHandler.onAccidentDetected.collect {
                if (isProtectionActive) {
                    triggerAlertSequence(ActivityAlertType.ACCIDENT)
                }
            }
        }

        serviceScope.launch {
            inactivityHandler.inactivityAlerts.collect { rule ->
                if (isProtectionActive) {
                    triggerAlertSequence(ActivityAlertType.FALL) // Trata inatividade como Fall/Alert
                }
            }
        }
    }

    // [ALTERAÇÃO 5] Nova lógica de contagem automática
    private fun triggerAlertSequence(type: ActivityAlertType) {
        // Se já estiver a contar, ignora para não spammar
        if (alertJob?.isActive == true) return

        // 1. Mostrar ecrã vermelho
        launchAlertUI(type)

        // 2. Iniciar contagem de 10 segundos
        alertJob = serviceScope.launch {
            delay(10000L) // Espera 10s

            if (isActive) {
                // Se chegou aqui e não foi cancelado: ENVIA
                createAlert(type, "Alerta Automático (${type.name})")

                // Limpa a notificação vermelha
                getSystemService(NotificationManager::class.java).cancel(999)
            }
            alertJob = null
        }
    }

    private fun launchAlertUI(type: ActivityAlertType) {
        val intent = Intent(this, pt.isec.amov2526.projetoKotlin.ui.MainActivity::class.java).apply {
            action = "ACTION_SHOW_ALERT"
            putExtra("ALERT_TYPE", type.name)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = getSystemService(NotificationManager::class.java)
        val channelId = "SafetySecAlertChannel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alertas de Emergência",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                setBypassDnd(true) // Fura o "Não incomodar"
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
            .setAutoCancel(false) // Não desaparece ao clicar, só por código
            .setOngoing(true)
            .build()

        notificationManager.notify(999, notification)

        // [IMPORTANTE] Tenta abrir a Activity diretamente (útil se a app já estiver aberta)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            // Em Androids recentes (10+), background activity start pode ser bloqueado,
            // mas o FullScreenIntent garante que funciona se estiver bloqueado.
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
                channelId,
                "Monitorização SafetySec",
                NotificationManager.IMPORTANCE_LOW
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

        locationHelper.stopLocationUpdates()
        accelerometerHandler.stopMonitoring()
        inactivityHandler.stopMonitoring()
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