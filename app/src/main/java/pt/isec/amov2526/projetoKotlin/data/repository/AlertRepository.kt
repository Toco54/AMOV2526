package pt.isec.amov2526.projetoKotlin.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import pt.isec.amov2526.projetoKotlin.ui.screens.monitor.ActivityAlertStatus
import pt.isec.amov2526.projetoKotlin.ui.screens.monitor.ActivityAlertType
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
        longitude: Double? = null
    ) {
        // Se 'auth' der erro aqui, é porque a função está fora da classe!
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid

        val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = Date()

        val alertId = UUID.randomUUID().toString()

        val alertData = hashMapOf(
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
} // <--- A classe SÓ fecha aqui!