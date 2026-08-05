# Fluid pond workshop mode

## Installation behavior

1. **Idle screensaver** — the display is a fully procedural sky-blue pond. Layered directional and
   radial waves move continuously across the full surface; the camera image is hidden.
2. **Face acquisition** — the first stable face starts a transient vortex and expanding displacement
   ring at the tracked face center.
3. **Fluid camera merge** — the complete beautified camera frame fades into the water. The face
   analytic face ellipse only guides clarity; it never clips the image into a cut-out.
4. **Timed transformation** — beauty corrections and camera visibility progress over
   `revealDurationSeconds` (10 seconds by default).
5. **New visitor** — sustained departure or direct tracker hand-off restarts the reveal and vortex.
6. **Editor dismissal** — Return to pond or tapping outside the editor restarts the experience.

## Curator controls

- **Camera reflection** controls how much of the complete camera image merges into the pond.
- **Face clarity** protects facial readability while retaining a water reflection.
- **Image deformation** controls refraction of the camera image by the water normals.
- **Arrival swirl** controls the transient face-centered vortex.
- **Ripple movement** controls wave speed and amplitude.
- **Optical depth** shifts the surface between pale sky reflection and deeper blue water.
- **Transformation duration** is adjustable from 3 to 30 seconds.

Presets:

- **Sky pond** — balanced exhibition default.
- **Silk water** — cleanest reflection and minimal deformation.
- **Fluid** — stronger motion, vortex and refraction.
- **Gallery** — camera-forward merge with polished moderate water movement.

## Rendering order

1. Camera transform and true-mirror presentation
2. Pose-aware masks and geometry only when a beauty pass needs them
3. Skin, under-eye, lighting and feature beautification
4. Procedural sky-water wave field and optical shading
5. Face-centered arrival vortex and expanding ring
6. Full-frame camera/water merge with broad face-guided clarity
7. Final display/capture composite

## Performance

The pond remains one fullscreen GPU pass. Performance mode executes only the three base waves;
Medium/High add finer directional and radial waves. The new merge does not require a face mask during
early reveal, removing the old mask-generation cost that could consume several milliseconds per
tracking update. Existing adaptive quality protection still targets the 33.3 ms frame budget.

A sensor delivering fewer than 30 unique camera frames cannot be forced above its hardware cadence,
but renderer work is reduced before it becomes the bottleneck.

## Visitor privacy

No face identity, photograph, crop or biometric history is stored. Session restart is based only on
tracking presence and geometric discontinuity.
