package com.baltito.crittervision

import android.graphics.ColorMatrix

/**
 * Scientifically-based animal vision simulation filters
 * Based on peer-reviewed research on cone photoreceptor sensitivity
 * and color space transformations
 */
object VisionColorFilter {

    enum class FilterType {
        ORIGINAL, DOG, CAT, BIRD
    }

    /**
     * Simulates Dog Vision - Dichromatic with peaks at 429nm (blue) and 555nm (yellow-green)
     * Based on Neitz et al. (1989) research showing dogs have protanopia-like vision
     * Dogs are red-green colorblind and see blues and yellows
     */
    fun getDogVisionMatrix(): ColorMatrix {
        // EXAGGERATED Dog vision matrix: Strong protanopia simulation
        // Reds and greens become very yellow/brown, blues enhanced
        val dogMatrix = floatArrayOf(
            0.3f,   0.7f,   0.0f,     0.0f, 0.0f,  // Red becomes very green-yellow
            0.8f,   0.2f,   0.0f,     0.0f, 0.0f,  // Green becomes very red-yellow
            0.0f,   0.0f,   1.5f,     0.0f, 0.0f,  // Blue highly enhanced
            0.0f,   0.0f,   0.0f,     1.0f, 0.0f   // Alpha unchanged
        )
        return ColorMatrix(dogMatrix)
    }

    /**
     * Simulates Cat Vision - Dichromatic with enhanced blue sensitivity
     * Based on research showing cats have enhanced blue vision and reduced red sensitivity
     * Cats see blues and greens well, reds appear muted
     */
    fun getCatVisionMatrix(): ColorMatrix {
        // EXAGGERATED Cat vision matrix: Very strong blue enhancement, red suppression
        // Cats see blues and greens vividly, reds very muted
        val catMatrix = floatArrayOf(
            0.2f,   0.6f,   0.0f,     0.0f, 0.0f,  // Red very muted, mixed with green
            0.0f,   0.9f,   0.3f,     0.0f, 0.0f,  // Green enhanced with blue
            0.0f,   0.3f,   1.8f,     0.0f, 0.0f,  // Blue extremely enhanced
            0.0f,   0.0f,   0.0f,     1.0f, 0.0f   // Alpha unchanged
        )
        return ColorMatrix(catMatrix)
    }

    /**
     * Simulates Bird Vision - Enhanced color perception with UV simulation
     * Based on research showing birds have tetrachromatic vision with UV sensitivity
     * Birds see more vibrant colors and patterns invisible to humans
     */
    fun getBirdVisionMatrix(): ColorMatrix {
        // EXAGGERATED Bird vision matrix: Extremely enhanced saturation and UV simulation
        // Birds see very vibrant colors with strong UV influence
        val birdMatrix = floatArrayOf(
            1.8f,   0.2f,   0.3f,     0.0f, 0.0f,  // Red extremely enhanced with UV
            0.2f,   2.0f,   0.3f,     0.0f, 0.0f,  // Green extremely enhanced with UV
            0.3f,   0.3f,   2.5f,     0.0f, 0.0f,  // Blue extremely enhanced (UV simulation)
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

    fun getMatrix(type: FilterType): ColorMatrix? {
        return when (type) {
            FilterType.DOG -> getDogVisionMatrix()
            FilterType.CAT -> getCatVisionMatrix()
            FilterType.BIRD -> getBirdVisionMatrix()
            FilterType.ORIGINAL -> getHumanVisionMatrix()
        }
    }

    /**
     * Get scientific description of each vision type
     */
    fun getVisionDescription(type: FilterType): String {
        return when (type) {
            FilterType.DOG -> "Dichromatic vision (429nm, 555nm peaks) - Red-green colorblind, sees blues and yellows"
            FilterType.CAT -> "Dichromatic vision with enhanced blue sensitivity - Reds appear muted, blues enhanced"
            FilterType.BIRD -> "Tetrachromatic vision with UV perception - Enhanced color perception and UV patterns"
            FilterType.ORIGINAL -> "Human trichromatic vision (420nm, 534nm, 564nm peaks)"
        }
    }
}