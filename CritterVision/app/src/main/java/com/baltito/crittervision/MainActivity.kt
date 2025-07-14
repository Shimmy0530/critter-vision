package com.baltito.crittervision

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var activeFilterTextView: TextView
    private lateinit var colorFilterProcessor: ColorFilterSurfaceProcessor

    private var currentFilter: VisionColorFilter.FilterType = VisionColorFilter.FilterType.ORIGINAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        activeFilterTextView = findViewById(R.id.activeFilterTextView)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Initialize the color filter processor
        colorFilterProcessor = ColorFilterSurfaceProcessor(cameraExecutor)

        // Setup buttons
        val dogVisionButton: Button = findViewById(R.id.dogVisionButton)
        val catVisionButton: Button = findViewById(R.id.catVisionButton)
        val birdVisionButton: Button = findViewById(R.id.birdVisionButton)
        val originalVisionButton: Button = findViewById(R.id.originalVisionButton)

        // Make buttons more prominent so they're visible through filters
        makeButtonsProminent(dogVisionButton, catVisionButton, birdVisionButton, originalVisionButton)

        updateActiveFilterTextView()

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

    private fun makeButtonsProminent(vararg buttons: Button) {
        buttons.forEach { button ->
            // Make buttons stand out with white background and bold text
            button.setBackgroundColor(Color.WHITE)
            button.setTextColor(Color.BLACK)
            button.elevation = 8f
            button.alpha = 0.9f
        }

        // Make the text view more prominent too
        activeFilterTextView.setBackgroundColor(Color.argb(200, 255, 255, 255))
        activeFilterTextView.setTextColor(Color.BLACK)
        activeFilterTextView.setPadding(16, 8, 16, 8)
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
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()

                // Set up the preview with the color filter processor
                preview.setSurfaceProvider(previewView.surfaceProvider)
                
                // Connect the color filter processor to the preview
                preview.setSurfaceProcessor(colorFilterProcessor)
                
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)

                Log.d(TAG, "Camera started successfully with color filter processor")

            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
                Toast.makeText(applicationContext, "Error starting camera: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun updatePreviewFilter() {
        Log.d(TAG, "Applying filter: $currentFilter")

        // Get the appropriate color matrix for the selected filter
        val colorMatrix = VisionColorFilter.getMatrix(currentFilter)
        
        // Apply the filter to the processor
        colorFilterProcessor.setFilter(colorMatrix)

        when (currentFilter) {
            VisionColorFilter.FilterType.DOG -> {
                Log.d(TAG, "Dog filter applied - scientific color matrix")
                Toast.makeText(this, "🐕 Dog Vision", Toast.LENGTH_SHORT).show()
            }
            VisionColorFilter.FilterType.CAT -> {
                Log.d(TAG, "Cat filter applied - scientific color matrix")
                Toast.makeText(this, "🐱 Cat Vision", Toast.LENGTH_SHORT).show()
            }
            VisionColorFilter.FilterType.BIRD -> {
                Log.d(TAG, "Bird filter applied - scientific color matrix")
                Toast.makeText(this, "🦅 Bird Vision", Toast.LENGTH_SHORT).show()
            }
            VisionColorFilter.FilterType.ORIGINAL -> {
                Log.d(TAG, "Original filter applied - no color matrix")
                Toast.makeText(this, "👁️ Human Vision", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateActiveFilterTextView() {
        val filterName = when (currentFilter) {
            VisionColorFilter.FilterType.DOG -> "🐕 Dog Vision"
            VisionColorFilter.FilterType.CAT -> "🐱 Cat Vision"
            VisionColorFilter.FilterType.BIRD -> "🦅 Bird Vision"
            VisionColorFilter.FilterType.ORIGINAL -> "👁️ Human Vision"
        }
        activeFilterTextView.text = "Current View: $filterName"
        Log.d(TAG, "Updated filter text to: $filterName")
    }

    override fun onDestroy() {
        super.onDestroy()
        colorFilterProcessor.release()
        cameraExecutor.shutdown()
    }
}