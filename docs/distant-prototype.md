# Distant height-field experiment — 2026-09-16

Status: opt-in research prototype, **not demo acceptance**. The default F10 renderer remains unchanged. `distantPrototype: true` in `run/config/interstellar-terrain.json` enables the experiment on the next F10 activation; F9 H toggles it for comparisons. F9 V/C also run a small independent distant flat-hit check. Diagnostic local rays retain their original solver/settings.

## Representation and ownership

Keep the accurate 96-cubed local capture. Add a 256-square, one-block-resolution column map centred on the source. Each column stores top height/material and, under the local footprint, a separate surface below the local volume. Height-field prisms exclude the local cube entirely, preventing that cube's wall from reappearing at an ordinary-camera position. Intersect both representations along the same chord and choose the nearer hit. No ordinary framebuffer image is sampled.

The map costs 1 MiB CPU plus 1 MiB GPU per snapshot, at most two snapshots. Capture is client-only, at most 4096 iterations and a soft two-millisecond budget per frame; downward searches resume between frames. It uses only loaded chunks. Local capture/upload budgets remain separate. Default F10 does not allocate or capture this map; F9 captures it for interactive comparisons. Current capture wall time is about 1.9 seconds, versus about 0.9 for local-only, so publication is roughly every 2.9 seconds in the tested scene.

## Explicit approximations

- Distant columns fill everything below the sampled surface: caves, overhangs, foliage and vertical materials are inaccurate. The below-local surface prevents the floating local demonstration platform from becoming a solid pillar.
- The world sky colour supplies a flat background. Clouds, sun, stars and directional atmosphere are absent. Unknown columns are skipped and terrain fades toward that colour from source-horizontal distance 80 to 128 blocks. This bounded, conspicuous fog is a prototype policy, not satisfactory seamless-world integration.
- Water is an opaque constant-colour surface; unsupported materials can still show diagnostic pink. Source-centred distance alone is not enough to decide whether nearby-to-camera geometry may be simplified.
- Standard local curved integration remains. Outside radius 80, chord length gradually grows toward four blocks at radius 144. Outgoing rays beyond both radius 96 and 12 Schwarzschild radii continue straight along their **current curved-path tangent**. Remaining weak-field bending is omitted; its error has not yet been quantified. The tangent is proportional to `-u' * radial + u * angular`, from differentiating `r = r_s/u`. This is not the original camera direction.
- A conservative maximum-height test skips distant segments entirely above all represented surfaces. It changes traversal cost, not geometry. Unloaded geometry is unavailable; no forced chunk loads occur.

## Checks and acceptance status

Build and 45 existing tests pass. Runtime shader compilation, frozen/live switching, a far downward landscape view and a side-view strafe were checked. No extra ordinary-camera wall copy was seen in those samples, but this is not a complete orbit/boundary acceptance run. No blocks were edited; snowfall changes the opaque count. Independent brute-force column slabs compare hit material and distance with GPU column traversal; this checks intersections, not the quality of the height-field approximation or omitted bending. Local V regression remains zero-mismatch.

Initial far-camera 1440p output/half-resolution results (RTX 5070 Ti, 120 warmup + 300 samples): original local-only GPU p95 4.667424 ms, first height-field version 13.476352 ms. Straight continuation without maximum-height rejection was worse: live GPU p95 17.362272 ms / frame interval p95 19.7836 ms. These are recorded failures, not a claimed improvement. Final measured results are appended below. The development client restart and shader reload required state-gated recovery; failed/interrupted timings were excluded.

Evidence: `distant-build.log`, `distant-verified-runtime.log`, `distant-bounds-runtime.log`; `run/distant-first.png`, `run/distant-landscape.png`, `run/distant-side-moved.png`. All local/ignored.

## Next decision

Do not enable this by default or call step 2 complete. Decide whether the measured speed is worth retaining a height field only for genuinely distant, camera-relative coverage. The visible fog cutoff, sky continuity, unloaded data and near-boundary overhang/occlusion need solving. Compare a coarser cached distant representation with directional/depth-aware sky and terrain captures; preserve nearby geometry ownership. Require moving-view tests and a performance comparison before selecting the demo default.

Technical references: official Yarn [ClientWorld sky colour](https://maven.fabricmc.net/docs/yarn-1.21%2Bbuild.9/net/minecraft/client/world/ClientWorld.html#getSkyColor(net.minecraft.util.math.Vec3d,float)) and [height-map API](https://maven.fabricmc.net/docs/yarn-1.21.1%2Bbuild.1/net/minecraft/world/class-use/Heightmap.Type.html). This experiment adds no claim of full-world scientific accuracy.

Final bound-rejection build: 93 distant rays / 21 hits / zero material-or-distance mismatches; local V 39x26, zero flat mismatches, 69 flat hits, 61 lensed opaque hits, zero unresolved. Frozen far camera r/r_s=12.68927, same 1440p/half-resolution setting: GPU p50/p95/p99 = 9.785280/9.932704/9.974400 ms; frame intervals = 11.1494/11.5638/11.7773 ms. 11700 opaque local cells, zero unknown/unsupported. This frozen-view measurement does not certify sustained live exploration or the whole demo. The final change after runtime checks only adjusts HUD wording; build rechecked without another screenshot.

Original local configuration restored; distantPrototype remains disabled by default. Normal F10 resumes the original approximately 0.9-second local capture (8 materials instead of 15 with the experiment). Player position restored to (72.867614,284.998488,-5.180181); no block edits. Prototype branch: codex/distant-heightfield. The experiment is available for continued engineering, not presented as finished world integration.
