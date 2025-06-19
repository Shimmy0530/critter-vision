package com.baltito.crittervision

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
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

    private var currentFilter: VisionColorFilter.FilterType = VisionColorFilter.FilterType.ORIGINAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        activeFilterTextView = findViewById(R.id.activeFilterTextView)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Setup buttons
        val dogVisionButton: ImageButton = findViewById(R.id.dogVisionImageButton)
        val catVisionButton: ImageButton = findViewById(R.id.catVisionImageButton)
        val birdVisionButton: ImageButton = findViewById(R.id.birdVisionImageButton)
        val originalVisionButton: ImageButton = findViewById(R.id.originalVisionImageButton)

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

                preview.setSurfaceProvider(previewView.surfaceProvider)
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)

                Log.d(TAG, "Camera started successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
                Toast.makeText(applicationContext, "Error starting camera: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun updatePreviewFilter() {
        Log.d(TAG, "Applying realistic filter: $currentFilter")

        // Apply the color matrix filter directly to the PreviewView
        val colorFilter = VisionColorFilter.getFilter(currentFilter)

        runOnUiThread {
            // Apply the filter to the PreviewView's surface
            if (colorFilter != null) {
                previewView.foreground = android.graphics.drawable.ColorDrawable().apply {
                    this.colorFilter = colorFilter
                }
            } else {
                previewView.foreground = null
            }
        }

        when (currentFilter) {
            VisionColorFilter.FilterType.DOG -> {
                Toast.makeText(this, "🐕 Dog Vision (Protanopia)", Toast.LENGTH_SHORT).show()
            }
            VisionColorFilter.FilterType.CAT -> {
                Toast.makeText(this, "🐱 Cat Vision (Deuteranopia-like)", Toast.LENGTH_SHORT).show()
            }
            VisionColorFilter.FilterType.BIRD -> {
                Toast.makeText(this, "🦅 Bird Vision (Tetrachromatic + UV)", Toast.LENGTH_SHORT).show()
            }
            VisionColorFilter.FilterType.ORIGINAL -> {
                Toast.makeText(this, "👁️ Human Vision", Toast.LENGTH_SHORT).show()
            }
            VisionColorFilter.FilterType.GRAYSCALE -> {
                Toast.makeText(this, "⚫ Grayscale Vision", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateActiveFilterTextView() {
        val filterName = when (currentFilter) {
            VisionColorFilter.FilterType.DOG -> "🐕 Dog Vision"
            VisionColorFilter.FilterType.CAT -> "🐱 Cat Vision"
            VisionColorFilter.FilterType.BIRD -> "🦅 Bird Vision"
            VisionColorFilter.FilterType.GRAYSCALE -> "⚫ Grayscale Vision"
            VisionColorFilter.FilterType.ORIGINAL -> "👁️ Human Vision"
        }
        activeFilterTextView.text = "Current View: $filterName"
        Log.d(TAG, "Updated filter text to: $filterName")
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}