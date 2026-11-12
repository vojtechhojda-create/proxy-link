package dev.proxylink.core

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import dev.proxylink.R
import java.security.MessageDigest

enum class IdentityState { DEBUG, NOT_CONFIGURED, UNKNOWN, MATCH, MISMATCH }

// local check of the signature of this installed app against the public release fingerprint(s) in res/values/release_cert.xml.
// limits: it runs inside the app, so someone who modifies the app can remove or bypass it, and it says nothing about the remote
// client. an install outside a store is no proof of modification. only a proven mismatch (a reference exists, the build is not
// debuggable, the signer is not in the reference) is MISMATCH; a missing reference or an unreadable signature is never a mismatch
object AppIdentity {
    @Volatile private var cached: Pair<IdentityState, List<String>>? = null

    private fun hex(b: ByteArray) = b.joinToString("") { "%02x".format(it) }

    private fun compute(ctx: Context): Pair<IdentityState, List<String>> {
        val signers = runCatching {
            val info = ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            info.signingInfo?.apkContentsSigners?.map { hex(MessageDigest.getInstance("SHA-256").digest(it.toByteArray())) } ?: emptyList()
        }.getOrDefault(emptyList())
        val reference = runCatching {
            ctx.resources.getStringArray(R.array.release_cert_sha256)
                .map { it.lowercase().filter { c -> c.isLetterOrDigit() } }
                .filter { it.length == 64 }
        }.getOrDefault(emptyList())
        val debuggable = ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val st = when {
            debuggable -> IdentityState.DEBUG
            reference.isEmpty() -> IdentityState.NOT_CONFIGURED
            signers.isEmpty() -> IdentityState.UNKNOWN
            signers.any { it in reference } -> IdentityState.MATCH
            else -> IdentityState.MISMATCH
        }
        return st to signers
    }

    private fun get(ctx: Context) = cached ?: compute(ctx).also { cached = it }

    fun state(ctx: Context): IdentityState = get(ctx).first

    // sha-256 of the certificate this installed app is signed with, to compare with your own release certificate
    fun signerFingerprints(ctx: Context): List<String> = get(ctx).second
}
