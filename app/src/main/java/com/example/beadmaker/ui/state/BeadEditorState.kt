package com.example.beadmaker.ui.state

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.example.beadmaker.ui.model.BeadShape
import com.example.beadmaker.ui.model.StitchMode
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

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
const val MinBoardScale = 1.0f
const val MaxBoardScale = 6.0f
const val DefaultBoardScale = 1.0f
const val InteractionModePaint = 0
const val InteractionModeTemplate = 1
const val InteractionModeGrid = 2

private const val MaxUndoStackSize = 30
private const val SavedPatternFileName = "saved_pattern.bm"
private const val PatternFormatVersion = 1
private const val MaxRecentColors = 6

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
    val boardScale: Float = DefaultBoardScale,
    val boardOffsetX: Float = 0f,
    val boardOffsetY: Float = 0f,
    val interactionMode: Int = InteractionModePaint,
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
        GridVerticalResizeDirection.Bottom
)

data class BoardSnapshot(
    val gridColumns: Int,
    val gridRows: Int,
    val stitchModeId: String,
    val beadShapeId: String,
    val beads: List<Int>
)

class BeadEditorState(
    context: Context,
    initialUiState: EditorUiState = EditorUiState()
) {
    private val appContext = context.applicationContext
    private val undoStack = mutableStateListOf<BoardSnapshot>()
    private val redoStack = mutableStateListOf<BoardSnapshot>()

    var uiState by mutableStateOf(initialUiState)
        private set

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    val isGridEmpty: Boolean
        get() = isGridEmpty(uiState.beads)

    fun toggleEraser() {
        uiState = uiState.copy(eraserSelected = !uiState.eraserSelected)
    }

    fun showColorPicker() {
        uiState = uiState.copy(showColorPickerDialog = true)
    }

    fun dismissColorPicker() {
        uiState = uiState.copy(showColorPickerDialog = false)
    }

    fun applySelectedColor(index: Int) {
        uiState = uiState.copy(
            selectedColorIndex = index,
            recentColorIndices = updateRecentColors(uiState.recentColorIndices, index),
            eraserSelected = false,
            showColorPickerDialog = false
        )
    }

    fun setPaintMode() {
        uiState = uiState.copy(interactionMode = InteractionModePaint)
    }

    fun toggleTemplateMode() {
        if (uiState.templateImageUriString == null) return
        uiState = uiState.copy(
            interactionMode = if (uiState.interactionMode == InteractionModeTemplate) {
                InteractionModePaint
            } else {
                InteractionModeTemplate
            }
        )
    }

    fun toggleGridMode() {
        uiState = uiState.copy(
            interactionMode = if (uiState.interactionMode == InteractionModeGrid) {
                InteractionModePaint
            } else {
                InteractionModeGrid
            }
        )
    }

    fun openToolsDialogAtTab(tabIndex: Int) {
        uiState = uiState.copy(
            selectedToolsTab = tabIndex,
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

    fun selectToolsTab(tabIndex: Int) {
        uiState = uiState.copy(selectedToolsTab = tabIndex)
    }

    fun updateTemplateOpacity(value: Float) {
        uiState = uiState.copy(templateOpacity = value)
    }

    fun updateTemplateTransform(panX: Float, panY: Float, zoom: Float) {
        uiState = uiState.copy(
            templateScale = (uiState.templateScale * zoom).coerceIn(MinTemplateScale, MaxTemplateScale),
            templateOffsetX = uiState.templateOffsetX + panX,
            templateOffsetY = uiState.templateOffsetY + panY
        )
    }

    fun updateBoardTransform(panX: Float, panY: Float, zoom: Float) {
        uiState = uiState.copy(
            boardScale = (uiState.boardScale * zoom).coerceIn(MinBoardScale, MaxBoardScale),
            boardOffsetX = uiState.boardOffsetX + panX,
            boardOffsetY = uiState.boardOffsetY + panY
        )
    }

    fun resetTemplateTransform() {
        uiState = uiState.copy(
            templateScale = DefaultTemplateScale,
            templateOffsetX = 0f,
            templateOffsetY = 0f
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
            boardScale = DefaultBoardScale,
            boardOffsetX = 0f,
            boardOffsetY = 0f,
            interactionMode = InteractionModePaint
        )
    }

    fun importTemplateFromPicker(uri: Uri) {
        copyTemplateImageToCache(appContext, uri)?.let { cachedUri ->
            replaceTemplateImage(cachedUri.toString())
            resetTemplateAndBoardAdjustments()
        }
    }

    fun prepareCameraTemplateCapture(uriString: String) {
        uiState = uiState.copy(pendingCameraUriString = uriString)
    }

    fun finishCameraTemplateCapture(success: Boolean) {
        val pendingUriString = uiState.pendingCameraUriString
        if (success && pendingUriString != null) {
            replaceTemplateImage(pendingUriString)
            resetTemplateAndBoardAdjustments()
        } else {
            deleteTemplateCacheFile(appContext, pendingUriString)
        }
        uiState = uiState.copy(pendingCameraUriString = null)
    }

    fun removeTemplateImage() {
        replaceTemplateImage(null)
        uiState = uiState.copy(
            templateScale = DefaultTemplateScale,
            templateOffsetX = 0f,
            templateOffsetY = 0f,
            interactionMode = if (uiState.interactionMode == InteractionModeTemplate) {
                InteractionModePaint
            } else {
                uiState.interactionMode
            }
        )
    }

    fun clearGrid() {
        if (isGridEmpty) return
        pushSnapshot()
        uiState = uiState.copy(
            beads = List(uiState.gridColumns * uiState.gridRows) { EmptyBead }
        )
    }

    fun paintCell(index: Int) {
        if (uiState.interactionMode != InteractionModePaint) return

        val nextColor = if (uiState.eraserSelected) {
            EmptyBead
        } else {
            uiState.selectedColorIndex
        }
        val updatedBeads = updateBeadAt(uiState.beads, index, nextColor)
        if (updatedBeads === uiState.beads) return

        pushSnapshot()
        uiState = uiState.copy(beads = updatedBeads)
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
            interactionMode = InteractionModePaint
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
            interactionMode = InteractionModePaint
        )
    }

    fun updatePendingStitch(id: String) {
        uiState = uiState.copy(pendingSettingsStitchId = id)
    }

    fun updatePendingBeadShape(id: String) {
        uiState = uiState.copy(pendingSettingsBeadShapeId = id)
    }

    fun updatePendingGridColumns(value: Float) {
        uiState = uiState.copy(pendingSettingsGridColumns = value)
    }

    fun updatePendingGridRows(value: Float) {
        uiState = uiState.copy(pendingSettingsGridRows = value)
    }

    fun updatePendingGridHorizontalResizeDirection(direction: GridHorizontalResizeDirection) {
        uiState = uiState.copy(pendingGridHorizontalResizeDirection = direction)
    }

    fun updatePendingGridVerticalResizeDirection(direction: GridVerticalResizeDirection) {
        uiState = uiState.copy(pendingGridVerticalResizeDirection = direction)
    }

    fun applyPendingGridSettings() {
        val updatedColumns = uiState.pendingSettingsGridColumns.toInt()
        val updatedRows = uiState.pendingSettingsGridRows.toInt()
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

    fun savePattern(): Boolean {
        return runCatching {
            File(appContext.filesDir, SavedPatternFileName).writeText(
                text = serializeBoardSnapshot(currentSnapshot()),
                charset = Charsets.UTF_8
            )
            true
        }.getOrDefault(false)
    }

    fun loadSavedPattern(): Boolean {
        val saveFile = File(appContext.filesDir, SavedPatternFileName)
        if (!saveFile.exists()) return false
        val snapshot = runCatching {
            deserializeBoardSnapshot(saveFile.readText(Charsets.UTF_8))
        }.getOrNull() ?: return false

        applySnapshot(snapshot)
        return true
    }

    fun exportPatternToUri(uri: Uri): Boolean {
        val payload = serializeBoardSnapshot(currentSnapshot())
        return runCatching {
            val stream = appContext.contentResolver.openOutputStream(uri) ?: return@runCatching false
            stream.bufferedWriter(Charsets.UTF_8).use {
                it.write(payload)
            }
            true
        }.getOrDefault(false)
    }

    fun suggestedExportFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "bead_pattern_$timestamp.bm"
    }

    private fun replaceTemplateImage(newUriString: String?) {
        deleteTemplateCacheFile(appContext, uiState.templateImageUriString)
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
            interactionMode = InteractionModePaint
        )
    }

    companion object {
        fun Saver(context: Context): Saver<BeadEditorState, Any> = listSaver(
            save = { state ->
                listOf(
                    state.uiState.gridColumns,
                    state.uiState.gridRows,
                    state.uiState.stitchModeId,
                    ArrayList(state.uiState.beads),
                    state.uiState.selectedColorIndex,
                    state.uiState.eraserSelected,
                    state.uiState.templateImageUriString,
                    state.uiState.templateOpacity,
                    state.uiState.templateScale,
                    state.uiState.templateOffsetX,
                    state.uiState.templateOffsetY,
                    state.uiState.boardScale,
                    state.uiState.boardOffsetX,
                    state.uiState.boardOffsetY,
                    state.uiState.interactionMode,
                    state.uiState.showColorPickerDialog,
                    state.uiState.showToolsDialog,
                    state.uiState.selectedToolsTab,
                    state.uiState.pendingCameraUriString,
                    state.uiState.pendingSettingsStitchId,
                    state.uiState.pendingSettingsGridColumns,
                    state.uiState.pendingSettingsGridRows,
                    state.uiState.beadShapeId,
                    state.uiState.pendingSettingsBeadShapeId,
                    state.uiState.pendingGridHorizontalResizeDirection.name,
                    state.uiState.pendingGridVerticalResizeDirection.name,
                    ArrayList(state.uiState.recentColorIndices)
                )
            },
            restore = { restored ->
                @Suppress("UNCHECKED_CAST")
                BeadEditorState(
                    context = context,
                    initialUiState = EditorUiState(
                        gridColumns = restored[0] as Int,
                        gridRows = restored[1] as Int,
                        stitchModeId = restored[2] as String,
                        beadShapeId = restored.getOrNull(22) as? String ?: BeadShape.defaults.id,
                        beads = restored[3] as ArrayList<Int>,
                        selectedColorIndex = restored[4] as Int,
                        recentColorIndices = ((restored.getOrNull(26) as? ArrayList<*>)?.mapNotNull {
                            (it as? Int)?.takeIf { index -> index >= 0 }
                        }?.distinct()?.take(MaxRecentColors)?.takeIf { it.isNotEmpty() })
                            ?: listOf((restored[4] as Int).coerceAtLeast(0)),
                        eraserSelected = restored[5] as Boolean,
                        templateImageUriString = restored[6] as String?,
                        templateOpacity = restored[7] as Float,
                        templateScale = restored[8] as Float,
                        templateOffsetX = restored[9] as Float,
                        templateOffsetY = restored[10] as Float,
                        boardScale = restored[11] as Float,
                        boardOffsetX = restored[12] as Float,
                        boardOffsetY = restored[13] as Float,
                        interactionMode = restored[14] as Int,
                        showColorPickerDialog = restored[15] as Boolean,
                        showToolsDialog = restored[16] as Boolean,
                        selectedToolsTab = restored[17] as Int,
                        pendingCameraUriString = restored[18] as String?,
                        pendingSettingsStitchId = restored[19] as String,
                        pendingSettingsGridColumns = restored[20] as Float,
                        pendingSettingsGridRows = restored[21] as Float,
                        pendingSettingsBeadShapeId = restored.getOrNull(23) as? String
                            ?: BeadShape.defaults.id,
                        pendingGridHorizontalResizeDirection =
                            (restored.getOrNull(24) as? String)?.let {
                                runCatching { GridHorizontalResizeDirection.valueOf(it) }.getOrNull()
                            } ?: GridHorizontalResizeDirection.Right,
                        pendingGridVerticalResizeDirection =
                            (restored.getOrNull(25) as? String)?.let {
                                runCatching { GridVerticalResizeDirection.valueOf(it) }.getOrNull()
                            } ?: GridVerticalResizeDirection.Bottom
                    )
                )
            }
        )
    }
}

