package com.lumera.app.ui.player.base

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleRatePresetsTest {
    @Test
    fun sameRateIsIdentityAndKeepsDelay() {
        assertEquals(5_000_000L, SubtitleRatePresets.toCueTimeUs(5_000_000L, 0L, 1.0))
        assertEquals(4_000_000L, SubtitleRatePresets.toCueTimeUs(5_000_000L, 1_000_000L, 1.0))
        assertEquals(6_000_000L, SubtitleRatePresets.toCueTimeUs(5_000_000L, -1_000_000L, 1.0))
    }

    @Test
    fun clampsAtZero() {
        assertEquals(0L, SubtitleRatePresets.toCueTimeUs(500_000L, 2_000_000L, 1.0))
    }

    @Test
    fun cueAtOneHourOf23976SubsLandsAtMatchingVideoTimeOn25Fps() {
        // Subtitle timestamps authored for 23.976 fps, video is 25 fps (shorter by 23.976/25).
        // A cue stamped 3600 s must show at video time 3600 * (23.976/25) s.
        val rate = SubtitleRatePresets.all.first { it.id == "23976-25" }.rate
        val videoUs = (3_600_000_000.0 * rate).toLong()
        val cueUs = SubtitleRatePresets.toCueTimeUs(videoUs, 0L, rate)
        assertEquals(3_600_000_000.0, cueUs.toDouble(), 2.0)
    }

    @Test
    fun delayIsAppliedAfterScaling() {
        // video = cue * rate + delay  =>  cue = (video - delay) / rate
        val rate = 0.5
        assertEquals(10_000_000L, SubtitleRatePresets.toCueTimeUs(6_000_000L, 1_000_000L, rate))
    }

    @Test
    fun invalidRateFallsBackToIdentity() {
        assertEquals(1_000L, SubtitleRatePresets.toCueTimeUs(1_000L, 0L, 0.0))
        assertEquals(1_000L, SubtitleRatePresets.toCueTimeUs(1_000L, 0L, Double.NaN))
        assertEquals(1_000L, SubtitleRatePresets.toCueTimeUs(1_000L, 0L, 10.0))
    }

    @Test
    fun presetsAreValidDistinctAndInverseRelated() {
        assertTrue(SubtitleRatePresets.all.all { SubtitleRatePresets.isValid(it.rate) })
        assertEquals(SubtitleRatePresets.all.size, SubtitleRatePresets.all.map { it.id }.toSet().size)
        // Every preset must resolve to itself, or stepping gets stuck on an earlier duplicate.
        SubtitleRatePresets.all.forEachIndexed { i, p -> assertEquals(i, SubtitleRatePresets.indexOf(p.rate)) }
        val a = SubtitleRatePresets.all.first { it.id == "23976-25" }.rate
        val b = SubtitleRatePresets.all.first { it.id == "25-23976" }.rate
        assertEquals(1.0, a * b, 1e-9)
    }

    @Test
    fun indexOfAndLabel() {
        assertEquals(0, SubtitleRatePresets.indexOf(1.0))
        assertEquals(-1, SubtitleRatePresets.indexOf(1.2345))
        assertFalse(SubtitleRatePresets.isValid(3.0))
        assertEquals("Same fps", SubtitleRatePresets.label(1.0))
        assertEquals("x1.2345", SubtitleRatePresets.label(1.2345))
    }

    @Test
    fun stepWrapsBothWays() {
        val n = SubtitleRatePresets.all.size
        assertEquals(SubtitleRatePresets.all[1], SubtitleRatePresets.step(1.0, 1))
        assertEquals(SubtitleRatePresets.all[n - 1], SubtitleRatePresets.step(1.0, -1))
        assertEquals(SubtitleRatePresets.all[0], SubtitleRatePresets.step(SubtitleRatePresets.all[n - 1].rate, 1))
    }
}
