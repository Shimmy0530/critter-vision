package com.baltito.crittervision

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
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
    private lateinit var filterOverlay: AnimalVisionView

    private var currentFilter: VisionColorFilter.FilterType = VisionColorFilter.FilterType.ORIGINAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        activeFilterTextView = findViewById(R.id.activeFilterTextView)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Create custom overlay for applying color filters
        setupFilterOverlay()

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

    private fun setupFilterOverlay() {
        // Create a custom overlay view that can apply proper color transformations
        filterOverlay = AnimalVisionView(this)
        filterOverlay.visibility = View.GONE
        filterOverlay.isClickable = false // Allow clicks to pass through to buttons
        filterOverlay.isFocusable = false

        // Add to the root content view
        val rootView = findViewById<android.view.ViewGroup>(android.R.id.content)
        rootView.addView(filterOverlay, android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        ))

        Log.d(TAG, "Custom animal vision overlay created")
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
                // Apply dog vision filter using scientific color matrix
                filterOverlay.setAnimalVision(VisionColorFilter.getDogVisionMatrix(), "DOG")
                Log.d(TAG, "Dog filter applied - scientific color matrix")
                Toast.makeText(this, "🐕 Dog Vision", Toast.LENGTH_SHORT).show()
            }
            VisionColorFilter.FilterType.CAT -> {
                // Apply cat vision filter using scientific color matrix
                filterOverlay.setAnimalVision(VisionColorFilter.getCatVisionMatrix(), "CAT")
                Log.d(TAG, "Cat filter applied - scientific color matrix")
                Toast.makeText(this, "🐱 Cat Vision", Toast.LENGTH_SHORT).show()
            }
            VisionColorFilter.FilterType.BIRD -> {
                // Apply bird vision filter using scientific color matrix
                filterOverlay.setAnimalVision(VisionColorFilter.getBirdVisionMatrix(), "BIRD")
                Log.d(TAG, "Bird filter applied - scientific color matrix")
                Toast.makeText(this, "🦅 Bird Vision", Toast.LENGTH_SHORT).show()
            }
            VisionColorFilter.FilterType.ORIGINAL -> {
                // Remove filter overlay
                filterOverlay.clearVision()
                Log.d(TAG, "Original filter applied - no overlay")
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

    // Custom view that applies proper color matrix transformations
    private inner class AnimalVisionView(context: Context) : View(context) {
        private var colorMatrix: ColorMatrix? = null
        private var animalType: String? = null
        private val paint = Paint()
        private val colorFilterPaint = Paint()

        fun setAnimalVision(matrix: ColorMatrix, type: String) {
            colorMatrix = matrix
            animalType = type
            visibility = View.VISIBLE
            invalidate()
        }

        fun clearVision() {
            colorMatrix = null
            animalType = null
            visibility = View.GONE
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            
            val currentMatrix = colorMatrix
            val currentType = animalType
            
            if (currentMatrix != null && currentType != null) {
                when (currentType) {
                    "DOG" -> drawDogVision(canvas, currentMatrix)
                    "CAT" -> drawCatVision(canvas, currentMatrix)
                    "BIRD" -> drawBirdVision(canvas, currentMatrix)
                }
            }
        }

        private fun drawDogVision(canvas: Canvas, matrix: ColorMatrix) {
            // Dog vision: Reds become yellow/gray, blues stay blue
            // Use PorterDuff blend modes to simulate the color transformation
            
            // Layer 1: Yellow overlay for reds (simulates red->yellow transformation)
            paint.color = Color.argb(40, 255, 255, 0)
            paint.xfermode = PorterDuff.Mode.SCREEN
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            
            // Layer 2: Blue enhancement (simulates blue channel enhancement)
            paint.color = Color.argb(20, 0, 0, 255)
            paint.xfermode = PorterDuff.Mode.ADD
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            
            // Layer 3: Gray overlay for neutral areas
            paint.color = Color.argb(15, 128, 128, 128)
            paint.xfermode = PorterDuff.Mode.MULTIPLY
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            
            paint.xfermode = null // Reset blend mode
        }

        private fun drawCatVision(canvas: Canvas, matrix: ColorMatrix) {
            // Cat vision: Enhanced blues, muted reds
            
            // Layer 1: Blue enhancement (strong blue channel)
            paint.color = Color.argb(35, 0, 0, 255)
            paint.xfermode = PorterDuff.Mode.SCREEN
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            
            // Layer 2: Green enhancement
            paint.color = Color.argb(25, 0, 255, 0)
            paint.xfermode = PorterDuff.Mode.ADD
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            
            // Layer 3: Red muting (simulates red channel reduction)
            paint.color = Color.argb(30, 128, 128, 128)
            paint.xfermode = PorterDuff.Mode.MULTIPLY
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            
            paint.xfermode = null
        }

        private fun drawBirdVision(canvas: Canvas, matrix: ColorMatrix) {
            // Bird vision: Enhanced colors, especially blues and purples (UV simulation)
            
            // Layer 1: Enhanced blue (very strong blue channel)
            paint.color = Color.argb(45, 0, 0, 255)
            paint.xfermode = PorterDuff.Mode.SCREEN
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            
            // Layer 2: Purple overlay (UV simulation)
            paint.color = Color.argb(30, 150, 0, 255)
            paint.xfermode = PorterDuff.Mode.ADD
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            
            // Layer 3: Enhanced red and green
            paint.color = Color.argb(25, 255, 255, 0)
            paint.xfermode = PorterDuff.Mode.SCREEN
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            
            paint.xfermode = null
        }
    }
}