# Beauty Mirror 3.1.0

## Exhibition scene

- Added a toggleable dark lake / marsh / well reflection.
- Water uses olive, charcoal and brown tones with restrained ripples rather than turquoise waves.
- Beautification runs before water treatment, and a tracked clarity window keeps corrections visible.
- Visitor detection drives a stable reveal that reaches approximately 99% within 0.9 seconds.
- Missed tracking frames fade slowly instead of flashing the treatment on and off.

## UI and interaction

- Reorganized Studio into Looks, Skin, Eyes, Lips, Shape, Scene and System.
- Studio opens opposite the tracked face and stays there while the visitor poses.
- Added subtle live region graphics while parameters are edited.
- Reduced redundant lake actions; the effect remains directly available from the main dock.
- Added deterministic 48dp+ targets, test tags, exactly-once switch behavior and back/dismiss flows.

## Performance

- Replaced abrupt capping with continuous interpolation of samples, optional effects and mask cadence.
- Resolution changes occur only after sustained overload and recover slowly to prevent oscillation.
- Added camera-limited cadence detection so a 24 FPS sensor does not force the renderer to destroy quality.
- Corrected p95 measurement to nearest-rank behavior.
- Cached lake face bounds and reduced the water shader to one texture tap under pressure.

## Validation

See `VALIDATION.md`. The source passed static, shader/uniform, resource, Kotlin syntax and pure-core
simulation checks. Full Android Gradle, Compose instrumentation and sustained camera/thermal tests
must be run with `make device-check` on the target phone.
