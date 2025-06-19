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

    /**
     * Simulates Dog Vision - Dichromatic with peaks at 429nm (blue) and 555nm (yellow-green)
     * Based on Neitz et al. (1989) research showing dogs have protanopia-like vision
     */
    fun getDogVisionMatrix(): ColorMatrix {
        // More accurate dichromatic simulation based on 429nm and 555nm peaks
        val dogMatrix = ColorMatrix(floatArrayOf(
            0.625f, 0.375f, 0.0f, 0.0f, 0.0f, // R: Blue+Green mixed, no pure red
            0.70f, 0.30f, 0.0f, 0.0f, 0.0f,   // G: Shifted toward yellow-green
            0.0f, 0.30f, 0.70f, 0.0f, 0.0f,   // B: Strong blue response
            0.0f, 0.0f, 0.0f, 1.0f, 0.0f      // A: Alpha unchanged
        ))
        // Reduce saturation (muted world)
        val saturationMatrix = ColorMatrix()
        saturationMatrix.setSaturation(0.6f)
        dogMatrix.postConcat(saturationMatrix)
        // Slightly reduce contrast
        val contrastMatrix = getContrastMatrix(0.92f)
        dogMatrix.postConcat(contrastMatrix)
        return dogMatrix
    }

    /**
     * Simulates Cat Vision - Dichromatic with neutral point at 505nm
     * Based on Clark & Clark (2016) research showing 460nm and 560nm peaks
     * Note: Some evidence suggests limited trichromatic ability
     */
    fun getCatVisionMatrix(): ColorMatrix {
        // Dichromatic vision similar to deuteranope (green-red color blind)
        val catMatrixValues = floatArrayOf(
            0.40f, 0.60f, -0.20f, 0.0f, 0.0f, // R: Shifted perception
            0.30f, 0.70f, 0.0f, 0.0f, 0.0f,   // G: Strong green response
            -0.05f, 0.15f, 0.90f, 0.0f, 0.0f, // B: Blue-dominant
            0.0f, 0.0f, 0.0f, 1.0f, 0.0f      // A: Alpha unchanged
        )
        val cm = ColorMatrix(catMatrixValues)
        // Reduce saturation (muted colors)
        val saturationMatrix = ColorMatrix()
        saturationMatrix.setSaturation(0.65f)
        cm.postConcat(saturationMatrix)
        // Slightly reduce contrast
        val contrastMatrix = getContrastMatrix(0.95f)
        cm.postConcat(contrastMatrix)
        // Slight brightness boost for low-light adaptation
        val brightnessMatrix = ColorMatrix()
        brightnessMatrix.setScale(1.10f, 1.10f, 1.10f, 1.0f)
        cm.postConcat(brightnessMatrix)
        return cm
    }

    /**
     * Simulates Bird Vision - Tetrachromatic with UV perception (320-400nm)
     * Based on research showing peaks at ~360nm (UV), 450nm (blue), 510-540nm (green), 565-620nm (red)
     * Birds see dramatically enhanced colors and ultraviolet patterns
     */
    fun getBirdVisionMatrix(): ColorMatrix {
        // Enhanced tetrachromatic vision simulation
        // Increased saturation to represent richer color perception
        val saturationMatrix = ColorMatrix()
        saturationMatrix.setSaturation(2.2f) // Higher than original for tetrachromatic richness
        // UV perception simulation - adds purple/violet shift to represent UV detection
        val uvEnhancementMatrix = ColorMatrix(floatArrayOf(
            1.1f, 0.0f, 0.15f, 0.0f, 10.0f, // R: Enhanced red + UV contribution
            0.05f, 1.2f, 0.10f, 0.0f, 5.0f, // G: Enhanced green + UV
            0.15f, 0.15f, 1.3f, 0.0f, 15.0f, // B: Strong blue + UV (violet)
            0.0f, 0.0f, 0.0f, 1.0f, 0.0f    // A: Alpha unchanged
        ))
        saturationMatrix.postConcat(uvEnhancementMatrix)
        // Boost contrast for sharper perception
        val contrastMatrix = getContrastMatrix(1.15f)
        saturationMatrix.postConcat(contrastMatrix)
        return saturationMatrix
    }

    // --- ColorMatrixColorFilter methods ---
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
