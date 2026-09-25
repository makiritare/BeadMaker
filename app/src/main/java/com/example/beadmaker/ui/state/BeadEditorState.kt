package com.example.beadmaker.ui.state

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.beadmaker.ui.model.BeadShape
import com.example.beadmaker.ui.model.InteractionMode
import com.example.beadmaker.ui.model.StitchMode
import java.net.URLDecoder
import java.net.URLEncoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val DefaultGridColumns = 16
const val DefaultGridRows = 16
const val MinGridSize = 8
const val MaxGridSize = 64
const val EmptyBead = -1
const val MinTemplateOpacity = 0.1f
const val DefaultTemplateOpacity = 0.55f
const val MinTemplateScale = 0.2f
const val MaxTemplateScale = 5.0f
const val DefaultTemplateScale = 1.0f
const val DefaultTemplateRotation = 0f
const val MinBoardScale = 0.5f
const val MaxBoardScale = 6.0f
const val DefaultBoardScale = 1.0f
const val PaletteColorCount = 24
private const val MaxUndoStackSize = 30
private const val SavedEditorStateVersion = 6
private const val FirstStringEditorStateVersion = 5
private const val MaxRecentColors = 6
private const val MaxToolsTabIndex = 1

enum class GridHorizontalResizeDirection {
    Left,
    Right
}

enum class GridVerticalResizeDirection {
    Top,
    Bottom
}

data class EditorUiState(
    val gridColumns: Int = DefaultGridColumns,
    val gridRows: Int = DefaultGridRows,
    val stitchModeId: String = StitchMode.defaults.id,
    val beadShapeId: String = BeadShape.defaults.id,
    val beads: List<Int> = List(DefaultGridColumns * DefaultGridRows) { EmptyBead },
    val selectedColorIndex: Int = 0,
    val recentColorIndices: List<Int> = listOf(0),
    val eraserSelected: Boolean = false,
    val templateImageUriString: String? = null,
    val templateOpacity: Float = DefaultTemplateOpacity,
    val templateScale: Float = DefaultTemplateScale,
    val templateOffsetX: Float = 0f,
    val templateOffsetY: Float = 0f,
    val templateRotation: Float = DefaultTemplateRotation,
    val boardScale: Float = DefaultBoardScale,
    val boardOffsetX: Float = 0f,
    val boardOffsetY: Float = 0f,
    val interactionMode: InteractionMode = InteractionMode.default,
    val brushSelected: Boolean = false,
    val pendingLineStartIndex: Int? = null,
    val pendingLineEndIndex: Int? = null,
    val showColorPickerDialog: Boolean = false,
    val showToolsDialog: Boolean = false,
    val selectedToolsTab: Int = 0,
    val pendingCameraUriString: String? = null,
    val pendingSettingsStitchId: String = StitchMode.defaults.id,
    val pendingSettingsBeadShapeId: String = BeadShape.defaults.id,
    val pendingSettingsGridColumns: Float = DefaultGridColumns.toFloat(),
    val pendingSettingsGridRows: Float = DefaultGridRows.toFloat(),
    val pendingGridHorizontalResizeDirection: GridHorizontalResizeDirection =
        GridHorizontalResizeDirection.Right,
    val pendingGridVerticalResizeDirection: GridVerticalResizeDirection =
        GridVerticalResizeDirection.Bottom,
    val isCreatingPattern: Boolean = false,
    val isImportingTemplate: Boolean = false,
    val isPatternIoInProgress: Boolean = false
)

data class BoardSnapshot(
    val gridColumns: Int,
    val gridRows: Int,
    val stitchModeId: String,
    val beadShapeId: String,
    val beads: List<Int>
)

