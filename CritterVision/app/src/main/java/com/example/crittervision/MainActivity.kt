package com.example.crittervision

import android.Manifest // For REQUIRED_PERMISSIONS
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter // For updatePreviewFilter
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
// import androidx.annotation.NonNull // Not strictly needed for overridden Kotlin methods unless specific interop demands
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    // Companion object for constants
    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var activeFilterTextView: TextView

    private var currentFilter: VisionColorFilter.FilterType = VisionColorFilter.FilterType.ORIGINAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        activeFilterTextView = findViewById(R.id.activeFilterTextView)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Setup buttons
        val dogVisionButton: Button = findViewById(R.id.dogVisionButton)
        val catVisionButton: Button = findViewById(R.id.catVisionButton)
        val birdVisionButton: Button = findViewById(R.id.birdVisionButton)
        val originalVisionButton: Button = findViewById(R.id.originalVisionButton)

        updateActiveFilterTextView() // Initial text

        dogVisionButton.setOnClickListener {
            currentFilter = VisionColorFilter.FilterType.DOG
            updatePreviewFilter()
            updateActiveFilterTextView()
        }
        catVisionButton.setOnClickListener {
            currentFilter = VisionColorFilter.FilterType.CAT
            updatePreviewFilter()
            updateActiveFilterTextView()
        }
        birdVisionButton.setOnClickListener {
            currentFilter = VisionColorFilter.FilterType.BIRD
            updatePreviewFilter()
            updateActiveFilterTextView()
        }
        originalVisionButton.setOnClickListener {
            currentFilter = VisionColorFilter.FilterType.ORIGINAL
            updatePreviewFilter()
            updateActiveFilterTextView()
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun allPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                // Consider disabling camera-dependent features or closing the app
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider) // Corrected: previewView.surfaceProvider
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll() // Unbind use cases before rebinding
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)

            } catch (e: ExecutionException) {
                Log.e(TAG, "Use case binding failed", e)
                Toast.makeText(applicationContext, "Error starting camera: ${e.message}", Toast.LENGTH_LONG).show()
            } catch (e: InterruptedException) {
                Log.e(TAG, "Use case binding failed", e)
                Toast.makeText(applicationContext, "Error starting camera: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun updatePreviewFilter() {
        val filter: ColorMatrixColorFilter? = VisionColorFilter.getFilter(currentFilter)
        // Performance Note: Applying ColorFilter to PreviewView's foreground can be inefficient for real-time camera streams.
        // If lag is observed, consider using CameraX Effects API (Preview.setEffect with a SurfaceProcessor from androidx.camera:camera-effects)
        // or an ImageAnalysis use case to process frames and display them on a separate ImageView for better performance.
        Log.d(TAG, "Attempting to apply filter: $currentFilter")

        // Ensure foreground exists for API 23+
        if (previewView.foreground == null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            previewView.foreground = ColorDrawable(Color.TRANSPARENT)
        }
        val foreground: Drawable? = previewView.foreground

        if (foreground != null) {
            if (filter == null) {
                // Performance Note: Applying ColorFilter to PreviewView's foreground can be inefficient for real-time camera streams.
                // If lag is observed, consider using CameraX Effects API (Preview.setEffect with a SurfaceProcessor from androidx.camera:camera-effects)
                // or an ImageAnalysis use case to process frames and display them on a separate ImageView for better performance.
                foreground.mutate().clearColorFilter()
                Log.d(TAG, "Clearing filter from PreviewView")
            } else {
                // Performance Note: Applying ColorFilter to PreviewView's foreground can be inefficient for real-time camera streams.
                // If lag is observed, consider using CameraX Effects API (Preview.setEffect with a SurfaceProcessor from androidx.camera:camera-effects)
                // or an ImageAnalysis use case to process frames and display them on a separate ImageView for better performance.
                foreground.mutate().colorFilter = filter // Kotlin property access
                Log.d(TAG, "Applying new filter to PreviewView")
            }
            previewView.invalidate() // Request redraw
        } else {
            Log.w(TAG, "PreviewView foreground is null, cannot apply filter.")
        }
    }

    private fun updateActiveFilterTextView() {
        val filterName = when (currentFilter) {
            VisionColorFilter.FilterType.DOG -> "Dog Vision"
            VisionColorFilter.FilterType.CAT -> "Cat Vision"
            VisionColorFilter.FilterType.BIRD -> "Bird Vision"
            VisionColorFilter.FilterType.ORIGINAL -> "Original Vision"
            // No 'else' needed if 'when' is exhaustive over an enum
        }
        activeFilterTextView.text = "Current View: $filterName" // String template
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
