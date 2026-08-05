# Pond workshop mode

## Installation behavior

1. **Idle screensaver** — the shader renders a living grey-green pond with slow murk drift,
   reflected light, sparse sediment and independent expanding rings. The camera image is not shown.
2. **Face acquisition** — tracking creates a blurred landmark face mask. Only that masked face is
   composited over the pond; the surrounding room remains water.
3. **Timed reveal** — the face is visible immediately as a mirror, while water veil, color return
   and the real beauty graph settle over `revealDurationSeconds` (10 seconds by default).
4. **New visitor** — a real departure or direct tracker hand-off restarts progress without storing
   or recognizing biometric identity.
5. **Editor dismissal** — tapping outside or returning to the pond starts the transformation again.

## Art direction

The target is a murky pond / marsh / well rather than a pool: low saturation, peat shadows,
soft workshop or sky reflections, restrained rings, no cyan caustic net, and no opaque water layer
covering the visitor's face.

## Intended experience

The phone behaves as a reflective pond rather than a camera-filter editor. After configuration is
closed, the interface disappears. With no visitor, the display is entirely procedural water. Once
a face is tracked, only the softly feathered face oval is composited over the pond; the room and
camera background remain hidden. Over the configured duration, the face corrections progressively
accumulate while the water veil becomes almost imperceptible.

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

No photograph, face crop, biometric identifier or identity history is stored or transmitted.

## Rendering order

1. Camera transform and true-mirror presentation
2. Pose-aware masks and face geometry
3. Skin / complexion / blemish correction
4. Under-eye correction
5. Face lighting and shine control
6. Eye, brow, lip, teeth, blush and contour enhancement
7. Optional restrained face geometry
8. Procedural pond screensaver/background
9. Landmark-mask face insertion with restrained refraction
10. Final display/capture composite

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
