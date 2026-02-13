package com.baltito.crittervision

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private lateinit var previewView: PreviewView
    private lateinit var activeFilterTextView: TextView

    private var currentMode = VisionColorFilter.VisionMode.HUMAN
    private var filterIntensity = 1.0f

    private lateinit var visionProcessor: AnimalVisionProcessor
    private lateinit var visionEffect: AnimalVisionEffect

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        activeFilterTextView = findViewById(R.id.activeFilterTextView)

        // Create the GPU processor and CameraX effect
        visionProcessor = AnimalVisionProcessor()
        visionEffect = AnimalVisionEffect(visionProcessor)

        setupButtons()
        setupIntensitySlider()
        updateVisionMode(VisionColorFilter.VisionMode.HUMAN)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun setupButtons() {
        val dogBtn: Button = findViewById(R.id.dogVisionButton)
        val catBtn: Button = findViewById(R.id.catVisionButton)
        val birdBtn: Button = findViewById(R.id.birdVisionButton)
        val originalBtn: Button = findViewById(R.id.originalVisionButton)
        val redBtn: Button = findViewById(R.id.redOnlyTestButton)
        val greenBtn: Button = findViewById(R.id.greenOnlyTestButton)
        val blueBtn: Button = findViewById(R.id.blueOnlyTestButton)

        makeButtonsProminent(dogBtn, catBtn, birdBtn, originalBtn, redBtn, greenBtn, blueBtn)

        dogBtn.setOnClickListener { updateVisionMode(VisionColorFilter.VisionMode.DOG) }
        catBtn.setOnClickListener { updateVisionMode(VisionColorFilter.VisionMode.CAT) }
        birdBtn.setOnClickListener { updateVisionMode(VisionColorFilter.VisionMode.BIRD) }
        originalBtn.setOnClickListener { updateVisionMode(VisionColorFilter.VisionMode.HUMAN) }
        redBtn.setOnClickListener { updateVisionMode(VisionColorFilter.VisionMode.RED_ONLY) }
        greenBtn.setOnClickListener { updateVisionMode(VisionColorFilter.VisionMode.GREEN_ONLY) }
        blueBtn.setOnClickListener { updateVisionMode(VisionColorFilter.VisionMode.BLUE_ONLY) }
    }

    private fun setupIntensitySlider() {
        val seekBar: SeekBar = findViewById(R.id.filterIntensitySeekBar)
        val valueText: TextView = findViewById(R.id.intensityValueText)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    filterIntensity = progress / 100.0f
                    valueText.text = "${progress}%"
                    applyCurrentVision()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateVisionMode(mode: VisionColorFilter.VisionMode) {
        currentMode = mode
        applyCurrentVision()
        updateFilterLabel()
        Log.d(TAG, "Vision mode: $mode (intensity: $filterIntensity)")
    }

    private fun applyCurrentVision() {
        val params = VisionColorFilter.getParams(currentMode)
        visionProcessor.setVisionParams(params, filterIntensity)
    }

    private fun updateFilterLabel() {
        val params = VisionColorFilter.getParams(currentMode)
        val intensityText = if (filterIntensity < 1.0f) " (${(filterIntensity * 100).toInt()}%)" else ""
        activeFilterTextView.text = "Current View: ${params.displayName}$intensityText"
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

    // ── Camera ──────────────────────────────────────────────────────────

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val useCaseGroup = UseCaseGroup.Builder()
                    .addUseCase(preview)
                    .addEffect(visionEffect)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, useCaseGroup
                )

                Log.d(TAG, "Camera started with OpenGL ES effect pipeline")

            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
                Toast.makeText(this, "Error starting camera: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // ── Permissions ─────────────────────────────────────────────────────

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
                Toast.makeText(this, "Camera permission required.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        visionProcessor.release()
    }
}
