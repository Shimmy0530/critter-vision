package com.baltito.crittervision

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter

/**
 * Scientifically-based animal vision simulation filters
 * Based on peer-reviewed research on cone photoreceptor sensitivity
 */
object VisionColorFilter {

    enum class FilterType {
        ORIGINAL, DOG, CAT, BIRD
    }

    /**
     * Simulates Dog Vision - Dichromatic with peaks at 429nm (blue) and 555nm (yellow-green)
     * Based on Neitz et al. (1989) research showing dogs have protanopia-like vision
     */
    fun getDogVisionMatrix(): ColorMatrix {
        // Updated matrix based on user specifications for more realistic dog vision
        val dogMatrix = floatArrayOf(
            0.625f, 0.375f, 0.0f,     0.0f, 0.0f,  // Red channel
            0.7f,   0.3f,   0.0f,     0.0f, 0.0f,  // Green channel
            0.0f,   0.3f,   0.7f,     0.0f, 0.0f,  // Blue channel
            0.0f,   0.0f,   0.0f,     1.0f, 0.0f   // Alpha channel
        )
        return ColorMatrix(dogMatrix)
    }

    /**
     * Simulates Cat Vision - Dichromatic with neutral point at 505nm
     * Based on Clark & Clark (2016) research showing 460nm and 560nm peaks
     * Note: Some evidence suggests limited trichromatic ability
     */
    fun getCatVisionMatrix(): ColorMatrix {
        // Updated matrix based on user specifications for more realistic cat vision
        val catMatrix = floatArrayOf(
            0.567f, 0.433f, 0.0f,     0.0f, 0.0f,  // Red channel
            0.558f, 0.442f, 0.0f,     0.0f, 0.0f,  // Green channel
            0.0f,   0.242f, 0.758f,   0.0f, 0.0f,  // Blue channel
            0.0f,   0.0f,   0.0f,     1.0f, 0.0f   // Alpha channel
        )
        return ColorMatrix(catMatrix)
    }

    /**
     * Simulates Bird Vision - Tetrachromatic with UV perception (320-400nm)
     * Based on research showing peaks at ~360nm (UV), 450nm (blue), 510-540nm (green), 565-620nm (red)
     * Birds see dramatically enhanced colors and ultraviolet patterns
     */
    fun getBirdVisionMatrix(): ColorMatrix {
        // Updated matrix based on user specifications for more realistic bird vision
        val birdMatrix = floatArrayOf(
            0.8f,   0.2f,   0.0f,     0.0f, 0.0f,  // Red channel
            0.1f,   0.8f,   0.1f,     0.0f, 0.0f,  // Green channel
            0.2f,   0.1f,   0.9f,     0.0f, 0.0f,  // Blue channel
            0.0f,   0.0f,   0.0f,     1.0f, 0.0f   // Alpha channel
        )
        return ColorMatrix(birdMatrix)
    }

    // Keep existing ColorMatrixColorFilter methods for backward compatibility
    fun getDogVisionFilter(): ColorMatrixColorFilter = ColorMatrixColorFilter(getDogVisionMatrix())
    fun getCatVisionFilter(): ColorMatrixColorFilter = ColorMatrixColorFilter(getCatVisionMatrix())
    fun getBirdVisionFilter(): ColorMatrixColorFilter = ColorMatrixColorFilter(getBirdVisionMatrix())

    fun getFilter(type: FilterType): ColorMatrixColorFilter? {
        return when (type) {
            FilterType.DOG -> getDogVisionFilter()
            FilterType.CAT -> getCatVisionFilter()
            FilterType.BIRD -> getBirdVisionFilter()
            FilterType.ORIGINAL -> null
        }
    }

    // Method that returns ColorMatrix directly
    fun getMatrix(type: FilterType): ColorMatrix? {
        return when (type) {
            FilterType.DOG -> getDogVisionMatrix()
            FilterType.CAT -> getCatVisionMatrix()
            FilterType.BIRD -> getBirdVisionMatrix()
            FilterType.ORIGINAL -> null
        }
    }

    /**
     * Get scientific description of each vision type
     */
    fun getVisionDescription(type: FilterType): String {
        return when (type) {
            FilterType.DOG -> "Dichromatic vision (429nm, 555nm peaks) - similar to red-green color blindness"
            FilterType.CAT -> "Dichromatic vision (460nm, 560nm peaks) - neutral point at 505nm"
            FilterType.BIRD -> "Tetrachromatic vision with UV perception (320-400nm) - four color dimensions"
            FilterType.ORIGINAL -> "Human trichromatic vision (420nm, 534nm, 564nm peaks)"
        }
    }
}