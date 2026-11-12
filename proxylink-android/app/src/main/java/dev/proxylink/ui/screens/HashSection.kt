package dev.proxylink.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.proxylink.R
import dev.proxylink.ui.components.*
import dev.proxylink.ui.theme.MonoStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

// manual sha-256 of a local file, compared with a hash the user pastes. everything runs on the phone, nothing is sent anywhere.
// the file is read in 64 kB pieces, never whole into memory. a match only says the bytes are the same as the file the hash came from
private enum class Cmp { NONE, MATCH, DIFFERENT, INVALID }

private fun normalize(s: String): String =
    s.lowercase().removePrefix("sha256:").removePrefix("sha-256:").filter { it.isLetterOrDigit() }

@Composable
fun HashSection(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf<String?>(null) }
    var hash by rememberSaveable { mutableStateOf<String?>(null) }
    var size by rememberSaveable { mutableStateOf(-1L) }
    var done by remember { mutableLongStateOf(0L) }
    var working by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    var pasted by rememberSaveable { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }

    fun compute(uri: Uri) {
        name = null; hash = null; failed = false; copied = false; done = 0; size = -1
        runCatching {
            ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (ni >= 0) name = c.getString(ni)
                    val si = c.getColumnIndex(OpenableColumns.SIZE)
                    if (si >= 0 && !c.isNull(si)) size = c.getLong(si)
                }
            }
        }
        if (name == null) name = "file"
        working = true
        job = scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val md = MessageDigest.getInstance("SHA-256")
                    val buf = ByteArray(65_536)
                    var total = 0L
                    var shown = 0L
                    ctx.contentResolver.openInputStream(uri)!!.use { s ->
                        while (isActive) {
                            val n = s.read(buf)
                            if (n < 0) break
                            md.update(buf, 0, n)
                            total += n
                            if (total - shown >= 1_048_576) { shown = total; done = total }
                        }
                    }
                    if (!isActive) null else md.digest().joinToString("") { "%02x".format(it) }
                }
            }
            working = false
            val h = result.getOrNull()
            if (result.isFailure) failed = true
            else if (h != null) { hash = h; done = if (size > 0) size else done }
        }
    }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) compute(uri) }
    val expected = normalize(pasted)
    val cmp = when {
        hash == null || expected.isEmpty() -> Cmp.NONE
        expected.length != 64 || !expected.all { it in "0123456789abcdef" } -> Cmp.INVALID
        expected == hash -> Cmp.MATCH
        else -> Cmp.DIFFERENT
    }

    SubScreen(stringResource(R.string.title_hash), onBack) {
        Panel {
            Text(stringResource(R.string.hash_intro), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppButton(onClick = { pick.launch(arrayOf("*/*")) }, enabled = !working) { Text(stringResource(R.string.hash_pick)) }
                if (working) AppOutlinedButton(onClick = { job?.cancel(); working = false }) { Text(stringResource(R.string.cancel)) }
            }
        }
        if (name != null) {
            Panel {
                Text(name ?: "", style = MaterialTheme.typography.titleSmall)
                when {
                    working -> {
                        if (size > 0) LinearProgressIndicator(progress = { (done.toFloat() / size).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
                        else LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
                        Text(stringResource(R.string.hash_working), style = MaterialTheme.typography.bodySmall)
                    }
                    failed -> Text(stringResource(R.string.hash_failed), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    hash != null -> {
                        Text("SHA-256", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        SelectionContainer { Text(hash ?: "", style = MonoStyle) }
                        Spacer(Modifier.height(8.dp))
                        AppOutlinedButton(onClick = {
                            ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("sha-256", hash))
                            copied = true
                        }) { Text(stringResource(if (copied) R.string.copied else R.string.hash_copy)) }
                    }
                    else -> Text(stringResource(R.string.hash_cancelled), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Panel {
            OutlinedTextField(
                value = pasted, onValueChange = { pasted = it },
                label = { Text(stringResource(R.string.hash_paste)) },
                textStyle = MonoStyle, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            // the result is always words, never only a color
            Text(
                stringResource(
                    when (cmp) {
                        Cmp.NONE -> if (hash == null) R.string.hash_cmp_wait_file else R.string.hash_cmp_wait_text
                        Cmp.MATCH -> R.string.hash_cmp_match
                        Cmp.DIFFERENT -> R.string.hash_cmp_diff
                        Cmp.INVALID -> R.string.hash_cmp_invalid
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = if (cmp == Cmp.DIFFERENT || cmp == Cmp.INVALID) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            if (cmp == Cmp.MATCH) {
                Text(stringResource(R.string.hash_match_limit), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(stringResource(R.string.hash_not_signature), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
