package pt.isec.amov2526.projetoKotlin.ui.viewmodels

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import pt.isec.amov2526.projetoKotlin.SafetySecApp
import pt.isec.amov2526.projetoKotlin.data.local.UserEntity
import pt.isec.amov2526.projetoKotlin.data.repository.AuthRepository
import pt.isec.amov2526.projetoKotlin.services.MonitorNotificationService
import pt.isec.amov2526.projetoKotlin.services.SafetyMonitorService

/**
 * ViewModel global de Autenticação.
 *
 * Gere o estado da sessão do utilizador, fluxos de login/registo e lógica de navegação inicial (Role-based).
 * Mantém o estado da UI para:
 * 1. Campos de formulário e erros.
 * 2. Estado de carregamento.
 * 3. Lógica de MFA (Multi-Factor Authentication).
 * 4. Determinação do modo de vista inicial (Monitor vs Protegido).
 */
class AuthViewModel(application: Application) :
    AndroidViewModel(application) {
    private val repository =
        AuthRepository(application as SafetySecApp)


    var user by mutableStateOf(repository.currentUser)
    var isLoading by mutableStateOf(false)
    var errorMsg by mutableStateOf<String?>(null)

    // Estados para fluxo MFA (código enviado por email).
    var mfaRequired by mutableStateOf(false)
    var generatedMfaCode by mutableStateOf<String?>(null)
    var tempUserEmail by mutableStateOf<String?>(null)

    // Role determinado após login/fetch do perfil.
    var userRole by mutableStateOf<String?>(null)

    // Modo de visualização ativo. Útil para utilizadores com role "BOTH" que podem alternar.
    var currentViewMode by mutableStateOf<String?>(null)

    // Dados completos do perfil do utilizador (Nome, PIN, etc.).
    var currentUserData by mutableStateOf<UserEntity?>(null)

    // Flag para controlo de UI durante emergências (evita navegação acidental).
    var isUserInEmergency by mutableStateOf(false)

    var userName by mutableStateOf<String>("Carregando...")
    var userEmail by mutableStateOf<String>("")

    init {
        // Se já existe sessão, carrega os dados imediatamente.
        if (user != null) {
            viewModelScope.launch {
                loadUserData(user!!.uid, user!!.email ?: "")
            }
        }
    }

    private fun loadUserData(uid: String, email: String) {
        userEmail = email
        viewModelScope.launch {
            isLoading = true
            val fullUser = repository.getUser(uid)
            if (fullUser != null) {
                currentUserData = fullUser
                userRole = fullUser.role
                userName = fullUser.name

                // Define o modo inicial. Se for BOTH, começa como PROTECTED por segurança.
                if (currentViewMode == null) {
                    // Default to Protected if Both, as it's the safer default
                    currentViewMode = if (fullUser.role == "BOTH") "PROTECTED" else fullUser.role
                }

                // Se o utilizador é protegido, verifica se o sistema estava ATIVO antes.
                // Se sim, reinicia o serviço de monitorização (recuperação de crash/reboot).
                if (fullUser.role == "PROTECTED" || fullUser.role == "BOTH") {
                    try {
                        FirebaseFirestore.getInstance()
                            .collection("users").document(uid).get()
                            .addOnSuccessListener { doc ->
                                val status = doc.getString("systemStatus") ?: "OFFLINE"
                                if (status == "ACTIVE") {
                                    val context = getApplication<Application>()
                                    val intent = Intent(
                                        context,
                                        SafetyMonitorService::class.java
                                    )
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        context.startForegroundService(intent)
                                    } else {
                                        context.startService(intent)
                                    }
                                }
                            }
                    } catch (e: Exception) {
                        // Ignorar falha na verificação de auto-start
                    }
                }
            } else {
                // Fallback: Tenta obter apenas o role se o perfil completo falhar
                userRole = repository.getUserRole(uid)
                if (currentViewMode == null) {
                    currentViewMode = if (userRole == "BOTH") "PROTECTED" else userRole
                }

                val nameFromDb = repository.getUserName(uid)
                if (nameFromDb != null) userName = nameFromDb
            }
            isLoading = false
        }
    }

    // Apenas utilizadores "BOTH" (Monitor + Protegido) podem alternar de interface.
    val canSwitchProfile: Boolean
        get() = userRole == "BOTH"


    fun switchViewMode() {
        if (!canSwitchProfile) return

        currentViewMode = if (currentViewMode == "MONITOR") {
            "PROTECTED"
        } else {
            "MONITOR"
        }
    }


    fun login(email: String, pass: String) {
        isLoading = true
        errorMsg = null
        viewModelScope.launch {

            val result = repository.login(email, pass)

            if (result.isSuccess) {
                val uid = result.getOrNull()
                if (uid != null) {
                    // Verifica se MFA está ativado no perfil local ou remoto.
                    val userEntity = repository.getUser(uid)
                    if (userEntity != null && userEntity.mfaEnabled) {

                        // Inicia fluxo MFA
                        tempUserEmail = email

                        // Envia email com código
                        val mfaResult = repository.sendMfaEmail(email)

                        if (mfaResult.isSuccess) {
                            generatedMfaCode = mfaResult.getOrNull()
                            mfaRequired = true
                        } else {
                            // Falha no envio do email -> aborta login por segurança
                            repository.logout()
                            errorMsg = "Password correta, mas falha ao enviar email de verificação."
                        }
                    } else {
                        // Login direto sem MFA
                        user = repository.currentUser
                        loadUserData(uid, email)
                    }
                } else {
                    user = repository.currentUser
                }
            } else {
                val exception = result.exceptionOrNull()
                errorMsg = when (exception) {
                    is FirebaseAuthInvalidUserException -> "User Not Found"
                    is FirebaseAuthInvalidCredentialsException -> "Invalid Credentials"
                    is FirebaseNetworkException -> "Network Error"
                    else -> "Login Failed"
                }
            }
            isLoading = false
        }
    }


    fun verifyMfaCode(inputCode: String) {
        if (inputCode == generatedMfaCode) {
            // Código correto -> Finaliza login
            mfaRequired = false
            generatedMfaCode = null

            // Atualiza o estado da UI para refletir o utilizador logado
            user = repository.currentUser

            // Atualiza timestamp de login/atividade
            if (user != null) {
                try {
                    FirebaseFirestore.getInstance()
                        .collection("users").document(user!!.uid)
                        .update("lastUpdate", System.currentTimeMillis())
                } catch (e: Exception) {
                    // Ignora erro de timestamp não crítico
                }
                loadUserData(user!!.uid, user!!.email ?: "")
            }
        } else {
            errorMsg = "Código Incorreto. Verifique o seu email."
        }
    }


    fun cancelMfa() {
        viewModelScope.launch {
            repository.logout()
            mfaRequired = false
            generatedMfaCode = null
            tempUserEmail = null
            errorMsg = null
            isLoading = false
        }
    }

    fun register(email: String, pass: String, name: String, isMonitor: Boolean, pin: String?) {
        isLoading = true
        errorMsg = null
        viewModelScope.launch {
            val result = repository.register(email, pass, name, isMonitor, pin)
            if (result.isSuccess) {
                user = repository.currentUser
                userRole = if (isMonitor) "MONITOR" else "PROTECTED"
                userName = name
                userEmail = email

                // Define modo de vista inicial
                currentViewMode = userRole

                // Carrega dados para garantir consistência
                loadUserData(user!!.uid, email)
            } else {
                val exception = result.exceptionOrNull()
                errorMsg = when (exception) {
                    is FirebaseAuthUserCollisionException -> "Email Already Registered"
                    is FirebaseAuthWeakPasswordException -> "Password Too Weak"
                    is FirebaseAuthInvalidCredentialsException -> "Invalid Email Format"
                    is FirebaseNetworkException -> "Network Error"
                    else -> "Registration Failed"
                }
            }
            isLoading = false
        }
    }

    fun resetPassword(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        isLoading = true
        errorMsg = null
        viewModelScope.launch {
            val result = repository.sendPasswordResetEmail(email)
            if (result.isSuccess) {
                onSuccess()
            } else {
                val exception = result.exceptionOrNull()
                val error = when (exception) {
                    is FirebaseAuthInvalidUserException -> "Email Not Found"
                    is FirebaseNetworkException -> "Network Error"
                    else -> "Reset Failed"
                }
                onError(error)
            }
            isLoading = false
        }
    }

    fun updateProfile(
        name: String,
        email: String,
        pass: String?,
        pin: String?,
        mfaEnabled: Boolean = false,
        currentPass: String? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val u = user ?: return
        isLoading = true
        errorMsg = null
        viewModelScope.launch {
            val result =
                repository.updateProfile(u.uid, name, email, pass, pin, mfaEnabled, currentPass)
            if (result.isSuccess) {
                // Recarrega dados locais após update remoto
                loadUserData(u.uid, email)
                onSuccess()
            } else {
                val exception = result.exceptionOrNull()
                val error = when (exception) {
                    is FirebaseAuthInvalidCredentialsException -> "Current Password Incorrect"
                    is FirebaseNetworkException -> "Network Error"
                    else -> "Change Failed"
                }
                errorMsg = "Update failed: $error"
                onError(error)
            }
            isLoading = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            try {
                context.stopService(
                    Intent(
                        context,
                        SafetyMonitorService::class.java
                    )
                )
                context.stopService(
                    Intent(
                        context,
                        MonitorNotificationService::class.java
                    )
                )
            } catch (e: Exception) {
                // Ignora se serviços já estiverem parados
            }

            repository.logout()
            user = null
            userRole = null
            currentViewMode = null
            userName = ""
            currentUserData = null

            mfaRequired = false
            generatedMfaCode = null
        }
    }

    fun enableDualRole(onSuccess: () -> Unit) {
        val u = user ?: return
        isLoading = true
        viewModelScope.launch {
            val result = repository.promoteToBoth(u.uid)
            if (result.isSuccess) {
                userRole = "BOTH"
                loadUserData(u.uid, userEmail)
                onSuccess()
            } else {
                errorMsg = "Failed to update role"
            }
            isLoading = false
        }
    }
}
