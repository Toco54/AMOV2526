package pt.isec.amov2526.projetoKotlin.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import pt.isec.amov2526.projetoKotlin.ui.screens.monitor.MonitorMainScreen
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.ProtectedMainScreen
import pt.isec.amov2526.projetoKotlin.ui.theme.ProjetoKotlinTheme

enum class AppRole { PROTECTED, MONITOR }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProjetoKotlinTheme {
                var currentRole by remember { mutableStateOf(AppRole.PROTECTED) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentRole) {
                        AppRole.PROTECTED -> ProtectedMainScreen(
                            onLogout = { currentRole = AppRole.MONITOR }
                        )
                        AppRole.MONITOR -> MonitorMainScreen(
                            onLogout = { currentRole = AppRole.PROTECTED }
                        )
                    }
                }
            }
        }
    }
}