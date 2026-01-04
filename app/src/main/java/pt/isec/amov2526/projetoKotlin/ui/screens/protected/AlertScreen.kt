package pt.isec.amov2526.projetoKotlin.ui.screens.protected

import android.location.Location
import android.util.Log
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate900
import pt.isec.amov2526.projetoKotlin.utils.EvidenceManager
import java.io.File

/**
 * Ecrã de Gestão de Alerta Ativo.
 *
 * Apresenta o fluxo crítico de emergência:
 * 1. Contagem decrescente com opção de cancelamento via PIN.
 * 2. Estado "Confirmado": Gravação automática de vídeo, upload de evidências e feedback de envio.
 *
 * Utiliza o `EvidenceManager` para capturar vídeo em background (ou preview) e fazer upload para o Firebase.
 */
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

    // Estados de gravação e upload
    var isRecordingFinished by remember { mutableStateOf(false) }
    var recordingStartTime by remember { mutableLongStateOf(0L) }

    var finalizedFile by remember { mutableStateOf<File?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val previewView = remember { PreviewView(context) }


    val SKIP_UPLOAD_FOR_TESTING = false

    /**
     * Inicia o upload do ficheiro de vídeo para o alerta específico.
     * Atualiza o documento do alerta no Firestore com o URL do vídeo após sucesso.
     */
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

    // Trigger de upload assim que o ficheiro de vídeo é finalizado e o alerta está confirmado (ID válido).
    LaunchedEffect(finalizedFile, alertIdFromIntent) {
        if (finalizedFile != null && alertIdFromIntent != null && isConfirmedState) {
            performUpload(finalizedFile!!, alertIdFromIntent)
        }
    }


    // Ciclo de vida da gravação: Inicia ao montar o ecrã, para ao desmontar.
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

    // Sincronização do temporizador de UI com o início real da gravação do hardware.
    LaunchedEffect(isConfirmedState) {
        if (isConfirmedState) {
            while (recordingStartTime == 0L) {
                recordingStartTime = evidenceManager.getRecordingStartTime()
                delay(100)
                if (isRecordingFinished) break
            }
        }
    }

    // Watchdog de segurança: Se a gravação/upload falhar ou demorar demasiado (>25s), liberta a UI para não prender o utilizador.
    LaunchedEffect(isConfirmedState) {
        if (isConfirmedState) {
            delay(25000L)
            if (!isRecordingFinished) {
                Log.e(
                    "AlertScreen",
                    "Watchdog triggered: Recording/Upload timed out. Forcing UI unlock."
                )

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
            // Limpeza de ficheiros temporários em caso de cancelamento.
            evidenceManager.deleteFile(vFile)
        }
        onCancel()
    }

    // Validação automática do PIN quando atinge 4 dígitos.
    LaunchedEffect(pinInput) {
        if (!isConfirmedState && pinInput.length == 4) {
            if (onValidatePin(pinInput)) handleCancel()
            else {
                delay(500); pinInput = ""
            }
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Slate900)) {
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
