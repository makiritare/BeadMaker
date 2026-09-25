package com.example.beadmaker.ui.state

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File
import java.io.IOException
import java.util.UUID

private const val TemplateImageDirectoryName = "template_images"

internal class TemplateImageStorage(private val context: Context) {

    fun createCaptureUri(): Uri? {
        var imageFile: File? = null
        return try {
            val newImageFile = File(
                ensureTemplateImageDirectory(),
                "template_${UUID.randomUUID()}.jpg"
            ).apply {
                if (!createNewFile()) throw IOException("Failed to create template image file.")
            }
            imageFile = newImageFile
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                newImageFile
            )
        } catch (_: Exception) {
            imageFile?.delete()
            null
        }
    }

    fun newCacheFile(): File {
        return File(
            File(context.cacheDir, TemplateImageDirectoryName),
            "template_${UUID.randomUUID()}.jpg"
        )
    }

    fun copyToCache(sourceUri: Uri, destinationFile: File = newCacheFile()): Uri? {
        return try {
            val imageDirectory = ensureTemplateImageDirectory()
            if (!isDirectChildOfDirectory(destinationFile, imageDirectory)) {
                throw IOException("Template image destination is outside the cache directory.")
            }
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                destinationFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: throw IOException("Failed to open template image stream.")
            destinationFile.toUri()
        } catch (_: Exception) {
            destinationFile.delete()
            null
        }
    }

    fun delete(uriString: String?): Boolean {
        val uri = uriString?.let(Uri::parse) ?: return false
        return when (uri.scheme) {
            ContentResolver.SCHEME_CONTENT -> {
                if (!isOwnedCaptureUri(uri)) {
                    false
                } else {
                    runCatching { context.contentResolver.delete(uri, null, null) > 0 }
                        .getOrDefault(false)
                }
            }

            ContentResolver.SCHEME_FILE, null -> {
                val cachedFile = uri.path?.let(::File) ?: return false
                val cacheDirectory = File(context.cacheDir, TemplateImageDirectoryName)
                isDirectChildOfDirectory(cachedFile, cacheDirectory) &&
                    cachedFile.exists() &&
                    cachedFile.delete()
            }

            else -> false
        }
    }

    fun isOwnedCaptureUri(uri: Uri): Boolean {
        return uri.scheme == ContentResolver.SCHEME_CONTENT &&
            uri.authority == "${context.packageName}.fileprovider"
    }

    private fun ensureTemplateImageDirectory(): File {
        return File(context.cacheDir, TemplateImageDirectoryName).apply {
            if (!exists() && !mkdirs()) {
                throw IOException("Failed to create template image cache directory.")
            }
            if (!isDirectory) {
                throw IOException("Template image cache path is not a directory.")
            }
        }
    }
}

fun createTemplateCaptureUri(context: Context): Uri? {
    return TemplateImageStorage(context.applicationContext).createCaptureUri()
}

fun copyTemplateImageToCache(context: Context, sourceUri: Uri): Uri? {
    return TemplateImageStorage(context.applicationContext).copyToCache(sourceUri)
}

fun deleteTemplateCacheFile(context: Context, uriString: String?): Boolean {
    return TemplateImageStorage(context.applicationContext).delete(uriString)
}

internal fun isDirectChildOfDirectory(file: File, directory: File): Boolean {
    return runCatching { file.canonicalFile.parentFile == directory.canonicalFile }
        .getOrDefault(false)
}
