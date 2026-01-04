package pt.isec.amov2526.projetoKotlin.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import pt.isec.amov2526.projetoKotlin.data.local.UserEntity
import pt.isec.amov2526.projetoKotlin.data.repository.AuthRepository

class AuthViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    private val repository = AuthRepository(application as pt.isec.amov2526.projetoKotlin.SafetySecApp)

    // Estados observáveis pela UI (Jetpack Compose)
    var user by mutableStateOf(repository.currentUser)
    var isLoading by mutableStateOf(false)
    var errorMsg by mutableStateOf<String?>(null)

    // [NOVO] Estados para o MFA
    var mfaRequired by mutableStateOf(false)
    var generatedMfaCode by mutableStateOf<String?>(null)
    var tempUserEmail by mutableStateOf<String?>(null)

    // Para navegação: guardar se é Monitor ou Protegido
    var userRole by mutableStateOf<String?>(null)

    // Modo de Visualização Atual (Dinâmico para permitir switch)
    var currentViewMode by mutableStateOf<String?>(null)

    // Dados completos do perfil (para edição)
    var currentUserData by mutableStateOf<UserEntity?>(null)

    var userName by mutableStateOf<String>("Carregando...")
    var userEmail by mutableStateOf<String>("")

    init {
        // Se o user já estava logado (sessão persistente), carregamos os dados
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

                // Inicializa o modo de visualização com o role da BD se ainda não estiver definido
                if (currentViewMode == null) {
                    currentViewMode = fullUser.role
                }
            } else {
                // Fallback (ex: erro de rede ou não encontrado)
                userRole = repository.getUserRole(uid)
                if (currentViewMode == null) {
                    currentViewMode = userRole
                }

                val nameFromDb = repository.getUserName(uid)
                if (nameFromDb != null) userName = nameFromDb
            }
            isLoading = false
        }
    }

    // Função para alternar entre modos
    fun switchViewMode() {
        currentViewMode = if (currentViewMode == "MONITOR") {
            "PROTECTED"
        } else {
            "MONITOR"
        }
    }

    // [ALTERADO] Login agora inicia o processo MFA
    fun login(email: String, pass: String) {
        isLoading = true
        errorMsg = null
        viewModelScope.launch {
            // 1. Autenticação Base (Firebase)
            val result = repository.login(email, pass)

            if (result.isSuccess) {
                // SUCESSO na password.
                // Mas NÃO definimos o 'user' ainda. Vamos iniciar o MFA.
                tempUserEmail = email

                // 2. Enviar Email MFA
                val mfaResult = repository.sendMfaEmail(email)

                if (mfaResult.isSuccess) {
                    generatedMfaCode = mfaResult.getOrNull()
                    mfaRequired = true // Isto faz a UI mudar para o ecrã de código
                } else {
                    // Se falhar o envio do email, fazemos logout forçado
                    repository.logout()
                    errorMsg = "Password correta, mas falha ao enviar email de verificação."
                }
            } else {
                val exception = result.exceptionOrNull()
                errorMsg = when (exception) {
                    is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "User Not Found"
                    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Invalid Credentials"
                    is com.google.firebase.FirebaseNetworkException -> "Network Error"
                    else -> "Login Failed"
                }
            }
            isLoading = false
        }
    }

    // [NOVO] Verifica o código inserido
    fun verifyMfaCode(inputCode: String) {
        if (inputCode == generatedMfaCode) {
            // CÓDIGO CORRETO -> Finalizar Login
            mfaRequired = false
            generatedMfaCode = null

            // Atualizar o estado principal 'user' que vai desencadear a navegação
            user = repository.currentUser

            // Initial Ping e Carregamento de dados
            if (user != null) {
                try {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(user!!.uid)
                        .update("lastUpdate", System.currentTimeMillis())
                } catch (e: Exception) {
                    // Ignore network error on initial ping
                }
                loadUserData(user!!.uid, user!!.email ?: "")
            }
        } else {
            errorMsg = "Código Incorreto. Verifique o seu email."
        }
    }

    // [NOVO] Cancelar o processo MFA
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

                // Define o modo inicial após registo
                currentViewMode = userRole

                // Force load to set currentUserData
                loadUserData(user!!.uid, email)
            } else {
                val exception = result.exceptionOrNull()
                errorMsg = when (exception) {
                    is com.google.firebase.auth.FirebaseAuthUserCollisionException -> "Email Already Registered"
                    is com.google.firebase.auth.FirebaseAuthWeakPasswordException -> "Password Too Weak"
                    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Invalid Email Format"
                    is com.google.firebase.FirebaseNetworkException -> "Network Error"
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
                    is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "Email Not Found"
                    is com.google.firebase.FirebaseNetworkException -> "Network Error"
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
        currentPass: String? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val u = user ?: return
        isLoading = true
        errorMsg = null
        viewModelScope.launch {
            val result = repository.updateProfile(u.uid, name, email, pass, pin, currentPass)
            if (result.isSuccess) {
                // Refresh data
                loadUserData(u.uid, email)
                onSuccess()
            } else {
                val exception = result.exceptionOrNull()
                val error = when (exception) {
                    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Current Password Incorrect"
                    is com.google.firebase.FirebaseNetworkException -> "Network Error"
                    else -> exception?.message ?: "Update Failed"
                }
                errorMsg = "Update failed: $error"
                onError(error)
            }
            isLoading = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            user = null
            userRole = null
            currentViewMode = null
            userName = ""
            currentUserData = null
            // Reset MFA
            mfaRequired = false
            generatedMfaCode = null
        }
    }
}