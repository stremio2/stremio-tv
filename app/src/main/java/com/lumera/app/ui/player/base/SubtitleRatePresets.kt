package com.lumera.app.ui.player.base

import kotlin.math.abs

/**
 * Subtitle time scale. A rate is `subtitle fps / video fps`, so that
 * `video time = cue time * rate + delay`. It fixes subtitles that drift over the length of a film
 * because they were authored for a different frame rate than the video (e.g. 23.976 fps subtitles
 * on a 25 fps video), which a constant delay cannot fix.
 */
object SubtitleRatePresets {
    data class Preset(val id: String, val label: String, val rate: Double)

    private const val FPS_23_976 = 24000.0 / 1001.0
    private const val FPS_29_97 = 30000.0 / 1001.0
    private const val EPSILON = 1e-6

    const val MIN_RATE = 0.5
    const val MAX_RATE = 2.0

    val SAME = Preset("off", "Same fps", 1.0)

    val all: List<Preset> = listOf(
        SAME,
        Preset("23976-25", "23.976 → 25 fps", FPS_23_976 / 25.0),
        Preset("25-23976", "25 → 23.976 fps", 25.0 / FPS_23_976),
        Preset("24-25", "24 → 25 fps", 24.0 / 25.0),
        Preset("25-24", "25 → 24 fps", 25.0 / 24.0),
        // 23.976 -> 24 and 29.97 -> 30 are the same ratio (1000/1001), as are their inverses,
        // so each pair is one entry; otherwise the second of a pair could never be selected.
        Preset("23976-24", "23.976 → 24 / 29.97 → 30", FPS_23_976 / 24.0),
        Preset("24-23976", "24 → 23.976 / 30 → 29.97", 24.0 / FPS_23_976),
        Preset("2997-25", "29.97 → 25 fps", FPS_29_97 / 25.0),
        Preset("25-2997", "25 → 29.97 fps", 25.0 / FPS_29_97),
    )

    /** Index of the preset matching [rate], or -1 for a custom value. */
    fun indexOf(rate: Double): Int = all.indexOfFirst { abs(it.rate - rate) < EPSILON }

    fun isValid(rate: Double): Boolean = rate.isFinite() && rate in MIN_RATE..MAX_RATE

    /** Returns [rate] if valid, else 1.0 (no scaling). */
    fun sanitize(rate: Double): Double = if (isValid(rate)) rate else SAME.rate

    /** The preset after (or before, if [step] < 0) [rate], wrapping around; a custom rate steps from "same". */
    fun step(rate: Double, step: Int): Preset {
        val current = indexOf(rate).takeIf { it >= 0 } ?: 0
        return all[Math.floorMod(current + step, all.size)]
    }

    fun label(rate: Double): String =
        all.getOrNull(indexOf(rate))?.label ?: "x%.4f".format(rate)

    /**
     * Maps a video playback position to the subtitle (cue) timeline:
     * `cue time = (video time - delay) / rate`, clamped at 0.
     */
    fun toCueTimeUs(videoPositionUs: Long, delayUs: Long, rate: Double): Long {
        val safeRate = sanitize(rate)
        val shifted = videoPositionUs - delayUs
        val scaled = if (safeRate == 1.0) shifted else (shifted / safeRate).toLong()
        return scaled.coerceAtLeast(0L)
    }
}
