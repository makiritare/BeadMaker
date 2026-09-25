package com.example.beadmaker.ui.state

import com.example.beadmaker.ui.model.BeadShape
import com.example.beadmaker.ui.model.InteractionMode
import com.example.beadmaker.ui.model.StitchMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class BeadEditorStateTest {

    @Test
    fun resizeBeadGrid_preservesOverlappingCells() {
        val beads = listOf(
            1, 2, 3,
            4, 5, 6
        )

        val resized = resizeBeadGrid(
            beads = beads,
            oldColumns = 3,
            oldRows = 2,
            newColumns = 4,
            newRows = 3
        )

        assertEquals(
            listOf(
                1, 2, 3, EmptyBead,
                4, 5, 6, EmptyBead,
                EmptyBead, EmptyBead, EmptyBead, EmptyBead
            ),
            resized
        )
    }

    @Test
    fun resizeBeadGrid_addsColumnsOnLeft_whenRequested() {
        val beads = listOf(
            1, 2, 3,
            4, 5, 6
        )

        val resized = resizeBeadGrid(
            beads = beads,
            oldColumns = 3,
            oldRows = 2,
            newColumns = 4,
            newRows = 2,
            horizontalDirection = GridHorizontalResizeDirection.Left
        )

        assertEquals(
            listOf(
                EmptyBead, 1, 2, 3,
                EmptyBead, 4, 5, 6
            ),
            resized
        )
    }

    @Test
    fun resizeBeadGrid_addsRowsOnTop_whenRequested() {
        val beads = listOf(
            1, 2, 3,
            4, 5, 6
        )

        val resized = resizeBeadGrid(
            beads = beads,
            oldColumns = 3,
            oldRows = 2,
            newColumns = 3,
            newRows = 3,
            verticalDirection = GridVerticalResizeDirection.Top
        )

        assertEquals(
            listOf(
                EmptyBead, EmptyBead, EmptyBead,
                1, 2, 3,
                4, 5, 6
            ),
            resized
        )
    }

    @Test
    fun resizeBeadGrid_shrinksFromTopAndLeft_whenTopLeftSelected() {
        val beads = listOf(
            1, 2, 3,
            4, 5, 6,
            7, 8, 9
        )

        val resized = resizeBeadGrid(
            beads = beads,
            oldColumns = 3,
            oldRows = 3,
            newColumns = 2,
            newRows = 2,
            horizontalDirection = GridHorizontalResizeDirection.Left,
            verticalDirection = GridVerticalResizeDirection.Top
        )

        assertEquals(
            listOf(
                5, 6,
                8, 9
            ),
            resized
        )
    }

    @Test
    fun updateRecentColors_movesSelectedToFront_andCapsToSix() {
        val updated = updateRecentColors(
            existing = listOf(2, 4, 6, 8, 10, 12),
            selectedIndex = 8
        )

        assertEquals(listOf(8, 2, 4, 6, 10, 12), updated)
    }

    @Test
    fun updateBeadAt_returnsSameListForNoOp() {
        val beads = listOf(1, 2, 3)

        val updated = updateBeadAt(beads, index = 1, nextColor = 2)

        assertSame(beads, updated)
    }

    @Test
    fun updateBeadAt_replacesRequestedCell() {
        val updated = updateBeadAt(
            beads = listOf(1, 2, 3),
            index = 1,
            nextColor = EmptyBead
        )

        assertEquals(listOf(1, EmptyBead, 3), updated)
    }

    @Test
    fun isGridEmpty_detectsNonEmptyGrid() {
        assertTrue(isGridEmpty(listOf(EmptyBead, EmptyBead)))
        assertTrue(!isGridEmpty(listOf(EmptyBead, 4, EmptyBead)))
    }

    @Test
    fun serializeBoardSnapshot_roundTripsIntoSameSnapshot() {
        val snapshot = BoardSnapshot(
            gridColumns = 8,
            gridRows = 8,
            stitchModeId = "peyote",
            beadShapeId = "rounded_rectangle",
            beads = List(64) { index -> if (index % 4 == 0) EmptyBead else index % 12 }
        )

        val restored = deserializeBoardSnapshot(serializeBoardSnapshot(snapshot))

        assertEquals(snapshot, restored)
    }

    @Test
    fun deserializeBoardSnapshot_rejectsMismatchedGridAndBeadsCount() {
        val serialized = """
            beadmaker_format=1
            grid_columns=8
            grid_rows=8
            stitch_mode_id=square
            bead_shape_id=circle
            beads=1,2,3
        """.trimIndent()

        val restored = deserializeBoardSnapshot(serialized)

        assertNull(restored)
    }

    @Test
    fun normalizeRotationDegrees_wrapsLargeAnglesIntoExpectedRange() {
        assertEquals(90f, normalizeRotationDegrees(450f), 0.0001f)
        assertEquals(-90f, normalizeRotationDegrees(-450f), 0.0001f)
        assertEquals(180f, normalizeRotationDegrees(540f), 0.0001f)
        assertEquals(DefaultTemplateRotation, normalizeRotationDegrees(Float.NaN), 0.0001f)
    }

    @Test
    fun calculateBitmapInSampleSize_downsamplesToViewportResolution() {
        assertEquals(
            4,
            calculateBitmapInSampleSize(
                sourceWidth = 4_000,
                sourceHeight = 3_000,
                requestedWidth = 1_000,
                requestedHeight = 1_000
            )
        )
    }

    @Test
    fun calculateBitmapInSampleSize_handlesVeryWideImages() {
        assertEquals(
            8,
            calculateBitmapInSampleSize(
                sourceWidth = 8_000,
                sourceHeight = 1_000,
                requestedWidth = 1_000,
                requestedHeight = 2_000
            )
        )
    }

    @Test
    fun calculateBitmapInSampleSize_doesNotUpsampleSmallImages() {
        assertEquals(
            1,
            calculateBitmapInSampleSize(
                sourceWidth = 640,
                sourceHeight = 480,
                requestedWidth = 1_080,
                requestedHeight = 1_920
            )
        )
    }

    @Test
    fun calculateBitmapInSampleSize_returnsSafeDefaultForInvalidDimensions() {
        assertEquals(
            1,
            calculateBitmapInSampleSize(
                sourceWidth = 0,
                sourceHeight = 3_000,
                requestedWidth = 1_000,
                requestedHeight = 1_000
            )
        )
    }

    @Test
    fun isDirectChildOfDirectory_acceptsOnlyFilesImmediatelyInsideCacheDirectory() {
        val cacheDirectory = Files.createTempDirectory("beadmaker-template-test").toFile()
        try {
            val directChild = File(cacheDirectory, "template.jpg")
            val nestedChild = File(File(cacheDirectory, "nested"), "template.jpg")
            val outsideFile = File(cacheDirectory.parentFile, "outside-template.jpg")

            assertTrue(isDirectChildOfDirectory(directChild, cacheDirectory))
            assertFalse(isDirectChildOfDirectory(nestedChild, cacheDirectory))
            assertFalse(isDirectChildOfDirectory(outsideFile, cacheDirectory))
        } finally {
            cacheDirectory.deleteRecursively()
        }
    }

    @Test
    fun editorUiStateSaveFormat_roundTripsAllRestorableState() {
        val state = EditorUiState(
            gridColumns = 8,
            gridRows = 8,
            beads = List(64) { index -> if (index % 3 == 0) EmptyBead else index % 10 },
            selectedColorIndex = 4,
            recentColorIndices = listOf(4, 2, 7),
            eraserSelected = true,
            templateImageUriString = "file:///cache/template image.jpg?value=a=b",
            templateOpacity = 0.7f,
            templateScale = 1.4f,
            templateOffsetX = 12f,
            templateOffsetY = -8f,
            templateRotation = 45f,
            boardScale = 2f,
            boardOffsetX = 3f,
            boardOffsetY = 4f,
            interactionMode = InteractionMode.Line,
            pendingLineStartIndex = 2,
            pendingLineEndIndex = 25,
            showColorPickerDialog = true,
            showToolsDialog = true,
            selectedToolsTab = 1,
            pendingCameraUriString = "content://com.example.beadmaker.fileprovider/camera",
            pendingSettingsGridColumns = 12f,
            pendingSettingsGridRows = 14f,
            pendingGridHorizontalResizeDirection = GridHorizontalResizeDirection.Left,
            pendingGridVerticalResizeDirection = GridVerticalResizeDirection.Top,
            isCreatingPattern = true,
            isImportingTemplate = true,
            isPatternIoInProgress = true
        )

        val restored = deserializeEditorUiState(serializeEditorUiState(state))

        assertEquals(
            state.copy(
                isCreatingPattern = false,
                isImportingTemplate = false,
                isPatternIoInProgress = false
            ),
            restored
        )
    }

    @Test
    fun editorUiStateSaveFormat_rejectsMalformedAndOutOfRangeState() {
        assertNull(deserializeEditorUiState("not-a-valid-save"))

        val valid = serializeEditorUiState(
            EditorUiState(
                gridColumns = 8,
                gridRows = 8,
                beads = List(64) { EmptyBead }
            )
        )
        assertNull(deserializeEditorUiState(valid.replace("gridColumns=8", "gridColumns=999")))
        assertNull(deserializeEditorUiState(valid.replace("beads=", "beads=1%2C")))
    }

    @Test
    fun editorUiStateSaveFormat_migratesVersionFiveNumericInteractionMode() {
        val current = serializeEditorUiState(
            EditorUiState(
                gridColumns = 8,
                gridRows = 8,
                beads = List(64) { EmptyBead },
                interactionMode = InteractionMode.Line
            )
        )
        val versionFive = current
            .replace("version=6", "version=5")
            .replace("interactionMode=line", "interactionMode=2")

        val restored = deserializeEditorUiState(versionFive)

        assertEquals(InteractionMode.Line, restored?.interactionMode)
    }

    @Test
    fun interactionMode_usesStableIdsAndFallsBackSafely() {
        assertEquals("template", InteractionMode.Template.id)
        assertEquals(InteractionMode.Paint, InteractionMode.fromId("unknown"))
        assertEquals(InteractionMode.Template, normalizeSavedInteractionMode(1, schemaVersion = 1))
        assertEquals(InteractionMode.Line, normalizeSavedInteractionMode(2, schemaVersion = 5))
    }

    @Test
    fun restoreEditorUiState_migratesVersionFourList() {
        val legacy = listOf(
            8,
            8,
            StitchMode.defaults.id,
            ArrayList(List(64) { EmptyBead }),
            3,
            false,
            null,
            DefaultTemplateOpacity,
            DefaultTemplateScale,
            0f,
            0f,
            DefaultBoardScale,
            0f,
            0f,
            2,
            false,
            5,
            false,
            true,
            1,
            null,
            StitchMode.defaults.id,
            8f,
            8f,
            BeadShape.defaults.id,
            BeadShape.defaults.id,
            GridHorizontalResizeDirection.Left.name,
            GridVerticalResizeDirection.Top.name,
            arrayListOf(3, 2),
            30f,
            4
        )

        val restored = restoreEditorUiState(legacy)

        assertEquals(5, restored?.pendingLineStartIndex)
        assertNull(restored?.pendingLineEndIndex)
        assertEquals(listOf(3, 2), restored?.recentColorIndices)
        assertEquals(GridHorizontalResizeDirection.Left, restored?.pendingGridHorizontalResizeDirection)
    }

    @Test
    fun restoreEditorUiState_rejectsTruncatedLegacyListWithoutThrowing() {
        assertNull(restoreEditorUiState(listOf(8, 8, "square")))
    }

    @Test
    fun normalizeEditorUiState_repairsInvalidDimensionsColorsAndTransforms() {
        val normalized = normalizeEditorUiState(
            EditorUiState(
                gridColumns = 2,
                gridRows = 100,
                beads = listOf(PaletteColorCount, EmptyBead - 1, 5),
                selectedColorIndex = PaletteColorCount,
                recentColorIndices = listOf(-2, PaletteColorCount, 3, 3),
                templateOpacity = Float.NaN,
                templateScale = Float.POSITIVE_INFINITY,
                templateOffsetX = Float.NEGATIVE_INFINITY,
                interactionMode = InteractionMode.Template,
                pendingLineStartIndex = 9_999,
                pendingLineEndIndex = 2,
                selectedToolsTab = 99,
                pendingSettingsGridColumns = Float.NaN,
                pendingSettingsGridRows = Float.POSITIVE_INFINITY
            )
        )

        assertEquals(MinGridSize, normalized.gridColumns)
        assertEquals(MaxGridSize, normalized.gridRows)
        assertEquals(MinGridSize * MaxGridSize, normalized.beads.size)
        assertEquals(listOf(EmptyBead, EmptyBead, 5), normalized.beads.take(3))
        assertEquals(0, normalized.selectedColorIndex)
        assertEquals(listOf(0, 3), normalized.recentColorIndices)
        assertEquals(DefaultTemplateOpacity, normalized.templateOpacity)
        assertEquals(DefaultTemplateScale, normalized.templateScale)
        assertEquals(0f, normalized.templateOffsetX)
        assertEquals(InteractionMode.Paint, normalized.interactionMode)
        assertNull(normalized.pendingLineStartIndex)
        assertNull(normalized.pendingLineEndIndex)
        assertEquals(1, normalized.selectedToolsTab)
        assertEquals(MinGridSize.toFloat(), normalized.pendingSettingsGridColumns)
        assertEquals(MaxGridSize.toFloat(), normalized.pendingSettingsGridRows)
    }

    @Test
    fun beadMutationHelpers_rejectInvalidPaletteIndices() {
        val beads = listOf(1, 2, 3, 4)

        assertSame(beads, updateBeadAt(beads, index = 1, nextColor = EmptyBead - 1))
        assertSame(beads, updateBeadAt(beads, index = 1, nextColor = PaletteColorCount))
        assertSame(
            beads,
            fillConnectedRegion(
                beads = beads,
                index = 0,
                replacementColor = PaletteColorCount,
                columns = 2
            )
        )
        assertSame(
            beads,
            drawLineOnGrid(
                beads = beads,
                startIndex = 0,
                endIndex = 3,
                replacementColor = EmptyBead - 1,
                columns = 2
            )
        )
    }

    @Test
    fun resizeBeadGrid_returnsSafeResultsForMalformedInput() {
        assertEquals(emptyList<Int>(), resizeBeadGrid(emptyList(), 0, 0, -1, 8))
        assertEquals(
            List(4) { EmptyBead },
            resizeBeadGrid(
                beads = listOf(PaletteColorCount),
                oldColumns = 2,
                oldRows = 2,
                newColumns = 2,
                newRows = 2
            )
        )
    }
}
