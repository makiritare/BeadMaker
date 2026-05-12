package com.example.beadmaker.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatColorFill
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ZoomIn
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.beadmaker.R
import com.example.beadmaker.ui.components.BeadGrid
import com.example.beadmaker.ui.model.BeadShape
import com.example.beadmaker.ui.model.StitchMode
import com.example.beadmaker.ui.state.InteractionModeFill
import com.example.beadmaker.ui.state.InteractionModeLine
import com.example.beadmaker.ui.state.InteractionModePaint
import com.example.beadmaker.ui.state.InteractionModeTemplate
import com.example.beadmaker.ui.state.MaxGridSize
import com.example.beadmaker.ui.state.MinGridSize
import com.example.beadmaker.ui.state.MinTemplateOpacity
import com.example.beadmaker.ui.state.GridHorizontalResizeDirection
import com.example.beadmaker.ui.state.GridVerticalResizeDirection
import com.example.beadmaker.ui.state.calculateLineIndices
import com.example.beadmaker.ui.state.createTemplateCaptureUri
import com.example.beadmaker.ui.state.rememberBeadEditorState
import com.example.beadmaker.ui.state.DefaultBoardScale
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs

private const val SpaceXs = 8
private const val SpaceSm = 12
private const val SpaceMd = 16
private const val PopupWidthFraction = 0.95f
private const val CompactDropdownHeight = 54
private const val CompactDropdownItemMinHeight = 42
private const val CompactDirectionButtonHeight = 36

