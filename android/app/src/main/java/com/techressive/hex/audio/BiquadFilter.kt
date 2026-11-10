package com.techressive.hex.audio

import kotlin.math.*

enum class FilterType(val id: Int) {
    PEAKING(0),
    LOW_SHELF(1),
    HIGH_SHELF(2),
    LOW_PASS(3),
    HIGH_PASS(4),
    NOTCH(5)
}

data class EQBandConfig(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val type: FilterType,
    var frequency: Double,
    var gainDb: Double,
    var q: Double = 1.0,
    var isEnabled: Boolean = true
) {
    val label: String get() = name
}

object BiquadFilter {
    private var nativeLoaded = false

    init {
        try {
            System.loadLibrary("hex_dsp")
            nativeLoaded = true
        } catch (e: Throwable) {
            nativeLoaded = false
        }
    }

    private external fun calculateMagnitudeCurveNative(
        types: IntArray,
        freqs: DoubleArray,
        gains: DoubleArray,
        qs: DoubleArray,
        enables: BooleanArray,
        count: Int,
        evalFreqs: DoubleArray,
        evalCount: Int,
        sampleRate: Double
    ): DoubleArray

    fun calculateMagnitudeCurve(
        bands: List<EQBandConfig>,
        evalFreqs: DoubleArray,
        sampleRate: Double = 48000.0
    ): DoubleArray {
        if (nativeLoaded) {
            try {
                val types = bands.map { it.type.id }.toIntArray()
                val freqs = bands.map { it.frequency }.toDoubleArray()
                val gains = bands.map { it.gainDb }.toDoubleArray()
                val qs = bands.map { it.q }.toDoubleArray()
                val enables = bands.map { it.isEnabled }.toBooleanArray()
                return calculateMagnitudeCurveNative(
                    types, freqs, gains, qs, enables, bands.size,
                    evalFreqs, evalFreqs.size, sampleRate
                )
            } catch (e: Throwable) {
                // Fallback to Kotlin implementation
            }
        }

        // Pure Kotlin RBJ implementation fallback
        val results = DoubleArray(evalFreqs.size)
        val activeBands = bands.filter { it.isEnabled }

        for (i in evalFreqs.indices) {
            val f = evalFreqs[i]
            var totalDb = 0.0
            val w = 2.0 * Math.PI * f / sampleRate
            val cosW = cos(w)
            val cos2W = cos(2.0 * w)
            val sinW = sin(w)
            val sin2W = sin(2.0 * w)

            for (b in activeBands) {
                val coeffs = calculateRBJCoefficients(b, sampleRate)
                val numReal = coeffs.b0 + coeffs.b1 * cosW + coeffs.b2 * cos2W
                val numImag = -(coeffs.b1 * sinW + coeffs.b2 * sin2W)

                val denReal = 1.0 + coeffs.a1 * cosW + coeffs.a2 * cos2W
                val denImag = -(coeffs.a1 * sinW + coeffs.a2 * sin2W)

                val numMagSq = numReal * numReal + numImag * numImag
                val denMagSq = denReal * denReal + denImag * denImag

                if (denMagSq > 1e-12) {
                    val hMagSq = numMagSq / denMagSq
                    totalDb += 10.0 * log10(max(1e-12, hMagSq))
                }
            }
            results[i] = totalDb
        }
        return results
    }

    private data class Coefficients(
        val b0: Double, val b1: Double, val b2: Double,
        val a1: Double, val a2: Double
    )

    private fun calculateRBJCoefficients(config: EQBandConfig, sampleRate: Double): Coefficients {
        val w0 = 2.0 * Math.PI * config.frequency / sampleRate
        val cosW0 = cos(w0)
        val sinW0 = sin(w0)
        val alpha = sinW0 / (2.0 * max(0.05, config.q))
        val a = 10.0.pow(config.gainDb / 40.0)

        var b0 = 1.0
        var b1 = 0.0
        var b2 = 0.0
        var a0 = 1.0
        var a1 = 0.0
        var a2 = 0.0

        when (config.type) {
            FilterType.PEAKING -> {
                b0 = 1.0 + alpha * a
                b1 = -2.0 * cosW0
                b2 = 1.0 - alpha * a
                a0 = 1.0 + alpha / a
                a1 = -2.0 * cosW0
                a2 = 1.0 - alpha / a
            }
            FilterType.LOW_SHELF -> {
                val sqrtA = sqrt(a)
                b0 = a * ((a + 1.0) - (a - 1.0) * cosW0 + 2.0 * sqrtA * alpha)
                b1 = 2.0 * a * ((a - 1.0) - (a + 1.0) * cosW0)
                b2 = a * ((a + 1.0) - (a - 1.0) * cosW0 - 2.0 * sqrtA * alpha)
                a0 = (a + 1.0) + (a - 1.0) * cosW0 + 2.0 * sqrtA * alpha
                a1 = -2.0 * ((a - 1.0) + (a + 1.0) * cosW0)
                a2 = (a + 1.0) + (a - 1.0) * cosW0 - 2.0 * sqrtA * alpha
            }
            FilterType.HIGH_SHELF -> {
                val sqrtA = sqrt(a)
                b0 = a * ((a + 1.0) + (a - 1.0) * cosW0 + 2.0 * sqrtA * alpha)
                b1 = -2.0 * a * ((a - 1.0) + (a + 1.0) * cosW0)
                b2 = a * ((a + 1.0) + (a - 1.0) * cosW0 - 2.0 * sqrtA * alpha)
                a0 = (a + 1.0) - (a - 1.0) * cosW0 + 2.0 * sqrtA * alpha
                a1 = 2.0 * ((a - 1.0) - (a + 1.0) * cosW0)
                a2 = (a + 1.0) - (a - 1.0) * cosW0 - 2.0 * sqrtA * alpha
            }
            FilterType.LOW_PASS -> {
                b0 = (1.0 - cosW0) / 2.0
                b1 = 1.0 - cosW0
                b2 = (1.0 - cosW0) / 2.0
                a0 = 1.0 + alpha
                a1 = -2.0 * cosW0
                a2 = 1.0 - alpha
            }
            FilterType.HIGH_PASS -> {
                b0 = (1.0 + cosW0) / 2.0
                b1 = -(1.0 + cosW0)
                b2 = (1.0 + cosW0) / 2.0
                a0 = 1.0 + alpha
                a1 = -2.0 * cosW0
                a2 = 1.0 - alpha
            }
            FilterType.NOTCH -> {
                b0 = 1.0
                b1 = -2.0 * cosW0
                b2 = 1.0
                a0 = 1.0 + alpha
                a1 = -2.0 * cosW0
                a2 = 1.0 - alpha
            }
        }

        return Coefficients(
            b0 = b0 / a0,
            b1 = b1 / a0,
            b2 = b2 / a0,
            a1 = a1 / a0,
            a2 = a2 / a0
        )
    }
}
