package com.example.beadmaker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.beadmaker.R

private const val DialogSpacing = 8
private const val DialogWidthFraction = 0.95f
private val DialogControlShape = RoundedCornerShape(14.dp)
private val PaletteChipShape = RoundedCornerShape(12.dp)
private val PaletteChipInsetShape = RoundedCornerShape(8.dp)

@Composable
internal fun FileActionsDialog(
    isOperationInProgress: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onExport: () -> Unit
) {
    AlertDialog(
        modifier = Modifier.fillMaxWidth(DialogWidthFraction),
        onDismissRequest = {
            if (!isOperationInProgress) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(text = stringResource(R.string.file_actions)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(DialogSpacing.dp)
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isOperationInProgress,
                    onClick = onSave,
                    shape = DialogControlShape
                ) {
                    Text(stringResource(R.string.save))
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isOperationInProgress,
                    onClick = onLoad,
                    shape = DialogControlShape
                ) {
                    Text(stringResource(R.string.load))
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isOperationInProgress,
                    onClick = onExport,
                    shape = DialogControlShape
                ) {
                    Text(stringResource(R.string.export))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                enabled = !isOperationInProgress,
                onClick = onDismiss
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
internal fun PalettePickerDialog(
    colors: List<Color>,
    selectedColorIndex: Int,
    recentColorIndices: List<Int>,
    onDismiss: () -> Unit,
    onApply: (Int) -> Unit
) {
    var pendingSelection by rememberSaveable(selectedColorIndex) {
        mutableIntStateOf(selectedColorIndex)
    }

    AlertDialog(
        modifier = Modifier.fillMaxWidth(DialogWidthFraction),
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(stringResource(R.string.select_color)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(DialogSpacing.dp)
            ) {
                Text(
                    text = stringResource(R.string.select_color_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(DialogSpacing.dp)
                ) {
                    colors.chunked(6).forEachIndexed { rowIndex, rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(DialogSpacing.dp)
                        ) {
                            rowColors.forEachIndexed { columnIndex, color ->
                                val colorIndex = rowIndex * 6 + columnIndex
                                PaletteColorChip(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f),
                                    color = color,
                                    selected = pendingSelection == colorIndex,
                                    onClick = { pendingSelection = colorIndex }
                                )
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.recent_colors),
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DialogSpacing.dp)
                ) {
                    repeat(6) { slot ->
                        val colorIndex = recentColorIndices.getOrNull(slot)
                        val color = colorIndex?.let(colors::getOrNull)
                        if (color != null) {
                            PaletteColorChip(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                                color = color,
                                selected = pendingSelection == colorIndex,
                                onClick = { pendingSelection = colorIndex }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(PaletteChipShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                        shape = PaletteChipShape
                                    )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(pendingSelection) }) {
                Text(stringResource(R.string.apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun PaletteColorChip(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(PaletteChipShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(
                width = if (selected) 4.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = PaletteChipShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
        ) {
            val radius = size.minDimension * 0.42f
            drawCircle(color = color, radius = radius)
            drawCircle(
                color = Color.Black.copy(alpha = 0.15f),
                radius = radius,
                style = Stroke(width = 1.2.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = radius * 0.4f,
                center = center.copy(
                    x = center.x - radius * 0.3f,
                    y = center.y - radius * 0.3f
                )
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.surface,
                        shape = PaletteChipInsetShape
                    )
            )
        }
    }
}
