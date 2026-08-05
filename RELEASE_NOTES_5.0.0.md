# Beauty Mirror 5.0.0 — Workshop pond installation

## Client direction implemented

- **No visitor:** a continuously animated pond screensaver. It is generated on the GPU and does
  not expose the room or front-camera feed.
- **Visitor arrives:** MediaPipe detects the face, the real landmark oval is softly feathered, and
  only the beautified face is inserted into the pond background.
- **Readable reflection:** water refraction over the face is deliberately tiny; the supplied demo
  video was used only as interaction reference, not as its bright swimming-pool visual style.
- **Timed transformation:** the face improvements accumulate over an adjustable 3–30 seconds,
  default 10 seconds.
- **Fresh visitor:** the timer restarts after a sustained departure/re-entry and after a direct
  tracker hand-off with a large position/scale discontinuity. Closing either editor also restarts it.

## Performance

- Idle mode avoids camera-color samples in the pond shader.
- Early face reveal renders only the one face mask; auxiliary skin/eye/lip masks are deferred until
  their effects become visible.
- Low/Performance quality removes extra pond noise, a ripple source, particles and secondary face
  sampling while preserving the installation concept.
- Existing average/p95/slow-frame 30 FPS protection remains active.

## Validation limitation

Source/static validation was run in this environment. A complete Android Gradle build and physical
camera/art-direction validation must be run on the exhibition phone.
