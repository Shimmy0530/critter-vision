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

    // Cached matrix instances to avoid reallocation and recalculation
    private val cachedDogVisionMatrix by lazy {
        val dogMatrix = floatArrayOf(
            0.625f, 0.375f, 0.0f,     0.0f, 0.0f,
            0.7f,   0.3f,   0.0f,     0.0f, 0.0f,
            0.0f,   0.3f,   0.7f,     0.0f, 0.0f,
            0.0f,   0.0f,   0.0f,     1.0f, 0.0f
        )
        ColorMatrix(dogMatrix)
    }

    private val cachedCatVisionMatrix by lazy {
        val catMatrix = floatArrayOf(
            0.295f, 0.685f, 0.021f,   0.0f, 0.0f,
            0.009f, 0.617f, 0.374f,   0.0f, 0.0f,
            0.000f, 0.182f, 0.817f,   0.0f, 0.0f,
            0.0f,   0.0f,   0.0f,     1.0f, 0.0f
        )
        ColorMatrix(catMatrix)
    }

    private val cachedBirdVisionMatrix by lazy {
        val birdMatrix = floatArrayOf(
            1.2f,  -0.1f,   0.0f,     0.0f, 0.0f,
            -0.05f, 1.3f,  -0.05f,    0.0f, 0.0f,
            0.0f,  -0.1f,   1.4f,     0.0f, 0.0f,
            0.0f,   0.0f,   0.0f,     1.0f, 0.0f
        )
        ColorMatrix(birdMatrix)
    }

    private val cachedHumanVisionMatrix by lazy {
        ColorMatrix().apply {
            val identityMatrix = floatArrayOf(
                1.0f, 0.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f, 0.0f
            )
            set(identityMatrix)
        }
    }

    private val cachedRedOnlyTestMatrix by lazy {
        val redOnlyMatrix = floatArrayOf(
            1.0f, 0.0f, 0.0f, 0.0f, 0.0f,
            0.3f, 0.3f, 0.3f, 0.0f, 0.0f,
            0.3f, 0.3f, 0.3f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f, 0.0f
        )
        ColorMatrix(redOnlyMatrix)
    }

    private val advancedDogMatrixArray: FloatArray by lazy {
        val matrix = FloatArray(20)
        val sConePeak = 430f
        val lConePeak = 555f
        val redWL = 650f
        val greenWL = 530f
        val blueWL = 430f

        matrix[0] = calculateConeResponse(redWL, lConePeak) * 0.8f
        matrix[1] = calculateConeResponse(greenWL, lConePeak) * 0.9f
        matrix[2] = calculateConeResponse(blueWL, sConePeak) * 0.1f
        matrix[3] = 0f; matrix[4] = 0f
        matrix[5] = calculateConeResponse(redWL, lConePeak) * 0.7f
        matrix[6] = calculateConeResponse(greenWL, lConePeak) * 0.8f
        matrix[7] = calculateConeResponse(blueWL, sConePeak) * 0.05f
        matrix[8] = 0f; matrix[9] = 0f
        matrix[10] = 0f
        matrix[11] = calculateConeResponse(greenWL, sConePeak) * 0.2f
        matrix[12] = calculateConeResponse(blueWL, sConePeak) * 0.9f
        matrix[13] = 0f; matrix[14] = 0f
        matrix[15] = 0f; matrix[16] = 0f; matrix[17] = 0f
        matrix[18] = 1f; matrix[19] = 0f

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
     * Simulates Dog Vision - Dichromatic with peaks at 430nm (blue-violet) and 555nm (yellow-green)
     * Based on peer-reviewed research showing dogs have dichromatic vision similar to deuteranopia
     * Dogs cannot distinguish between red and green colors, seeing them as variations of yellow and brown
     */
    fun getDogVisionMatrix(): ColorMatrix = cachedDogVisionMatrix

    fun getCatVisionMatrix(): ColorMatrix = cachedCatVisionMatrix

    fun getBirdVisionMatrix(): ColorMatrix = cachedBirdVisionMatrix

    fun getHumanVisionMatrix(): ColorMatrix = cachedHumanVisionMatrix

    fun getRedOnlyTestMatrix(): ColorMatrix = cachedRedOnlyTestMatrix

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