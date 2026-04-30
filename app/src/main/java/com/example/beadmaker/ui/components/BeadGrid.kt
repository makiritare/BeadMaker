package com.example.beadmaker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.beadmaker.ui.model.StitchLayoutStyle
import com.example.beadmaker.ui.model.StitchMode

private val BeadCellShape = RoundedCornerShape(12.dp)

@Composable
fun BeadGrid(
    beads: List<Int>,
    colors: List<Color>,
    stitchMode: StitchMode,
    modifier: Modifier = Modifier,
    columns: Int = 16,
    onCellTap: (index: Int) -> Unit
) {
    val safeColumns = columns.coerceAtLeast(1)
    val gap = when (stitchMode) {
        StitchMode.Square -> 6.dp
        StitchMode.Peyote, StitchMode.Peyote2Drop, StitchMode.Peyote3Drop -> 7.dp
        StitchMode.Brick -> 5.dp
    }
    val offsetFactor = when (stitchMode) {
        StitchMode.Peyote, StitchMode.Peyote2Drop, StitchMode.Peyote3Drop -> 0.5f
        StitchMode.Brick -> 0.58f
        StitchMode.Square -> 0f
    }
    val groupSize = when (stitchMode) {
        StitchMode.Peyote2Drop -> 2
        StitchMode.Peyote3Drop -> 3
        else -> 1
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val evenBeadSize = (maxWidth - gap * (safeColumns - 1)) / safeColumns.toFloat()
        val staggeredBeadSize =
            (maxWidth - gap * ((safeColumns - 1) + offsetFactor)) / (safeColumns + offsetFactor)
        val beadSize = when (stitchMode.layoutStyle) {
            StitchLayoutStyle.Staggered -> staggeredBeadSize
            else -> evenBeadSize
        }
        val oddRowOffset = (beadSize + gap) * offsetFactor

        Column(
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            beads.chunked(safeColumns).forEachIndexed { rowIndex, row ->
                val isStaggeredRow = (rowIndex / groupSize) % 2 == 1
                val rowOffset = when (stitchMode.layoutStyle) {
                    StitchLayoutStyle.Staggered -> if (isStaggeredRow) oddRowOffset else 0.dp
                    else -> 0.dp
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (stitchMode.layoutStyle == StitchLayoutStyle.Staggered && isStaggeredRow) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.055f)
                            } else {
                                Color.Transparent
                            }
                        )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        if (rowOffset > 0.dp) {
                            Spacer(modifier = Modifier.width(rowOffset))
                        }
                        row.forEachIndexed { columnIndex, beadColorIndex ->
                            if (columnIndex > 0) {
                                Spacer(modifier = Modifier.width(gap))
                            }
                            val beadIndex = rowIndex * safeColumns + columnIndex
                            BeadCell(
                                beadColor = beadColorIndex
                                    .takeIf { it >= 0 }
                                    ?.let(colors::getOrNull),
                                modifier = Modifier.width(beadSize),
                                onTap = { onCellTap(beadIndex) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BeadCell(
    beadColor: Color?,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    val outline = MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(BeadCellShape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = BeadCellShape
            )
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            if (beadColor == null) {
                drawCircle(
                    color = outline.copy(alpha = 0.45f),
                    radius = size.minDimension / 2,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            } else {
                drawCircle(
                    color = beadColor,
                    radius = size.minDimension / 2
                )
                drawCircle(
                    color = outline.copy(alpha = 0.18f),
                    radius = size.minDimension / 2,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                )
            }
        }
    }
}
