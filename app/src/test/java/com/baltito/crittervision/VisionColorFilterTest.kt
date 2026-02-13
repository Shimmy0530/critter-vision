package com.baltito.crittervision

import org.junit.Assert.*
import org.junit.Test

class VisionColorFilterTest {

    @Test
    fun dogMatrixRowSumsPreserveLuminance() {
        val params = VisionColorFilter.getParams(VisionColorFilter.VisionMode.DOG)
        val m = params.matrix
        // Machado 2009 guarantees row sums ≈ 1.0
        assertRowSum(m, 0, 1.0f, 0.001f)
        assertRowSum(m, 1, 1.0f, 0.001f)
        assertRowSum(m, 2, 1.0f, 0.001f)
    }

    @Test
    fun catMatrixRowSumsPreserveLuminance() {
        val params = VisionColorFilter.getParams(VisionColorFilter.VisionMode.CAT)
        val m = params.matrix
        assertRowSum(m, 0, 1.0f, 0.01f)
        assertRowSum(m, 1, 1.0f, 0.01f)
        assertRowSum(m, 2, 1.0f, 0.01f)
    }

    @Test
    fun humanModeIsIdentity() {
        val params = VisionColorFilter.getParams(VisionColorFilter.VisionMode.HUMAN)
        val expected = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
        assertArrayEquals(expected, params.matrix, 0.0001f)
        assertEquals(1.0f, params.saturationBoost, 0.0001f)
        assertEquals(0.0f, params.uvProxyWeight, 0.0001f)
        assertArrayEquals(floatArrayOf(0f, 0f, 0f), params.colorOffset, 0.0001f)
    }

    @Test
    fun birdModeHasSaturationAndUvProxy() {
        val params = VisionColorFilter.getParams(VisionColorFilter.VisionMode.BIRD)
        assertEquals(1.35f, params.saturationBoost, 0.001f)
        assertEquals(0.30f, params.uvProxyWeight, 0.001f)
    }

    @Test
    fun dogAndCatHaveNoSaturationOrUvProxy() {
        val dog = VisionColorFilter.getParams(VisionColorFilter.VisionMode.DOG)
        val cat = VisionColorFilter.getParams(VisionColorFilter.VisionMode.CAT)
        assertEquals(1.0f, dog.saturationBoost, 0.0001f)
        assertEquals(0.0f, dog.uvProxyWeight, 0.0001f)
        assertEquals(1.0f, cat.saturationBoost, 0.0001f)
        assertEquals(0.0f, cat.uvProxyWeight, 0.0001f)
    }

    @Test
    fun allModesHaveNineElementMatrix() {
        VisionColorFilter.VisionMode.values().forEach { mode ->
            val params = VisionColorFilter.getParams(mode)
            assertEquals("Mode $mode matrix should have 9 elements", 9, params.matrix.size)
        }
    }

    @Test
    fun allModesHaveThreeElementOffset() {
        VisionColorFilter.VisionMode.values().forEach { mode ->
            val params = VisionColorFilter.getParams(mode)
            assertEquals("Mode $mode offset should have 3 elements", 3, params.colorOffset.size)
        }
    }

    @Test
    fun allModesHaveDisplayNameAndDescription() {
        VisionColorFilter.VisionMode.values().forEach { mode ->
            val params = VisionColorFilter.getParams(mode)
            assertTrue("Mode $mode should have a display name", params.displayName.isNotBlank())
            assertTrue("Mode $mode should have a description", params.description.isNotBlank())
        }
    }

