package com.example.crittervision;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;

// Note: These color matrices are initial approximations for simulating animal vision.
// They may require further tuning based on visual testing to achieve the desired perceptual effect.
public class VisionColorFilter {

    public enum FilterType {
        ORIGINAL,
        DOG,
        CAT,
        BIRD
    }

    /**
     * Simulates Dog Vision (Protanomaly-like: reduced red sensitivity).
     * Reds appear darker and desaturated; greens shift towards yellow/brown.
     * Dogs primarily see in blues and yellows.
     */
    public static ColorMatrixColorFilter getDogVisionFilter() {
        // Protanomaly matrix (common approximation for red-green color blindness)
        float[] dogMatrix = {
            0.56667f, 0.43333f, 0.0f,     0.0f, 0.0f, // R channel output
            0.55833f, 0.44167f, 0.0f,     0.0f, 0.0f, // G channel output
            0.0f,     0.24167f, 0.75833f, 0.0f, 0.0f, // B channel output
            0.0f,     0.0f,     0.0f,     1.0f, 0.0f  // A channel output
        };
        ColorMatrix cm = new ColorMatrix(dogMatrix);
        return new ColorMatrixColorFilter(cm);
    }

    /**
     * Simulates Cat Vision (Deuteranomaly-like: reduced green sensitivity).
     * Greens shift towards beige/yellow; reds appear yellowish-brown.
     * Cats also see blues and yellows, similar to dogs but with different nuances.
     * Adds a slight brightness enhancement.
     */
    public static ColorMatrixColorFilter getCatVisionFilter() {
        // Deuteranomaly matrix (common approximation for green-weak color blindness)
        float[] catMatrix = {
            0.625f, 0.375f, 0.0f,   0.0f, 0.0f,
            0.700f, 0.300f, 0.0f,   0.0f, 0.0f, // This matrix is often cited for Tritanopia actually.
                                               // Let's use a matrix that makes greens more beige/yellow
                                               // and reds more yellowish/brown, blues are preserved.
            0.0f,   0.300f, 0.700f, 0.0f, 0.0f, // This does not achieve cat vision accurately.
            0.0f,   0.0f,   0.0f,   1.0f, 0.0f
        };

        // A better approximation for cat vision (dichromatic, blue and greenish-yellow)
        // This matrix attempts to make reds and some greens appear as shades of yellow/gray,
        // while blues and some greens are more vivid.
        float[] catVisionMatrix = {
            0.40f, 0.40f, 0.20f, 0.0f, 0.0f, // R: Mix to desaturated yellow/gray
            0.25f, 0.50f, 0.25f, 0.0f, 0.0f, // G: Shift towards greenish-yellow/gray
            0.15f, 0.30f, 0.55f, 0.0f, 0.0f, // B: Keep blue tones, slightly muted by other channels
            0.0f,  0.0f,  0.0f,  1.0f, 0.0f
        };
        // Enhance blue and green, desaturate red
        // Values from research on cat vision suggest sensitivity peaks around blue-violet and greenish-yellow.
         float[] catMatrixAdjusted = {
            0.15f, 0.70f, 0.15f, 0, 0, // Red channel output (make reds appear dim/grayish or brownish-yellow)
            0.15f, 0.70f, 0.15f, 0, 0, // Green channel output (greens are perceived, perhaps more yellowish)
            0.70f, 0.15f, 0.15f, 0, 0, // Blue channel output (blues are well perceived) - this makes blue reddish
            0, 0, 0, 1, 0
        };
        // Let's use a standard deuteranopia matrix and slightly boost brightness/contrast.
        // Deuteranopia: greens look more like beige/yellow, reds look yellowish brown.
        float[] deutanMatrix = {
            0.3602f, 0.8636f, -0.2238f, 0.0f, 0.0f,
            0.2610f, 0.6021f,  0.1369f, 0.0f, 0.0f,
           -0.0580f, 0.0922f,  0.9658f, 0.0f, 0.0f,
            0.0f,    0.0f,     0.0f,    1.0f, 0.0f
        };


        ColorMatrix cm = new ColorMatrix(deutanMatrix);

        // Slight brightness/contrast increase for night vision simulation aspect
        ColorMatrix brightnessContrastMatrix = new ColorMatrix();
        brightnessContrastMatrix.setScale(1.1f, 1.1f, 1.1f, 1.0f); // Scale RGB, alpha unchanged
        // You could also adjust contrast using pre/postOffset, but scale is simpler for brightness.
        // float contrast = 1.2f; // 0-1 for less, >1 for more
        // float brightness = 10; // -255 to 255
        // ColorMatrix contrastMatrix = new ColorMatrix(new float[] {
        //     contrast, 0, 0, 0, brightness,
        //     0, contrast, 0, 0, brightness,
        //     0, 0, contrast, 0, brightness,
        //     0, 0, 0, 1, 0
        // });
        // cm.postConcat(contrastMatrix);
        cm.postConcat(brightnessContrastMatrix);

        return new ColorMatrixColorFilter(cm);
    }

    /**
     * Simulates Bird Vision (enhanced color vibrancy, UV hint).
     * Increases saturation and adds a subtle violet/blue shift.
     */
    public static ColorMatrixColorFilter getBirdVisionFilter() {
        ColorMatrix birdSaturationMatrix = new ColorMatrix();
        birdSaturationMatrix.setSaturation(1.8f); // Significantly increase saturation

        // Add a hint of violet/UV by slightly boosting blue and adding a small amount from red.
        ColorMatrix birdUvHintMatrix = new ColorMatrix(new float[]{
            1.0f, 0.0f, 0.1f, 0.0f, 0.0f,  // R channel (slight R into B for violet hint)
            0.0f, 1.0f, 0.0f, 0.0f, 0.0f,  // G channel
            0.1f, 0.1f, 1.0f, 0.0f, 5.0f, // B channel (boost B, add bit of R/G, slight blue offset)
            0.0f, 0.0f, 0.0f, 1.0f, 0.0f   // A channel
        });

        birdSaturationMatrix.postConcat(birdUvHintMatrix);
        return new ColorMatrixColorFilter(birdSaturationMatrix);
    }

    public static ColorMatrixColorFilter getFilter(FilterType type) {
        if (type == null) {
            return null;
        }
        switch (type) {
            case DOG:
                return getDogVisionFilter();
            case CAT:
                return getCatVisionFilter();
            case BIRD:
                return getBirdVisionFilter();
            case ORIGINAL:
            default:
                return null; // No filter for original
        }
    }
}
