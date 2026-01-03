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



@Composable
fun ActivityScreen(viewModel: pt.isec.amov2526.projetoKotlin.ui.viewmodels.ProtectedMainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    var selectedActivity by remember { mutableStateOf<ActivityItem?>(null) }

    val activities = viewModel.activities

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

        if (activities.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No alerts recorded yet.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // [ALTERADO] Usamos 'activities' em vez de 'mockActivities'
                items(activities) { activity ->
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
    }

    if (selectedActivity != null) {
        AlertDetailModal(
            activity = selectedActivity!!,
            onDismiss = { selectedActivity = null }
        )
    }
}
