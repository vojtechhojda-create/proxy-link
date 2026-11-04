package app.proxylink

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun loadbitmap(ctx: Context, uri: Uri, max: Int): Bitmap? = runCatching {
    val cr = ctx.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / sample > max * 2 || bounds.outHeight / sample > max * 2) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
}.getOrNull()

@Composable
fun thumb(uri: String, modifier: Modifier = Modifier.fillMaxSize()) {
    val ctx = LocalContext.current
    val bmp by produceState<ImageBitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) { loadbitmap(ctx, Uri.parse(uri), 160)?.asImageBitmap() }
    }
    val b = bmp
    if (b != null) {
        Image(bitmap = b, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
    }
}
