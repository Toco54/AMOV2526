package pt.isec.amov2526.projetoKotlin.data.repository

import androidx.compose.material.icons.filled.Shield
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import pt.isec.amov2526.projetoKotlin.ui.screens.monitor.ProtectionRule
import pt.isec.amov2526.projetoKotlin.ui.screens.monitor.RuleType

class RuleRepository {
    private val db = FirebaseFirestore.getInstance()

    // Base path:

    
    private fun getRulesCollection(protegeUid: String, monitorUid: String) =
        db.collection("users").document(protegeUid)
            .collection("connections").document(monitorUid)
            .collection("rules")

    suspend fun addRule(rule: ProtectionRule, monitorId: String, protegeUid: String) {
        val docData = hashMapOf(
            "type" to rule.type.name,
            "isEnabled" to rule.isEnabled,
            "param1" to rule.param1,
            "param2" to rule.param2,
            "scheduleDays" to rule.scheduleDays,
            "scheduleStart" to rule.scheduleStart,
            "scheduleEnd" to rule.scheduleEnd,
            "pausedUntil" to rule.pausedUntil,
            "isDisabledByProtege" to rule.isDisabledByProtege
        )
        
        val collection = getRulesCollection(protegeUid, monitorId)
        
        if (rule.id.startsWith("NEW_")) {
            collection.add(docData).await()
        } else {
            collection.document(rule.id).set(docData).await()
        }
    }

    suspend fun updateRule(rule: ProtectionRule, monitorId: String, protegeUid: String) {
        val updates = mapOf(
            "isEnabled" to rule.isEnabled,
            "param1" to rule.param1,
            "param2" to rule.param2,
            "scheduleDays" to rule.scheduleDays,
            "scheduleStart" to rule.scheduleStart,
            "scheduleEnd" to rule.scheduleEnd,
            "pausedUntil" to rule.pausedUntil,
            "isDisabledByProtege" to rule.isDisabledByProtege
        )
        getRulesCollection(protegeUid, monitorId).document(rule.id).update(updates).await()
    }

    suspend fun deleteRule(ruleId: String, monitorId: String, protegeUid: String) {
        getRulesCollection(protegeUid, monitorId).document(ruleId).delete().await()
    }

    /**
     * Real-time stream of rules for a specific connection.
     */
    fun getRulesFlow(monitorId: String, protegeUid: String): Flow<List<ProtectionRule>> = callbackFlow {
        val query = getRulesCollection(protegeUid, monitorId)

        val registration: ListenerRegistration = query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                close(e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val rules = snapshot.documents.mapNotNull { doc ->
                    try {
                        val typeStr = doc.getString("type") ?: return@mapNotNull null
                        val type = RuleType.valueOf(typeStr)
                        
                        ProtectionRule(
                            id = doc.id,
                            type = type,
                            // Icon/Color set by UI
                            icon = androidx.compose.material.icons.Icons.Default.Shield, 
                            iconColor = androidx.compose.ui.graphics.Color.Gray,
                            isEnabled = doc.getBoolean("isEnabled") ?: true,
                            param1 = doc.getString("param1") ?: "",
                            param2 = doc.getString("param2") ?: "",
                            scheduleDays = (doc.get("scheduleDays") as? List<Long>)?.map { it.toInt() } ?: emptyList(),
                            scheduleStart = doc.getString("scheduleStart") ?: "",
                            scheduleEnd = doc.getString("scheduleEnd") ?: "",
                            pausedUntil = doc.getLong("pausedUntil") ?: 0L,
                            isDisabledByProtege = doc.getBoolean("isDisabledByProtege") ?: false
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                trySend(rules)
            }
        }

        awaitClose { registration.remove() }
    }
}