@Composable
fun rememberBeadEditorState(
    context: Context = LocalContext.current
): BeadEditorState {
    return rememberSaveable(saver = BeadEditorState.Saver(context)) {
        BeadEditorState(context)
    }
}

fun createTemplateCaptureUri(context: Context): Uri? {
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

fun copyTemplateImageToCache(context: Context, sourceUri: Uri): Uri? {
    return runCatching {
        val imageDirectory = File(context.cacheDir, "template_images").apply {
            if (!exists() && !mkdirs()) {
                throw IOException("Failed to create template_images cache directory.")
            }
        }
        val imageFile = File(imageDirectory, "template_${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            imageFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw IOException("Failed to open template image stream.")
        imageFile.toUri()
    }.getOrNull()
}

fun deleteTemplateCacheFile(context: Context, uriString: String?) {
    val cachedTemplateUri = uriString?.let(Uri::parse) ?: return
    val cachedTemplateFile = cachedTemplateUri.path?.let(::File) ?: return
    val cacheDirectory = File(context.cacheDir, "template_images")
    if (cachedTemplateFile.parentFile == cacheDirectory && cachedTemplateFile.exists()) {
        cachedTemplateFile.delete()
    }
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
    val resizedBeads = MutableList(newColumns * newRows) { EmptyBead }
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
            resizedBeads[targetIndex] = beads[sourceIndex]
        }
    }

    return resizedBeads
}