private val ControlShape = RoundedCornerShape(14.dp)
private val PaletteChipShape = RoundedCornerShape(12.dp)
private val PaletteChipInsetShape = RoundedCornerShape(8.dp)
private val HistoryDockShape = RoundedCornerShape(24.dp)
private val GridFrameShape = RoundedCornerShape(0.dp)
private val GridInnerFrameShape = RoundedCornerShape(0.dp)

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
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val paletteColors = BasicPaletteColorValues.map { Color(it) }

    val gridColumns = uiState.gridColumns
    val gridRows = uiState.gridRows
    val beads = uiState.beads
    val templateImageUri = uiState.templateImageUriString?.let(Uri::parse)
    val stitchMode = StitchMode.fromId(uiState.stitchModeId)
    val beadShape = BeadShape.fromId(uiState.beadShapeId)
    val currentColorIndex = uiState.selectedColorIndex.takeIf { it in paletteColors.indices } ?: 0
    val templateAdjustMode = uiState.interactionMode == InteractionModeTemplate
    val paintModeSelected = uiState.interactionMode == InteractionModePaint && !uiState.brushSelected && !uiState.eraserSelected
    val brushModeSelected = uiState.interactionMode == InteractionModePaint && uiState.brushSelected
    val fillModeSelected = uiState.interactionMode == InteractionModeFill && !uiState.eraserSelected
    val lineModeSelected = uiState.interactionMode == InteractionModeLine && !uiState.eraserSelected
    val linePreviewIndices = if (lineModeSelected &&
        uiState.pendingLineStartIndex != null &&
        uiState.pendingLineEndIndex != null
    ) {
        calculateLineIndices(
            startIndex = uiState.pendingLineStartIndex,
            endIndex = uiState.pendingLineEndIndex,
            columns = gridColumns,
            maxIndex = beads.lastIndex
        ).toSet()
    } else {
        emptySet()
    }
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
        lineModeSelected && uiState.pendingLineEndIndex != null -> stringResource(R.string.status_line_ready)
        lineModeSelected && uiState.pendingLineStartIndex != null -> stringResource(R.string.status_line_pending)
        lineModeSelected -> stringResource(R.string.status_line)
        fillModeSelected -> stringResource(R.string.status_fill)
        brushModeSelected && uiState.eraserSelected -> stringResource(R.string.status_brush_eraser)
        brushModeSelected -> stringResource(R.string.status_brush)
        uiState.eraserSelected -> stringResource(R.string.status_eraser)
        else -> stringResource(R.string.status_paint)
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(vertical = SpaceMd.dp),
                verticalArrangement = Arrangement.spacedBy(SpaceMd.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = SpaceMd.dp),
                    verticalArrangement = Arrangement.spacedBy(SpaceMd.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(6) { slot ->
                                val colorIndex = uiState.recentColorIndices.getOrNull(slot)
                                val swatchColor = colorIndex?.let { paletteColors.getOrNull(it) }
                                val isSelected = colorIndex == currentColorIndex
                                RecentColorSwatch(
                                    color = swatchColor,
                                    selected = isSelected,
                                    onClick = {
                                        if (colorIndex == null || isSelected) {
                                            editorState.showColorPicker()
                                        } else {
                                            editorState.applySelectedColor(colorIndex)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.weight(1f))

                        ModeCircleButton(
                            selected = paintModeSelected,
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

                        ModeCircleButton(
                            selected = uiState.eraserSelected,
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.ink_eraser_24px),
                                    contentDescription = stringResource(R.string.eraser)
                                )
                            },
                            onClick = editorState::toggleEraser
                        )

                        ModeCircleButton(
                            selected = brushModeSelected,
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Brush,
                                    contentDescription = stringResource(R.string.mode_brush)
                                )
                            },
                            onClick = editorState::setBrushMode
                        )

                        ModeCircleButton(
                            selected = fillModeSelected,
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedContentColor = MaterialTheme.colorScheme.onSecondary,
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.FormatColorFill,
                                    contentDescription = stringResource(R.string.mode_fill)
                                )
                            },
                            onClick = editorState::setFillMode
                        )

                        ModeCircleButton(
                            selected = lineModeSelected,
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedContentColor = MaterialTheme.colorScheme.onSecondary,
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.diagonal_line_24px),
                                    contentDescription = stringResource(R.string.mode_line)
                                )
                            },
                            onClick = editorState::setLineMode
                        )

                        ModeCircleButton(
                            selected = uiState.interactionMode == InteractionModeTemplate,
                            selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                            selectedContentColor = MaterialTheme.colorScheme.onTertiary,
                            icon = {
                                ZoomModeIcon(
                                    baseIcon = Icons.Outlined.Image,
                                    contentDescription = stringResource(R.string.mode_template)
                                )
                            },
                            onClick = {
                                if (templateImageUri != null) {
                                    editorState.toggleTemplateMode()
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = context.getString(R.string.template_image_required_warning)
                                        )
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Text(
                        text = modeStatusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
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
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
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
                            .pointerInput(templateAdjustMode) {
                                if (!templateAdjustMode) {
                                    detectMultiTouchTransformGestures(
                                        panZoomLock = true,
                                        allowSingleFingerPan = uiState.boardScale > DefaultBoardScale
                                    ) { pan, zoom, _ ->
                                        editorState.updateBoardTransform(
                                            panX = pan.x,
                                            panY = pan.y,
                                            zoom = zoom
                                        )
                                    }
                                }
                            }
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
                                        rotationZ = uiState.templateRotation
                                    },
                                contentScale = ContentScale.Fit
                            )
                        }

                        BeadGrid(
                            modifier = Modifier
                                .fillMaxSize(),
                            beads = beads,
                            colors = paletteColors,
                            stitchMode = stitchMode,
                            beadShape = beadShape,
                            boardScale = uiState.boardScale,
                            columns = gridColumns,
                            brushEnabled = brushModeSelected,
                            onBrushStrokeStart = editorState::startBrushStroke,
                            onBrushStrokePaint = editorState::paintBrushCell,
                            onBrushStrokeEnd = editorState::endBrushStroke,
                            lineStartIndex = uiState.pendingLineStartIndex,
                            linePreviewIndices = linePreviewIndices,
                            onCellTap = editorState::paintCell
                        )
                    }

                    if (templateImageUri != null && templateAdjustMode) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(templateAdjustMode, templateImageUri) {
                                    detectTransformGestures(panZoomLock = false) { _, pan, zoom, rotation ->
                                        editorState.updateTemplateTransform(
                                            panX = pan.x,
                                            panY = pan.y,
                                            zoom = zoom,
                                            rotation = rotation
                                        )
                                    }
                            }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = SpaceSm.dp, top = SpaceSm.dp)
                            .clip(HistoryDockShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
                                shape = HistoryDockShape
                            )
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(
                            modifier = Modifier.size(56.dp),
                            onClick = editorState::undo,
                            enabled = editorState.canUndo
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Undo,
                                contentDescription = stringResource(R.string.undo),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        IconButton(
                            modifier = Modifier.size(56.dp),
                            onClick = editorState::redo,
                            enabled = editorState.canRedo
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Redo,
                                contentDescription = stringResource(R.string.redo),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(SpaceXs.dp))
                Spacer(modifier = Modifier.height(72.dp))
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(horizontal = SpaceMd.dp, vertical = SpaceMd.dp),
                horizontalArrangement = Arrangement.spacedBy(SpaceXs.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomDockButton(
                    icon = Icons.Outlined.Image,
                    contentDescription = stringResource(R.string.template),
                    onClick = { editorState.openToolsDialogAtTab(0) }
                )
                BottomDockButton(
                    icon = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.settings),
                    onClick = { editorState.openToolsDialogAtTab(1) }
                )
                BottomDockButton(
                    icon = Icons.Outlined.Save,
                    contentDescription = stringResource(R.string.file_actions),
                    onClick = { showFileActionsDialog = true }
                )
            }
        }
    }

    if (uiState.showColorPickerDialog) {
        PalettePickerDialog(
            colors = paletteColors,
            selectedColorIndex = currentColorIndex,
            recentColorIndices = uiState.recentColorIndices,
            onDismiss = editorState::dismissColorPicker,
            onApply = editorState::applySelectedColor
        )
    }

    if (uiState.showToolsDialog) {
        AlertDialog(
            modifier = Modifier.fillMaxWidth(PopupWidthFraction),
            onDismissRequest = editorState::dismissToolsDialog,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            ),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uiState.selectedToolsTab == 0) {
                            stringResource(R.string.template)
                        } else {
                            stringResource(R.string.settings)
                        }
                    )
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
                    verticalArrangement = Arrangement.spacedBy(SpaceXs.dp)
                ) {
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
                                        .fillMaxWidth()
                                        .height(CompactDropdownHeight.dp),
                                    shape = ControlShape
                                )

                                ExposedDropdownMenu(
                                    expanded = stitchDropdownExpanded,
                                    onDismissRequest = { stitchDropdownExpanded = false }
                                ) {
                                    StitchMode.entries
                                        .sortedBy { option ->
                                            when (option) {
                                                StitchMode.Peyote, StitchMode.Peyote2Drop, StitchMode.Peyote3Drop -> 1
                                                else -> 0
                                            }
                                        }
                                        .forEach { option ->
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
                                            modifier = Modifier.heightIn(min = CompactDropdownItemMinHeight.dp),
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
                                        .fillMaxWidth()
                                        .height(CompactDropdownHeight.dp),
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
                                            modifier = Modifier.heightIn(min = CompactDropdownItemMinHeight.dp),
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
                            Text(
                                text = "${stringResource(R.string.width)}: ${uiState.pendingSettingsGridColumns.toInt()}",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Slider(
                                value = uiState.pendingSettingsGridColumns,
                                onValueChange = editorState::updatePendingGridColumns,
                                valueRange = MinGridSize.toFloat()..MaxGridSize.toFloat(),
                                steps = MaxGridSize - MinGridSize - 1
                            )
                            ResizeDirectionToggle(
                                label = stringResource(R.string.add_columns_on),
                                firstContentDescription = stringResource(R.string.left),
                                secondContentDescription = stringResource(R.string.right),
                                firstIcon = Icons.AutoMirrored.Outlined.ArrowBack,
                                secondIcon = Icons.AutoMirrored.Outlined.ArrowForward,
                                firstSelected = uiState.pendingGridHorizontalResizeDirection ==
                                    GridHorizontalResizeDirection.Left,
                                onSelectFirst = {
                                    editorState.updatePendingGridHorizontalResizeDirection(
                                        GridHorizontalResizeDirection.Left
                                    )
                                },
                                onSelectSecond = {
                                    editorState.updatePendingGridHorizontalResizeDirection(
                                        GridHorizontalResizeDirection.Right
                                    )
                                }
                            )
                            Text(
                                text = "${stringResource(R.string.height)}: ${uiState.pendingSettingsGridRows.toInt()}",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Slider(
                                value = uiState.pendingSettingsGridRows,
                                onValueChange = editorState::updatePendingGridRows,
                                valueRange = MinGridSize.toFloat()..MaxGridSize.toFloat(),
                                steps = MaxGridSize - MinGridSize - 1
                            )
                            ResizeDirectionToggle(
                                label = stringResource(R.string.add_rows_on),
                                firstContentDescription = stringResource(R.string.top),
                                secondContentDescription = stringResource(R.string.bottom),
                                firstIcon = Icons.Outlined.KeyboardArrowUp,
                                secondIcon = Icons.Outlined.KeyboardArrowDown,
                                firstSelected = uiState.pendingGridVerticalResizeDirection ==
                                    GridVerticalResizeDirection.Top,
                                onSelectFirst = {
                                    editorState.updatePendingGridVerticalResizeDirection(
                                        GridVerticalResizeDirection.Top
                                    )
                                },
                                onSelectSecond = {
                                    editorState.updatePendingGridVerticalResizeDirection(
                                        GridVerticalResizeDirection.Bottom
                                    )
                                }
                            )
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
            modifier = Modifier.fillMaxWidth(PopupWidthFraction),
            onDismissRequest = { showFileActionsDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
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
private fun RecentColorSwatch(
    color: Color?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)
                },
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (color != null) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2.2f
                drawCircle(
                    color = Color.Black.copy(alpha = 0.15f),
                    radius = radius,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.25f),
                    radius = radius * 0.4f,
                    center = center.copy(x = center.x - radius * 0.3f, y = center.y - radius * 0.3f)
                )
            }
        }
    }
}

