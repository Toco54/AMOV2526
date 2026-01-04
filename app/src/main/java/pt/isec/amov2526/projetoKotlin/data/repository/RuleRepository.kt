package pt.isec.amov2526.projetoKotlin.data.repository

import androidx.compose.material.icons.filled.Shield
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import pt.isec.amov2526.projetoKotlin.data.model.MonitorRuleType
import pt.isec.amov2526.projetoKotlin.data.model.ProtectionRule

/**
 * Repositório para gestão de regras de monitorização.
 *
 * As regras são definidas pelos monitores para os protegidos.
 * Este repositório abstrai a complexidade da estrutura de sub-coleções do Firestore:
 * `users/{protegeUid}/connections/{monitorUid}/rules/{ruleId}`
 */
class RuleRepository {
    private val db = FirebaseFirestore.getInstance()

    // Helper para obter a referência correta da coleção de regras.
    private fun getRulesCollection(protegeUid: String, monitorUid: String) =
        db.collection("users").document(protegeUid)
            .collection("connections").document(monitorUid)
            .collection("rules")

    /**
     * Adiciona uma nova regra de proteção.
     * Se o ID começar por "NEW_", gera um novo documento no Firestore. Caso contrário, substitui (upsert).
     */
    suspend fun addRule(rule: ProtectionRule, monitorId: String, protegeUid: String) {
        val docData = hashMapOf(
            "type" to rule.type.name,
            "isEnabled" to rule.isEnabled,
            "param1" to rule.param1,
            "param2" to rule.param2,
            "scheduleDays" to rule.scheduleDays,
            "scheduleStart" to rule.scheduleStart,
            "scheduleEnd" to rule.scheduleEnd,
            "isScheduleEnabled" to rule.isScheduleEnabled,
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
            "isScheduleEnabled" to rule.isScheduleEnabled,
            "pausedUntil" to rule.pausedUntil,
            "isDisabledByProtege" to rule.isDisabledByProtege
        )
        getRulesCollection(protegeUid, monitorId).document(rule.id).update(updates).await()
    }

    suspend fun deleteRule(ruleId: String, monitorId: String, protegeUid: String) {
        getRulesCollection(protegeUid, monitorId).document(ruleId).delete().await()
    }

    /**
     * Retorna um fluxo (Flow) em tempo real das regras definidas por um monitor específico.
     *
     * Utiliza `callbackFlow` para converter o listener baseado em callbacks do Firestore num Flow de Kotlin.
     * Garante que o listener é removido quando o fluxo é cancelado (awaitClose).
     */
    fun getRulesFlow(monitorId: String, protegeUid: String): Flow<List<ProtectionRule>> =
        callbackFlow {
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
                            val type = MonitorRuleType.valueOf(typeStr)

                            ProtectionRule(
                                id = doc.id,
                                type = type,
                                // UI Defaults (Icon/Color) são preenchidos no ViewModel/UI layer, aqui usamos placeholders.
                                icon = androidx.compose.material.icons.Icons.Default.Shield,
                                iconColor = androidx.compose.ui.graphics.Color.Gray,
                                isEnabled = doc.getBoolean("isEnabled") ?: true,
                                param1 = doc.getString("param1") ?: "",
                                param2 = doc.getString("param2") ?: "",
                                scheduleDays = (doc.get("scheduleDays") as? List<Long>)?.map { it.toInt() }
                                    ?: emptyList(),
                                scheduleStart = doc.getString("scheduleStart") ?: "",
                                scheduleEnd = doc.getString("scheduleEnd") ?: "",
                                isScheduleEnabled = doc.getBoolean("isScheduleEnabled") ?: false,
                                pausedUntil = doc.getLong("pausedUntil") ?: 0L,
                                isDisabledByProtege = doc.getBoolean("isDisabledByProtege")
                                    ?: false,
                                deletedByProtege = doc.getBoolean("deletedByProtege") ?: false
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
