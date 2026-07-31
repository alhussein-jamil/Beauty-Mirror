# Validation record

Revision 2.0.0 prepared on 2026-07-30.

## Completed in the repair environment

- Ran `tools/static-checks.sh`, including model checksum, shader inventory, privacy guards,
  generated-file guards, and the Compose `weight` import regression guard.
- Parsed every Android XML resource and every repository JSON document.
- Validated all shell scripts with `bash -n` and dry-ran the main Makefile workflows:
  `apk`, `phone`, `expo`, `fps`, and `perf`.
- Scanned 101 Kotlin and GLSL files for malformed delimiters, unterminated strings/comments,
  and merge markers.
- Audited every Kotlin render-pass uniform lookup against its matching GLSL declaration.
- Checked shaders for the reserved GLSL local identifiers that previously broke startup.
- Compiled the pure-Kotlin settings/preset/quality core.
- Compiled the adaptive-quality controller against the complete frame-timing snapshot contract.
- Compiled the frame-timing collector with a minimal Android clock stub.
- Simulated adaptive demotion `HIGH -> MEDIUM -> LOW -> PERFORMANCE` under overload and
  controlled recovery after a sustained comfortable window.
- Performed a Kotlin parser pass over the rewritten Compose UI. Android/Compose symbols are
  intentionally unresolved without the Android classpath, but no Kotlin syntax errors were found.
- Added/updated regression tests for PERFORMANCE-stage shedding, p95/slow-frame telemetry,
  adaptive hysteresis, new feature settings, and settings persistence.
- Verified the final source archive with `unzip -t` and checked that it excludes SDK directories,
  Gradle caches, `local.properties`, VCS metadata, build outputs, and secrets.

## Performance design validated in source

- Preview requests a 30–60 FPS range through CameraX.
- Frame protection evaluates camera FPS, average render time, p95 render time, slow-frame ratio,
  and analysis starvation every 500 ms.
- Severe overload lowers one profile after roughly 500 ms; sustained overload after roughly
  1.25 seconds; recovery waits for eight comfortable seconds.
- The hidden PERFORMANCE profile uses a 480-pixel render height, 128-pixel masks, 6 Hz analysis,
  two smoothing samples, cached masks, and disables geometry, detail restoration, and optional
  feature styling while preserving core skin, under-eye, and lighting correction.

## Not executable in the repair environment

The environment cannot resolve the Gradle distribution or Android/Maven dependencies and has no
attached Android device. Therefore the complete Android compilation and physical-camera performance
pass remain to be run on the target machine:

```bash
make doctor
make phone
make perf
```

The expected debug artifact is `releases/beauty-mirror-debug.apk`.

A camera sensor or vendor camera session that cannot supply 30 FPS cannot be converted into 30
unique camera frames by post-processing. The adaptive system protects renderer latency and sheds
optional work, but the exhibition phone still requires a sustained physical-device test under its
actual lighting and thermal conditions.
