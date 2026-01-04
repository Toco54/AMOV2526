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

/**
 * Activity única da aplicação (Single Activity Architecture).
 *
 * Serve como contentor para toda a navegação Jetpack Compose.
 * Gere funcionalidades de nível de sistema:
 * 1. Permissões de runtime (Câmara, Localização, Notificações, Sobreposição).
 * 2. Intents de entrada (Deep Links, Alertas de Notificação).
 * 3. Configuração de janela para ecrã de bloqueio (ShowOnLockScreen) durante emergências.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verifica se a Activity foi lançada por um Intent de Alerta (SOS, Queda, etc.).
        // Se sim, configura a janela para aparecer sobre o ecrã de bloqueio.
        checkIntentForAlert(intent)


        // Se não for um alerta de emergência e o utilizador estiver logado,
        // envia um sinal ao serviço para resetar qualquer estado de alerta pendente na UI anterior.
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

            // Definição da lista de permissões críticas requeridas no arranque.
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

            // Trigger inicial de pedido de permissões.
            LaunchedEffect(Unit) {
                permissionLauncher.launch(permissionsToRequest.toTypedArray())
                // Verifica permissão especial de sobreposição (SYSTEM_ALERT_WINDOW)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(
                        context
                    )
                ) {
                    showOverlayPermissionDialog = true
                }
            }

            // Diálogo explicativo para a permissão de sobreposição (obrigatória para Full Screen Intents em background no Android 10+).
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

            // Ponto de entrada da UI Compose.
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

    /**
     * Configura as flags da janela para permitir que a aplicação acorde o dispositivo
     * e apareça sobre o Keyguard (ecrã de bloqueio) em caso de emergência.
     */
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