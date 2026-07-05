package com.example.fitto.data.remote.logmeal

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.io.FileOutputStream

internal interface LogMealImagePreparer {
    fun prepare(sourceFile: File): File?
}

internal class AndroidLogMealImagePreparer : LogMealImagePreparer {

    override fun prepare(sourceFile: File): File? {
        val originalBitmap = BitmapFactory.decodeFile(sourceFile.absolutePath) ?: return null
        val scaledBitmap = originalBitmap.scaleDownIfNeeded(maxSide = MAX_IMAGE_SIDE)
        val outputFile = File(
            sourceFile.parentFile,
            "logmeal_${sourceFile.nameWithoutExtension}.jpg"
        )

        var quality = INITIAL_JPEG_QUALITY
        do {
            FileOutputStream(outputFile).use { output ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
            }
            quality -= JPEG_QUALITY_STEP
        } while (outputFile.length() > MAX_UPLOAD_BYTES && quality >= MIN_JPEG_QUALITY)

        if (scaledBitmap != originalBitmap) {
            originalBitmap.recycle()
            scaledBitmap.recycle()
        }

        Log.d(TAG, "Prepared image size=${outputFile.length()} quality=${quality + JPEG_QUALITY_STEP}")
        return outputFile
    }

    private fun Bitmap.scaleDownIfNeeded(maxSide: Int): Bitmap {
        val currentMaxSide = maxOf(width, height)
        if (currentMaxSide <= maxSide) return this

        val ratio = maxSide.toFloat() / currentMaxSide.toFloat()
        val targetWidth = (width * ratio).toInt().coerceAtLeast(1)
        val targetHeight = (height * ratio).toInt().coerceAtLeast(1)

        return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
    }

    private companion object {
        const val TAG = "LogMealClient"
        const val MAX_IMAGE_SIDE = 1000
        const val MAX_UPLOAD_BYTES = 1_000_000L
        const val INITIAL_JPEG_QUALITY = 88
        const val JPEG_QUALITY_STEP = 8
        const val MIN_JPEG_QUALITY = 48
    }
}
