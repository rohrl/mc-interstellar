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

## Later experiments — 2026-09-22

Grazing fix verified:28 analytic GPU cases pass with zero channel error, including near-parallel planes at coordinate512. The normal offset is max(0.1mm,4e-7 times largest coordinate magnitude), normally0.1–0.3mm within this scene. Still an approximation; do not claim exact coplanar multilayer composition. Both.04/.08 angular caps pass26240 optical samples. .08 adds negligible speed over.04 and is not a preferred setting.

`refinement-experiments-runtime.log` tests.04 angular cap with2/4mm nominal curvature targets (same near-critical angular guard, AA/resolution/coverage). Both pass26240 optical samples and28 material cases. Wall2mm selective GPU p50/p95=17.362/19.347ms; down2mm selective31.953/32.969ms, full31.147/32.290ms. Wall4mm selective16.219/18.027ms; down4mm selective30.801/31.779ms. These are frozen experiments, not final live baseline acceptance. Compared with.02/1mm, wall2mm pair16359643443451799546 has RGB MAE0.00015329 and0.0688% pixels over8 levels; down4mm pair2484438275258875559 has0.00015699 and0.0674%. Fine silhouettes change slightly; these are not pixel-identical.

AA experiments use a stronger reference: full-resolution4ray with fine path,.02 angular cap and1x tolerance multiplier. A new standalone Java tool, `tools/CompareSecondaryImages.java`, measures a geometrically defined incoming-ray annulus (impact squared27/4..8), plus a narrow photon-edge region6.70..6.85. Source position/radius are explicit arguments; this region is a proxy for secondary images, not proof that every included ray is a returning image. It writes native-resolution crops and metrics, without rescaling/alignment.

- Alternating sample diagonals: whole-image MAE improved~0.1%, but annulus error worsened~2.2% down and0.5% wall. Rejected; preserve original two-sample pattern. Pairs4359127467756100378/14963823024872105283 down and9385226905264618458/10487168026268009660 wall. A wall contact image was inspected; two-ray reconstruction remains coherent but does not match the finest subpixel ring detail.
- Four-ray photon-edge experiment: two extra ray draws only in impact-squared6.70..6.85, averaged in float32 before final8bit conversion. Edge MAE improved0.11192→0.10093 wall and0.10969→0.10232 down. Cost is unacceptable: wall selective GPU16.226→24.459ms; down30.573→36.216ms. Rejected under the FPS floor. Log ring-aa-runtime.log; pairs1593864463012078008/2437233226502186090 wall and14132499573160211185/3117977260108173289 down. Retain evidence, remove the extra-ray implementation from the final candidate.

Next bounded AA trial: orient the existing two samples across the near-critical radial gradient, without adding rays. Then finish final live baseline comparison and packaging. Full special-effect coverage (additive eyes/glint, signs/text, particles, coplanar material overlays, boat water masks) remains outside this demonstrated subset; do not imply those layers work.
