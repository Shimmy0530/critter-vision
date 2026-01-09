package com.baltito.crittervision

import android.graphics.ColorMatrix
import kotlin.math.exp
import kotlin.math.pow

/**
 * Scientifically-based animal vision simulation filters
 * Based on peer-reviewed research on cone photoreceptor sensitivity
 * and color space transformations
 */
object VisionColorFilter {

    enum class FilterType {
        ORIGINAL, DOG, CAT, BIRD, RED_ONLY_TEST,
        DOG_ADVANCED, CAT_ADVANCED, BIRD_ADVANCED  // Advanced spectral simulation modes
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
            FilterType.DOG_ADVANCED -> getAdvancedDogMatrix()
            FilterType.CAT_ADVANCED -> getAdvancedCatMatrix()
            FilterType.BIRD_ADVANCED -> getAdvancedBirdMatrix()
            FilterType.RED_ONLY_TEST -> getRedOnlyTestMatrix()
            FilterType.ORIGINAL -> getHumanVisionMatrix()
        }
    }

    /**
     * Advanced dog vision matrix using spectral response calculations
     * More sophisticated dichromatic simulation based on actual cone sensitivity curves
     */
    private val advancedDogMatrixArray: FloatArray by lazy {
        val matrix = FloatArray(20)

        // Dog cone spectral sensitivity peaks
        val sConePeak = 430f  // Short wavelength (blue-violet)
        val lConePeak = 555f  // Long wavelength (yellow-green)

        // RGB wavelength approximations
        val redWL = 650f
        val greenWL = 530f
        val blueWL = 430f

        // Calculate spectral responses for each RGB component
        matrix[0] = calculateConeResponse(redWL, lConePeak) * 0.8f    // Red → L-cone response
        matrix[1] = calculateConeResponse(greenWL, lConePeak) * 0.9f  // Green → L-cone response
        matrix[2] = calculateConeResponse(blueWL, sConePeak) * 0.1f   // Blue → S-cone influence on red
        matrix[3] = 0f  // No alpha influence on red
        matrix[4] = 0f  // No offset

        matrix[5] = calculateConeResponse(redWL, lConePeak) * 0.7f    // Red → L-cone for green channel
        matrix[6] = calculateConeResponse(greenWL, lConePeak) * 0.8f  // Green → L-cone response
        matrix[7] = calculateConeResponse(blueWL, sConePeak) * 0.05f  // Blue → slight S-cone influence
        matrix[8] = 0f  // No alpha influence on green
        matrix[9] = 0f  // No offset

        matrix[10] = 0f // Red has no direct influence on blue channel
        matrix[11] = calculateConeResponse(greenWL, sConePeak) * 0.2f // Green → S-cone response
        matrix[12] = calculateConeResponse(blueWL, sConePeak) * 0.9f  // Blue → S-cone response
        matrix[13] = 0f // No alpha influence on blue
        matrix[14] = 0f // No offset

        matrix[15] = 0f // Alpha channel
        matrix[16] = 0f
        matrix[17] = 0f
        matrix[18] = 1f // Preserve alpha
        matrix[19] = 0f // No offset

        matrix
    }

    fun getAdvancedDogMatrix(): ColorMatrix {
        // Return a new ColorMatrix instance to avoid shared mutable state,
        // but use the cached calculation for the array content.
        return ColorMatrix(advancedDogMatrixArray)
    }

    /**
     * Advanced cat vision matrix with enhanced spectral accuracy
     */
    private val advancedCatMatrixArray: FloatArray by lazy {
        val matrix = FloatArray(20)

        // Cat cone spectral sensitivity peaks
        val sConePeak = 450f  // Short wavelength (blue)
        val mConePeak = 500f  // Possible middle wavelength
        val lConePeak = 555f  // Long wavelength (green)

        // RGB wavelength approximations
        val redWL = 650f
        val greenWL = 530f
        val blueWL = 450f

        // Calculate more nuanced spectral responses
        matrix[0] = calculateConeResponse(redWL, lConePeak) * 0.6f    // Red → L-cone
        matrix[1] = calculateConeResponse(greenWL, lConePeak) * 0.7f +
                calculateConeResponse(greenWL, mConePeak) * 0.2f   // Green → L+M cones
        matrix[2] = calculateConeResponse(blueWL, sConePeak) * 0.05f  // Blue → slight influence
        matrix[3] = 0f
        matrix[4] = 0f

        matrix[5] = calculateConeResponse(redWL, lConePeak) * 0.5f    // Red → L-cone for green
        matrix[6] = calculateConeResponse(greenWL, lConePeak) * 0.6f +
                calculateConeResponse(greenWL, mConePeak) * 0.3f   // Green → L+M cones
        matrix[7] = calculateConeResponse(blueWL, sConePeak) * 0.1f   // Blue → S-cone influence
        matrix[8] = 0f
        matrix[9] = 0f

        matrix[10] = 0f // Red minimal influence on blue
        matrix[11] = calculateConeResponse(greenWL, sConePeak) * 0.15f // Green → S-cone
        matrix[12] = calculateConeResponse(blueWL, sConePeak) * 0.85f  // Blue → S-cone
        matrix[13] = 0f
        matrix[14] = 0f

        matrix[15] = 0f // Alpha channel
        matrix[16] = 0f
        matrix[17] = 0f
        matrix[18] = 1f
        matrix[19] = 0f

        matrix
    }

    fun getAdvancedCatMatrix(): ColorMatrix {
        return ColorMatrix(advancedCatMatrixArray)
    }

    /**
     * Advanced bird vision matrix simulating tetrachromatic perception
     */
    private val advancedBirdMatrixArray: FloatArray by lazy {
        val matrix = FloatArray(20)

        // Bird cone spectral sensitivity peaks
        val uvConePeak = 370f   // UV/Violet
        val sConePeak = 450f    // Short wavelength (blue)
        val mConePeak = 535f    // Medium wavelength (green)
        val lConePeak = 565f    // Long wavelength (red)

        // RGB wavelength approximations
        val redWL = 650f
        val greenWL = 530f
        val blueWL = 450f

        // Enhanced color discrimination with UV simulation
        matrix[0] = calculateConeResponse(redWL, lConePeak) * 1.3f +
                calculateConeResponse(redWL, uvConePeak) * 0.1f   // Red enhanced with UV contribution
        matrix[1] = calculateConeResponse(greenWL, mConePeak) * 0.1f  // Cross-channel enhancement
        matrix[2] = calculateConeResponse(blueWL, uvConePeak) * 0.05f // UV contribution to red
        matrix[3] = 0f
        matrix[4] = 0f

        matrix[5] = calculateConeResponse(redWL, lConePeak) * -0.1f   // Opponent processing
        matrix[6] = calculateConeResponse(greenWL, mConePeak) * 1.4f +
                calculateConeResponse(greenWL, uvConePeak) * 0.1f  // Green enhanced with UV
        matrix[7] = calculateConeResponse(blueWL, sConePeak) * -0.05f // Opponent processing
        matrix[8] = 0f
        matrix[9] = 0f

        matrix[10] = calculateConeResponse(redWL, uvConePeak) * 0.05f // UV simulation
        matrix[11] = calculateConeResponse(greenWL, sConePeak) * -0.1f // Opponent processing
        matrix[12] = calculateConeResponse(blueWL, sConePeak) * 1.5f +
                calculateConeResponse(blueWL, uvConePeak) * 0.2f  // Blue enhanced with strong UV
        matrix[13] = 0f
        matrix[14] = 0f

        matrix[15] = 0f // Alpha channel
        matrix[16] = 0f
        matrix[17] = 0f
        matrix[18] = 1f
        matrix[19] = 0f

        matrix
    }

    fun getAdvancedBirdMatrix(): ColorMatrix {
        return ColorMatrix(advancedBirdMatrixArray)
    }
    
    /**
     * Calculate cone spectral response using Gaussian approximation
     * @param wavelength Target wavelength in nanometers
     * @param conePeak Peak sensitivity wavelength for the cone type
     * @return Normalized response value (0.0 to 1.0)
     */
    private fun calculateConeResponse(wavelength: Float, conePeak: Float): Float {
        val sigma = 40f // Standard deviation for cone sensitivity curve (nm)
        val response = exp(-0.5 * ((wavelength - conePeak) / sigma).pow(2f))
        return response.toFloat().coerceIn(0f, 1f)
    }
    
    /**
     * Get scientific description of each vision type based on peer-reviewed research
     */
    fun getVisionDescription(type: FilterType): String {
        return when (type) {
            FilterType.DOG -> "Dichromatic vision (430nm, 555nm peaks) - Red-green confusion similar to deuteranopia, sees blue-yellow spectrum"
            FilterType.CAT -> "Limited dichromatic vision (450nm, 555nm peaks, possibly 500nm) - Protanopia-like, primarily blues and yellows"
            FilterType.BIRD -> "Tetrachromatic vision (370nm UV, 450nm blue, 535nm green, 565nm red) - Enhanced color discrimination with UV perception"
            FilterType.DOG_ADVANCED -> "Advanced dichromatic simulation - Spectral response calculation based on cone sensitivity curves (430nm, 555nm)"
            FilterType.CAT_ADVANCED -> "Advanced limited dichromatic - Enhanced spectral accuracy with possible triple-cone simulation (450nm, 500nm, 555nm)"
            FilterType.BIRD_ADVANCED -> "Advanced tetrachromatic - Full spectral simulation with UV contribution and opponent processing (370nm, 450nm, 535nm, 565nm)"
            FilterType.RED_ONLY_TEST -> "TEST: Red channel only - Green and blue converted to grayscale for filter validation"
            FilterType.ORIGINAL -> "Human trichromatic vision (420nm, 535nm, 565nm peaks) - Full RGB spectrum without UV"
        }
    }
}