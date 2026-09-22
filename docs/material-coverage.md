# Native material coverage — experiment in progress, 2026-09-22

**Not yet accepted under the owner's FPS floor.** Packaging checkpoint24958d8 remains the last pushed demo branch. This work continues step4; secondary-image refinement and the requested subsequent algorithm review remain outstanding.

## Implemented and exercised

- Capture native non-living entities and block-entity renderers, preserving first-person camera-body exclusion. Chests, beds, a boat and a dropped item were checked in the separate demo dimension. Block-entity lists inspect loaded chunks' maps once per game tick; no block-array scan or generated chunks.
- Capture native fluid quads, preserving section-relative positions, native biome colour, face shading and light. Lava shares the opaque path; water uses native texture alpha. Translucent baked block models and normal entity/item translucent layers enter the same curved scene. Unsupported additive effects, glint, shadows, text and some special block-entity layers remain explicitly omitted.
- Composite transparent hits in ray order; continue the remaining part of the same optical chord after each transparent hit. Fancy clouds still contribute only their nearest face, now ordered relative to other transparent surfaces.32-layer budget; residual transmission below0.001 terminates. This models vanilla surface blending, not physical water refraction or absorption.
- Entity textures with only0/255 alpha use the existing cutout path when vertex alpha is opaque. Layer flags are consistent across each quad. Guard unsupported/multi-texture phases before casting the texture accessor.

At the first visual checkpoint, stacked red/blue stained glass and a glass-fronted water tank showed the expected background and tint; native boat/bed/chest/item geometry was visible. Screenshots `2026-09-22_15.34.18.png` and `2026-09-22_15.46.39.png` inspected. Fixtures are in the dedicated demo dimension only; they have not yet been added to the deterministic packaged exhibit.

## Validation

`selective-material-build.log`: build/53 unit tests pass. `selective-material-runtime.log`:26240 sampled optical comparisons, zero mismatch/inconclusive/unresolved;24 analytic GPU material cases, zero channel error. Material cases cover two ordered layers, two-sided entity alpha, opaque occlusion, cloud/transparent ordering, native nearest-cloud semantics, reversed input and empty-cache variants. These are sampled checks, not general Minecraft material certification.

Full and selective compositors produce byte-identical PNGs in frozen wall/down pairs3076519621853606379 and16400811295145407760. The first pair is854x480; the second2560x1440. Pair metadata preserves camera/settings. The selective path traces an initial material probe, copies its alpha mask to a separate framebuffer, and retraces only marked rays with full composition. No texture/framebuffer feedback or unbent overlay. Extra mask storage isRGBA8 at2x logical width.

Found during review: a fixed ray-direction offset can round back onto a transparent surface at grazing incidence. A normal-directed, coordinate-scaled offset and an additional grazing GPU case are implemented in the working tree but **not yet runtime verified** as of this checkpoint.

## Performance gate — currently failing

Same natural-world camera `(16.5,302,-45.5)`, N65,r_s8.125, yaw.281, wall pitch.91/down35.91,1440p output/half-scale2xAA,120 warmup/300 samples. Today’s pre-coverage baseline has6092246 terrain triangles; coverage has6260820. Original older79aa1d0 evidence remains in triangle-row.md; do not replace it with easier demo measurements.

| Live implementation | Wall GPU/frame p50 ms | Down GPU/frame p50 ms |
| --- | --- | --- |
| Pre-coverage24958d8 (unchanged renderer) |20.550/21.676|32.145/33.297|
| Full material path |26.654/27.673|38.058/39.234|

Independent live runs with evolving actors/time, not same-frame speedup proof. Logs demo-packaging-final-runtime.log and materials-resume-runtime.log.

Frozen same-scene comparison, selective-material-runtime.log:

| Mode | Wall GPU p50/p95/p99 ms | Down GPU p50/p95/p99 ms |
| --- | --- | --- |
| Full |24.784/26.737/27.347|38.419/39.732/40.330|
| Selective |22.204/23.680/23.906|38.359/39.622/40.311|

Selective helps wall but does not recover the terrain-heavy deficit. Not accepted as the final default. The first nested32-layer traversal design also linked shaders excessively slowly; the current curved integrator resumes a pending chord in its outer loop. Cold material shader registration still takes roughly100seconds on this driver; shader-cache warm startup must be checked separately.

## Active next experiment

Retain .02-radian reference; expose .04/.08 for measurement. Keep near-critical rays with dimensionless impact-squared within.005 of27/4 at.02. Retain nominal1mm curvature target and16-block spatial cap; remove the old .45-block minimum when it would exceed the curvature target in the wider-step experiment. This changes numerical sampling and requires the independent fixture, multiple fixed-pose image pairs, visual inspection and FPS checks before acceptance. F9 `[` cycles angular cap; Shift+`[` captures against.02. F9 Z/Shift+Z compares selective/full materials. No acceptance or new FPS claim yet.
