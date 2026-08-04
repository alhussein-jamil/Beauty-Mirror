Beauty Mirror 1.3.2

Changes in this package:
- Reworked front-camera mirroring compensation to honor CameraX transformation mirroring metadata and avoid reversed left/right behavior.
- Fixed pose semantics for mirrored front preview by mirroring yaw/roll into display space and falling back from bad transformation-matrix pose estimates when they disagree with landmark-based pose.
- Improved diagnostics overlay layout so tracking and timing panels no longer pile on top of the main top chrome.
- Added dedicated Lips controls page with Lip enhancement, Lip tint, Lip definition, and Lip gloss sliders.
- Extended feature-enhancement shader with dedicated lip tint / definition / gloss controls.
- Bumped app version to 1.3.2.

Note:
- Gradle compilation was not executed in this environment because the wrapper needs to download Gradle from services.gradle.org and outbound network access is blocked here.
