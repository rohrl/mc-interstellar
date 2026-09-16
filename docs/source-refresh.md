# Automatic source refresh

## Behavior

A selection now tracks the original inspected block position for the current player/world session. Relevant mass edits and chunk load/unload events immediately withdraw stale optical metadata, then trigger a bounded rescan. Unrelated chunks remain ignored for completed selections. F10 preserves its armed state while waiting and rebuilds the terrain renderer only after a complete black-hole source arrives.

- **Split:** follow the connected fragment containing the inspected block. Do not silently switch to a larger fragment.
- **Anchor removed:** show normal view and wait for a mass block at the same position; the player can inspect another source instead.
- **Unloaded/partial:** wait for relevant chunk events; never force-load chunks or poll the whole region.
- **Extended source:** metadata remains available, but F10 pauses until black-hole compactness returns.
- **Over 4096 blocks:** show the limit and wait for a relevant edit; never render an incomplete mass estimate.
- **Manual F10 off:** stays off across later source updates. World changes/disconnect clear the live-view session. Tracking is not persisted across reconnects.

F9 remains frozen: a source change invalidates its snapshot, and reopening obtains current metadata. It does not automatically replace the frozen scene while a diagnostic is being inspected.

## Work bounds

`RefreshSchedule` debounces bursts for five quiet server ticks. A world-epoch change during a probe discards it and backs off for 20 ticks; later events cannot shorten that cooldown. At most eight probes are queued, sharing four 64-cell slices per tick (256 queries total), with a 4096-mass-block limit per probe. Each tracked player has at most one queued probe. Waiting requests remain pending if all eight slots are occupied.

Completed or incomplete probes do not repeatedly run while the world is unchanged. Source dependencies initially include the anchor shell; probing expands them to include newly visited/unknown chunks. A complete result replaces them with the enclosing-radius-plus-neighbour-shell chunk bounds. This allows growth/partial components to wake when newly relevant chunks load. In-flight epochs remain conservatively world-wide.

Separate state packets distinguish refreshing, unloaded, removed and limited sources from complete metadata. The client discards stale payloads during these states and keeps F10's armed intent separately from renderer lifetime. Native capture buffers, textures and benchmark queries are released when the source changes. Automatic updates log results without repeating chat announcements; explicit inspection still reports its summary.

This is event-driven maintenance of one anchored selection per player, not a global cluster index or automatic selection of a replacement fragment.

## Verification — 2026-09-15

Build and 45 tests pass. Five new regressions cover initial one-shot scheduling, debounce, retry cooldown, events during a probe, idle incomplete state and expansion of watched chunk bounds. Existing probe tests cover split/merge topology, incomplete components and the mass limit.

Agent-operated runtime checks in the saved world:

1. Main source N=63/r_s=7.875. One temporary attached mass block at (13,300,14), placed only in air, changed it automatically to N=64/r_s=8.0 and rebuilt live terrain. Removing it restored N=63. No reinspection or F10 press.
2. Temporarily removed the verified mass block at inspected anchor (14,300,14): paused normal-view HUD explicitly reported removal. Restoring the same block resumed the original N=63 source automatically.
3. Created one temporary mass block in air at (512,310,512). Selecting it while F10 remained armed showed the extended-source paused HUD. Leaving its chunks produced UNLOADED; returning refreshed the same N=1 source without inspection.
4. Filled only air in (512,310,512)..(514,312,514). The confirmed 26 new blocks plus the owned anchor produced N=27/r_s=3.375 and automatically resumed F10.
5. Removed the owned middle nine-block plane at x=513. The selected fragment became N=9 and extended; the separate opposite fragment was not selected. Restored that plane: N=27 and F10 resumed automatically.
6. Removed all 27 confirmed temporary blocks, returned to the reference camera and selected the preserved N=63 main source. F10 resumed without another toggle. Main anchor was restored; no temporary mass blocks remain.

Runtime log: refresh-runtime.log. Visually inspected paused HUDs: run/refresh-removed.png, run/refresh-extended.png and run/refresh-unloaded.png. Logs/images are local and ignored by Git. Multiplayer queue saturation, dimension transfer, sustained-edit starvation and the 4096-block runtime limit have not been exercised in-game. A continuous stream of changes may legitimately keep the source paused until a stable complete inspection is possible.

## Final regression and lifecycle checks

Manual F10-off remained off when explicit reinspection delivered new metadata: the live-source adoption count did not change until F10 was enabled again. F9 V regression reported zero flat mismatches (39x26; 479 flat hits, 641 lensed opaque hits). The restored live image was captured in run/refresh-final.png; log refresh-final-runtime.log. Source/world state is cleared on disconnect or dimension change; an additional guard clears metadata if respawn replaces the server player without replacing the client world. That respawn guard compiled successfully but was not runtime-tested by killing the player's character.

Final publication checks continued on 2026-09-16. Source-refresh behavior was tested on 2026-09-15; final build includes the respawn guard and updated frozen-preview wording. All 45 tests pass. The temporary cluster and attached block were removed and the original main anchor restored before the client saved and exited on 2026-09-15.

## RMB placement fix and enlarged scene — 2026-09-16

The owner reported that RMB would not place more mass blocks. MassBlock.onUse was consuming the click for inspection even with a held item. It now returns PASS if either hand holds an item, allowing vanilla placement/use; inspection requires both hands empty. No sneak modifier is required. Runtime verified actual RMB placement into a confirmed air cell at (16,302,13), with automatic N=64->65 refresh, then removed that single test protrusion and verified actual empty-hand RMB inspection of the cube (anchor 16,302,14). Off-hand placement is allowed by the guard but was not separately exercised in-game.

The owner explicitly authorized enlarging the edited source. Filled only air in (14,300,14)..(17,303,17): 37 added blocks, resulting in a complete 64-block cube, COM=(16,302,16), enclosing radius=sqrt(12), r_s=8. Other scene blocks were preserved. Earlier absent-anchor/small-source notes are superseded by this authorized enlargement. Final image run/placement-final.png was visually checked; client left in F10 with an 870x519 window. The player can now add blocks normally.

Final benchmark: 2560x1440 output / 1280x720 internal, STANDARD path, lensing and live refresh on, N=64, r/r_s=4.80295 at benchmark start, 11579 opaque cells, zero unknown/unsupported. RTX 5070 Ti / NVIDIA 616.92, 120 warmup frames + 300 samples: GPU p50=4.960544, p95=5.066368, p99=5.119360 ms; frame-interval p95=9.423199 ms. The player had moved from the earlier reference view, so this is not a fixed-camera comparison with previous timings or full-frame GPU measurement.

Final build/45 tests pass. Log placement-runtime.log records successful fill, actual RMB air-before/mass-after markers, automatic metadata updates, empty-hand inspection and timing. Initial raw SendKeys commands were corrupted by movement/Caps Lock and did not execute. A local clipboard-preserving helper in run/send-safe-command.ps1 made command input reliable; no execution-policy setting was changed. No temporary mass blocks remain outside the authorized cube.
