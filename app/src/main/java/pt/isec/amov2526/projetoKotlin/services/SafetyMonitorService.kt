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
import pt.isec.amov2526.projetoKotlin.data.model.RuleInfraction
import pt.isec.amov2526.projetoKotlin.ui.MainActivity
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.GuardViewModel
import pt.isec.amov2526.projetoKotlin.utils.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * Serviço em primeiro plano (Foreground Service) responsável pela orquestração central da monitorização de segurança.
 *
 * Este serviço atua como o "cérebro" da proteção ativa, mantendo-se em execução independentemente da interface do utilizador.
 * As suas responsabilidades principais incluem:
 * 1. Gerir o ciclo de vida dos sensores (Acelerómetro, Localização, Inatividade).
 * 2. Manter a sincronização de estado (ATIVO/PAUSA/OFFLINE) com o Firebase Firestore.
 * 3. Avaliar regras de proteção em tempo real e detetar infrações.
 * 4. Coordenar a sequência de alertas (Detetar -> Interface de Cancelamento -> Envio para Backend).
 */
class SafetyMonitorService : Service() {

    companion object {
        /**
         * Flag estática para consulta rápida do estado de execução do serviço por outros componentes (Activities/ViewModels).
         * Evita a necessidade de bind para verificações simples de estado.
         */
        var isServiceRunning = false
        const val ACTION_START_ALERT = "pt.isec.amov2526.projetoKotlin.services.ACTION_START_ALERT"
        const val ACTION_STOP_TIMER = "pt.isec.amov2526.projetoKotlin.services.ACTION_STOP_TIMER"
        const val ACTION_FINISH_ALERT = "pt.isec.amov2526.projetoKotlin.services.ACTION_FINISH_ALERT"
        const val ACTION_CANCEL_ALERT = "pt.isec.amov2526.projetoKotlin.services.ACTION_CANCEL_ALERT"
        const val ACTION_UI_RESET = "pt.isec.amov2526.projetoKotlin.services.ACTION_UI_RESET"
    }

    // Escopo de corrotinas dedicado ao ciclo de vida do serviço, garantindo cancelamento estruturado no onDestroy.
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Estado interno de proteção. 'isProtectionActive' é derivado de 'systemStatus' e regras temporais.
    private var isProtectionActive = false
    private var systemStatus = "OFFLINE" 

    // Variáveis de estado para controlo de agendamento global e pausas temporárias.
    private var globalScheduleDays: List<Int> = listOf(1, 2, 3, 4, 5)
    private var globalScheduleStart: String = "09:00"
    private var globalScheduleEnd: String = "18:00"
    private var isGlobalScheduleEnabled: Boolean = false
    private var pausedUntil: Long = 0L

    // Mapas para cache de estado de grupos de regras e regras individuais, otimizando a avaliação frequente (ex: loop de sensores).
    private var groupStatuses = mutableMapOf<RuleType, GuardViewModel.ProtectionStatus>()
    private val ruleStatusCache = mutableMapOf<String, Boolean>()

    // Helpers para abstração do hardware.
    private lateinit var locationHelper: LocationHelper
    private lateinit var sensorHelper: SensorHelper
    
    // Handlers de lógica de domínio que processam dados brutos dos sensores contra regras definidas.
    private lateinit var locationHandler: LocationHandler
    private lateinit var accelerometerHandler: AccelerometerHandler
    private lateinit var inactivityHandler: InactivityHandler

    private val alertRepository = AlertRepository()
    private val ruleListeners = mutableListOf<ListenerRegistration>()

    /**
     * Estrutura de dados para armazenar alertas pendentes enquanto um alerta está a ser processado pela UI.
     * Garante que múltiplos eventos simultâneos (ex: Queda + Saída de Geofernce) não são perdidos.
     */
    private data class PendingServiceAlert(
        val type: ActivityAlertType,
        val details: String?,
        val monitors: List<String>?,
        val infractions: List<RuleInfraction>?,
        val alertId: String
    )
    private val serviceQueue = mutableListOf<PendingServiceAlert>()
    private var isAlertInProgress = false

    private var alertJob: Job? = null
    private var uiTimeoutJob: Job? = null 
    private var lastBatteryAlertTime = 0L

    // Receiver para monitorização passiva do nível de bateria, acionando alertas críticos se necessário.
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

