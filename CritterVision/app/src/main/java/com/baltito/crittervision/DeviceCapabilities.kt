package com.baltito.crittervision

import android.app.ActivityManager
import android.content.Context
import android.util.Log

/**
 * Utility class for detecting device capabilities and recommending optimal processing modes
 * for animal vision simulation based on hardware specifications
 */
object DeviceCapabilities {
    
    private const val TAG = "DeviceCapabilities"
    
    /**
     * Check if device supports advanced OpenGL ES filtering
     * Requires OpenGL ES 2.0 or higher for GPU-accelerated processing
     */
    fun supportsAdvancedFiltering(context: Context): Boolean {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val configInfo = activityManager.deviceConfigurationInfo
            val supportsGLES20 = configInfo.reqGlEsVersion >= 0x20000 // OpenGL ES 2.0+
            
            Log.d(TAG, "Device OpenGL ES version: ${Integer.toHexString(configInfo.reqGlEsVersion)}")
            Log.d(TAG, "Supports advanced filtering: $supportsGLES20")
            
            supportsGLES20
        } catch (e: Exception) {
            Log.e(TAG, "Error checking OpenGL ES support", e)
            false
        }
    }
    
    /**
     * Get recommended processing mode based on device capabilities
     * Advanced mode provides better performance and more accurate simulation
     */
    fun getRecommendedMode(context: Context): ProcessingMode {
        val supportsAdvanced = supportsAdvancedFiltering(context)
        val recommendedMode = if (supportsAdvanced) {
            ProcessingMode.ADVANCED
        } else {
            ProcessingMode.SIMPLE
        }
        
        Log.d(TAG, "Recommended processing mode: $recommendedMode")
        return recommendedMode
    }
    
    /**
     * Get device information for debugging and optimization
     */
    fun getDeviceInfo(context: Context): DeviceInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val configInfo = activityManager.deviceConfigurationInfo
        
        return DeviceInfo(
            glEsVersion = configInfo.reqGlEsVersion,
            glEsVersionString = configInfo.glEsVersion ?: "Unknown",
            supportsAdvancedFiltering = supportsAdvancedFiltering(context),
            recommendedMode = getRecommendedMode(context)
        )
    }
}

/**
 * Processing modes for animal vision simulation
 */
enum class ProcessingMode {
    /**
     * Simple ColorMatrix-based filtering
     * - Compatible with all devices
     * - Lower CPU overhead
     * - Basic color transformation
     */
    SIMPLE,
    
    /**
     * Advanced OpenGL shader-based filtering
     * - Requires OpenGL ES 2.0+
     * - GPU acceleration
     * - Pixel-level control and spectral simulation
     */
    ADVANCED
}

/**
 * Device capability information for debugging and optimization
 */
data class DeviceInfo(
    val glEsVersion: Int,
    val glEsVersionString: String,
    val supportsAdvancedFiltering: Boolean,
    val recommendedMode: ProcessingMode
) {
    override fun toString(): String {
        return "DeviceInfo(glEsVersion=${Integer.toHexString(glEsVersion)}, " +
                "glEsVersionString='$glEsVersionString', " +
                "supportsAdvancedFiltering=$supportsAdvancedFiltering, " +
                "recommendedMode=$recommendedMode)"
    }
}
