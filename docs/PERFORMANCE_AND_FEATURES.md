# Beauty Mirror 2.0 performance and feature audit

## 30 FPS protection

The presentation target is 30 FPS (33.3 ms per displayed frame). The app requests a 30–60 FPS
camera range where CameraX/device support permits it, then continuously observes displayed camera
FPS, render-thread frame time and analysis throughput.

Runtime profiles:

| Profile | Render height | Mask size | Analysis | Skin samples | Optional stages |
|---|---:|---:|---:|---:|---|
| High | 1080 | 576 | 20 Hz | 10 | all |
| Medium | 900 | 384 | 15 Hz | 7 | all |
| Low | 720 | 224 | 10 Hz | 4 | no detail restoration |
| Performance | 480 | 128 | 6 Hz | 2 | no geometry, detail restoration or feature styling |

Demotion is fast (roughly 0.5 s for a severe stall, 1.25 s for a sustained miss). Recovery requires
an 8 s comfortable window to avoid oscillation. The user-selected quality is a ceiling; Performance
is automatic-only.

The largest optimization is mask caching. MediaPipe landmarks update at 8–20 Hz, while camera frames
usually arrive near 30 FPS. Face/skin/eye/brow/lip/mouth/detail masks are now redrawn only when the
tracking timestamp or runtime quality changes. Cached textures are reused for intervening frames.

`make fps` resets Android gfx statistics, waits ten seconds, then prints frame/jank information.
`make perf` launches exhibition mode first.

## Feature audit

### Skin

- Edge-aware two-scale smoothing
- Texture retention
- Complexion evenness
- Spot/blemish softening based on local bilateral residual
- Redness neutralization
- Shine compression on upper luminance shoulder
- Mid-tone skin glow
- Face-local exposure, shadows, highlights, warmth and contrast

### Under eyes

- Dark-circle luminance correction using side-specific nearby cheek references
- Blue/purple chroma neutralization
- Local puffiness/texture softening
- User-controlled maximum lift
- Independent side visibility and pose attenuation

### Eyes and brows

- Iris/lash clarity from untreated detail
- Sclera brightening restricted to light, low-saturation candidates
- Subtle eye sparkle
- Brow definition restricted to brow masks and existing dark hair energy
- Teeth whitening restricted to bright, low-saturation mouth pixels

### Lips

- Natural chroma enhancement
- Luminance-preserving rose tint
- Vermilion-border definition
- Mouth-centered gloss highlight

### Shape and styling

- Pose-aware face slimming, eye enlargement and nose refinement
- Contour, nose light and blush with side visibility attenuation
- Geometry automatically disabled in the emergency Performance profile

## UX audit

The primary screen is camera-first. Controls are closed initially and opened from the labeled
Studio action. The studio groups controls by the face region a visitor understands rather than by
shader pass. Each group includes a stylized face-zone graphic, concise descriptions and bounded
sliders. Quick corrections provide safe starting points. The top bar exposes tracking and FPS state;
the System page explains the active runtime quality and quality ceiling.
