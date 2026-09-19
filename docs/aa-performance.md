# Antialiasing and ray-step comparison

Owner priority (2026-09-19): demo packaging is paused; improve AA and FPS while preserving the accepted native-world appearance. Unverified packaging is isolated at2fe8674 on codex/demo-packaging-wip. The original unverified AA checkpoint8ad46eb remains on its separate branch; its two-ray idea is adapted to the current mesh/cloud renderer here.

## Controls and quality comparisons

- F9 **A** cycles OFF → EDGE → 2x. Default **2x** traces two diagonal subpixel rays per internal pixel, then uses clamped cubic reconstruction when upscaling. Each ray has independent cloud/hit state.
- EDGE is the inexpensive spatial filter experiment. The owner found it blurry, so it is **not the default**. It combines linear reconstruction with short edge-directed smoothing; it cannot recover missing detail.
- OFF retains the original centre ray and nearest reconstruction. Q changes internal scale between0.5 and1. AA does not increase the output resolution or recover all detail lost at half scale. Two samples are limited spatial AA, not temporal AA or complete shimmer removal.
- Persistent `run/config/interstellar-terrain.json`: `"antialiasing": "2x"`, `"off"` or `"edge"`. Missing setting defaults to2x; old boolean true/false maps to2x/off. Reopen F9/F10 after editing. Keyboard overrides are temporary. The F10 HUD shows the active AA mode.
- F9 **Shift+P** compares the selected AA/scale with four rays per pixel at full resolution in the same frozen scene. The reference shares the optical/model assumptions; it is a sampling reference, not perfect ground truth.
- F9 **G** toggles adaptive mesh path steps. **Ctrl+P** compares the original and selected stepping at identical AA, scale and scene. Ordinary **P** still compares vanilla with unbent, full-resolution, AA-OFF rendering.
- C runs the independent CPU fixture for both original/adaptive paths. Diagnostics always use centre rays, regardless of AA.
- B/F12 GPU timings now include final reconstruction as well as ray rendering. They still exclude CPU geometry work, native world rendering and sky capture. Older pass-only measurements have different scope.

All pairs use the existing Java comparator; contact-sheet labels are generic so AA/path comparisons are not incorrectly labelled vanilla. Compare identical reference hashes and metadata before treating separate pairs as an A/B test. Do not optimize merely for lower mean image error: blur can lower that metric.

## Curvature-guided steps

The existing orbit state is u=r_s/r and v=du/dphi, with u''=1.5u²-u. In the renderer's existing Euclidean embedding, p=r e_r, p'=r' e_r+r e_phi and p''=(r''-r)e_r+2r'e_phi. Therefore the spatial curvature magnitude is

`kappa = |p' × p''| / |p'|³ = 1.5 u⁵ / [r_s (u²+v²)^(3/2)]`.

Use the local circular-arc sagitta estimate `error ≈ kappa × chordLength² / 8` to select a chord target. Standard paths use0.001 blocks; fine paths use0.00025. Keep the previous minimum step and4-block maximum, the0.02-radian angular cap, RK4 orbit equations and2048-iteration budget. This spends fewer intersection searches on weakly curved segments. The local estimate is a **heuristic**, not a rigorous accumulated error bound: curvature changes within a step. Independent hits and same-scene images must constrain adoption. This introduces no straight-camera overlay, geometry omission, animation throttling or reduced terrain range.

The equations use the already documented [finite-surface orbit conventions](curved-terrain-validation.md); the curvature derivation above is ours. No change to source mass, coordinate model or observer frame is implied.

## Dynamic upload reuse

Moving mob/cloud meshes now retain their two GPU textures and native staging buffers. Capacity grows in powers of two; ordinary updates use subimage uploads into existing storage. Logical node/triangle counts bound reads, so stale padding is never traversed. Closing the preview releases retained storage. Capturing, animating and rebuilding the dynamic BVH still happens every live frame.

This removes per-frame texture creation/deletion and staging allocation. That is a measured allocation reduction once runtime counters are checked, not by itself proof of an FPS gain; uploads and CPU mesh rebuilding still cost time.

## Measured checkpoint — 2026-09-19

`adaptive-build.log`: build/51 tests pass. `adaptive-runtime.log`: shader compilation and1440 original/adaptive, monolithic/two-level, standard/fine CPU/GPU comparisons pass, no inconclusive or unresolved rays. Maximum CPU invariant drift3.8636e-14; fixture wall676ms. This doubles the earlier720-comparison suite.

