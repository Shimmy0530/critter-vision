package com.example.crittervision

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

        // Try to find an existing overlay view in the layout, or create one
        setupFilterOverlay()

        // Setup buttons
        val dogVisionButton: Button = findViewById(R.id.dogVisionButton)
        val catVisionButton: Button = findViewById(R.id.catVisionButton)
        val birdVisionButton: Button = findViewById(R.id.birdVisionButton)
        val originalVisionButton: Button = findViewById(R.id.originalVisionButton)

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
        // Create a simple view that acts as our color overlay
        filterOverlay = View(this)
        filterOverlay.setBackgroundColor(Color.TRANSPARENT)
        filterOverlay.visibility = View.GONE

        // Add it to the content view as an overlay
        val contentView = findViewById<android.view.ViewGroup>(android.R.id.content)
        val layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
        contentView.addView(filterOverlay, layoutParams)

        Log.d(TAG, "Filter overlay created and added to content view")
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
        Log.d(TAG, "Applying filter: $currentFilter")

        when (currentFilter) {
            VisionColorFilter.FilterType.DOG -> {
                // Dog vision: Yellow overlay to simulate protanopia (red-green color blindness)
                filterOverlay.setBackgroundColor(Color.argb(120, 255, 255, 0)) // Semi-transparent yellow
                filterOverlay.visibility = View.VISIBLE
                Log.d(TAG, "Dog filter applied - yellow overlay visible")
                Toast.makeText(this, "🐕 Dog Vision Active: Yellow-dominant world", Toast.LENGTH_LONG).show()
            }
            VisionColorFilter.FilterType.CAT -> {
                // Cat vision: Blue-gray overlay for muted colors
                filterOverlay.setBackgroundColor(Color.argb(100, 100, 150, 200)) // Semi-transparent blue-gray
                filterOverlay.visibility = View.VISIBLE
                Log.d(TAG, "Cat filter applied - blue-gray overlay visible")
                Toast.makeText(this, "🐱 Cat Vision Active: Muted color perception", Toast.LENGTH_LONG).show()
            }
            VisionColorFilter.FilterType.BIRD -> {
                // Bird vision: Light purple overlay to simulate UV perception
                filterOverlay.setBackgroundColor(Color.argb(80, 200, 100, 255)) // Semi-transparent purple
                filterOverlay.visibility = View.VISIBLE
                Log.d(TAG, "Bird filter applied - purple overlay visible")
                Toast.makeText(this, "🦅 Bird Vision Active: UV-enhanced perception", Toast.LENGTH_LONG).show()
            }
            VisionColorFilter.FilterType.ORIGINAL -> {
                // Original vision: No overlay
                filterOverlay.visibility = View.GONE
                Log.d(TAG, "Original filter applied - overlay hidden")
                Toast.makeText(this, "👁️ Human Vision: Normal color perception", Toast.LENGTH_LONG).show()
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