package pt.isec.amov2526.projetoKotlin.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import pt.isec.amov2526.projetoKotlin.data.repository.AuthRepository

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    // Estados observáveis pela UI (Jetpack Compose)
    var user by mutableStateOf(repository.currentUser)
    var isLoading by mutableStateOf(false)
    var errorMsg by mutableStateOf<String?>(null)

    // Para navegação: guardar se é Monitor ou Protegido
    var userRole by mutableStateOf<String?>(null)

    init {
        if (user != null) {
            viewModelScope.launch {
                isLoading = true
                userRole = repository.getUserRole(user!!.uid)
                isLoading = false
            }
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
                userRole = repository.getUserRole(user!!.uid)
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
            } else {
                errorMsg = result.exceptionOrNull()?.message
            }
            isLoading = false
        }
    }

    fun logout() {
        repository.logout()
        user = null
        userRole = null
    }
}