Frozen streamed world: player(16.5,302,-45.5), yaw.281/pitch.91, current owner source N65/r_s8.125, r/r_s7.57103,6,092,160 terrain triangles plus16,812 moving triangles/109 supported mobs.427×240 internal →854×480 window, standard paths, RTX5070Ti,120 warmup/300 samples per run. No blocks/time/weather changed by the agent.

| Mode | GPU p50 / p95 / p99, ms | Frame interval p95, ms |
| --- | --- | --- |
| Original steps, 2x AA |49.218 /52.367 /54.504|53.746|
| Adaptive steps, 2x AA |33.584 /36.102 /37.483|37.182|
| Original steps, AA OFF |25.354 /27.598 /28.387|28.658|
| Adaptive steps, AA OFF |17.579 /19.573 /20.187|20.509|

At matched2x quality, adaptive stepping reduces measured GPU p95 by31.1%; with AA OFF,29.1%. These are repeated frames of one frozen pose, not a whole-game speedup, animation benchmark,1440p result or statistical multi-run certification. No effects, scene geometry or coverage were removed for these comparisons.

Original/adaptive same-AA pair3512306314574202495: full RGB MAE0.00001569;0.0073% pixels differ by more than8/255 in any channel. Contact sheet inspected; no visible systematic displacement in this view. AA comparison pairs11476391722803368108 (2x) and700265656719790367 (OFF) share identical four-ray/full-resolution reference hashes. Full MAE improves.0036→.0032 and centre.0078→.0070 with2x, but the full-image fraction over8 levels rises3.16%→3.87%. Filtering spreads some errors; the scalar scores do not establish an unqualified perceptual win. The2x candidate was inspected. Half-resolution detail remains limited and two rays still roughly double tracing cost.

Monolithic/streamed unbent probe pairs15093924048906674412 /1537547102433014654 have identical vanilla reference hashes. Candidate images are close but **not byte-identical** in this scene:0.03% pixels differ over8 levels; worst32-pixel tile MAE.00153186. Do not reuse the earlier checkpoint's exact-hash claim for this run.

Second path comparison, same player position with pitch35: pair1951835587080724502 covers the platform edge and mountains below. Full RGB MAE0.00001657;0.0071% pixels exceed8 levels. Contact sheet inspected. These two views support adoption of the conservative adaptive steps; they do not certify all geometry, critical subpixels or movement.

Live checks: after600 updates with changing105→104 supported mobs, triangle/node allocation counters remain1/1. Animation fingerprints change. Live small-window GPU p50/p95/p99=34.324/38.111/40.561ms; frame intervals35.875/40.149/42.382ms. These runs are not a matched legacy/new-upload FPS comparison; only allocation reuse and ongoing geometry updates are established.

**Full-screen limit:** live2560×1440 output,1280×720 internal,2xAA/adaptive paths, same pose/source: GPU p50/p95/p99=159.902/167.944/176.106ms; frame intervals166.240/177.935/183.302ms, about6FPS at the median.120 warmup/300 samples. This is far from the60FPS target. No same-pose full-screen original-step baseline was run, so do not extrapolate the31% small-window reduction as a measured1440p speedup. The optical GPU pass dominates this measured frame; prioritize ray/geometry traversal work next. Restored the870×519 window afterward, F10 active.

## Reused, vectorized bounds checks — 2026-09-19

The default mesh traversal computes reciprocal chord directions once, shared by terrain and moving BVHs, then uses vector slab intersections. Near-parallel axes (absolute delta below1e-12) retain explicit padded-box containment and finite placeholders; no zero-times-infinity arithmetic. The previous1e-5 box padding, traversal order, nearest-hit selection, triangle/material checks, optical integration, AA and resolution are preserved. This rearranges floating-point arithmetic, so sampled pixel identity is not a universal proof at all geometric boundaries.

F9 **T** toggles fast/original bounds; **Ctrl+Shift+P** captures original versus selected bounds with the same path/AA/scale. Timing and capture metadata include fastBounds. F9 C now covers both bounds algorithms, both mesh layouts, both path algorithms and standard/fine steps:2880 comparisons over180 distinct rays.

