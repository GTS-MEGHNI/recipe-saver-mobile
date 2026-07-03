package com.recipesaver.app.data.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Saves recipe gallery photos into the app's private internal storage
 * (`filesDir/recipe_images/`), which persists across app updates and is never cleared by the OS
 * (unlike `cacheDir`). Every image is downsampled to at most [MAX_EDGE] px on its longest edge and
 * re-encoded as JPEG before saving, so full-resolution camera photos never hit disk. Orientation
 * is baked in from the source EXIF, since re-encoding drops the metadata Coil would otherwise use.
 */
class ImageStorageManager(
    private val context: Context,
) {
    private val imagesDir: File
        get() = File(context.filesDir, IMAGES_DIR).apply { mkdirs() }

    /**
     * Copies [source] (a picker `content://` URI) into internal storage, downsampled and rotated
     * upright. Returns a Coil-loadable `file://` URI string, or null if the image couldn't be read.
     * [uniqueSuffix] disambiguates files saved in the same millisecond.
     */
    suspend fun saveImage(
        source: Uri,
        recipeId: Long,
        uniqueSuffix: Int,
    ): String? =
        withContext(Dispatchers.IO) {
            val bitmap = decodeDownsampled(source) ?: return@withContext null
            val rotated = applyExifRotation(source, bitmap)
            val file = File(imagesDir, "recipe_${recipeId}_${System.currentTimeMillis()}_$uniqueSuffix.jpg")
            try {
                file.outputStream().use { out ->
                    rotated.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                }
            } finally {
                rotated.recycle()
                if (rotated !== bitmap) bitmap.recycle()
            }
            file.toUri().toString()
        }

    /** Deletes the file backing a stored `file://` URI. No-op if it's already gone. */
    fun deleteImage(fileUri: String) {
        runCatching {
            val file = fileUri.toUri().path?.let(::File) ?: return
            if (file.exists()) file.delete()
        }
    }

    /** Decodes [source] with an `inSampleSize` that keeps it at least [MAX_EDGE] px, then scales down. */
    private fun decodeDownsampled(source: Uri): Bitmap? {
        val bounds =
            BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val decodeOptions =
            BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
            }
        val decoded =
            context.contentResolver.openInputStream(source)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            } ?: return null

        return scaleToMaxEdge(decoded)
    }

    private fun sampleSizeFor(
        width: Int,
        height: Int,
    ): Int {
        var sample = 1
        var longest = maxOf(width, height)
        while (longest / 2 >= MAX_EDGE) {
            longest /= 2
            sample *= 2
        }
        return sample
    }

    private fun scaleToMaxEdge(bitmap: Bitmap): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= MAX_EDGE) return bitmap
        val scale = MAX_EDGE.toFloat() / longest
        val scaled =
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true,
            )
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    private fun applyExifRotation(
        source: Uri,
        bitmap: Bitmap,
    ): Bitmap {
        val orientation =
            context.contentResolver.openInputStream(source)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private companion object {
        const val IMAGES_DIR = "recipe_images"
        const val MAX_EDGE = 1080
        const val JPEG_QUALITY = 85
    }
}
