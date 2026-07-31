# Beauty Mirror 1.4.0

This release addresses the case where the app appeared to apply only face brightening.

## Effect pipeline

- Reworked skin smoothing into a stronger two-scale edge-aware filter.
- Increased the useful smoothing radius and made it adapt to render resolution and face size.
- Strengthened complexion evening and selective redness correction.
- Strengthened under-eye correction without turning it into a flat bright patch.
- Increased eye clarity, lips, blush, contour, face slimming, eye enlargement and nose refinement response.
- Reduced exposure in Stage and Glam so brightness no longer visually dominates the makeover.
- Added all dedicated lip parameters to the feature-pass activation condition.

## Presets

- Natural remains restrained but is now visible.
- Soft applies a clear but conservative makeover.
- Stage is tuned for a visibly demonstrable exhibition result.
- Glam is intentionally strong.

## Migration

Legacy Custom looks are automatically rebased onto the stronger effects curve the first time this version is opened. Existing Stage/Glam preset selections automatically use the new tuning.

## UI

- Added a smoothing-radius control.
- Updated the comparison hint so visitors can hold the compare button to verify the complete effect stack.