`bounds-build.log`: build and51 tests pass. `bounds-runtime.log`: runtime shader compilation;2880 comparisons pass with zero mismatches, inconclusive or unresolved rays (742ms). Frozen streamed pose(16.5,302,-45.5), yaw.281/pitch.91, owner source N65/r_s8.125,6,092,190 terrain triangles plus6516 moving triangles/28 mobs. Both modes use adaptive paths and2x AA. RTX5070Ti;120 warmup/300 samples; optical GPU time includes resolve.

| Output / internal size | Bounds | GPU p50 / p95 / p99, ms | Frame p50 / p95, ms |
| --- | --- | --- | --- |
|854x480 /427x240|Original|29.528 /32.580 /33.520|30.599 /33.534|
|854x480 /427x240|Fast|27.274 /30.121 /30.636|28.339 /31.157|
|2560x1440 /1280x720|Original|114.520 /120.778 /126.873|119.373 /127.976|
|2560x1440 /1280x720|Fast|106.926 /111.447 /115.449|111.499 /119.469|

Matched GPU p95 reductions:7.5% small window,7.7%1440p. Small-window fast was measured first; fullscreen original first. Single frozen pose, not a multi-run whole-game certification. The1440p optimized frame median is still about9FPS. This scene has fewer live actors than the earlier checkpoint; do not attribute cross-session timing differences to this optimization.

Pairs600111862750755422 (small) and3810031342862256033 (1440p) have zero pixel differences and identical reference/candidate PNG SHA-256 within each pair. Hashes respectively A75EE0AFCD865A43198763C40EE090455E85A03EF6071EB9C44B00647F787847 and71C4410F6E317E42C2F6D71A41FB53C910F7010C1A169D09930CE4A93FA02323. Small candidate inspected. No blocks, time, weather or saved quality settings changed. The main remaining cost is repeated ray/geometry traversal; this gain does not meet the FPS target.

## Constant-width mesh addressing — 2026-09-19

`meshData` specializes integer division/remainder for the actual4095-wide streamed arenas and4096-wide monolithic/moving textures. The shader compiler can simplify fixed-divisor arithmetic. Integer texel coordinates, geometry, traversal, optical paths, AA and all sampling stay unchanged; other texture widths retain the original general path. Default enabled in F9/F10. F9 **R** toggles original/specialized addressing; **Alt+P** captures a same-frame pair. Benchmarks and capture metadata include fastFetch.

`fetch-build.log`: build51 tests pass. `fetch-runtime.log`: runtime shader compilation and5760 CPU/GPU sampled hit comparisons pass (974ms), zero mismatches/inconclusive/unresolved. This is180 distinct rays across addressing/bounds/path/layout/step variants, not5760 independent directions. The synthetic arenas are4095-wide; actual-world comparisons also exercise the4096-wide moving textures.

Frozen streamed scene: player(16.5,302,-45.5), yaw.281/pitch.91, N65/r_s8.125;6,092,214 terrain and8412 moving triangles,50 supported mobs.2xAA, half-resolution, adaptive paths and fast bounds in both addressing modes. RTX5070Ti, driver616.92;120 warmup/300 samples. GPU timings include resolve. Actor counts and world state differ from previous sessions; compare within this session only.

Small-window original GPU p50/p95/p99=40.389/44.300/44.696ms; specialized33.998/37.261/37.928ms (p95 reduction15.9%). Frame p50/p95 original41.514/45.371ms, specialized35.017/38.433ms.854x480 output,427x240 internal. Specialized measured first, then original.

Same-frame pairs15920050732033724678 (small) and2859121864403485792 (1440p) have zero pixel differences. Small candidate visually inspected. Small pair reference/candidate PNG hash: C107D171FAE55D9F3525507C9AC254B0E07C68C3753A06B7FFDE50FD7E24682C. No blocks, weather, time or saved quality options edited.

At2560x1440 output/1280x720 internal, original GPU p50/p95/p99=152.278/160.323/163.842ms; specialized128.636/135.443/138.134ms (p95 reduction15.5%). Frame p50/p95 original158.378/166.937ms, specialized133.422/142.031ms, about7.5FPS optimized median. Original measured first, then specialized. Fullscreen pair PNG hash13B4B1D5EF29992A4C7D5DC40CB93E6E256AC22549D3D81B8C30512AD00BF7FA in both images. These are one-pose frozen comparisons, not a multi-run statistical or whole-game FPS certification.

