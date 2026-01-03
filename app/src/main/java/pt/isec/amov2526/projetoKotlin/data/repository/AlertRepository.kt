package pt.isec.amov2526.projetoKotlin.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import pt.isec.amov2526.projetoKotlin.data.model.*
//import pt.isec.amov2526.projetoKotlin.data.model.ActivityAlertStatus
//import pt.isec.amov2526.projetoKotlin.data.model.ActivityAlertType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AlertRepository {
    // Estas variáveis pertencem à classe
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // A função TEM de estar dentro da classe para ver o 'db' e o 'auth'
    suspend fun createAlert(
        type: ActivityAlertType,
        title: String,
        latitude: Double? = null,
        longitude: Double? = null,
        visibleToMonitors: List<String>? = null // [NOVO] Lista de IDs de monitores autorizados
    ) {
        // Se 'auth' der erro aqui, é porque a função está fora da classe!
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid

        val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = Date()

        val alertId = UUID.randomUUID().toString()

        val alertData = hashMapOf<String, Any?>(
            "id" to alertId,
            "type" to type.name,
            "title" to title,
            "date" to sdfDate.format(now),
            "time" to sdfTime.format(now),
            "status" to ActivityAlertStatus.PENDING.name,
            "latitude" to latitude,
            "longitude" to longitude,
            "created_at" to System.currentTimeMillis()
        )
        
        // Se a lista for null ou vazia, assume-se que é visível a TODOS (comportamento fallback)
        // ou guardamos explicitamente se foi passado
        if (visibleToMonitors != null) {
            alertData["visibleTo"] = visibleToMonitors
        }

        try {
            // Se o .await() der erro aqui, falta adicionar a dependência no build.gradle
            db.collection("users").document(uid)
                .collection("alerts").document(alertId)
                .set(alertData)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun listenToAlerts(onUpdate: (List<ActivityItem>) -> Unit): ListenerRegistration? {
        val currentUser = auth.currentUser ?: return null

        val query = db.collection("users").document(currentUser.uid)
            .collection("alerts")
            .orderBy("created_at", Query.Direction.DESCENDING) // Mais recentes primeiro

        return query.addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener

            val activities = snapshot.documents.mapNotNull { doc ->
                try {
                    val typeStr = doc.getString("type") ?: return@mapNotNull null
                    val statusStr = doc.getString("status") ?: "PENDING"

                    // Converter Monitors Actions (se existirem)
                    val monitorsList = mutableListOf<MonitorAction>()
                    val monitorsRaw = doc.get("notifiedMonitors") as? List<Map<String, Any>>
                    monitorsRaw?.forEach { m ->
                        monitorsList.add(MonitorAction(
                            name = m["name"] as? String ?: "Unknown",
                            state = MonitorActionState.valueOf(m["state"] as? String ?: "SENT"),
                            time = m["time"] as? String ?: ""
                        ))
                    }

                    ActivityItem(
                        id = doc.getString("id") ?: doc.id,
                        type = ActivityAlertType.valueOf(typeStr),
                        title = doc.getString("title") ?: "Alert",
                        date = doc.getString("date") ?: "",
                        time = doc.getString("time") ?: "",
                        status = ActivityAlertStatus.valueOf(statusStr),
                        notifiedMonitors = monitorsList
                    )
                } catch (ex: Exception) {
                    null
                }
            }
            onUpdate(activities)
        }
    }

} // <--- A classe SÓ fecha aqui!