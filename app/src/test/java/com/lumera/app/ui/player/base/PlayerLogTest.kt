package com.lumera.app.ui.player.base

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerLogTest {
    private val secret = "CpTaiJSh_mr1C1tfpL25Ht5Bdc9d4S6Wb5GCe4Cko0X6Z4"

    @Test
    fun redactKeepsOnlySchemeHostAndLength() {
        val url = "https://comet.elfhosted.com/playback/$secret/file.mkv?token=abc"
        val out = PlayerLog.redact(url)
        assertEquals("https://comet.elfhosted.com/...(${url.length} chars)", out)
        assertFalse(out.contains(secret))
    }

    @Test
    fun redactDropsUserInfoAndMagnets() {
        val withUserInfo = "https://user:pass@host.example/a/b/c.mkv"
        val redacted = PlayerLog.redact(withUserInfo)
        assertEquals("https://host.example/...(${withUserInfo.length} chars)", redacted)
        assertFalse(redacted.contains("pass"))
        assertTrue(PlayerLog.redact("magnet:?xt=urn:btih:abcdef&dn=x").startsWith("magnet:<redacted>"))
        assertEquals("<none>", PlayerLog.redact(null))
        assertEquals("<none>", PlayerLog.redact(" "))
    }

    @Test
    fun scrubRemovesUrlsEverywhereInText() {
        val url = "https://comet.elfhosted.com/playback/$secret/file.mkv"
        val text = "state=READY source=$url error: Unable to connect to $url (timeout)"
        val out = PlayerLog.scrub(text)
        assertFalse(out.contains(secret))
        assertFalse(out.contains("file.mkv"))
        assertTrue(out.contains("comet.elfhosted.com"))
        assertTrue(out.endsWith("(timeout)"))
    }

    @Test
    fun scrubLeavesPlainTextAlone() {
        val text = "state=BUFFERING pos=0ms dur=100ms rate=Same fps"
        assertEquals(text, PlayerLog.scrub(text))
    }

    @Test
    fun scrubIsIdempotent() {
        val raw = "src=https://host.example/a/b?token=secret magnet:?xt=urn:btih:abc"
        val once = PlayerLog.scrub(raw)
        assertEquals(once, PlayerLog.scrub(once))
    }
}
