package com.baltito.crittervision

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter

/**
 * Android ColorMatrix (4x5) representations of each [VisionColorFilter.VisionMode].
 *
 * ## Approach choice
 *
 * The primary rendering path for CritterVision is GPU-based: CameraX feeds frames
 * into [AnimalVisionProcessor], which applies a GLSL fragment shader with sRGB
 * linearization, a 3×3 color matrix, saturation boost, UV proxy simulation, and
 * intensity blending — all in a single draw call with no shader recompilation.
 *
 * These 4×5 [ColorMatrix] versions are provided as a **fallback** for contexts where
 * OpenGL is unavailable (e.g. applying filters to saved Bitmaps, thumbnail previews,
 * or ImageView overlays). They embed the 3×3 linear-RGB matrices from
 * [VisionColorFilter] into Android's 4×5 RGBA+offset format.
 *
 * **Limitations vs. the shader path:**
 * - ColorMatrix operates in sRGB (gamma-encoded) space, not linear RGB.
 *   Results will differ slightly from the shader, which linearizes before transform.
 * - Saturation boost, UV proxy, and intensity blending are not representable in a
 *   single ColorMatrix. Only the core 3×3 color transform + additive offset are
 *   included here.
 *
 * ## How to add a new species
 *
 * 1. Add a new entry to [VisionColorFilter.VisionMode].
 * 2. Define a 9-element row-major 3×3 matrix in [VisionColorFilter] with a
 *    [VisionColorFilter.VisionParams] mapping in `getParams()`.
 * 3. Add a corresponding 4×5 [ColorMatrix] array here and update [getColorMatrix].
 * 4. In [MainActivity], add the new mode to `animalModes` or `bottomModes` so a
 *    button appears in the UI. No shader recompilation is needed — the processor
 *    reads new parameters via `@Volatile` fields each frame.
 * 5. Add unit tests in `VisionColorFilterTest` for row sums, parameter ranges, etc.
 */
object VisionMatrices {

    // ── Dog vision (dichromat approximation) ────────────────────────────
    // Machado et al. 2009 deuteranopia at severity=1.0.
    // Embedded in 4×5 format: rows = R', G', B', A'; columns = R, G, B, A, offset.
    // Note: the 3×3 core matches VisionColorFilter.DOG_MATRIX (linear RGB);
    // when used via ColorMatrix it operates in sRGB space (approximate).
    val dogMatrixArray = floatArrayOf(
        0.367322f,  0.860646f, -0.227968f, 0f, 0f,
        0.280085f,  0.672501f,  0.047413f, 0f, 0f,
       -0.011820f,  0.042940f,  0.968881f, 0f, 0f,
        0f,         0f,         0f,        1f, 0f
    )

    // ── Cat vision (dichromat + desaturated) ────────────────────────────
    // Viénot/Brettel method at S=450nm L=554nm, blended 0.625 with Rec.709
    // luminance to model low cone density (25:1 rod:cone ratio).
    val catMatrixArray = floatArrayOf(
        0.317f,  0.656f, 0.026f, 0f, 0f,
        0.255f,  0.688f, 0.057f, 0f, 0f,
        0.074f,  0.310f, 0.616f, 0f, 0f,
        0f,      0f,     0f,     1f, 0f
    )

    // ── Bird vision (tetrachromat-inspired) ─────────────────────────────
    // Heuristic spectral shift (Hart 2001) with oil droplet narrowing.
    // Full effect also requires saturation ×1.35 and UV proxy 0.30 (shader only).
    val birdMatrixArray = floatArrayOf(
        1.10f, -0.05f, -0.05f, 0f, 0f,
       -0.08f,  1.20f, -0.12f, 0f, 0f,
       -0.05f, -0.15f,  1.40f, 0f, 0f,
        0f,     0f,     0f,    1f, 0f
    )

    // ── Eagle vision (tetrachromatic + acuity) ──────────────────────────
    // Enhanced color separation; offset in column 5 approximates brightness boost.
    val eagleMatrixArray = floatArrayOf(
        1.40f, -0.20f, -0.10f, 0f, 0.10f,
       -0.10f,  1.30f, -0.10f, 0f, 0.10f,
       -0.10f, -0.20f,  1.50f, 0f, 0.20f,
        0f,     0f,     0f,    1f, 0f
    )

    // ── Horse vision (dichromatic, S=428nm L=539nm) ─────────────────────
    // Red/green collapse to muddy yellow; blue-yellow world.
    val horseMatrixArray = floatArrayOf(
        0.15f, 0.65f, 0.20f, 0f, 0f,
        0.10f, 0.55f, 0.35f, 0f, 0f,
        0.05f, 0.25f, 0.70f, 0f, 0f,
        0f,    0f,    0f,    1f, 0f
    )

    // ── Mantis Shrimp (12-16 receptors, hypersaturated) ─────────────────
    // Full effect requires saturation ×1.80 and UV proxy 0.40 (shader only).
    val mantisShrimpMatrixArray = floatArrayOf(
        1.30f, -0.20f,  0.10f, 0f, 0f,
       -0.10f,  1.40f, -0.10f, 0f, 0f,
        0.20f, -0.30f,  1.50f, 0f, 0f,
        0f,     0f,     0f,    1f, 0f
    )

