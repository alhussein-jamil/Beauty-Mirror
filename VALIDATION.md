# Validation record

Revision 6.0.0 prepared on 2026-08-05.

## Completed in this environment

- Ran repository static checks.
- Parsed all Android XML and verified English/French string-key parity.
- Audited Kotlin pass / GLSL uniform parity for the fluid pond shader.
- Checked the shader for previously encountered reserved GLSL identifiers.
- Compiled the pure-Kotlin settings/interpolation/reveal core with `kotlinc`.
- Verified new water controls are clamped, persisted, migrated and interpolated.
- Verified the reveal controller and visitor-session restart logic remain unchanged.
- Verified pond-only early reveal no longer triggers landmark-mask rendering.
- Verified low quality executes three base waves while higher quality enables optional waves.

## Physical-device validation still required

The repair environment has no Android SDK, cached Maven graph or camera device, so run:

```bash
make doctor
make device-check
make workshop
make fps
```

On the exhibition phone, tune **Camera reflection**, **Image deformation** and **Arrival swirl** first.
The defaults are intentionally balanced rather than maximal.
