package com.baltito.crittervision

import android.util.Log
import androidx.camera.core.CameraEffect
import java.util.concurrent.Executors

/**
 * CameraX CameraEffect that applies animal vision color transformations
 * to the camera preview using GPU-based OpenGL ES fragment shaders.
 *
 * Wraps [AnimalVisionProcessor] as a [CameraEffect] targeting PREVIEW.
 */
class AnimalVisionEffect(
    val processor: AnimalVisionProcessor
) : CameraEffect(
    PREVIEW,
    Executors.newSingleThreadExecutor(),
    processor,
    { throwable -> Log.e("AnimalVisionEffect", "Effect error", throwable) }
)
