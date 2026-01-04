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
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File

/**
 * Gestor de captura e upload de evidências (Vídeo/Áudio) em situações de emergência.
 *
 * Utiliza a biblioteca CameraX para gravação de fundo (quando possível) ou pré-visualização.
 * Gere o ciclo de vida da câmara e a transferência assíncrona para o Firebase Storage.
 */
class EvidenceManager(private val context: Context) {

    private var activeRecording: Recording? = null
    private var videoFile: File? = null
    private var recordingStartTime: Long = 0L
    private var cameraProvider: ProcessCameraProvider? = null

    /**
     * Inicia a gravação de vídeo utilizando a câmara traseira.
     *
     * Configura a qualidade para a mais baixa possível para garantir uploads rápidos em redes móveis.
     * Define um limite de duração (12s) para criar clipes curtos e leves.
     *
     * @param lifecycleOwner O proprietário do ciclo de vida (Activity/Fragment) para vincular a câmara.
     * @param surfaceProvider Provider de superfície para pré-visualização (opcional, mas recomendado pelo CameraX).
     * @param onVideoFinalized Callback invocado quando o vídeo é guardado com sucesso no disco local.
     * @return O ficheiro temporário onde o vídeo será guardado, ou null se não houver permissões.
     */
    @SuppressLint("MissingPermission")
    fun startVideoRecording(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        onVideoFinalized: (File) -> Unit
    ): File? {
        // Validação defensiva de permissões antes de tentar aceder ao hardware.
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        val file = File(context.cacheDir, "video_${System.currentTimeMillis()}.mp4")
        videoFile = file

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)

        cameraProviderFuture.addListener({
            // Prevenção de crashes: Não tentar iniciar câmara se a UI já foi destruída.
            if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                return@addListener
            }

            try {
                cameraProvider = cameraProviderFuture.get()

                // Limpeza de casos de uso anteriores para evitar conflitos de recursos.
                cameraProvider?.unbindAll()

                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(surfaceProvider)

                // Estratégia de Qualidade: Preferência por SD (Lowest) para upload rápido.
                val qualitySelector = QualitySelector.from(
                    Quality.LOWEST,
                    FallbackStrategy.higherQualityOrLowerThan(Quality.LOWEST)
                )

                val recorder = Recorder.Builder()
                    .setQualitySelector(qualitySelector)
                    .build()

                val videoCapture = VideoCapture.withOutput(recorder)

                // Vinculação ao ciclo de vida: Garante que a câmara fecha se a app for para background (salvo em serviços foreground específicos).
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    videoCapture
                )

                val outputOptions = FileOutputOptions.Builder(file)
                    .setDurationLimitMillis(12000) // Limite rígido de 12 segundos.
                    .build()

                activeRecording = videoCapture.output
                    .prepareRecording(context, outputOptions)
                    .withAudioEnabled()
                    .start(executor) { recordEvent ->
                        when (recordEvent) {
                            is VideoRecordEvent.Start -> {
                                recordingStartTime = System.currentTimeMillis()
                                Log.d("EvidenceManager", "Recording effectively started")
                            }

                            is VideoRecordEvent.Finalize -> {
                                if (recordEvent.hasError()) {
                                    val errorMsg =
                                        "Video capture failed: code=${recordEvent.error}, cause=${recordEvent.cause?.message}"
                                    Log.e("EvidenceManager", errorMsg)
                                    // NOTE: Em produção, deveria haver uma política de retry ou fallback para apenas áudio.
                                } else {
                                    Log.d(
                                        "EvidenceManager",
                                        "Recording finalize event received successfully"
                                    )
                                }
                                activeRecording = null
                                onVideoFinalized(file)
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e("EvidenceManager", "Camera binding or start failed", e)
            }
        }, executor)

        return file
    }

    /**
     * Para a gravação atual de forma segura.
     *
     * @return O ficheiro de vídeo resultante (pode ainda não estar finalizado assincronamente).
     */
    fun stopVideoRecording(): File? {
        try {
            activeRecording?.let {
                it.stop()
                activeRecording = null
            }
        } catch (e: Exception) {
            Log.e("EvidenceManager", "Error during stopVideoRecording", e)
        }
        return videoFile
    }

    fun getRecordingDuration(): Long {
        if (recordingStartTime == 0L) return 0L
        return System.currentTimeMillis() - recordingStartTime
    }

    fun getRecordingStartTime(): Long {
        return recordingStartTime
    }

    /**
     * Realiza o upload do ficheiro de vídeo para o Firebase Storage.
     *
     * @param file O ficheiro local a enviar.
     * @param folder O caminho/pasta de destino no Storage.
     * @return O URL de download público do ficheiro, ou null em caso de erro.
     */
    suspend fun uploadFile(file: File, folder: String): String? {
        if (!file.exists()) {
            Log.e("EvidenceManager", "Upload failed: File does not exist -> ${file.absolutePath}")
            return null
        }
        if (file.length() == 0L) {
            Log.e(
                "EvidenceManager",
                "Upload failed: File is empty (0 bytes). Recording likely failed."
            )
            return null
        }

        Log.d(
            "EvidenceManager",
            "Starting upload: ${file.name} (${file.length()} bytes) to $folder"
        )

        val storageRef = FirebaseStorage.getInstance().reference
        val fileRef = storageRef.child("$folder/${file.name}")

        return try {
            fileRef.putFile(Uri.fromFile(file)).await()
            val url = fileRef.downloadUrl.await().toString()
            Log.d("EvidenceManager", "Upload success: $url")
            url
        } catch (e: Exception) {
            Log.e("EvidenceManager", "Firebase Upload failed", e)
            null
        }
    }

    fun deleteFile(file: File?) {
        if (file != null && file.exists()) {
            file.delete()
        }
    }
}
