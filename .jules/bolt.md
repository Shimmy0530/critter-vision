# Bolt's Journal

## 2024-05-24 - [VisionColorFilter caching]
**Learning:** The `VisionColorFilter` class was recalculating complex matrices (involving `exp` and `pow`) on every frame, causing significant CPU overhead.
**Action:** Implemented lazy initialization and caching for all color matrices to ensure these expensive calculations happen only once.

## 2024-05-25 - [OpenGL Uniform Caching]
**Learning:** `glGetUniformLocation` is a blocking string-based lookup. Calling it every frame in `ColorFilterSurfaceProcessor` for "sTexture" created significant overhead.
**Action:** Cached the uniform handle during shader initialization/recompilation to remove this lookup from the hot render loop.

## 2024-05-26 - [Zero Allocation Loop]
**Learning:** `Bitmap.createBitmap` in a high-frequency camera loop (30-60 FPS) for simple rotation causes massive GC pressure due to per-frame allocations.
**Action:** Use `ImageView.ScaleType.MATRIX` with a manually calculated `Matrix` to handle rotation/scaling, and implement double-buffering for the source `Bitmap` to enable zero-allocation updates.
