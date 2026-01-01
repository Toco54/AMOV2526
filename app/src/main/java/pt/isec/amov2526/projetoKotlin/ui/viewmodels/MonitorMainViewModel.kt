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

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

class MonitorMainViewModel : ViewModel() {
    private val repository = ConnectionRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var listener: ListenerRegistration? = null

    var isLoading by mutableStateOf(false)
    var errorMsg by mutableStateOf<String?>(null)
    var connectionCandidate by mutableStateOf<ConnectionCodeInfo?>(null)
    var proteges = mutableStateListOf<ConnectedUser>()

    init {
        startListeningProteges()
    }

    private fun startListeningProteges() {
        val currentUser = auth.currentUser ?: return
        isLoading = true
        
        val query = db.collection("users").document(currentUser.uid)
            .collection("connections")
            .whereEqualTo("role", "Protege")

        listener = query.addSnapshotListener { snapshot, e ->
            isLoading = false
            if (e != null) {
                errorMsg = e.message ?: "Failed to listen for proteges"
                return@addSnapshotListener
            }

            if (snapshot != null) {
                viewModelScope.launch {
                    val newList = snapshot.documents.mapNotNull { doc ->
                        val uid = doc.getString("uid") ?: ""
                        val revoked = doc.getBoolean("revokedByProtege") ?: false
                        try {
                            val userDoc = db.collection("users").document(uid).get().await()
                            ConnectedUser(
                                uid = uid,
                                name = userDoc.getString("name") ?: "Unknown",
                                email = userDoc.getString("email") ?: "",
                                role = "Protege",
                                revokedByProtege = revoked
                            )
                        } catch (ex: Exception) {
                            null
                        }
                    }
                    proteges.clear()
                    proteges.addAll(newList)
                }
            }
        }
    }

    fun fetchProteges() {
        // No longer needed for one-time fetch, but keeping signature if used elsewhere
        // Or simply trigger a refresh if needed, but the listener handles it.
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
                val candidate = result.getOrNull()
                if (candidate != null) {
                    // Check for duplicate BEFORE showing confirmation
                    val isDuplicate = proteges.any { it.uid == candidate.protegeUid }
                    if (isDuplicate) {
                        errorMsg = "User already in your list"
                    } else {
                        connectionCandidate = candidate
                    }
                }
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

    fun deleteConnection(protegeUid: String) {
        val currentUser = auth.currentUser ?: return
        isLoading = true
        viewModelScope.launch {
            val result = repository.deleteConnection(currentUser.uid, protegeUid)
            if (result.isSuccess) {
                // Fetch handled by listener
            } else {
                errorMsg = result.exceptionOrNull()?.message ?: "Failed to delete connection"
            }
            isLoading = false
        }
    }

    fun clearError() {
        errorMsg = null
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}