package pt.isec.amov2526.projetoKotlin.ui.screens.monitor.components.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import pt.isec.amov2526.projetoKotlin.R
import pt.isec.amov2526.projetoKotlin.data.model.AlertStat

import pt.isec.amov2526.projetoKotlin.ui.theme.*

@Composable
fun AlertTrendsCard(stats: List<AlertStat>) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.ShowChart, null, tint = Slate400, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(id = R.string.monitor_alert_trends), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate900)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth().height(160.dp), verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    listOf("4", "3", "2", "1", "0").forEach { label -> Text(label, style = MaterialTheme.typography.labelSmall, color = Slate400, fontSize = 10.sp) }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        repeat(5) { Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Slate100)) }
                    }
                    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.Bottom) {
                        stats.forEach { stat ->
                            val barHeightFactor = stat.count / 4f
                            Box(modifier = Modifier.width(16.dp).fillMaxHeight(barHeightFactor.coerceAtLeast(0.02f)).background(Emerald500, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(start = 18.dp, top = 8.dp), horizontalArrangement = Arrangement.SpaceAround) {
                stats.forEach { stat -> Text(stat.label, style = MaterialTheme.typography.labelSmall, color = Slate500, textAlign = TextAlign.Center, fontSize = 9.sp, modifier = Modifier.width(36.dp)) }
            }
        }
    }
}
