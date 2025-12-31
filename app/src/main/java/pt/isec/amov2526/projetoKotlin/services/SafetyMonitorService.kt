package pt.isec.amov2526.projetoKotlin.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import pt.isec.amov2526.projetoKotlin.R
import pt.isec.amov2526.projetoKotlin.data.repository.AlertRepository
import pt.isec.amov2526.projetoKotlin.ui.screens.monitor.ActivityAlertType
import pt.isec.amov2526.projetoKotlin.utils.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SafetyMonitorService : Service() {

    companion object {
        var isServiceRunning = false
    }
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var isProtectionActive = true

    // As tuas ferramentas (Utils)
    private lateinit var locationHelper: LocationHelper
    private lateinit var sensorHelper: SensorHelper
    private lateinit var accelerometerHandler: AccelerometerHandler
    private lateinit var inactivityHandler: InactivityHandler

    private var pausedUntil: Long = 0L
    private val alertRepository = AlertRepository()

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        startForegroundService() // Torna o serviço imortal (mostra notificação)

        // 1. Inicializar os Helpers
        locationHelper = LocationHelper(this)
        sensorHelper = SensorHelper(this)

        // Passamos o scope para eles poderem correr corrotinas
        accelerometerHandler = AccelerometerHandler(sensorHelper, serviceScope)
        inactivityHandler = InactivityHandler(sensorHelper, serviceScope)

        // 2. Ligar os sensores
        locationHelper.startLocationUpdates(5000L) // GPS a cada 5s
        accelerometerHandler.startMonitoring()
        inactivityHandler.startMonitoring()

        // 3. Começar a escutar os eventos
        observeSensors()
        observeSystemStatus()
    }

    private fun observeSystemStatus() {
        val uid = auth.currentUser?.uid ?: return

        // 1. Verificar se o utilizador Pausou a aplicação
        serviceScope.launch {
            db.collection("users").document(uid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        val status = snapshot.getString("systemStatus") ?: "ACTIVE"

                        //val pausedUntil = snapshot.getLong("globalPausedUntil") ?: 0L
                        pausedUntil = snapshot.getLong("globalPausedUntil") ?: 0L

                        val now = System.currentTimeMillis()

                        // Se estiver PAUSED e o tempo ainda não acabou -> Desativar proteção
                        if (status == "PAUSED" && (pausedUntil == -1L || pausedUntil > now)) {
                            isProtectionActive = false
                        } else {
                            isProtectionActive = true
                        }
                    }
                }
        }

        // 2. Enviar a localização em tempo real para o Firestore
        serviceScope.launch {
            locationHelper.currentLocation.collect { loc ->

                if (loc != null && isProtectionActive ) {
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

    private fun observeSensors() {
        // Escutar Quedas
        serviceScope.launch {
            accelerometerHandler.onFallDetected.collect {
                if (isProtectionActive) { // <--- NOVO CHECK
                    createAlert(ActivityAlertType.FALL, "Queda Detetada!")
                }
            }
        }

        // Escutar Acidentes (G-Force elevado)
        serviceScope.launch {
            accelerometerHandler.onAccidentDetected.collect {
                if (isProtectionActive) { // <--- NOVO CHECK
                    createAlert(ActivityAlertType.ACCIDENT, "Acidente de Viação Detetado!")
                }
            }
        }

        // Escutar Inatividade (falta de movimento)
        serviceScope.launch {
            inactivityHandler.inactivityAlerts.collect { rule ->
                if (isProtectionActive) { // <--- NOVO CHECK
                    createAlert(ActivityAlertType.FALL, "Inatividade: ${rule.name}")
                }
            }
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

    // Configuração da Notificação Obrigatória do Android
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
            .setSmallIcon(R.drawable.ic_shield_check) // Usa o teu ícone de escudo
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        // Inicia o serviço em modo Foreground (com tipo location para Android 14)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        // Limpar tudo ao fechar
        locationHelper.stopLocationUpdates()
        accelerometerHandler.stopMonitoring()
        inactivityHandler.stopMonitoring()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}