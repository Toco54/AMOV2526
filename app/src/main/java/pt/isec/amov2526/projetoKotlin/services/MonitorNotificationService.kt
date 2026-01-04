package pt.isec.amov2526.projetoKotlin.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import pt.isec.amov2526.projetoKotlin.R
import pt.isec.amov2526.projetoKotlin.ui.MainActivity

class MonitorNotificationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var connectionsListener: ListenerRegistration? = null
    

    private val knownAlerts = mutableMapOf<String, MutableSet<String>>()
    private var isFirstLoad = true

    companion object {
        const val CHANNEL_ID = "MonitorAlertsChannel"
        const val NOTIFICATION_ID = 2001
    }

    override fun onCreate() {
        super.onCreate()
        if (auth.currentUser == null) {
            Log.w("MonitorService", "Service started without an authenticated user. Stopping.")
            stopSelf()
            return
        }
        startForegroundService()
        startListening()
    }

    private fun startForegroundService() {
        val channelId = "MonitorServiceChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Monitorização Remota",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.monitor_notif_title))
            .setContentText(getString(R.string.monitor_notif_desc))
            .setSmallIcon(R.drawable.ic_shield_check)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(2, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(2, notification)
        }
    }

    private fun startListening() {
        val uid = auth.currentUser?.uid ?: return

        Log.d("MonitorService", "Iniciando escuta de conexões para $uid")
        

        connectionsListener = db.collection("users").document(uid)
            .collection("connections")
            .whereEqualTo("role", "Protege") 
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("MonitorService", "Erro ao ouvir conexões: ${e.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    snapshot.documents.forEach { doc ->
                        val protegeId = doc.getString("uid") ?: return@forEach
                        val protegeName = doc.getString("name") ?: "Protegido"
                        
                        
                        val rawAlerts = doc.get("alertRefs") as? Map<String, String> ?: emptyMap()
                        val currentAlertIds = rawAlerts.keys

                        
                        if (!knownAlerts.containsKey(protegeId)) {
                            knownAlerts[protegeId] = currentAlertIds.toMutableSet()
                        } else {
                            
                            val previousIds = knownAlerts[protegeId]!!
                            val newIds = currentAlertIds - previousIds
                            
                            newIds.forEach { newAlertId ->
                                val alertPath = rawAlerts[newAlertId]
                                if (!alertPath.isNullOrEmpty()) {
                                    Log.d("MonitorService", "Novo alerta detetado de $protegeName: $newAlertId. Obtendo detalhes...")
                                    serviceScope.launch {
                                        var finalName = protegeName
                                        
                                        
                                        if (finalName == "Protegido" || finalName == "Protege" || finalName == "Monitor" || finalName == "Unknown") {
                                            try {
                                                val userDoc = db.collection("users").document(protegeId).get().await()
                                                val realName = userDoc.getString("name")
                                                if (!realName.isNullOrEmpty()) {
                                                    finalName = realName
                                                }
                                            } catch (e: Exception) {
                                                Log.w("MonitorService", "Falha ao obter nome real do utilizador: ${e.message}")
                                            }
                                        }

                                        try {
                                            val alertDoc = db.document(alertPath).get().await()
                                            val type = alertDoc.getString("type") ?: "ALERTA"
                                            val title = alertDoc.getString("title") ?: "Novo Incidente"
                                            
                                            
                                            val friendlyType = when(type) {
                                                "FALL" -> "Queda Detetada"
                                                "ACCIDENT" -> "Acidente"
                                                "GEOFENCE" -> "Zona Segura"
                                                "SPEED" -> "Excesso de Velocidade"
                                                "BATTERY" -> "Bateria Crítica"
                                                "PANIC" -> "Botão SOS"
                                                "INACTIVITY" -> "Inatividade"
                                                else -> title
                                            }

                                            notifyNewAlert(protegeId, finalName, newAlertId, friendlyType)
                                        } catch (e: Exception) {
                                            Log.e("MonitorService", "Erro ao obter detalhes do alerta: ${e.message}")
                                            
                                            notifyNewAlert(protegeId, finalName, newAlertId, "Novo Alerta")
                                        }
                                    }
                                }
                                previousIds.add(newAlertId)
                            }
                        }
                    }
                    isFirstLoad = false
                }
            }
    }

    private fun notifyNewAlert(protegeId: String, protegeName: String, alertId: String, alertTypeDesc: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alertas de Protegidos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações quando um protegido emite um alerta"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        
        val intent = Intent(this, MainActivity::class.java).apply {
            action = "ACTION_MONITOR_ALERT"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("TARGET_PROTEGE_ID", protegeId)
            putExtra("TARGET_PROTEGE_NAME", protegeName)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 
            alertId.hashCode(), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield_check)
            .setContentTitle("Alerta de $protegeName")
            .setContentText(getString(R.string.monitor_notif_alert_desc, alertTypeDesc))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(alertId.hashCode(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        connectionsListener?.remove()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
