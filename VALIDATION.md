# Validation record

Revision 4.0.0 prepared on 2026-08-04.

## Completed in the repair environment

- Ran `tools/static-checks.sh`, including model checksum, shader inventory, scoped Compose import
  guards, reserved GLSL identifiers and Kotlin-pass/GLSL uniform parity.
- Parsed every Android XML string resource and validated all shell scripts with `bash -n`.
- Dry-ran `make workshop` through build, APK copy, install and exhibition launch commands.
- Compiled the pure-Kotlin settings/tracking core with `kotlinc`.
- Executed a reveal/session smoke test: visible progress after two seconds, completion at ten
  seconds and a two-result direct visitor hand-off restart.
- Verified the 3–30 second setting is clamped, persisted, migrated and preserved across presets.
- Added JVM regression tests for reveal timing, brief detector misses, sustained absence, ordinary
  visitor movement and direct tracking hand-off.
- Verified the pond shader stays one pass, clamps refraction, removes extra taps/particles in
  Performance mode and contains no reserved GLSL ES identifiers.

## Performance design validated in source

- The renderer budgets against 33.3 ms and evaluates average/p95 frame time, slow-frame ratio,
  camera cadence and analysis cadence every 250 ms.
- Beauty work grows with visitor reveal, so expensive geometry and feature passes are skipped during
  the earliest part of the transition when their visual contribution would be negligible.
- Landmark masks are reused between analysis results and refreshed less often under pressure.
- The pond uses three texture taps only when quality permits; Performance mode uses one tap and
  removes the second ambient ripple, particles, geometry, detail restoration and feature styling.
- PERFORMANCE uses 480p rendering, 128px masks, 6 Hz analysis and two skin samples while retaining
  core skin, under-eye and face-lighting correction.

## Not executable in the repair environment

The environment cannot download the Gradle distribution/Maven dependencies and has no Android SDK,
connected device or camera. Full Android compilation and physical camera validation remain required
on the target machine:

```bash
make doctor
make device-check
make workshop
```

A camera sensor unable to provide 30 unique frames cannot be converted to 30 FPS by post-processing.
The app distinguishes sensor-limited cadence from renderer overload and avoids unnecessary quality
loss in that case.
