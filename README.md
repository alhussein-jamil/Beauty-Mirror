# Beauty Mirror


## Workshop pond experience

The default exhibition scene is an animated sky-blue pond screensaver. With no visitor, the water
moves continuously across the whole display. When a face appears, a fluid vortex opens at its
location and the complete beautified camera image gradually merges with the pond—never as a cut-out.
Camera blend, image deformation, vortex strength and the 3–30 second reveal are curator-controlled.
Run it with:

```bash
make workshop
```


A fully local Android beauty mirror designed for a live exhibition. CameraX feeds a custom OpenGL ES 3.0 renderer and MediaPipe Face Landmarker supplies a stabilized face mesh plus its canonical 3-D facial transformation matrix when available. The GPU pipeline improves skin, under-eyes, eyes, brows, lips, lighting and facial shape while a permanent 30 FPS protection loop removes optional work before motion becomes visibly choppy.

## Current scope

- True mirrored front-camera presentation with rear-camera switching
- CameraX preview request targeting a stable 30 FPS when supported by the device
- OpenGL ES multipass preview with no CPU pixel processing in the preview loop
- One-face MediaPipe LIVE_STREAM tracking with latest-frame backpressure
- Landmark masks cached between analysis updates instead of being rebuilt every camera frame
- Edge-aware smoothing, complexion evenness, spot softening, redness reduction, shine control and skin glow
- Separate under-eye controls for dark-circle lift, blue/purple neutralization and puffiness softening
- Eye clarity, sclera brightening, sparkle, brow definition and teeth whitening
- Luminance-preserving lip enhancement, tint, border definition and tracked center gloss
- Face-local exposure, shadow, highlight, warmth, contrast, contour and blush controls
- Conservative face slimming, eye enlargement and nose refinement with pose attenuation
- Quick corrections for Fresh eyes, Even skin, Defined features and Stage ready
- Face-zone graphics plus subtle live region outlines while editing
- Face-safe Studio docking: controls open opposite the tracked face and remain stable while posing
- Sky-blue procedural pond with continuous multi-directional ripples, optical highlights and a transient face-centered fluid vortex
- Parameterized 3–30 second visitor transformation (10 seconds by default): face corrections accumulate continuously while the visitor looks
- Automatic transformation restart after settings are committed, after a visitor leaves and returns, or when tracking directly hands off to a different face
- Off, Natural, Soft, Bright, Stage, Glam and Custom presets
- Press-and-hold untreated comparison and processed MediaStore photo capture
- Camera-first Compose UX with a compact top status bar, studio sheet and labeled capture dock
- HIGH/MEDIUM/LOW plus automatic PERFORMANCE quality with continuous interpolation, fast demotion and slow recovery
- Camera-limited cadence detection avoids destroying image quality when the sensor—not the renderer—is the bottleneck
- No accounts, analytics, uploads, or runtime model download; network only for GitHub OTA


## Art-workshop pond mode

The pond is a final presentation layer applied after the full beauty graph. It is a sky-reflecting
fluid surface rather than a flat image or a pool-caustic filter. The idle scene uses layered wave
normals over the entire screen. Once a visitor appears, the full camera reflection blends into the
water; a broad face guide improves readability without creating a silhouette edge. Deformation and
the arrival vortex are independently adjustable.

When the editor is closed with **Return to pond**, or the visitor taps outside it, the app:

1. commits the current beauty settings;
2. switches to the pond scene;
3. hides application chrome and system bars;
4. restarts the configured reveal timer;
5. progressively increases the real face-correction strengths while the reflection settles.

The timer defaults to 10 seconds and is adjustable from 3 to 30 seconds under Studio → Scene.
A sustained face departure rearms the installation. A direct MediaPipe hand-off to a face at a very
different position/scale also restarts the sequence without storing or identifying faces.

At runtime, optional pond samples, particles, geometry, feature styling, mask resolution and camera
resolution are shed in stages whenever the renderer approaches the 33.3 ms frame budget. Core
skin, under-eye and face-lighting correction remain available in emergency Performance mode.

See [`docs/POND_WORKSHOP_MODE.md`](docs/POND_WORKSHOP_MODE.md) for the complete state machine and
performance behavior.

## Privacy

Face processing stays on-device. The app may use `INTERNET` only to check/download updates from GitHub Releases (Bokko-style OTA). `ACCESS_NETWORK_STATE` stays stripped. Camera analysis frames and landmarks are not persisted. Only photos explicitly captured by the user are saved.

