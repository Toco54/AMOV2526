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

    // Para navegação: guardar se é Monitor ou Protegido
    var userRole by mutableStateOf<String?>(null)
    
    // Dados completos do perfil (para edição)
    var currentUserData by mutableStateOf<UserEntity?>(null)

    var userName by mutableStateOf<String>("Carregando...")
    var userEmail by mutableStateOf<String>("")

    init {
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
            } else {
                // Fallback (ex: erro de rede ou não encontrado)
                userRole = repository.getUserRole(uid)
                val nameFromDb = repository.getUserName(uid)
                if (nameFromDb != null) userName = nameFromDb
            }
            isLoading = false
        }
    }

    fun login(email: String, pass: String) {
        isLoading = true
        errorMsg = null
        viewModelScope.launch {
            val result = repository.login(email, pass)
            if (result.isSuccess) {
                user = repository.currentUser
                loadUserData(user!!.uid, user!!.email ?: "")
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
            userName = ""
            currentUserData = null
        }
    }
}