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

    @SuppressLint("MissingPermission")
    fun startVideoRecording(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        onVideoFinalized: (File) -> Unit
    ): File? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        val file = File(context.cacheDir, "video_${System.currentTimeMillis()}.mp4")
        videoFile = file

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)

        cameraProviderFuture.addListener({
            if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                return@addListener
            }

            try {
                cameraProvider = cameraProviderFuture.get()


                cameraProvider?.unbindAll()

                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(surfaceProvider)

                val qualitySelector = QualitySelector.from(
                    Quality.LOWEST,
                    FallbackStrategy.higherQualityOrLowerThan(Quality.LOWEST)
                )

                val recorder = Recorder.Builder()
                    .setQualitySelector(qualitySelector)
                    .build()

                val videoCapture = VideoCapture.withOutput(recorder)


                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    videoCapture
                )

                val outputOptions = FileOutputOptions.Builder(file)
                    .setDurationLimitMillis(12000)
                    .build()

                activeRecording = videoCapture.output
                    .prepareRecording(context, outputOptions)
                    .withAudioEnabled()
                    .start(executor) { recordEvent ->
                        when(recordEvent) {
                            is VideoRecordEvent.Start -> {
                                recordingStartTime = System.currentTimeMillis()
                                Log.d("EvidenceManager", "Recording effectively started")
                            }
                            is VideoRecordEvent.Finalize -> {
                                if (recordEvent.hasError()) {
                                    val errorMsg = "Video capture failed: code=${recordEvent.error}, cause=${recordEvent.cause?.message}"
                                    Log.e("EvidenceManager", errorMsg)
                                    
                                    
                                } else {
                                    Log.d("EvidenceManager", "Recording finalize event received successfully")
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

    suspend fun uploadFile(file: File, folder: String): String? {
        if (!file.exists()) {
            Log.e("EvidenceManager", "Upload failed: File does not exist -> ${file.absolutePath}")
            return null
        }
        if (file.length() == 0L) {
            Log.e("EvidenceManager", "Upload failed: File is empty (0 bytes). Recording likely failed.")
            return null
        }

        Log.d("EvidenceManager", "Starting upload: ${file.name} (${file.length()} bytes) to $folder")
        
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