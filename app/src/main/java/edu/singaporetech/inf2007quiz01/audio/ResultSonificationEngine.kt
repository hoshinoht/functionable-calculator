package edu.singaporetech.inf2007quiz01.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.sin

/**
 * Result Sonification Engine --- Because Seeing the Answer Isn't Enough
 *
 * Converts calculator results into musical melodies by mapping each digit
 * to a frequency in a musically-appropriate scale. The scale is chosen via
 * rigorous number-theoretic classification:
 *
 *   - Prime results get a MAJOR_CHORD (triumphant, heroic)
 *   - Fibonacci results get PENTATONIC (mystical, ancient)
 *   - Everything else gets CHROMATIC (atonal, unsettling --- like your GPA)
 *
 * This is the audio equivalent of our raytracer: completely unnecessary,
 * computationally expensive, and deeply satisfying to nobody in particular.
 *
 * The engine generates PCM sine waves at 44.1kHz, applies attack/release
 * envelopes to prevent audible clicks, and streams them through Android's
 * AudioTrack API on a background thread. Your calculator now doubles as
 * a synthesiser. You're welcome.
 *
 * Technical specs that nobody asked for:
 *   - Sample rate: 44100 Hz (CD quality, for a calculator)
 *   - Bit depth: 16-bit signed PCM (because 8-bit is for amateurs)
 *   - Channels: Mono (stereo would require a distributed consensus engine)
 *   - Envelope: 10ms linear attack/release (studio-grade click prevention)
 */
@Singleton
class ResultSonificationEngine @Inject constructor() {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val NOTE_DURATION_MS = 150L
        private const val ENVELOPE_MS = 10

        /**
         * Chromatic scale: C4 through A4
         * For those unremarkable results that are neither prime nor Fibonacci.
         * Musically equivalent to elevator music. Appropriate.
         */
        private val CHROMATIC_FREQUENCIES = doubleArrayOf(
            261.63, 277.18, 293.66, 311.13, 329.63,
            349.23, 369.99, 392.00, 415.30, 440.00
        )

        /**
         * Pentatonic scale: C4 pentatonic across two octaves.
         * Reserved for Fibonacci numbers. Because if the golden ratio
         * sounds like anything, it sounds like a pentatonic scale.
         * (Citation needed. Citation will never be provided.)
         */
        private val PENTATONIC_FREQUENCIES = doubleArrayOf(
            261.63, 293.66, 329.63, 392.00, 440.00,
            523.25, 587.33, 659.25, 783.99, 880.00
        )

        /**
         * C major chord harmonics: root, third, fifth, and their octaves.
         * The victory fanfare of mathematics. Plays when your result
         * is prime, because primes deserve to be celebrated.
         * Euler would have wanted this.
         */
        private val MAJOR_CHORD_FREQUENCIES = doubleArrayOf(
            261.63, 329.63, 392.00, 523.25, 659.25,
            783.99, 1046.50, 1318.51, 1567.98, 2093.00
        )

