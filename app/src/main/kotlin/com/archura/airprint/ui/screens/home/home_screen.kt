package com.archura.airprint.ui.screens.home

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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    HomeContent(
        uiState = uiState,
        onToggleReceiver = viewModel::setReceiverEnabled,
        onDeleteImage = viewModel::deleteImage,
        onOpenImage = onOpenImage,
        onOpenSettings = onOpenSettings,
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
