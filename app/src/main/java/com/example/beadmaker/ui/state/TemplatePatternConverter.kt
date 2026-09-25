package com.example.beadmaker.ui.state

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.exifinterface.media.ExifInterface
import com.example.beadmaker.ui.model.StitchLayoutStyle
import com.example.beadmaker.ui.model.StitchMode
import kotlin.math.floor
import kotlin.math.min

fun findNearestPaletteIndex(pixel: Int, paletteColors: List<Int>): Int {
    if (paletteColors.isEmpty()) return EmptyBead
    val red = android.graphics.Color.red(pixel)
    val green = android.graphics.Color.green(pixel)
    val blue = android.graphics.Color.blue(pixel)

    var bestIndex = 0
    var bestDistance = Long.MAX_VALUE
    paletteColors.forEachIndexed { index, paletteColor ->
        val paletteRed = (paletteColor shr 16) and 0xFF
        val paletteGreen = (paletteColor shr 8) and 0xFF
        val paletteBlue = paletteColor and 0xFF
        val deltaRed = red - paletteRed
        val deltaGreen = green - paletteGreen
        val deltaBlue = blue - paletteBlue
        val distance = deltaRed.toLong() * deltaRed +
            deltaGreen.toLong() * deltaGreen +
            deltaBlue.toLong() * deltaBlue
        if (distance < bestDistance) {
            bestDistance = distance
            bestIndex = index
        }
    }
    return bestIndex
}

internal fun EditorUiState.hasSameTemplateConversionInputAs(other: EditorUiState): Boolean {
    return templateImageUriString == other.templateImageUriString &&
        gridColumns == other.gridColumns &&
        gridRows == other.gridRows &&
        stitchModeId == other.stitchModeId &&
        templateScale == other.templateScale &&
        templateOffsetX == other.templateOffsetX &&
        templateOffsetY == other.templateOffsetY &&
        templateRotation == other.templateRotation
}

internal fun createPatternBeads(
    context: Context,
    imageUri: Uri,
    paletteColors: List<Int>,
    viewportWidth: Int,
    viewportHeight: Int,
    sourceState: EditorUiState
): List<Int>? {
    val bitmap = decodeBitmapRespectingExif(
        context = context,
        uri = imageUri,
        requestedWidth = viewportWidth,
        requestedHeight = viewportHeight
    ) ?: return null

    return try {
        val renderedBitmap = renderTemplateToViewportBitmap(
            bitmap = bitmap,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            templateScale = sourceState.templateScale,
            templateOffsetX = sourceState.templateOffsetX,
            templateOffsetY = sourceState.templateOffsetY,
            templateRotation = sourceState.templateRotation
        )
        try {
            val nextBeads = MutableList(sourceState.gridColumns * sourceState.gridRows) { EmptyBead }
            val stitchMode = StitchMode.fromId(sourceState.stitchModeId)
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
            val layoutOffsetUnits = when (stitchMode.layoutStyle) {
                StitchLayoutStyle.Staggered -> offsetFactor
                else -> 0f
            }
            val beadSizePx = minOf(
                viewportWidth / (sourceState.gridColumns + layoutOffsetUnits),
                viewportHeight / sourceState.gridRows.toFloat()
            )
            val oddRowOffsetPx = beadSizePx * offsetFactor
            val contentWidthPx = beadSizePx * (sourceState.gridColumns + layoutOffsetUnits)
            val contentHeightPx = beadSizePx * sourceState.gridRows
            val contentOriginX = (viewportWidth - contentWidthPx) / 2f
            val contentOriginY = (viewportHeight - contentHeightPx) / 2f

            repeat(sourceState.gridRows) { row ->
                repeat(sourceState.gridColumns) { column ->
                    val beadIndex = row * sourceState.gridColumns + column
                    val rowOffset = if (
                        stitchMode.layoutStyle == StitchLayoutStyle.Staggered &&
                        (row / groupSize) % 2 == 1
                    ) {
                        oddRowOffsetPx
                    } else {
                        0f
                    }
                    val sampledPixel = sampleViewportCellPixel(
                        bitmap = renderedBitmap,
                        left = contentOriginX + rowOffset + column * beadSizePx,
                        top = contentOriginY + row * beadSizePx,
                        cellSize = beadSizePx
                    )
                    nextBeads[beadIndex] = sampledPixel?.let {
                        findNearestPaletteIndex(it, paletteColors)
                    } ?: EmptyBead
                }
            }
            nextBeads
        } finally {
            renderedBitmap.recycle()
        }
    } finally {
        bitmap.recycle()
    }
}

