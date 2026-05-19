package com.archura.airprint.ui.screens.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archura.airprint.ui.components.ReceivedImageCard
import com.archura.airprint.ui.components.StatusIndicator

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenImage: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingScanJobId by viewModel.scanRequestManager.pendingJobId.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Tracks whether the "iOS is requesting a scan" dialog is showing
    var showScanDialog by remember { mutableStateOf(false) }
    var activeScanJobId by remember { mutableStateOf<String?>(null) }

    // Launcher for the scan-triggered file picker
    val scanFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        val jobId = activeScanJobId
        if (jobId != null) {
            if (uri != null) {
                viewModel.fulfillScanRequest(jobId, uri, context.contentResolver)
            } else {
                viewModel.cancelScanRequest(jobId)
            }
        }
        activeScanJobId = null
        showScanDialog = false
    }

    // Launcher for the manual "Share File with iOS" button
    val manualFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) viewModel.importFile(uri)
    }

    // Watch for incoming iOS scan requests
    LaunchedEffect(pendingScanJobId) {
        val jobId = pendingScanJobId
        if (jobId != null) {
            activeScanJobId = jobId
            showScanDialog = true
        }
    }

    // Dialog shown when iOS presses "Scan"
    if (showScanDialog && activeScanJobId != null) {
        AlertDialog(
            onDismissRequest = {
                activeScanJobId?.let { viewModel.cancelScanRequest(it) }
                activeScanJobId = null
                showScanDialog = false
            },
            title = { Text("📲 iOS tarama isteği") },
            text = {
                Text(
                    "iPhone/iPad, cihazınızdan bir dosya taramak istiyor.\n\n" +
                        "İletmek istediğiniz görsel veya PDF'i seçin.",
                )
            },
            confirmButton = {
                Button(onClick = { scanFilePickerLauncher.launch("*/*") }) {
                    Text("Dosya Seç")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    activeScanJobId?.let { viewModel.cancelScanRequest(it) }
                    activeScanJobId = null
                    showScanDialog = false
                }) {
                    Text("İptal")
                }
            },
        )
    }

    HomeContent(
        uiState = uiState,
        onToggleReceiver = viewModel::setReceiverEnabled,
        onDeleteImage = viewModel::deleteImage,
        onOpenImage = onOpenImage,
        onOpenSettings = onOpenSettings,
        onManualImport = { manualFilePickerLauncher.launch("*/*") },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    uiState: HomeScreenState,
    onToggleReceiver: (Boolean) -> Unit,
    onDeleteImage: (String) -> Unit,
    onOpenImage: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onManualImport: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "AirPrint Receiver") },
                actions = {
                    TextButton(onClick = onOpenSettings) {
                        Text(text = "Settings")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Local receiver",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    StatusIndicator(
                        enabled = uiState.printerStatus.enabled,
                        detail = uiState.printerStatus.statusMessage,
                    )
                }
                Switch(
                    checked = uiState.printerStatus.enabled,
                    onCheckedChange = onToggleReceiver,
                )
            }

            OutlinedButton(
                onClick = onManualImport,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "📁 iOS'a Dosya Gönder (Manuel)")
            }

            if (uiState.images.isEmpty()) {
                EmptyGallery(
                    receiverEnabled = uiState.printerStatus.enabled,
                    onToggleReceiver = onToggleReceiver,
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 148.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        items = uiState.images,
                        key = { image -> image.id },
                    ) { image ->
                        ReceivedImageCard(
                            image = image,
                            onDelete = { onDeleteImage(image.id) },
                            onOpen = { onOpenImage(image.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyGallery(
    receiverEnabled: Boolean,
    onToggleReceiver: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "No received print jobs yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = if (receiverEnabled) {
                "Choose this Android device from iOS or macOS Print."
            } else {
                "Enable receiver mode to publish the local AirPrint endpoint."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!receiverEnabled) {
            Button(onClick = { onToggleReceiver(true) }) {
                Text(text = "Enable receiver")
            }
        }
    }
}
