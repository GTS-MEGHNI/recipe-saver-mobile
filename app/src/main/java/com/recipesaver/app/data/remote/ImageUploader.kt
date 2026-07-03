package com.recipesaver.app.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

/**
 * Turns a picked `content://` image into a multipart body part for upload. The image is downsampled
 * to at most [MAX_EDGE] px on its longest edge, rotated upright from its EXIF orientation, and
 * re-encoded as JPEG — so full-resolution camera photos aren't shipped over the network (the server
 * also caps and optimizes, but shrinking client-side keeps uploads fast). Field name is `image`,
 * matching the API's validation.
 */
class ImageUploader(
    private val context: Context,
) {
    /** Builds the `image` multipart part, or null if [source] couldn't be decoded. */
    suspend fun buildPart(source: Uri): MultipartBody.Part? =
        withContext(Dispatchers.IO) {
            val bitmap = decodeDownsampled(source) ?: return@withContext null
            val rotated = applyExifRotation(source, bitmap)
            val bytes =
                ByteArrayOutputStream().use { out ->
                    rotated.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                    out.toByteArray()
                }
            rotated.recycle()
            if (rotated !== bitmap) bitmap.recycle()

            val body = bytes.toRequestBody("image/jpeg".toMediaType())
            MultipartBody.Part.createFormData("image", "upload.jpg", body)
        }

    private fun decodeDownsampled(source: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
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
        const val MAX_EDGE = 1080
        const val JPEG_QUALITY = 85
    }
}
