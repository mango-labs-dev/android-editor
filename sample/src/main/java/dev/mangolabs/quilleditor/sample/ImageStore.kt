package dev.mangolabs.quilleditor.sample

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/**
 * Loads an image from a content/file URI, resamples it to a max long edge,
 * compresses to JPEG, and writes it under filesDir/images/<uuid>.jpg.
 * Returns the generated UUID; the caller forms an app-image://<uuid> URL
 * and hands it to [dev.mangolabs.quilleditor.QuillState.insertImage].
 */
class ImageStore(private val context: Context) {

  suspend fun saveImage(uri: Uri): String = withContext(Dispatchers.IO) {
    val source = context.contentResolver.openInputStream(uri).use { input ->
      BitmapFactory.decodeStream(input)
    } ?: throw IOException("Cannot decode image from $uri")

    val resampled = resample(source, MAX_LONG_EDGE_PX)
    val id = UUID.randomUUID().toString()
    val outFile = File(context.filesDir, "images/$id.jpg").apply {
      parentFile?.mkdirs()
    }
    FileOutputStream(outFile).use { out ->
      resampled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
    }
    id
  }

  private fun resample(src: Bitmap, maxEdge: Int): Bitmap {
    val longEdge = maxOf(src.width, src.height)
    if (longEdge <= maxEdge) return src
    val scale = maxEdge.toFloat() / longEdge
    return Bitmap.createScaledBitmap(
      src,
      (src.width * scale).toInt(),
      (src.height * scale).toInt(),
      true
    )
  }

  companion object {
    private const val MAX_LONG_EDGE_PX = 1080
    private const val JPEG_QUALITY = 80
  }
}
