package pt.isec.amov2526.projetoKotlin.data.repository


import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import pt.isec.amov2526.projetoKotlin.data.model.ActivityAlertStatus
import pt.isec.amov2526.projetoKotlin.data.model.ActivityAlertType
import pt.isec.amov2526.projetoKotlin.data.model.ActivityItem
import pt.isec.amov2526.projetoKotlin.data.model.MonitorAction
import pt.isec.amov2526.projetoKotlin.data.model.MonitorActionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Repositório responsável pela persistência e sincronização de Alertas de Segurança.
 *
 * Gere o ciclo de vida dos documentos de alerta no Firebase Firestore, incluindo:
 * 1. Criação atómica de alertas (garantindo que o alerta e as referências nos monitores são criados em conjunto).
 * 2. Atualização de evidências (URLs de vídeo).
 * 3. Escuta em tempo real de novos alertas para a UI.
 */
class AlertRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Cria um novo alerta de segurança e propaga referências para os monitores relevantes.
     *
     * Utiliza uma `WriteBatch` para garantir consistência: O alerta é criado na coleção do utilizador
     * E as referências são injetadas nas coleções dos monitores simultaneamente.
     *
     * @param type Tipo de alerta (FALL, SOS, etc.).
     * @param title Título descritivo.
     * @param visibleToMonitors Lista de UIDs dos monitores que devem receber este alerta.
     * @param initialActions Estado inicial da notificação para cada monitor (ex: "Enviado").
     * @param forcedId ID opcional pré-gerado (útil para consistência entre UI local e Backend).
     * @return O ID do alerta criado, ou null em caso de falha.
     */
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

        // Adiciona metadados de visibilidade para regras de segurança do Firestore (Security Rules).
        if (visibleToMonitors != null) {
            alertData["visibleTo"] = visibleToMonitors
        }

        // Serializa as ações iniciais para mapa (Firestore não suporta data classes diretamente sem conversão).
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

            // 1. Criar o documento do alerta na coleção do utilizador protegido.
            val alertRef = db.collection("users").document(uid)
                .collection("alerts").document(alertId)
            batch.set(alertRef, alertData)

            // 2. Atualizar os documentos de conexão de cada monitor para incluir a referência a este novo alerta.
            // Isto serve como um "Inbox" ou "Feed" para o monitor.
            visibleToMonitors?.forEach { monitorUid ->
                val connectionDoc = db.collection("users").document(monitorUid)
                    .collection("connections").document(uid)

                val updateData = mapOf(
                    "alertRefs.$alertId" to "users/$uid/alerts/$alertId"
                )
                batch.update(connectionDoc, updateData)
            }

            // Executa tudo atomicamente.
            batch.commit().await()
            return alertId

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Atualiza o URL do vídeo de evidência após o upload bem-sucedido.
     */
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

    /**
     * Subscreve ao feed de alertas do próprio utilizador para atualizar a UI de histórico.
     *
     * @param onUpdate Callback invocado com a lista atualizada de atividades.
     * @return O ListenerRegistration para cancelar a subscrição quando necessário.
     */
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

                    // Deserialização manual da lista de monitores.
                    val monitorsList = mutableListOf<MonitorAction>()
                    val monitorsRaw = doc.get("notifiedMonitors") as? List<Map<String, Any>>
                    monitorsRaw?.forEach { m ->
                        monitorsList.add(
                            MonitorAction(
                                name = m["name"] as? String ?: "Unknown",
                                state = MonitorActionState.valueOf(m["state"] as? String ?: "SENT"),
                                time = m["time"] as? String ?: "",
                                uid = m["uid"] as? String
                            )
                        )
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