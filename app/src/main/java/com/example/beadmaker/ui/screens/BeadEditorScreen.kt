package com.example.beadmaker.ui.screens

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixOff
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Image
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
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.beadmaker.ui.model.StitchMode
import coil.compose.AsyncImage
import java.io.File
import java.util.UUID

private const val DefaultGridColumns = 16
private const val DefaultGridRows = 16
private const val MinGridSize = 8
private const val MaxGridSize = 64
private const val EmptyBead = -1
private const val MinTemplateOpacity = 0.1f
private const val DefaultTemplateOpacity = 0.55f
private const val MinTemplateScale = 0.2f
private const val MaxTemplateScale = 5.0f
private const val DefaultTemplateScale = 1.0f
private const val MinBoardScale = 1.0f
private const val MaxBoardScale = 6.0f
private const val DefaultBoardScale = 1.0f
private const val InteractionModePaint = 0
private const val InteractionModeTemplate = 1
private const val InteractionModeGrid = 2

private const val SpaceXs = 8
private const val SpaceSm = 12
private const val SpaceMd = 16
private const val SpaceLg = 24

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
    var gridColumns by rememberSaveable { mutableIntStateOf(DefaultGridColumns) }
    var gridRows by rememberSaveable { mutableIntStateOf(DefaultGridRows) }
    var stitchModeId by rememberSaveable { mutableStateOf<String>(StitchMode.defaults.id) }
    var beads by rememberSaveable {
        mutableStateOf<List<Int>>(List(gridColumns * gridRows) { EmptyBead })
    }
    val paletteColors = BasicPaletteColorValues.map { Color(it) }

    var selectedColorIndex by rememberSaveable { mutableIntStateOf(0) }
    var eraserSelected by rememberSaveable { mutableStateOf(false) }
    var templateImageUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var templateOpacity by rememberSaveable { mutableFloatStateOf(DefaultTemplateOpacity) }
    var templateScale by rememberSaveable { mutableFloatStateOf(DefaultTemplateScale) }
    var templateOffsetX by rememberSaveable { mutableFloatStateOf(0f) }
    var templateOffsetY by rememberSaveable { mutableFloatStateOf(0f) }
    var boardScale by rememberSaveable { mutableFloatStateOf(DefaultBoardScale) }
    var boardOffsetX by rememberSaveable { mutableFloatStateOf(0f) }
    var boardOffsetY by rememberSaveable { mutableFloatStateOf(0f) }
    var interactionMode by rememberSaveable { mutableIntStateOf(InteractionModePaint) }
    var showColorPickerDialog by rememberSaveable { mutableStateOf(false) }
    var showToolsDialog by rememberSaveable { mutableStateOf(false) }
    var selectedToolsTab by rememberSaveable { mutableIntStateOf(0) }
    var pendingCameraUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingSettingsStitchId by rememberSaveable { mutableStateOf(stitchModeId) }
    var pendingSettingsGridColumns by rememberSaveable { mutableFloatStateOf(gridColumns.toFloat()) }
    var pendingSettingsGridRows by rememberSaveable { mutableFloatStateOf(gridRows.toFloat()) }

    val templateImageUri = templateImageUriString?.let(Uri::parse)
    val stitchMode = StitchMode.fromId(stitchModeId)
    val currentColorIndex = selectedColorIndex.takeIf { it in paletteColors.indices } ?: 0
    val currentColor = paletteColors[currentColorIndex]
    val templateAdjustMode = interactionMode == InteractionModeTemplate
    val boardAdjustMode = interactionMode == InteractionModeGrid
    val resetTemplateAndBoardAdjustments = {
        templateScale = DefaultTemplateScale
        templateOffsetX = 0f
        templateOffsetY = 0f
        boardScale = DefaultBoardScale
        boardOffsetX = 0f
        boardOffsetY = 0f
        interactionMode = InteractionModePaint
    }
    val openToolsDialogAtTab: (Int) -> Unit = { tabIndex ->
        selectedToolsTab = tabIndex
        pendingSettingsStitchId = stitchModeId
        pendingSettingsGridColumns = gridColumns.toFloat()
        pendingSettingsGridRows = gridRows.toFloat()
        showToolsDialog = true
    }
    val templatePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            templateImageUriString = uri.toString()
            resetTemplateAndBoardAdjustments()
        }
    }
    val cameraTemplateCapture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingCameraUriString?.let {
                templateImageUriString = it
                resetTemplateAndBoardAdjustments()
            }
        }
        pendingCameraUriString = null
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SpaceMd.dp, vertical = SpaceMd.dp),
            verticalArrangement = Arrangement.spacedBy(SpaceMd.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SpaceXs.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            color = if (eraserSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
                            }
                        )
                        .border(
                            width = if (eraserSelected) 2.dp else 1.dp,
                            color = if (eraserSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            shape = CircleShape
                        ),
                    onClick = { eraserSelected = !eraserSelected },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoFixOff,
                        contentDescription = stringResource(R.string.eraser),
                        tint = if (eraserSelected) {
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
                            width = if (eraserSelected) 1.dp else 2.dp,
                            color = if (eraserSelected) {
                                MaterialTheme.colorScheme.outlineVariant
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            shape = CircleShape
                        ),
                    onClick = { showColorPickerDialog = true },
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

            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SpaceXs.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModeIconButton(
                    modifier = Modifier.weight(1f),
                    selected = interactionMode == InteractionModePaint,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.mode_paint)
                        )
                    },
                    onClick = { interactionMode = InteractionModePaint }
                )

                ModeIconButton(
                    modifier = Modifier.weight(1f),
                    selected = interactionMode == InteractionModeTemplate,
                    enabled = templateImageUri != null,
                    selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                    selectedContentColor = MaterialTheme.colorScheme.onTertiary,
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = stringResource(R.string.mode_template)
                        )
                    },
                    onClick = {
                        interactionMode =
                            if (interactionMode == InteractionModeTemplate) {
                                InteractionModePaint
                            } else {
                                InteractionModeTemplate
                            }
                    }
                )

                ModeIconButton(
                    modifier = Modifier.weight(1f),
                    selected = interactionMode == InteractionModeGrid,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.GridOn,
                            contentDescription = stringResource(R.string.mode_grid)
                        )
                    },
                    onClick = {
                        interactionMode =
                            if (interactionMode == InteractionModeGrid) {
                                InteractionModePaint
                            } else {
                                InteractionModeGrid
                            }
                    }
                )
            }

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
                    onClick = { openToolsDialogAtTab(0) }
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
                            scaleX = boardScale
                            scaleY = boardScale
                            translationX = boardOffsetX
                            translationY = boardOffsetY
                        }
                ) {
                    if (templateImageUri != null) {
                        AsyncImage(
                            model = templateImageUri,
                            contentDescription = stringResource(R.string.template_image),
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(templateOpacity)
                                .graphicsLayer {
                                    scaleX = templateScale
                                    scaleY = templateScale
                                    translationX = templateOffsetX
                                    translationY = templateOffsetY
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
                                        templateScale =
                                            (templateScale * zoom).coerceIn(MinTemplateScale, MaxTemplateScale)
                                        templateOffsetX += pan.x
                                        templateOffsetY += pan.y
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
                        columns = gridColumns,
                        onCellTap = { index ->
                            if (templateAdjustMode || boardAdjustMode) {
                                return@BeadGrid
                            }
                            val nextColor = if (eraserSelected) {
                                EmptyBead
                            } else {
                                currentColorIndex
                            }

                            beads = beads.toMutableList().apply {
                                this[index] = nextColor
                            }
                        }
                    )
                }

                if (boardAdjustMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    boardScale =
                                        (boardScale * zoom).coerceIn(MinBoardScale, MaxBoardScale)
                                    boardOffsetX += pan.x
                                    boardOffsetY += pan.y
                                }
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(SpaceXs.dp))
        }
    }

    if (showColorPickerDialog) {
        PalettePickerDialog(
            colors = paletteColors,
            selectedColorIndex = currentColorIndex,
            onDismiss = { showColorPickerDialog = false },
            onApply = { updatedSelection ->
                selectedColorIndex = updatedSelection
                eraserSelected = false
                showColorPickerDialog = false
            }
        )
    }

    if (showToolsDialog) {
        AlertDialog(
            onDismissRequest = { showToolsDialog = false },
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
                        onClick = { showToolsDialog = false }
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
                    TabRow(selectedTabIndex = selectedToolsTab) {
                        Tab(
                            selected = selectedToolsTab == 0,
                            onClick = { selectedToolsTab = 0 },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Image,
                                    contentDescription = stringResource(R.string.template)
                                )
                            }
                        )
                        Tab(
                            selected = selectedToolsTab == 1,
                            onClick = { selectedToolsTab = 1 },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = stringResource(R.string.editor)
                                )
                            }
                        )
                    }

                    when (selectedToolsTab) {
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
                                        showToolsDialog = false
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
                                            pendingCameraUriString = captureUri.toString()
                                            cameraTemplateCapture.launch(captureUri)
                                            showToolsDialog = false
                                        }
                                    },
                                    shape = ControlShape
                                ) {
                                    Text(stringResource(R.string.take_photo))
                                }
                            }
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    templateImageUriString = null
                                    templateScale = DefaultTemplateScale
                                    templateOffsetX = 0f
                                    templateOffsetY = 0f
                                    if (interactionMode == InteractionModeTemplate) {
                                        interactionMode = InteractionModePaint
                                    }
                                },
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
                                    value = templateOpacity,
                                    onValueChange = { templateOpacity = it },
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
                                            templateOpacity
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
                                        onClick = {
                                            interactionMode =
                                                if (interactionMode == InteractionModeTemplate) {
                                                    InteractionModePaint
                                                } else {
                                                    InteractionModeTemplate
                                                }
                                        },
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
                                        onClick = {
                                            templateScale = DefaultTemplateScale
                                            templateOffsetX = 0f
                                            templateOffsetY = 0f
                                        },
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
                                    onClick = {
                                        beads = List(gridColumns * gridRows) { EmptyBead }
                                    },
                                    shape = ControlShape
                                ) {
                                    Text(stringResource(R.string.clear_grid))
                                }
                                OutlinedButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        boardScale = DefaultBoardScale
                                        boardOffsetX = 0f
                                        boardOffsetY = 0f
                                    },
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
                            val selectedStitchMode = StitchMode.fromId(pendingSettingsStitchId)

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
                                                pendingSettingsStitchId = option.id
                                                stitchDropdownExpanded = false
                                            },
                                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                        )
                                    }
                                }
                            }

                            Text(
                                text = stringResource(
                                    R.string.grid_size,
                                    pendingSettingsGridColumns.toInt(),
                                    pendingSettingsGridRows.toInt()
                                ),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(stringResource(R.string.width))
                            Slider(
                                value = pendingSettingsGridColumns,
                                onValueChange = { pendingSettingsGridColumns = it },
                                valueRange = MinGridSize.toFloat()..MaxGridSize.toFloat(),
                                steps = MaxGridSize - MinGridSize - 1
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${MinGridSize}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(
                                        R.string.current_value,
                                        pendingSettingsGridColumns.toInt()
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${MaxGridSize}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(stringResource(R.string.height))
                            Slider(
                                value = pendingSettingsGridRows,
                                onValueChange = { pendingSettingsGridRows = it },
                                valueRange = MinGridSize.toFloat()..MaxGridSize.toFloat(),
                                steps = MaxGridSize - MinGridSize - 1
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${MinGridSize}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(
                                        R.string.current_value,
                                        pendingSettingsGridRows.toInt()
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${MaxGridSize}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (selectedToolsTab == 1) {
                    TextButton(
                        onClick = {
                            val updatedColumns = pendingSettingsGridColumns.toInt()
                            val updatedRows = pendingSettingsGridRows.toInt()
                            stitchModeId = StitchMode.fromId(pendingSettingsStitchId).id
                            if (updatedColumns != gridColumns || updatedRows != gridRows) {
                                gridColumns = updatedColumns
                                gridRows = updatedRows
                                beads = List(gridColumns * gridRows) { EmptyBead }
                            }
                            showToolsDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.apply))
                    }
                } else {
                    TextButton(onClick = { showToolsDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            },
            dismissButton = {
                if (selectedToolsTab == 1) {
                    TextButton(onClick = { showToolsDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        )
    }
}

private fun createTemplateCaptureUri(context: Context): Uri? {
    return runCatching {
        val imageDirectory = File(context.cacheDir, "template_images").apply {
            if (!exists()) mkdirs()
        }
        val imageFile = File(imageDirectory, "template_${UUID.randomUUID()}.jpg").apply {
            createNewFile()
        }
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }.getOrNull()
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
    var pendingSelection by rememberSaveable { mutableStateOf(selectedColorIndex) }

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
