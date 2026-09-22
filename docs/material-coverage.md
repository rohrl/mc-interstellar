# Native material coverage — accepted refinement, 2026-09-22

**Accepted for the demonstrated material subset after the final FPS/quality checks below.** This completes the bounded step4 pass: native materials/models broaden, current AA is retained after measured alternatives fail, and the [subsequent algorithm review](performance-review-2026-09-22.md) identifies the next experiments. This is not universal Minecraft rendering support.

## Final acceptance

Default maximum angular cap0.08 and nominal4mm local chord target apply at observer radius6r_s and beyond. Through4r_s retain0.02/1mm; smoothly blend between them during CPU shader setup. Near-critical rays still retain the0.02 angular cap. Resolution, two-ray AA,16-block spatial cap, source/terrain coverage and live actor updates are retained.

Build/package and53 unit tests pass (`final-quality-build.log`). Actual shader startup succeeds. Final GPU fixture:52,480 comparisons over680 distinct directions at radii16/24/32/40/48/96/148/252 (r_s=8), with zero mismatches, unresolved or inconclusive results;28 analytic material cases have zero channel error. Counts repeat settings/layouts. The diagnostic shader does not directly certify the specialized Diagnostic=0 executable; image comparisons and runtime checks cover that path separately.

Close demo pair9886427308615695907 is byte-identical to0.02/1mm, SHA256 F60EC3FEB31693BA146FFD16BC8055AB112465A14660AC1F2F1BFDB09B8C6272. Final natural-wall pair11317958034842552915 has RGB MAE0.00015300 and0.1021% of pixels differing by more than8 levels; secondary-annulus MAE0.00030216, photon-edge0.00044183. Earlier wall/down/close crops were inspected, and final native screenshot2026-09-22_17.24.34.png was visually checked. The comparisons establish small sampled differences, not universal visual equivalence.

**Fresh matched live1440p gate:** same natural-world sourceN65/r_s8.125, player(16.5,302,-45.5), yaw.281, wall pitch.91/down35.91,1280x720 internal/two-ray AA,RTX5070Ti/driver616.92,120 warmup/300 samples. Two runs per pose; world actors/time evolve normally between runs. Pre-coverage24958d8 has6,092,246 terrain triangles, final has6,260,820. No easier demo scene substituted.

| Version/view | GPU p50 / p95 / p99 ms | Frame p50 / p95 / p99 ms |
| --- | --- | --- |
| Baseline wall1 |18.230 /19.801 /20.364|18.793 /20.934 /21.584|
| Baseline wall2 |18.200 /19.665 /20.081|18.842 /20.817 /21.399|
| Final wall1 |15.720 /17.839 /18.490|16.959 /19.229 /20.669|
| Final wall2 |15.745 /17.728 /18.307|16.844 /19.255 /20.636|
| Baseline down1 |29.715 /30.997 /31.574|30.625 /32.390 /33.177|
| Baseline down2 |29.376 /30.716 /31.223|30.261 /31.946 /32.845|
| Final down1 |28.604 /29.689 /30.094|29.802 /31.411 /32.423|
| Final down2 |28.498 /29.850 /30.458|29.698 /31.652 /32.452|

Logs `final-baseline-runtime.log` and `final-quality-runtime.log`. Representative medians improve about53→59FPS wall and33→34FPS down, with lower measured p95/p99 frame intervals. This is parity/improvement in the matched conditions, not a universal minimum FPS or an isolated same-frame speedup measurement. The older79aa1d0 down median29.796ms is also essentially matched; do not substitute the much faster intermediate wall/terrain runs as the final headline.

An intermediate guard implemented in the fragment shader had terrain frame p95 up to34.598ms despite better medians (`guarded-material-runtime.log`). The final policy is set on the CPU instead of recomputed per ray; the final measurements above pass. Live scene variation means these runs do not isolate the cost of moving that calculation. One earlier sample in `final-material-runtime.log` had a mismatched camera after resizing and was discarded; all final table poses match.

The expanded close-distance fixture originally found two rays crossing a neighbouring surface-cell boundary with wide steps (144 failures across repeated configurations).0.02/1mm passed. The near-observer policy fixes all expanded sampled checks; keep this regression coverage.

## Implemented and exercised

- Capture native non-living entities and block-entity renderers, preserving first-person camera-body exclusion. Chests, beds, a boat and a dropped item were checked in the separate demo dimension. Block-entity lists inspect loaded chunks' maps once per game tick; no block-array scan or generated chunks.
- Capture native fluid quads, preserving section-relative positions, native biome colour, face shading and light. Lava shares the opaque path; water uses native texture alpha. Translucent baked block models and normal entity/item translucent layers enter the same curved scene. Unsupported additive effects, glint, shadows, text and some special block-entity layers remain explicitly omitted.
- Composite transparent hits in ray order; continue the remaining part of the same optical chord after each transparent hit. Fancy clouds still contribute only their nearest face, now ordered relative to other transparent surfaces.32-layer budget; residual transmission below0.001 terminates. This models vanilla surface blending, not physical water refraction or absorption.
- Entity textures with only0/255 alpha use the existing cutout path when vertex alpha is opaque. Layer flags are consistent across each quad. Guard unsupported/multi-texture phases before casting the texture accessor.

At the first visual checkpoint, stacked red/blue stained glass and a glass-fronted water tank showed the expected background and tint; native boat/bed/chest/item geometry was visible. Screenshots `2026-09-22_15.34.18.png` and `2026-09-22_15.46.39.png` inspected. Fixtures are in the dedicated demo dimension only. New deterministic exhibit builds also include a bed/chest, stained-glass layers and contained pool; existing layouts are retained.

