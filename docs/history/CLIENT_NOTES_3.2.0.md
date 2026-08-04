# Beauty Mirror 3.2.0 — client-driven refinement

## What changed

### 1. Stronger beautification
- Stage and Glam presets are noticeably stronger.
- Overall intensity now ramps up more aggressively.
- Added a one-tap **Reflection++** quick action for a clearly visible benefit.
- Under-eye, complexion, lip, contour and subtle morphing controls are pushed further while staying bounded.

### 2. Better dark-water art direction
- Reworked the lake shader to feel more like a **dark pond / marsh / well** instead of a swimming pool.
- Water now has a darker olive / peat / graphite palette.
- The face remains readable through the water thanks to a protected **clarity portal** centered on the detected face.
- Added ring-based surface ripples around the face for a stronger “reflection transformation” moment.
- Visitor reveal transition was sped up so the reflection changes in under a second.

### 3. Better scene UX
- Added 4 scene moods:
  - **Still well**
  - **Marsh**
  - **Soft ripple**
  - **Reveal++**
- Added a direct **face clarity** slider in the quick lake panel.
- Added a scene hint to guide non-technical operators.

## Recommended exhibition setup
- For maximum wow effect while keeping the face legible, start from:
  - **Scene:** Reveal++
  - **Preset:** Glam or Stage
  - **Quick action:** Reflection++
- If the image must stay calmer and more readable, use:
  - **Scene:** Still well
  - **Preset:** Stage

## Notes
- I could not run a full Gradle/device build in this offline environment because the Gradle distribution could not be downloaded.
- XML resources and code/resource references were sanity-checked locally.