    /**
     * Inicializa o serviço, dependências e canais de comunicação.
     *
     * Sequência de inicialização:
     * 1. Validação de autenticação (abortar se logout).
     * 2. Promoção a Foreground Service (obrigatório para sensores em background).
     * 3. Instanciação de helpers e handlers.
     * 4. Início da escuta de sensores e listeners do Firestore.
     */
    override fun onCreate() {
        super.onCreate()
        
        // Validação crítica: O serviço depende de um UID válido para aceder ao Firestore.
        if (auth.currentUser == null) {
            Log.w("SafetyMonitorService", "Service started without an authenticated user. Stopping.")
            stopSelf()
            return
        }

        isServiceRunning = true
        startForegroundService()

        locationHelper = LocationHelper(this)
        sensorHelper = SensorHelper(this)

        // Callback injetado nos handlers para verificar a validade de uma regra em tempo real antes de processar infrações.
        val checker = { rule: MonitorRule -> isRuleEffectivelyActive(rule) }
        
        locationHandler = LocationHandler(locationHelper, serviceScope, checker)
        accelerometerHandler = AccelerometerHandler(sensorHelper, serviceScope, checker)
        inactivityHandler = InactivityHandler(sensorHelper, serviceScope, checker)

        // Início da recolha de dados de sensores.
        locationHelper.startLocationUpdates(5000L)
        accelerometerHandler.startMonitoring()
        inactivityHandler.startMonitoring()
        locationHandler.startMonitoring()

        registerReceiver(batteryReceiver, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        observeSensors()
        observeSystemStatus()
        startScheduleChecker()
    }

    /**
     * Determina se uma regra deve ser aplicada no momento atual.
     *
     * Lógica hierárquica de decisão:
     * 1. Sistema Global: O sistema está 'ACTIVE'?
     * 2. Grupo de Regras: A categoria da regra (ex: FALL) está ativa e dentro do horário?
     * 3. Regra Individual: A regra específica está ativa, não pausada, e dentro do seu agendamento?
     *
     * @param rule A regra a ser avaliada.
     * @return true se a regra deve ser processada, false caso contrário.
     */
    private fun isRuleEffectivelyActive(rule: MonitorRule): Boolean {
        val now = System.currentTimeMillis()
        
        // Nível 1: Master Switch
        val globalActive = isProtectionActive

        // Nível 2: Estado do Grupo (Categoria)
        val groupStatus = groupStatuses[rule.type]
        val groupActive = if (groupStatus != null) {
            TimeUtils.calculateStatus(groupStatus.isEnabled, groupStatus.pausedUntil, groupStatus.isScheduleEnabled, groupStatus.scheduleDays, groupStatus.scheduleStart, groupStatus.scheduleEnd, now).isActive
        } else true

        // Nível 3: Estado Local da Regra
        val localActive = TimeUtils.calculateStatus(rule.isEnabled && !rule.isDisabledByProtege, rule.pausedUntil, rule.isScheduleEnabled, rule.scheduleDays, rule.scheduleStart, rule.scheduleEnd, now).isActive
        
        val effectivelyActive = globalActive && groupActive && localActive
        
        // Cache simples para logging de alterações de estado (debugging).
        val previousStatus = ruleStatusCache[rule.id]
        if (previousStatus != effectivelyActive) {
            Log.d("SafetyMonitorService", "Rule ${rule.type} (${rule.monitorName}) active: $effectivelyActive")
            ruleStatusCache[rule.id] = effectivelyActive
        }
        return effectivelyActive
    }

    /**
     * Gere a fila de alertas pendentes.
     *
     * Chamado quando um alerta termina (confirmado ou cancelado) ou expira.
     * Se houver alertas na fila, desencadeia o próximo. Caso contrário, limpa o estado de alerta.
     */
    private fun processNextInQueue() {
        // Limpeza de jobs anteriores para evitar leaks ou execuções fantasma.
        uiTimeoutJob?.cancel()
        uiTimeoutJob = null
        alertJob?.cancel()
        alertJob = null

        if (serviceQueue.isNotEmpty()) {
            val next = serviceQueue.removeAt(0)
            Log.d("SafetyMonitorService", "Processing next queued alert: ${next.type}")
            
            // Inicia o ciclo de UI e agendamento para o próximo item.
            launchAlertUI(next.type, next.details, next.infractions, next.alertId)
            scheduleAlertCreation(next.type, next.details, next.monitors, next.infractions, next.alertId)
        } else {
            isAlertInProgress = false
            getSystemService(NotificationManager::class.java).cancel(999)
        }
    }

    /**
     * Ponto de entrada para comandos via Intent (normalmente da UI).
     *
     * Gerencia ações como iniciar alertas manuais (SOS), parar temporizadores (Confirmação de segurança)
     * ou resetar o estado da UI.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_ALERT -> {
                isAlertInProgress = true
                // Cancela agendamento automático pois o alerta já foi iniciado manualmente.
                alertJob?.cancel()
                alertJob = null
            }
            ACTION_STOP_TIMER -> {
                // Utilizador confirmou segurança ou pediu ajuda imediata -> Parar contagem decrescente.
                alertJob?.cancel()
                alertJob = null
                getSystemService(NotificationManager::class.java).cancel(999)
            }
            ACTION_FINISH_ALERT, ACTION_CANCEL_ALERT -> {
                // Alerta resolvido, processar fila.
                processNextInQueue()
            }
            ACTION_UI_RESET -> {
                // Sincronização de estado UI-Serviço. Se a UI for destruída e recriada, garante consistência.
                if (isAlertInProgress) {
                    Log.d("SafetyMonitorService", "UI Reset detected. Clearing stale alert state.")
                    
                    if (serviceQueue.isNotEmpty()) {
                        processNextInQueue() 
                    } else {
                        isAlertInProgress = false
                        uiTimeoutJob?.cancel() 
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    /**
     * Configura observadores nos fluxos (Flows) dos handlers de sensores.
     * Quando uma infração é detetada, desencadeia a sequência de alerta apropriada.
     */
    private fun observeSensors() {
        serviceScope.launch {
            accelerometerHandler.infractions.collect { result ->
                if (isProtectionActive && result.infractions.isNotEmpty()) {
                    val first = result.infractions.first()
                    val type = if (first.ruleType == RuleType.FALL) ActivityAlertType.FALL else ActivityAlertType.ACCIDENT
                    triggerAlertSequence(type, result.infractions.map { it.description }.distinct().joinToString(", "), result.activeMonitorsToNotify.toList(), result.infractions)
                }
            }
        }
        serviceScope.launch {
            inactivityHandler.infractions.collect { result ->
                if (isProtectionActive && result.infractions.isNotEmpty()) {
                    triggerAlertSequence(ActivityAlertType.INACTIVITY, result.infractions.map { it.description }.distinct().joinToString(", "), result.activeMonitorsToNotify.toList(), result.infractions)
                }
            }
        }
        serviceScope.launch {
            locationHandler.infractions.collect { result ->
                if (isProtectionActive && result.infractions.isNotEmpty()) {
                    val first = result.infractions.first()
                    val type = if (first.ruleType == RuleType.SPEED) ActivityAlertType.SPEED else ActivityAlertType.GEOFENCE
                    triggerAlertSequence(type, result.infractions.map { it.description }.distinct().joinToString(", "), result.activeMonitorsToNotify.toList(), result.infractions)
                }
            }
        }
    }

    /**
     * Inicia o fluxo de um novo alerta.
     *
     * Se já existir um alerta em progresso, enfileira o novo evento.
     * Caso contrário, lança a UI de alerta (Full Screen Intent) e agenda a criação do registo no backend (após atraso de segurança).
     */
    private fun triggerAlertSequence(type: ActivityAlertType, details: String?, monitors: List<String>?, infractions: List<RuleInfraction>?) {
        val alertId = java.util.UUID.randomUUID().toString()
        if (isAlertInProgress) {
            Log.d("SafetyMonitorService", "Alert in progress. Enqueuing $type")
            serviceQueue.add(PendingServiceAlert(type, details, monitors, infractions, alertId))
            return
        }
        isAlertInProgress = true
        launchAlertUI(type, details, infractions, alertId)
        scheduleAlertCreation(type, details, monitors, infractions, alertId)
    }

    /**
     * Agenda a criação definitiva do alerta no backend após um período de carência (10s).
     *
     * Este atraso permite ao utilizador cancelar falsos positivos na UI antes que os monitores sejam notificados.
     * Se o utilizador não interagir, o alerta é enviado automaticamente.
     */
    private fun scheduleAlertCreation(type: ActivityAlertType, details: String?, monitors: List<String>?, infractions: List<RuleInfraction>?, alertId: String) {
        alertJob = serviceScope.launch {
            delay(10000L)
            if (isActive) {
                // Formatação amigável do título/detalhes para notificação.
                val title = when(type) {
                    ActivityAlertType.FALL -> "Queda Detetada"
                    ActivityAlertType.ACCIDENT -> "Acidente de Viação"
                    ActivityAlertType.BATTERY -> "Bateria Fraca"
                    ActivityAlertType.GEOFENCE -> "Saiu da Zona Segura"
                    ActivityAlertType.SPEED -> "Velocidade Excessiva"
                    ActivityAlertType.INACTIVITY -> "Inatividade Detetada"
                    else -> "Alerta Automático"
                }
                
                val finalMessage = if (details != null) {
                    if (details.startsWith(title)) details else "$title: $details"
                } else title

                // Persistência no Firestore.
                createAlert(type, finalMessage, monitors, infractions, alertId)
                getSystemService(NotificationManager::class.java).cancel(999)
            }
            alertJob = null
        }
    }

    /**
     * Lança a Interface de Utilizador para gestão do alerta.
     *
     * Utiliza uma Full Screen Intent para garantir visibilidade imediata, mesmo com o ecrã bloqueado.
     * Configura um timeout de segurança (60s) para evitar bloqueios de UI se o utilizador não responder.
     */
    private fun launchAlertUI(type: ActivityAlertType, details: String?, infractions: List<RuleInfraction>?, alertId: String) {
        // Watchdog timer para limpar estado se a UI falhar ou o utilizador ignorar.
        uiTimeoutJob?.cancel()
        uiTimeoutJob = serviceScope.launch {
            delay(60000L) 
            if (isAlertInProgress) {
                Log.d("SafetyMonitorService", "Alert UI timed out (60s). Moving to next.")
                processNextInQueue()
            }
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            action = "ACTION_SHOW_ALERT"
            putExtra("ALERT_TYPE", type.name)
            putExtra("ALERT_TIMESTAMP", System.currentTimeMillis())
            putExtra("ALERT_ID", alertId)
            if (details != null) putExtra("ALERT_DETAILS", details)
            if (infractions != null) {
                // Serialização manual simples para passar lista de infrações via Intent extras.
                val serialized = infractions.joinToString(";") { "${it.monitorName}|${it.ruleType.name}|${it.description}|${it.ruleParameter ?: ""}|${it.monitorId}" }
                putExtra("ALERT_INFRACTIONS", serialized)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        // Criação da notificação de alta prioridade para acompanhar a Activity.
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val channelId = "SafetySecAlertChannel"
        val notificationManager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Alertas de Emergência", NotificationManager.IMPORTANCE_HIGH).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_shield_check)
            .setContentTitle(getString(R.string.app_name).uppercase())
            .setContentText(getString(R.string.evidence_help_coming))
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

    /**
     * Persiste o alerta no Firebase e notifica os monitores associados.
     *
     * Lógica de resolução de destinatários:
     * 1. Se existirem infrações específicas, notifica os monitores responsáveis por essas regras.
     * 2. Se não, notifica uma lista explícita de monitores (se fornecida).
     * 3. Fallback: Notifica todos os monitores conectados ao utilizador.
     */
    private suspend fun createAlert(type: ActivityAlertType, title: String, monitors: List<String>?, infractions: List<RuleInfraction>?, alertId: String) {
        if (!isProtectionActive) return
        val loc = locationHelper.currentLocation.value
        var initialActions: List<MonitorAction>? = null
        var targetMonitors: List<String>? = monitors
        
        // Prioridade 1: Monitores ligados às infrações detetadas.
        if (infractions != null && infractions.isNotEmpty()) {
            initialActions = infractions.map { inf ->
                MonitorAction(inf.monitorName, MonitorActionState.SENT, java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date()), inf.monitorId)
            }
            targetMonitors = infractions.map { it.monitorId }.distinct()
        } 
        // Prioridade 2: Lista explícita fornecida.
        else if (monitors != null && monitors.isNotEmpty()) {
             try {
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    val snapshot = db.collection("users").document(uid)
                        .collection("connections")
                        .whereIn("uid", monitors)
                        .get().await()
                    
                    initialActions = snapshot.documents.map { doc ->
                        MonitorAction(doc.getString("name") ?: "Monitor", MonitorActionState.SENT, java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date()), doc.getString("uid"))
                    }
                }
            } catch (e: Exception) {
                initialActions = monitors.map { MonitorAction("Monitor", MonitorActionState.SENT, "", it) }
            }
        }
        
        // Fallback: Todos os monitores.
        if (initialActions == null) {
             try {
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    val snapshot = db.collection("users").document(uid).collection("connections").whereEqualTo("role", "Monitor").get().await()
                    targetMonitors = snapshot.documents.mapNotNull { it.getString("uid") }
                    initialActions = snapshot.documents.map { doc ->
                        MonitorAction(doc.getString("name") ?: "Monitor", MonitorActionState.SENT, java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date()), doc.getString("uid"))
                    }
                }
            } catch (e: Exception) {}
        }

