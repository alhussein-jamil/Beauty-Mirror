# Beauty Mirror 3.1 performance and feature audit

## 30 FPS protection

The presentation target is 30 FPS (33.3 ms per displayed frame). CameraX requests a stable
30 FPS preview where supported. Every 500 ms the app evaluates camera cadence, average render
latency, nearest-rank p95 latency, slow-frame ratio and MediaPipe analysis throughput.

Protection happens in two layers:

1. **Continuous interpolation:** before any camera rebind, smoothing samples, optional geometry,
   feature styling, mask blur and mask refresh cadence are reduced smoothly. Recovery is slower
   than degradation to prevent visible oscillation.
2. **Rare quality-tier changes:** resolution changes only after sustained overload. The user choice
   is a ceiling; automatic recovery never exceeds it.

Runtime profiles:

| Profile | Render height | Mask size | Analysis | Skin samples | Optional stages |
|---|---:|---:|---:|---:|---|
| High | 1080 | 512 | 18 Hz | 8 | all |
| Medium | 900 | 384 | 15 Hz | 7 | all |
| Low | 720 | 224 | 10 Hz | 4 | all, continuously attenuated as needed |
| Performance | 480 | 128 | 6 Hz | 2 | no geometry, detail restoration or feature styling |

The controller recognizes a camera-limited session: when camera FPS is low but render p95,
average latency and slow-frame ratio are comfortable, it reports the sensor/CameraX cadence as
the limit rather than deleting visual quality that cannot improve the source frame rate.

The largest optimization is mask caching. MediaPipe landmarks update less often than camera frames,
so face/skin/eye/brow/lip/mouth/detail masks are redrawn only when tracking advances and the adaptive
mask interval permits. Lake face bounds are likewise recomputed only on a new tracking timestamp.

`make fps` samples Android gfx statistics. `make perf` launches the lake exhibition first.
`make device-check` runs static checks, JVM tests, lint, APK assembly, Compose interaction tests,
exhibition launch and the FPS sample on a connected device.

## Dark-lake exhibition scene

The water treatment is a single post-beauty OpenGL pass:

- dark olive, charcoal and brown grading rather than pool turquoise;
- restrained horizontal surface motion and sparse face-centered arrival ripples;
- an elliptical face-clarity window so under-eye, skin and feature corrections remain visible;
- a frame-rate-independent visitor reveal reaching approximately 99% within 0.9 seconds;
- slow release so one missed tracking frame does not pulse the reflection;
- three-tap softening at higher budgets and a one-tap path under pressure.

## Feature audit

### Skin

- Edge-aware two-scale smoothing
- Texture retention
- Complexion evenness
- Spot/blemish softening based on local bilateral residual
- Redness neutralization
- Shine compression on the upper luminance shoulder
- Mid-tone skin glow
- Face-local exposure, shadows, highlights, warmth and contrast

### Under eyes

- Dark-circle luminance correction using side-specific nearby cheek references
- Blue/purple chroma neutralization
- Local puffiness/texture softening
- User-controlled maximum lift
- Independent side visibility and pose attenuation

### Eyes, brows, teeth and lips

- Iris/lash clarity and restrained sparkle
- Sclera brightening restricted to light, low-saturation candidates
- Brow definition restricted to existing dark hair energy
- Teeth whitening restricted to bright, low-saturation mouth pixels
- Natural lip chroma, luminance-preserving tint, border definition and tracked center gloss

### Shape and styling

- Pose-aware face slimming, eye enlargement and nose refinement
- Contour, nose light and blush with side visibility attenuation
- Geometry is continuously reduced under load and disabled in Performance

## UX audit

The screen remains camera-first. The labeled dock exposes Lake, Studio, capture and press-and-hold
compare. Opening Studio chooses the side of the screen opposite the current face and keeps that
placement stable for the editing session. The live preview receives only thin region outlines;
sliders and explanations stay in the face-safe sheet. Controls are grouped by visitor intent:
Looks, Skin, Eyes/under-eyes, Lips, Shape, Scene and System. Redundant lake controls were removed
from quick actions because Lake is already available from the permanent dock and Scene page.
