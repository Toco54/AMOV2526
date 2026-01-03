package pt.isec.amov2526.projetoKotlin.ui.screens.protected

import android.location.Location
import android.util.Log
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pt.isec.amov2526.projetoKotlin.data.repository.AlertRepository
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.alert.ConfirmedPanel
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.alert.CountdownPanel
import pt.isec.amov2526.projetoKotlin.utils.EvidenceManager
import java.io.File

private val DarkBackground = Color(0xFF0F172A)

@Composable
fun AlertScreen(
    alertType: String = "SOS",
    onCancel: () -> Unit,
    onConfirm: suspend () -> String?,
    onValidatePin: suspend (String) -> Boolean,
    currentLocation: Location? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val evidenceManager = remember { EvidenceManager(context) }
    val alertRepository = remember { AlertRepository() }
    val auth = remember { FirebaseAuth.getInstance() }

    var isConfirmed by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var alertId by remember { mutableStateOf<String?>(null) }
    var isRecordingFinished by remember { mutableStateOf(false) }
    var recordingStartTime by remember { mutableLongStateOf(0L) }
    
    var finalizedFile by remember { mutableStateOf<File?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val previewView = remember { PreviewView(context) }
    
    fun performUpload(file: File, id: String) {
        if (isUploading) return
        isUploading = true
        scope.launch {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                val vUrl = evidenceManager.uploadFile(file, "users/$uid/alerts/$id/video")
                alertRepository.updateAlertEvidence(id, null, vUrl, null)
                isRecordingFinished = true
            }
        }
    }

    LaunchedEffect(finalizedFile, alertId) {
        if (finalizedFile != null && alertId != null && isConfirmed) {
            performUpload(finalizedFile!!, alertId!!)
        }
    }

    // Lifecycle Aware Recording Start/Stop
    DisposableEffect(lifecycleOwner) {
        // Iniciar
        val file = evidenceManager.startVideoRecording(
            lifecycleOwner = lifecycleOwner,
            surfaceProvider = previewView.surfaceProvider,
            onVideoFinalized = { finalizedFile = it }
        )

        onDispose {
            // STOP IMPERATIVO
            Log.d("AlertScreen", "Disposing screen, stopping recording")
            evidenceManager.stopVideoRecording()
        }
    }
    
    LaunchedEffect(isConfirmed) {
        if (isConfirmed) {
             while (recordingStartTime == 0L) {
                 recordingStartTime = evidenceManager.getRecordingStartTime()
                 delay(100)
                 if (isRecordingFinished) break 
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
        if (!isConfirmed && pinInput.length == 4) {
            if (onValidatePin(pinInput)) handleCancel()
            else { delay(500); pinInput = "" }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        if (!isConfirmed) {
            CountdownPanel(
                alertType = alertType, pinInput = pinInput,
                onPinDigit = { if (pinInput.length < 4) pinInput += it },
                onPinDelete = { if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1) },
                onTimerFinished = { 
                    isConfirmed = true
                    scope.launch { alertId = onConfirm() } 
                },
                onHelpNow = { 
                    isConfirmed = true
                    scope.launch { alertId = onConfirm() } 
                }
            )
        } else {
            ConfirmedPanel(
                currentLocation = currentLocation,
                alertId = alertId,
                previewView = previewView,
                isRecordingFinished = isRecordingFinished,
                recordingStartTime = recordingStartTime,
                onClose = { onCancel() }
            )
        }
    }
}
