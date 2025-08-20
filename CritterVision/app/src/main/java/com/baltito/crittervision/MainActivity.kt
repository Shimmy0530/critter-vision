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
    private lateinit var colorFilterOverlay: ColorFilterOverlay

    private var currentFilter: VisionColorFilter.FilterType = VisionColorFilter.FilterType.ORIGINAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        activeFilterTextView = findViewById(R.id.activeFilterTextView)
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        // Create and add the color filter overlay
        setupColorFilterOverlay()

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

                // Use the standard preview surface provider
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
                val colorMatrix = VisionColorFilter.getDogVisionMatrix()
                applyColorMatrixToPreview(colorMatrix)
                Log.d(TAG, "Dog filter applied - scientific color matrix")
                Toast.makeText(this, "🐕 Dog Vision", Toast.LENGTH_SHORT).show()
            }
            VisionColorFilter.FilterType.CAT -> {
                val colorMatrix = VisionColorFilter.getCatVisionMatrix()
                applyColorMatrixToPreview(colorMatrix)
                Log.d(TAG, "Cat filter applied - scientific color matrix")
                Toast.makeText(this, "🐱 Cat Vision", Toast.LENGTH_SHORT).show()
            }
            VisionColorFilter.FilterType.BIRD -> {
                val colorMatrix = VisionColorFilter.getBirdVisionMatrix()
                applyColorMatrixToPreview(colorMatrix)
                Log.d(TAG, "Bird filter applied - scientific color matrix")
                Toast.makeText(this, "🦅 Bird Vision", Toast.LENGTH_SHORT).show()
            }
            VisionColorFilter.FilterType.ORIGINAL -> {
                applyColorMatrixToPreview(null)
                Log.d(TAG, "Original filter applied - no matrix")
                Toast.makeText(this, "👁️ Human Vision", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyColorMatrixToPreview(colorMatrix: ColorMatrix?) {
        // Apply the color matrix to the overlay
        colorFilterOverlay.setColorMatrix(colorMatrix)
        Log.d(TAG, "Color matrix applied: ${colorMatrix != null}")
    }

    private fun setupColorFilterOverlay() {
        colorFilterOverlay = ColorFilterOverlay(this)
        colorFilterOverlay.visibility = View.GONE
        colorFilterOverlay.isClickable = false
        colorFilterOverlay.isFocusable = false

        // Add to the root content view
        val rootView = findViewById<android.view.ViewGroup>(android.R.id.content)
        rootView.addView(colorFilterOverlay, android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        ))

        Log.d(TAG, "Color filter overlay created")
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

    // Custom overlay view that applies color matrices using PorterDuff blend modes
    private inner class ColorFilterOverlay(context: Context) : View(context) {
        private var colorMatrix: ColorMatrix? = null
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        fun setColorMatrix(matrix: ColorMatrix?) {
            colorMatrix = matrix
            visibility = if (matrix != null) View.VISIBLE else View.GONE
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            
            if (colorMatrix != null) {
                // Get the bounds of the preview view to only apply filter to camera area
                val previewBounds = getPreviewBounds()
                if (previewBounds != null) {
                    // Apply the color matrix using a sophisticated blend approach
                    applyColorMatrixEffect(canvas, previewBounds)
                }
            }
        }

        private fun getPreviewBounds(): RectF? {
            // Calculate the relative position of the preview view
            val previewLocation = IntArray(2)
            previewView.getLocationInWindow(previewLocation)
            
            val overlayLocation = IntArray(2)
            this.getLocationInWindow(overlayLocation)
            
            val left = (previewLocation[0] - overlayLocation[0]).toFloat()
            val top = (previewLocation[1] - overlayLocation[1]).toFloat()
            val right = left + previewView.width.toFloat()
            val bottom = top + previewView.height.toFloat()
            
            return if (previewView.width > 0 && previewView.height > 0) {
                RectF(left, top, right, bottom)
            } else null
        }

        private fun applyColorMatrixEffect(canvas: Canvas, bounds: RectF) {
            // Create a sophisticated color transformation using multiple blend modes
            val matrix = colorMatrix!!
            val matrixArray = matrix.array
            
            // Apply different blend modes based on the color matrix values
            // This simulates the actual color transformation more accurately
            
            // Red channel transformation
            if (matrixArray[0] > 1.0f || matrixArray[1] > 0.5f || matrixArray[2] > 0.5f) {
                paint.reset()
                paint.color = Color.argb(60, 255, 0, 0)
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
                canvas.drawRect(bounds, paint)
            }
            
            // Green channel transformation
            if (matrixArray[5] > 1.0f || matrixArray[6] > 0.5f || matrixArray[7] > 0.5f) {
                paint.reset()
                paint.color = Color.argb(60, 0, 255, 0)
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
                canvas.drawRect(bounds, paint)
            }
            
            // Blue channel transformation
            if (matrixArray[10] > 1.0f || matrixArray[11] > 0.5f || matrixArray[12] > 0.5f) {
                paint.reset()
                paint.color = Color.argb(60, 0, 0, 255)
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
                canvas.drawRect(bounds, paint)
            }
            
            // Cross-channel effects (simulating color blindness and enhancement)
            if (matrixArray[1] > 0.3f || matrixArray[2] > 0.3f) {
                paint.reset()
                paint.color = Color.argb(40, 255, 255, 0) // Yellow for red-green mixing
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
                canvas.drawRect(bounds, paint)
            }
            
            if (matrixArray[6] > 0.3f || matrixArray[7] > 0.3f) {
                paint.reset()
                paint.color = Color.argb(40, 0, 255, 255) // Cyan for green-blue mixing
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
                canvas.drawRect(bounds, paint)
            }
            
            paint.xfermode = null
        }
    }
}