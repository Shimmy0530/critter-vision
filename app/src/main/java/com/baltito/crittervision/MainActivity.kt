package com.baltito.crittervision

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
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
    private var selectedButton: Button? = null
    private val buttonDefaultColors = mutableMapOf<Button, Int>()

    private lateinit var visionProcessor: AnimalVisionProcessor
    private lateinit var visionEffect: AnimalVisionEffect

    // Animal modes in scrollable top row
    private val animalModes = listOf(
        VisionColorFilter.VisionMode.DOG,
        VisionColorFilter.VisionMode.CAT,
        VisionColorFilter.VisionMode.BIRD,
        VisionColorFilter.VisionMode.EAGLE,
        VisionColorFilter.VisionMode.HORSE,
        VisionColorFilter.VisionMode.MANTIS_SHRIMP,
        VisionColorFilter.VisionMode.REINDEER,
        VisionColorFilter.VisionMode.CUTTLEFISH,
        VisionColorFilter.VisionMode.PIT_VIPER
    )

    // Bottom row: Human + test/debug modes with tint colors
    private val bottomModes = listOf(
        VisionColorFilter.VisionMode.HUMAN to null,
        VisionColorFilter.VisionMode.RED_ONLY to 0xFFFFB3B3.toInt(),
        VisionColorFilter.VisionMode.GREEN_ONLY to 0xFFB3FFB3.toInt(),
        VisionColorFilter.VisionMode.BLUE_ONLY to 0xFFB3B3FF.toInt()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        activeFilterTextView = findViewById(R.id.activeFilterTextView)
        activeFilterTextView.setBackgroundColor(Color.argb(200, 255, 255, 255))
        activeFilterTextView.setTextColor(Color.BLACK)
        activeFilterTextView.setPadding(16, 8, 16, 8)

        // Create the GPU processor and CameraX effect
        visionProcessor = AnimalVisionProcessor()
        visionEffect = AnimalVisionEffect(visionProcessor)

        setupVisionChips()
        setupIntensitySlider()
        updateVisionMode(VisionColorFilter.VisionMode.HUMAN)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun setupVisionChips() {
        val animalRow: LinearLayout = findViewById(R.id.animalButtonsRow)
        val bottomRow: LinearLayout = findViewById(R.id.bottomButtonsRow)

        // Animal vision buttons (scrollable top row)
        for (mode in animalModes) {
            val params = VisionColorFilter.getParams(mode)
            val btn = createChipButton(params.displayName)
            buttonDefaultColors[btn] = Color.WHITE
            btn.setOnClickListener {
                updateVisionMode(mode)
                highlightButton(btn)
            }
            animalRow.addView(btn)
        }

        // Bottom row: Human + test buttons (fixed, equal width)
        for ((mode, tint) in bottomModes) {
            val params = VisionColorFilter.getParams(mode)
            val btn = createChipButton(params.displayName)
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            lp.setMargins(4, 0, 4, 0)
            btn.layoutParams = lp
            val bgColor = tint ?: Color.WHITE
            btn.setBackgroundColor(bgColor)
            buttonDefaultColors[btn] = bgColor
            btn.setOnClickListener {
                updateVisionMode(mode)
                highlightButton(btn)
            }
            bottomRow.addView(btn)
            if (mode == VisionColorFilter.VisionMode.HUMAN) {
                highlightButton(btn)
            }
        }
    }

    private fun createChipButton(label: String): Button {
        val btn = Button(this)
        btn.text = label
        btn.setBackgroundColor(Color.WHITE)
        btn.setTextColor(Color.BLACK)
        btn.elevation = 8f
        btn.alpha = 0.9f
        btn.isAllCaps = false
        btn.textSize = 13f
        btn.setPadding(24, 8, 24, 8)
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.setMargins(4, 0, 4, 0)
        btn.layoutParams = lp
        return btn
    }

    private fun highlightButton(btn: Button) {
        // Reset previous selection to its default color
        selectedButton?.let {
            it.setBackgroundColor(buttonDefaultColors[it] ?: Color.WHITE)
            it.setTextColor(Color.BLACK)
        }
        // Highlight new selection
        btn.setBackgroundColor(Color.parseColor("#1565C0"))
        btn.setTextColor(Color.WHITE)
        selectedButton = btn
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
