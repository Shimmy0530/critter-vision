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

    // Helper: assert sum of row i (3 elements starting at i*3) equals expected
    private fun assertRowSum(matrix: FloatArray, row: Int, expected: Float, delta: Float) {
        val start = row * 3
        val sum = matrix[start] + matrix[start + 1] + matrix[start + 2]
        assertEquals("Row $row sum", expected, sum, delta)
    }
}