Required permissions:

- `CAMERA`
- `INTERNET` (GitHub OTA only)
- `REQUEST_INSTALL_PACKAGES` (sideload updates)
- `WRITE_EXTERNAL_STORAGE` only on Android 8–9 (`maxSdkVersion=28`)

## Requirements

- JDK 17 or newer
- Android SDK platform 35
- Android Build Tools 35.0.0 or compatible
- OpenGL ES 3.0 device
- Front or rear camera

The face model is bundled at `app/src/main/assets/face_landmarker.task`. Its checksum is documented in [`docs/MODEL_ASSET.md`](docs/MODEL_ASSET.md).

## Build and verify

The build scripts discover the Android SDK from `ANDROID_HOME`, `ANDROID_SDK_ROOT`,
`local.properties`, or common Linux SDK locations. They also refresh `local.properties`
automatically.

The normal workflow is now:

```bash
make doctor   # first time / troubleshooting
make          # build installable debug APK
make phone    # build, install and launch on a connected phone
make demo     # launch mirrored Stage mode in the pond scene with UI hidden
make workshop # same exhibition workflow, named for installation use
make fps      # collect 10 seconds of Android frame/jank statistics
make perf     # launch exhibition mode, then run the FPS check
make ui-test  # exercise Compose controls on a connected device
make device-check # full build/lint/UI/FPS device gate
```

Generated debug APK:

```text
releases/beauty-mirror-debug.apk
```

Inside the app, use the eye icon to enter a clean full-screen artwork. The panel,
buttons, diagnostics, status bar, and navigation bar disappear. In normal mode, tap the image to
restore them. In workshop/exhibition mode, visitor taps are ignored; a curator long-press restores
the controls.

Other useful targets:

```bash
make devices
make phone
make demo
make expo
make workshop
make screenshot
make fps
make perf
make logs
make release
make fresh
make check
make verify
make where
```

A release build is deliberately unsigned unless production credentials are supplied. It never falls back to the debug keystore.

### Production signing

Provide all four values as Gradle properties or environment variables:

```text
BEAUTY_MIRROR_RELEASE_STORE_FILE
BEAUTY_MIRROR_RELEASE_STORE_PASSWORD
BEAUTY_MIRROR_RELEASE_KEY_ALIAS
BEAUTY_MIRROR_RELEASE_KEY_PASSWORD
```

Example using environment variables:

```bash
export BEAUTY_MIRROR_RELEASE_STORE_FILE=/secure/path/beauty-mirror.jks
export BEAUTY_MIRROR_RELEASE_STORE_PASSWORD='...'
export BEAUTY_MIRROR_RELEASE_KEY_ALIAS='beauty-mirror'
export BEAUTY_MIRROR_RELEASE_KEY_PASSWORD='...'
make release
```

No key material or secret belongs in the repository.

## Architecture

```text
CameraX Preview -> SurfaceTexture/OES -> GLES render graph -> SurfaceView
                         |
CameraX ImageAnalysis -> MediaPipe LIVE_STREAM -> temporal smoothing -> GPU masks
```

Main modules:

| Package | Responsibility |
|---|---|
| `camera` | CameraX lifecycle, surface requests, analysis, processed capture |
| `tracking` | MediaPipe lifecycle, coordinates, temporal filtering, cheek metrics, head pose |
| `masks` | Landmark polygons and reusable feathered mask textures |
| `rendering` | EGL/GLES ownership, render graph, frame metrics |
| `rendering.passes` | Camera input, face warp, skin, under-eye, lighting, features, color, composite |
| `settings` | Presets, clamping, DataStore, adaptive quality |
| `ui` | Compose mirror controls and debug presentation |

Detailed invariants are in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Important correctness rules

