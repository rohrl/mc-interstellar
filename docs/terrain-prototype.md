# Minecraft terrain-lensing prototype

Inspect a complete black-hole proxy, stand outside 1.05 r_s and within the captured region, then press **F9**. It captures actual nearby client-world blocks, including off-screen blocks, and traces rays through them on the GPU. This is a frozen preview screen, not live replacement of Minecraft's world renderer. F8 remains the independent sky lab.

| Control | Result |
|---|---|
| Space | Lensing on/off, using the same geometry and camera |
| Arrows | Look around the frozen scene in five-degree increments |
| L | Aim at the selected source |
| Q | Half/full framebuffer resolution |
| J | Standard/fine curved-path segment target |
| V | Check flat geometry against independent cube intersections; audit off-screen lensed hits |
| B | Benchmark the terrain shader pass |
| Esc | Return to Minecraft; move and reopen F9 to recapture |

Persistent defaults load from `run/config/interstellar-terrain.json` on F9 open: `enabled` (true by default) and `renderScale` (0.5 or 1, default 0.5). Invalid files are preserved and defaults used. Session controls are not saved. A disabled preview does not capture or allocate terrain textures.

## Data and rendering bounds

Capture a 96^3 cube centered on the floored cluster COM, with at most 8192 cell checks per rendered frame and a soft 3 ms time budget checked every 64 cells. No forced chunk loading or server ray computation. Snapshot capture is incremental, not atomic in multiplayer; the integrated singleplayer world pauses. Reopening refreshes the geometry. Source invalidation, world changes and resource reloads prevent the old snapshot from continuing to render. GPU objects are deleted on screen close.

The voxel texture is an R32F 2D flattening, 9216x96 (3.375 MiB): air, unknown, unsupported, or material index. At most 509 distinct supported material rows plus three reserved entries. Each supported face uses the first baked face quad, with an affine mapping from its positions to atlas UVs. This preserves basic cube-face UV rotations. Tint uses the first occurrence of that block state; biome tint variation and additional overlay quads are not represented. Nonopaque/non-full cubes and unsupported models are visible pink obstacles, not silently reconstructed as accurate geometry. Fluids, foliage, stairs, entities and the player body are not implemented.

Pixels trace the Schwarzschild spatial orbit in its radial/tangential plane. The observer uses a static orthonormal frame outside 1.05 r_s. World block coordinates are explicitly interpreted as a Cartesian embedding of Schwarzschild areal-radius/angular coordinates, not Euclidean proper distances. Initialize u=r_s/r and u'=-mu*u*sqrt(1-u)/sqrt(1-mu^2). RK4 angular steps are at most 0.02 rad and target chord lengths of 0.45 blocks (standard) or 0.225 (fine), using the local coordinate speed estimate. Limits are 2048 steps or 16 radians. Each chord uses ordered voxel traversal, so foreground cubes occlude background rays before horizon capture. Chords are approximations to curved segments; finite-surface convergence remains open.

The flat comparison traces straight rays through the same cells, including the mass blocks. The lensed view stops at the selected horizon. Other clusters are ordinary captured blocks; this is one selected spherical metric, not combined gravity. The sky lab's falling-observer horizon crossing remains separate from this exterior terrain prototype.

No gravitational spectral shift, beaming, realistic illumination transport, shadows or ambient occlusion is applied. Textures use illustrative directional face lighting. Point sampling and reduced resolution alias narrow images. Larger quality settings are not accuracy guarantees. See the separate critical-ray limits.

## Edges and missing data

Foreground terrain can make a sharp occlusion edge across a black-hole image. It should not be faded into background images. Other rays can form secondary images of the floor around the shadow; the test scene visibly shows this. The finite floor and wall themselves also have real block edges. This is distinct from the amber grid: a ray that leaves the snapshot ends at an explicit unavailable-data boundary. It may otherwise have hit uncaptured terrain or returned into the region, so this boundary is not a physical sky. Solid amber is an unloaded cell. Pink means an unsupported cell or exhausted trace budget. No smooth blend is used to conceal missing geometry.

## Verification — 2026-09-15

Build and 31 JUnit tests pass. The new independent slab-intersection reference has axis-aligned, inside-origin, parallel, behind-camera, nearest-occluder and diagonal tests. V reads GPU hit cells, not displayed RGB, and compares straight-ray results against brute-force cube intersections. CPU intersection work is capped at about 12 million box checks by reducing sample count for dense snapshots. This validates scene encoding, camera mapping and straight traversal; it does not independently validate the curved finite-surface integration.

