package com.baltito.crittervision

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
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
    private lateinit var filterOverlay: View
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var activeFilterTextView: TextView

    private var currentFilter: VisionColorFilter.FilterType = VisionColorFilter.FilterType.ORIGINAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        activeFilterTextView = findViewById(R.id.activeFilterTextView)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Create overlay immediately
        setupFilterOverlay()

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

    private fun setupFilterOverlay() {
        // Create a simple full-screen overlay
        filterOverlay = View(this)
        filterOverlay.setBackgroundColor(Color.TRANSPARENT)
        filterOverlay.visibility = View.GONE
        filterOverlay.isClickable = false // Allow clicks to pass through to buttons
        filterOverlay.isFocusable = false
        filterOverlay.elevation = 1f // Ensure filterOverlay is above PreviewView's surface

        // Add to the camera_container FrameLayout
        val cameraContainer = findViewById<FrameLayout>(R.id.camera_container)
        cameraContainer.addView(filterOverlay, android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        ))

        Log.d(TAG, "FilterOverlay added to cameraContainer. cameraContainer child count: ${cameraContainer.childCount}")
        Log.d(TAG, "FilterOverlay Z: ${filterOverlay.z}, Elevation: ${filterOverlay.elevation}")
        Log.d(TAG, "PreviewView Z: ${previewView.z}, Elevation: ${previewView.elevation}")
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
        Log.d(TAG, "Updating filter. Overlay parent: ${filterOverlay.parent}, Visibility: ${filterOverlay.visibility}, Alpha: ${filterOverlay.alpha}, Background: ${filterOverlay.background}")
        Log.d(TAG, "Applying filter: $currentFilter")

        when (currentFilter) {
            VisionColorFilter.FilterType.DOG -> {
                // Dog vision: Strong yellow overlay
                filterOverlay.setBackgroundColor(Color.argb(150, 255, 255, 0)) // Stronger yellow
                filterOverlay.visibility = View.VISIBLE
                Log.d(TAG, "DOG filter: Overlay visibility: ${filterOverlay.visibility}, Color set.")
                Toast.makeText(this, "🐕 Dog Vision", Toast.LENGTH_SHORT).show()
            }
            VisionColorFilter.FilterType.CAT -> {
                // Cat vision: Strong blue-gray overlay
                filterOverlay.setBackgroundColor(Color.argb(130, 100, 150, 200)) // Stronger blue-gray
                filterOverlay.visibility = View.VISIBLE
                Log.d(TAG, "CAT filter: Overlay visibility: ${filterOverlay.visibility}, Color set.")
                Toast.makeText(this, "🐱 Cat Vision", Toast.LENGTH_SHORT).show()
            }
            VisionColorFilter.FilterType.BIRD -> {
                // Bird vision: Strong purple overlay
                filterOverlay.setBackgroundColor(Color.argb(110, 200, 100, 255)) // Stronger purple
                filterOverlay.visibility = View.VISIBLE
                Log.d(TAG, "BIRD filter: Overlay visibility: ${filterOverlay.visibility}, Color set.")
                Toast.makeText(this, "🦅 Bird Vision", Toast.LENGTH_SHORT).show()
            }
            VisionColorFilter.FilterType.ORIGINAL -> {
                // Original vision: No overlay
                filterOverlay.visibility = View.GONE
                Log.d(TAG, "ORIGINAL filter: Overlay visibility: ${filterOverlay.visibility}")
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
        cameraExecutor.shutdown()
    }
}