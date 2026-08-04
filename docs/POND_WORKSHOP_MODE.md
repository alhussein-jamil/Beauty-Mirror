# Pond workshop mode

## Intended experience

The phone behaves as a reflective pond rather than a camera-filter editor. After configuration is
closed, the interface disappears and the camera image is rendered through a muted, irregular water
surface. The visitor initially sees a murky reflection. Over the configured duration, the pond
settles around the tracked face and the selected beauty corrections progressively accumulate.

Default duration: **10 seconds**. Supported range: **3–30 seconds**.

## Transition state machine

A visitor session starts when a stable face becomes visible. The reveal controller progresses in
real elapsed time, independent of camera frame rate, using a smoothstep curve. Brief detector misses
do not restart or visibly pause the work.

The reveal is restarted by:

- pressing **Return to pond**;
- tapping outside either editor sheet;
- entering exhibition mode;
- a new face appearing after a sustained absence;
- a direct one-face tracker hand-off with a sustained implausible position/scale discontinuity.

No face embedding, photograph, biometric identifier or identity history is created.

## Rendering order

1. Camera transform and true-mirror presentation
2. Pose-aware masks and face geometry
3. Skin / complexion / blemish correction
4. Under-eye correction
5. Face lighting and shine control
6. Eye, brow, lip, teeth, blush and contour enhancement
7. Optional restrained face geometry
8. Pond reflection and refraction
9. Final display/capture composite

The reveal value scales the actual beauty graph, not merely pond clarity. Therefore the face visibly
changes the longer the visitor looks.

## Pond visual language

The shader follows the supplied workshop references:

- low-saturation slate-grey / peat-green absorption;
- broad irregular murk rather than turquoise water;
- sparse stable mineral specks and occasional dark debris;
- wide pale reflected-light bands;
- slow concentric arrival ripples with alternating crests/troughs;
- tightly clamped refraction so the face remains legible;
- no hard circular portal or swimming-pool wave pattern.

## Performance protection

The target presentation cadence is 30 FPS. The app monitors average and p95 render time, slow-frame
ratio, camera cadence and analysis cadence. It first reduces continuous sample/mask budgets, then
steps through High → Medium → Low → Performance only after sustained overload.

In Performance mode:

- render height targets 480 px;
- masks use 128 px;
- analysis targets 6 Hz;
- smoothing uses two samples;
- optional geometry, detail restoration and feature styling are disabled;
- pond extra samples, secondary ambient ripple and particles are disabled;
- core skin, under-eye and lighting correction remain active.

A camera sensor that supplies fewer than 30 unique frames cannot be forced above its hardware
cadence; the controller distinguishes that case from renderer overload and avoids needlessly
removing visual quality.

## Workshop operation

```bash
make workshop
make fps
```

`make workshop` builds, installs and launches the mirrored Stage preset in pond mode with the UI and
system bars hidden. Tap the reflection to reopen controls. Use Studio → Scene to tune water and
transition duration, then press Return to pond.
