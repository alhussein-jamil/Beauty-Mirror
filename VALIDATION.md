# Validation record

Revision 3.1.0 prepared on 2026-07-31.

## Completed in the repair environment

- Ran `tools/static-checks.sh`, including model checksum, shader inventory, privacy/signing guards,
  scoped Compose import regressions, reserved GLSL identifiers and Kotlin-pass/GLSL uniform parity.
- Parsed every Android XML resource and confirmed exact English/French key parity.
- Scanned every `R.string` reference and confirmed a matching default resource.
- Validated all shell scripts with `bash -n` and dry-ran Makefile help, UI-test and device-check flows.
- Parsed all production, JVM-test and instrumentation Kotlin files with Kotlin PSI; no syntax errors.
- Compiled and executed pure-Kotlin simulations for adaptive overload, camera-limited cadence and
  the 0.9-second visitor reveal.
- Added regression tests for source-limited FPS, nearest-rank p95, reveal attack/release, lake
  persistence and scene controls.
- Added Compose interaction tests for every Studio page, lake selection/slider, one-tap actions,
  presets, reset and exactly-once switches.
- Verified the final source archive with `unzip -t`; build outputs, local SDK state, VCS metadata,
  caches and secrets are excluded.

## Performance design validated in source

- CameraX requests a stable 30 FPS preview.
- The controller evaluates camera FPS, average/p95 render time, slow-frame ratio and analysis
  starvation every 500 ms.
- Optional samples/effects and mask cadence interpolate continuously before a resolution tier is
  changed; degradation is fast and recovery deliberately slow.
- Low source FPS with inexpensive rendering is classified as camera-limited rather than causing
  destructive quality shedding.
- PERFORMANCE uses 480p rendering, 128px masks, 6 Hz analysis and two skin samples while retaining
  core skin, under-eye and lighting correction.
- The lake scene is one post-beauty pass and falls back from three texture taps to one under load.

## Not executable in the repair environment

This environment has no Android SDK/Maven cache, connected device or outbound dependency access.
Therefore full Gradle compilation and physical camera validation remain required on the target
machine:

```bash
make doctor
make device-check
```

A camera sensor unable to provide 30 unique frames cannot be converted to 30 FPS by post-processing.
The app now distinguishes that condition from renderer overload and preserves visual quality.