## Validation

`selective-material-build.log`: build/53 unit tests pass. `selective-material-runtime.log`:26240 sampled optical comparisons, zero mismatch/inconclusive/unresolved;24 analytic GPU material cases, zero channel error. Material cases cover two ordered layers, two-sided entity alpha, opaque occlusion, cloud/transparent ordering, native nearest-cloud semantics, reversed input and empty-cache variants. These are sampled checks, not general Minecraft material certification.

Full and selective compositors produce byte-identical PNGs in frozen wall/down pairs3076519621853606379 and16400811295145407760. The first pair is854x480; the second2560x1440. Pair metadata preserves camera/settings. The selective path traces an initial material probe, copies its alpha mask to a separate framebuffer, and retraces only marked rays with full composition. No texture/framebuffer feedback or unbent overlay. Extra mask storage isRGBA8 at2x logical width.

Found and fixed during review: a fixed ray-direction offset can round back onto a transparent surface at grazing incidence. A normal-directed, coordinate-scaled offset and an additional grazing GPU case pass in the final28-case fixture.

## Earlier rejected performance settings

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

## Earlier curvature experiment

Retain .02-radian reference; expose .04/.08 for measurement. Keep near-critical rays with dimensionless impact-squared within.005 of27/4 at.02. Retain nominal1mm curvature target and16-block spatial cap; remove the old .45-block minimum when it would exceed the curvature target in the wider-step experiment. This changes numerical sampling and requires the independent fixture, multiple fixed-pose image pairs, visual inspection and FPS checks before acceptance. F9 `[` cycles angular cap; Shift+`[` captures against.02. F9 Z/Shift+Z compares selective/full materials. No acceptance or new FPS claim yet.

## Later experiments — 2026-09-22

Grazing fix verified:28 analytic GPU cases pass with zero channel error, including near-parallel planes at coordinate512. The normal offset is max(0.1mm,4e-7 times largest coordinate magnitude), normally0.1–0.3mm within this scene. Still an approximation; do not claim exact coplanar multilayer composition. Both.04/.08 angular caps pass the original26240 optical samples. At1mm,.08 adds negligible speed over.04; the coupled4mm trial below differs.

`refinement-experiments-runtime.log` tests.04 angular cap with2/4mm nominal curvature targets (same near-critical angular guard, AA/resolution/coverage). Both pass26240 optical samples and28 material cases. Wall2mm selective GPU p50/p95=17.362/19.347ms; down2mm selective31.953/32.969ms, full31.147/32.290ms. Wall4mm selective16.219/18.027ms; down4mm selective30.801/31.779ms. These are frozen experiments, not final live baseline acceptance. Compared with.02/1mm, wall2mm pair16359643443451799546 has RGB MAE0.00015329 and0.0688% pixels over8 levels; down4mm pair2484438275258875559 has0.00015699 and0.0674%. Fine silhouettes change slightly; these are not pixel-identical.

AA experiments use a stronger reference: full-resolution4ray with fine path,.02 angular cap and1x tolerance multiplier. A new standalone Java tool, `tools/CompareSecondaryImages.java`, measures a geometrically defined incoming-ray annulus (impact squared27/4..8), plus a narrow photon-edge region6.70..6.85. Source position/radius are explicit arguments; this region is a proxy for secondary images, not proof that every included ray is a returning image. It writes native-resolution crops and metrics, without rescaling/alignment.

- Alternating sample diagonals: whole-image MAE improved~0.1%, but annulus error worsened~2.2% down and0.5% wall. Rejected; preserve original two-sample pattern. Pairs4359127467756100378/14963823024872105283 down and9385226905264618458/10487168026268009660 wall. A wall contact image was inspected; two-ray reconstruction remains coherent but does not match the finest subpixel ring detail.
- Four-ray photon-edge experiment: two extra ray draws only in impact-squared6.70..6.85, averaged in float32 before final8bit conversion. Edge MAE improved0.11192→0.10093 wall and0.10969→0.10232 down. Cost is unacceptable: wall selective GPU16.226→24.459ms; down30.573→36.216ms. Rejected under the FPS floor. Log ring-aa-runtime.log; pairs1593864463012078008/2437233226502186090 wall and14132499573160211185/3117977260108173289 down. Retain evidence, remove the extra-ray implementation from the final candidate.

Radial-diagonal AA also rejected: wall annulus MAE0.017915→0.018488 and photon-edge0.111935→0.116272; down annulus0.015509→0.016514 and edge0.109687→0.116660. It adds no rays, but does not improve the image. Pairs70403789083905362/4003594537183299439 wall and13676531280584191269/2449557148834518084 down. All three AA experiments were removed; retain the original two-ray pattern.

The coupled .08 angular cap /4mm target is more effective than .08/1mm: frozen selective wall GPU16.281/17.361/17.603ms and down28.504/29.439/29.864ms (p50/p95/p99). Full compositor down28.088/29.154/29.379ms. Optical26240 and material28 cases pass. Log radial-aa-runtime.log. Same-frame .02/1mm comparisons: wall13143804118029983029 RGB MAE0.00036821,0.1950% pixels over8 levels; down11001807646990927023 MAE approximately0.0003,0.15%. Wall annulus MAE0.00068457, edge0.00112783; down0.00049325/0.00061655. Wall contact crop visually inspected with no obvious loss. This is a small numerical approximation, not a hard error bound for every trajectory.

Full special-effect coverage (additive eyes/glint, signs/text, particles, coplanar material overlays, boat water masks) remains outside this demonstrated subset; do not imply those layers work. Live acceptance and closer-pose validation are recorded at the top.
