package pt.isec.amov2526.projetoKotlin.ui.screens.monitor.components.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.isec.amov2526.projetoKotlin.data.model.ConnectedUser

import pt.isec.amov2526.projetoKotlin.ui.theme.*

@Composable
fun ProtegeCard(
    protege: ConnectedUser,
    onClick: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val isRevoked = protege.revokedByProtege
    
    Card(
        colors = CardDefaults.cardColors(containerColor = if(isRevoked) Red50 else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = !isRevoked) { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(if(isRevoked) Red600.copy(alpha=0.1f) else Slate100, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isRevoked) {
                    Icon(Icons.Default.LinkOff, null, tint = Red600)
                } else {
                    Text(
                        text = if (protege.name.isNotEmpty()) protege.name.first().toString() else "?",
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = protege.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if(isRevoked) Slate400 else Slate900
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if(isRevoked) Red600 else Emerald500, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRevoked) "Connection Revoked" else "Active Monitoring",
                        style = MaterialTheme.typography.bodySmall,
                        color = if(isRevoked) Red600 else Slate500
                    )
                }
            }
            
            if (isRevoked) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Remove", tint = Red600)
                }
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = Slate200,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = Slate400,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}