Runtime checks: striped background visibly curves around the shadow; the foreground gold pillar remains in front; Space restores straight geometry and mass blocks. Several landscape/portrait V runs have zero flat mismatches. A turned portrait view recorded 933 lensed opaque hits, including 192 cells outside the ordinary camera frustum with a two-block margin, and zero sampled trace-budget failures. This establishes actual off-screen scene access, not just screen-space resampling. Sample sizes and conditions are logged for each V press. Do not interpret the count as a global fidelity guarantee.

A native driver crash on a second snapshot was traced to voxel upload reading beyond its buffer under inherited pixel-unpack state. Subsequent logging found Minecraft leaving `UNPACK_SKIP_ROWS=19`, later 10. Upload now saves, resets and restores unpack layout and PBO binding; readback separately isolates pack state. Repeat captures succeeded after the fix. The original crash report is local under run/, never a tracked project artifact. Resources and saved worlds were preserved.

## Performance

RTX 5070 Ti, NVIDIA 616.92, 2560x1440 window. Source N=63, r_s=7.875, observer r/r_s=4.51426. Scene initially 6964 opaque cells, seven materials, zero unknown/unsupported. Owner later removed four wall blocks; these edits were preserved (6960 cells in subsequent checks). Capture took about 0.90 s of wall time, spread across frames.

| Mode | Internal resolution | GPU p95 |
|---|---|---:|
| Flat, standard | 1280x720 | 0.894016 ms |
| Lensed, standard | 1280x720 | 3.601376 ms |
| Lensed, fine | 2560x1440 | 16.449408 ms |

Each timing used 120 warmup frames and 300 samples. The final row came from an owner-triggered benchmark in the same client and was verified in its log. Shader timings exclude the final upscale and HUD; they are not full-frame GPU timings. Lensed half-resolution sampled frame-interval p95=8.7562 ms; full-resolution fine p95=25.3531 ms, both including cap/vsync. Full-resolution fine does not satisfy the 60 FPS overall target in that run. Half resolution is the deliberate default for this prototype.

## Saved demonstration scene

World: Interstellar Calibration. Original source: x14..17, y300..303, z14..17, now 63 blocks after owner edits. Inspect `(14,300,14)`. Reference player position `(16.5,302,-19.5)`, yaw/pitch 0; a stone-brick support is at `(16,301,-20)`. F9 uses the eye/camera position, not foot position. L aims precisely at COM.

Added only into air: stone-brick floor x=-16..48, y=288, z=-24..48; striped wall x=-16..47, y=289..318, z=44; gold foreground pillar x=8..10, y=289..314, z=-4..-2. Wall consists of eight 8-block-wide white/blue/white/red/yellow/white/blue/white bands. Existing source and owner edits were not overwritten. Structures remain for exploration. They are regular Minecraft blocks, not shader-invented scenery.

## Next work

Live camera/world integration and bounded updates; more explicit boundary presentation; finite-surface numerical comparison and filtering; broader natural-terrain/model coverage. Interior terrain, player-body images, transparency and spectral transport remain unfinished. Dedicated-server/multiplayer behavior and dense/capped-material capture have not been runtime stress-tested.
## Final verification addendum — 2026-09-15

Final-build V pair used the same logged camera (16.5,303.6199998855591,-19.5), yaw=0.8451603, pitch=2.6502786, aspect=1.7791666666666666. Standard/fine path targets 0.45/0.225 both reported zero flat mismatches, 721 flat hits, 982 lensed opaque hits, one off-screen hit with margin and zero lensed unresolved samples. Aggregate counts agree; individual curved hit-cell convergence was not measured. Config enabled=false was runtime checked and the exact original bytes restored. Repeated captures after the pixel-unpack fix succeeded. Resource-reload invalidation is implemented but the attempted F3+T input in the screen did not trigger a reload, so that path is not runtime verified.

Final-build 1440p-window timing, internal 1280x720, STANDARD path, lensing on, source aimed with L, 6960 opaque cells: GPU p50=3.638848, p95=3.740352, p99=3.791520 ms; sampled frame-interval p95=8.6177 ms. This is the most recent default-mode measurement. The client is left open for optional exploration; no user testing is pending.