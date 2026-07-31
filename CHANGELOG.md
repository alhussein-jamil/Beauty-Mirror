# Changelog

## 2.0.0

- Camera-first exhibition UI with compact status chrome and labeled capture dock.
- Region-based Beauty Studio with face-zone graphics and quick corrections.
- New spot softening, shine control, skin glow, under-eye smoothing, eye sparkle and brow definition.
- Automatic 480p Performance quality floor with average/p95/slow-frame-aware 30 FPS protection.
- Reuse face masks between tracking updates; lower mask/render budgets under load.
- CameraX 30–60 FPS target request where supported.


## 1.4.0

- Made beautification effects visibly effective instead of exposure-dominated.
- Added stronger multiscale skin smoothing and complexion correction.
- Increased pose-aware eye, lip, contour and geometry response.
- Added legacy custom-look migration and smoothing-radius UI control.

## 1.3.1

- Fixed startup failure on OpenGL ES drivers that reserve the GLSL identifier `active`.
- Added a static shader identifier guard so reserved GLSL ES words cannot silently ship again.

## 1.3.0

- Added stabilized yaw, pitch and roll handling that prefers MediaPipe’s canonical 3-D facial transformation matrix, with a conservative landmark fallback.
- Made front-camera mirroring explicit in the camera-input shader so preview, masks and captured photos stay in one deterministic mirror space.
- Added pose-aware per-side visibility and automatic attenuation at steep head angles.
- Added bounded face slimming, eye enlargement and nose refinement using landmark-local GPU warps.
- Added complexion evenness, redness correction, eye brightening, teeth whitening, lip enhancement, blush and face-local contour.
- Added lip and mouth-interior masks so lip/teeth effects remain separated.
- Added Stage and Glam presets; Stage is the exhibition-oriented default used by `make demo`.
- Added an exhibition launch mode that starts mirrored, on High quality, with the interface hidden.
- Simplified the Makefile around `make`, `make phone`, `make demo`/`make expo`, `make screenshot`, `make fresh`, and aliases for logs/deploy.
- Added mirror, pose and coordinate-space regression tests and expanded packaged-shader validation.

## 1.2.0

- Corrected portrait preview aspect handling by using CameraX transformation rotation when calculating the OpenGL fill crop.
- Added CameraX transformation-info propagation from each `SurfaceRequest` into the renderer.
- Added a true distraction-free mirror mode: hide all UI and system bars, then tap the preview or the small reveal button to restore them.
- Replaced the permanently large overlay with a collapsible beauty panel and compact camera dock.
- Refined presets, intensity, advanced controls, quality selection, diagnostics, capture progress, and before/after interaction.
- Updated the visual system with clearer hierarchy, larger touch targets, safer edge-to-edge spacing, and more legible transient errors.
- Made `make`, `make debug`, and `make release` build and copy clearly named APKs automatically.
- Added automatic Android SDK discovery and `local.properties` repair for every Gradle invocation.
- Added `make run`, `make install`, `make devices`, `make logs`, `make where`, and simpler release APK targets.
- Added rotated-camera aspect crop tests and a static guard for the invalid Compose `weight` import.

## 1.1.0

- Hardened EGL initialization, pbuffer fallback, context teardown, and surface validation.
- Configured the camera SurfaceTexture from CameraX's requested buffer size.
- Added aspect-fill camera cropping to prevent stretched previews.
- Fixed MediaPipe camera-switch and shutdown ownership races.
- Reset stale luma and cheek state with tracking resets.
- Kept photo encoding and MediaStore writes off the GL thread.
- Preserved capture aspect ratio and prevented duplicate captures.
- Fixed under-eye and warmth strength being applied twice.
- Reworked face lighting into bounded, skin-tone-independent controls.
- Allowed advanced Custom effects to work independently of the global slider.
- Bypassed unnecessary render passes.
- Added retry, capture progress, scrollable advanced controls, and narrow-screen preset scrolling.
- Restored permission state after returning from Android settings.
- Removed duplicate shader trees and the unused FileProvider.
- Removed release use of the debug keystore and added external production signing.
- Added CI, static privacy/asset checks, model checksum verification, and APK verification.
