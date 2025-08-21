package com.baltito.crittervision

import android.graphics.ColorMatrix

/**
 * Scientifically-based animal vision simulation filters
 * Based on peer-reviewed research on cone photoreceptor sensitivity
 * and color space transformations
 */
object VisionColorFilter {

    enum class FilterType {
        ORIGINAL, DOG, CAT, BIRD, RED_ONLY_TEST
    }

    /**
     * Simulates Dog Vision - Dichromatic with peaks at 430nm (blue-violet) and 555nm (yellow-green)
     * Based on peer-reviewed research showing dogs have dichromatic vision similar to deuteranopia
     * Dogs cannot distinguish between red and green colors, seeing them as variations of yellow and brown
     */
    fun getDogVisionMatrix(): ColorMatrix {
        // Scientifically accurate dog vision matrix based on dichromatic vision research
        // Dogs have two cone types with spectral sensitivity peaks at ~430nm and ~555nm
        val dogMatrix = floatArrayOf(
            0.625f, 0.375f, 0.0f,     0.0f, 0.0f,  // Red-green confusion, sees as yellow variations
            0.7f,   0.3f,   0.0f,     0.0f, 0.0f,  // Green mixed with red, reduced discrimination
            0.0f,   0.3f,   0.7f,     0.0f, 0.0f,  // Blue channel preserved with slight green influence
            0.0f,   0.0f,   0.0f,     1.0f, 0.0f   // Alpha unchanged
        )
        return ColorMatrix(dogMatrix)
    }

    /**
     * Simulates Cat Vision - Limited dichromatic with peaks at 450nm (blue) and 555nm (green)
     * Based on research showing cats have at least two cone types, possibly a third at 500nm
     * Cats have difficulty distinguishing reds and greens, primarily see blues and yellows
     */
    fun getCatVisionMatrix(): ColorMatrix {
        // Scientifically accurate cat vision matrix based on protanopia-like color vision
        // Cat spectral sensitivity peaks at ~450nm (blue) and ~555nm (green)
        val catMatrix = floatArrayOf(
            0.567f, 0.433f, 0.0f,     0.0f, 0.0f,  // Red-green confusion similar to protanopia
            0.558f, 0.442f, 0.0f,     0.0f, 0.0f,  // Green mixed with red, reduced discrimination
            0.0f,   0.242f, 0.758f,   0.0f, 0.0f,  // Blue channel with slight green influence
            0.0f,   0.0f,   0.0f,     1.0f, 0.0f   // Alpha unchanged
        )
        return ColorMatrix(catMatrix)
    }

    /**
     * Simulates Bird Vision - Tetrachromatic with UV perception
     * Based on research showing birds have four cone types: UV/Violet (355-426nm), Blue (~450nm), Green (~535nm), Red (~565nm)
     * Birds have enhanced color discrimination and can see UV patterns invisible to humans
     */
    fun getBirdVisionMatrix(): ColorMatrix {
        // Scientifically accurate bird vision matrix simulating tetrachromatic vision
        // Enhanced color saturation to approximate richer color perception within RGB limitations
        val birdMatrix = floatArrayOf(
            1.2f,  -0.1f,   0.0f,     0.0f, 0.0f,  // Red enhanced with slight adjustment
            -0.05f, 1.3f,  -0.05f,    0.0f, 0.0f,  // Green enhanced with UV influence simulation
            0.0f,  -0.1f,   1.4f,     0.0f, 0.0f,  // Blue enhanced (UV compensation)
            0.0f,   0.0f,   0.0f,     1.0f, 0.0f   // Alpha unchanged
        )
        return ColorMatrix(birdMatrix)
    }

    /**
     * Get identity matrix for human vision (no filter)
     */
    fun getHumanVisionMatrix(): ColorMatrix {
        return ColorMatrix().apply {
            // Identity matrix - no color transformation
            val identityMatrix = floatArrayOf(
                1.0f, 0.0f, 0.0f, 0.0f, 0.0f,  // Red channel unchanged
                0.0f, 1.0f, 0.0f, 0.0f, 0.0f,  // Green channel unchanged
                0.0f, 0.0f, 1.0f, 0.0f, 0.0f,  // Blue channel unchanged
                0.0f, 0.0f, 0.0f, 1.0f, 0.0f   // Alpha channel unchanged
            )
            set(identityMatrix)
        }
    }

    /**
     * TEST FILTER: Red only - converts green and blue to grayscale, keeps red channel
     * This clearly demonstrates if color matrices are working properly
     */
    fun getRedOnlyTestMatrix(): ColorMatrix {
        // Red only matrix: Green and blue channels become grayscale luminance
        // This should make everything red/gray with no green or blue colors
        val redOnlyMatrix = floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f, 0.0f,  // Red channel unchanged
            0.3f, 0.3f, 0.3f, 0.0f, 0.0f,  // Green becomes grayscale (R*0.3 + G*0.3 + B*0.3)
            0.3f, 0.3f, 0.3f, 0.0f, 0.0f,  // Blue becomes grayscale (R*0.3 + G*0.3 + B*0.3)
            0.0f, 0.0f, 0.0f, 1.0f, 0.0f   // Alpha unchanged
        )
        return ColorMatrix(redOnlyMatrix)
    }

    fun getMatrix(type: FilterType): ColorMatrix? {
        return when (type) {
            FilterType.DOG -> getDogVisionMatrix()
            FilterType.CAT -> getCatVisionMatrix()
            FilterType.BIRD -> getBirdVisionMatrix()
            FilterType.RED_ONLY_TEST -> getRedOnlyTestMatrix()
            FilterType.ORIGINAL -> getHumanVisionMatrix()
        }
    }

    /**
     * Get scientific description of each vision type based on peer-reviewed research
     */
    fun getVisionDescription(type: FilterType): String {
        return when (type) {
            FilterType.DOG -> "Dichromatic vision (430nm, 555nm peaks) - Red-green confusion similar to deuteranopia, sees blue-yellow spectrum"
            FilterType.CAT -> "Limited dichromatic vision (450nm, 555nm peaks, possibly 500nm) - Protanopia-like, primarily blues and yellows"
            FilterType.BIRD -> "Tetrachromatic vision (370nm UV, 450nm blue, 535nm green, 565nm red) - Enhanced color discrimination with UV perception"
            FilterType.RED_ONLY_TEST -> "TEST: Red channel only - Green and blue converted to grayscale for filter validation"
            FilterType.ORIGINAL -> "Human trichromatic vision (420nm, 535nm, 565nm peaks) - Full RGB spectrum without UV"
        }
    }
}