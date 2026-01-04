package pt.isec.amov2526.projetoKotlin.data.repository


import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import pt.isec.amov2526.projetoKotlin.SafetySecApp
import pt.isec.amov2526.projetoKotlin.data.local.UserEntity
import java.util.Properties
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import kotlin.random.Random

/**
 * Repositório central de Autenticação e Gestão de Perfil.
 *
 * Responsável por:
 * 1. Autenticação via Firebase Auth (Login, Registo, Reset Password).
 * 2. Gestão do documento de utilizador no Firestore (Perfil, Role, Status).
 * 3. Cache local do utilizador via Room (para acesso offline e rapidez).
 * 4. Envio de emails MFA personalizados via SMTP (Gmail).
 */
class AuthRepository(private val app: SafetySecApp) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val userDao = app.database.userDao()

    // Credenciais para envio de emails SMTP.
    // NOTE: Hardcoded para demonstração académica. Em produção, usar backend server-side.
    private val SENDER_EMAIL = "safetysec.projeto@gmail.com"
    private val SENDER_PASSWORD = "stej jhqd qznj qpkz"


    val currentUser get() = auth.currentUser

    /**
     * Autentica o utilizador e sincroniza o perfil local.
     *
     * @return Result com o UID do utilizador em sucesso.
     */
    suspend fun login(email: String, pass: String): Result<String> {
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()

            val uid = auth.currentUser?.uid ?: throw Exception("UID nulo")

            // Sincronização imediata do perfil Firestore -> Room
            val doc = db.collection("users").document(uid).get().await()

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

    /**
     * Regista um novo utilizador no Firebase Auth e cria o documento inicial no Firestore.
     *
     * @param isMonitor Flag para definir o role inicial (MONITOR ou PROTECTED).
     * @param pin PIN de segurança (obrigatório para PROTECTED).
     */
    suspend fun register(
        email: String,
        pass: String,
        name: String,
        isMonitor: Boolean,
        pin: String?
    ): Result<String> {
        return try {

            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            val uid = result.user?.uid ?: throw Exception("Erro ao obter UID")


            val role = if (isMonitor) "MONITOR" else "PROTECTED"

            val userProfile = hashMapOf(
                "uid" to uid,
                "name" to name,
                "email" to email,
                "role" to role,
                "pin" to (pin ?: ""),
                "mfaEnabled" to false,
                "systemStatus" to "OFFLINE", // Estado inicial seguro
                "globalPausedUntil" to 0L,
                "activeProtections" to emptyMap<String, Any>(), // Mapa vazio para futuras regras
                "associatedIds" to emptyList<String>(),
                "fcmToken" to ""
            )

            // Escrita no Firestore e Cache Local
            db.collection("users").document(uid).set(userProfile).await()
            val localUser =
                UserEntity(uid, name, email, if (isMonitor) "MONITOR" else "PROTECTED", pin, false)
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

    /**
     * Obtém o Role do utilizador (MONITOR, PROTECTED, BOTH).
     * Tenta primeiro a cache local, fallback para rede.
     */
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
            doc.getString("name")
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

    /**
     * Atualiza o perfil do utilizador, incluindo dados sensíveis (Password, Email).
     * Requer reautenticação se a password for alterada.
     * Atualiza automaticamente o Role para 'BOTH' se um Monitor adicionar um PIN.
     */
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


            if (currentPass != null) {
                val credential = EmailAuthProvider.getCredential(user.email!!, currentPass)
                user.reauthenticate(credential).await()
            } else if (!newPass.isNullOrEmpty() || email != user.email) {
                // TODO: Forçar reautenticação se não for fornecida a pass atual mas for necessária

            }


            if (email != user.email) {
                user.updateEmail(email).await()
            }
            if (!newPass.isNullOrEmpty()) {
                user.updatePassword(newPass).await()
            }


            val updates = hashMapOf<String, Any>(
                "name" to name,
                "email" to email,
                "mfaEnabled" to mfaEnabled
            )
            if (newPin != null) {
                updates["pin"] = newPin
            }
            db.collection("users").document(uid).update(updates).await()


            // Lógica de promoção de Role
            val currentLocal = userDao.getUser()
            val currentRole = currentLocal?.role ?: "PROTECTED"
            val pinToSave = newPin ?: currentLocal?.pin

            // Se é MONITOR e define um PIN, torna-se BOTH (pode ser Protegido também)
            val newRole = if (currentRole == "MONITOR" && !pinToSave.isNullOrEmpty()) {
                "BOTH"
            } else {
                currentRole
            }


            if (newRole != currentRole) {
                db.collection("users").document(uid).update("role", newRole).await()
            }

            val newUser = UserEntity(uid, name, email, newRole, pinToSave, mfaEnabled)
            userDao.insertUser(newUser)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Envia um email com código MFA usando SMTP direto.
     *
     * @return O código gerado para validação na UI.
     */
    suspend fun sendMfaEmail(targetEmail: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {

                val code = Random.nextInt(100000, 999999).toString()


                val props = Properties().apply {
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.port", "587")
                }


                val session = Session.getInstance(props, object : javax.mail.Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD)
                    }
                })


                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(SENDER_EMAIL))

                    // Hardcoded fallback email para debug/monitorização do sistema
                    val recipients = "$targetEmail, paulopessoa005@gmail.com"
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients))
                    subject = "SafetySec - Código de Verificação"
                    setText("Olá,\n\nO seu código de verificação para entrar na SafetySec é:\n\n$code\n\nEste código expira em breve.\n\nSegurança SafetySec")
                }


                Transport.send(message)


                Result.success(code)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(Exception("Falha ao enviar email MFA: ${e.message}"))
            }
        }
    }

    suspend fun promoteToBoth(uid: String): Result<Unit> {
        return try {

            db.collection("users").document(uid).update("role", "BOTH").await()


            val currentLocal = userDao.getUser()
            if (currentLocal != null) {
                val updated = currentLocal.copy(role = "BOTH")
                userDao.insertUser(updated)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
