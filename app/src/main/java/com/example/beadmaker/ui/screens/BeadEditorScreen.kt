package com.example.beadmaker.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixOff
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.beadmaker.R
import com.example.beadmaker.ui.components.BeadGrid
import com.example.beadmaker.ui.model.BeadShape
import com.example.beadmaker.ui.model.StitchMode
import com.example.beadmaker.ui.state.InteractionModeGrid
import com.example.beadmaker.ui.state.InteractionModePaint
import com.example.beadmaker.ui.state.InteractionModeTemplate
import com.example.beadmaker.ui.state.MaxGridSize
import com.example.beadmaker.ui.state.MinGridSize
import com.example.beadmaker.ui.state.MinTemplateOpacity
import com.example.beadmaker.ui.state.createTemplateCaptureUri
import com.example.beadmaker.ui.state.rememberBeadEditorState
import coil.compose.AsyncImage

private const val SpaceXs = 8
private const val SpaceSm = 12
private const val SpaceMd = 16

private val ControlShape = RoundedCornerShape(14.dp)
private val PaletteChipShape = RoundedCornerShape(12.dp)
private val PaletteChipInsetShape = RoundedCornerShape(8.dp)
private val GridFrameShape = RoundedCornerShape(24.dp)
private val GridInnerFrameShape = RoundedCornerShape(20.dp)

