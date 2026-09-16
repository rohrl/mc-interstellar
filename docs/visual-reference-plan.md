# Visual integration reference: feasibility and next gate

2026-09-16 plan; checkpoint updated 2026-09-17. Owner rejected white mountains and a dark, flat sky. The first same-frame vanilla/zero-bending capture and regional comparison tool now runs, alongside native sky/light and snow-cap repairs: see [native-appearance.md](native-appearance.md). Appearance parity and the broader pose suite remain unfinished; the slow lensed reference is not implemented. Owner has deferred automated movement/flicker checks; retain periodic fixed-snapshot inspection.

## Diagnosis

`DistantTerrain.surface` retains the top block material for an entire filled column. Snow on a peak can become snow on its cliff faces. `terrain.fsh` shades materials with a fixed directional formula, rather than Minecraft's complete lighting/material pipeline, and returns a single `SkyColor` for background rays. These are representation/appearance failures, not primarily geodesic accuracy problems. Projection is also hard-coded to a 70-degree vertical field of view; comparison must control or correctly reproduce vanilla projection, camera movement and other view effects.

## Decision: staged reference comparisons are worthwhile

Adopt the owner's paired-image feedback method, but do not first build an unlimited, supposedly perfect alternate Minecraft renderer. Slow rendering alone cannot recover absent geometry, material layers, lighting or atmosphere. A baseline sharing the same omissions would falsely certify the optimized result. No numerical token-saving or development-time claim is made before measurement.

1. **Vanilla appearance reference.** Capture the normal Minecraft render and the proposed integration backend with bending disabled, using identical world/frame/camera/projection. The integration backend must actually render through its appearance path; simply bypassing it and copying the vanilla image is not a valid parity test. Start with snowy cliffs and sky, plus coloured wall, foliage, water and an occluder. This is the cheapest authoritative check for the current failures.
2. **Small deterministic capture suite.** Reuse one frozen scene state and fixed camera poses, resolution, render distance, lighting/time/weather, exposure/gamma and animation phase. Capture the framebuffer without HUD, hand, chat or desktop borders. Record metadata and backend/settings/version. Repeat identical captures to establish noise before choosing thresholds; reject mismatched metadata rather than aligning away projection errors. Preserve the owner's live world; use a separate test copy or client-side frozen state for controlled changes.
3. **Slow lensed quality reference, incrementally.** Once appearance parity exists, reuse the same supported geometry/material/lighting inputs with broad scene coverage, conservative ray tolerances, full resolution and dense sampling. Pair it with the existing independent numerical ray reference. Validate the quality reference at increasing quality before treating it as an oracle. State which model classes, sky effects and transparency are covered; do not call it perfect or complete until demonstrated.
4. **Automated approximation comparisons.** Compare fast/reference frames at the same poses. Output compact machine-readable metrics and heatmaps/contact sheets only for failures or milestones. Keep per-region and worst-region errors (sky, terrain, silhouette/occluder, strong-lensing region) so a large uniform sky cannot hide a small duplicate wall. Preserve colour/luminance error, edge displacement and an appropriate perceptual score; retain temporal comparisons over short recorded paths for flicker and popping. Avoid optimizing a single scalar that could reward blur or suppress detail.
5. **Performance/quality decision together.** Measure live CPU/GPU/frame-time percentiles, update spikes and memory separately from slow-reference captures. Keep human inspection for initial baseline approval, major appearance changes and suspicious failures. Automated diffs reduce routine image reading; they cannot certify their own reference or guarantee perceptual invisibility.

## Rendering direction to investigate

Preserve the working local ray propagation. First investigate reusing Minecraft's rendered appearance and/or its baked model, tint, lightmap and shading inputs, rather than inventing more distant colours and lighting. Colour-plus-depth captures from multiple directions/layers are a candidate for expensive reference capture and later caching, but not automatically ground truth: they can miss surfaces exposed by curved paths and mishandle parallax/occlusion. A single ordinary screen image, or a depthless panorama, is insufficient for nearby scene integration. First-hit ownership and coverage must remain explicit.

The first implementation gate is a reproducible **vanilla-versus-zero-bending** capture pair and comparison report. Do not begin another distant approximation or a large full-world reference renderer until the appearance path and deterministic capture are shown viable. Full lensed reference development follows that evidence; its cost is currently unmeasured.

## Primary references

- NVIDIA's [FLIP implementation and documentation](https://github.com/NVlabs/flip) supplies rendered-image perceptual difference metrics. Evaluate it as a tool dependency; no third-party code/license has been added to this repository.
- [Proxy-guided Image-based Rendering](https://resources.mpi-inf.mpg.de/ProxyIBR/) illustrates colour/depth reprojection and the need for additional views/layers to address newly visible surfaces. It supports the coverage concern, not a claim that its method directly solves this mod.

Basic pixel differences remain useful diagnostics, but image noise, colour space, geometry displacement and temporal artifacts need separate treatment. The initial suite should stay small; increase its coverage only when a new failure or feature justifies it.
