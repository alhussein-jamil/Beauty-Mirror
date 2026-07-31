# Architecture and invariants

## Threads

- **Main:** Compose state and user interaction
- **Camera executor:** CameraX callbacks and surface requests
- **Analysis HandlerThread:** ImageAnalysis and MediaPipe lifecycle
- **GL HandlerThread:** EGL context, SurfaceTexture, render graph, and capture readback
- **Dispatchers.IO:** pixel conversion, JPEG encoding, and MediaStore writes

No camera, MediaPipe, or GL work belongs inside a composable.

## Preview path

1. CameraX requests a preview surface.
2. `PreviewSurfaceProvider` passes the exact requested dimensions to `BeautyRenderer`.
3. The renderer configures `SurfaceTexture.setDefaultBufferSize` before returning its persistent `Surface`.
4. `CameraInputPass` applies the producer transform matrix, centered aspect-fill crop, and the explicit front-camera mirror.
5. Optional effect passes operate on reusable textures and FBOs in that presentation space.
6. `FinalCompositePass` chooses processed/original output and presents to the `SurfaceView`.

The frame-available callback already runs on the GL handler and renders directly, avoiding a second queue of stale draw requests.

## Analysis ownership

`ImageAnalysis` uses `STRATEGY_KEEP_ONLY_LATEST`. `FaceLandmarkerEngine.detectAsync` returns whether it took ownership of the `ImageProxy`.

- Accepted MediaImage path: the engine closes the proxy after MediaPipe's result/error callback.
- Rejected or bitmap-fallback path: the CameraX analyzer closes the proxy.
- Only one request is in flight.
- Camera switches increment a generation. Late results are ignored but still release their matching image and clear the in-flight gate.
- `FaceLandmarker.close()` runs on the analysis looper before any remaining proxy is released.

This prevents native MediaPipe from reading an already-closed `android.media.Image`.

## Coordinates

| Space | X mirror | Y direction | Consumer |
|---|---:|---:|---|
| MediaPipe normalized | no | down | tracking input |
| Aligned preview | no | down | common mapping |
| Display landmarks | front only | down | debug overlay |
| GPU mask texture | front only | up | render passes |
| Final screen/photo | front only | output | composite |

The MediaImage path asks MediaPipe to rotate and then maps with `rotationDegrees=0`. The bitmap fallback keeps sensor orientation and applies rotation in `FaceCoordinateMapper`. Rotation must never be applied twice.

The mapper and camera shader both use centered aspect-fill semantics. `AspectCrop` has pure JVM tests for equal, wide, tall, and invalid dimensions.

## Masks

`FaceMaskGenerator` creates reusable polygon geometry for:

- face/skin
- protected detail
- eyes
- packed left/right under-eye regions
- lips and mouth interior

`MaskTextureRenderer` rasterizes and blurs masks at a quality-dependent reduced resolution. Effects use soft alpha and tracking opacity; no hard face boundary is composited.

## Render graph

```text
OES camera + mirror
  -> original RGBA
  -> pose-aware face warp (optional)
  -> skin smoothing / complexion correction (optional)
  -> pose-aware under-eye correction (optional)
  -> face lighting (optional)
  -> detail restoration / eye clarity (optional)
  -> feature enhancement (optional)
  -> warmth (optional)
  -> dark-lake reflection (optional, post-beauty)
  -> final untreated/processed selection
```

Expensive passes are bypassed when their effective strength is zero. Detail restoration only runs for preservation after another pass modified the image, while eye clarity may run independently.


## Dark-lake exhibition scene

`LakeReflectionPass` runs after all beauty corrections so skin, eye, lip, and under-eye changes remain readable beneath the water treatment. The shader uses restrained horizontal displacement, sparse face-centered arrival ripples, and dark olive/charcoal/brown grading. A soft elliptical clarity window around the tracked face limits displacement and darkness where facial detail matters.

`VisitorRevealController` converts face presence into a frame-rate-independent reveal value: the enhanced reflection reaches approximately 99% within 0.9 seconds and releases more slowly after tracking loss. The reveal is monotonic under stable detection, so intermittent landmark misses do not make the scene pulse. The scene is bypassed for press-and-hold compare but is included in processed captures.

## Capture

Capture reuses the last camera frame already consumed by the preview loop. It does not call `SurfaceTexture.updateTexImage` a second time. The graph renders to an offscreen FBO, performs one `glReadPixels`, restores preview dimensions in `finally`, then returns the buffer. Conversion, vertical flip, JPEG compression, and MediaStore I/O run on `Dispatchers.IO`. A mutex serializes captures and protects the reusable buffer.

## EGL lifecycle

`GlContextManager` always keeps a valid pbuffer when no window surface is available. It does not depend on optional surfaceless-context support. Window surfaces are checked for validity, EGL failures include error codes, and all GPU resources are destroyed on the GL thread before the context terminates.

## Adaptive quality

The user-selected quality is a ceiling. `AdaptiveQualityController` first produces a continuous pressure signal from average frame time, nearest-rank p95, slow-frame ratio, camera cadence, and analysis starvation. `RenderGraph` interpolates sample count, optional-effect scale, geometry/detail scale, and mask refresh interval from that pressure before any render-resolution tier changes.

Pressure rises quickly and recovers slowly to avoid visible oscillation. A discrete quality-tier rebind occurs only after sustained stress; recovery requires a longer stable window. When camera FPS is low but renderer average/p95 and slow-frame ratio are comfortable, the controller marks the source as camera-limited instead of unnecessarily stripping visual quality. Runtime adaptation is never persisted and leaves the user's selected strengths unchanged.

## Privacy and release

- Source and merged release manifests allow `INTERNET` for GitHub OTA only; `ACCESS_NETWORK_STATE` must stay removed.
- The model and shader assets are bundled and verified.
- Release signing is supplied externally; debug signing is never used for release.
- The unused FileProvider was removed.

## Pose-aware beauty graph

The camera-input pass establishes the final presentation space, including the optional front-camera mirror. Landmark mapping applies the same mirror before converting to OpenGL UV coordinates, so preview pixels, masks, geometry effects, diagnostics, and captures remain aligned.

The live face mesh produces smoothed yaw, pitch, and roll estimates. Face-local anchors are derived every frame from the stabilized mesh. Geometry and asymmetric feature effects use per-side visibility and pose attenuation so the compressed or occluded side fades instead of being dragged at steep angles.

Render order:

```text
camera/mirror -> face warp -> skin/complexion -> under-eye -> lighting
              -> detail restoration -> feature enhancement -> color
              -> dark-lake reflection -> composite
```
