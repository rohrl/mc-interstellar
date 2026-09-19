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
