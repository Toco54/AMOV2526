package pt.isec.amov2526.projetoKotlin.ui.screens.monitor.components.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import pt.isec.amov2526.projetoKotlin.ui.theme.*

@Composable
fun ProfileMenuOverlay(
    name: String,
    email: String,
    onDismiss: () -> Unit,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
    onSwitchMode: () -> Unit,
    canSwitchProfile: Boolean = false
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        modifier = Modifier
            .width(320.dp)
            .border(1.dp, Slate100, RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = Slate500
            )
            Text(
                text = "Monitor Account",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate500
            )
            Spacer(modifier = Modifier.height(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuOptionButton(
                    icon = Icons.Outlined.ManageAccounts,
                    text = "Edit Profile",
                    onClick = onEditProfile
                )


                MenuOptionButton(
                    icon = Icons.Default.SwapHoriz,
                    text = "Switch to Protected Mode",
                    onClick = onSwitchMode
                )

                MenuOptionButton(
                    icon = Icons.Outlined.Settings,
                    text = "App Settings",
                    onClick = { }
                )
                MenuOptionButton(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    text = "Logout",
                    textColor = Red600,
                    iconColor = Red600,
                    isDestructive = true,
                    onClick = onLogout
                )
            }
        }
    }
}

@Composable
fun MenuOptionButton(
    icon: ImageVector,
    text: String,
    textColor: Color = Slate900,
    iconColor: Color = Slate500,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isDestructive) Red50 else Color.White,
            contentColor = if (isDestructive) Red600 else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    fontSize = 14.sp
                )
            }
            if (!isDestructive) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Slate200
                )
            }
        }
    }
}