@Composable
private fun BottomDockButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
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
        onClick = onClick
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription
        )
    }
}

@Composable
private fun ModeCircleButton(
    selected: Boolean,
    enabled: Boolean = true,
    selectedContainerColor: Color = MaterialTheme.colorScheme.primary,
    selectedContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(46.dp),
        shape = CircleShape,
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
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
    }
}

@Composable
private fun ZoomModeIcon(
    baseIcon: ImageVector,
    contentDescription: String
) {
    Box(
        modifier = Modifier.size(30.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = baseIcon,
            contentDescription = contentDescription,
            modifier = Modifier
                .align(Alignment.Center)
                .size(25.dp)
        )
        Icon(
            imageVector = Icons.Outlined.ZoomIn,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 9.dp)
                .offset(y = (-2).dp)
                .size(29.dp)
        )
    }
}

private suspend fun PointerInputScope.detectMultiTouchTransformGestures(
    panZoomLock: Boolean = false,
    allowSingleFingerPan: Boolean = false,
    onGesture: (pan: Offset, zoom: Float, rotation: Float) -> Unit
) {
    awaitEachGesture {
        var rotation = 0f
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        var lockedToPanZoom = false

        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val canceled = event.changes.any { it.isConsumed }
            val activePointerCount = event.changes.count { it.pressed && it.previousPressed }
            val allowTransform = activePointerCount >= 2
            val allowPan = activePointerCount >= 2 || (allowSingleFingerPan && activePointerCount == 1)

            if (!canceled && allowPan) {
                val zoomChange = if (allowTransform) event.calculateZoom() else 1f
                val rotationChange = if (allowTransform) event.calculateRotation() else 0f
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    rotation += rotationChange
                    pan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val rotationMotion = abs(rotation * PI.toFloat() * centroidSize / 180f)
                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop ||
                        rotationMotion > touchSlop ||
                        panMotion > touchSlop
                    ) {
                        pastTouchSlop = true
                        lockedToPanZoom = panZoomLock && rotationMotion < touchSlop
                    }
                }

                if (pastTouchSlop) {
                    val effectiveRotation = if (lockedToPanZoom) 0f else rotationChange
                    if (effectiveRotation != 0f ||
                        zoomChange != 1f ||
                        panChange != Offset.Zero
                    ) {
                        onGesture(panChange, zoomChange, effectiveRotation)
                    }
                }
            }

            if (pastTouchSlop) {
                event.changes.forEach { change ->
                    if (change.positionChanged()) {
                        change.consume()
                    }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })
    }
}

