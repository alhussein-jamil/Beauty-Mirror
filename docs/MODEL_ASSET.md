# Face Landmarker model asset

The app bundles the MediaPipe Face Landmarker task model at:

`app/src/main/assets/face_landmarker.task`

Expected SHA-256:

`64184e229b263107bc2b804c6625db1341ff2bb731874b0bcc2fe6544e0bc9ff`

The build never downloads a model at runtime. `tools/static-checks.sh` verifies the file and checksum before builds. When replacing the model, update the checksum, validate landmark compatibility, document the source/version, and confirm its redistribution terms.
