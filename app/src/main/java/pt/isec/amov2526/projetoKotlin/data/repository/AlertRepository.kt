package pt.isec.amov2526.projetoKotlin.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import pt.isec.amov2526.projetoKotlin.data.model.*


import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AlertRepository {
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    
    suspend fun createAlert(
        type: ActivityAlertType,
        title: String,
        latitude: Double? = null,
        longitude: Double? = null,
        visibleToMonitors: List<String>? = null, 
        initialActions: List<MonitorAction>? = null, 
        forcedId: String? = null 
    ): String? { 
        
        val currentUser = auth.currentUser ?: return null
        val uid = currentUser.uid

        val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = Date()

        val alertId = forcedId ?: UUID.randomUUID().toString()

        val alertData = hashMapOf<String, Any?>(
            "id" to alertId,
            "type" to type.name,
            "title" to title,
            "date" to sdfDate.format(now),
            "time" to sdfTime.format(now),
            "status" to ActivityAlertStatus.PENDING.name,
            "latitude" to latitude,
            "longitude" to longitude,
            "created_at" to System.currentTimeMillis(),
            "videoUrl" to null 
        )
        
        
        
        if (visibleToMonitors != null) {
            alertData["visibleTo"] = visibleToMonitors
        }

        
        if (initialActions != null) {
            val actionsList = initialActions.map { action ->
                mapOf(
                    "name" to action.name,
                    "state" to action.state.name,
                    "time" to action.time,
                    "uid" to (action.uid ?: "") 
                )
            }
            alertData["notifiedMonitors"] = actionsList
        }

        try {
            val batch = db.batch()

            
            val alertRef = db.collection("users").document(uid)
                .collection("alerts").document(alertId)
            batch.set(alertRef, alertData)

            
            visibleToMonitors?.forEach { monitorUid ->
                val connectionDoc = db.collection("users").document(monitorUid)
                    .collection("connections").document(uid)
                
                val updateData = mapOf(
                    "alertRefs.$alertId" to "users/$uid/alerts/$alertId"
                )
                batch.update(connectionDoc, updateData)
            }

            
            batch.commit().await()
            return alertId

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    suspend fun updateAlertEvidence(alertId: String, videoUrl: String?) {
        val currentUser = auth.currentUser ?: return
        try {
            db.collection("users").document(currentUser.uid)
                .collection("alerts").document(alertId)
                .update("videoUrl", videoUrl)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun listenToAlerts(onUpdate: (List<ActivityItem>) -> Unit): ListenerRegistration? {
        val currentUser = auth.currentUser ?: return null

        val query = db.collection("users").document(currentUser.uid)
            .collection("alerts")
            .orderBy("created_at", Query.Direction.DESCENDING) 

        return query.addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener

            val activities = snapshot.documents.mapNotNull { doc ->
                try {
                    val typeStr = doc.getString("type") ?: return@mapNotNull null
                    val statusStr = doc.getString("status") ?: "PENDING"

                    
                    val monitorsList = mutableListOf<MonitorAction>()
                    val monitorsRaw = doc.get("notifiedMonitors") as? List<Map<String, Any>>
                    monitorsRaw?.forEach { m ->
                        monitorsList.add(MonitorAction(
                            name = m["name"] as? String ?: "Unknown",
                            state = MonitorActionState.valueOf(m["state"] as? String ?: "SENT"),
                            time = m["time"] as? String ?: "",
                            uid = m["uid"] as? String 
                        ))
                    }

                    ActivityItem(
                        id = doc.getString("id") ?: doc.id,
                        type = ActivityAlertType.valueOf(typeStr),
                        title = doc.getString("title") ?: "Alert",
                        date = doc.getString("date") ?: "",
                        time = doc.getString("time") ?: "",
                        status = ActivityAlertStatus.valueOf(statusStr),
                        notifiedMonitors = monitorsList,
                        latitude = doc.getDouble("latitude"),
                        longitude = doc.getDouble("longitude"),
                        videoUrl = doc.getString("videoUrl")
                    )
                } catch (ex: Exception) {
                    null
                }
            }
            onUpdate(activities)
        }
    }

} 