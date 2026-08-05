# Validation record

Revision 5.0.0 prepared on 2026-08-05.

## Completed in the repair environment

- Ran `tools/static-checks.sh`, including model checksum, shader inventory, scoped Compose import
  guards, reserved GLSL identifiers and Kotlin-pass/GLSL uniform parity.
- Parsed every Android XML resource and verified English/French string-key parity.
- Checked shell scripts and Makefile installation targets.
- Compiled the pure-Kotlin tracking/reveal/session core with `kotlinc`.
- Executed a reveal/session simulation: completion at ten seconds, explicit restart, sustained
  absence rearm, and two-result direct tracking hand-off restart.
- Updated Robolectric default-setting assertions for the new workshop pond schema.
- Verified the 3–30 second setting is clamped, persisted and migrated with a 10 second default.
- Verified the pond shader contains no reserved GLSL ES identifiers, has matching uniforms, clamps
  face refraction, avoids camera sampling in its idle path, and removes optional work at low quality.
- Verified the final archive with `unzip -t`.

## Performance design validated in source

- No-face mode renders one procedural pond pass and performs no landmark-mask rendering.
- Early visitor reveal renders and blurs only the face-oval mask; seven auxiliary masks are deferred
  until beautification passes become visible.
- Landmark masks are cached between analysis updates and refreshed less frequently under pressure.
- Low/Performance pond quality removes one ripple source, a noise octave, particles and the second
  face sample while keeping the face-readable installation behavior.
- The existing renderer protection evaluates average/p95 frame time, slow-frame ratio, camera
  cadence and analysis cadence against the 33.3 ms budget.
- PERFORMANCE uses 480p rendering, 128 px masks, 6 Hz analysis and two skin samples while retaining
  core skin, under-eye and face-lighting correction.

## Not executable in the repair environment

The environment cannot download the Gradle distribution/Maven dependencies and has no Android SDK,
connected device or camera. Full Android compilation and physical camera/art-direction validation
remain required on the target machine:

```bash
make doctor
make device-check
make workshop
```

A camera sensor unable to provide 30 unique frames cannot be converted to 30 FPS by post-processing.
The app distinguishes sensor-limited cadence from renderer overload and avoids unnecessary quality
loss in that case.
