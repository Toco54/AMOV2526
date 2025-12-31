package pt.isec.amov2526.projetoKotlin.ui.screens.protected

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pt.isec.amov2526.projetoKotlin.data.model.*
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.activity.ActivityCard
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.activity.AlertDetailModal

// --- CORES ---
private val Slate50 = Color(0xFFF8FAFC)
private val Slate900 = Color(0xFF0F172A)

private val mockActivities = listOf(
    ActivityItem(
        "1",
        ActivityAlertType.SPEED,
        "Speed Alert",
        "23/12/2025",
        "21:36",
        ActivityAlertStatus.CONFIRMED,
        listOf(
            MonitorAction("Alice Monitor", MonitorActionState.CALLED_USER, "21:37"),
            MonitorAction("Bob Supervisor", MonitorActionState.VIEWED, "21:38")
        )
    ),
    ActivityItem(
        "2",
        ActivityAlertType.FALL,
        "Fall Detected",
        "22/12/2025",
        "14:20",
        ActivityAlertStatus.CANCELLED,
        listOf(
            MonitorAction("Alice Monitor", MonitorActionState.MARKED_SAFE, "14:22")
        )
    ),
    ActivityItem(
        "3",
        ActivityAlertType.GEOFENCE,
        "Safe Zone Exit",
        "20/12/2025",
        "09:15",
        ActivityAlertStatus.CONFIRMED,
        listOf(
            MonitorAction("Alice Monitor", MonitorActionState.ON_THE_WAY, "09:20"),
            MonitorAction("Charlie Guard", MonitorActionState.SENT, "09:15")
        )
    ),
    ActivityItem(
        "4",
        ActivityAlertType.PANIC,
        "SOS Activated",
        "18/12/2025",
        "23:45",
        ActivityAlertStatus.PENDING,
        listOf(
            MonitorAction("Alice Monitor", MonitorActionState.CALLED_112, "23:46"),
            MonitorAction("Bob Supervisor", MonitorActionState.ON_THE_WAY, "23:50"),
            MonitorAction("Charlie Guard", MonitorActionState.VIEWED, "23:47")
        )
    )
)

@Composable
fun ActivityScreen() {
    var selectedActivity by remember { mutableStateOf<ActivityItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Alert History",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Slate900,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(mockActivities) { activity ->
                ActivityCard(
                    activity = activity,
                    onClick = { selectedActivity = activity }
                )
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (selectedActivity != null) {
        AlertDetailModal(
            activity = selectedActivity!!,
            onDismiss = { selectedActivity = null }
        )
    }
}
