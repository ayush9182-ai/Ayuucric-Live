package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream

object MediaAttachmentHelper {

    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Scales and compresses a [Bitmap] to high definition data URI string (data:image/jpeg;base64,...)
     * ensuring crystal-clear text readability while staying safely within Firestore limits.
     */
    fun bitmapToDataUrl(bitmap: Bitmap, maxDimension: Int = 2048, quality: Int = 92): String {
        val width = bitmap.width
        val height = bitmap.height

        val scaled = if (width > maxDimension || height > maxDimension) {
            val ratio = width.toFloat() / height.toFloat()
            val targetW: Int
            val targetH: Int
            if (ratio > 1f) {
                targetW = maxDimension
                targetH = (maxDimension / ratio).toInt().coerceAtLeast(1)
            } else {
                targetH = maxDimension
                targetW = (maxDimension * ratio).toInt().coerceAtLeast(1)
            }
            Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
        } else {
            bitmap
        }

        var currentQuality = quality
        var baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, currentQuality, baos)
        var byteArray = baos.toByteArray()

        // Adaptive guard to stay safely under 850KB for fast Firestore sync while preserving sharp text
        while (byteArray.size > 850_000 && currentQuality > 60) {
            currentQuality -= 6
            baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, currentQuality, baos)
            byteArray = baos.toByteArray()
        }

        return "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    /**
     * Reads a media [Uri] selected from Gallery / Photo Picker and compresses it to a data URL.
     */
    fun uriToDataUrl(context: Context, uri: Uri, isVideo: Boolean = false): String? {
        return try {
            val bitmap: Bitmap? = if (isVideo) {
                extractVideoFrame(context, uri)
            } else {
                decodeImageUri(context, uri)
            }
            if (bitmap != null) {
                bitmapToDataUrl(bitmap)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeImageUri(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                val input = context.contentResolver.openInputStream(uri)
                val bmp = BitmapFactory.decodeStream(input)
                input?.close()
                bmp
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractVideoFrame(context: Context, uri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * Decodes a Base64 data URL (e.g. data:image/jpeg;base64,...) or raw Base64 string into a [Bitmap].
     * Uses unscaled ARGB_8888 config to preserve fine text and detail.
     */
    fun decodeDataUrlToBitmap(dataUrl: String): Bitmap? {
        if (dataUrl.isBlank()) return null
        return try {
            val base64Data = if (dataUrl.contains(",")) {
                dataUrl.substringAfter(",")
            } else {
                dataUrl
            }
            val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
            val options = BitmapFactory.Options().apply {
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size, options)
        } catch (e: Exception) {
            null
        }
    }
}
