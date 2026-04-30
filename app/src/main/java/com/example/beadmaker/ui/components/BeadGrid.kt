package com.example.beadmaker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.beadmaker.ui.model.BeadShape
import com.example.beadmaker.ui.model.StitchLayoutStyle
import com.example.beadmaker.ui.model.StitchMode

private val BeadCellCircleShape = CircleShape
private val BeadCellRoundedRectShape = RoundedCornerShape(6.dp)

@Composable
fun BeadGrid(
    beads: List<Int>,
    colors: List<Color>,
    stitchMode: StitchMode,
    beadShape: BeadShape,
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
                                beadShape = beadShape,
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
    beadShape: BeadShape,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val outline = MaterialTheme.colorScheme.outline
    val cellShape = when (beadShape) {
        BeadShape.Circle -> BeadCellCircleShape
        BeadShape.RoundedRectangle -> BeadCellRoundedRectShape
    }
    val circleCellBackground = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }
    val cellBackground = when (beadShape) {
        BeadShape.Circle -> circleCellBackground
        BeadShape.RoundedRectangle -> Color.Transparent
    }
    val cellBorderColor = when (beadShape) {
        BeadShape.Circle -> if (isDark) {
            outline.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }
        BeadShape.RoundedRectangle -> Color.Transparent
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .clip(cellShape)
            .background(cellBackground)
            .border(
                width = 0.5.dp,
                color = cellBorderColor,
                shape = cellShape
            )
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            val shapeSize = size.minDimension * 0.9f
            val shapeRadius = shapeSize / 2f
            val rectWidth = size.width * 0.92f
            val rectHeight = size.height * 0.62f
            val rectLeft = (size.width - rectWidth) / 2f
            val rectTop = (size.height - rectHeight) / 2f
            val rectCorner = CornerRadius(rectHeight * 0.18f, rectHeight * 0.18f)
            val rimStroke = Stroke(width = 1.dp.toPx())
            val emptyStroke = Stroke(width = 1.5.dp.toPx())

            if (beadColor == null) {
                // Empty "socket" look
                when (beadShape) {
                    BeadShape.Circle -> drawCircle(
                        color = outline.copy(alpha = if (isDark) 0.5f else 0.35f),
                        radius = shapeRadius,
                        style = emptyStroke
                    )

                    BeadShape.RoundedRectangle -> drawRoundRect(
                        color = outline.copy(alpha = if (isDark) 0.5f else 0.35f),
                        topLeft = androidx.compose.ui.geometry.Offset(rectLeft, rectTop),
                        size = androidx.compose.ui.geometry.Size(rectWidth, rectHeight),
                        cornerRadius = rectCorner,
                        style = emptyStroke
                    )
                }
            } else {
                // Painted bead with slight depth
                when (beadShape) {
                    BeadShape.Circle -> {
                        drawCircle(
                            color = beadColor,
                            radius = shapeRadius
                        )
                        // Dark rim for contrast
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.15f),
                            radius = shapeRadius,
                            style = rimStroke
                        )
                    }

                    BeadShape.RoundedRectangle -> {
                        drawRoundRect(
                            color = beadColor,
                            topLeft = androidx.compose.ui.geometry.Offset(rectLeft, rectTop),
                            size = androidx.compose.ui.geometry.Size(rectWidth, rectHeight),
                            cornerRadius = rectCorner
                        )
                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.15f),
                            topLeft = androidx.compose.ui.geometry.Offset(rectLeft, rectTop),
                            size = androidx.compose.ui.geometry.Size(rectWidth, rectHeight),
                            cornerRadius = rectCorner,
                            style = rimStroke
                        )
                    }
                }
                // Small highlight to give 3D feel
                when (beadShape) {
                    BeadShape.Circle -> drawCircle(
                        color = Color.White.copy(alpha = 0.25f),
                        radius = shapeRadius * 0.4f,
                        center = center.copy(
                            x = center.x - shapeRadius * 0.3f,
                            y = center.y - shapeRadius * 0.3f
                        )
                    )

                    BeadShape.RoundedRectangle -> drawRoundRect(
                        color = Color.White.copy(alpha = 0.2f),
                        topLeft = androidx.compose.ui.geometry.Offset(
                            x = rectLeft + rectWidth * 0.1f,
                            y = rectTop + rectHeight * 0.12f
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            width = rectWidth * 0.35f,
                            height = rectHeight * 0.28f
                        ),
                        cornerRadius = rectCorner
                    )
                }
            }
        }
    }
}