fun calculateBitmapInSampleSize(
    sourceWidth: Int,
    sourceHeight: Int,
    requestedWidth: Int,
    requestedHeight: Int
): Int {
    if (sourceWidth <= 0 || sourceHeight <= 0 || requestedWidth <= 0 || requestedHeight <= 0) {
        return 1
    }

    val fitScale = min(
        requestedWidth.toDouble() / sourceWidth,
        requestedHeight.toDouble() / sourceHeight
    )
    if (fitScale >= 1.0) return 1

    val maximumSampleSize = floor(1.0 / fitScale).toInt().coerceAtLeast(1)
    var sampleSize = 1
    while (sampleSize <= maximumSampleSize / 2) sampleSize *= 2
    return sampleSize
}

fun decodeBitmapRespectingExif(
    context: Context,
    uri: Uri,
    requestedWidth: Int,
    requestedHeight: Int
): Bitmap? {
    val orientation = runCatching<Int?> {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }
    }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val swapsDimensions = orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
        orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
        orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
        orientation == ExifInterface.ORIENTATION_TRANSVERSE
    val orientedWidth = if (swapsDimensions) bounds.outHeight else bounds.outWidth
    val orientedHeight = if (swapsDimensions) bounds.outWidth else bounds.outHeight
    val options = BitmapFactory.Options().apply {
        inSampleSize = calculateBitmapInSampleSize(
            orientedWidth,
            orientedHeight,
            requestedWidth,
            requestedHeight
        )
    }
    val bitmap = runCatching<Bitmap?> {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }.getOrNull() ?: return null

    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f)
            matrix.postScale(-1f, 1f)
        }
    }
    if (matrix.isIdentity) return bitmap

    return try {
        Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        ).also { transformed ->
            if (transformed !== bitmap) bitmap.recycle()
        }
    } catch (exception: Exception) {
        bitmap.recycle()
        throw exception
    }
}

fun renderTemplateToViewportBitmap(
    bitmap: Bitmap,
    viewportWidth: Int,
    viewportHeight: Int,
    templateScale: Float,
    templateOffsetX: Float,
    templateOffsetY: Float,
    templateRotation: Float
): Bitmap {
    val renderedBitmap = createBitmap(viewportWidth, viewportHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(renderedBitmap)
    val matrix = Matrix()
    val sourceRect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
    val destinationRect = RectF(0f, 0f, viewportWidth.toFloat(), viewportHeight.toFloat())
    matrix.setRectToRect(sourceRect, destinationRect, Matrix.ScaleToFit.CENTER)
    val pivotX = viewportWidth / 2f
    val pivotY = viewportHeight / 2f
    matrix.postScale(templateScale, templateScale, pivotX, pivotY)
    matrix.postRotate(templateRotation, pivotX, pivotY)
    matrix.postTranslate(templateOffsetX, templateOffsetY)
    canvas.drawBitmap(bitmap, matrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    return renderedBitmap
}

fun sampleViewportCellPixel(bitmap: Bitmap, left: Float, top: Float, cellSize: Float): Int? {
    val safeLeft = left.toInt().coerceIn(0, bitmap.width - 1)
    val safeTop = top.toInt().coerceIn(0, bitmap.height - 1)
    val safeRight = maxOf(safeLeft + 1, (left + cellSize).toInt()).coerceAtMost(bitmap.width)
    val safeBottom = maxOf(safeTop + 1, (top + cellSize).toInt()).coerceAtMost(bitmap.height)

    var alphaTotal = 0L
    var redTotal = 0L
    var greenTotal = 0L
    var blueTotal = 0L
    var sampleCount = 0L
    for (y in safeTop until safeBottom) {
        for (x in safeLeft until safeRight) {
            val pixel = bitmap[x, y]
            val alpha = android.graphics.Color.alpha(pixel)
            if (alpha < 32) continue
            alphaTotal += alpha
            redTotal += android.graphics.Color.red(pixel)
            greenTotal += android.graphics.Color.green(pixel)
            blueTotal += android.graphics.Color.blue(pixel)
            sampleCount += 1
        }
    }
    if (sampleCount == 0L) return null

    return android.graphics.Color.argb(
        (alphaTotal / sampleCount).toInt(),
        (redTotal / sampleCount).toInt(),
        (greenTotal / sampleCount).toInt(),
        (blueTotal / sampleCount).toInt()
    )
}
