package com.example.crittervision

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter

object VisionColorFilter {

    enum class FilterType {
        ORIGINAL, DOG, CAT, BIRD
    }

    /**
     * Simulates Dog Vision (Protanomaly-like: reduced red sensitivity).
     */
    fun getDogVisionMatrix(): ColorMatrix {
        val dogMatrix = floatArrayOf(
            0.56667f, 0.43333f, 0.0f,     0.0f, 0.0f,
            0.55833f, 0.44167f, 0.0f,     0.0f, 0.0f,
            0.0f,     0.24167f, 0.75833f, 0.0f, 0.0f,
            0.0f,     0.0f,     0.0f,     1.0f, 0.0f
        )
        return ColorMatrix(dogMatrix)
    }

    /**
     * Simulates Cat Vision (Deuteranopia-like: reduced green sensitivity).
     */
    fun getCatVisionMatrix(): ColorMatrix {
        val catMatrixValues = floatArrayOf(
            0.3602f,  0.8636f, -0.2238f, 0.0f, 0.0f,
            0.2610f,  0.6021f,  0.1369f, 0.0f, 0.0f,
            -0.0580f,  0.0922f,  0.9658f, 0.0f, 0.0f,
            0.0f,     0.0f,     0.0f,    1.0f, 0.0f
        )
        val cm = ColorMatrix(catMatrixValues)

        // Slight brightness/contrast increase
        val brightnessMatrix = ColorMatrix()
        brightnessMatrix.setScale(1.1f, 1.1f, 1.1f, 1.0f)
        cm.postConcat(brightnessMatrix)
        return cm
    }

    /**
     * Simulates Bird Vision (enhanced color vibrancy, UV hint).
     */
    fun getBirdVisionMatrix(): ColorMatrix {
        // Increased saturation
        val saturationMatrix = ColorMatrix()
        saturationMatrix.setSaturation(1.8f)

        // Subtle violet/blue overlay for UV perception hint
        val uvHintMatrix = ColorMatrix(floatArrayOf(
            1.0f, 0.0f, 0.1f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f, 0.0f,
            0.1f, 0.1f, 1.0f, 0.0f, 5.0f,
            0.0f, 0.0f, 0.0f, 1.0f, 0.0f
        ))
        saturationMatrix.postConcat(uvHintMatrix)
        return saturationMatrix
    }

    // Keep the existing ColorMatrixColorFilter methods for backward compatibility
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

    // New method that returns ColorMatrix directly
    fun getMatrix(type: FilterType): ColorMatrix? {
        return when (type) {
            FilterType.DOG -> getDogVisionMatrix()
            FilterType.CAT -> getCatVisionMatrix()
            FilterType.BIRD -> getBirdVisionMatrix()
            FilterType.ORIGINAL -> null
        }
    }
}