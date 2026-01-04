package pt.isec.amov2526.projetoKotlin.ui.screens.protected

import android.location.Location
import android.util.Log
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pt.isec.amov2526.projetoKotlin.data.model.InfractionDisplayInfo
import pt.isec.amov2526.projetoKotlin.data.model.MonitorRule
import pt.isec.amov2526.projetoKotlin.data.repository.AlertRepository
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.alert.ConfirmedPanel
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.alert.CountdownPanel
import pt.isec.amov2526.projetoKotlin.ui.theme.*
import pt.isec.amov2526.projetoKotlin.utils.EvidenceManager
import java.io.File

@Composable
fun AlertScreen(
    alertType: String = "SOS",
    alertDetails: String? = null,
    alertIdFromIntent: String? = null,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    onValidatePin: suspend (String) -> Boolean,
    currentLocation: Location? = null,
    activeGeofences: List<MonitorRule> = emptyList(),
    infractions: List<InfractionDisplayInfo> = emptyList(),
    initialSeconds: Int = 10,
    isConfirmedState: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val evidenceManager = remember { EvidenceManager(context) }
    val alertRepository = remember { AlertRepository() }
    val auth = remember { FirebaseAuth.getInstance() }


    var pinInput by remember { mutableStateOf("") }

    
    var isRecordingFinished by remember { mutableStateOf(false) }
    var recordingStartTime by remember { mutableLongStateOf(0L) }
    
    var finalizedFile by remember { mutableStateOf<File?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val previewView = remember { PreviewView(context) }
    

    val SKIP_UPLOAD_FOR_TESTING = false

    fun performUpload(file: File, id: String) {
        if (isUploading) return
        isUploading = true
        scope.launch {
            if (SKIP_UPLOAD_FOR_TESTING) {
                delay(2000)
                isRecordingFinished = true
                return@launch
            }

            val uid = auth.currentUser?.uid
            if (uid != null) {

                val vUrl = evidenceManager.uploadFile(file, "users/$uid/alerts/$id")
                if (vUrl != null) {
                    alertRepository.updateAlertEvidence(id, vUrl)
                } else {
                    Log.e("AlertScreen", "Upload returned NULL. Check logs.")
                    hasError = true
                    errorMessage = "UPLOADING FAILED"
                }

                isRecordingFinished = true
            } else {
                isRecordingFinished = true
            }
            isUploading = false
        }
    }

    LaunchedEffect(finalizedFile, alertIdFromIntent) {
        if (finalizedFile != null && alertIdFromIntent != null && isConfirmedState) {
            performUpload(finalizedFile!!, alertIdFromIntent)
        }
    }



    DisposableEffect(lifecycleOwner, alertIdFromIntent) {
        evidenceManager.startVideoRecording(
            lifecycleOwner = lifecycleOwner,
            surfaceProvider = previewView.surfaceProvider,
            onVideoFinalized = { finalizedFile = it }
        )

        onDispose {
            Log.d("AlertScreen", "Disposing screen, stopping recording")
            evidenceManager.stopVideoRecording()
        }
    }


    LaunchedEffect(isConfirmedState) {
        if (isConfirmedState) {
             while (recordingStartTime == 0L) {
                 recordingStartTime = evidenceManager.getRecordingStartTime()
                 delay(100)
                 if (isRecordingFinished) break 
             }
        }
    }


    LaunchedEffect(isConfirmedState) {
        if (isConfirmedState) {
            delay(25000L)
            if (!isRecordingFinished) {
                Log.e("AlertScreen", "Watchdog triggered: Recording/Upload timed out. Forcing UI unlock.")

                evidenceManager.stopVideoRecording()
                hasError = true
                errorMessage = "RECORDING FAILED"
                isRecordingFinished = true
                isUploading = false
            }
        }
    }

    fun handleCancel() {
        val vFile = evidenceManager.stopVideoRecording()
        scope.launch {
            delay(500)
            evidenceManager.deleteFile(vFile)
        }
        onCancel()
    }

    LaunchedEffect(pinInput) {
        if (!isConfirmedState && pinInput.length == 4) {
            if (onValidatePin(pinInput)) handleCancel()
            else { delay(500); pinInput = "" }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Slate900)) {
        if (!isConfirmedState) {
            CountdownPanel(
                alertType = alertType,
                alertDetails = alertDetails,
                pinInput = pinInput,
                initialSeconds = initialSeconds,
                onPinDigit = { if (pinInput.length < 4) pinInput += it },
                onPinDelete = { if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1) },
                onTimerFinished = { 
                    onConfirm() 
                },
                onHelpNow = { 
                    onConfirm() 
                }
            )
        } else {
            ConfirmedPanel(
                currentLocation = currentLocation,
                activeGeofences = activeGeofences,
                infractions = infractions,
                previewView = previewView,
                isRecordingFinished = isRecordingFinished,
                recordingStartTime = recordingStartTime,
                hasError = hasError,
                errorMessage = errorMessage,
                recordedVideoFile = finalizedFile,
                onClose = { onCancel() }
            )
        }
    }
}