        alertRepository.createAlert(type, title, loc?.latitude, loc?.longitude, targetMonitors, initialActions, alertId)
    }

    private fun startForegroundService() {
        val channelId = "SafetySecChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Monitorização SafetySec", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.notif_active_title))
            .setContentText(getString(R.string.notif_active_desc))
            .setSmallIcon(R.drawable.ic_shield_check).setPriority(NotificationCompat.PRIORITY_LOW).setOngoing(true).build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        else startForeground(1, notification)
    }

    private fun updateNotification(isPaused: Boolean) {
        val channelId = "SafetySecChannel"
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(if (isPaused) getString(R.string.notif_paused_title) else getString(R.string.notif_active_title))
            .setContentText(if (isPaused) getString(R.string.notif_paused_desc) else getString(R.string.notif_active_desc))
            .setSmallIcon(R.drawable.ic_shield_check).setPriority(NotificationCompat.PRIORITY_LOW).setOngoing(true).setOnlyAlertOnce(true).build()
        getSystemService(NotificationManager::class.java).notify(1, notification)
    }

    /**
     * Escuta alterações no documento do utilizador no Firestore.
     *
     * Funciona como um mecanismo de controlo remoto (Remote Config), permitindo:
     * - Pausa/Retoma remota do sistema.
     * - Sincronização de configurações de proteção.
     * - Encerramento remoto (Kill Switch).
     */
    private fun observeSystemStatus() {
        val uid = auth.currentUser?.uid ?: return
        startHeartbeat(uid)
        startListeningToRules(uid)
        serviceScope.launch {
            db.collection("users").document(uid).addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    systemStatus = snapshot.getString("systemStatus") ?: "OFFLINE"
                    
                    // Kill Switch remoto
                    if (systemStatus == "OFFLINE") {
                        isProtectionActive = false
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        return@addSnapshotListener
                    }
                    
                    pausedUntil = snapshot.getLong("globalPausedUntil") ?: 0L
                    isGlobalScheduleEnabled = snapshot.getBoolean("isGlobalScheduleEnabled") ?: false
                    
                    // Parsing manual das configurações de proteção complexas (Mapas aninhados).
                    val rawGroups = snapshot.get("activeProtections") as? Map<String, Any>
                    if (rawGroups != null) {
                        val newMap = mutableMapOf<RuleType, GuardViewModel.ProtectionStatus>()
                        rawGroups.forEach { (key, value) ->
                            try {
                                val type = RuleType.valueOf(key)
                                if (value is Map<*, *>) {
                                    newMap[type] = GuardViewModel.ProtectionStatus(
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
    }

    /**
     * Carrega e escuta regras definidas por monitores associados.
     *
     * Navega pela sub-coleção 'connections' e depois 'rules' para cada monitor.
     * Atualiza os handlers (Location, Accelerometer, Inactivity) com as novas configurações em tempo real.
     */
    private fun startListeningToRules(uid: String) {
        db.collection("users").document(uid).collection("connections").whereEqualTo("role", "Monitor").addSnapshotListener { connections, _ ->
            if (connections == null) return@addSnapshotListener
            
            // Limpeza preventiva para evitar regras duplicadas ou obsoletas.
            locationHandler.clearAllRules(); accelerometerHandler.clearAllRules(); inactivityHandler.clearAllRules()
            
            connections.documents.forEach { connDoc ->
                val monitorUid = connDoc.getString("uid") ?: return@forEach
                serviceScope.launch {
                    var monitorName = connDoc.getString("name")
                    if (monitorName == null || monitorName == "Monitor") {
                        try {
                            val monitorDoc = db.collection("users").document(monitorUid).get().await()
                            monitorName = monitorDoc.getString("name") ?: "Monitor"
                        } catch (e: Exception) { monitorName = "Monitor" }
                    }
                    db.collection("users").document(uid).collection("connections").document(monitorUid).collection("rules").addSnapshotListener { rules, _ ->
                        if (rules == null) return@addSnapshotListener
                        rules.documents.forEach { doc ->
                            if (doc.getBoolean("deletedByProtege") == true) return@forEach
                            val typeStr = doc.getString("type") ?: return@forEach
                            val type = try { RuleType.valueOf(typeStr) } catch(e:Exception) { null } ?: return@forEach
                            
                            val param1 = doc.getString("param1") ?: ""
                            val param2 = doc.getString("param2") ?: ""
                            
                            // Parsing específico para Geofencing (Raio + Coordenadas).
                            var geofenceData: Triple<Double, Double, Float>? = null
                            if (type == RuleType.GEOFENCING) {
                                val radius = param1.toFloatOrNull()
                                val coordParts = param2.split(Regex("[,;]"))
                                if (radius != null && coordParts.size >= 2) {
                                    val lat = coordParts[0].trim().toDoubleOrNull()
                                    val lng = coordParts[1].trim().toDoubleOrNull()
                                    if (lat != null && lng != null) {
                                        geofenceData = Triple(lat, lng, radius)
                                    }
                                }
                            }

                            val rule = MonitorRule(
                                id = doc.id, monitorId = monitorUid, monitorName = monitorName!!,
                                parameter = param1, isEnabled = doc.getBoolean("isEnabled") ?: true,
                                type = type, scheduleDays = (doc.get("scheduleDays") as? List<Number>)?.map { it.toInt() } ?: emptyList(),
                                scheduleStart = doc.getString("scheduleStart") ?: "", scheduleEnd = doc.getString("scheduleEnd") ?: "",
                                isScheduleEnabled = doc.getBoolean("isScheduleEnabled") ?: false, pausedUntil = doc.getLong("pausedUntil") ?: 0L,
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
            }
        }
    }

    /**
     * Mantém um 'heartbeat' (batimento cardíaco) atualizando o timestamp 'lastUpdate' no Firestore.
     * Permite aos monitores saberem se o dispositivo protegido está online e a reportar.
     */
    private fun startHeartbeat(uid: String) {
        serviceScope.launch { while (isActive) { try { db.collection("users").document(uid).update("lastUpdate", System.currentTimeMillis()) } catch (_: Exception) {}; delay(60000L) } }
    }

    private fun startScheduleChecker() {
        serviceScope.launch { while (isActive) { evaluateProtectionStatus(); delay(60000L) } }
    }

    /**
     * Recalcula o estado global de proteção com base em pausas temporizadas e agendamentos.
     *
     * Responsável por transitar automaticamente de PAUSED para ACTIVE quando o tempo de pausa expira.
     */
    private fun evaluateProtectionStatus() {
        val now = System.currentTimeMillis()
        val previouslyActive = isProtectionActive

        // Transição automática PAUSED -> ACTIVE se o tempo expirar.
        if (systemStatus == "PAUSED" && pausedUntil > 0 && pausedUntil < now) {
            systemStatus = "ACTIVE"; pausedUntil = 0L
            auth.currentUser?.uid?.let { db.collection("users").document(it).update(mapOf("systemStatus" to "ACTIVE", "globalPausedUntil" to 0L)) }
        }
        
        isProtectionActive = systemStatus == "ACTIVE"
        
        // Reset de timers de inatividade ao reativar a proteção para evitar falsos positivos imediatos.
        if (!previouslyActive && isProtectionActive) {
            inactivityHandler.resetTimer()
        }
        
        updateNotification(!isProtectionActive)
    }

    private fun updateBatteryLevel(pct: Int) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update("batteryLevel", pct)
        if (pct <= 15 && isProtectionActive) {
            val now = System.currentTimeMillis()
            // Throttle para alertas de bateria: Apenas um a cada 15 minutos.
            if (now - lastBatteryAlertTime > 900000L) {
                lastBatteryAlertTime = now
                serviceScope.launch { createAlert(ActivityAlertType.BATTERY, "Bateria Fraca ($pct%)", null, null, java.util.UUID.randomUUID().toString()) }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        locationHelper.stopLocationUpdates()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}