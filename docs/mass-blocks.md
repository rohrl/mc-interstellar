# Mass blocks and bounded cluster inspection

Mass Block (interstellar:mass_block) is registered as a normal block and block item, listed in the Functional Blocks creative group. It currently references Minecraft's lodestone-top texture as provisional art. No Minecraft texture file is copied into the repository. Each block contributes 0.125 coordinate blocks to the effective Schwarzschild radius.

## Try it

Get a Mass Block from the creative inventory or use /give @s interstellar:mass_block. Place face-connected blocks and right-click with an empty hand to inspect. The operator command /interstellar inspect x y z performs the same inspection within 128 blocks of the player. Results appear in chat and the server log.

Blocks do not yet activate world lensing or select the F8 source. This checkpoint provides source data and inspectable geometry; shader source selection/networking and a maintained cluster index remain work for the next part of iteration 2. Terrain scene rendering remains iteration 3.

## Model

For N equal-mass blocks, the centre is the arithmetic mean of their block-centre coordinates (x+0.5, y+0.5, z+0.5). The enclosing radius R is the farthest block corner from that centre:

R = max sqrt((abs(dx)+0.5)^2 + (abs(dy)+0.5)^2 + (abs(dz)+0.5)^2).

Set r_s=0.125*N and compactness C=r_s/R. A complete cluster with C>=1 is labelled a black-hole proxy; otherwise it is an extended source. This is an explicit spherical approximation to a chosen coordinate geometry, not a relativistic collapse theorem for arbitrary shapes. It does not model pressure, binding energy, matter dynamics, proper distances or a nonspherical metric. No actual collapse, terrain deletion, block movement or server-side light-ray integration occurs.

The enclosing radius avoids declaring a very long chain compact merely because its block count is large. A 4x4x4 cube gives N=64, R=sqrt(12), r_s=8 and C=2.309. A straight 64-block chain has the same mass but R>32, and does not qualify. Shape therefore matters under this proxy. Only face adjacency connects blocks; edge/corner contact alone does not.

## Runtime architecture and limits

This is an on-demand inspector, not a live global clustering index. Standard Minecraft chunk persistence stores the blocks. Fixed mass needs no BlockEntity, per-block ticker, duplicated persistence or custom saved-data format. New inspections recompute the component, so edits and world reloads naturally update reported geometry.

- At most eight jobs globally, one per player. A newer request replaces that player's pending inspection.
- Four round-robin slices of at most 64 cell queries per server tick, for at most 256 queued inspection queries total. Request validation additionally reads the one target cell.
- At most 4096 mass blocks per job; final centre/radius reduction is bounded by that same cap. Jobs also track their neighbouring visited cells, so 4096 is not a bound on all visited air cells.
- No chunk is force-loaded. Encountering an unknown neighbouring chunk produces PARTIAL. Discovering more than the cap produces LIMIT. Neither result is assigned a compactness phase.
- Mass-block additions/removals and any chunk load/unload advance a per-world revision. A changed revision cancels a pending inspection; disconnected players and dimension changes discard it. This deliberately conservative invalidation can cancel jobs due to unrelated chunk activity.
- Server shutdown clears jobs/revisions. Normal vanilla block callbacks are assumed; mods bypassing callbacks have not been tested.

No result cache is retained for rendering yet. A future maintained index/source bridge must preserve these incomplete/stale-result distinctions. No server inspection wall-time benchmark has been collected; the above are structural work limits, not timing guarantees.

## Executed checks — 2026-09-15

Build passes with 25 JUnit tests total. Seven new probe tests cover centre/corner geometry, cube compactness, equal-mass elongated shape, face connectivity, fresh-inspection split/merge, per-call read budget, unknown cells, capacity limits and empty seeds.

Automated Minecraft checks in the disposable Interstellar Calibration world:

1. Placed a 64-block cube at x=14..17, y=300..303, z=14..17, spanning four loaded chunks; replaced only air.
2. Inspection returned N=64, centre=(16,302,16), R=3.464, r_s=8, C=2.309.
3. Removed only the newly placed mass-block slice x=15: inspections returned N=16 at x=14 (R=2.872, C=0.696) and N=32 at x=16 (R=3, C=1.333).
4. Replaced that slice into air and recovered the original N=64 result.
5. Empty-hand right-click returned the same result; the loot command returned one named Mass Block from the block loot table. Block model rendered without missing-texture placeholders.
6. Saved/quitted to title and reloaded the world; a fresh inspection returned the same N=64 and geometry.

A separate single Mass Block at (12,301,10) supports the test player at (12.5,302,10.5); it is not connected to the cube. The test structures remain in this disposable world. No existing terrain was removed.

Live unknown-chunk/capacity/cancellation/multiplayer stress cases and dedicated-server startup were not exercised. The pure probe's unknown/capacity behaviors are unit tested. This checkpoint does not certify the untested runtime paths.