Monolithic4096-wide layout: pair17256894883602490531 also has zero pixel differences; both PNG hashes C857AB790EE393F1B94AA7EDFBDA0C5411D72671292BE7A66C2837144A679BBB. This compares original/specialized addressing within that layout, not monolithic/streamed parity. Live F10 smoke check passed600 updates with changing45→54 mobs and geometry fingerprints; triangle/node allocations remain1/1. No separate live FPS comparison. Small window restored and F10 left active; no runtime ERROR/Exception logged.

## Empty regions learned during traversal — 2026-09-19

Default ray-local cache: each terrain/moving-tree search starts a candidate box16 blocks around the segment start. For every rejected subtree, choose a separating axis and clip the box so it excludes that subtree's padded bounds. Publish only when traversal completes without entering any triangle leaf and every rejection has a strict separating axis. Later segments skip that tree only when both endpoints lie strictly inside the certified box. Boxes are convex, so the full straight segment is inside; the optical integrator still evaluates exactly the same curved path/chords. Any touched leaf, uncertain separation or exhausted traversal prevents caching. No alpha/material/cloud approximation. Separate caches for both trees reset for every ray/AA sample; nothing persists across frames or mob updates.

F9 **I** toggles the cache; **Alt+Shift+P** captures uncached/selected comparisons. Internal flag/metadata is named emptyCells. This version adds no separate occupancy traversal. An earlier fixed16-cube occupancy-query prototype was rejected: GPU p95 regressed41.706→47.743ms despite identical pixels. Do not revive that prototype or conflate its timings with the retained implementation.

`regions-build.log`: build51 tests pass. `cells-runtime.log` after the20:39 resource reload contains the retained shader's tests:11520 comparisons pass, zero mismatches/inconclusive/unresolved,1370ms.180 distinct directions over distances32/96/148/252, both layouts, path/bounds/addressing/cache modes, standard/fine steps. Limited synthetic opaque-box hit-cell validation, not universal mesh/material certification. Initial authentication public-key timeout was unrelated to rendering and did not prevent testing.

Frozen streamed wall pose: player(16.5,302,-45.5), yaw.281/pitch.91, N65/r_s8.125,6,092,216 terrain plus12,252 moving triangles/94 mobs.2xAA, adaptive paths, fast bounds/addressing unchanged. RTX5070Ti, driver616.92,120 warmup/300 samples, GPU pass includes resolve.

| Output/internal | Cache | GPU p50 / p95 / p99 ms | Frame p50 / p95 ms |
| --- | --- | --- | --- |
|854x480 /427x240|OFF|41.740 /44.630 /46.299|42.876 /45.940|
|854x480 /427x240|ON|21.366 /22.954 /23.657|22.419 /24.374|
|2560x1440 /1280x720|OFF|154.892 /161.340 /165.569|158.534 /170.419|
|2560x1440 /1280x720|ON|74.884 /77.698 /78.982|83.334 /87.210|

Matched GPU p95 reductions48.6% small and51.8%1440p. Small cache measured first; fullscreen uncached first. These compare toggles within the revised shader, not separately compiled releases; shader register allocation and scene changes prevent extrapolating across sessions. Fullscreen cached median frame is about12FPS, still below target. No quality settings or scene coverage reduced.

Pairs677871936328255550 (small) and1529780201675721445 (1440p) have zero pixel differences. Small reference/candidate hashes383FD5BB80539FB22E959549E6A26C925E7DD113D4071C3F4BC3CFD3CC121C51 also match the inspected earlier candidate367943847523181633. No block/time/weather/config edits. Saved images/metrics and logs remain local ignored artifacts.

Downward terrain view, same position/pitch35: pair9230438034004577933 is pixel-identical; candidate inspected.6,092,216 terrain plus12,336 moving triangles/95 mobs. Small-window cached GPU p50/p95/p99=29.581/31.966/32.499ms, uncached46.099/48.696/50.993ms (p95 reduction34.4%). Frame p50/p95 cached30.921/32.906ms, uncached47.222/49.960ms. Same2xAA/half-resolution and other flags; cached first. Confirms the benefit varies with view; not every scene is guaranteed a speedup.

