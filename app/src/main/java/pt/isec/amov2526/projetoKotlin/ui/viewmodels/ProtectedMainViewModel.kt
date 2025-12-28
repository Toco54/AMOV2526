package pt.isec.amov2526.projetoKotlin.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import pt.isec.amov2526.projetoKotlin.data.repository.ConnectionRepository

import androidx.compose.runtime.mutableStateListOf
import pt.isec.amov2526.projetoKotlin.data.model.ConnectedUser

class ProtectedMainViewModel : ViewModel() {
    private val repository = ConnectionRepository()
    private val auth = FirebaseAuth.getInstance()

    var isLoading by mutableStateOf(false)
    var generatedCode by mutableStateOf<String?>(null)
    var errorMsg by mutableStateOf<String?>(null)
    var monitors = mutableStateListOf<ConnectedUser>()

    init {
        fetchMonitors()
    }

    fun fetchMonitors() {
        val currentUser = auth.currentUser ?: return
        isLoading = true
        viewModelScope.launch {
            val result = repository.getMonitors(currentUser.uid)
            if (result.isSuccess) {
                monitors.clear()
                monitors.addAll(result.getOrDefault(emptyList()))
            } else {
                errorMsg = result.exceptionOrNull()?.message ?: "Failed to fetch monitors"
            }
            isLoading = false
        }
    }

    fun removeMonitor(targetUid: String) {
        val currentUser = auth.currentUser ?: return
        isLoading = true
        viewModelScope.launch {
            val result = repository.removeConnection(currentUser.uid, targetUid)
            if (result.isSuccess) {
                fetchMonitors()
            } else {
                errorMsg = result.exceptionOrNull()?.message ?: "Failed to remove monitor"
            }
            isLoading = false
        }
    }

    fun generateCode(accessLevel: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            errorMsg = "User not logged in"
            return
        }

        isLoading = true
        errorMsg = null
        generatedCode = null
        viewModelScope.launch {
            val result = repository.generateConnectionCode(currentUser.uid, accessLevel)
            if (result.isSuccess) {
                generatedCode = result.getOrNull()
            } else {
                errorMsg = result.exceptionOrNull()?.message ?: "Failed to generate code"
            }
            isLoading = false
        }
    }

    fun clearError() {
        errorMsg = null
    }
}