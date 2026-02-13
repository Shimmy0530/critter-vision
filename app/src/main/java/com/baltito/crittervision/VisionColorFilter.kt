package com.baltito.crittervision

/**
 * Scientifically-validated animal vision simulation parameters.
 *
 * Dog: Machado, Oliveira & Fernandes (2009) deuteranopia at severity=1.0
 * Cat: Viénot/Brettel method adapted for cat cone positions (S=450nm, L=554nm)
 *      with 0.625 desaturation blend modeling low cone density
 * Bird: Heuristic spectral shift matrix based on Hart (2001) cone data
 *       with oil droplet filtering approximation via saturation boost + UV proxy
 * Eagle: Tetrachromatic + 4-8x acuity; enhanced color separation with brightness offset
 * Horse: Dichromatic (blue/yellow), red/green collapse
 * Mantis Shrimp: 12-16 receptor types; hypersaturated with UV proxy
 * Reindeer: UV-sensitive for snow/trail navigation; UV → cyan glow
 * Cuttlefish: Polarization-sensitive W-pupil; enhanced specular detection
 * Pit Viper: Infrared pit organs; luminance → thermal red mapping
 *
 * All 3x3 matrices operate on linear RGB (sRGB must be linearized first).
 * colorOffset is applied in sRGB display space after gamma encoding.
 */
object VisionColorFilter {

    enum class VisionMode {
        HUMAN, DOG, CAT, BIRD,
        EAGLE, HORSE, MANTIS_SHRIMP, REINDEER, CUTTLEFISH, PIT_VIPER,
        RED_ONLY, GREEN_ONLY, BLUE_ONLY
    }

    data class VisionParams(
        val matrix: FloatArray,        // 9 elements, row-major 3x3 (linear RGB)
        val colorOffset: FloatArray = floatArrayOf(0f, 0f, 0f), // 3 elements, RGB additive offset (sRGB space)
        val saturationBoost: Float = 1.0f,
        val uvProxyWeight: Float = 0.0f,
        val displayName: String,
        val description: String
    )

    // Identity
    private val IDENTITY = floatArrayOf(
        1f, 0f, 0f,
        0f, 1f, 0f,
        0f, 0f, 1f
    )

    // Machado et al. 2009 — deuteranopia severity=1.0 (linear RGB)
    // IEEE Transactions on Visualization and Computer Graphics
    // Row sums ≈ 1.0, preserving luminance
    private val DOG_MATRIX = floatArrayOf(
         0.367322f,  0.860646f, -0.227968f,
         0.280085f,  0.672501f,  0.047413f,
        -0.011820f,  0.042940f,  0.968881f
    )

    // Derived: Viénot/Brettel method at cat cone positions (S=450nm, L=554nm)
    // blended with Rec.709 luminance matrix at factor 0.625 to model low cone density
    // M_cat = 0.625 × M_dichromat + 0.375 × M_luminance
    private val CAT_MATRIX = floatArrayOf(
        0.317f, 0.656f, 0.026f,
        0.255f, 0.688f, 0.057f,
        0.074f, 0.310f, 0.616f
    )

    // Heuristic spectral shift matrix (Hart 2001 cone data + oil droplet shifts)
    // R +10% sharpened, G +20% sharpened, B +40% (combined UV+S response)
    // Applied with saturation ×1.35 and UV proxy weight 0.30
    private val BIRD_MATRIX = floatArrayOf(
         1.10f, -0.05f, -0.05f,
        -0.08f,  1.20f, -0.12f,
        -0.05f, -0.15f,  1.40f
    )

    // Debug: channel isolation as color
    private val RED_ONLY_MATRIX = floatArrayOf(
        1f, 0f, 0f,
        0f, 0f, 0f,
        0f, 0f, 0f
    )

    private val GREEN_ONLY_MATRIX = floatArrayOf(
        0f, 0f, 0f,
        0f, 1f, 0f,
        0f, 0f, 0f
    )

    private val BLUE_ONLY_MATRIX = floatArrayOf(
        0f, 0f, 0f,
        0f, 0f, 0f,
        0f, 0f, 1f
    )

    // Eagle: Tetrachromatic + extreme acuity (4-8x human)
    // Enhanced color separation with brightness offset from 4x5 column 5
    private val EAGLE_MATRIX = floatArrayOf(
         1.40f, -0.20f, -0.10f,
        -0.10f,  1.30f, -0.10f,
        -0.10f, -0.20f,  1.50f
    )
    private val EAGLE_OFFSET = floatArrayOf(0.10f, 0.10f, 0.20f)

    // Horse: Dichromatic (S ~428nm, L ~539nm), red/green collapse to muddy yellow
    private val HORSE_MATRIX = floatArrayOf(
        0.15f, 0.65f, 0.20f,
        0.10f, 0.55f, 0.35f,
        0.05f, 0.25f, 0.70f
    )

