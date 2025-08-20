package com.baltito.crittervision

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import android.widget.ImageView
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import java.util.Timer
import java.util.TimerTask
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
    private lateinit var processedImageView: ImageView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var activeFilterTextView: TextView
    private var imageCapture: ImageCapture? = null

    private var currentFilter: VisionColorFilter.FilterType = VisionColorFilter.FilterType.ORIGINAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        processedImageView = findViewById(R.id.processedImageView)
        activeFilterTextView = findViewById(R.id.activeFilterTextView)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Setup buttons
        val dogVisionButton: Button = findViewById(R.id.dogVisionButton)
        val catVisionButton: Button = findViewById(R.id.catVisionButton)
        val birdVisionButton: Button = findViewById(R.id.birdVisionButton)
        val originalVisionButton: Button = findViewById(R.id.originalVisionButton)
        val redOnlyTestButton: Button = findViewById(R.id.redOnlyTestButton)

        // Make buttons more prominent so they're visible through filters
        makeButtonsProminent(dogVisionButton, catVisionButton, birdVisionButton, originalVisionButton, redOnlyTestButton)

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

        redOnlyTestButton.setOnClickListener {
            currentFilter = VisionColorFilter.FilterType.RED_ONLY_TEST
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

                // Preview use case
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                // ImageCapture use case for capturing frames
                imageCapture = ImageCapture.Builder()
                    .build()
                
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

                // Start continuous frame capture for color matrix processing
                startFrameCapture()

                Log.d(TAG, "Camera started successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
                Toast.makeText(applicationContext, "Error starting camera: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startFrameCapture() {
        // Capture frames continuously for processing
        val captureTimer = java.util.Timer()
        captureTimer.scheduleAtFixedRate(object : java.util.TimerTask() {
            override fun run() {
                captureAndProcessFrame()
            }
        }, 0, 100) // Capture every 100ms for smooth video-like effect
    }

    private fun captureAndProcessFrame() {
        val imageCapture = imageCapture ?: return

        val outputStream = ByteArrayOutputStream()
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(outputStream).build()

        imageCapture.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val imageBytes = outputStream.toByteArray()
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    
                    // Apply color matrix to the bitmap and display in ImageView
                    runOnUiThread {
                        processedImageView.setImageBitmap(bitmap)
                        applyColorMatrixToImageView()
                    }
                }
            }
        )
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
            VisionColorFilter.FilterType.RED_ONLY_TEST -> {
                val colorMatrix = VisionColorFilter.getRedOnlyTestMatrix()
                applyColorMatrixToPreview(colorMatrix)
                Log.d(TAG, "Red only test filter applied")
                Toast.makeText(this, "🔴 RED ONLY TEST", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyColorMatrixToPreview(colorMatrix: ColorMatrix?) {
        // This method now just triggers the ImageView update
        applyColorMatrixToImageView()
        Log.d(TAG, "Color matrix set for next frame: ${colorMatrix != null}")
    }

    private fun applyColorMatrixToImageView() {
        // Apply color matrix directly to the ImageView - the proper way!
        val colorMatrix = when (currentFilter) {
            VisionColorFilter.FilterType.DOG -> VisionColorFilter.getDogVisionMatrix()
            VisionColorFilter.FilterType.CAT -> VisionColorFilter.getCatVisionMatrix()
            VisionColorFilter.FilterType.BIRD -> VisionColorFilter.getBirdVisionMatrix()
            VisionColorFilter.FilterType.RED_ONLY_TEST -> VisionColorFilter.getRedOnlyTestMatrix()
            VisionColorFilter.FilterType.ORIGINAL -> null
        }

        if (colorMatrix != null) {
            // Use the official Android API approach from your research
            val filter = ColorMatrixColorFilter(colorMatrix)
            processedImageView.colorFilter = filter
            Log.d(TAG, "Applied ColorMatrixColorFilter to ImageView")
        } else {
            // Remove color filter for original vision
            processedImageView.colorFilter = null
            Log.d(TAG, "Removed ColorMatrixColorFilter from ImageView")
        }
    }



    private fun updateActiveFilterTextView() {
        val filterName = when (currentFilter) {
            VisionColorFilter.FilterType.DOG -> "🐕 Dog Vision"
            VisionColorFilter.FilterType.CAT -> "🐱 Cat Vision"
            VisionColorFilter.FilterType.BIRD -> "🦅 Bird Vision"
            VisionColorFilter.FilterType.RED_ONLY_TEST -> "🔴 RED ONLY TEST"
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