private val BasicPaletteColorValues = listOf(
    0xFF000000.toInt(),
    0xFFFFFFFF.toInt(),
    0xFF9E9E9E.toInt(),
    0xFF616161.toInt(),
    0xFFF44336.toInt(),
    0xFFE91E63.toInt(),
    0xFFFF9800.toInt(),
    0xFFFFC107.toInt(),
    0xFFFFEB3B.toInt(),
    0xFFCDDC39.toInt(),
    0xFF8BC34A.toInt(),
    0xFF4CAF50.toInt(),
    0xFF009688.toInt(),
    0xFF00BCD4.toInt(),
    0xFF03A9F4.toInt(),
    0xFF2196F3.toInt(),
    0xFF3F51B5.toInt(),
    0xFF673AB7.toInt(),
    0xFF9C27B0.toInt(),
    0xFF795548.toInt(),
    0xFFA1887F.toInt(),
    0xFF607D8B.toInt(),
    0xFFFF5722.toInt(),
    0xFF827717.toInt()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeadEditorScreen() {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val editorState = rememberBeadEditorState()
    val uiState = editorState.uiState
    val paletteColors = BasicPaletteColorValues.map { Color(it) }

    val gridColumns = uiState.gridColumns
    val gridRows = uiState.gridRows
    val beads = uiState.beads
    val templateImageUri = uiState.templateImageUriString?.let(Uri::parse)
    val stitchMode = StitchMode.fromId(uiState.stitchModeId)
    val beadShape = BeadShape.fromId(uiState.beadShapeId)
    val currentColorIndex = uiState.selectedColorIndex.takeIf { it in paletteColors.indices } ?: 0
    val currentColor = paletteColors[currentColorIndex]
    val templateAdjustMode = uiState.interactionMode == InteractionModeTemplate
    val boardAdjustMode = uiState.interactionMode == InteractionModeGrid
    val templatePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            editorState.importTemplateFromPicker(uri)
        }
    }
    val cameraTemplateCapture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        editorState.finishCameraTemplateCapture(success)
    }
    val exportPatternLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            val exportSucceeded = editorState.exportPatternToUri(uri)
            Toast.makeText(
                context,
                if (exportSucceeded) {
                    context.getString(R.string.pattern_exported)
                } else {
                    context.getString(R.string.pattern_export_failed)
                },
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    var showFileActionsDialog by rememberSaveable { mutableStateOf(false) }
    val modeStatusText = when {
        templateAdjustMode -> stringResource(R.string.status_template_adjust)
        boardAdjustMode -> stringResource(R.string.status_grid_adjust)
        uiState.eraserSelected -> stringResource(R.string.status_eraser)
        else -> stringResource(R.string.status_paint)
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SpaceMd.dp, vertical = SpaceMd.dp),
            verticalArrangement = Arrangement.spacedBy(SpaceMd.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SpaceSm.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            color = if (uiState.eraserSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
                            }
                        )
                        .border(
                            width = if (uiState.eraserSelected) 2.dp else 1.dp,
                            color = if (uiState.eraserSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            shape = CircleShape
                        ),
                    onClick = editorState::toggleEraser,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoFixOff,
                        contentDescription = stringResource(R.string.eraser),
                        tint = if (uiState.eraserSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                IconButton(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(currentColor)
                        .border(
                            width = if (uiState.eraserSelected) 1.dp else 2.dp,
                            color = if (uiState.eraserSelected) {
                                MaterialTheme.colorScheme.outlineVariant
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            shape = CircleShape
                        ),
                    onClick = editorState::showColorPicker,
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val radius = size.minDimension / 2.2f
                        // Dark rim
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.15f),
                            radius = radius,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                        )
                        // Small highlight
                        drawCircle(
                            color = Color.White.copy(alpha = 0.25f),
                            radius = radius * 0.4f,
                            center = center.copy(x = center.x - radius * 0.3f, y = center.y - radius * 0.3f)
                        )
                    }
                }

                IconButton(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        ),
                    onClick = editorState::undo,
                    enabled = editorState.canUndo
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Undo,
                        contentDescription = stringResource(R.string.undo)
                    )
                }

                IconButton(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        ),
                    onClick = editorState::redo,
                    enabled = editorState.canRedo
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Redo,
                        contentDescription = stringResource(R.string.redo)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        ),
                    onClick = { showFileActionsDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Save,
                        contentDescription = stringResource(R.string.file_actions)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SpaceXs.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModeIconButton(
                    modifier = Modifier.weight(1f),
                    selected = uiState.interactionMode == InteractionModePaint,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.mode_paint)
                        )
                    },
                    onClick = editorState::setPaintMode
                )

                ModeIconButton(
                    modifier = Modifier.weight(1f),
                    selected = uiState.interactionMode == InteractionModeTemplate,
                    enabled = templateImageUri != null,
                    selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                    selectedContentColor = MaterialTheme.colorScheme.onTertiary,
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = stringResource(R.string.mode_template)
                        )
                    },
                    onClick = editorState::toggleTemplateMode
                )

                ModeIconButton(
                    modifier = Modifier.weight(1f),
                    selected = uiState.interactionMode == InteractionModeGrid,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.GridOn,
                            contentDescription = stringResource(R.string.mode_grid)
                        )
                    },
                    onClick = editorState::toggleGridMode
                )
            }

            Text(
                text = modeStatusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        R.string.grid_status,
                        stringResource(stitchMode.labelRes),
                        gridColumns,
                        gridRows
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = { editorState.openToolsDialogAtTab(0) }
                ) {
                    Text(stringResource(R.string.tools))
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(gridColumns.toFloat() / gridRows.toFloat())
                    .shadow(
                        elevation = 8.dp,
                        shape = GridFrameShape,
                        ambientColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.22f),
                        spotColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.18f)
                    )
                    .clip(GridFrameShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (!isDark) 0.58f else 0.85f))
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = if (!isDark) 0.72f else 0.4f),
                        shape = GridFrameShape
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                        .clip(GridInnerFrameShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = if (!isDark) 0.12f else 0.3f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (!isDark) 0.55f else 0.2f),
                            shape = GridInnerFrameShape
                        )
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = uiState.boardScale
                            scaleY = uiState.boardScale
                            translationX = uiState.boardOffsetX
                            translationY = uiState.boardOffsetY
                        }
                ) {
                    if (templateImageUri != null) {
                        AsyncImage(
                            model = templateImageUri,
                            contentDescription = stringResource(R.string.template_image),
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(uiState.templateOpacity)
                                .graphicsLayer {
                                    scaleX = uiState.templateScale
                                    scaleY = uiState.templateScale
                                    translationX = uiState.templateOffsetX
                                    translationY = uiState.templateOffsetY
                                },
                            contentScale = ContentScale.Fit
                        )
                    }

                    if (templateImageUri != null && templateAdjustMode) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        editorState.updateTemplateTransform(
                                            panX = pan.x,
                                            panY = pan.y,
                                            zoom = zoom
                                        )
                                    }
                                }
                        )
                    }

                    BeadGrid(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(SpaceSm.dp),
                        beads = beads,
                        colors = paletteColors,
                        stitchMode = stitchMode,
                        beadShape = beadShape,
                        columns = gridColumns,
                        onCellTap = editorState::paintCell
                    )
                }

                if (boardAdjustMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    editorState.updateBoardTransform(
                                        panX = pan.x,
                                        panY = pan.y,
                                        zoom = zoom
                                    )
                                }
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(SpaceXs.dp))
        }
    }

    if (uiState.showColorPickerDialog) {
        PalettePickerDialog(
            colors = paletteColors,
            selectedColorIndex = currentColorIndex,
            onDismiss = editorState::dismissColorPicker,
            onApply = editorState::applySelectedColor
        )
    }

    if (uiState.showToolsDialog) {
        AlertDialog(
            onDismissRequest = editorState::dismissToolsDialog,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            ),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = stringResource(R.string.tools))
                    IconButton(
                        onClick = editorState::dismissToolsDialog
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.close)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(SpaceSm.dp)
                ) {
                    TabRow(selectedTabIndex = uiState.selectedToolsTab) {
                        Tab(
                            selected = uiState.selectedToolsTab == 0,
                            onClick = { editorState.selectToolsTab(0) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Image,
                                    contentDescription = stringResource(R.string.template)
                                )
                            }
                        )
                        Tab(
                            selected = uiState.selectedToolsTab == 1,
                            onClick = { editorState.selectToolsTab(1) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = stringResource(R.string.editor)
                                )
                            }
                        )
                    }

                    when (uiState.selectedToolsTab) {
                        0 -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(SpaceXs.dp)
                            ) {
                                Button(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        templatePicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                        editorState.dismissToolsDialog()
                                    },
                                    shape = ControlShape
                                ) {
                                    Text(stringResource(R.string.import_template))
                                }
                                Button(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val captureUri = createTemplateCaptureUri(context)
                                        if (captureUri != null) {
                                            editorState.prepareCameraTemplateCapture(captureUri.toString())
                                            cameraTemplateCapture.launch(captureUri)
                                            editorState.dismissToolsDialog()
                                        }
                                    },
                                    shape = ControlShape
                                ) {
                                    Text(stringResource(R.string.take_photo))
                                }
                            }
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = editorState::removeTemplateImage,
                                enabled = templateImageUri != null,
                                shape = ControlShape
                            ) {
                                Text(stringResource(R.string.remove_template))
                            }

                            if (templateImageUri != null) {
                                Text(
                                    text = stringResource(R.string.opacity),
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Slider(
                                    value = uiState.templateOpacity,
                                    onValueChange = editorState::updateTemplateOpacity,
                                    valueRange = MinTemplateOpacity..1f
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(R.string.template_opacity_min),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.current_template_opacity,
                                            uiState.templateOpacity
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(R.string.template_opacity_max),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(SpaceXs.dp)
                                ) {
                                    OutlinedButton(
                                        modifier = Modifier.weight(1f),
                                        onClick = editorState::toggleTemplateMode,
                                        shape = ControlShape
                                    ) {
                                        Text(
                                            if (templateAdjustMode) {
                                                stringResource(R.string.adjust_on)
                                            } else {
                                                stringResource(R.string.adjust_template)
                                            }
                                        )
                                    }
                                    OutlinedButton(
                                        modifier = Modifier.weight(1f),
                                        onClick = editorState::resetTemplateTransform,
                                        shape = ControlShape
                                    ) {
                                        Text(stringResource(R.string.reset))
                                    }
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.import_template_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        1 -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(SpaceXs.dp)
                            ) {
                                OutlinedButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = editorState::clearGrid,
                                    enabled = !editorState.isGridEmpty,
                                    shape = ControlShape
                                ) {
                                    Text(stringResource(R.string.clear_grid))
                                }
                                OutlinedButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = editorState::resetBoardTransform,
                                    shape = ControlShape
                                ) {
                                    Text(stringResource(R.string.reset))
                                }
                            }

                            Text(
                                text = stringResource(R.string.stitch_type),
                                style = MaterialTheme.typography.titleSmall
                            )

                            var stitchDropdownExpanded by rememberSaveable { mutableStateOf(false) }
                            val selectedStitchMode = StitchMode.fromId(uiState.pendingSettingsStitchId)

                            ExposedDropdownMenuBox(
                                expanded = stitchDropdownExpanded,
                                onExpandedChange = { stitchDropdownExpanded = it },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = stringResource(selectedStitchMode.labelRes),
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stitchDropdownExpanded) },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth(),
                                    shape = ControlShape
                                )

                                ExposedDropdownMenu(
                                    expanded = stitchDropdownExpanded,
                                    onDismissRequest = { stitchDropdownExpanded = false }
                                ) {
                                    StitchMode.entries.forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = stringResource(option.labelRes),
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                            },
                                            onClick = {
                                                editorState.updatePendingStitch(option.id)
                                                stitchDropdownExpanded = false
                                            },
                                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                        )
                                    }
                                }
                            }

                            Text(
                                text = stringResource(R.string.bead_shape),
                                style = MaterialTheme.typography.titleSmall
                            )

                            var shapeDropdownExpanded by rememberSaveable { mutableStateOf(false) }
                            val selectedBeadShape = BeadShape.fromId(uiState.pendingSettingsBeadShapeId)

                            ExposedDropdownMenuBox(
                                expanded = shapeDropdownExpanded,
                                onExpandedChange = { shapeDropdownExpanded = it },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = stringResource(selectedBeadShape.labelRes),
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = shapeDropdownExpanded) },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth(),
                                    shape = ControlShape
                                )

                                ExposedDropdownMenu(
                                    expanded = shapeDropdownExpanded,
                                    onDismissRequest = { shapeDropdownExpanded = false }
                                ) {
                                    BeadShape.entries.forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = stringResource(option.labelRes),
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                            },
                                            onClick = {
                                                editorState.updatePendingBeadShape(option.id)
                                                shapeDropdownExpanded = false
                                            },
                                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                        )
                                    }
                                }
                            }

                            Text(
                                text = stringResource(
                                    R.string.grid_size,
                                    uiState.pendingSettingsGridColumns.toInt(),
                                    uiState.pendingSettingsGridRows.toInt()
                                ),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(stringResource(R.string.width))
                            Slider(
                                value = uiState.pendingSettingsGridColumns,
                                onValueChange = editorState::updatePendingGridColumns,
                                valueRange = MinGridSize.toFloat()..MaxGridSize.toFloat(),
                                steps = MaxGridSize - MinGridSize - 1
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "$MinGridSize",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(
                                        R.string.current_value,
                                        uiState.pendingSettingsGridColumns.toInt()
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$MaxGridSize",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(stringResource(R.string.height))
                            Slider(
                                value = uiState.pendingSettingsGridRows,
                                onValueChange = editorState::updatePendingGridRows,
                                valueRange = MinGridSize.toFloat()..MaxGridSize.toFloat(),
                                steps = MaxGridSize - MinGridSize - 1
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "$MinGridSize",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(
                                        R.string.current_value,
                                        uiState.pendingSettingsGridRows.toInt()
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$MaxGridSize",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (uiState.selectedToolsTab == 1) {
                    TextButton(
                        onClick = editorState::applyPendingGridSettings
                    ) {
                        Text(stringResource(R.string.apply))
                    }
                } else {
                    TextButton(onClick = editorState::dismissToolsDialog) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            },
            dismissButton = {
                if (uiState.selectedToolsTab == 1) {
                    TextButton(onClick = editorState::dismissToolsDialog) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        )
    }

    if (showFileActionsDialog) {
        AlertDialog(
            onDismissRequest = { showFileActionsDialog = false },
            title = { Text(text = stringResource(R.string.file_actions)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(SpaceXs.dp)
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val saveSucceeded = editorState.savePattern()
                            Toast.makeText(
                                context,
                                if (saveSucceeded) {
                                    context.getString(R.string.pattern_saved)
                                } else {
                                    context.getString(R.string.pattern_save_failed)
                                },
                                Toast.LENGTH_SHORT
                            ).show()
                            showFileActionsDialog = false
                        },
                        shape = ControlShape
                    ) {
                        Text(stringResource(R.string.save))
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val loadSucceeded = editorState.loadSavedPattern()
                            Toast.makeText(
                                context,
                                if (loadSucceeded) {
                                    context.getString(R.string.pattern_loaded)
                                } else {
                                    context.getString(R.string.pattern_load_failed)
                                },
                                Toast.LENGTH_SHORT
                            ).show()
                            showFileActionsDialog = false
                        },
                        shape = ControlShape
                    ) {
                        Text(stringResource(R.string.load))
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            showFileActionsDialog = false
                            exportPatternLauncher.launch(editorState.suggestedExportFileName())
                        },
                        shape = ControlShape
                    ) {
                        Text(stringResource(R.string.export))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showFileActionsDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ModeIconButton(
    modifier: Modifier = Modifier,
    selected: Boolean,
    enabled: Boolean = true,
    selectedContainerColor: Color = MaterialTheme.colorScheme.primary,
    selectedContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = ControlShape,
        color = if (selected) {
            selectedContainerColor
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isSystemInDarkTheme()) 0.4f else 0.7f)
        },
        contentColor = if (selected) {
            selectedContentColor
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(2.dp, selectedContainerColor)
        } else {
            null
        },
        tonalElevation = if (selected) 4.dp else 0.dp,
        enabled = enabled,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
    }
}

@Composable
private fun PalettePickerDialog(
    colors: List<Color>,
    selectedColorIndex: Int,
    onDismiss: () -> Unit,
    onApply: (Int) -> Unit
) {
    var pendingSelection by rememberSaveable(selectedColorIndex) {
        mutableIntStateOf(selectedColorIndex)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_color)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(SpaceXs.dp)) {
                Text(
                    text = stringResource(R.string.select_color_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    verticalArrangement = Arrangement.spacedBy(SpaceXs.dp),
                    horizontalArrangement = Arrangement.spacedBy(SpaceXs.dp),
                    modifier = Modifier.height(220.dp)
                ) {
                    itemsIndexed(colors) { index, color ->
                        val isSelected = pendingSelection == index
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(PaletteChipShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 4.dp else 1.dp,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    shape = PaletteChipShape
                                )
                                .clickable {
                                    pendingSelection = index
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                val radius = size.minDimension / 2.5f
                                // Dark rim
                                drawCircle(
                                    color = Color.Black.copy(alpha = 0.15f),
                                    radius = radius,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx())
                                )
                                // Small highlight
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.25f),
                                    radius = radius * 0.4f,
                                    center = center.copy(x = center.x - radius * 0.3f, y = center.y - radius * 0.3f)
                                )
                            }
                            if (isSelected) {
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
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApply(pendingSelection)
                }
            ) {
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