- Front-camera mirroring is resolved against CameraX transformation metadata, then applied exactly once before every beauty effect.
- GPU masks and saved captures use the same mirrored presentation space as the visible preview.
- Mask textures are refreshed only when a new tracked face result or runtime quality profile arrives.
- 30 FPS protection first interpolates samples, mask cadence and optional effect strength, then changes resolution only after sustained overload. PERFORMANCE skips optional geometry, detail restoration and feature styling while retaining core skin, under-eye and lighting correction.
- A low camera cadence with inexpensive rendering is classified as camera-limited instead of triggering destructive quality shedding.
- The workshop pond pass runs after face correction. Idle output is fully procedural; with a visitor, the complete camera reflection is optically blended into the pond. A broad analytic face guide protects clarity, and optional pond wave layers are removed under load.
- Camera buffers are configured to CameraX's requested resolution before the surface is provided.
- The camera input pass performs a centered aspect-fill crop using CameraX rotation metadata instead of stretching the image.
- MediaPipe owns an accepted `ImageProxy` until its asynchronous callback completes.
- Camera changes invalidate old tracking generations without closing an image still used by native code.
- Preview rendering never calls `glReadPixels`; capture is the only readback path.
- JPEG conversion and MediaStore I/O run off the GL thread.
- The Off preset is an untreated render path apart from orientation, crop, and mirroring.

## Tests

The JVM suite covers coordinate transforms, mask geometry, temporal behavior, settings and preset mapping, the PERFORMANCE quality floor, adaptive 30 FPS hysteresis, camera-limited cadence detection, visitor reveal timing, capture sizing, and aspect-fill crop calculations. Compose instrumentation tests exercise every Studio page, scene selection, sliders, presets, one-tap actions, reset and switch semantics.

CI runs static repository checks, unit tests, debug/release lint, debug assembly, unsigned-or-signed release assembly, packaged-asset verification, and merged-manifest privacy verification.

## Device smoke test

Before publishing an APK, validate on at least one physical front-camera device:

```bash
adb devices
./gradlew :app:installDebug
adb logcat -c
adb logcat | grep -E 'BeautyMirror|BeautyRenderer|FaceLandmarker'
```

Check cold launch, permission grant/denial, front/rear switching, all rotations, tracking alignment at frame edges, glasses/facial hair, low light, before/after, repeated captures, background/resume, and sustained thermal behavior.

## Releases + in-app OTA

Sideload builds check GitHub Releases for a newer `versionCode` (`buildNumber` in `version.json`), download the debug APK, verify SHA-256, then open the system installer. Auto-update is on by default (Studio → System).

- Local publish: `make ship-release` (writes OTA `version.json` from the debug APK)
- Auto publish: push tag `v*` — **Release APK** workflow uploads APK + `version.json`
- Public repo: no OTA token baked into the APK
- Optional host PAT: `tools/ota/token.local` with `BM_USE_OTA_TOKEN=1` for publish helpers only

## Known limitations

- Only one primary face is processed.
- Face lighting is bounded photographic correction, not physically accurate relighting.
- Under-eye correction, spot softening, shine control and facial-hair protection are heuristic visual effects, not dermatological analysis.
- Geometry controls are deliberately bounded; this is not identity-changing generation.
- Head orientation prefers MediaPipe’s canonical 3-D facial transformation matrix, then falls back to a conservative mesh estimate when the optional matrix is unavailable. It is still an interactive visual effect, not calibrated medical tracking.
- No trained custom beautification model is bundled.
- Capture still uses a synchronous one-shot GLES readback, though conversion and saving are moved off the GL thread.
- Camera frame-rate requests are advisory. A camera sensor limited below 30 FPS cannot be made to produce 30 unique frames; the app still minimizes render latency and reduces optional processing.
- Exact camera crop behavior can vary by vendor; physical-device alignment testing remains mandatory.

## Local validation status

See [`VALIDATION.md`](VALIDATION.md). The repair environment used for this revision lacked an Android SDK and attached device, so full Gradle and hardware validation could not be executed there. Static checks, XML parsing, shell validation, archive checks, and pure-Kotlin compilation were executed.

## License

Application code is Apache-2.0. Review the bundled model's upstream redistribution terms before distributing the APK.

## Build troubleshooting

Run `make doctor` before the first Gradle build. It checks Java 17, free disk space,
the Gradle cache directory, the Android SDK, and connected devices.

The wrapper uses the smaller Gradle `bin` distribution to reduce download and disk usage.
If Gradle reports that a `.zip.lck` path does not exist, the cache directory is missing,
unwritable, a broken symlink, or the filesystem is out of space/inodes. Repair it with:

```bash
rm -rf ~/.gradle/wrapper/dists/gradle-8.14.3-*
mkdir -p ~/.gradle/wrapper/dists
chmod -R u+rwX ~/.gradle
make doctor
make debug
```

Use plain `make` or `make debug` for the installable development APK. Both copy the
artifact to `releases/beauty-mirror-debug.apk`. `make release` builds and copies the release
variant; it may be unsigned when production signing credentials are not configured.
