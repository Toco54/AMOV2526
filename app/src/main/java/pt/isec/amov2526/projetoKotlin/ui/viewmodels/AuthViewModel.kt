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


    var user by mutableStateOf(repository.currentUser)
    var isLoading by mutableStateOf(false)
    var errorMsg by mutableStateOf<String?>(null)


    var mfaRequired by mutableStateOf(false)
    var generatedMfaCode by mutableStateOf<String?>(null)
    var tempUserEmail by mutableStateOf<String?>(null)


    var userRole by mutableStateOf<String?>(null)


    var currentViewMode by mutableStateOf<String?>(null)


    var currentUserData by mutableStateOf<UserEntity?>(null)



    var isUserInEmergency by mutableStateOf(false)

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


                if (currentViewMode == null) {

                    currentViewMode = if (fullUser.role == "BOTH") "PROTECTED" else fullUser.role
                }
                

                if (fullUser.role == "PROTECTED" || fullUser.role == "BOTH") {
                    try {
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("users").document(uid).get()
                            .addOnSuccessListener { doc ->
                                val status = doc.getString("systemStatus") ?: "OFFLINE"
                                if (status == "ACTIVE") {
                                    val context = getApplication<android.app.Application>()
                                    val intent = android.content.Intent(context, pt.isec.amov2526.projetoKotlin.services.SafetyMonitorService::class.java)
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        context.startForegroundService(intent)
                                    } else {
                                        context.startService(intent)
                                    }
                                }
                            }
                    } catch (e: Exception) {

                    }
                }
            } else {

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

                    val userEntity = repository.getUser(uid)
                    if (userEntity != null && userEntity.mfaEnabled) {


                        tempUserEmail = email

                        
                        val mfaResult = repository.sendMfaEmail(email)

                        if (mfaResult.isSuccess) {
                            generatedMfaCode = mfaResult.getOrNull()
                            mfaRequired = true 
                        } else {
                            
                            repository.logout()
                            errorMsg = "Password correta, mas falha ao enviar email de verificação."
                        }
                    } else {
                        
                        user = repository.currentUser
                        loadUserData(uid, email)
                    }
                } else {
                     user = repository.currentUser
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

    
    fun verifyMfaCode(inputCode: String) {
        if (inputCode == generatedMfaCode) {
            
            mfaRequired = false
            generatedMfaCode = null

            
            user = repository.currentUser

            
            if (user != null) {
                try {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(user!!.uid)
                        .update("lastUpdate", System.currentTimeMillis())
                } catch (e: Exception) {
                    
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

                
                currentViewMode = userRole

                
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
        mfaEnabled: Boolean = false, 
        currentPass: String? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val u = user ?: return
        isLoading = true
        errorMsg = null
        viewModelScope.launch {
            val result = repository.updateProfile(u.uid, name, email, pass, pin, mfaEnabled, currentPass)
            if (result.isSuccess) {
                
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
            val context = getApplication<android.app.Application>()
            try {
                context.stopService(android.content.Intent(context, pt.isec.amov2526.projetoKotlin.services.SafetyMonitorService::class.java))
                context.stopService(android.content.Intent(context, pt.isec.amov2526.projetoKotlin.services.MonitorNotificationService::class.java))
            } catch (e: Exception) {
                
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