        /**
         * Known Fibonacci numbers up to 144.
         * We could compute these dynamically using our Byzantine Fault Tolerant
         * Fibonacci Consensus Engine, but that would require importing half the
         * app's dependency graph into an audio class. Even we have limits.
         * (Barely.)
         */
        private val FIBONACCI_SET = setOf(
            0L, 1L, 2L, 3L, 5L, 8L, 13L, 21L, 34L, 55L, 89L, 144L
        )
    }

    /**
     * Metadata about a sonification event.
     *
     * Returned so the caller can display which scale was used and how many
     * notes were played, in case anyone on Earth cares about the musical
     * properties of "7 + 3 = 10".
     */
    data class SonificationResult(
        /** The musical scale chosen: "MAJOR_CHORD", "PENTATONIC", or "CHROMATIC". */
        val scaleType: String,
        /** Number of notes (digits) that were sonified. */
        val noteCount: Int,
        /** Total duration in milliseconds. Shorter than the blockchain verification, at least. */
        val durationMs: Long
    )

    /** The currently playing AudioTrack, if any. Guarded by [lock]. */
    @Volatile
    private var currentTrack: AudioTrack? = null

    /** Synchronisation lock. Even our audio engine is thread-safe. You're welcome. */
    private val lock = Any()

    /**
     * Sonifies a calculator result string.
     *
     * Parses the numeric value, classifies it via number theory (prime vs
     * Fibonacci vs plebeian), selects the appropriate musical scale, maps
     * each digit to a frequency, generates PCM audio, and streams it to
     * the speaker.
     *
     * All of this happens because someone typed "2+2" on a calculator.
     *
     * @param result The calculator result string (e.g., "42", "-7", "3.14")
     * @return [SonificationResult] with metadata about the audio that was played
     */
    fun sonify(result: String): SonificationResult {
        // Stop any currently playing sonification (one melody at a time;
        // polyphonic support would require a mixing engine and at least
        // two more design patterns)
        stop()

        val scaleType = classifyResult(result)
        val frequencies = when (scaleType) {
            "MAJOR_CHORD" -> MAJOR_CHORD_FREQUENCIES
            "PENTATONIC" -> PENTATONIC_FREQUENCIES
            else -> CHROMATIC_FREQUENCIES
        }

        // Extract playable digits (skip '-' and '.' because they have
        // no musical representation. Yet.)
        val digits = result.filter { it.isDigit() }
            .map { it.digitToInt() }

        if (digits.isEmpty()) {
            return SonificationResult(scaleType, 0, 0L)
        }

        val totalDurationMs = digits.size * NOTE_DURATION_MS

        // Generate the complete PCM buffer for all notes
        val samplesPerNote = (SAMPLE_RATE * NOTE_DURATION_MS / 1000).toInt()
        val totalSamples = samplesPerNote * digits.size
        val pcmBuffer = ShortArray(totalSamples)
        val envelopeSamples = (SAMPLE_RATE * ENVELOPE_MS / 1000)

        for ((noteIndex, digit) in digits.withIndex()) {
            val frequency = frequencies[digit]
            val offset = noteIndex * samplesPerNote

            for (i in 0 until samplesPerNote) {
                // Generate sine wave
                val time = i.toDouble() / SAMPLE_RATE
                val sample = sin(2.0 * PI * frequency * time)

                // Apply attack/release envelope to avoid clicks
                // (because we are professionals, even when we're not)
                val envelope = when {
                    i < envelopeSamples -> i.toDouble() / envelopeSamples
                    i > samplesPerNote - envelopeSamples -> {
                        (samplesPerNote - i).toDouble() / envelopeSamples
                    }
                    else -> 1.0
                }

                // Scale to 16-bit signed range with some headroom
                pcmBuffer[offset + i] = (sample * envelope * Short.MAX_VALUE * 0.8).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    .toShort()
            }
        }

        // Stream the audio on a background thread because blocking the UI
        // thread to play calculator music would be a fireable offence
        // (the calculator itself is apparently fine though)
        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(totalSamples * 2) // * 2 because 16-bit = 2 bytes per sample

        Thread {
            val track = AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM
            )

            synchronized(lock) {
                currentTrack = track
            }

            try {
                track.play()
                track.write(pcmBuffer, 0, pcmBuffer.size)
                // Wait for playback to finish before releasing
                // (AudioTrack.write returns immediately in stream mode)
                Thread.sleep(totalDurationMs)
            } catch (_: IllegalStateException) {
                // Track was released by stop() — this is fine
            } catch (_: InterruptedException) {
                // Thread interrupted by stop() — also fine
            } finally {
                synchronized(lock) {
                    if (currentTrack === track) {
                        currentTrack = null
                    }
                }
                try {
                    track.stop()
                    track.release()
                } catch (_: IllegalStateException) {
                    // Already released — nothing to do
                }
            }
        }.apply {
            name = "SonificationThread"
            isDaemon = true
            start()
        }

        return SonificationResult(
            scaleType = scaleType,
            noteCount = digits.size,
            durationMs = totalDurationMs
        )
    }

    /**
     * Stops any currently playing sonification.
     *
     * Releases the AudioTrack immediately. The silence that follows
     * is arguably more pleasant than the sonification itself.
     */
    fun stop() {
        synchronized(lock) {
            currentTrack?.let { track ->
                currentTrack = null
                try {
                    track.stop()
                    track.release()
                } catch (_: IllegalStateException) {
                    // Already stopped/released — the AudioTrack has achieved nirvana
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NUMBER CLASSIFICATION — Serious Mathematics for Silly Purposes
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Classifies a result string into a musical scale category.
     *
     * The classification hierarchy (in order of priority):
     *   1. Prime → MAJOR_CHORD (the noble numbers)
     *   2. Fibonacci → PENTATONIC (the golden numbers)
     *   3. Everything else → CHROMATIC (the commoners)
     *
     * If the result isn't a valid number, it defaults to CHROMATIC,
     * because errors deserve atonal soundscapes.
     */
    private fun classifyResult(result: String): String {
        val numericValue = result.toDoubleOrNull() ?: return "CHROMATIC"
        val absIntValue = abs(numericValue.toLong())

        return when {
            isPrime(absIntValue) -> "MAJOR_CHORD"
            isFibonacci(absIntValue) -> "PENTATONIC"
            else -> "CHROMATIC"
        }
    }

    /**
     * Primality test via trial division.
     *
     * Yes, we could use Miller-Rabin. Yes, we could call out to Rust
     * and run it through the blockchain. We chose trial division because
     * even this codebase has a shame threshold. (It's very high, but it exists.)
     */
    private fun isPrime(n: Long): Boolean {
        if (n < 2) return false
        if (n == 2L || n == 3L) return true
        if (n % 2 == 0L || n % 3 == 0L) return false
        var i = 5L
        while (i * i <= n) {
            if (n % i == 0L || n % (i + 2) == 0L) return false
            i += 6
        }
        return true
    }

    /**
     * Checks if a number is in the Fibonacci sequence.
     *
     * Uses a hardcoded set for small values because computing Fibonacci
     * numbers dynamically would require invoking the Byzantine Fault Tolerant
     * Consensus Engine across 5 language implementations, and even we
     * recognise that's overkill for an audio feature. (Just barely.)
     *
     * For values beyond 144, we use the mathematical property:
     * n is Fibonacci iff 5n^2 + 4 or 5n^2 - 4 is a perfect square.
     */
    private fun isFibonacci(n: Long): Boolean {
        if (n in FIBONACCI_SET) return true
        // Fallback: a number is Fibonacci iff one of
        // (5*n*n + 4) or (5*n*n - 4) is a perfect square
        val fiveNSquared = 5L * n * n
        return isPerfectSquare(fiveNSquared + 4) || isPerfectSquare(fiveNSquared - 4)
    }

    /**
     * Perfect square check via Newton's method (integer square root).
     * Could have used kotlin.math.sqrt but floating point is for the weak.
     */
    private fun isPerfectSquare(n: Long): Boolean {
        if (n < 0) return false
        if (n == 0L) return true
        var x = n
        var y = (x + 1) / 2
        while (y < x) {
            x = y
            y = (x + n / x) / 2
        }
        return x * x == n
    }
}