class BeadEditorState private constructor(
    private val providedAppContext: Context?,
    initialUiState: EditorUiState = EditorUiState(),
    @Suppress("UNUSED_PARAMETER") contextFreeMarker: Unit
) {
    constructor(
        context: Context,
        initialUiState: EditorUiState = EditorUiState()
    ) : this(
        providedAppContext = context.applicationContext,
        initialUiState = initialUiState,
        contextFreeMarker = Unit
    )

    internal constructor(initialUiState: EditorUiState = EditorUiState()) : this(
        providedAppContext = null,
        initialUiState = initialUiState,
        contextFreeMarker = Unit
    )

    private val appContext: Context by lazy {
        requireNotNull(providedAppContext) {
            "An Android context is required for template and pattern I/O."
        }
    }
    private val patternStorage: PatternStorage by lazy { PatternStorage(appContext) }
    private val templateImageStorage: TemplateImageStorage by lazy {
        TemplateImageStorage(appContext)
    }
    private val undoStack = mutableStateListOf<BoardSnapshot>()
    private val redoStack = mutableStateListOf<BoardSnapshot>()
    private var brushStrokeActive = false
    private var lastBrushIndex: Int? = null

    var uiState by mutableStateOf(normalizeEditorUiState(initialUiState))
        private set

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    val isGridEmpty: Boolean
        get() = isGridEmpty(uiState.beads)

    fun toggleEraser() {
        val nextEraserSelected = !uiState.eraserSelected
        uiState = uiState.copy(
            eraserSelected = nextEraserSelected,
            interactionMode = if (nextEraserSelected) {
                InteractionMode.Paint
            } else {
                uiState.interactionMode
            },
            brushSelected = if (nextEraserSelected && uiState.interactionMode != InteractionMode.Paint) {
                false
            } else {
                uiState.brushSelected
            },
            pendingLineStartIndex = null,
            pendingLineEndIndex = null
        )
    }

    fun showColorPicker() {
        uiState = uiState.copy(showColorPickerDialog = true)
    }

    fun dismissColorPicker() {
        uiState = uiState.copy(showColorPickerDialog = false)
    }

    fun applySelectedColor(index: Int) {
        if (index !in 0 until PaletteColorCount) return
        uiState = uiState.copy(
            selectedColorIndex = index,
            recentColorIndices = updateRecentColors(uiState.recentColorIndices, index),
            eraserSelected = false,
            pendingLineStartIndex = null,
            pendingLineEndIndex = null,
            showColorPickerDialog = false
        )
    }

    fun setPaintMode() {
        uiState = uiState.copy(
            interactionMode = InteractionMode.Paint,
            eraserSelected = false,
            brushSelected = false,
            pendingLineStartIndex = null,
            pendingLineEndIndex = null
        )
    }

    fun setBrushMode() {
        uiState = uiState.copy(
            interactionMode = InteractionMode.Paint,
            eraserSelected = false,
            brushSelected = true,
            pendingLineStartIndex = null,
            pendingLineEndIndex = null
        )
    }

    fun setFillMode() {
        uiState = uiState.copy(
            interactionMode = InteractionMode.Fill,
            eraserSelected = false,
            brushSelected = false,
            pendingLineStartIndex = null,
            pendingLineEndIndex = null
        )
    }

    fun setLineMode() {
        uiState = uiState.copy(
            interactionMode = InteractionMode.Line,
            eraserSelected = false,
            brushSelected = false,
            pendingLineStartIndex = null,
            pendingLineEndIndex = null
        )
    }

    fun toggleTemplateMode() {
        if (uiState.templateImageUriString == null) return
        uiState = uiState.copy(
            interactionMode = if (uiState.interactionMode == InteractionMode.Template) {
                InteractionMode.Paint
            } else {
                InteractionMode.Template
            },
            eraserSelected = false,
            brushSelected = false,
            pendingLineStartIndex = null,
            pendingLineEndIndex = null
        )
    }

    fun startBrushStroke() {
        brushStrokeActive = false
        lastBrushIndex = null
    }

    fun paintBrushCell(index: Int) {
        if (uiState.interactionMode != InteractionMode.Paint || index !in uiState.beads.indices) return

        val nextColor = if (uiState.eraserSelected) EmptyBead else uiState.selectedColorIndex
        val updatedBeads = if (lastBrushIndex == null) {
            updateBeadAt(uiState.beads, index, nextColor)
        } else {
            updateBeadsAt(
                beads = uiState.beads,
                indices = calculateLineIndices(
                    startIndex = lastBrushIndex!!,
                    endIndex = index,
                    columns = uiState.gridColumns,
                    maxIndex = uiState.beads.lastIndex
                ),
                nextColor = nextColor
            )
        }
        if (updatedBeads === uiState.beads) return

        if (!brushStrokeActive) {
            pushSnapshot()
            brushStrokeActive = true
        }

        uiState = uiState.copy(
            beads = updatedBeads,
            pendingLineStartIndex = null,
            pendingLineEndIndex = null
        )
        lastBrushIndex = index
    }

    fun endBrushStroke() {
        brushStrokeActive = false
        lastBrushIndex = null
    }

    fun openToolsDialogAtTab(tabIndex: Int) {
        uiState = uiState.copy(
            selectedToolsTab = tabIndex.coerceIn(0, MaxToolsTabIndex),
            pendingSettingsStitchId = uiState.stitchModeId,
            pendingSettingsBeadShapeId = uiState.beadShapeId,
            pendingSettingsGridColumns = uiState.gridColumns.toFloat(),
            pendingSettingsGridRows = uiState.gridRows.toFloat(),
            pendingGridHorizontalResizeDirection = GridHorizontalResizeDirection.Right,
            pendingGridVerticalResizeDirection = GridVerticalResizeDirection.Bottom,
            showToolsDialog = true
        )
    }

    fun dismissToolsDialog() {
        uiState = uiState.copy(showToolsDialog = false)
    }

    fun updateTemplateOpacity(value: Float) {
        if (!value.isFinite()) return
        uiState = uiState.copy(templateOpacity = value.coerceIn(MinTemplateOpacity, 1f))
    }

    fun updateTemplateTransform(panX: Float, panY: Float, zoom: Float, rotation: Float) {
        val safeZoom = zoom.takeIf { it.isFinite() && it > 0f } ?: 1f
        val safePanX = panX.takeIf(Float::isFinite) ?: 0f
        val safePanY = panY.takeIf(Float::isFinite) ?: 0f
        val safeRotation = rotation.takeIf(Float::isFinite) ?: 0f
        uiState = uiState.copy(
            templateScale = (uiState.templateScale * safeZoom)
                .finiteOr(DefaultTemplateScale)
                .coerceIn(MinTemplateScale, MaxTemplateScale),
            templateOffsetX = (uiState.templateOffsetX + safePanX).finiteOr(0f),
            templateOffsetY = (uiState.templateOffsetY + safePanY).finiteOr(0f),
            templateRotation = normalizeRotationDegrees(
                (uiState.templateRotation + safeRotation).finiteOr(DefaultTemplateRotation)
            )
        )
    }

    fun updateBoardTransform(panX: Float, panY: Float, zoom: Float) {
        val safeZoom = zoom.takeIf { it.isFinite() && it > 0f } ?: 1f
        val safePanX = panX.takeIf(Float::isFinite) ?: 0f
        val safePanY = panY.takeIf(Float::isFinite) ?: 0f
        uiState = uiState.copy(
            boardScale = (uiState.boardScale * safeZoom)
                .finiteOr(DefaultBoardScale)
                .coerceIn(MinBoardScale, MaxBoardScale),
            boardOffsetX = (uiState.boardOffsetX + safePanX).finiteOr(0f),
            boardOffsetY = (uiState.boardOffsetY + safePanY).finiteOr(0f)
        )
    }

    fun resetTemplateTransform() {
        uiState = uiState.copy(
            templateScale = DefaultTemplateScale,
            templateOffsetX = 0f,
            templateOffsetY = 0f,
            templateRotation = DefaultTemplateRotation
        )
    }

    fun resetBoardTransform() {
        uiState = uiState.copy(
            boardScale = DefaultBoardScale,
            boardOffsetX = 0f,
            boardOffsetY = 0f
        )
    }

    fun resetTemplateAndBoardAdjustments() {
        uiState = uiState.copy(
            templateScale = DefaultTemplateScale,
            templateOffsetX = 0f,
            templateOffsetY = 0f,
            templateRotation = DefaultTemplateRotation,
            boardScale = DefaultBoardScale,
            boardOffsetX = 0f,
            boardOffsetY = 0f,
            interactionMode = InteractionMode.Paint,
            brushSelected = false
        )
    }

    suspend fun importTemplateFromPicker(uri: Uri): Boolean {
        if (uiState.isImportingTemplate || uiState.isCreatingPattern) return false

        val destinationFile = templateImageStorage.newCacheFile()
        var installed = false
        uiState = uiState.copy(isImportingTemplate = true)
        return try {
            val cachedUri = withContext(Dispatchers.IO) {
                templateImageStorage.copyToCache(uri, destinationFile)
            } ?: return false
            replaceTemplateImage(cachedUri.toString())
            installed = true
            resetTemplateAndBoardAdjustments()
            true
        } finally {
            if (!installed) destinationFile.delete()
            uiState = uiState.copy(isImportingTemplate = false)
        }
    }

    fun prepareCameraTemplateCapture(uriString: String) {
        val uri = uriString.takeIf { it.isNotBlank() }?.let(Uri::parse) ?: return
        if (!templateImageStorage.isOwnedCaptureUri(uri)) return
        uiState = uiState.copy(pendingCameraUriString = uriString)
    }

    fun finishCameraTemplateCapture(success: Boolean) {
        val pendingUriString = uiState.pendingCameraUriString
        if (success && pendingUriString != null) {
            replaceTemplateImage(pendingUriString)
            resetTemplateAndBoardAdjustments()
        } else {
            templateImageStorage.delete(pendingUriString)
        }
        uiState = uiState.copy(pendingCameraUriString = null)
    }

    fun removeTemplateImage() {
        replaceTemplateImage(null)
        uiState = uiState.copy(
            templateScale = DefaultTemplateScale,
            templateOffsetX = 0f,
            templateOffsetY = 0f,
            templateRotation = DefaultTemplateRotation,
            interactionMode = if (uiState.interactionMode == InteractionMode.Template) {
                InteractionMode.Paint
            } else {
                uiState.interactionMode
            },
            brushSelected = false
        )
    }

    fun clearGrid() {
        if (isGridEmpty) return
        pushSnapshot()
        uiState = uiState.copy(
            beads = List(uiState.gridColumns * uiState.gridRows) { EmptyBead }
        )
    }

    suspend fun createPatternFromTemplateImage(
        paletteColors: List<Int>,
        viewportWidth: Int,
        viewportHeight: Int
    ): Boolean {
        val sourceState = uiState
        val imageUri = sourceState.templateImageUriString?.let(Uri::parse) ?: return false
        val supportedPaletteColors = paletteColors.take(PaletteColorCount)
        if (
            sourceState.isCreatingPattern ||
            sourceState.isImportingTemplate ||
            supportedPaletteColors.isEmpty() ||
            viewportWidth <= 0 ||
            viewportHeight <= 0
        ) {
            return false
        }

        uiState = sourceState.copy(isCreatingPattern = true)
        return try {
            val nextBeads = try {
                withContext(Dispatchers.Default) {
                    createPatternBeads(
                        context = appContext,
                        imageUri = imageUri,
                        paletteColors = supportedPaletteColors,
                        viewportWidth = viewportWidth,
                        viewportHeight = viewportHeight,
                        sourceState = sourceState
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }

            if (
                nextBeads == null ||
                nextBeads == sourceState.beads ||
                !uiState.hasSameTemplateConversionInputAs(sourceState)
            ) {
                false
            } else {
                pushSnapshot()
                uiState = uiState.copy(
                    beads = nextBeads,
                    interactionMode = InteractionMode.Paint,
                    brushSelected = false,
                    eraserSelected = false,
                    pendingLineStartIndex = null,
                    pendingLineEndIndex = null
                )
                true
            }
        } finally {
            uiState = uiState.copy(isCreatingPattern = false)
        }
    }

    fun paintCell(index: Int) {
        if (uiState.interactionMode == InteractionMode.Template || index !in uiState.beads.indices) return

        val nextColor = if (uiState.eraserSelected) {
            EmptyBead
        } else {
            uiState.selectedColorIndex
        }
        if (uiState.interactionMode == InteractionMode.Line) {
            val startIndex = uiState.pendingLineStartIndex
            if (startIndex == null) {
                uiState = uiState.copy(
                    pendingLineStartIndex = index,
                    pendingLineEndIndex = null
                )
                return
            }

            val endIndex = uiState.pendingLineEndIndex
            if (endIndex == null) {
                uiState = if (index == startIndex) {
                    uiState.copy(
                        pendingLineStartIndex = null,
                        pendingLineEndIndex = null
                    )
                } else {
                    uiState.copy(pendingLineEndIndex = index)
                }
                return
            }

            if (index != endIndex) {
                uiState = if (index == startIndex) {
                    uiState.copy(
                        pendingLineStartIndex = index,
                        pendingLineEndIndex = null
                    )
                } else {
                    uiState.copy(pendingLineEndIndex = index)
                }
                return
            }

            val updatedBeads = drawLineOnGrid(
                beads = uiState.beads,
                startIndex = startIndex,
                endIndex = endIndex,
                replacementColor = nextColor,
                columns = uiState.gridColumns
            )
            uiState = if (updatedBeads === uiState.beads) {
                uiState.copy(
                    pendingLineStartIndex = null,
                    pendingLineEndIndex = null
                )
            } else {
                pushSnapshot()
                uiState.copy(
                    beads = updatedBeads,
                    pendingLineStartIndex = null,
                    pendingLineEndIndex = null
                )
            }
            return
        }

        val updatedBeads = if (uiState.interactionMode == InteractionMode.Fill) {
            fillConnectedRegion(
                beads = uiState.beads,
                index = index,
                replacementColor = nextColor,
                columns = uiState.gridColumns
            )
        } else {
            updateBeadAt(uiState.beads, index, nextColor)
        }
        if (updatedBeads === uiState.beads) return

        pushSnapshot()
        uiState = uiState.copy(
            beads = updatedBeads,
            pendingLineStartIndex = null,
            pendingLineEndIndex = null
        )
    }

    fun undo() {
        val snapshot = undoStack.removeLastOrNull() ?: return
        redoStack += currentSnapshot()
        uiState = uiState.copy(
            gridColumns = snapshot.gridColumns,
            gridRows = snapshot.gridRows,
            stitchModeId = snapshot.stitchModeId,
            beadShapeId = snapshot.beadShapeId,
            beads = snapshot.beads,
            pendingSettingsStitchId = snapshot.stitchModeId,
            pendingSettingsBeadShapeId = snapshot.beadShapeId,
            pendingSettingsGridColumns = snapshot.gridColumns.toFloat(),
            pendingSettingsGridRows = snapshot.gridRows.toFloat(),
            interactionMode = InteractionMode.Paint,
            brushSelected = false,
            pendingLineStartIndex = null,
            pendingLineEndIndex = null
        )
    }

    fun redo() {
        val snapshot = redoStack.removeLastOrNull() ?: return
        undoStack += currentSnapshot()
        uiState = uiState.copy(
            gridColumns = snapshot.gridColumns,
            gridRows = snapshot.gridRows,
            stitchModeId = snapshot.stitchModeId,
            beadShapeId = snapshot.beadShapeId,
            beads = snapshot.beads,
            pendingSettingsStitchId = snapshot.stitchModeId,
            pendingSettingsBeadShapeId = snapshot.beadShapeId,
            pendingSettingsGridColumns = snapshot.gridColumns.toFloat(),
            pendingSettingsGridRows = snapshot.gridRows.toFloat(),
            interactionMode = InteractionMode.Paint,
            brushSelected = false,
            pendingLineStartIndex = null,
            pendingLineEndIndex = null
        )
    }

    fun updatePendingStitch(id: String) {
        uiState = uiState.copy(pendingSettingsStitchId = StitchMode.fromId(id).id)
    }

    fun updatePendingBeadShape(id: String) {
        uiState = uiState.copy(pendingSettingsBeadShapeId = BeadShape.fromId(id).id)
    }

    fun updatePendingGridColumns(value: Float) {
        if (!value.isFinite()) return
        uiState = uiState.copy(
            pendingSettingsGridColumns = value.coerceIn(
                MinGridSize.toFloat(),
                MaxGridSize.toFloat()
            )
        )
    }

    fun updatePendingGridRows(value: Float) {
        if (!value.isFinite()) return
        uiState = uiState.copy(
            pendingSettingsGridRows = value.coerceIn(
                MinGridSize.toFloat(),
                MaxGridSize.toFloat()
            )
        )
    }

    fun updatePendingGridHorizontalResizeDirection(direction: GridHorizontalResizeDirection) {
        uiState = uiState.copy(pendingGridHorizontalResizeDirection = direction)
    }

    fun updatePendingGridVerticalResizeDirection(direction: GridVerticalResizeDirection) {
        uiState = uiState.copy(pendingGridVerticalResizeDirection = direction)
    }

    fun applyPendingGridSettings() {
        val updatedColumns = uiState.pendingSettingsGridColumns
            .finiteOr(uiState.gridColumns.toFloat())
            .toInt()
            .coerceIn(MinGridSize, MaxGridSize)
        val updatedRows = uiState.pendingSettingsGridRows
            .finiteOr(uiState.gridRows.toFloat())
            .toInt()
            .coerceIn(MinGridSize, MaxGridSize)
        val updatedStitchModeId = StitchMode.fromId(uiState.pendingSettingsStitchId).id
        val updatedBeadShapeId = BeadShape.fromId(uiState.pendingSettingsBeadShapeId).id
        val gridChanged = updatedColumns != uiState.gridColumns || updatedRows != uiState.gridRows
        val stitchChanged = updatedStitchModeId != uiState.stitchModeId
        val beadShapeChanged = updatedBeadShapeId != uiState.beadShapeId

        if (gridChanged || stitchChanged || beadShapeChanged) {
            pushSnapshot()
        }

        uiState = uiState.copy(
            gridColumns = updatedColumns,
            gridRows = updatedRows,
            stitchModeId = updatedStitchModeId,
            beadShapeId = updatedBeadShapeId,
            beads = if (gridChanged) {
                resizeBeadGrid(
                    beads = uiState.beads,
                    oldColumns = uiState.gridColumns,
                    oldRows = uiState.gridRows,
                    newColumns = updatedColumns,
                    newRows = updatedRows,
                    horizontalDirection = uiState.pendingGridHorizontalResizeDirection,
                    verticalDirection = uiState.pendingGridVerticalResizeDirection
                )
            } else {
                uiState.beads
            },
            showToolsDialog = false
        )
    }

    suspend fun savePattern(): Boolean {
        if (uiState.isPatternIoInProgress) return false

        val snapshot = currentSnapshot()
        uiState = uiState.copy(isPatternIoInProgress = true)
        return try {
            withContext(Dispatchers.IO) {
                patternStorage.save(snapshot)
            }
        } finally {
            uiState = uiState.copy(isPatternIoInProgress = false)
        }
    }

    suspend fun loadSavedPattern(): Boolean {
        if (uiState.isPatternIoInProgress) return false

        uiState = uiState.copy(isPatternIoInProgress = true)
        return try {
            val snapshot = withContext(Dispatchers.IO) {
                patternStorage.load()
            } ?: return false
            applySnapshot(snapshot)
            true
        } finally {
            uiState = uiState.copy(isPatternIoInProgress = false)
        }
    }

    suspend fun exportPatternToUri(uri: Uri): Boolean {
        if (uiState.isPatternIoInProgress) return false

        val snapshot = currentSnapshot()
        uiState = uiState.copy(isPatternIoInProgress = true)
        return try {
            withContext(Dispatchers.IO) {
                patternStorage.export(uri, snapshot)
            }
        } finally {
            uiState = uiState.copy(isPatternIoInProgress = false)
        }
    }

    fun suggestedExportFileName(): String {
        return patternStorage.suggestedExportFileName()
    }

    private fun replaceTemplateImage(newUriString: String?) {
        templateImageStorage.delete(uiState.templateImageUriString)
        uiState = uiState.copy(templateImageUriString = newUriString)
    }

    private fun pushSnapshot() {
        undoStack += currentSnapshot()
        redoStack.clear()
        if (undoStack.size > MaxUndoStackSize) {
            undoStack.removeAt(0)
        }
    }

    private fun currentSnapshot(): BoardSnapshot {
        return BoardSnapshot(
            gridColumns = uiState.gridColumns,
            gridRows = uiState.gridRows,
            stitchModeId = uiState.stitchModeId,
            beadShapeId = uiState.beadShapeId,
            beads = uiState.beads
        )
    }

    private fun applySnapshot(snapshot: BoardSnapshot) {
        undoStack.clear()
        redoStack.clear()
        uiState = uiState.copy(
            gridColumns = snapshot.gridColumns,
            gridRows = snapshot.gridRows,
            stitchModeId = snapshot.stitchModeId,
            beadShapeId = snapshot.beadShapeId,
            beads = snapshot.beads,
            pendingSettingsStitchId = snapshot.stitchModeId,
            pendingSettingsBeadShapeId = snapshot.beadShapeId,
            pendingSettingsGridColumns = snapshot.gridColumns.toFloat(),
            pendingSettingsGridRows = snapshot.gridRows.toFloat(),
            interactionMode = InteractionMode.Paint,
            brushSelected = false
        )
    }

    companion object {
        fun Saver(context: Context): Saver<BeadEditorState, Any> = Saver(
            save = { state -> serializeEditorUiState(state.uiState) },
            restore = { restored ->
                restoreEditorUiState(restored)?.let { restoredState ->
                    BeadEditorState(context = context, initialUiState = restoredState)
                }
            }
        )
    }
}

internal fun serializeEditorUiState(state: EditorUiState): String = buildString {
    fun appendField(key: String, value: Any?) {
        append(key)
        append('=')
        append(URLEncoder.encode(value?.toString().orEmpty(), Charsets.UTF_8.name()))
        append('\n')
    }

    appendField("version", SavedEditorStateVersion)
    appendField("gridColumns", state.gridColumns)
    appendField("gridRows", state.gridRows)
    appendField("stitchModeId", state.stitchModeId)
    appendField("beadShapeId", state.beadShapeId)
    appendField("beads", state.beads.joinToString(","))
    appendField("selectedColorIndex", state.selectedColorIndex)
    appendField("recentColorIndices", state.recentColorIndices.joinToString(","))
    appendField("eraserSelected", state.eraserSelected)
    appendField("templateImageUriString", state.templateImageUriString)
    appendField("templateOpacity", state.templateOpacity)
    appendField("templateScale", state.templateScale)
    appendField("templateOffsetX", state.templateOffsetX)
    appendField("templateOffsetY", state.templateOffsetY)
    appendField("templateRotation", state.templateRotation)
    appendField("boardScale", state.boardScale)
    appendField("boardOffsetX", state.boardOffsetX)
    appendField("boardOffsetY", state.boardOffsetY)
    appendField("interactionMode", state.interactionMode.id)
    appendField("brushSelected", state.brushSelected)
    appendField("pendingLineStartIndex", state.pendingLineStartIndex)
    appendField("pendingLineEndIndex", state.pendingLineEndIndex)
    appendField("showColorPickerDialog", state.showColorPickerDialog)
    appendField("showToolsDialog", state.showToolsDialog)
    appendField("selectedToolsTab", state.selectedToolsTab)
    appendField("pendingCameraUriString", state.pendingCameraUriString)
    appendField("pendingSettingsStitchId", state.pendingSettingsStitchId)
    appendField("pendingSettingsBeadShapeId", state.pendingSettingsBeadShapeId)
    appendField("pendingSettingsGridColumns", state.pendingSettingsGridColumns)
    appendField("pendingSettingsGridRows", state.pendingSettingsGridRows)
    appendField(
        "pendingGridHorizontalResizeDirection",
        state.pendingGridHorizontalResizeDirection.name
    )
    appendField(
        "pendingGridVerticalResizeDirection",
        state.pendingGridVerticalResizeDirection.name
    )
}

internal fun restoreEditorUiState(saved: Any): EditorUiState? = when (saved) {
    is String -> deserializeEditorUiState(saved)
    is List<*> -> restoreLegacyEditorUiState(saved)
    else -> null
}

internal fun deserializeEditorUiState(serialized: String): EditorUiState? {
    val values = runCatching {
        serialized.lineSequence()
            .filter { it.isNotEmpty() }
            .associate { line ->
                val separatorIndex = line.indexOf('=')
                require(separatorIndex > 0)
                line.substring(0, separatorIndex) to URLDecoder.decode(
                    line.substring(separatorIndex + 1),
                    Charsets.UTF_8.name()
                )
            }
    }.getOrNull() ?: return null

    val savedStateVersion = values["version"]?.toIntOrNull() ?: return null
    if (savedStateVersion !in FirstStringEditorStateVersion..SavedEditorStateVersion) return null

    val gridColumns = values["gridColumns"]?.toIntOrNull() ?: return null
    val gridRows = values["gridRows"]?.toIntOrNull() ?: return null
    val beads = parseSavedIntList(values["beads"]) ?: return null
    val selectedColorIndex = values["selectedColorIndex"]?.toIntOrNull() ?: 0
    val lineStart = values["pendingLineStartIndex"]?.toIntOrNull()
    val lineEnd = values["pendingLineEndIndex"]?.toIntOrNull()

    return sanitizeRestoredEditorUiState(
        EditorUiState(
            gridColumns = gridColumns,
            gridRows = gridRows,
            stitchModeId = values["stitchModeId"].orEmpty(),
            beadShapeId = values["beadShapeId"].orEmpty(),
            beads = beads,
            selectedColorIndex = selectedColorIndex,
            recentColorIndices = parseSavedIntList(values["recentColorIndices"])
                ?: listOf(selectedColorIndex),
            eraserSelected = values["eraserSelected"]?.toBooleanStrictOrNull() ?: false,
            templateImageUriString = values["templateImageUriString"].nullIfEmpty(),
            templateOpacity = values["templateOpacity"]?.toFloatOrNull()
                ?: DefaultTemplateOpacity,
            templateScale = values["templateScale"]?.toFloatOrNull() ?: DefaultTemplateScale,
            templateOffsetX = values["templateOffsetX"]?.toFloatOrNull() ?: 0f,
            templateOffsetY = values["templateOffsetY"]?.toFloatOrNull() ?: 0f,
            templateRotation = values["templateRotation"]?.toFloatOrNull()
                ?: DefaultTemplateRotation,
            boardScale = values["boardScale"]?.toFloatOrNull() ?: DefaultBoardScale,
            boardOffsetX = values["boardOffsetX"]?.toFloatOrNull() ?: 0f,
            boardOffsetY = values["boardOffsetY"]?.toFloatOrNull() ?: 0f,
            interactionMode = if (savedStateVersion >= SavedEditorStateVersion) {
                InteractionMode.fromId(values["interactionMode"].orEmpty())
            } else {
                normalizeSavedInteractionMode(
                    savedValue = values["interactionMode"]?.toIntOrNull() ?: 0,
                    schemaVersion = savedStateVersion
                )
            },
            brushSelected = values["brushSelected"]?.toBooleanStrictOrNull() ?: false,
            pendingLineStartIndex = lineStart,
            pendingLineEndIndex = lineEnd,
            showColorPickerDialog = values["showColorPickerDialog"]?.toBooleanStrictOrNull()
                ?: false,
            showToolsDialog = values["showToolsDialog"]?.toBooleanStrictOrNull() ?: false,
            selectedToolsTab = values["selectedToolsTab"]?.toIntOrNull() ?: 0,
            pendingCameraUriString = values["pendingCameraUriString"].nullIfEmpty(),
            pendingSettingsStitchId = values["pendingSettingsStitchId"].orEmpty(),
            pendingSettingsBeadShapeId = values["pendingSettingsBeadShapeId"].orEmpty(),
            pendingSettingsGridColumns = values["pendingSettingsGridColumns"]?.toFloatOrNull()
                ?: gridColumns.toFloat(),
            pendingSettingsGridRows = values["pendingSettingsGridRows"]?.toFloatOrNull()
                ?: gridRows.toFloat(),
            pendingGridHorizontalResizeDirection = values["pendingGridHorizontalResizeDirection"]
                ?.let { runCatching { GridHorizontalResizeDirection.valueOf(it) }.getOrNull() }
                ?: GridHorizontalResizeDirection.Right,
            pendingGridVerticalResizeDirection = values["pendingGridVerticalResizeDirection"]
                ?.let { runCatching { GridVerticalResizeDirection.valueOf(it) }.getOrNull() }
                ?: GridVerticalResizeDirection.Bottom
        )
    )
}

private fun restoreLegacyEditorUiState(restored: List<*>): EditorUiState? {
    val schemaVersion = (restored.lastOrNull() as? Number)?.toInt()?.takeIf { it in 2..4 }
    val versionFourLayout = schemaVersion == 4
    val gridColumns = (restored.getOrNull(0) as? Number)?.toInt() ?: return null
    val gridRows = (restored.getOrNull(1) as? Number)?.toInt() ?: return null
    val beads = (restored.getOrNull(3) as? List<*>)?.map { value ->
        (value as? Number)?.toInt() ?: return null
    } ?: return null
    val selectedColorIndex = (restored.getOrNull(4) as? Number)?.toInt() ?: 0
    fun valueAt(versionFourIndex: Int, legacyIndex: Int): Any? =
        restored.getOrNull(if (versionFourLayout) versionFourIndex else legacyIndex)

    return sanitizeRestoredEditorUiState(
        EditorUiState(
            gridColumns = gridColumns,
            gridRows = gridRows,
            stitchModeId = restored.getOrNull(2) as? String ?: StitchMode.defaults.id,
            beadShapeId = valueAt(24, 23) as? String ?: BeadShape.defaults.id,
            beads = beads,
            selectedColorIndex = selectedColorIndex,
            recentColorIndices = (valueAt(28, 27) as? List<*>)?.mapNotNull { value ->
                (value as? Number)?.toInt()
            } ?: listOf(selectedColorIndex),
            eraserSelected = restored.getOrNull(5) as? Boolean ?: false,
            templateImageUriString = restored.getOrNull(6) as? String,
            templateOpacity = (restored.getOrNull(7) as? Number)?.toFloat()
                ?: DefaultTemplateOpacity,
            templateScale = (restored.getOrNull(8) as? Number)?.toFloat()
                ?: DefaultTemplateScale,
            templateOffsetX = (restored.getOrNull(9) as? Number)?.toFloat() ?: 0f,
            templateOffsetY = (restored.getOrNull(10) as? Number)?.toFloat() ?: 0f,
            templateRotation = (valueAt(29, 28) as? Number)?.toFloat()
                ?: DefaultTemplateRotation,
            boardScale = (restored.getOrNull(11) as? Number)?.toFloat() ?: DefaultBoardScale,
            boardOffsetX = (restored.getOrNull(12) as? Number)?.toFloat() ?: 0f,
            boardOffsetY = (restored.getOrNull(13) as? Number)?.toFloat() ?: 0f,
            interactionMode = normalizeSavedInteractionMode(
                savedValue = (restored.getOrNull(14) as? Number)?.toInt()
                    ?: 0,
                schemaVersion = schemaVersion
            ),
            brushSelected = if (versionFourLayout) {
                restored.getOrNull(15) as? Boolean ?: false
            } else {
                false
            },
            pendingLineStartIndex = (valueAt(16, 15) as? Number)?.toInt(),
            showColorPickerDialog = valueAt(17, 16) as? Boolean ?: false,
            showToolsDialog = valueAt(18, 17) as? Boolean ?: false,
            selectedToolsTab = (valueAt(19, 18) as? Number)?.toInt() ?: 0,
            pendingCameraUriString = valueAt(20, 19) as? String,
            pendingSettingsStitchId = valueAt(21, 20) as? String ?: StitchMode.defaults.id,
            pendingSettingsGridColumns = (valueAt(22, 21) as? Number)?.toFloat()
                ?: gridColumns.toFloat(),
            pendingSettingsGridRows = (valueAt(23, 22) as? Number)?.toFloat()
                ?: gridRows.toFloat(),
            pendingSettingsBeadShapeId = valueAt(25, 24) as? String
                ?: BeadShape.defaults.id,
            pendingGridHorizontalResizeDirection = (valueAt(26, 25) as? String)?.let {
                runCatching { GridHorizontalResizeDirection.valueOf(it) }.getOrNull()
            } ?: GridHorizontalResizeDirection.Right,
            pendingGridVerticalResizeDirection = (valueAt(27, 26) as? String)?.let {
                runCatching { GridVerticalResizeDirection.valueOf(it) }.getOrNull()
            } ?: GridVerticalResizeDirection.Bottom
        )
    )
}

private fun sanitizeRestoredEditorUiState(state: EditorUiState): EditorUiState? {
    if (state.gridColumns !in MinGridSize..MaxGridSize || state.gridRows !in MinGridSize..MaxGridSize) {
        return null
    }
    if (
        state.beads.size != state.gridColumns * state.gridRows ||
        state.beads.any { it !in EmptyBead until PaletteColorCount }
    ) {
        return null
    }

    return normalizeEditorUiState(state).copy(
        isCreatingPattern = false,
        isImportingTemplate = false,
        isPatternIoInProgress = false
    )
}

internal fun normalizeEditorUiState(state: EditorUiState): EditorUiState {
    val gridColumns = state.gridColumns.coerceIn(MinGridSize, MaxGridSize)
    val gridRows = state.gridRows.coerceIn(MinGridSize, MaxGridSize)
    val expectedBeadCount = gridColumns * gridRows
    val beads = List(expectedBeadCount) { index ->
        state.beads.getOrNull(index)
            ?.takeIf { it in EmptyBead until PaletteColorCount }
            ?: EmptyBead
    }
    val selectedColorIndex = state.selectedColorIndex.takeIf {
        it in 0 until PaletteColorCount
    } ?: 0
    val validRecentColors = state.recentColorIndices
        .filter { it in 0 until PaletteColorCount }
        .distinct()
        .take(MaxRecentColors)
    val recentColors = updateRecentColors(
        existing = validRecentColors,
        selectedIndex = selectedColorIndex,
        maxSize = MaxRecentColors
    )
    val lineStart = state.pendingLineStartIndex?.takeIf { it in beads.indices }
    val lineEnd = state.pendingLineEndIndex?.takeIf {
        lineStart != null && it in beads.indices
    }
    val templateImageUriString = state.templateImageUriString.nullIfEmpty()
    val interactionMode = state.interactionMode.let { mode ->
        if (mode == InteractionMode.Template && templateImageUriString == null) {
            InteractionMode.Paint
        } else {
            mode
        }
    }

    return state.copy(
        gridColumns = gridColumns,
        gridRows = gridRows,
        stitchModeId = StitchMode.fromId(state.stitchModeId).id,
        beadShapeId = BeadShape.fromId(state.beadShapeId).id,
        beads = beads,
        selectedColorIndex = selectedColorIndex,
        recentColorIndices = recentColors,
        templateImageUriString = templateImageUriString,
        templateOpacity = state.templateOpacity.finiteOr(DefaultTemplateOpacity).coerceIn(
            MinTemplateOpacity,
            1f
        ),
        templateScale = state.templateScale.finiteOr(DefaultTemplateScale).coerceIn(
            MinTemplateScale,
            MaxTemplateScale
        ),
        templateOffsetX = state.templateOffsetX.finiteOr(0f),
        templateOffsetY = state.templateOffsetY.finiteOr(0f),
        templateRotation = normalizeRotationDegrees(
            state.templateRotation.finiteOr(DefaultTemplateRotation)
        ),
        boardScale = state.boardScale.finiteOr(DefaultBoardScale).coerceIn(
            MinBoardScale,
            MaxBoardScale
        ),
        boardOffsetX = state.boardOffsetX.finiteOr(0f),
        boardOffsetY = state.boardOffsetY.finiteOr(0f),
        interactionMode = interactionMode,
        brushSelected = state.brushSelected && interactionMode == InteractionMode.Paint,
        pendingLineStartIndex = lineStart,
        pendingLineEndIndex = lineEnd,
        selectedToolsTab = state.selectedToolsTab.coerceIn(0, MaxToolsTabIndex),
        pendingCameraUriString = state.pendingCameraUriString.nullIfEmpty(),
        pendingSettingsStitchId = StitchMode.fromId(state.pendingSettingsStitchId).id,
        pendingSettingsBeadShapeId = BeadShape.fromId(state.pendingSettingsBeadShapeId).id,
        pendingSettingsGridColumns = state.pendingSettingsGridColumns
            .finiteOr(gridColumns.toFloat())
            .coerceIn(MinGridSize.toFloat(), MaxGridSize.toFloat()),
        pendingSettingsGridRows = state.pendingSettingsGridRows
            .finiteOr(gridRows.toFloat())
            .coerceIn(MinGridSize.toFloat(), MaxGridSize.toFloat())
    )
}

private fun parseSavedIntList(value: String?): List<Int>? {
    if (value == null) return null
    if (value.isEmpty()) return emptyList()
    return value.split(',').map { token -> token.toIntOrNull() ?: return null }
}

private fun String?.nullIfEmpty(): String? = this?.takeIf { it.isNotEmpty() }

private fun Float.finiteOr(defaultValue: Float): Float = if (isFinite()) this else defaultValue

@Composable
fun rememberBeadEditorState(
    context: Context = LocalContext.current
): BeadEditorState {
    return rememberSaveable(saver = BeadEditorState.Saver(context)) {
        BeadEditorState(context)
    }
}

fun normalizeRotationDegrees(degrees: Float): Float {
    if (!degrees.isFinite()) return DefaultTemplateRotation
    var normalized = degrees % 360f
    if (normalized > 180f) {
        normalized -= 360f
    } else if (normalized <= -180f) {
        normalized += 360f
    }
    return normalized
}

fun resizeBeadGrid(
    beads: List<Int>,
    oldColumns: Int,
    oldRows: Int,
    newColumns: Int,
    newRows: Int,
    horizontalDirection: GridHorizontalResizeDirection = GridHorizontalResizeDirection.Right,
    verticalDirection: GridVerticalResizeDirection = GridVerticalResizeDirection.Bottom
): List<Int> {
    val newSize = newColumns.toLong() * newRows
    if (newColumns <= 0 || newRows <= 0 || newSize > Int.MAX_VALUE) return emptyList()
    val resizedBeads = MutableList(newSize.toInt()) { EmptyBead }
    if (oldColumns <= 0 || oldRows <= 0) return resizedBeads
    val preservedColumns = minOf(oldColumns, newColumns)
    val preservedRows = minOf(oldRows, newRows)
    val sourceStartColumn = when (horizontalDirection) {
        GridHorizontalResizeDirection.Right -> 0
        GridHorizontalResizeDirection.Left -> oldColumns - preservedColumns
    }
    val targetStartColumn = when (horizontalDirection) {
        GridHorizontalResizeDirection.Right -> 0
        GridHorizontalResizeDirection.Left -> newColumns - preservedColumns
    }
    val sourceStartRow = when (verticalDirection) {
        GridVerticalResizeDirection.Bottom -> 0
        GridVerticalResizeDirection.Top -> oldRows - preservedRows
    }
    val targetStartRow = when (verticalDirection) {
        GridVerticalResizeDirection.Bottom -> 0
        GridVerticalResizeDirection.Top -> newRows - preservedRows
    }

    repeat(preservedRows) { rowIndex ->
        repeat(preservedColumns) { columnIndex ->
            val sourceIndex =
                (sourceStartRow + rowIndex) * oldColumns + (sourceStartColumn + columnIndex)
            val targetIndex =
                (targetStartRow + rowIndex) * newColumns + (targetStartColumn + columnIndex)
            resizedBeads[targetIndex] = beads.getOrNull(sourceIndex)
                ?.takeIf(::isValidBeadColor)
                ?: EmptyBead
        }
    }

    return resizedBeads
}

fun updateRecentColors(
    existing: List<Int>,
    selectedIndex: Int,
    maxSize: Int = MaxRecentColors
): List<Int> {
    if (maxSize <= 0 || selectedIndex !in 0 until PaletteColorCount) return existing
    return buildList {
        add(selectedIndex)
        existing.forEach { index ->
            if (index in 0 until PaletteColorCount && index != selectedIndex && size < maxSize) {
                add(index)
            }
        }
    }
}

fun isValidBeadColor(colorIndex: Int): Boolean {
    return colorIndex in EmptyBead until PaletteColorCount
}

fun updateBeadAt(
    beads: List<Int>,
    index: Int,
    nextColor: Int
): List<Int> {
    if (index !in beads.indices || !isValidBeadColor(nextColor) || beads[index] == nextColor) {
        return beads
    }
    return beads.toMutableList().apply {
        this[index] = nextColor
    }
}

fun updateBeadsAt(
    beads: List<Int>,
    indices: List<Int>,
    nextColor: Int
): List<Int> {
    if (indices.isEmpty() || !isValidBeadColor(nextColor)) return beads

    var changed = false
    val updated = beads.toMutableList()
    indices.forEach { index ->
        if (index in updated.indices && updated[index] != nextColor) {
            updated[index] = nextColor
            changed = true
        }
    }
    return if (changed) updated else beads
}

fun fillConnectedRegion(
    beads: List<Int>,
    index: Int,
    replacementColor: Int,
    columns: Int
): List<Int> {
    if (index !in beads.indices || columns <= 0 || !isValidBeadColor(replacementColor)) {
        return beads
    }

    val targetColor = beads[index]
    if (targetColor == replacementColor) return beads

    val rows = beads.size / columns
    val updatedBeads = beads.toMutableList()
    val pending = ArrayDeque<Int>()
    pending.add(index)
    updatedBeads[index] = replacementColor

    while (pending.isNotEmpty()) {
        val currentIndex = pending.removeFirst()
        val row = currentIndex / columns
        val column = currentIndex % columns

        fun enqueueIfMatch(candidateIndex: Int) {
            if (candidateIndex in beads.indices && updatedBeads[candidateIndex] == targetColor) {
                updatedBeads[candidateIndex] = replacementColor
                pending.add(candidateIndex)
            }
        }

        if (column > 0) enqueueIfMatch(currentIndex - 1)
        if (column < columns - 1) enqueueIfMatch(currentIndex + 1)
        if (row > 0) enqueueIfMatch(currentIndex - columns)
        if (row < rows - 1) enqueueIfMatch(currentIndex + columns)
    }

    return updatedBeads
}

fun drawLineOnGrid(
    beads: List<Int>,
    startIndex: Int,
    endIndex: Int,
    replacementColor: Int,
    columns: Int
): List<Int> {
    if (
        startIndex !in beads.indices ||
        endIndex !in beads.indices ||
        columns <= 0 ||
        !isValidBeadColor(replacementColor)
    ) {
        return beads
    }

    val lineIndices = calculateLineIndices(
        startIndex = startIndex,
        endIndex = endIndex,
        columns = columns,
        maxIndex = beads.lastIndex
    )
    val updatedBeads = beads.toMutableList()
    var changed = false

    lineIndices.forEach { index ->
        if (index in updatedBeads.indices && updatedBeads[index] != replacementColor) {
            updatedBeads[index] = replacementColor
            changed = true
        }
    }

    return if (changed) updatedBeads else beads
}

fun calculateLineIndices(
    startIndex: Int,
    endIndex: Int,
    columns: Int,
    maxIndex: Int
): List<Int> {
    if (startIndex !in 0..maxIndex || endIndex !in 0..maxIndex || columns <= 0) return emptyList()

    val x0 = startIndex % columns
    val y0 = startIndex / columns
    val x1 = endIndex % columns
    val y1 = endIndex / columns

    val steps = maxOf(kotlin.math.abs(x1 - x0), kotlin.math.abs(y1 - y0))
    if (steps == 0) return listOf(startIndex)

    return buildList {
        repeat(steps + 1) { step ->
            val progress = step / steps.toFloat()
            val x = kotlin.math.round(x0 + (x1 - x0) * progress).toInt()
            val y = kotlin.math.round(y0 + (y1 - y0) * progress).toInt()
            val index = y * columns + x
            if (index in 0..maxIndex && (isEmpty() || last() != index)) {
                add(index)
            }
        }
    }
}

fun normalizeSavedInteractionMode(
    savedValue: Int,
    schemaVersion: Int?
): InteractionMode = when (schemaVersion) {
    null, 1 -> when (savedValue) {
        1 -> InteractionMode.Template
        2 -> InteractionMode.Grid
        else -> InteractionMode.Paint
    }
    2 -> when (savedValue) {
        1 -> InteractionMode.Fill
        2 -> InteractionMode.Template
        3 -> InteractionMode.Grid
        else -> InteractionMode.Paint
    }
    else -> when (savedValue) {
        0 -> InteractionMode.Paint
        1 -> InteractionMode.Fill
        2 -> InteractionMode.Line
        3 -> InteractionMode.Template
        4 -> InteractionMode.Grid
        else -> InteractionMode.Paint
    }
}

fun isGridEmpty(beads: List<Int>): Boolean = beads.all { it == EmptyBead }
