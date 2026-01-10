# Bolt's Journal

## 2024-05-24 - [VisionColorFilter caching]
**Learning:** The `VisionColorFilter` class was recalculating complex matrices (involving `exp` and `pow`) on every frame, causing significant CPU overhead.
**Action:** Implemented lazy initialization and caching for all color matrices to ensure these expensive calculations happen only once.
