# Native foreground clouds — 2026-09-18

F9 **M** now captures native cloud geometry alongside terrain and supported frozen living entities. **N** toggles foreground cloud composition; OFF restores the previous background-only cloud cube for comparison. **Space** enables lensing. This remains a frozen integration experiment, separate from F10.

## Representation

Invoke the pinned WorldRenderer cloud-buffer builder, preserving its camera-relative transform, wind wrapping, vertex RGBA, texture UVs and culling. Borrow the native cloud texture; close the temporary vertex buffer. Respect cloud mode OFF and dimensions without clouds. Cloud triangles enter the shared BVH with separate material tags, without adding a thirteenth sampler.

Along each ray, retain the nearest cloud surface ahead of the nearest opaque hit, then blend it once over opaque terrain/entities or sky. Native FANCY clouds use a depth prepass, so this intentionally follows their nearest-surface behavior rather than accumulating all intersected faces. Cloud colour is already native shaded colour, without terrain lightmap multiplication. Fog follows native per-vertex observer-space distance and interpolation. Capture a sky-only cube while foreground clouds are enabled, avoiding duplicate background clouds. Switching N invalidates the cube cache.

The outgoing mesh escape sphere now encloses the actual BVH bounds, with a small source-centre margin, instead of always stopping at radius512. This includes the broader native cloud footprint; it does not certify remaining far-field deflection.

## Fixed-pose appearance checks

All pairs are full-resolution 854×480 vanilla versus unbent mesh. ON/OFF vanilla references are pixel-identical within each comparison. RGB MAE is normalized to 0–1; smaller is better. The cloud OFF column means background-only clouds, not Minecraft clouds disabled.

| Pose | Foreground ON | Background only | ON / OFF pair IDs |
| --- | ---: | ---: | --- |
| Daylight mountains, camera (16.5,303.62,-111.5), yaw0.281/pitch35 | 0.0071 | 0.0126 | 6819754196681512581 / 4766191610027561085 |
| Below clouds near terrain, camera (16.5,181.62,16.5), yaw0.281/pitch-65 | 0.0035 | 0.0120 | 9716014540523919532 / 17119921486166019939 |
| Clear night view below clouds, camera (16.5,191.62,-45.5), yaw0.281/pitch-35 | 0.0007 | 0.0013 | 11270848800471300808 / 8757514075849811300 |

Identity reports: cloud-above-repeat, cloud-below-repeat, cloud-open-repeat. Captures and reports remain under ignored run/interstellar-captures. Contact sheets inspected for all three poses. The middle pose has nearby partial terrain faces and is not the cleanest acceptance view; the final open-air pose supplies a clearer below-cloud check. Daylight lower-half error improves 0.0240→0.0134; missing terrain at capture edges remains the largest visible residual there. Final night pair has 0.17% of pixels exceeding 8/255 maximum-channel error, versus 1.36% with background-only clouds. These are straight-ray appearance checks, not independent curved-ray validation.

## Runtime and cost

cloud-build.log: successful build, 47 tests, zero failures/errors. cloud-runtime.log and cloud-final-runtime.log: shader/mixin startup, native cloud capture and comparison switches passed. Inspected run/cloud-final-lensed.png. Afterward, closed the preview and verified F10 source adoption and a fresh terrain snapshot, then reopened the mesh demo.

Final measured capture: 2,527,528 triangles including 2,816 cloud triangles, 94 supported living entities, zero missing sections, 19,739 omitted fluid/translucent incidences; 15.243 s capture/build/upload. FANCY clouds, height192, two-sided. Local voxel snapshot: 11,723 opaque, zero unknown, 28 unsupported; source N64 unchanged.

Diagnostic timing: RTX5070Ti, 427×240 internal / 854×480 window, standard paths, lensing/mobs/native lighting/clouds ON, r/r_s15.79470, 120 warmup and 300 samples. Optical GPU p50/p95/p99 = 10.798016/11.507328/11.547552 ms; sampled frame intervals = 11.9351/12.9182/13.0536 ms. GPU pass timing excludes capture/build/upload, native sky capture and upscale. This is not a target-resolution FPS claim or an optimization gate.

## Limits and next work

- Captured native clouds are bounded and camera-dependent, including native face omissions. Arbitrary curved multiple crossings or clouds intersecting the source are not certified. Single-surface composition is not volumetric transport or a general transparency solution.
- FANCY mode verified above/below clouds in daylight/night. OFF, FAST, Fabulous and other dimensions are not runtime-certified.
- Terrain remains source-centred 16×16 chunks/full height; unloaded sections and the footprint can leave visible holes. The 128-block camera guard remains. Broader camera-relative coverage is next.
- Fluids, special entity layers (glow, shadows, translucency), non-living entities and block entities remain incomplete. Curved fog remains an observer-space appearance approximation.
- No block, time, weather or population edits made. Only player navigation/mode changes and read-only air checks. Automated movement/flicker checks remain deferred to the owner. AA work stays separate.
