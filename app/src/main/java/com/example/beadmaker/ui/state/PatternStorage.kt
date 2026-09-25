package com.example.beadmaker.ui.state

import android.content.Context
import android.net.Uri
import android.util.AtomicFile
import com.example.beadmaker.ui.model.BeadShape
import com.example.beadmaker.ui.model.StitchMode
import java.io.File
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SavedPatternFileName = "saved_pattern.bm"
private const val PatternFormatVersion = 1
@Suppress("SpellCheckingInspection")
private const val TimestampPattern = "yyyyMMdd_HHmmss"

internal class PatternStorage(private val context: Context) {

    fun save(snapshot: BoardSnapshot): Boolean {
        val payload = serializeBoardSnapshot(snapshot)
        val atomicFile = AtomicFile(File(context.filesDir, SavedPatternFileName))
        val outputStream = try {
            atomicFile.startWrite()
        } catch (_: Exception) {
            return false
        }

        return try {
            OutputStreamWriter(outputStream, Charsets.UTF_8).apply {
                write(payload)
                flush()
            }
            atomicFile.finishWrite(outputStream)
            true
        } catch (_: Exception) {
            runCatching { atomicFile.failWrite(outputStream) }
            false
        }
    }

    fun load(): BoardSnapshot? {
        val atomicFile = AtomicFile(File(context.filesDir, SavedPatternFileName))
        return runCatching {
            val serialized = atomicFile.openRead().bufferedReader(Charsets.UTF_8).use { reader ->
                reader.readText()
            }
            deserializeBoardSnapshot(serialized)
        }.getOrNull()
    }

    fun export(uri: Uri, snapshot: BoardSnapshot): Boolean {
        val payload = serializeBoardSnapshot(snapshot)
        return runCatching {
            val stream = context.contentResolver.openOutputStream(uri) ?: return@runCatching false
            stream.bufferedWriter(Charsets.UTF_8).use { writer -> writer.write(payload) }
            true
        }.getOrDefault(false)
    }

    fun suggestedExportFileName(): String {
        val timestamp = SimpleDateFormat(TimestampPattern, Locale.US).format(Date())
        return "bead_pattern_$timestamp.bm"
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

    if (values["beadmaker_format"]?.toIntOrNull() != PatternFormatVersion) return null

    val gridColumns = values["grid_columns"]?.toIntOrNull() ?: return null
    val gridRows = values["grid_rows"]?.toIntOrNull() ?: return null
    if (gridColumns !in MinGridSize..MaxGridSize || gridRows !in MinGridSize..MaxGridSize) {
        return null
    }

    val beads = values["beads"]?.split(',')?.map { token ->
        token.toIntOrNull() ?: return null
    } ?: return null
    if (beads.size != gridColumns * gridRows || beads.any { !isValidBeadColor(it) }) {
        return null
    }

    return BoardSnapshot(
        gridColumns = gridColumns,
        gridRows = gridRows,
        stitchModeId = StitchMode.fromId(values["stitch_mode_id"].orEmpty()).id,
        beadShapeId = BeadShape.fromId(values["bead_shape_id"].orEmpty()).id,
        beads = beads
    )
}
