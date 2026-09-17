# Native fog and boundary check — 2026-09-17

Owner reports improved nighttime sky, with a small apparent viewport/size jump and different mountain shading when crossing the F10 viewing boundary.

## Change

The distant renderer blended terrain into sky colour between source-relative radii80–128. This was an artificial coverage fade, unrelated to Minecraft fog, and could change mountain colour as the view crossed the boundary. Replace it with Minecraft's actual terrain fog start/end, colour/alpha and spherical/cylindrical distance mode, captured at `WorldRenderEvents.BEFORE_ENTITIES` before vanilla clears fog for the HUD. Apply the same rule to supported opaque local and distant hits. Capture metadata now records the fog inputs.

Checked the pinned Minecraft1.21.1 mapped `WorldRenderer` bytecode and runtime `assets/minecraft/shaders/include/fog.glsl`: spherical distance is endpoint distance; cylindrical distance is max(horizontal distance, absolute height difference), followed by smoothstep between start/end. On curved rays, endpoint distance remains an appearance approximation, not integrated physical optical depth. Missing geometry, placeholder water and diagnostic colours remain outside this opaque-terrain parity claim. Removing the artificial fade does not expand coverage and can expose finite height-field edges.

## Boundary diagnosis

At camera(16.5,303.6199998855591,-111.5), yaw0.281/pitch35, the same-frame vanilla and zero-bending backend have matching effective FOV77 degrees and aligned calibration-platform geometry. This does not demonstrate another fixed-FOV bug. The remaining mountain discrepancy includes simplified terrain geometry, light/AO sampling and filtering. The existing hard128-block activation limit still switches off nonzero lensing, which can produce an apparent size jump even with matched camera projection. It remains unresolved; no lensing fade or modified gravitational equations were introduced to hide it. Moving camera effects are not certified by a fixed-pose comparison.

## Evidence

- `native-fog-build.log`: build passes,45 existing tests with zero failures/errors. No ray equations/intersection changes; no new curved-solver certification.
- `native-fog-runtime.log`: shader rendering succeeds. Pair `run/interstellar-captures/pair-9808688224094421306` uses cylindrical fog start172.8/end192 and colour(0.03856888,0.04338999,0.076836444), alpha1.
- Pair RGB MAE: full0.0211, top-third0.0029, bottom-half0.0397; full linear-luminance MAE0.0060. Contact sheet inspected; mountain geometry/shading still differs. This is nighttime parity evidence, not a controlled before/after improvement or daylight certification.
- Two fixed positions straddling the limit logged automatic F10 pause outside and recovery inside. This tests the reported boundary, not the owner-deferred movement/flicker suite.
- Established far pose, N64/11700 opaque, standard, 1440p output/half-resolution tracing,120 warmup/300 samples: GPU p50/p95/p99=9.508928/9.645760/9.702272ms; frame intervals10.8540/11.7428/11.9791ms. Optical GPU timer excludes capture/upload, native sky capture and upscale; scene-specific frame samples are not a universal performance guarantee.

Client left F10 enabled, creative flight, far reference(16.5,302,-85.5), yaw0.281/pitch0.91, window870x519. No blocks edited; AA remains separate. Next address actual terrain lighting/geometry and continuous useful coverage, including the hard viewing boundary. Keep fixed-pose captures to distinguish approximation errors from intended lensing.
