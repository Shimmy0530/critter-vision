package com.example.crittervision

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter

// Note: These color matrices are initial approximations for simulating animal vision.
// They may require further tuning based on visual testing to achieve the desired perceptual effect.
object VisionColorFilter {

    enum class FilterType {
        ORIGINAL, DOG, CAT, BIRD
    }

    /**
     * Simulates Dog Vision (Protanomaly-like: reduced red sensitivity).
     * Reds appear darker and desaturated; greens shift towards yellow/brown.
     * Dogs primarily see in blues and yellows.
     */
    fun getDogVisionFilter(): ColorMatrixColorFilter {
        // Protanomaly matrix (common approximation for red-green color blindness)
        val dogMatrix = floatArrayOf(
            0.56667f, 0.43333f, 0.0f,     0.0f, 0.0f, // R channel output
            0.55833f, 0.44167f, 0.0f,     0.0f, 0.0f, // G channel output
            0.0f,     0.24167f, 0.75833f, 0.0f, 0.0f, // B channel output
            0.0f,     0.0f,     0.0f,     1.0f, 0.0f  // A channel output
        )
        return ColorMatrixColorFilter(ColorMatrix(dogMatrix))
    }

    /**
     * Simulates Cat Vision (Deuteranopia-like: reduced green sensitivity).
     * Greens shift towards beige/yellow; reds appear yellowish-brown.
     * Adds a slight brightness enhancement.
     */
    fun getCatVisionFilter(): ColorMatrixColorFilter {
        // Deuteranopia matrix (greens look more like beige/yellow, reds look yellowish brown)
        val catMatrixValues = floatArrayOf(
            0.3602f,  0.8636f, -0.2238f, 0.0f, 0.0f,
            0.2610f,  0.6021f,  0.1369f, 0.0f, 0.0f,
           -0.0580f,  0.0922f,  0.9658f, 0.0f, 0.0f,
            0.0f,     0.0f,     0.0f,    1.0f, 0.0f
        )
        val cm = ColorMatrix(catMatrixValues)

        // Slight brightness/contrast increase for cat night vision simulation aspect
        val brightnessMatrix = ColorMatrix().apply {
            setScale(1.1f, 1.1f, 1.1f, 1.0f) // Scale RGB, keep Alpha
        }
        cm.postConcat(brightnessMatrix)
        return ColorMatrixColorFilter(cm)
    }

    /**
     * Simulates Bird Vision (enhanced color vibrancy, UV hint).
     * Increases saturation and adds a subtle violet/blue shift.
     */
    fun getBirdVisionFilter(): ColorMatrixColorFilter {
        // Increased saturation
        val saturationMatrix = ColorMatrix().apply {
            setSaturation(1.8f)
        }
        // Subtle violet/blue overlay for UV perception hint
        val uvHintMatrix = ColorMatrix(floatArrayOf(
            1.0f, 0.0f, 0.1f, 0.0f, 0.0f,  // R channel
            0.0f, 1.0f, 0.0f, 0.0f, 0.0f,  // G channel
            0.1f, 0.1f, 1.0f, 0.0f, 5.0f,  // B channel (add some R & G to B, slight blue bias value)
            0.0f, 0.0f, 0.0f, 1.0f, 0.0f   // A channel
        ))
        saturationMatrix.postConcat(uvHintMatrix)
        return ColorMatrixColorFilter(saturationMatrix)
    }

    fun getFilter(type: FilterType): ColorMatrixColorFilter? {
        return when (type) {
            FilterType.DOG -> getDogVisionFilter()
            FilterType.CAT -> getCatVisionFilter()
            FilterType.BIRD -> getBirdVisionFilter()
            FilterType.ORIGINAL -> null
        }
    }
}
