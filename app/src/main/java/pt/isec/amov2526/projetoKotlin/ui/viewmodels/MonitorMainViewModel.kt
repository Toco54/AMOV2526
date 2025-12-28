package pt.isec.amov2526.projetoKotlin.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import pt.isec.amov2526.projetoKotlin.data.repository.ConnectionRepository

import pt.isec.amov2526.projetoKotlin.data.repository.ConnectionCodeInfo

import pt.isec.amov2526.projetoKotlin.data.model.ConnectedUser

class MonitorMainViewModel : ViewModel() {
    private val repository = ConnectionRepository()
    private val auth = FirebaseAuth.getInstance()

    var isLoading by mutableStateOf(false)
    var errorMsg by mutableStateOf<String?>(null)
    var connectionCandidate by mutableStateOf<ConnectionCodeInfo?>(null)
    var proteges = mutableStateListOf<ConnectedUser>()

    init {
        fetchProteges()
    }

    fun fetchProteges() {
        val currentUser = auth.currentUser ?: return
        isLoading = true
        viewModelScope.launch {
            val result = repository.getProteges(currentUser.uid)
            if (result.isSuccess) {
                proteges.clear()
                proteges.addAll(result.getOrDefault(emptyList()))
            } else {
                errorMsg = result.exceptionOrNull()?.message ?: "Failed to fetch proteges"
            }
            isLoading = false
        }
    }

    // Step 1: Validate Code
    fun validateCode(code: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            errorMsg = "User not logged in"
            return
        }

        if (code.length != 6) {
            errorMsg = "Code must be 6 digits"
            return
        }

        isLoading = true
        errorMsg = null
        connectionCandidate = null
        viewModelScope.launch {
            val result = repository.validateCode(code)
            if (result.isSuccess) {
                connectionCandidate = result.getOrNull()
            } else {
                errorMsg = result.exceptionOrNull()?.message ?: "Failed to validate code"
            }
            isLoading = false
        }
    }

    // Step 2: Accept Connection
    fun acceptConnection(onSuccess: () -> Unit) {
        val currentUser = auth.currentUser ?: return
        val candidate = connectionCandidate ?: return

        isLoading = true
        errorMsg = null
        viewModelScope.launch {
            val result = repository.connectWithCode(currentUser.uid, candidate.code)
            if (result.isSuccess) {
                connectionCandidate = null // Clear dialog
                fetchProteges()
                onSuccess()
            } else {
                errorMsg = result.exceptionOrNull()?.message ?: "Failed to connect"
            }
            isLoading = false
        }
    }

    fun rejectConnection() {
        connectionCandidate = null
    }

    fun clearError() {
        errorMsg = null
    }
}