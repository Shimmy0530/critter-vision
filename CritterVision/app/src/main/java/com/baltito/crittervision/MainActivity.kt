package com.baltito.crittervision

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import android.widget.ImageView
import java.nio.ByteBuffer
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

    private lateinit var processedImageView: ImageView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var activeFilterTextView: TextView
    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private var cachedColorFilter: ColorFilter? = null
    private var currentFilter: VisionColorFilter.FilterType = VisionColorFilter.FilterType.ORIGINAL
    private var useAdvancedFilters = false
    private var filterIntensity = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        processedImageView = findViewById(R.id.processedImageView)
        activeFilterTextView = findViewById(R.id.activeFilterTextView)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Setup buttons
        val dogVisionButton: Button = findViewById(R.id.dogVisionButton)
        val catVisionButton: Button = findViewById(R.id.catVisionButton)
        val birdVisionButton: Button = findViewById(R.id.birdVisionButton)
        val originalVisionButton: Button = findViewById(R.id.originalVisionButton)
        val redOnlyTestButton: Button = findViewById(R.id.redOnlyTestButton)
        val advancedFilterToggle: Button = findViewById(R.id.advancedFilterToggle)
        val filterIntensitySeekBar: SeekBar = findViewById(R.id.filterIntensitySeekBar)
        val intensityValueText: TextView = findViewById(R.id.intensityValueText)

        // Make buttons more prominent so they're visible through filters
        makeButtonsProminent(dogVisionButton, catVisionButton, birdVisionButton, originalVisionButton, redOnlyTestButton)

        // Initialize cached filter
        updateCachedColorFilter()
        updateActiveFilterTextView()

        // Setup filter buttons
        dogVisionButton.setOnClickListener {
            currentFilter = if (useAdvancedFilters) VisionColorFilter.FilterType.DOG_ADVANCED else VisionColorFilter.FilterType.DOG
            updatePreviewFilter()
            updateActiveFilterTextView()
        }
        catVisionButton.setOnClickListener {
            currentFilter = if (useAdvancedFilters) VisionColorFilter.FilterType.CAT_ADVANCED else VisionColorFilter.FilterType.CAT
            updatePreviewFilter()
            updateActiveFilterTextView()
        }
        birdVisionButton.setOnClickListener {
            currentFilter = if (useAdvancedFilters) VisionColorFilter.FilterType.BIRD_ADVANCED else VisionColorFilter.FilterType.BIRD
            updatePreviewFilter()
            updateActiveFilterTextView()
        }
        originalVisionButton.setOnClickListener {
            currentFilter = VisionColorFilter.FilterType.ORIGINAL
            updatePreviewFilter()
            updateActiveFilterTextView()
        }
        redOnlyTestButton.setOnClickListener {
            currentFilter = VisionColorFilter.FilterType.RED_ONLY_TEST
            updatePreviewFilter()
            updateActiveFilterTextView()
        }
        
        // Setup advanced filter toggle
        advancedFilterToggle.setOnClickListener { toggleAdvancedFilters() }
        
        // Setup filter intensity controls
        filterIntensitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    filterIntensity = progress / 100.0f
                    intensityValueText.text = "${progress}%"
                    updatePreviewFilter()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        updateUI()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun toggleAdvancedFilters() {
        useAdvancedFilters = !useAdvancedFilters
        val advancedFilterToggle = findViewById<Button>(R.id.advancedFilterToggle)
        val filterIntensityPanel = findViewById<View>(R.id.filterIntensityPanel)

        if (useAdvancedFilters) {
            advancedFilterToggle.text = "Advanced Filters"
            filterIntensityPanel.visibility = View.VISIBLE
            currentFilter = when (currentFilter) {
                VisionColorFilter.FilterType.DOG -> VisionColorFilter.FilterType.DOG_ADVANCED
                VisionColorFilter.FilterType.CAT -> VisionColorFilter.FilterType.CAT_ADVANCED
                VisionColorFilter.FilterType.BIRD -> VisionColorFilter.FilterType.BIRD_ADVANCED
                else -> currentFilter
            }
        } else {
            advancedFilterToggle.text = "Standard Filters"
            filterIntensityPanel.visibility = View.GONE
            currentFilter = when (currentFilter) {
                VisionColorFilter.FilterType.DOG_ADVANCED -> VisionColorFilter.FilterType.DOG
                VisionColorFilter.FilterType.CAT_ADVANCED -> VisionColorFilter.FilterType.CAT
                VisionColorFilter.FilterType.BIRD_ADVANCED -> VisionColorFilter.FilterType.BIRD
                else -> currentFilter
            }
        }

        updatePreviewFilter()
        updateActiveFilterTextView()
        Log.d(TAG, "Advanced filters: $useAdvancedFilters")
    }

    private fun updateUI() {
        updateActiveFilterTextView()
    }

    private fun makeButtonsProminent(vararg buttons: Button) {
        buttons.forEach { button ->
            button.setBackgroundColor(Color.WHITE)
            button.setTextColor(Color.BLACK)
            button.elevation = 8f
            button.alpha = 0.9f
        }

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

                // ImageAnalysis use case for processing frames
                imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, RgbImageAnalyzer())
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalyzer)

                Log.d(TAG, "Camera started successfully with ImageAnalysis")

            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
                Toast.makeText(applicationContext, "Error starting camera: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private inner class RgbImageAnalyzer : ImageAnalysis.Analyzer {
        private var rgbBitmap: Bitmap? = null
        private var pixelBuffer: ByteArray? = null

        override fun analyze(image: ImageProxy) {
            val width = image.width
            val height = image.height

            // Initialize or reuse bitmap
            if (rgbBitmap == null || rgbBitmap?.width != width || rgbBitmap?.height != height) {
                rgbBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            }

            val plane = image.planes[0]
            val buffer = plane.buffer
            val rowStride = plane.rowStride

            // If the buffer is packed (rowStride == width * 4), we can copy directly
            if (rowStride == width * 4) {
                buffer.rewind()
                rgbBitmap?.copyPixelsFromBuffer(buffer)
            } else {
                // Handle padding: copy row by row into a packed buffer
                val bufferSize = width * height * 4
                if (pixelBuffer == null || pixelBuffer?.size != bufferSize) {
                    pixelBuffer = ByteArray(bufferSize)
                }
                val tempBuffer = pixelBuffer!!

                buffer.rewind()
                val rowWidth = width * 4
                for (row in 0 until height) {
                    buffer.position(row * rowStride)
                    buffer.get(tempBuffer, row * rowWidth, rowWidth)
                }
                rgbBitmap?.copyPixelsFromBuffer(ByteBuffer.wrap(tempBuffer))
            }

            val rotationDegrees = image.imageInfo.rotationDegrees

            // Create a matrix for the rotation
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            val rotatedBitmap = Bitmap.createBitmap(rgbBitmap!!, 0, 0, width, height, matrix, true)

            runOnUiThread {
                processedImageView.setImageBitmap(rotatedBitmap)
                applyColorMatrixToImageView()
            }

            image.close()
        }
    }

    private fun updatePreviewFilter() {
        updateCachedColorFilter()
        applyColorMatrixToImageView()
        Log.d(TAG, "Filter changed to: $currentFilter (intensity: $filterIntensity)")
    }

    private fun updateCachedColorFilter() {
        val colorMatrix = VisionColorFilter.getMatrix(currentFilter)

        cachedColorFilter = if (colorMatrix != null) {
            // Apply intensity scaling to the color matrix
            if (filterIntensity < 1.0f) {
                val scaledMatrix = scaleColorMatrixIntensity(colorMatrix, filterIntensity)
                ColorMatrixColorFilter(scaledMatrix)
            } else {
                ColorMatrixColorFilter(colorMatrix)
            }
        } else {
            null
        }
    }

    private fun applyColorMatrixToImageView() {
        processedImageView.colorFilter = cachedColorFilter
    }
    
    /**
     * Scale color matrix intensity by blending with identity matrix
     * @param matrix Original color matrix
     * @param intensity Intensity factor (0.0 = identity, 1.0 = full effect)
     */
    private fun scaleColorMatrixIntensity(matrix: ColorMatrix, intensity: Float): ColorMatrix {
        val identityMatrix = ColorMatrix()
        val scaledMatrix = ColorMatrix()
        
        // Interpolate between identity and the target matrix
        val invIntensity = 1.0f - intensity
        val originalArray = matrix.array
        val identityArray = identityMatrix.array
        val scaledArray = FloatArray(20)
        
        for (i in originalArray.indices) {
            scaledArray[i] = identityArray[i] * invIntensity + originalArray[i] * intensity
        }
        
        scaledMatrix.set(scaledArray)
        return scaledMatrix
    }



    private fun updateActiveFilterTextView() {
        val filterName = when (currentFilter) {
            VisionColorFilter.FilterType.DOG -> "🐕 Dog Vision"
            VisionColorFilter.FilterType.CAT -> "🐱 Cat Vision"
            VisionColorFilter.FilterType.BIRD -> "🦅 Bird Vision"
            VisionColorFilter.FilterType.DOG_ADVANCED -> "🐕 Dog Vision (Advanced)"
            VisionColorFilter.FilterType.CAT_ADVANCED -> "🐱 Cat Vision (Advanced)"
            VisionColorFilter.FilterType.BIRD_ADVANCED -> "🦅 Bird Vision (Advanced)"
            VisionColorFilter.FilterType.RED_ONLY_TEST -> "🔴 RED ONLY TEST"
            VisionColorFilter.FilterType.ORIGINAL -> "👁️ Human Vision"
        }
        val intensityText = if (filterIntensity < 1.0f) " (${(filterIntensity * 100).toInt()}%)" else ""
        activeFilterTextView.text = "Current View: $filterName$intensityText"
        Log.d(TAG, "Updated filter text to: $filterName$intensityText")
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }


}