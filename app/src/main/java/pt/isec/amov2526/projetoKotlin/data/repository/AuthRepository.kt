package pt.isec.amov2526.projetoKotlin.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import pt.isec.amov2526.projetoKotlin.SafetySecApp
import pt.isec.amov2526.projetoKotlin.data.local.UserEntity

// [NOVO] Imports para o MFA e Envio de Email
import java.util.Properties
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

class AuthRepository(private val app: SafetySecApp) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val userDao = app.database.userDao()

    // [NOVO] --- CONFIGURAÇÃO DO EMAIL DE SISTEMA (MFA) ---
    // Substitui pelo teu email e pela App Password de 16 caracteres
    private val SENDER_EMAIL = "safetysec.projeto@gmail.com"
    private val SENDER_PASSWORD = "stej jhqd qznj qpkz"

    // Variável para saber quem está logado
    val currentUser get() = auth.currentUser

    // Login simples (Mantido igual)
    suspend fun login(email: String, pass: String): Result<String> {
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()

            val uid = auth.currentUser?.uid ?: throw Exception("UID nulo")

            // 1. Buscar dados à Firestore
            val doc = db.collection("users").document(uid).get().await()

            // 2. Criar objeto para guardar localmente
            val localUser = UserEntity(
                uid = uid,
                name = doc.getString("name") ?: "Sem Nome",
                email = email,
                role = doc.getString("role") ?: "PROTECTED",
                pin = doc.getString("pin"),
                mfaEnabled = doc.getBoolean("mfaEnabled") ?: false
            )

            userDao.insertUser(localUser)

            Result.success(auth.currentUser?.uid ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Registo com criação de perfil na Firestore (Mantido igual)
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
                "mfaEnabled" to false,
                // Campos futuros para a Associação (OTP) pedida no enunciado
                "associatedIds" to emptyList<String>(),
                "fcmToken" to "" // Para notificações (Firebase Messaging)
            )

            // 3. Guardar na Firestore
            db.collection("users").document(uid).set(userProfile).await()
            val localUser = UserEntity(uid, name, email, if (isMonitor) "MONITOR" else "PROTECTED", pin, false)
            userDao.insertUser(localUser)


            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        auth.signOut()
        userDao.clearUser()
    }

    // Função auxiliar para saber o tipo de utilizador depois do login (Mantido igual)
    suspend fun getUserRole(uid: String): String? {
        val localUser = userDao.getUser()
        if (localUser != null && localUser.uid == uid) {
            return localUser.role
        }

        return try {
            val doc = db.collection("users").document(uid).get().await()
            doc.getString("role")
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserName(uid: String): String? {

        val localUser = userDao.getUser()
        if (localUser != null && localUser.uid == uid) {
            return localUser.name
        }

        return try {
            val doc = db.collection("users").document(uid).get().await()
            doc.getString("name") // Retorna o campo "name" da base de dados
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUser(uid: String): UserEntity? {
        val localUser = userDao.getUser()
        if (localUser != null && localUser.uid == uid) {
            return localUser
        }
        return try {
            val doc = db.collection("users").document(uid).get().await()
            if (doc.exists()) {
                val fetchedUser = UserEntity(
                    uid = uid,
                    name = doc.getString("name") ?: "",
                    email = doc.getString("email") ?: "",
                    role = doc.getString("role") ?: "PROTECTED",
                    pin = doc.getString("pin"),
                    mfaEnabled = doc.getBoolean("mfaEnabled") ?: false
                )
                userDao.insertUser(fetchedUser)
                fetchedUser
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        uid: String,
        name: String,
        email: String,
        newPass: String?,
        newPin: String?,
        mfaEnabled: Boolean,
        currentPass: String?
    ): Result<Unit> {
        return try {
            val user = auth.currentUser
            if (user == null || user.uid != uid) throw Exception("User not logged in or UID mismatch")

            // Re-authentication is required for sensitive operations (Email/Password change)
            if (currentPass != null) {
                val credential = EmailAuthProvider.getCredential(user.email!!, currentPass)
                user.reauthenticate(credential).await()
            } else if (!newPass.isNullOrEmpty() || email != user.email) {
                // If changing pass/email but no current pass provided -> Error (or let Firebase fail)
                // However, the UI logic should ensure currentPass is passed if fields are edited.
            }

            // 1. Update Firebase Auth (Email & Password)
            if (email != user.email) {
                user.updateEmail(email).await()
            }
            if (!newPass.isNullOrEmpty()) {
                user.updatePassword(newPass).await()
            }

            // 2. Update Firestore
            val updates = hashMapOf<String, Any>(
                "name" to name,
                "email" to email,
                "mfaEnabled" to mfaEnabled
            )
            if (newPin != null) {
                updates["pin"] = newPin
            }
            db.collection("users").document(uid).update(updates).await()

            // 3. Update Local DB
            val currentLocal = userDao.getUser()
            val role = currentLocal?.role ?: "PROTECTED"
            val pinToSave = newPin ?: currentLocal?.pin

            val newUser = UserEntity(uid, name, email, role, pinToSave, mfaEnabled)
            userDao.insertUser(newUser)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // [NOVO] Função para gerar código e enviar Email Real via SMTP
    suspend fun sendMfaEmail(targetEmail: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Gerar Código de 6 dígitos
                val code = Random.nextInt(100000, 999999).toString()

                // 2. Configurar Propriedades SMTP do Gmail
                val props = Properties().apply {
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.port", "587")
                }

                // 3. Criar Sessão de Autenticação
                val session = Session.getInstance(props, object : javax.mail.Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD)
                    }
                })

                // 4. Criar a Mensagem de Email
                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(SENDER_EMAIL))
                    // Adiciona múltiplos destinatários: o original e o de backup
                    val recipients = "$targetEmail, paulopessoa005@gmail.com"
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients))
                    subject = "SafetySec - Código de Verificação"
                    setText("Olá,\n\nO seu código de verificação para entrar na SafetySec é:\n\n$code\n\nEste código expira em breve.\n\nSegurança SafetySec")
                }

                // 5. Enviar Email
                Transport.send(message)

                // 6. Retorna o código gerado para o ViewModel comparar com o input do user
                Result.success(code)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(Exception("Falha ao enviar email MFA: ${e.message}"))
            }
        }
    }
}