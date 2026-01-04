package pt.isec.amov2526.projetoKotlin.ui.screens.protected

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pt.isec.amov2526.projetoKotlin.R
import pt.isec.amov2526.projetoKotlin.data.model.ActivityItem
import pt.isec.amov2526.projetoKotlin.data.model.MonitorRule
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.activity.ActivityCard
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.activity.AlertDetailModal
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate50
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate900

@Composable
fun ActivityScreen(
    viewModel: pt.isec.amov2526.projetoKotlin.ui.viewmodels.ProtectedMainViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    activeGeofences: List<MonitorRule> = emptyList()
) {
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
            text = stringResource(id = R.string.history_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Slate900,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (activities.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(stringResource(id = R.string.history_no_alerts), color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {

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
            onDismiss = { selectedActivity = null },
            activeGeofences = activeGeofences
        )
    }
}