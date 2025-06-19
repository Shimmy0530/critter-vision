package com.baltito.crittervision

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter

/**
 * Scientifically-based animal vision simulation filters
 * Based on peer-reviewed research on cone photoreceptor sensitivity
 */
object VisionColorFilter {

    enum class FilterType {
        ORIGINAL, DOG, CAT, BIRD, GRAYSCALE
    }

    /**
     * Simulates Dog Vision - Protanopia (red-green color blindness)
     * Based on scientific research showing dogs have dichromatic vision
     */
    fun getDogVisionMatrix(): ColorMatrix {
        return ColorMatrix(floatArrayOf(
            0.567f, 0.433f, 0.000f, 0f, 0f,
            0.558f, 0.442f, 0.000f, 0f, 0f,
            0.000f, 0.242f, 0.758f, 0f, 0f,
            0.000f, 0.000f, 0.000f, 1f, 0f
        ))
    }

    /**
     * Simulates Cat Vision - Deuteranopia-like vision
     * Based on research showing cats have limited color discrimination
     */
    fun getCatVisionMatrix(): ColorMatrix {
        return ColorMatrix(floatArrayOf(
            0.625f, 0.375f, 0.000f, 0f, 0f,
            0.700f, 0.300f, 0.000f, 0f, 0f,
            0.000f, 0.300f, 0.700f, 0f, 0f,
            0.000f, 0.000f, 0.000f, 1f, 0f
        ))
    }

    /**
     * Simulates Bird Vision - Enhanced tetrachromatic vision with UV perception
     * Birds have superior color vision with four types of cone cells
     */
    fun getBirdVisionMatrix(): ColorMatrix {
        // Enhanced saturation and contrast to simulate tetrachromatic vision
        val saturationMatrix = ColorMatrix()
        saturationMatrix.setSaturation(1.8f) // Enhanced color richness

        // UV enhancement simulation - adds violet/purple shift
        val uvEnhancementMatrix = ColorMatrix(floatArrayOf(
            1.05f, 0.0f, 0.10f, 0.0f, 8.0f,  // R: Enhanced red + UV contribution
            0.02f, 1.15f, 0.08f, 0.0f, 3.0f, // G: Enhanced green + UV
            0.10f, 0.10f, 1.25f, 0.0f, 12.0f, // B: Strong blue + UV (violet)
            0.0f, 0.0f, 0.0f, 1.0f, 0.0f     // A: Alpha unchanged
        ))

        saturationMatrix.postConcat(uvEnhancementMatrix)

        // Boost contrast for sharper perception
        val contrastMatrix = getContrastMatrix(1.12f)
        saturationMatrix.postConcat(contrastMatrix)

        return saturationMatrix
    }

    /**
     * Simulates complete color blindness (Achromatopsia)
     * Converts everything to grayscale using luminance weights
     */
    fun getGrayscaleMatrix(): ColorMatrix {
        return ColorMatrix(floatArrayOf(
            0.299f, 0.587f, 0.114f, 0f, 0f,
            0.299f, 0.587f, 0.114f, 0f, 0f,
            0.299f, 0.587f, 0.114f, 0f, 0f,
            0.000f, 0.000f, 0.000f, 1f, 0f
        ))
    }

    // --- Utility: Contrast Matrix ---
    private fun getContrastMatrix(contrast: Float): ColorMatrix {
        // contrast >1 increases, <1 decreases. 1.0 = no change.
        val scale = contrast
        val translate = (-0.5f * scale + 0.5f) * 255f
        return ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
    }

    // --- ColorMatrixColorFilter methods ---
    fun getDogVisionFilter(): ColorMatrixColorFilter = ColorMatrixColorFilter(getDogVisionMatrix())
    fun getCatVisionFilter(): ColorMatrixColorFilter = ColorMatrixColorFilter(getCatVisionMatrix())
    fun getBirdVisionFilter(): ColorMatrixColorFilter = ColorMatrixColorFilter(getBirdVisionMatrix())
    fun getGrayscaleFilter(): ColorMatrixColorFilter = ColorMatrixColorFilter(getGrayscaleMatrix())

    fun getFilter(type: FilterType): ColorMatrixColorFilter? {
        return when (type) {
            FilterType.DOG -> getDogVisionFilter()
            FilterType.CAT -> getCatVisionFilter()
            FilterType.BIRD -> getBirdVisionFilter()
            FilterType.GRAYSCALE -> getGrayscaleFilter()
            FilterType.ORIGINAL -> null
        }
    }

    // Method that returns ColorMatrix directly
    fun getMatrix(type: FilterType): ColorMatrix? {
        return when (type) {
            FilterType.DOG -> getDogVisionMatrix()
            FilterType.CAT -> getCatVisionMatrix()
            FilterType.BIRD -> getBirdVisionMatrix()
            FilterType.GRAYSCALE -> getGrayscaleMatrix()
            FilterType.ORIGINAL -> null
        }
    }

    /**
     * Get scientific description of each vision type
     */
    fun getVisionDescription(type: FilterType): String {
        return when (type) {
            FilterType.DOG -> "Protanopia - Dichromatic vision missing long-wavelength (red) cones"
            FilterType.CAT -> "Deuteranopia-like - Limited red-green discrimination, enhanced blue sensitivity"
            FilterType.BIRD -> "Tetrachromatic vision with UV perception - Four color dimensions including ultraviolet"
            FilterType.GRAYSCALE -> "Achromatopsia - Complete color blindness, grayscale vision only"
            FilterType.ORIGINAL -> "Human trichromatic vision (420nm, 534nm, 564nm peaks)"
        }
    }
}