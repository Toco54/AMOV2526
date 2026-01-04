package pt.isec.amov2526.projetoKotlin.ui.screens.protected

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.isec.amov2526.projetoKotlin.R
import pt.isec.amov2526.projetoKotlin.data.model.ConnectedUser
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.monitors.GenerateCodeModal
import pt.isec.amov2526.projetoKotlin.ui.screens.protected.components.monitors.MonitorCard
import pt.isec.amov2526.projetoKotlin.ui.viewmodels.ProtectedMainViewModel

// Cores
private val Slate50 = Color(0xFFF8FAFC)
private val Slate400 = Color(0xFF94A3B8)
private val Slate500 = Color(0xFF64748B)
private val Slate900 = Color(0xFF0F172A)
private val Indigo600 = Color(0xFF4F46E5)
private val Red50 = Color(0xFFFEF2F2)
private val Red600 = Color(0xFFDC2626)

@Composable
fun MonitorsScreen(
    viewModel: ProtectedMainViewModel = viewModel()
) {
    val monitors = viewModel.monitors
    var showRemoveDialog by remember { mutableStateOf<ConnectedUser?>(null) }
    var showGenerateCodeModal by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.monitors_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // ACTIVE MONITORS
            if (monitors.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(id = R.string.monitors_active),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                items(monitors) { monitor ->
                    MonitorCard(
                        monitor = monitor,
                        onRemove = { showRemoveDialog = monitor }
                    )
                }
            } else {
                item {
                    Text(
                        text = stringResource(id = R.string.monitors_no_active),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate400,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        FloatingActionButton(
            onClick = {
                viewModel.generateCode("Full Monitoring")
                showGenerateCodeModal = true
            },
            containerColor = Indigo600,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(64.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(id = R.string.monitors_add),
                modifier = Modifier.size(28.dp)
            )
        }
    }

    // --- DIALOGS ---
    if (showRemoveDialog != null) {
        Dialog(onDismissRequest = { showRemoveDialog = null }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Red50, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonRemove,
                            contentDescription = null,
                            tint = Red600,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(id = R.string.monitors_remove_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.monitors_remove_desc, showRemoveDialog!!.name),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate500,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showRemoveDialog = null }) {
                            Text(
                                text = stringResource(id = R.string.action_cancel),
                                fontWeight = FontWeight.Bold,
                                color = Slate500
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(Red600, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.removeMonitor(showRemoveDialog!!.uid)
                                    showRemoveDialog = null
                                }
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                                .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp))
                        ) {
                            Text(
                                text = stringResource(id = R.string.action_remove),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showGenerateCodeModal) {
        GenerateCodeModal(
            viewModel = viewModel,
            onDismiss = { showGenerateCodeModal = false }
        )
    }
}
