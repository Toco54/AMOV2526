package pt.isec.amov2526.projetoKotlin.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import pt.isec.amov2526.projetoKotlin.data.repository.AuthRepository

class AuthViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    private val repository = AuthRepository(application as pt.isec.amov2526.projetoKotlin.SafetySecApp)

    // Estados observáveis pela UI (Jetpack Compose)
    var user by mutableStateOf(repository.currentUser)
    var isLoading by mutableStateOf(false)
    var errorMsg by mutableStateOf<String?>(null)

    // Para navegação: guardar se é Monitor ou Protegido
    var userRole by mutableStateOf<String?>(null)

    var userName by mutableStateOf<String>("Carregando...")

    var userEmail by mutableStateOf<String>("")

    init {
        if (user != null) {
            viewModelScope.launch {
                loadUserData(user!!.uid, user!!.email ?: "")
//                isLoading = true
//                userRole = repository.getUserRole(user!!.uid)
//                isLoading = false
            }
        }
    }

    private fun loadUserData(uid: String, email: String) {
        userEmail = email
        viewModelScope.launch {
            isLoading = true
            userRole = repository.getUserRole(uid)
            val nameFromDb = repository.getUserName(uid)
            if (nameFromDb != null) {
                userName = nameFromDb
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
                // Buscar a role para saber para que ecrã ir
//                userRole = repository.getUserRole(user!!.uid)
                loadUserData(user!!.uid, user!!.email ?: "")
            } else {
                errorMsg = result.exceptionOrNull()?.message
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
            } else {
                errorMsg = result.exceptionOrNull()?.message
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
        }
    }
}