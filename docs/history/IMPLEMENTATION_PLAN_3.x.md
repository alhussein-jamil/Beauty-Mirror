# Beauty Mirror production redesign plan

## Product goal
Turn the current technical beauty-control demo into an exhibition-ready mirror experience that is immediate, understandable, visually premium, and resilient on heterogeneous Android phones.

## Experience principles
1. Camera first: the face remains unobstructed; the studio docks above or below the tracked face and stays stable for the editing session.
2. Result language: users choose a desired result (fresh eyes, even skin, lake reflection) before seeing technical sliders.
3. One obvious action per control: no overlapping gesture layers or invisible click interceptors.
4. Progressive disclosure: quick looks first, detailed corrections second, diagnostics last.
5. Exhibition-safe: controls can disappear, settings persist, the screen remains responsive, and expensive work degrades gracefully.
6. Natural water, not a pool filter: the lake scene uses dark marsh/lake tones, restrained ripples, and a face-preserving contrast window.

## Implementation phases

### 1. State and persistence
- Add a first-class reflection scene (`MIRROR`, `DARK_LAKE`).
- Persist lake intensity, motion, darkness, and face clarity.
- Preserve scene settings when changing beauty presets.
- Migrate older settings without resetting existing looks.

### 2. Rendering
- Add one GPU lake-reflection post-process pass after beautification and before final compare/composite.
- Use low-amplitude horizontal water displacement plus sparse radial ripples around the tracked face.
- Keep the palette dark olive/charcoal/brown rather than turquoise.
- Preserve enough local contrast around the face so corrections remain visible under the water treatment.
- Animate from monotonic time; include the pass in captured photos.
- Make shader complexity adaptive through a continuous performance budget.

### 3. Performance
- Keep discrete render-resolution tiers only as a last resort.
- Add a continuously interpolated pressure signal driven by average frame time, p95, slow-frame ratio, camera FPS, and analysis starvation.
- Reduce skin sample count, mask refresh frequency, optional detail, geometry, and lake sampling smoothly before changing resolution tiers.
- Degrade quickly and recover slowly to avoid oscillation.
- Keep core skin, under-eye, lighting, and lake identity visible at every tier.

### 4. UI/UX redesign
- Replace the oversized editor with a compact, polished face-safe studio sheet that chooses a stable top or bottom dock when opened.
- Add a direct Lake/Mirror scene action in the main dock.
- Reorganize studio pages into `Looks`, `Skin`, `Eyes`, `Lips`, `Shape`, `Scene`, and `System`.
- Use concise outcome cards and compact illustrated feature maps.
- Keep all sliders inside the sheet; never place parameter labels over the tracked face.
- Move camera switching to the top action area and keep capture central.
- Make all interactive targets at least 48 dp with explicit semantics.
- Close the studio through Done, back, or scrim tap without blocking other controls.

### 5. Reliability and cleanup
- Remove stale release notes and redundant UI paths.
- Add tests for scene persistence, adaptive pressure interpolation, and lake settings clamping.
- Add static shader/uniform checks and Makefile commands for build, install, exhibition, tests, FPS, and screenshots.
- Validate XML, shell scripts, shader interfaces, Kotlin source structure, and ZIP integrity.

## Acceptance criteria
- Mirror and lake scenes toggle immediately and persist.
- Dark-lake rendering remains readable while beauty corrections are visibly preserved.
- No UI element covers the central face while editing.
- All primary actions are reachable with one tap and have non-overlapping hit regions.
- Adaptive quality changes are gradual; resolution rebinding is infrequent.
- Compare shows the untreated camera image, including bypassing the lake scene.
- Captures match the processed preview.
