package pt.isec.amov2526.projetoKotlin.ui.viewmodels

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import pt.isec.amov2526.projetoKotlin.data.model.ConnectedUser
import pt.isec.amov2526.projetoKotlin.data.repository.ConnectionRepository
import pt.isec.amov2526.projetoKotlin.services.SafetyMonitorService

// MUDANÇA 1: Herdar de 'AndroidViewModel(application)' para ter acesso ao Contexto (necessário para iniciar Serviços)
class ProtectedMainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ConnectionRepository()
    private val auth = FirebaseAuth.getInstance()

    // --- ESTADOS EXISTENTES (Do teu colega) ---
    var isLoading by mutableStateOf(false)
    var generatedCode by mutableStateOf<String?>(null)
    var errorMsg by mutableStateOf<String?>(null)
    var monitors = mutableStateListOf<ConnectedUser>()

    // --- NOVO ESTADO (Para o Serviço de Segurança) ---
    // Verifica se o serviço já está a correr (útil se fechares e abrires a app)
    var isProtectionActive by mutableStateOf(SafetyMonitorService.isServiceRunning)
        private set

    init {
        fetchMonitors()
        refreshServiceState()
    }

    // --- NOVA FUNÇÃO: Ligar/Desligar Proteção ---
    fun toggleProtection() {
        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, SafetyMonitorService::class.java)

        if (isProtectionActive) {
            // Desligar
            context.stopService(intent)
            isProtectionActive = false
        } else {
            // Ligar (Adaptado para versões recentes do Android)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            isProtectionActive = true
        }
    }

    // Sincronizar estado visual caso o serviço mude
    fun refreshServiceState() {
        isProtectionActive = SafetyMonitorService.isServiceRunning
    }

    // --- FUNÇÕES EXISTENTES (Lógica do teu colega mantida a 100%) ---

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