Live check: default cache enabled, changing96→97 mobs/geometry over600 updates, texture allocations still1/1. F12 confirmed live=true/emptyCells=true while the camera moved and streaming queued170 chunks: GPU p50/p95/p99=15.243/20.424/22.267ms, frame16.186/23.008/25.275ms at427x240. Camera r/r_s1.79676 and scene4,909,366 triangles differ from the frozen setup; this is an observational smoke test, not a controlled performance gain. No subsequent camera reset; owner may be exploring. F10 remains active in the small window.

## Larger initial empty-region extent — 2026-09-19

Raise the initial per-axis cache half-extent from16 to1024 blocks. Rejected subtree bounds still clip the learned box, and all conservative publication/strict-containment/per-ray reset rules stay unchanged. This controls reuse of proven empty space, not viewing range, captured terrain, optical step size or image resolution. F9 **Shift+I** switches16/1024; **Ctrl+Alt+P** captures16 versus the selected extent. I and Alt+Shift+P still compare caching with no cache.

`reach-build.log`: build51 tests pass. `reach-runtime.log`:17280 sampled comparisons pass (180 distinct directions, both extents only where caching is enabled, other prior mode/layout variants), zero mismatch/inconclusive/unresolved;1832ms. Frozen wall scene: same player(16.5,302,-45.5), yaw.281/pitch.91, N65/r_s8.125;6,092,230 terrain and12,672 moving triangles/97 mobs.2xAA, adaptive/fast bounds/addressing/caching unchanged. RTX5070Ti;120 warmup/300 samples, GPU timing includes resolve. Within-session comparisons only.

| Output/internal | Initial half-extent | GPU p50 / p95 / p99 ms | Frame p50 / p95 ms |
| --- | --- | --- | --- |
|854x480 /427x240|16|22.088 /23.950 /24.934|23.369 /25.205|
|854x480 /427x240|1024|18.103 /19.645 /19.986|19.162 /20.790|
|2560x1440 /1280x720|16|81.579 /84.342 /85.649|83.679 /92.059|
|2560x1440 /1280x720|1024|59.221 /61.369 /62.382|66.670 /66.988|

Wall-view p95 reduction18.0% small and27.2%1440p. Large extent measured first at small size,16 first at fullscreen. Fullscreen median frame about15FPS; no target or whole-game certification. Pairs18253456007042841607 (small) and4539380152757232634 (1440p) have identical PNG hashes within each pair and zero pixel differences. All performance claims use the same frozen scene and quality settings, not comparisons against previous sessions.

Additional lensed views at854x480 output/427x240 internal, same position and settings:

| View | Half-extent | GPU p50 / p95 / p99 ms | Frame p50 / p95 ms |
| --- | --- | --- | --- |
|Downward, yaw.281/pitch35|16|30.575 /32.869 /33.740|31.927 /34.111|
|Downward, yaw.281/pitch35|1024|24.096 /25.705 /26.271|25.430 /27.102|
|Away, yaw180/pitch25|16|16.623 /17.566 /18.050|17.653 /18.545|
|Away, yaw180/pitch25|1024|13.902 /14.787 /15.221|14.815 /15.792|

P95 reductions21.8% downward and15.8% away. Downward16 first; away1024 first. Pairs4772766165275653898 and12809664373608801229 have zero pixel differences and identical PNG hashes within each pair. Away candidate inspected. An initial downward run/pair7724198574473978919 had lensing OFF; it is excluded from these lensed performance claims. No block/time/weather/quality-config edits; these are frozen same-scene comparisons, not a universal speedup guarantee.

Live check confirms emptyReach=1024, changing actors over600 updates, retained1/1 texture allocations. Small-window live GPU p50/p95/p99=18.709/19.612/20.308ms; frames20.042/21.848/23.307ms at the wall pose, source radius ratio7.57103 and zero queued chunks. This confirms live operation, not a matched16/1024 live gain. F10 left active in the small window. No renderer errors observed; authentication public-key timeouts in this log are nonblocking.

## Native shader specialization and code review — 2026-09-19

Compile native mesh rendering as a separate program with MeshMode fixed to1. Both programs import terrain_shared.glsl, keeping one implementation of optics, traversal, materials and all earlier optimization toggles. The native program omits voxel-only samplers/uniforms; its unreachable legacy column diagnostic is removed by constant propagation. No ray steps, AA, geometry or lighting changed. F9 S switches programs; Shift+S compares the general program with the selected program in one frame. Native is the default in F9 meshes/F10; voxels retain the general program. This is compiler specialization, not evidence about register occupancy (not measured).

