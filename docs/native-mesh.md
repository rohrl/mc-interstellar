# Native baked-mesh integration experiment

2026-09-17, branch `codex/world-mesh-reference`. Quality-first experiment authorized by the owner; FPS is not its acceptance gate. The existing F10 backend remains available. This is not yet the final integrated renderer or a converged lensed reference.

## Controls and representation

Inspect the source, open **F9**, then press **M**. Capture takes about 14–16 seconds in the measured scene. M starts with lensing off; **Space** enables it. **P** saves the same-frame vanilla/zero-bending pair through the mesh backend. M toggles back to the existing voxel/column backend without recapturing; Esc releases both captures. V/C remain voxel-only diagnostics.

The native BlockModelRenderer supplies visible baked quads with model offsets, position-dependent model seed, UVs, tint, directional shade, corner AO and lightmap coordinates. Quads retain native triangle order. A CPU-built bounding-volume hierarchy supplies conservative bounds and escape links; GPU chord traversal finds the nearest front-facing, alpha-tested triangle. One mesh owns both nearby and distant terrain, with no straight-camera terrain overlay or filled columns. Mesh mode retains ray integration beyond the old 12-radius distant straight-continuation cutoff.

Capture covers a source-centred 16×16 chunk square over the world's full build height, loaded chunks only. It does not generate terrain or edit blocks. Client capture is sliced at 5 ms; final tree construction/upload are synchronous. Four million triangles is a hard safety cap, with an explicit failure rather than silent truncation. The GPU traversal cap is visibly magenta. Texture slots are shared with the mutually exclusive voxel backend to respect Minecraft's 12-slot state tracker.

This is deliberately memory-heavy: triangle storage alone uses 144 bytes per triangle, on CPU during capture and on GPU afterward; tree data and temporary upload storage add to that. CPU capture arrays are released after upload. No periodic mesh recapture or live mesh mode exists yet.

## Evidence

Build and 47 tests pass, including empty/degenerate trees, payload preservation, leaf ownership and conservative child bounds. Runtime shader compilation, two zero-bending comparisons and a lensed wall/occluder snapshot passed. Both A/B pairs have pixel-identical vanilla references. Tests were at night, 854×480, actual vertical FOV77 despite configured70. No daytime acceptance is claimed.

| Frozen pose / pair | Height-field RGB MAE | Mesh RGB MAE |
| --- | ---: | ---: |
| Downward mountains, full image | 0.0162 | 0.0134 |
| Downward mountains, bottom half | 0.0297 | 0.0243 |
| Wall/pillar, full image | 0.0106 | 0.0105 |

Mountain camera `(16.5,303.62,-111.5)`, yaw0.281/pitch35: mesh pair `13757748429017828718`, old pair `16214435908096064741`, identity report `mesh-mountain-repeat`. Capture: 2,264,320 triangles, 912 unloaded sections, 14,571 omitted fluid/translucent incidences; 13.904 s including build/upload. The source-centred square does not cover all terrain visible from this boundary pose. Contact sheet confirms missing edge coverage and cloud differences, alongside better actual cliff geometry.

Wall camera `(16.5,303.62,-45.5)`, yaw0.281/pitch0.91: mesh pair `886265746699817478`, old pair `17074187507979693881`, identity report `mesh-wall-repeat`. Capture: 2,514,090 triangles, zero unloaded sections, 19,739 omitted incidences; 15.339 s. Visible mobs on the vanilla platform account for conspicuous omitted detail; terrain improvement here is small. `run/mesh-lensed-wall.png` shows lensing with the foreground gold pillar occluding the wall image. This is visual evidence, not an independent curved-triangle accuracy certificate.

Final diagnostic timing only: frozen mesh, lensing on, 427×240 internal / 854×480 window, standard path, 120 warmup / 300 samples: optical GPU p50/p95/p99 5.69776/6.492192/6.69904 ms; frame intervals 8.3691/8.8615/9.107 ms. Excludes mesh capture/build/upload and is not a target-resolution or FPS claim. Final build `mesh-final-build.log` passes 47 tests; `mesh-final-runtime.log` verifies F10, native mesh capture (15.967 s), corrected sky escape direction, and the explicitly labeled mesh timing. `run/mesh-final-lensed-wall.png` inspected. Earlier failed runs discovered the texture-slot limit and an insufficient one-million-triangle cap; both were corrected before these captures.

## Remaining integration work

- Foreground clouds need depth/opacity ordering; the inherited native sky capture only supplies the background on terrain misses.
- Fluids, translucent blocks, entities/block entities and precipitation are omitted. Omitted-block statistics count fluid/translucent incidences, not entities; waterlogged translucent blocks can count twice.
- Texture filtering/mip selection, animation freezing and full native material behavior are incomplete. Atlas sampling currently uses LOD0.
- Broader camera-relative coverage and the existing 128-block F9/F10 viewing guard remain unresolved. Loaded data is not equivalent to full visible-world coverage.
- Mesh lensing inherits finite chord steps, escape/iteration limits and endpoint-based fog. At the 512-block escape sphere it samples the sky using the outgoing tangent (not the radial endpoint); remaining far-field deflection is omitted. It is not a certified optical reference; establish convergence and independent mesh-hit checks before using it as one.

Next prioritize cloud/transparent foreground composition and capture coverage, then entity coverage and daytime pairs. Keep ordinary Minecraft as the appearance oracle; optimize only after these integration errors are understood and corrected. Automated movement/flicker checks remain deferred to the owner.
