# Frozen native mobs and terrain lightmap parity

2026-09-17, `codex/world-mesh-reference`. Owner visually accepted the baked terrain experiment as a substantial improvement and requested mobs. Continue quality-first; performance optimization remains deferred.

## Behavior

**F9 → M** now captures supported living-entity geometry together with terrain. **E** toggles captured mob bodies; **Space** toggles lensing; **P** saves a same-frame vanilla/zero-bending pair. **K** defaults ON and applies native terrain lightmap sampling; OFF reproduces the previous half-texel offset for comparison. F10 still uses the older live voxel/column backend.

Mob poses and equipment come from Minecraft's entity render dispatcher and native vertex consumers, including interpolated position, animation/model transforms, UVs, vertex colour and light. Layer metadata selects native solid/cutout entity and armor programs and their culling policy. Textures are copied from the loaded GPU textures into a bounded 2048² RGBA atlas; block-atlas equipment shares the existing atlas. Captured world diffuse-light directions and normals reproduce standard entity shading. Both two-sided and culled faces join the terrain BVH, so curved rays use coherent closest-hit ordering rather than a pasted normal-camera mob image.

The implementation captures eligible loaded living entities within the terrain footprint, including off-screen models; no entity is spawned or moved by the capture. The first-person camera entity is excluded. This does **not** implement the planned returning-light player-body experience or retarded-time/horizon history. F9 freezes the simulation: these are captured poses, not live moving mobs.

Safety bounds: 1.2 million supported entity vertices, shared four-million-triangle scene limit, and 2048² texture atlas. Overflow refuses capture rather than silently truncating. Additional atlas storage is 16 MiB CPU plus 16 MiB GPU until close; geometry shares the existing capture/BVH buffers. GPU pack/unpack/PBO state is isolated during texture copy/upload. GPU slots remain at twelve by reusing the inactive voxel light sampler. Returning to F10 and reopening mesh capture were runtime checked.

## Terrain lighting correction

Pinned Minecraft shader inspection exposed a previous mismatch: terrain `rendertype_solid/cutout.vsh` calls `minecraft_sample_lightmap`, sampling clamped **UV2/256** with filtering. Standard entity shaders use `texelFetch(UV2/16)`. Terrain capture had incorrectly added eight to each light coordinate, sampling entity-style texel centres. The mesh terrain path now follows native terrain sampling; entities retain discrete centres. This explains the broad floor brightness error that remained after the initial mesh checkpoint. K permits a controlled same-scene comparison. The legacy F10 path is unchanged.

## Verification

Final build: `entity-final-build.log`, **47 tests, zero failures/errors**. Runtime: `entity-final-runtime.log`, shader/mixin startup, native model capture, paired comparisons, lensed visual inspection, and F10 cleanup/reentry. No blocks, time, weather or entity population were edited. No automated movement/flicker tests.

Final close-up camera `(16.5,293.62,-14.5)`, yaw0.281/pitch15, nighttime, 854×480, effective vertical FOV77. 96 living entities with supported geometry, zero wholly omitted bodies, 19,896 entity vertices, nine textures. Scene: 2,514,506 triangles, zero missing sections, 19,739 omitted fluid/translucent block incidences. Capture/build/upload 14.579 s. Some feature layers were omitted even though all eligible bodies had supported geometry.

| Same frozen frame / pair directory suffix | Full RGB MAE | Bottom-half RGB MAE |
| --- | ---: | ---: |
| Mobs ON, native terrain lighting ON — `874799505564909488` | 0.0024 | 0.0026 |
| Mobs OFF, native lighting ON — `1201485951223068481` | 0.0070 | 0.0085 |
| Mobs ON, previous terrain light offset — `11382140473605084901` | 0.0188 | 0.0274 |

Vanilla reference images are pixel-identical across all three captures (`entity-close-repeat`, `entity-light-repeat`). Final p95 maximum-channel error 0.0078; 2.35% of pixels differ by more than 8/255. Contact sheet inspected: body positions, textures and terrain match closely; spider glow and shadows still differ. These are appearance measurements with lensing disabled, not a test of geodesic accuracy or a universal visual guarantee.

Earlier wider wall pose `(16.5,303.62,-45.5)`, before the lightmap correction: mobs ON pair `15680397238800330521`, OFF `2574246169754357111`, full RGB MAE 0.0073 versus 0.0097, identical references (`entity-wall-repeat`). Lensed screenshot `run/entity-lensed-wall.png` shows mobs sharing terrain/foreground ordering. Final close-up lensed screenshot `run/entity-final-lensed.png` inspected after L aimed the frozen camera at the source.

Diagnostic timing only, final close-up aimed at source, 427×240 internal, standard paths, mobs/native lighting ON, 120 warmup/300 samples: optical GPU p50/p95/p99 **6.623872/7.430944/7.44064 ms**; frame intervals **8.5658/11.0231/11.3639 ms**. Excludes capture/build/upload; not a target-resolution or FPS acceptance claim.

## Remaining limitations / next work

Native shadow, translucent-cull and eyes layers were explicitly omitted in the measured scene. Additive glow, transparency, hurt/flash overlays, glint, nameplates, leashes and arbitrary special shaders are not reproduced. Non-living entities and block entities are not captured. Equipment support is limited to supported quad layers, not certified across all items/armor. Native per-entity distance culling is not replicated; off-screen geometry is intentionally available to curved rays.

Next extend coherent composition for foreground clouds and transparent/emissive layers, broaden coverage and obtain daytime pairs, then bring the proven representation into live rendering. Texture filtering, viewing boundary and independent curved-mesh convergence remain open. The slow mesh mode is still an experiment, not a perfect reference.