shader-split-build.log:51 tests pass; shader-split-final-build.log passes after equivalent JSON formatting cleanup. shader-split-runtime.log: both programs compile, each passes17280 sampled CPU/GPU comparisons, zero mismatches/inconclusive/unresolved (native1757ms, general1682ms). Each run covers180 distinct directions across existing layout/flag/path variants; the total is not34560 independent directions. Shared-source comparison confirms the original algorithm apart from the explicit backend/diagnostic selection. This is sampled opaque-box evidence, not arbitrary-material certification.

Frozen streamed scene:6,092,214 terrain triangles plus10,404 moving triangles/67 mobs; source N65/r_s8.125; player(16.5,302,-45.5), yaw.281/pitch.91. RTX5070Ti/driver616.92,2xAA/half-resolution, adaptive steps/fast bounds/addressing/empty-region cache1024 enabled.120 warmup/300 samples; GPU pass includes resolve.

| View / output | Program | GPU p50 / p95 / p99 ms | Frame p50 / p95 ms |
| --- | --- | --- | --- |
|Wall2560x1440, first pair|general|58.491 /61.011 /61.881|66.647 /67.047|
|Wall2560x1440, first pair|native|56.583 /59.059 /60.616|58.451 /66.928|
|Wall2560x1440, reversed order|native|56.740 /59.319 /60.169|58.539 /66.943|
|Wall2560x1440, reversed order|general|58.468 /61.424 /63.038|66.647 /66.988|
|Wall854x480|native|17.099 /18.332 /18.728|18.242 /19.271|
|Wall854x480|general|17.454 /18.681 /19.467|18.579 /19.795|
|Downward854x480, pitch35|native|24.860 /26.885 /27.571|26.174 /28.047|
|Downward854x480, pitch35|general|25.145 /26.914 /27.724|26.430 /28.006|

Wall GPU p95 reductions3.2–3.4% at1440p,1.9% small. No meaningful downward p95 gain (0.1%). Keep this modest, view-dependent improvement without claiming target FPS or multiplying earlier gains. Native fullscreen median frame about58.5ms (~17FPS), with cap/vsync effects; no whole-game GPU profiling. An interrupted downward run and an uncompleted repeat are excluded; completed timings alone appear above.

Paired PNGs have matching SHA256 hashes and zero pixel differences:6081711448631759605 (1440p streamed wall),16001729553208532114 (small streamed downward; candidate inspected). Preliminary monolithic wall pair17285760811997495812 is also identical; its6,102,618 triangles include moving geometry. That preliminary small-window native/general GPU p95 was15.456/15.892ms, but the streamed comparisons above represent F10's layout. No block/time/weather/config edits.

### Review conclusions and next experiments

The current quality-preserving plan remains appropriate: the black-hole GPU pass still dominates measured frame cost. No new correctness bug was confirmed in the reviewed empty-region certificates, node/triangle address ranges, shader state or source-shift bounds. Existing incomplete materials/entities remain limitations; these checks are not a general correctness proof.

Next, measure node/triangle visits and test a better occupied-geometry search tree (for example, binned surface-area splits versus the current midpoint split). Include capture time and live actor rebuild cost; tree reordering can change ties at coincident faces, so retain image and independent-hit comparisons. Another smaller experiment is shading only the final nearest opaque hit after alpha testing: current traversal repeats vertex/lightmap shading for temporary nearest candidates, but retaining hit data may increase register pressure, so the gain is uncertain. Compact integer node storage is a later option to reduce texture bandwidth. Do not reduce AA, ray accuracy, animation rate or captured coverage for these comparisons. Packaging remains paused.

Final live check: F10 defaults to program=native-mesh with unchanged2xAA and other defaults;600 changing actor updates retain1/1 triangle/node allocations. At the small wall pose, GPU p50/p95/p99=17.686/19.018/19.918ms; frame18.848/20.621/21.748ms. Scene actors differ from the frozen tests; this is a live smoke test, not a matched live gain. An initial attempt found Minecraft paused after focus loss; resumed normally before the completed measurement. Client left in live F10 at the wall pose, small window. No renderer errors observed.