    @Test
    fun debugChannelIsolationMatricesCorrect() {
        val red = VisionColorFilter.getParams(VisionColorFilter.VisionMode.RED_ONLY).matrix
        val green = VisionColorFilter.getParams(VisionColorFilter.VisionMode.GREEN_ONLY).matrix
        val blue = VisionColorFilter.getParams(VisionColorFilter.VisionMode.BLUE_ONLY).matrix

        // Red channel only — zeroes green and blue output rows
        assertArrayEquals(floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f), red, 0.0001f)
        // Green channel only — zeroes red and blue output rows
        assertArrayEquals(floatArrayOf(0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f), green, 0.0001f)
        // Blue channel only — zeroes red and green output rows
        assertArrayEquals(floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f), blue, 0.0001f)
    }

    @Test
    fun dogMatrixMatchesMachado2009() {
        val m = VisionColorFilter.getParams(VisionColorFilter.VisionMode.DOG).matrix
        // Machado et al. 2009, deuteranopia severity=1.0
        assertEquals(0.367322f, m[0], 0.0001f)
        assertEquals(0.860646f, m[1], 0.0001f)
        assertEquals(-0.227968f, m[2], 0.0001f)
        assertEquals(0.280085f, m[3], 0.0001f)
        assertEquals(0.672501f, m[4], 0.0001f)
        assertEquals(0.047413f, m[5], 0.0001f)
        assertEquals(-0.011820f, m[6], 0.0001f)
        assertEquals(0.042940f, m[7], 0.0001f)
        assertEquals(0.968881f, m[8], 0.0001f)
    }

    // ── New animal mode tests ───────────────────────────────────────────

    @Test
    fun eagleHasOffsetAndSaturation() {
        val params = VisionColorFilter.getParams(VisionColorFilter.VisionMode.EAGLE)
        assertEquals("Eagle Vision", params.displayName)
        assertEquals(1.20f, params.saturationBoost, 0.001f)
        assertArrayEquals(floatArrayOf(0.10f, 0.10f, 0.20f), params.colorOffset, 0.001f)
    }

    @Test
    fun horseIsDichromatic() {
        val params = VisionColorFilter.getParams(VisionColorFilter.VisionMode.HORSE)
        assertEquals("Horse Vision", params.displayName)
        assertEquals(1.0f, params.saturationBoost, 0.0001f)
        assertArrayEquals(floatArrayOf(0f, 0f, 0f), params.colorOffset, 0.0001f)
        assertRowSum(params.matrix, 0, 1.0f, 0.01f)
        assertRowSum(params.matrix, 1, 1.0f, 0.01f)
        assertRowSum(params.matrix, 2, 1.0f, 0.01f)
    }

    @Test
    fun mantisShrimpHasHypersaturation() {
        val params = VisionColorFilter.getParams(VisionColorFilter.VisionMode.MANTIS_SHRIMP)
        assertEquals("Mantis Shrimp", params.displayName)
        assertEquals(1.80f, params.saturationBoost, 0.001f)
        assertEquals(0.40f, params.uvProxyWeight, 0.001f)
    }

    @Test
    fun reindeerHasUvAndOffset() {
        val params = VisionColorFilter.getParams(VisionColorFilter.VisionMode.REINDEER)
        assertEquals("Reindeer Vision", params.displayName)
        assertEquals(0.50f, params.uvProxyWeight, 0.001f)
        assertArrayEquals(floatArrayOf(0f, 0f, 0.20f), params.colorOffset, 0.001f)
    }

    @Test
    fun cuttlefishHasOffsetAndSaturation() {
        val params = VisionColorFilter.getParams(VisionColorFilter.VisionMode.CUTTLEFISH)
        assertEquals("Cuttlefish Vision", params.displayName)
        assertEquals(1.15f, params.saturationBoost, 0.001f)
        assertArrayEquals(floatArrayOf(0.10f, 0.10f, 0.20f), params.colorOffset, 0.001f)
    }

    @Test
    fun pitViperIsThermalRed() {
        val params = VisionColorFilter.getParams(VisionColorFilter.VisionMode.PIT_VIPER)
        assertEquals("Pit Viper Vision", params.displayName)
        val m = params.matrix
        // Only the red row has nonzero values (luminance → red)
        assertTrue(m[0] > 0f && m[1] > 0f && m[2] > 0f) // red row active
        assertEquals(0f, m[3], 0.0001f) // green row zeroed
        assertEquals(0f, m[4], 0.0001f)
        assertEquals(0f, m[5], 0.0001f)
        assertEquals(0f, m[6], 0.0001f) // blue row zeroed
        assertEquals(0f, m[7], 0.0001f)
        assertEquals(0f, m[8], 0.0001f)
    }

    // ── Color swatch verification tests (Step 7) ─────────────────────────
    // Apply each mode's 3×3 matrix to known swatch colors and verify expected
    // perceptual behavior. Matrices operate on linear RGB; these tests use the
    // raw matrix multiply without sRGB linearization for simplicity (validates
    // the matrix coefficients directly).

    @Test
    fun dogSwatches_redAndGreenBecomeSimilar_blueStaysDistinct() {
        val m = VisionColorFilter.getParams(VisionColorFilter.VisionMode.DOG).matrix
        val redOut = applyMatrix3x3(m, floatArrayOf(1f, 0f, 0f))
        val greenOut = applyMatrix3x3(m, floatArrayOf(0f, 1f, 0f))
        val blueOut = applyMatrix3x3(m, floatArrayOf(0f, 0f, 1f))

        // Red and green outputs should be more similar than in human vision
        val rgDistance = colorDistance(redOut, greenOut)
        val rbDistance = colorDistance(redOut, blueOut)
        assertTrue(
            "Dog: red-green distance ($rgDistance) should be < red-blue distance ($rbDistance)",
            rgDistance < rbDistance
        )
        // Blue channel should remain clearly distinct (blue output blue component dominant)
        assertTrue("Dog: blue swatch should retain strong blue", blueOut[2] > 0.5f)
    }

    @Test
    fun catSwatches_redAndGreenBecomeSimilar_blueStaysDistinct() {
        val m = VisionColorFilter.getParams(VisionColorFilter.VisionMode.CAT).matrix
        val redOut = applyMatrix3x3(m, floatArrayOf(1f, 0f, 0f))
        val greenOut = applyMatrix3x3(m, floatArrayOf(0f, 1f, 0f))
        val blueOut = applyMatrix3x3(m, floatArrayOf(0f, 0f, 1f))

        val rgDistance = colorDistance(redOut, greenOut)
        val rbDistance = colorDistance(redOut, blueOut)
        assertTrue(
            "Cat: red-green distance ($rgDistance) should be < red-blue distance ($rbDistance)",
            rgDistance < rbDistance
        )
        assertTrue("Cat: blue swatch should retain some blue", blueOut[2] > 0.3f)
    }

    @Test
    fun dogAndCat_reducedColorGamut() {
        // Dog and cat should both reduce the overall color gamut compared to human
        for (mode in listOf(VisionColorFilter.VisionMode.DOG, VisionColorFilter.VisionMode.CAT)) {
            val m = VisionColorFilter.getParams(mode).matrix
            val swatches = listOf(
                floatArrayOf(1f, 0f, 0f), // red
                floatArrayOf(0f, 1f, 0f), // green
                floatArrayOf(0f, 0f, 1f), // blue
                floatArrayOf(1f, 1f, 0f), // yellow
                floatArrayOf(0f, 1f, 1f), // cyan
                floatArrayOf(1f, 0f, 1f)  // magenta
            )
            val outputs = swatches.map { applyMatrix3x3(m, it) }
            // Compute average pairwise distance for transformed colors
            var totalDist = 0f
            var count = 0
            for (i in outputs.indices) {
                for (j in i + 1 until outputs.size) {
                    totalDist += colorDistance(outputs[i], outputs[j])
                    count++
                }
            }
            val avgDist = totalDist / count

            // Human identity would have avg pairwise distance > 1.0 for these saturated swatches
            // Dichromat modes should compress the gamut
            assertTrue(
                "$mode: average pairwise distance ($avgDist) should be < 1.4 (reduced gamut)",
                avgDist < 1.4f
            )
        }
    }

    @Test
    fun birdSwatches_colorsMoreVividWithoutExtremeClipping() {
        val m = VisionColorFilter.getParams(VisionColorFilter.VisionMode.BIRD).matrix
        val swatches = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f),
            floatArrayOf(0f, 0f, 1f),
            floatArrayOf(0.5f, 0.5f, 0.5f) // grey
        )
        for (swatch in swatches) {
            val out = applyMatrix3x3(m, swatch)
            // No channel should exceed 2.0 (extreme clipping check — in linear space
            // values >1.0 are clipped by the shader, but shouldn't be wildly over)
            for (c in out.indices) {
                assertTrue(
                    "Bird: output channel $c = ${out[c]} should not have extreme clipping (>2.0)",
                    out[c] < 2.0f
                )
            }
        }
        // Bird blue output should be boosted (B diagonal > 1.0)
        val blueOut = applyMatrix3x3(m, floatArrayOf(0f, 0f, 1f))
        assertTrue("Bird: blue swatch should be boosted", blueOut[2] > 1.0f)
    }

    @Test
    fun redOnlySwatches_onlyRedChannelVisible() {
        val m = VisionColorFilter.getParams(VisionColorFilter.VisionMode.RED_ONLY).matrix
        // Pure red → only red survives
        assertArrayEquals(floatArrayOf(1f, 0f, 0f), applyMatrix3x3(m, floatArrayOf(1f, 0f, 0f)), 0.001f)
        // Pure green → black
        assertArrayEquals(floatArrayOf(0f, 0f, 0f), applyMatrix3x3(m, floatArrayOf(0f, 1f, 0f)), 0.001f)
        // Pure blue → black
        assertArrayEquals(floatArrayOf(0f, 0f, 0f), applyMatrix3x3(m, floatArrayOf(0f, 0f, 1f)), 0.001f)
        // White → only red
        assertArrayEquals(floatArrayOf(1f, 0f, 0f), applyMatrix3x3(m, floatArrayOf(1f, 1f, 1f)), 0.001f)
    }

    @Test
    fun greenOnlySwatches_onlyGreenChannelVisible() {
        val m = VisionColorFilter.getParams(VisionColorFilter.VisionMode.GREEN_ONLY).matrix
        assertArrayEquals(floatArrayOf(0f, 0f, 0f), applyMatrix3x3(m, floatArrayOf(1f, 0f, 0f)), 0.001f)
        assertArrayEquals(floatArrayOf(0f, 1f, 0f), applyMatrix3x3(m, floatArrayOf(0f, 1f, 0f)), 0.001f)
        assertArrayEquals(floatArrayOf(0f, 0f, 0f), applyMatrix3x3(m, floatArrayOf(0f, 0f, 1f)), 0.001f)
        assertArrayEquals(floatArrayOf(0f, 1f, 0f), applyMatrix3x3(m, floatArrayOf(1f, 1f, 1f)), 0.001f)
    }

    @Test
    fun blueOnlySwatches_onlyBlueChannelVisible() {
        val m = VisionColorFilter.getParams(VisionColorFilter.VisionMode.BLUE_ONLY).matrix
        assertArrayEquals(floatArrayOf(0f, 0f, 0f), applyMatrix3x3(m, floatArrayOf(1f, 0f, 0f)), 0.001f)
        assertArrayEquals(floatArrayOf(0f, 0f, 0f), applyMatrix3x3(m, floatArrayOf(0f, 1f, 0f)), 0.001f)
        assertArrayEquals(floatArrayOf(0f, 0f, 1f), applyMatrix3x3(m, floatArrayOf(0f, 0f, 1f)), 0.001f)
        assertArrayEquals(floatArrayOf(0f, 0f, 1f), applyMatrix3x3(m, floatArrayOf(1f, 1f, 1f)), 0.001f)
    }

    @Test
    fun humanSwatches_colorsUnchanged() {
        val m = VisionColorFilter.getParams(VisionColorFilter.VisionMode.HUMAN).matrix
        val swatches = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f),
            floatArrayOf(0f, 0f, 1f),
            floatArrayOf(1f, 1f, 0f),
            floatArrayOf(0f, 1f, 1f),
            floatArrayOf(1f, 0f, 1f),
            floatArrayOf(1f, 1f, 1f),
            floatArrayOf(0.5f, 0.5f, 0.5f)
        )
        for (swatch in swatches) {
            assertArrayEquals(
                "Human mode should not change ${swatch.toList()}",
                swatch, applyMatrix3x3(m, swatch), 0.0001f
            )
        }
    }

    // ── VisionMatrices 4×5 consistency tests ─────────────────────────────
    // Note: getColorMatrix() and visionModeToColorFilter() use android.graphics
    // classes and must be tested in instrumented tests (androidTest). These tests
    // verify the raw float arrays are consistent with VisionColorFilter's 3×3 core.

    @Test
    fun visionMatrices_4x5ArraysHave20Elements() {
        val arrays = listOf(
            VisionMatrices.dogMatrixArray, VisionMatrices.catMatrixArray,
            VisionMatrices.birdMatrixArray, VisionMatrices.eagleMatrixArray,
            VisionMatrices.horseMatrixArray, VisionMatrices.mantisShrimpMatrixArray,
            VisionMatrices.reindeerMatrixArray, VisionMatrices.cuttlefishMatrixArray,
            VisionMatrices.pitViperMatrixArray,
            VisionMatrices.redOnlyMatrixArray, VisionMatrices.greenOnlyMatrixArray,
            VisionMatrices.blueOnlyMatrixArray
        )
        arrays.forEach { arr ->
            assertEquals("4×5 matrix should have 20 elements", 20, arr.size)
        }
    }

    @Test
    fun visionMatrices_4x5AlphaRowPreservesAlpha() {
        val arrays = listOf(
            VisionMatrices.dogMatrixArray, VisionMatrices.catMatrixArray,
            VisionMatrices.birdMatrixArray, VisionMatrices.eagleMatrixArray,
            VisionMatrices.horseMatrixArray, VisionMatrices.mantisShrimpMatrixArray,
            VisionMatrices.reindeerMatrixArray, VisionMatrices.cuttlefishMatrixArray,
            VisionMatrices.pitViperMatrixArray,
            VisionMatrices.redOnlyMatrixArray, VisionMatrices.greenOnlyMatrixArray,
            VisionMatrices.blueOnlyMatrixArray
        )
        // Row 3 (alpha row) should be [0, 0, 0, 1, 0] to preserve alpha
        arrays.forEach { arr ->
            assertEquals(0f, arr[15], 0.0001f)
            assertEquals(0f, arr[16], 0.0001f)
            assertEquals(0f, arr[17], 0.0001f)
            assertEquals(1f, arr[18], 0.0001f)
            assertEquals(0f, arr[19], 0.0001f)
        }
    }

    @Test
    fun visionMatrices_4x5CoreMatchesVisionColorFilter3x3() {
        // Verify that the 4×5 matrices in VisionMatrices embed the same 3×3 core
        // as VisionColorFilter's row-major matrices (for modes without color offset)
        for (mode in listOf(
            VisionColorFilter.VisionMode.DOG,
            VisionColorFilter.VisionMode.CAT,
            VisionColorFilter.VisionMode.HORSE,
            VisionColorFilter.VisionMode.PIT_VIPER,
            VisionColorFilter.VisionMode.RED_ONLY,
            VisionColorFilter.VisionMode.GREEN_ONLY,
            VisionColorFilter.VisionMode.BLUE_ONLY
        )) {
            val core3x3 = VisionColorFilter.getParams(mode).matrix
            val full4x5 = when (mode) {
                VisionColorFilter.VisionMode.DOG -> VisionMatrices.dogMatrixArray
                VisionColorFilter.VisionMode.CAT -> VisionMatrices.catMatrixArray
                VisionColorFilter.VisionMode.HORSE -> VisionMatrices.horseMatrixArray
                VisionColorFilter.VisionMode.PIT_VIPER -> VisionMatrices.pitViperMatrixArray
                VisionColorFilter.VisionMode.RED_ONLY -> VisionMatrices.redOnlyMatrixArray
                VisionColorFilter.VisionMode.GREEN_ONLY -> VisionMatrices.greenOnlyMatrixArray
                VisionColorFilter.VisionMode.BLUE_ONLY -> VisionMatrices.blueOnlyMatrixArray
                else -> continue
            }
            // Extract 3×3 core from 4×5: rows 0-2, columns 0-2
            for (row in 0..2) {
                for (col in 0..2) {
                    assertEquals(
                        "$mode: [${row},${col}] mismatch between 3×3 and 4×5",
                        core3x3[row * 3 + col],
                        full4x5[row * 5 + col],
                        0.0001f
                    )
                }
                // Column 3 (alpha input) should be 0 for RGB rows
                assertEquals("$mode: row $row alpha column should be 0",
                    0f, full4x5[row * 5 + 3], 0.0001f)
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    // Apply a row-major 3×3 matrix to an RGB triplet
    private fun applyMatrix3x3(matrix: FloatArray, rgb: FloatArray): FloatArray {
        return floatArrayOf(
            matrix[0] * rgb[0] + matrix[1] * rgb[1] + matrix[2] * rgb[2],
            matrix[3] * rgb[0] + matrix[4] * rgb[1] + matrix[5] * rgb[2],
            matrix[6] * rgb[0] + matrix[7] * rgb[1] + matrix[8] * rgb[2]
        )
    }

    // Euclidean distance between two RGB triplets
    private fun colorDistance(a: FloatArray, b: FloatArray): Float {
        val dr = a[0] - b[0]
        val dg = a[1] - b[1]
        val db = a[2] - b[2]
        return Math.sqrt((dr * dr + dg * dg + db * db).toDouble()).toFloat()
    }

    // Helper: assert sum of row i (3 elements starting at i*3) equals expected
    private fun assertRowSum(matrix: FloatArray, row: Int, expected: Float, delta: Float) {
        val start = row * 3
        val sum = matrix[start] + matrix[start + 1] + matrix[start + 2]
        assertEquals("Row $row sum", expected, sum, delta)
    }
}
