package com.commandbar.android

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Bridges an Android `WebView` file `<input>` to the system file picker.
 *
 * Android's `WebView` does not open a picker for `<input type="file">` on its own — unlike
 * iOS's `WKWebView`, which handles it natively. The embedder must override
 * [WebChromeClient.onShowFileChooser], launch a picker `Intent`, and hand the selected URIs
 * back through the [ValueCallback]. Because the WebView may be hosted in a `Dialog` (the
 * Resource Center / Assistant bottom sheet) or an arbitrary host `Activity` we cannot rely on,
 * we route the picker through a dedicated transparent [EngagementFileChooserActivity] and shuttle
 * the in-flight callback through this object. This keeps the flow self-contained: the host app
 * does not need to forward `onActivityResult`.
 */
internal object EngagementFileChooser {
    private const val TAG = "EngagementFileChooser"

    @Volatile
    private var pendingCallback: ValueCallback<Array<Uri>>? = null

    @Volatile
    private var pendingIntent: Intent? = null

    /**
     * Starts the system file picker for a WebView file input.
     *
     * @return `true` if the picker was launched (the [callback] will be invoked later, possibly
     *   with `null` if the user cancels); `false` if it could not be launched, in which case the
     *   caller must not also invoke [callback].
     */
    fun show(
        context: Context,
        params: WebChromeClient.FileChooserParams?,
        callback: ValueCallback<Array<Uri>>,
    ): Boolean {
        val pickerIntent = params?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        // A picker is already in flight; cancel it so its WebView input is not left waiting.
        pendingCallback?.onReceiveValue(null)
        pendingCallback = callback
        pendingIntent = pickerIntent

        return try {
            val launchIntent = Intent(context, EngagementFileChooserActivity::class.java)
            if (context !is Activity) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(launchIntent)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to launch file chooser", e)
            deliverResult(null)
            false
        }
    }

    /** Consumes the picker intent for the proxy activity. Returns `null` if nothing is pending. */
    fun consumePendingIntent(): Intent? {
        val intent = pendingIntent
        pendingIntent = null
        return intent
    }

    /** Delivers the picked URIs (or `null` when cancelled/failed) to the waiting WebView input. */
    fun deliverResult(results: Array<Uri>?) {
        val callback = pendingCallback
        pendingCallback = null
        pendingIntent = null
        callback?.onReceiveValue(results)
    }

    /**
     * Copies a freshly-picked document into the SDK cache and returns a stable `content://` URI
     * served by [EngagementFileProvider].
     *
     * The URI handed back by the system picker carries a temporary read grant scoped to the
     * picking activity. Our proxy activity finishes immediately, and on Android 11+ `WebView`
     * also blocks `file://` access by default — so by the time the WebView reads bytes to build
     * the multipart upload, the original grant may be gone, yielding an empty/unreadable `File`
     * and a server-side upload failure. Copying while we still hold the grant and re-exposing the
     * bytes through our own (same-process) provider makes the read deterministic on every API
     * level. Returns `null` if the copy fails so the file is simply omitted from the result.
     */
    fun copyToReadableUri(context: Context, source: Uri): Uri? {
        return try {
            val resolver = context.contentResolver
            val displayName = queryDisplayName(resolver, source) ?: "upload_${System.currentTimeMillis()}"
            val safeName = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")

            val uploadsDir = File(context.cacheDir, UPLOADS_DIR).apply { mkdirs() }
            pruneStaleUploads(uploadsDir)

            val destination = File(uploadsDir, "${System.currentTimeMillis()}_$safeName")
            resolver.openInputStream(source)?.use { input ->
                FileOutputStream(destination).use { output -> input.copyTo(output) }
            } ?: return null

            FileProvider.getUriForFile(context, fileProviderAuthority(context), destination)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cache picked file for upload", e)
            null
        }
    }

    /** Authority must match the `${'$'}{applicationId}` provider declared in the SDK manifest. */
    private fun fileProviderAuthority(context: Context): String =
        "${context.packageName}.commandbar.engagementfileprovider"

    private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
        return try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index) else null
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Best-effort cleanup so cached uploads do not accumulate across sessions. */
    private fun pruneStaleUploads(dir: File) {
        val cutoff = System.currentTimeMillis() - STALE_UPLOAD_TTL_MS
        dir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoff) {
                file.delete()
            }
        }
    }

    private const val UPLOADS_DIR = "amplitude_uploads"
    private const val STALE_UPLOAD_TTL_MS = 6 * 60 * 60 * 1000L
}

/**
 * SDK-owned [FileProvider] subclass. A distinct class name (vs. reusing
 * `androidx.core.content.FileProvider`) avoids a manifest-merger collision if the host app also
 * declares a FileProvider.
 */
internal class EngagementFileProvider : FileProvider()

/**
 * Transparent proxy activity that launches the system file picker on behalf of a WebView file
 * input and forwards the result back through [EngagementFileChooser]. Declared in the SDK
 * manifest; not intended to be started by host apps directly.
 */
internal class EngagementFileChooserActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If the process was recreated mid-pick we cannot recover the original request; bail out
        // so the WebView input is not left hanging.
        if (savedInstanceState != null) {
            finishWithResult(null)
            return
        }

        val pickerIntent = EngagementFileChooser.consumePendingIntent()
        if (pickerIntent == null) {
            finishWithResult(null)
            return
        }

        try {
            startActivityForResult(pickerIntent, REQUEST_CODE_FILE_CHOOSER)
        } catch (e: ActivityNotFoundException) {
            finishWithResult(null)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_FILE_CHOOSER) {
            val picked = WebChromeClient.FileChooserParams.parseResult(resultCode, data)
            // Copy into SDK-owned storage now, while this activity still holds the read grant.
            val readable = picked
                ?.mapNotNull { EngagementFileChooser.copyToReadableUri(this, it) }
                ?.toTypedArray()
            finishWithResult(readable)
        }
    }

    private fun finishWithResult(results: Array<Uri>?) {
        EngagementFileChooser.deliverResult(results)
        finish()
        overridePendingTransition(0, 0)
    }

    companion object {
        private const val REQUEST_CODE_FILE_CHOOSER = 0xCB01
    }
}
