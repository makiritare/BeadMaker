package com.example.beadmaker.ui.state

import com.example.beadmaker.ui.model.InteractionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BeadEditorBehaviorTest {

    @Test
    fun paintUndoAndRedo_restoreEachBoardVersion() {
        val state = createState()
        state.applySelectedColor(4)

        state.paintCell(10)

        assertEquals(4, state.uiState.beads[10])
        assertTrue(state.canUndo)
        assertFalse(state.canRedo)

        state.undo()

        assertEquals(EmptyBead, state.uiState.beads[10])
        assertFalse(state.canUndo)
        assertTrue(state.canRedo)

        state.redo()

        assertEquals(4, state.uiState.beads[10])
        assertTrue(state.canUndo)
        assertFalse(state.canRedo)
    }

    @Test
    fun brushStroke_isGroupedIntoOneUndoOperation() {
        val state = createState()
        state.applySelectedColor(6)
        state.setBrushMode()

        state.startBrushStroke()
        state.paintBrushCell(0)
        state.paintBrushCell(1)
        state.paintBrushCell(2)
        state.endBrushStroke()

        assertEquals(listOf(6, 6, 6), state.uiState.beads.take(3))

        state.undo()

        assertTrue(state.uiState.beads.all { it == EmptyBead })
        assertFalse(state.canUndo)
    }

    @Test
    fun fillMode_replacesOnlyConnectedRegion() {
        val beads = MutableList(64) { EmptyBead }.apply {
            this[0] = 1
            this[1] = 1
            this[2] = 2
            this[8] = 1
            this[9] = 2
        }
        val state = createState(beads)
        state.applySelectedColor(5)
        state.setFillMode()

        state.paintCell(0)

        assertEquals(5, state.uiState.beads[0])
        assertEquals(5, state.uiState.beads[1])
        assertEquals(5, state.uiState.beads[8])
        assertEquals(2, state.uiState.beads[2])
        assertEquals(2, state.uiState.beads[9])
    }

    @Test
    fun lineMode_previewsThenCommitsOnEndpointConfirmation() {
        val state = createState()
        state.applySelectedColor(7)
        state.setLineMode()

        state.paintCell(0)
        state.paintCell(18)

        assertEquals(0, state.uiState.pendingLineStartIndex)
        assertEquals(18, state.uiState.pendingLineEndIndex)
        assertTrue(state.uiState.beads.all { it == EmptyBead })

        state.paintCell(18)

        val expectedIndices = calculateLineIndices(0, 18, columns = 8, maxIndex = 63)
        assertTrue(expectedIndices.all { state.uiState.beads[it] == 7 })
        assertNull(state.uiState.pendingLineStartIndex)
        assertNull(state.uiState.pendingLineEndIndex)

        state.undo()

        assertTrue(state.uiState.beads.all { it == EmptyBead })
    }

    @Test
    fun applyingGridResize_preservesAnchoredBeadsAndIsUndoable() {
        val state = createState()
        state.applySelectedColor(3)
        state.paintCell(0)
        state.openToolsDialogAtTab(1)
        state.updatePendingGridColumns(9f)
        state.updatePendingGridHorizontalResizeDirection(GridHorizontalResizeDirection.Left)

        state.applyPendingGridSettings()

        assertEquals(9, state.uiState.gridColumns)
        assertEquals(8, state.uiState.gridRows)
        assertEquals(3, state.uiState.beads[1])
        assertEquals(InteractionMode.Paint, state.uiState.interactionMode)

        state.undo()

        assertEquals(8, state.uiState.gridColumns)
        assertEquals(8, state.uiState.gridRows)
        assertEquals(3, state.uiState.beads[0])
    }

    @Test
    fun invalidCellInput_doesNotCreateHistoryOrPendingLineState() {
        val state = createState()
        state.setLineMode()

        state.paintCell(-1)
        state.paintCell(64)

        assertFalse(state.canUndo)
        assertNull(state.uiState.pendingLineStartIndex)
        assertNull(state.uiState.pendingLineEndIndex)
    }

    private fun createState(
        beads: List<Int> = List(64) { EmptyBead }
    ): BeadEditorState {
        return BeadEditorState(
            EditorUiState(
                gridColumns = 8,
                gridRows = 8,
                beads = beads
            )
        )
    }
}
