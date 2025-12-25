package pt.isec.amov2526.projetoKotlin.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Variável para saber quem está logado
    val currentUser get() = auth.currentUser

    // Login simples
    suspend fun login(email: String, pass: String): Result<String> {
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()
            Result.success(auth.currentUser?.uid ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Registo com criação de perfil na Firestore (Como pede o enunciado: Monitor vs Protegido)
    suspend fun register(email: String, pass: String, name: String, isMonitor: Boolean, pin: String?): Result<String> {
        return try {
            // 1. Criar utilizador na Autenticação
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            val uid = result.user?.uid ?: throw Exception("Erro ao obter UID")

            // 2. Preparar os dados do perfil
            val role = if (isMonitor) "MONITOR" else "PROTECTED"

            val userProfile = hashMapOf(
                "uid" to uid,
                "name" to name,
                "email" to email,
                "role" to role,
                "pin" to (pin ?: ""),
                // Campos futuros para a Associação (OTP) pedida no enunciado
                "associatedIds" to emptyList<String>(),
                "fcmToken" to "" // Para notificações (Firebase Messaging)
            )

            // 3. Guardar na Firestore
            db.collection("users").document(uid).set(userProfile).await()

            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }

    // Função auxiliar para saber o tipo de utilizador depois do login
    suspend fun getUserRole(uid: String): String? {
        return try {
            val doc = db.collection("users").document(uid).get().await()
            doc.getString("role")
        } catch (e: Exception) {
            null
        }
    }
}