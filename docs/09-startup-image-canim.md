# 09: Boot animation (`.canim`)

We decoded the format but kept the stock Audi boot animation in our car, so **nothing here was tested in the car.**

- Location: `/mnt/app/eso/hmi/splashscreen/*.canim`, chosen by slot in `StartupImageConfig.txt`
  (our A6 uses `AudiMIBHigh_SUA_A6.canim`, slot 6).
- Format: the whole file is **zlib**-compressed. Decompressed it starts with the magic **`ANIM1`**,
  a **32-byte header**, then a **command block** (frame timing/placement), then raw image data in **BGRA**,
  starting at byte `32 + command block length`.
- Frames are 800×480 for the centre screen.

`tools/preview/startup.py` renders a front view of a car model as an 800×480 startup picture (design only).
If you build a new `.canim`, keep the header/command block of the original and only replace pixel data of the
same size, re-compress with zlib, and install it with the same safety pattern as `content.kzb` (docs/06).
