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
import pt.isec.amov2526.projetoKotlin.data.model.MonitorRule
import pt.isec.amov2526.projetoKotlin.data.model.InfractionDisplayInfo
import java.io.File

@Composable
fun AlertScreen(
    alertType: String = "SOS",
    alertDetails: String? = null,
    alertIdFromIntent: String? = null, // [NOVO]
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    onValidatePin: suspend (String) -> Boolean,
    currentLocation: Location? = null,
    activeGeofences: List<MonitorRule> = emptyList(),
    infractions: List<InfractionDisplayInfo> = emptyList(),
    initialSeconds: Int = 10,
    isConfirmedState: Boolean = false // [NOVO] Estado recebido do pai
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val evidenceManager = remember { EvidenceManager(context) }
    val alertRepository = remember { AlertRepository() }
    val auth = remember { FirebaseAuth.getInstance() }

    // [CORREÇÃO] Usar estado do pai
    var pinInput by remember { mutableStateOf("") }
    // var alertId não é necessário, usamos alertIdFromIntent diretamente
    
    var isRecordingFinished by remember { mutableStateOf(false) }
    var recordingStartTime by remember { mutableLongStateOf(0L) }
    
    var finalizedFile by remember { mutableStateOf<File?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val previewView = remember { PreviewView(context) }
    
    // Flag para facilitar testes sem carregar para o Firebase (mudar para false em produção)
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
                // MultiMonitor: O vídeo é carregado uma vez para a pasta do alerta
                val vUrl = evidenceManager.uploadFile(file, "users/$uid/alerts/$id")
                if (vUrl != null) {
                    alertRepository.updateAlertEvidence(id, vUrl)
                } else {
                    Log.e("AlertScreen", "Upload returned NULL. Check logs.")
                    hasError = true
                    errorMessage = "UPLOADING FAILED"
                }
                // [CORREÇÃO] Sempre finalizar, mesmo que falhe, para não prender a UI
                isRecordingFinished = true
            } else {
                isRecordingFinished = true
            }
            isUploading = false // Reset flag
        }
    }

    LaunchedEffect(finalizedFile, alertIdFromIntent) {
        if (finalizedFile != null && alertIdFromIntent != null && isConfirmedState) {
            performUpload(finalizedFile!!, alertIdFromIntent)
        }
    }

    // Iniciar gravação assim que o ecrã aparece (mesmo em countdown)
    // [CORREÇÃO] Reiniciar gravação se o alertId mudar
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

    // Sincronizar o timer visual com o início real da gravação
    LaunchedEffect(isConfirmedState) {
        if (isConfirmedState) {
             while (recordingStartTime == 0L) {
                 recordingStartTime = evidenceManager.getRecordingStartTime()
                 delay(100)
                 if (isRecordingFinished) break 
             }
        }
    }

    // [NOVO] Watchdog de Segurança: Garante que a UI nunca fica presa em "UPLOADING"
    LaunchedEffect(isConfirmedState) {
        if (isConfirmedState) {
            delay(25000L) // 25 Segundos (12s gravação + 13s margem para falhas/upload lento)
            if (!isRecordingFinished) {
                Log.e("AlertScreen", "Watchdog triggered: Recording/Upload timed out. Forcing UI unlock.")
                // Tenta parar gravação se ainda estiver a decorrer
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

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
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
                recordedVideoFile = finalizedFile, // [NOVO]
                onClose = { onCancel() }
            )
        }
    }
}