fun updateRecentColors(
    existing: List<Int>,
    selectedIndex: Int,
    maxSize: Int = MaxRecentColors
): List<Int> {
    if (maxSize <= 0 || selectedIndex < 0) return existing
    return buildList {
        add(selectedIndex)
        existing.forEach { index ->
            if (index >= 0 && index != selectedIndex && size < maxSize) {
                add(index)
            }
        }
    }
}

fun updateBeadAt(
    beads: List<Int>,
    index: Int,
    nextColor: Int
): List<Int> {
    if (index !in beads.indices || beads[index] == nextColor) return beads
    return beads.toMutableList().apply {
        this[index] = nextColor
    }
}

fun serializeBoardSnapshot(snapshot: BoardSnapshot): String {
    val serializedBeads = snapshot.beads.joinToString(",")
    return buildString {
        appendLine("beadmaker_format=$PatternFormatVersion")
        appendLine("grid_columns=${snapshot.gridColumns}")
        appendLine("grid_rows=${snapshot.gridRows}")
        appendLine("stitch_mode_id=${snapshot.stitchModeId}")
        appendLine("bead_shape_id=${snapshot.beadShapeId}")
        append("beads=$serializedBeads")
    }
}

fun deserializeBoardSnapshot(serialized: String): BoardSnapshot? {
    val values = serialized
        .lineSequence()
        .mapNotNull { line ->
            val separatorIndex = line.indexOf('=')
            if (separatorIndex <= 0) {
                null
            } else {
                val key = line.substring(0, separatorIndex).trim()
                val value = line.substring(separatorIndex + 1).trim()
                key to value
            }
        }
        .toMap()

    if (values["beadmaker_format"]?.toIntOrNull() != PatternFormatVersion) {
        return null
    }

    val gridColumns = values["grid_columns"]?.toIntOrNull() ?: return null
    val gridRows = values["grid_rows"]?.toIntOrNull() ?: return null
    if (gridColumns !in MinGridSize..MaxGridSize || gridRows !in MinGridSize..MaxGridSize) {
        return null
    }

    val stitchModeId = StitchMode.fromId(values["stitch_mode_id"].orEmpty()).id
    val beadShapeId = BeadShape.fromId(values["bead_shape_id"].orEmpty()).id
    val beadsText = values["beads"] ?: return null
    val beads = beadsText.split(',').map { token ->
        token.toIntOrNull() ?: return null
    }
    if (beads.size != gridColumns * gridRows || beads.any { it < EmptyBead }) {
        return null
    }

    return BoardSnapshot(
        gridColumns = gridColumns,
        gridRows = gridRows,
        stitchModeId = stitchModeId,
        beadShapeId = beadShapeId,
        beads = beads
    )
}

fun isGridEmpty(beads: List<Int>): Boolean = beads.all { it == EmptyBead }
