package pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.monitors

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import pt.isec.amov2526.projetoKotlin.ui.theme.Emerald100
import pt.isec.amov2526.projetoKotlin.ui.theme.Emerald50
import pt.isec.amov2526.projetoKotlin.ui.theme.Emerald500
import pt.isec.amov2526.projetoKotlin.ui.theme.Indigo100
import pt.isec.amov2526.projetoKotlin.ui.theme.Indigo50
import pt.isec.amov2526.projetoKotlin.ui.theme.Indigo500
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate100
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate400
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate50
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate500
import pt.isec.amov2526.projetoKotlin.ui.theme.Slate900

@Composable
fun AccessSelectionDialog(
    title: String,
    targetName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = Emerald500,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }
                Text(
                    text = "Choose what type of monitoring $targetName can perform.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                )

                AccessOptionCard(
                    title = "Full Monitoring",
                    subtitle = "RECOMMENDED",
                    icon = Icons.Outlined.Shield,
                    color = Emerald500,
                    bgColor = Emerald50,
                    borderColor = Emerald100,
                    onClick = { onConfirm("Full Monitoring") }
                )
                Spacer(modifier = Modifier.height(12.dp))
                AccessOptionCard(
                    title = "Alerts Only",
                    subtitle = "LIMITED ACCESS",
                    icon = Icons.Outlined.Notifications,
                    color = Slate500,
                    bgColor = Slate50,
                    borderColor = Slate100,
                    onClick = { onConfirm("Alerts Only") }
                )
                Spacer(modifier = Modifier.height(12.dp))
                AccessOptionCard(
                    title = "Location Only",
                    subtitle = "LIMITED ACCESS",
                    icon = Icons.Default.LocationOn,
                    color = Indigo500,
                    bgColor = Indigo50,
                    borderColor = Indigo100,
                    onClick = { onConfirm("Location Only") }
                )

                Spacer(modifier = Modifier.height(24.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "Back",
                        color = Slate400,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AccessOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    bgColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
