package pt.isec.amov2526.projetoKotlin.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.concurrent.Executor

class EvidenceManager(private val context: Context) {

    private var activeRecording: Recording? = null
    private var videoFile: File? = null
    private var recordingStartTime: Long = 0L
    private var cameraProvider: ProcessCameraProvider? = null

    // Flag para evitar arrancar gravações se o stop já tiver sido chamado
    private var isStopping = false

    @SuppressLint("MissingPermission")
    fun startVideoRecording(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        onVideoFinalized: (File) -> Unit
    ): File? {
        // 1. Reset da flag de paragem
        isStopping = false

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e("EvidenceManager", "Permissões em falta")
            return null
        }

        val file = File(context.cacheDir, "video_${System.currentTimeMillis()}.mp4")
        videoFile = file

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)

        cameraProviderFuture.addListener({
            // 2. SAFETY CHECK: Se o utilizador já saiu do ecrã, NÃO INICIAR NADA.
            // Isto resolve o erro "Released by resource manager" e "Conflict Device 0"
            if (isStopping || lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                Log.w("EvidenceManager", "Ciclo de vida destruído antes da câmara iniciar. Abortar.")
                return@addListener
            }

            try {
                cameraProvider = cameraProviderFuture.get()

                // 3. Garantir limpeza absoluta
                cameraProvider?.unbindAll()

                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(surfaceProvider)

                val qualitySelector = QualitySelector.from(Quality.LOWEST)

                val recorder = Recorder.Builder()
                    .setQualitySelector(qualitySelector)
                    .build()

                val videoCapture = VideoCapture.withOutput(recorder)

                // Tentar vincular
                try {
                    cameraProvider?.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        videoCapture
                    )
                } catch(e: Exception) {
                    Log.e("EvidenceManager", "Erro ao vincular câmara (possivelmente ecrã rodou)", e)
                    return@addListener
                }

                val outputOptions = FileOutputOptions.Builder(file)
                    .setDurationLimitMillis(30000)
                    .build()

                // 4. Iniciar Gravação
                try {
                    activeRecording = videoCapture.output
                        .prepareRecording(context, outputOptions)
                        .withAudioEnabled()
                        .start(executor) { recordEvent ->
                            handleEvent(recordEvent, file, onVideoFinalized)
                        }
                } catch (se: SecurityException) {
                    Log.e("EvidenceManager", "Conflito de áudio ou permissão negada", se)
                } catch (e: Exception) {
                    Log.e("EvidenceManager", "Erro genérico ao iniciar gravação", e)
                }

            } catch (e: Exception) {
                Log.e("EvidenceManager", "Falha crítica ao obter CameraProvider", e)
            }
        }, executor)

        return file
    }

    private fun handleEvent(event: VideoRecordEvent, file: File, onFinalized: (File) -> Unit) {
        when(event) {
            is VideoRecordEvent.Start -> {
                recordingStartTime = System.currentTimeMillis()
                Log.d("EvidenceManager", "Recording STARTED")
            }
            is VideoRecordEvent.Finalize -> {
                // Limpar referência IMEDIATAMENTE
                activeRecording = null

                if (event.hasError()) {
                    // Ignorar erro 6 (Resource Manager) se o ficheiro existir, pois foi um stop forçado
                    val error = event.error
                    Log.w("EvidenceManager", "Finalizado com código: $error")
                }

                Log.d("EvidenceManager", "Recording FINALIZED. File size: ${file.length()}")
                onFinalized(file)
            }
        }
    }

    fun stopVideoRecording(): File? {
        isStopping = true // Marca que queremos parar, bloqueia novos arranques pendentes

        try {
            // Se activeRecording for null, pode ser que a câmara ainda esteja a inicializar (Race Condition).
            // O 'isStopping = true' impede que ela arranque nesse caso.

            if (activeRecording != null) {
                activeRecording?.stop()
                activeRecording = null
            }

            // Força bruta para libertar o microfone/câmara
            cameraProvider?.unbindAll()

        } catch (e: Exception) {
            Log.e("EvidenceManager", "Error stopping video", e)
        }
        return videoFile
    }

    // Métodos auxiliares mantêm-se iguais
    fun getRecordingDuration(): Long = if (recordingStartTime == 0L) 0L else System.currentTimeMillis() - recordingStartTime
    fun getRecordingStartTime(): Long = recordingStartTime

    suspend fun uploadFile(file: File, folder: String): String? {
        if (!file.exists()) return null
        val storageRef = FirebaseStorage.getInstance().reference
        val fileRef = storageRef.child("$folder/${file.name}")
        return try {
            fileRef.putFile(Uri.fromFile(file)).await()
            fileRef.downloadUrl.await().toString()
        } catch (e: Exception) { null }
    }

    fun deleteFile(file: File?) {
        if (file != null && file.exists()) file.delete()
    }
}