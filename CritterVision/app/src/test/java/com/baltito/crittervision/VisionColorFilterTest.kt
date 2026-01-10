package com.baltito.crittervision

import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.math.exp
import kotlin.math.pow

class VisionColorFilterTest {

    /**
     * This test benchmarks the mathematical operations used in VisionColorFilter.
     * Note: We cannot test VisionColorFilter.getMatrix() directly in this unit test environment
     * because it depends on android.graphics.ColorMatrix which is not mocked.
     *
     * However, this test demonstrates the computational cost of the operations we are caching.
     */
    @Test
    fun benchmarkConeResponseCalculation() {
        val iterations = 100000
        val startTime = System.nanoTime()

        // Simulating the math performed in getAdvancedDogMatrix
        // Each call to getAdvancedDogMatrix performs roughly 9 calls to calculateConeResponse
        for (i in 0 until iterations) {
            val sConePeak = 430f
            val lConePeak = 555f
            val redWL = 650f
            val greenWL = 530f
            val blueWL = 430f

            calculateConeResponse(redWL, lConePeak) * 0.8f
            calculateConeResponse(greenWL, lConePeak) * 0.9f
            calculateConeResponse(blueWL, sConePeak) * 0.1f
            calculateConeResponse(redWL, lConePeak) * 0.7f
            calculateConeResponse(greenWL, lConePeak) * 0.8f
            calculateConeResponse(blueWL, sConePeak) * 0.05f
            calculateConeResponse(greenWL, sConePeak) * 0.2f
            calculateConeResponse(blueWL, sConePeak) * 0.9f
        }

        val endTime = System.nanoTime()
        val durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime)
        println("Math Benchmark: $iterations iterations took $durationMs ms")
    }

    private fun calculateConeResponse(wavelength: Float, conePeak: Float): Float {
        val sigma = 40f
        val response = exp(-0.5 * ((wavelength - conePeak) / sigma).pow(2f))
        return response.toFloat().coerceIn(0f, 1f)
    }
}
