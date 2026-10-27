package com.lumera.app.ui.player.base

import android.util.Log

/**
 * Logging for the play flow, all under one tag so a whole session can be read with
 * `adb logcat -s LumeraPlay`.
 *
 * Stream URLs (debrid links, tokenised addon URLs) carry credentials, so never log them raw:
 * use [redact].
 */
object PlayerLog {
    const val TAG = "LumeraPlay"

    private val urlPattern = Regex("""(?:https?|magnet|rtmp|rtsp)[:][^\s"'<>)]+""")

    fun d(message: String) {
        Log.d(TAG, scrub(message))
    }

    fun i(message: String) {
        Log.i(TAG, scrub(message))
    }

    fun w(message: String, error: Throwable? = null) {
        Log.w(TAG, scrub(withTrace(message, error)))
    }

    fun e(message: String, error: Throwable? = null) {
        Log.e(TAG, scrub(withTrace(message, error)))
    }

    /**
     * Replaces every URL in [text] with its redacted form (idempotent: redacted output is left alone). Applied to all log output, because source
     * ids are the stream URL itself and exception messages often embed the URL that failed.
     */
    fun scrub(text: String): String = urlPattern.replace(text) { match ->
        val alreadyRedacted = match.value.contains("/...(") ||
            text.startsWith("<redacted>", match.range.last + 1)
        if (alreadyRedacted) match.value else redact(match.value)
    }

    private fun withTrace(message: String, error: Throwable?): String =
        if (error == null) message else message + "\n" + Log.getStackTraceString(error)

    /** `scheme://host/...(N chars)`: enough to tell sources apart, without path, query or token. */
    fun redact(url: String?): String {
        if (url.isNullOrBlank()) return "<none>"
        if (url.startsWith("magnet:")) return "magnet:<redacted>(${url.length} chars)"
        val schemeEnd = url.indexOf("://")
        if (schemeEnd < 0) return "<${url.length} chars>"
        val hostEnd = url.indexOf('/', schemeEnd + 3).takeIf { it >= 0 } ?: url.length
        val host = url.substring(schemeEnd + 3, hostEnd).substringAfter('@')
        return "${url.substring(0, schemeEnd)}://$host/...(${url.length} chars)"
    }
}