@Composable
private fun ResizeDirectionToggle(
    label: String,
    firstContentDescription: String,
    secondContentDescription: String,
    firstIcon: ImageVector,
    secondIcon: ImageVector,
    firstSelected: Boolean,
    onSelectFirst: () -> Unit,
    onSelectSecond: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpaceXs.dp)
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = label,
            style = MaterialTheme.typography.titleSmall
        )
        Row(
            modifier = Modifier.weight(1.4f),
            horizontalArrangement = Arrangement.spacedBy(SpaceXs.dp)
        ) {
            if (firstSelected) {
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(CompactDirectionButtonHeight.dp),
                    onClick = onSelectFirst,
                    shape = ControlShape
                ) {
                    Icon(
                        imageVector = firstIcon,
                        contentDescription = firstContentDescription
                    )
                }
            } else {
                OutlinedButton(
                    modifier = Modifier
                        .weight(1f)
                        .height(CompactDirectionButtonHeight.dp),
                    onClick = onSelectFirst,
                    shape = ControlShape
                ) {
                    Icon(
                        imageVector = firstIcon,
                        contentDescription = firstContentDescription
                    )
                }
            }

            if (!firstSelected) {
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(CompactDirectionButtonHeight.dp),
                    onClick = onSelectSecond,
                    shape = ControlShape
                ) {
                    Icon(
                        imageVector = secondIcon,
                        contentDescription = secondContentDescription
                    )
                }
            } else {
                OutlinedButton(
                    modifier = Modifier
                        .weight(1f)
                        .height(CompactDirectionButtonHeight.dp),
                    onClick = onSelectSecond,
                    shape = ControlShape
                ) {
                    Icon(
                        imageVector = secondIcon,
                        contentDescription = secondContentDescription
                    )
                }
            }
        }
    }
}

