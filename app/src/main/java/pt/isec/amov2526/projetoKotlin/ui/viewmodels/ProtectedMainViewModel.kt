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
import kotlinx.coroutines.tasks.await
import pt.isec.amov2526.projetoKotlin.data.model.ConnectedUser
import pt.isec.amov2526.projetoKotlin.data.repository.ConnectionRepository
import pt.isec.amov2526.projetoKotlin.services.SafetyMonitorService

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

// MUDANÇA 1: Herdar de 'AndroidViewModel(application)' para ter acesso ao Contexto (necessário para iniciar Serviços)
class ProtectedMainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ConnectionRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var monitorListener: ListenerRegistration? = null

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
        startListeningMonitors()
        refreshServiceState()
    }
    
    private fun startListeningMonitors() {
        val currentUser = auth.currentUser ?: return
        isLoading = true
        
        val query = db.collection("users").document(currentUser.uid)
            .collection("connections")
            .whereEqualTo("role", "Monitor")
            
        monitorListener = query.addSnapshotListener { snapshot, e ->
            isLoading = false
            if (e != null) {
                errorMsg = e.message
                return@addSnapshotListener
            }
            
            if (snapshot != null) {
                viewModelScope.launch {
                    val newList = snapshot.documents.mapNotNull { doc ->
                        val uid = doc.getString("uid") ?: ""
                        val accessLevel = doc.getString("accessLevel") ?: "Full Monitoring"
                        try {
                            val userDoc = db.collection("users").document(uid).get().await()
                            ConnectedUser(
                                uid = uid,
                                name = userDoc.getString("name") ?: "Unknown",
                                email = userDoc.getString("email") ?: "",
                                role = "Monitor",
                                accessLevel = accessLevel
                            )
                        } catch (ex: Exception) {
                            null
                        }
                    }
                    monitors.clear()
                    monitors.addAll(newList)
                }
            }
        }
    }

    // --- NOVA FUNÇÃO: Ligar/Desligar Proteção ---
    fun toggleProtection() {
        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, SafetyMonitorService::class.java)
        val uid = auth.currentUser?.uid ?: return
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        if (isProtectionActive) {


            // --- DESLIGAR (OFFLINE) ---
            //context.stopService(intent)
            isProtectionActive = false
            //db.collection("users").document(uid).update(updates)
            
            viewModelScope.launch {
                try {
                    val doc = db.collection("users").document(uid).get().await()
                    val status = doc.getString("systemStatus")
                    val pausedUntil = doc.getLong("globalPausedUntil") ?: 0L
                    val now = System.currentTimeMillis()
                    
                    val updates = mutableMapOf<String, Any>("systemStatus" to "OFFLINE")
                    
                    // Se estava em Pausa, guardar o tempo restante
                    if (status == "PAUSED" && pausedUntil > now) {
                        val remaining = pausedUntil - now
                        updates["storedRemainingPause"] = remaining
                        updates["globalPausedUntil"] = 0L // Limpar timer ativo
                    } else if (status == "PAUSED" && pausedUntil == -1L) {
                         // Pausa indefinida
                         updates["storedRemainingPause"] = -1L
                         updates["globalPausedUntil"] = 0L
                    } else {
                        // Limpar qualquer lixo anterior
                         updates["storedRemainingPause"] = 0L
                         updates["globalPausedUntil"] = 0L
                    }
                    
                    db.collection("users").document(uid).update(updates).await()
                } catch (e: Exception) {
                    // Fail silent
                }
            }

        } else {
            // --- LIGAR (ONLINE) ---
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            isProtectionActive = true
            
            viewModelScope.launch {
                try {
                    val doc = db.collection("users").document(uid).get().await()
                    val storedRemaining = doc.getLong("storedRemainingPause") ?: 0L
                    val now = System.currentTimeMillis()
                    
                    val updates = mutableMapOf<String, Any>(
                        "lastUpdate" to now
                    )
                    
                    if (storedRemaining > 0) {
                        // Restaurar Pausa
                        updates["systemStatus"] = "PAUSED"
                        updates["globalPausedUntil"] = now + storedRemaining
                        updates["storedRemainingPause"] = 0L
                    } else if (storedRemaining == -1L) {
                        // Restaurar Pausa Indefinida
                        updates["systemStatus"] = "PAUSED"
                        updates["globalPausedUntil"] = -1L
                        updates["storedRemainingPause"] = 0L
                    } else {
                        // Ativar Normal
                        updates["systemStatus"] = "ACTIVE"
                        updates["globalPausedUntil"] = 0L
                        updates["storedRemainingPause"] = 0L
                    }
                    
                    db.collection("users").document(uid).update(updates).await()
                } catch (e: Exception) {
                    // Fail silent
                }
            }
        }
    }

    // Sincronizar estado visual caso o serviço mude
    fun refreshServiceState() {
        isProtectionActive = SafetyMonitorService.isServiceRunning
    }

    // Used when GuardViewModel handles the DB update (e.g. Indefinite Pause)
    fun stopServiceOnly() {
        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, SafetyMonitorService::class.java)
        context.stopService(intent)
        isProtectionActive = false
    }

    // --- FUNÇÕES EXISTENTES (Lógica do teu colega mantida a 100%) ---

    fun fetchMonitors() {
        // Kept for compatibility but effectively replaced by listener
    }

    fun removeMonitor(targetUid: String) {
        val currentUser = auth.currentUser ?: return
        isLoading = true
        viewModelScope.launch {
            val result = repository.removeConnection(currentUser.uid, targetUid)
            if (result.isSuccess) {
                // fetchMonitors() // Listener updates automatically
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

    override fun onCleared() {
        super.onCleared()
        monitorListener?.remove()
    }
}
