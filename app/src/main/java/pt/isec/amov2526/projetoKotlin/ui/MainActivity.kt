package pt.isec.amov2526.projetoKotlin.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import pt.isec.amov2526.projetoKotlin.ui.screens.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
//            setShowWhenLocked(true)
//            setTurnScreenOn(true)
//        } else {
//            window.addFlags(
//                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
//                        android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
//            )
//        }

        checkIntentForAlert(intent)

        enableEdgeToEdge()
        setContent {
            // --- Permission Handling in MainActivity ---
            val permissionsToRequest = remember {
                val perms = mutableListOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    perms.add(Manifest.permission.ACTIVITY_RECOGNITION)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                perms
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions(),
                onResult = { /* Handle results if needed */ }
            )

            LaunchedEffect(Unit) {
                permissionLauncher.launch(permissionsToRequest.toTypedArray())
            }

            // --- UI Entry Point ---
            MainScreen()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Atualiza a intenção atual para a nova (que traz o Alerta)
        checkIntentForAlert(intent)
    }

    private fun checkIntentForAlert(intent: Intent?) {
        if (intent?.action == "ACTION_SHOW_ALERT") {
            setShowOnLockScreen(true)
        }
    }

    fun setShowOnLockScreen(show: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(show)
            setTurnScreenOn(show)
        } else {
            if (show) {
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            } else {
                window.clearFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }
        }
    }
}
