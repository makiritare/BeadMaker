package com.example.beadmaker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.beadmaker.ui.model.BeadShape
import com.example.beadmaker.ui.model.StitchLayoutStyle
import com.example.beadmaker.ui.model.StitchMode

private val BeadCellCircleShape = CircleShape
private val BeadCellRoundedRectShape = RoundedCornerShape(6.dp)
private val GridNumberMinVisibleCellSize = 18.dp

@Composable
fun BeadGrid(
    beads: List<Int>,
    colors: List<Color>,
    stitchMode: StitchMode,
    beadShape: BeadShape,
    modifier: Modifier = Modifier,
    boardScale: Float = 1f,
    columns: Int = 16,
    brushEnabled: Boolean = false,
    lineStartIndex: Int? = null,
    linePreviewIndices: Set<Int> = emptySet(),
    onBrushStrokeStart: () -> Unit = {},
    onBrushStrokePaint: (index: Int) -> Unit = {},
    onBrushStrokeEnd: () -> Unit = {},
    onCellPressStart: (index: Int) -> Unit = {},
    onCellPressEnd: () -> Unit = {},
    onCellTap: (index: Int) -> Unit
) {
    val safeColumns = columns.coerceAtLeast(1)
    val beadRows = beads.chunked(safeColumns)
    val safeRows = beadRows.size.coerceAtLeast(1)
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
        val density = LocalDensity.current
        val layoutOffsetUnits = when (stitchMode.layoutStyle) {
            StitchLayoutStyle.Staggered -> offsetFactor
            else -> 0f
        }
        val unlabeledBeadSizeByWidth = maxWidth / (safeColumns + layoutOffsetUnits)
        val unlabeledBeadSizeByHeight = maxHeight / safeRows.toFloat()
        val unlabeledBeadSize = if (unlabeledBeadSizeByWidth < unlabeledBeadSizeByHeight) {
            unlabeledBeadSizeByWidth
        } else {
            unlabeledBeadSizeByHeight
        }
        val labeledBeadSizeByWidth = maxWidth / (safeColumns + layoutOffsetUnits + 1f)
        val labeledBeadSizeByHeight = maxHeight / (safeRows + 1f)
        val labeledBeadSize = if (labeledBeadSizeByWidth < labeledBeadSizeByHeight) {
            labeledBeadSizeByWidth
        } else {
            labeledBeadSizeByHeight
        }
        val showGridNumbers = labeledBeadSize * boardScale >= GridNumberMinVisibleCellSize
        val beadSize = if (showGridNumbers) labeledBeadSize else unlabeledBeadSize
        val oddRowOffset = beadSize * offsetFactor
        val beadSizePx = with(density) { beadSize.toPx() }
        val oddRowOffsetPx = with(density) { oddRowOffset.toPx() }
        val contentWidthPx = beadSizePx * (safeColumns + layoutOffsetUnits + if (showGridNumbers) 1f else 0f)
        val contentHeightPx = beadSizePx * (safeRows + if (showGridNumbers) 1 else 0)
        val contentOriginX = (constraints.maxWidth - contentWidthPx) / 2f
        val contentOriginY = (constraints.maxHeight - contentHeightPx) / 2f
        val gridOriginX = contentOriginX + if (showGridNumbers) beadSizePx else 0f
        val gridOriginY = contentOriginY + if (showGridNumbers) beadSizePx else 0f

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(
                    brushEnabled,
                    beadSizePx,
                    gridOriginX,
                    gridOriginY,
                    oddRowOffsetPx,
                    safeColumns,
                    safeRows,
                    groupSize,
                    stitchMode.layoutStyle
                ) {
                    if (!brushEnabled) return@pointerInput

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var lastPaintedIndex: Int? = null
                        val startIndex = beadIndexAtPosition(
                            position = down.position,
                            gridOriginX = gridOriginX,
                            gridOriginY = gridOriginY,
                            beadSizePx = beadSizePx,
                            oddRowOffsetPx = oddRowOffsetPx,
                            columns = safeColumns,
                            rows = safeRows,
                            groupSize = groupSize,
                            isStaggered = stitchMode.layoutStyle == StitchLayoutStyle.Staggered
                        )
                        if (startIndex != null) {
                            onBrushStrokeStart()
                            onBrushStrokePaint(startIndex)
                            lastPaintedIndex = startIndex
                        }

                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val activeChange = event.changes.firstOrNull { it.pressed } ?: break
                            val currentIndex = beadIndexAtPosition(
                                position = activeChange.position,
                                gridOriginX = gridOriginX,
                                gridOriginY = gridOriginY,
                                beadSizePx = beadSizePx,
                                oddRowOffsetPx = oddRowOffsetPx,
                                columns = safeColumns,
                                rows = safeRows,
                                groupSize = groupSize,
                                isStaggered = stitchMode.layoutStyle == StitchLayoutStyle.Staggered
                            )
                            if (currentIndex != null && currentIndex != lastPaintedIndex) {
                                onBrushStrokePaint(currentIndex)
                                lastPaintedIndex = currentIndex
                            }
                        } while (event.changes.any { it.pressed })

                        onBrushStrokeEnd()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column {
                if (showGridNumbers) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GridNumberCell(
                            label = null,
                            modifier = Modifier.width(beadSize)
                        )
                        repeat(safeColumns) { columnIndex ->
                            GridNumberCell(
                                label = (columnIndex + 1).toString(),
                                modifier = Modifier.width(beadSize)
                            )
                        }
                        if (oddRowOffset > 0.dp) {
                            Spacer(modifier = Modifier.width(oddRowOffset))
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.Top
                ) {
                    if (showGridNumbers) {
                        Column {
                            repeat(safeRows) { rowIndex ->
                                GridNumberCell(
                                    label = (rowIndex + 1).toString(),
                                    modifier = Modifier.width(beadSize)
                                )
                            }
                        }
                    }

                    Column {
                        beadRows.forEachIndexed { rowIndex, row ->
                            val isStaggeredRow = (rowIndex / groupSize) % 2 == 1
                            val rowOffset = when (stitchMode.layoutStyle) {
                                StitchLayoutStyle.Staggered -> if (isStaggeredRow) oddRowOffset else 0.dp
                                else -> 0.dp
                            }
                            Box(
                                modifier = Modifier
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
                                            highlighted = beadIndex in linePreviewIndices,
                                            startMarked = beadIndex == lineStartIndex,
                                            modifier = Modifier.width(beadSize),
                                            inputEnabled = !brushEnabled,
                                            onPressStart = { onCellPressStart(beadIndex) },
                                            onPressEnd = onCellPressEnd,
                                            onTap = { onCellTap(beadIndex) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GridNumberCell(
    label: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        if (label != null) {
            androidx.compose.material3.Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BeadCell(
    beadColor: Color?,
    beadShape: BeadShape,
    highlighted: Boolean,
    startMarked: Boolean,
    modifier: Modifier = Modifier,
    inputEnabled: Boolean,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    onTap: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val outline = MaterialTheme.colorScheme.outline
    val previewColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.95f)
    val startMarkerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.98f)
    val startMarkerFillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
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
            .pointerInput(inputEnabled, onTap, onPressStart, onPressEnd) {
                if (!inputEnabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        onPressStart()
                        tryAwaitRelease()
                        onPressEnd()
                    },
                    onTap = { onTap() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val shapeSize = size.minDimension * 0.9f
            val shapeRadius = shapeSize / 2f
            // Rounded rectangles should almost fill the square cell so stacked rows do not
            // leave a visible seam in the square layout.
            val rectWidth = size.width * 0.96f
            val rectHeight = size.height * 0.92f
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

            if (highlighted) {
                val previewStroke = Stroke(width = 2.dp.toPx())
                when (beadShape) {
                    BeadShape.Circle -> drawCircle(
                        color = previewColor,
                        radius = shapeRadius,
                        style = previewStroke
                    )

                    BeadShape.RoundedRectangle -> drawRoundRect(
                        color = previewColor,
                        topLeft = androidx.compose.ui.geometry.Offset(rectLeft, rectTop),
                        size = androidx.compose.ui.geometry.Size(rectWidth, rectHeight),
                        cornerRadius = rectCorner,
                        style = previewStroke
                    )
                }
            }

            if (startMarked) {
                val startStroke = Stroke(width = 2.5.dp.toPx())
                when (beadShape) {
                    BeadShape.Circle -> {
                        drawCircle(
                            color = startMarkerColor,
                            radius = shapeRadius,
                            style = startStroke
                        )
                        drawCircle(
                            color = startMarkerFillColor,
                            radius = shapeRadius * 0.22f
                        )
                    }

                    BeadShape.RoundedRectangle -> {
                        drawRoundRect(
                            color = startMarkerColor,
                            topLeft = androidx.compose.ui.geometry.Offset(rectLeft, rectTop),
                            size = androidx.compose.ui.geometry.Size(rectWidth, rectHeight),
                            cornerRadius = rectCorner,
                            style = startStroke
                        )
                        val markerSize = size.minDimension * 0.22f
                        drawRoundRect(
                            color = startMarkerFillColor,
                            topLeft = androidx.compose.ui.geometry.Offset(
                                center.x - markerSize / 2f,
                                center.y - markerSize / 2f
                            ),
                            size = androidx.compose.ui.geometry.Size(markerSize, markerSize),
                            cornerRadius = CornerRadius(markerSize * 0.3f, markerSize * 0.3f)
                        )
                    }
                }
            }
        }
    }
}

private fun beadIndexAtPosition(
    position: Offset,
    gridOriginX: Float,
    gridOriginY: Float,
    beadSizePx: Float,
    oddRowOffsetPx: Float,
    columns: Int,
    rows: Int,
    groupSize: Int,
    isStaggered: Boolean
): Int? {
    if (beadSizePx <= 0f) return null

    val relativeY = position.y - gridOriginY
    if (relativeY < 0f) return null
    val row = (relativeY / beadSizePx).toInt()
    if (row !in 0 until rows) return null

    val rowOffset = if (isStaggered && (row / groupSize) % 2 == 1) oddRowOffsetPx else 0f
    val relativeX = position.x - gridOriginX - rowOffset
    if (relativeX < 0f) return null
    val column = (relativeX / beadSizePx).toInt()
    if (column !in 0 until columns) return null

    return row * columns + column
}