    // ── Reindeer (UV-sensitive) ─────────────────────────────────────────
    // UV reflectance → blue offset in column 5; full UV proxy in shader only.
    val reindeerMatrixArray = floatArrayOf(
        0.80f, 0.10f, 0.40f, 0f, 0f,
        0.20f, 1.00f, 0.30f, 0f, 0f,
        0.30f, 0.20f, 1.40f, 0f, 0.20f,
        0f,    0f,    0f,    1f, 0f
    )

    // ── Cuttlefish (polarization-sensitive) ──────────────────────────────
    // Subtle saturation boost (shader only); offset approximated in column 5.
    val cuttlefishMatrixArray = floatArrayOf(
        0.90f,  0.20f, -0.10f, 0f, 0.10f,
        0.10f,  0.80f,  0.10f, 0f, 0.10f,
       -0.10f,  0.10f,  1.10f, 0f, 0.20f,
        0f,     0f,     0f,    1f, 0f
    )

    // ── Pit Viper (infrared pit organs) ─────────────────────────────────
    // Luminance → thermal red; green/blue rows zeroed.
    val pitViperMatrixArray = floatArrayOf(
        0.30f, 0.60f, 0.10f, 0f, 0f,
        0.00f, 0.00f, 0.00f, 0f, 0f,
        0.00f, 0.00f, 0.00f, 0f, 0f,
        0f,    0f,    0f,    1f, 0f
    )

    // ── Debug: primary-color channel isolation ──────────────────────────
    val redOnlyMatrixArray = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,   // R' = R
        0f, 0f, 0f, 0f, 0f,   // G' = 0
        0f, 0f, 0f, 0f, 0f,   // B' = 0
        0f, 0f, 0f, 1f, 0f    // A' = A
    )

    val greenOnlyMatrixArray = floatArrayOf(
        0f, 0f, 0f, 0f, 0f,   // R' = 0
        0f, 1f, 0f, 0f, 0f,   // G' = G
        0f, 0f, 0f, 0f, 0f,   // B' = 0
        0f, 0f, 0f, 1f, 0f    // A' = A
    )

    val blueOnlyMatrixArray = floatArrayOf(
        0f, 0f, 0f, 0f, 0f,   // R' = 0
        0f, 0f, 0f, 0f, 0f,   // G' = 0
        0f, 0f, 1f, 0f, 0f,   // B' = B
        0f, 0f, 0f, 1f, 0f    // A' = A
    )

    /**
     * Returns the 4×5 [ColorMatrix] array for the given [VisionColorFilter.VisionMode].
     * Returns `null` for [VisionColorFilter.VisionMode.HUMAN] (identity / no transform).
     */
    fun getColorMatrix(mode: VisionColorFilter.VisionMode): ColorMatrix? {
        val matrixArray = when (mode) {
            VisionColorFilter.VisionMode.HUMAN -> return null
            VisionColorFilter.VisionMode.DOG -> dogMatrixArray
            VisionColorFilter.VisionMode.CAT -> catMatrixArray
            VisionColorFilter.VisionMode.BIRD -> birdMatrixArray
            VisionColorFilter.VisionMode.EAGLE -> eagleMatrixArray
            VisionColorFilter.VisionMode.HORSE -> horseMatrixArray
            VisionColorFilter.VisionMode.MANTIS_SHRIMP -> mantisShrimpMatrixArray
            VisionColorFilter.VisionMode.REINDEER -> reindeerMatrixArray
            VisionColorFilter.VisionMode.CUTTLEFISH -> cuttlefishMatrixArray
            VisionColorFilter.VisionMode.PIT_VIPER -> pitViperMatrixArray
            VisionColorFilter.VisionMode.RED_ONLY -> redOnlyMatrixArray
            VisionColorFilter.VisionMode.GREEN_ONLY -> greenOnlyMatrixArray
            VisionColorFilter.VisionMode.BLUE_ONLY -> blueOnlyMatrixArray
        }
        return ColorMatrix(matrixArray)
    }

    /**
     * Maps a [VisionColorFilter.VisionMode] to a [ColorMatrixColorFilter] suitable for
     * [android.widget.ImageView.setColorFilter] or [android.graphics.Paint.setColorFilter].
     *
     * Returns `null` for HUMAN mode (remove any existing filter to show original colors).
     *
     * Usage example:
     * ```
     * fun applyVisionMode(imageView: ImageView, mode: VisionColorFilter.VisionMode) {
     *     imageView.colorFilter = VisionMatrices.visionModeToColorFilter(mode)
     * }
     * ```
     */
    fun visionModeToColorFilter(mode: VisionColorFilter.VisionMode): ColorMatrixColorFilter? {
        val colorMatrix = getColorMatrix(mode) ?: return null
        return ColorMatrixColorFilter(colorMatrix)
    }
}
