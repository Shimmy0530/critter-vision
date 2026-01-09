## 2024-05-22 - [Optimizing Camera Frame Processing]
**Learning:** Android's ImageAnalysis default YUV output often leads to inefficient conversion pipelines (YUV -> JPEG -> Bitmap).
**Action:** Always check if `OUTPUT_IMAGE_FORMAT_RGBA_8888` is supported and use it to avoid expensive compression/decompression cycles. Reuse Bitmaps to minimize GC pressure.
