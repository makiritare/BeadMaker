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
    boardScale: Float = 1f,
    columns: Int = 16,
    onCellTap: (index: Int) -> Unit
) {
    val safeColumns = columns.coerceAtLeast(1)
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
        val beadSize = when (stitchMode.layoutStyle) {
            StitchLayoutStyle.Staggered ->
                maxWidth / (safeColumns + offsetFactor)
            else ->
                maxWidth / safeColumns.toFloat()
        }
        val oddRowOffset = beadSize * offsetFactor

        Column {
            beads.chunked(safeColumns).forEachIndexed { rowIndex, row ->
                val isStaggeredRow = (rowIndex / groupSize) % 2 == 1
                val rowOffset = when (stitchMode.layoutStyle) {
                    StitchLayoutStyle.Staggered -> if (isStaggeredRow) oddRowOffset else 0.dp
                    else -> 0.dp
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (stitchMode.layoutStyle == StitchLayoutStyle.Staggered && isStaggeredRow) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.055f)
                            } else {
                                Color.Transparent
                            },
                            shape = RoundedCornerShape(10.dp)
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
    val emptyBeadOutlineColor = if (isDark) {
        Color.White.copy(alpha = 0.42f)
    } else {
        outline.copy(alpha = 0.5f)
    }
    val paintedBeadRimColor = if (isDark) {
        Color.White.copy(alpha = 0.32f)
    } else {
        Color.White.copy(alpha = 0.42f)
    }
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
            Color.White.copy(alpha = 0.22f)
        } else {
            Color.White.copy(alpha = 0.6f)
        }
        BeadShape.RoundedRectangle -> Color.Transparent
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
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
        Canvas(modifier = Modifier.fillMaxSize()) {
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
                        color = emptyBeadOutlineColor,
                        radius = shapeRadius,
                        style = emptyStroke
                    )

                    BeadShape.RoundedRectangle -> drawRoundRect(
                        color = emptyBeadOutlineColor,
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
                        drawCircle(
                            color = paintedBeadRimColor,
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
                            color = paintedBeadRimColor,
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
