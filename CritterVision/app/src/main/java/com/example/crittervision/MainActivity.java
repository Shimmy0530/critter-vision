package com.example.crittervision;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View; // Required for View.LAYER_TYPE_HARDWARE (though not directly used in foreground method)
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private VisionColorFilter.FilterType currentFilter = VisionColorFilter.FilterType.ORIGINAL;
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{android.Manifest.permission.CAMERA};
    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    // private ImageView filteredImageView; // Decided against for now
    private TextView activeFilterTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        activeFilterTextView = findViewById(R.id.activeFilterTextView);
        cameraExecutor = Executors.newSingleThreadExecutor();

        Button dogVisionButton = findViewById(R.id.dogVisionButton);
        Button catVisionButton = findViewById(R.id.catVisionButton);
        Button birdVisionButton = findViewById(R.id.birdVisionButton);
        Button originalVisionButton = findViewById(R.id.originalVisionButton);

        updateActiveFilterTextView(); // Set initial text

        dogVisionButton.setOnClickListener(v -> {
            currentFilter = VisionColorFilter.FilterType.DOG;
            updatePreviewFilter();
            updateActiveFilterTextView();
        });

        catVisionButton.setOnClickListener(v -> {
            currentFilter = VisionColorFilter.FilterType.CAT;
            updatePreviewFilter();
            updateActiveFilterTextView();
        });

        birdVisionButton.setOnClickListener(v -> {
            currentFilter = VisionColorFilter.FilterType.BIRD;
            updatePreviewFilter();
            updateActiveFilterTextView();
        });

        originalVisionButton.setOnClickListener(v -> {
            currentFilter = VisionColorFilter.FilterType.ORIGINAL;
            updatePreviewFilter();
            updateActiveFilterTextView();
        });

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                // Consider disabling camera-dependent features or closing the app
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll(); // Unbind use cases before rebinding
                cameraProvider.bindToLifecycle(this, cameraSelector, preview);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Use case binding failed", e);
                Toast.makeText(getApplicationContext(), "Error starting camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }

    private void updateActiveFilterTextView() {
        String filterName = "Original Vision"; // Default
        switch (currentFilter) {
            case DOG:
                filterName = "Dog Vision";
                break;
            case CAT:
                filterName = "Cat Vision";
                break;
            case BIRD:
                filterName = "Bird Vision";
                break;
            case ORIGINAL:
                // filterName is already "Original Vision"
                break;
        }
        activeFilterTextView.setText("Current View: " + filterName);
    }

    private void updatePreviewFilter() {
        ColorMatrixColorFilter filter = VisionColorFilter.getFilter(currentFilter);
        Log.d(TAG, "Updating filter to: " + currentFilter);

        Drawable foreground = previewView.getForeground();

        if (filter == null) {
            // Clear existing filter
            if (foreground != null) {
                // Performance Note: Applying ColorFilter to PreviewView's foreground can be inefficient for real-time camera streams.
                // If lag is observed, consider using CameraX Effects API (Preview.setEffect with a SurfaceProcessor from androidx.camera:camera-effects)
                // or an ImageAnalysis use case to process frames and display them on a separate ImageView for better performance.
                foreground.mutate().clearColorFilter();
                previewView.invalidate(); // Request redraw
                Log.d(TAG, "Cleared filter from PreviewView foreground.");
            } else {
                Log.d(TAG, "PreviewView foreground is null, no filter to clear.");
            }
        } else {
            // Apply new filter
            if (foreground == null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                // Ensure foreground exists for API 23+ to apply a ColorFilter to it.
                // Using a transparent drawable as a base for the filter.
                previewView.setForeground(new ColorDrawable(Color.TRANSPARENT));
                foreground = previewView.getForeground();
                Log.d(TAG, "Initialized PreviewView foreground for filter application.");
            }

            if (foreground != null) {
                // Performance Note: Applying ColorFilter to PreviewView's foreground can be inefficient for real-time camera streams.
                // If lag is observed, consider using CameraX Effects API (Preview.setEffect with a SurfaceProcessor from androidx.camera:camera-effects)
                // or an ImageAnalysis use case to process frames and display them on a separate ImageView for better performance.
                foreground.mutate().setColorFilter(filter);
                previewView.invalidate(); // Request redraw
                Log.d(TAG, "Applied new filter to PreviewView foreground.");
            } else {
                Log.d(TAG, "PreviewView foreground is null, cannot apply filter.");
                 // Fallback or alternative methods could be logged or attempted here if foreground is not available/working.
                 // e.g., Log.w(TAG, "Consider using setEffect for PreviewView if foreground filtering is insufficient.");
            }
        }
    }
}
