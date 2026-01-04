package pt.isec.amov2526.projetoKotlin.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import pt.isec.amov2526.projetoKotlin.R
import pt.isec.amov2526.projetoKotlin.ui.screens.MainScreen
import pt.isec.amov2526.projetoKotlin.services.SafetyMonitorService
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)











        checkIntentForAlert(intent)




        if (intent.action != "ACTION_SHOW_ALERT" && FirebaseAuth.getInstance().currentUser != null) {
            val resetIntent = Intent(
                this,
                SafetyMonitorService::class.java
            ).apply {
                action = SafetyMonitorService.ACTION_UI_RESET
            }
            startService(resetIntent)
        }

        enableEdgeToEdge()
        setContent {

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
                onResult = { }
            )

            val context = LocalContext.current
            var showOverlayPermissionDialog by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                permissionLauncher.launch(permissionsToRequest.toTypedArray())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(
                        context
                    )
                ) {
                    showOverlayPermissionDialog = true
                }
            }

            if (showOverlayPermissionDialog) {
                AlertDialog(
                    onDismissRequest = { showOverlayPermissionDialog = false },
                    title = { Text(stringResource(id = R.string.dialog_perm_title)) },
                    text = { Text(stringResource(id = R.string.dialog_perm_desc)) },
                    confirmButton = {
                        Button(onClick = {
                            showOverlayPermissionDialog = false
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }) {
                            Text(stringResource(id = R.string.dialog_perm_confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showOverlayPermissionDialog = false }) {
                            Text(stringResource(id = R.string.dialog_perm_dismiss))
                        }
                    }
                )
            }


            MainScreen()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
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
