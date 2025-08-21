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
import java.nio.ByteBuffer
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

    private lateinit var processedImageView: ImageView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var activeFilterTextView: TextView
    private var imageAnalyzer: ImageAnalysis? = null

    private var currentFilter: VisionColorFilter.FilterType = VisionColorFilter.FilterType.ORIGINAL

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

                // ImageAnalysis use case for processing frames
                imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
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
        override fun analyze(image: ImageProxy) {
            val bitmap = image.toBitmap()
            val rotationDegrees = image.imageInfo.rotationDegrees

            // Create a matrix for the rotation
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

            runOnUiThread {
                processedImageView.setImageBitmap(rotatedBitmap)
                applyColorMatrixToImageView()
            }
            
            image.close()
        }
    }

    private fun ImageProxy.toBitmap(): Bitmap {
        val yBuffer = planes[0].buffer // Y
        val uBuffer = planes[1].buffer // U
        val vBuffer = planes[2].buffer // V

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun updatePreviewFilter() {
        // The actual filter application now happens in the analyzer
        // This method just logs and updates the UI text
        Log.d(TAG, "Filter changed to: $currentFilter")
    }

    private fun applyColorMatrixToImageView() {
        val colorMatrix = VisionColorFilter.getMatrix(currentFilter)

        if (colorMatrix != null) {
            val filter = ColorMatrixColorFilter(colorMatrix)
            processedImageView.colorFilter = filter
        } else {
            processedImageView.colorFilter = null
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