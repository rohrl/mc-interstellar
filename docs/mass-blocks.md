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


## Selecting a source for the optical lab

A completed inspection now synchronizes its geometry to that player's client. The world HUD displays the selected N and r_s. Open F8 and press S to adopt a black-hole proxy in the controlled-sky lab. Its initial r/r_s is the camera's Euclidean coordinate distance to the COM divided by r_s; this is an explicit coordinate mapping, not a proper-distance measurement. Below 1.05 r_s the lab chooses the freely falling observer; its supported range remains 0.35..64 (static minimum 1.05). Clamping is labelled. Thereafter lab controls change the observer independently of player movement. R restores the JSON reference defaults, and reopening F8 starts the reference lab again.

Extended sources can be selected for metadata but S refuses to treat them as black holes. Incomplete results never become selections. A new inspection replaces the previous selection. Any mass/chunk revision in the source's server world invalidates it on the next server tick; this is conservative, including unrelated chunk activity. No automatic rescan occurs. Disconnects and client-world replacements clear client state. An open source-bound lab restores the configured reference sky on invalidation. Integrated singleplayer pauses while the lab is open; multiplayer can continue ticking.

Selection checks cost O(selected players) per server tick with no extra world queries. Each selected player gets at most one invalidation packet for a batch of changes, not one per changed block. Inspection retains its existing cell budgets. The payload is registered on both sides through Fabric's typed play networking, and receiver work runs on the client thread. API reference: https://maven.fabricmc.net/docs/fabric-api-0.106.0%2B1.21.1/net/fabricmc/fabric/api/networking/v1/PayloadTypeRegistry.html (same 1.21.1 API family; compiled against the project's pinned 0.102.1).
### Bridge runtime verification — 2026-09-15

Build and 25 existing tests pass. The saved edited cluster had N=63, r_s=7.875, enclosing R=3.496. F8/S used camera r/r_s=0.706777535 and correctly selected a falling observer. R restored static r/r_s=8. Inspecting the isolated block at (12,301,10) replaced the selection and S refused black-hole optics. Reinspecting the main source then placing one temporary block into air at (20,306,20) cleared its eligibility; the temporary block was removed with a mass-only replacement. Reinspect/save/disconnect/reload left no selection, confirmed through S. Existing source edits were preserved.

No multiplayer live-screen invalidation, dimension-transfer, dedicated-server startup, or live capped/unknown/stale-job test was run. Source-state cleanup at reload and mass revision invalidation were exercised through actual packets, not inferred from unit tests. No new GPU cost measurement; the shader is unchanged.