@Composable
private fun PalettePickerDialog(
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
        modifier = Modifier.fillMaxWidth(PopupWidthFraction),
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(stringResource(R.string.select_color)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SpaceXs.dp)
            ) {
                Text(
                    text = stringResource(R.string.select_color_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(SpaceXs.dp)
                ) {
                    colors.chunked(6).forEachIndexed { rowIndex, rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(SpaceXs.dp)
                        ) {
                            rowColors.forEachIndexed { columnIndex, color ->
                                val colorIndex = rowIndex * 6 + columnIndex
                                PaletteColorChip(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f),
                                    color = color,
                                    selected = pendingSelection == colorIndex,
                                    onClick = {
                                        pendingSelection = colorIndex
                                    }
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
                    horizontalArrangement = Arrangement.spacedBy(SpaceXs.dp)
                ) {
                    repeat(6) { slot ->
                        val colorIndex = recentColorIndices.getOrNull(slot)
                        if (colorIndex == null) {
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
                        } else {
                            val color = colors.getOrNull(colorIndex)
                            if (color != null) {
                                PaletteColorChip(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f),
                                    color = color,
                                    selected = pendingSelection == colorIndex,
                                    onClick = {
                                        pendingSelection = colorIndex
                                    }
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
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
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = radius * 0.4f,
                center = center.copy(x = center.x - radius * 0.3f, y = center.y - radius * 0.3f)
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
