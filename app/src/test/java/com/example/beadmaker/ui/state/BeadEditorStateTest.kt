package com.example.beadmaker.ui.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
    }
}
