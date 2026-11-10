package dev.proxylink.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.util.UUID

// smaller copy of a jpeg photo for the "send image" action only. the original file is never changed.
// re-encoding drops the metadata, including the location, the orientation is applied to the pixels first
object ImageOptimizer {
    enum class Quality(val key: String, val maxSide: Int, val jpeg: Int) {
        ORIGINAL("original", 0, 0),
        HIGH("high", 2560, 90),
        ECONOMY("economy", 1600, 80);

        companion object {
            fun from(key: String?) = entries.firstOrNull { it.key == key } ?: ORIGINAL
        }
    }

    class Result(val file: File, val name: String)

    // null = send the original: quality "original", not a jpeg, already small enough, or anything went wrong
    fun optimize(ctx: Context, uri: Uri, q: Quality): Result? {
        if (q == Quality.ORIGINAL) return null
        val cr = ctx.contentResolver
        if (cr.getType(uri) != "image/jpeg") return null   // png, gif, webp keep transparency and animation
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val w = bounds.outWidth
            val h = bounds.outHeight
            if (w <= 0 || h <= 0 || maxOf(w, h) <= q.maxSide) return null   // never enlarge

            var sample = 1
            while (maxOf(w, h) / (sample * 2) >= q.maxSide) sample *= 2
            val decoded = cr.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return null

            val scale = q.maxSide.toFloat() / maxOf(decoded.width, decoded.height)
            val matrix = Matrix().apply {
                if (scale < 1f) postScale(scale, scale)
                when (cr.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1) }) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(270f); postScale(-1f, 1f) }
                    else -> {}
                }
            }
            val out = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            if (out !== decoded) decoded.recycle()

            val dir = File(ctx.cacheDir, "send").apply { mkdirs() }
            val file = File(dir, UUID.randomUUID().toString() + ".jpg")
            file.outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, q.jpeg, it) }
            out.recycle()

            var base = "image"
            runCatching {
                cr.query(uri, null, null, null, null)?.use { c ->
                    val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (c.moveToFirst() && i >= 0) c.getString(i)?.let { base = it.substringBeforeLast('.') }
                }
            }
            Result(file, "$base.jpg")
        } catch (_: Throwable) {   // a broken image or too little memory must not crash the app
            null
        }
    }
}
