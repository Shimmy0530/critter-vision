# Bolt's Journal

## 2024-05-24 - [VisionColorFilter caching]
**Learning:** The `VisionColorFilter` class was recalculating complex matrices (involving `exp` and `pow`) on every frame, causing significant CPU overhead.
**Action:** Implemented lazy initialization and caching for all color matrices to ensure these expensive calculations happen only once.

## 2024-05-25 - [OpenGL Uniform Caching]
**Learning:** `glGetUniformLocation` is a blocking string-based lookup. Calling it every frame in `ColorFilterSurfaceProcessor` for "sTexture" created significant overhead.
**Action:** Cached the uniform handle during shader initialization/recompilation to remove this lookup from the hot render loop.

## 2026-02-02 - [Bitmap Rotation Optimization]
**Learning:** `Bitmap.createBitmap` for rotation is a costly allocation-heavy operation.
**Action:** Replaced CPU rotation with `ImageView` matrix transformation and implemented double buffering to eliminate per-frame allocations.