    // Mantis Shrimp: 12-16 photoreceptor types + UV + polarization
    // Hypersaturated "impossible colors" approximation
    private val MANTIS_SHRIMP_MATRIX = floatArrayOf(
         1.30f, -0.20f,  0.10f,
        -0.10f,  1.40f, -0.10f,
         0.20f, -0.30f,  1.50f
    )

    // Reindeer: UV-sensitive vision for snow/trail navigation
    // UV reflectance mapped to blue channel boost
    private val REINDEER_MATRIX = floatArrayOf(
        0.80f, 0.10f, 0.40f,
        0.20f, 1.00f, 0.30f,
        0.30f, 0.20f, 1.40f
    )
    private val REINDEER_OFFSET = floatArrayOf(0f, 0f, 0.20f)

    // Cuttlefish: Polarization sensitivity + W-shaped pupils
    // Enhanced specular/shiny surface detection
    private val CUTTLEFISH_MATRIX = floatArrayOf(
         0.90f,  0.20f, -0.10f,
         0.10f,  0.80f,  0.10f,
        -0.10f,  0.10f,  1.10f
    )
    private val CUTTLEFISH_OFFSET = floatArrayOf(0.10f, 0.10f, 0.20f)

    // Pit Viper: Infrared pit organs — luminance → thermal red channel
    private val PIT_VIPER_MATRIX = floatArrayOf(
        0.30f, 0.60f, 0.10f,
        0.00f, 0.00f, 0.00f,
        0.00f, 0.00f, 0.00f
    )

    fun getParams(mode: VisionMode): VisionParams = when (mode) {
        VisionMode.HUMAN -> VisionParams(
            matrix = IDENTITY,
            displayName = "Human Vision",
            description = "Trichromatic — full RGB spectrum"
        )
        VisionMode.DOG -> VisionParams(
            matrix = DOG_MATRIX,
            displayName = "Dog Vision",
            description = "Dichromatic (Machado 2009 deuteranopia) — blue-yellow world, no red-green"
        )
        VisionMode.CAT -> VisionParams(
            matrix = CAT_MATRIX,
            displayName = "Cat Vision",
            description = "Dichromatic + desaturated — washed-out pastels, S=450nm L=554nm"
        )
        VisionMode.BIRD -> VisionParams(
            matrix = BIRD_MATRIX,
            saturationBoost = 1.35f,
            uvProxyWeight = 0.30f,
            displayName = "Bird Vision",
            description = "Tetrachromatic approximation — enhanced saturation with UV proxy"
        )
        VisionMode.EAGLE -> VisionParams(
            matrix = EAGLE_MATRIX,
            colorOffset = EAGLE_OFFSET,
            saturationBoost = 1.20f,
            displayName = "Eagle Vision",
            description = "Tetrachromatic + 4-8× acuity — extreme color separation and brightness"
        )
        VisionMode.HORSE -> VisionParams(
            matrix = HORSE_MATRIX,
            displayName = "Horse Vision",
            description = "Dichromatic (S=428nm L=539nm) — blue-yellow world, red/green collapse"
        )
        VisionMode.MANTIS_SHRIMP -> VisionParams(
            matrix = MANTIS_SHRIMP_MATRIX,
            saturationBoost = 1.80f,
            uvProxyWeight = 0.40f,
            displayName = "Mantis Shrimp",
            description = "12-16 receptor types — hypersaturated with UV + polarization proxy"
        )
        VisionMode.REINDEER -> VisionParams(
            matrix = REINDEER_MATRIX,
            colorOffset = REINDEER_OFFSET,
            uvProxyWeight = 0.50f,
            displayName = "Reindeer Vision",
            description = "UV-sensitive — snow/trail navigation with UV → cyan glow"
        )
        VisionMode.CUTTLEFISH -> VisionParams(
            matrix = CUTTLEFISH_MATRIX,
            colorOffset = CUTTLEFISH_OFFSET,
            saturationBoost = 1.15f,
            displayName = "Cuttlefish Vision",
            description = "Polarization-sensitive W-pupil — enhanced specular surface detection"
        )
        VisionMode.PIT_VIPER -> VisionParams(
            matrix = PIT_VIPER_MATRIX,
            displayName = "Pit Viper Vision",
            description = "Infrared pit organs — luminance mapped to thermal red"
        )
        VisionMode.RED_ONLY -> VisionParams(
            matrix = RED_ONLY_MATRIX,
            displayName = "Red Channel",
            description = "Red channel isolation as grayscale"
        )
        VisionMode.GREEN_ONLY -> VisionParams(
            matrix = GREEN_ONLY_MATRIX,
            displayName = "Green Channel",
            description = "Green channel isolation as grayscale"
        )
        VisionMode.BLUE_ONLY -> VisionParams(
            matrix = BLUE_ONLY_MATRIX,
            displayName = "Blue Channel",
            description = "Blue channel isolation as grayscale"
        )
    }
}
