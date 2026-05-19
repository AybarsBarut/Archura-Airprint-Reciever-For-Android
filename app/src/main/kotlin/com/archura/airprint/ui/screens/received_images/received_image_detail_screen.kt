package com.archura.airprint.ui.screens.received_images

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archura.airprint.domain.model.DocumentFormat
import com.archura.airprint.domain.model.ReceivedImage
import com.archura.airprint.util.formatTimestamp

@Composable
fun ReceivedImageDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReceivedImageDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ReceivedImageDetailContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onCropSquare = viewModel::cropSquare,
        onRotateLeft = viewModel::rotateLeft,
        onRotateRight = viewModel::rotateRight,
        onSaveToGallery = viewModel::saveToGallery,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceivedImageDetailContent(
    uiState: ReceivedImageDetailState,
    onNavigateBack: () -> Unit,
    onCropSquare: () -> Unit,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onSaveToGallery: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Image actions") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text(text = "<")
                    }
                },
            )
        },
    ) { paddingValues ->
        val image = uiState.image
        if (image == null) {
            MissingImage(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )
        } else {
            ImageActions(
                image = image,
                isBusy = uiState.isBusy,
                message = uiState.message,
                onCropSquare = onCropSquare,
                onRotateLeft = onRotateLeft,
                onRotateRight = onRotateRight,
                onSaveToGallery = onSaveToGallery,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )
        }
    }
}

@Composable
private fun ImageActions(
    image: ReceivedImage,
    isBusy: Boolean,
    message: String?,
    onCropSquare: () -> Unit,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onSaveToGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEditable = image.format == DocumentFormat.JPEG && !isBusy

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ImagePreview(image = image)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = image.fileName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${image.format.displayName} - ${formatTimestamp(image.timestampMillis)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onRotateLeft,
                enabled = isEditable,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = "Rotate left")
            }
            OutlinedButton(
                onClick = onRotateRight,
                enabled = isEditable,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = "Rotate right")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onCropSquare,
                enabled = isEditable,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = "Crop square")
            }
            Button(
                onClick = onSaveToGallery,
                enabled = isEditable,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = "Save gallery")
            }
        }

        if (image.format != DocumentFormat.JPEG) {
            Text(
                text = "Only JPEG images can be edited or saved to gallery. Convert incoming PDFs to JPEG in Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun ImagePreview(
    image: ReceivedImage,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember(image.path, image.timestampMillis, image.sizeBytes) {
        if (image.format == DocumentFormat.JPEG) {
            BitmapFactory.decodeFile(image.path)?.asImageBitmap()
        } else {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = image.fileName,
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentScale = ContentScale.Fit,
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = image.format.displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun MissingImage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Image not found",
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
