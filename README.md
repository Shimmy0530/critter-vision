# CritterVision

CritterVision is an Android camera app that simulates how different animals see the world. It applies real-time GPU color transformations to your live camera feed, letting you experience vision through the eyes of 10 different animals.

## Features

- **10 Animal Vision Modes:** Dog, Cat, Bird, Eagle, Horse, Mantis Shrimp, Reindeer, Cuttlefish, Pit Viper, and Human (baseline)
- **3 Debug Modes:** Red, Green, and Blue channel isolation for development and testing
- **Real-Time GPU Processing:** OpenGL ES 2.0 fragment shaders process camera frames in under 0.5ms
- **Intensity Slider:** Adjust filter strength from 0% to 100% to blend between normal and simulated vision
- **Simple Interface:** Scrollable animal button row + fixed bottom row for Human and debug modes

## How to Use

1. Launch CritterVision and grant camera permission when prompted.
2. The app opens with your live camera feed in Human Vision (unfiltered).
3. Tap any animal button in the scrollable top row to apply that vision filter.
4. Use the intensity slider to adjust filter strength.
5. Tap **Human Vision** in the bottom row to return to normal view.
6. Use the **Red/Green/Blue Channel** buttons for debug visualization.

## Vision Modes

Each filter is based on published research on animal color perception. All color matrices operate on linear RGB (sRGB linearized in the shader).

| Animal | Type | Scientific Basis |
|---|---|---|
| **Dog** | Dichromatic | Machado et al. 2009 deuteranopia (severity=1.0) — blue-yellow world, no red-green distinction |
| **Cat** | Dichromatic + desaturated | Vienot/Brettel method at cat cone positions (S=450nm, L=554nm) with 0.625 desaturation blend for low cone density |
| **Bird** | Tetrachromatic approximation | Heuristic spectral shift from Hart 2001 cone data + saturation x1.35 (oil droplet narrowing) + UV proxy |
| **Eagle** | Tetrachromatic + high acuity | Enhanced color separation with brightness offset; models 4-8x human acuity |
| **Horse** | Dichromatic | Blue-yellow vision (S=428nm, L=539nm); red and green collapse to muddy yellow |
| **Mantis Shrimp** | Hyperspectral | 12-16 receptor approximation; hypersaturation x1.80 + UV proxy 0.40 |
| **Reindeer** | UV-sensitive | UV reflectance mapped to blue/cyan glow for snow and trail navigation |
| **Cuttlefish** | Polarization-sensitive | W-pupil model with subtle saturation boost x1.15 and specular surface enhancement |
| **Pit Viper** | Infrared | Thermal pit organ simulation; luminance mapped to red channel (green/blue zeroed) |

## Architecture

```
Camera → CameraX Preview
  → AnimalVisionEffect (CameraEffect wrapper)
    → AnimalVisionProcessor (OpenGL ES 2.0 SurfaceProcessor)
      → OES texture → GLSL fragment shader → PreviewView
```

**Shader pipeline:** sRGB linearization → 3x3 color matrix → sRGB encoding → color offset → saturation boost → UV proxy → intensity blend

Key files:
- `AnimalVisionProcessor.kt` — EGL context, GLSL shaders, GL rendering on a dedicated HandlerThread
- `AnimalVisionEffect.kt` — CameraEffect wrapper passing the processor to CameraX
- `VisionColorFilter.kt` — VisionMode enum, VisionParams data class, 3x3 matrices and per-mode parameters
- `MainActivity.kt` — UI setup, CameraX binding with UseCaseGroup, intensity slider

## Technologies

- **Language:** Kotlin (managed by AGP 9.0 built-in Kotlin support)
- **Platform:** Android (Min SDK 26, Target SDK 34)
- **Camera:** CameraX 1.4.0 (camera-core, camera-camera2, camera-lifecycle, camera-view, camera-effects)
- **GPU Processing:** OpenGL ES 2.0 fragment shaders via CameraX SurfaceProcessor
- **Build:** Gradle 9.1, Android Gradle Plugin 9.0, Java 21 toolchain
- **UI:** XML layouts with programmatic button creation (no Compose)

## Building

```bash
# Set Java 21 (required)
export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"

# Build debug APK
./gradlew assembleDebug --no-daemon

# Run unit tests
./gradlew test --no-daemon
```

## Contributing

Contributions are welcome! If you have ideas for new animal vision modes, improved color science, or bug fixes:

1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/YourFeature`).
3. Make your changes and update tests as appropriate.
4. Commit and push to your branch.
5. Open a Pull Request.

## License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for details.
