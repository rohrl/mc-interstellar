# Current handoff — 2026-09-18

Repo C:\work\code\minecraft\interstellar\interstellar; branch codex/world-mesh-reference; origin https://github.com/rohrl/mc-interstellar.git. JDK C:\Portable\jdks\temurin-21.0.12.1, Fabric/Minecraft1.21.1. Read AGENTS.md. Branches/commits/pushes and autonomous runtime checks authorized. No force push/subagents. Preserve worlds/.idea/secrets.

## Owner priorities

Native terrain/live mobs visually accepted. Initial loading acceptable for v1; teleport support excluded. Rain/snow deferred. Continue live-world integration, then packaging, then AA. Quality before FPS. Owner handles movement/flicker acceptance; brief functional movement checks and fixed-pose images remain appropriate. Owner asks about automatic inspection and notices different stars: inspection already refreshes selected sources; star resampling is suspected, no star change made.

## Current checkpoint

F10 streams native chunk meshes with live camera, animated mobs and clouds. Each terrain chunk has its own BVH/GPU row allocation, with a small top-level index. Native section-render/light invalidations queue chunks; Fabric load/unload events also invalidate neighbours. Capture slices5ms; revision changes during capture trigger retry. Publish individual chunks, preserve overlapping entries on camera movement, remove unloaded/retired data. Actors/clouds retain their separate per-frame BVH and persistent entity texture atlas. Shared curved nearest-hit ordering; no baked actor copies or straight-camera overlays.

GPU arenas reserve about1280MiB:4095-wide RGBA32F,16384 triangle rows and4096 node rows, first4 node rows reserved for top-level BVH. Rows align36-float triangles/12-float nodes. RowArena coalesces frees; no defragmentation fallback. Seven-million terrain triangle cap, render distance above16 refused. Window=render distance+1 chunk each way, full height, loaded data only. Initial loading waits; incremental updates may lag. F10 off still frees/reloads on reactivation.

Source refresh now preserves terrain: invalid/refreshing metadata shows normal paused view, then updates optical centre/radius when usable. Initial source selection still manual; anchor removal needs replacement/new selection. No full terrain recapture for mass edits. Camera guard is now256 blocks; exterior/source-availability guards and recovery remain.

F9 beyond128 automatically uses the chunk-based native reference and cannot disable mesh rendering; all F9/F10 access is limited to256. Inside128, F9 M remains monolithic frozen reference; Shift+M selects the chunk-based frozen reference. Switching backend recaptures without advancing world. P vanilla/unbent pair; Space lensing; E mobs; N clouds; U old bounds; K old lighting. F12 live timing.

## Latest range checkpoint

Latest numerical checkpoint: F9 native mesh C runs MeshValidation/MeshRayFixture, four opaque boxes represented independently as GPU triangles/CPU cells. 180 rays at distances32/96/148/252, both mesh layouts and path settings:720 comparisons, zero mismatches/inconclusive/unresolved,577ms. Production distant steps stay enabled. A development fixture sentinel-address bug was corrected; no optical solver change. mesh-fixture-final-build.log:51 tests pass; mesh-fixture-final-runtime.log: suite, shader, before/after P hashes identical, appearance/lensed image inspected, frozen GPU p95=15.975104ms at427×240. docs/mesh-ray-validation.md has exact evidence and narrow scope. C remains voxel-world check outside mesh mode.

viewing-range-build.log:51 tests pass, including CPU analytic capture-boundary brackets at r/r_s16,24,32,64,128. viewing-range-runtime.log: normal backward flight crossed the old128 limit to148 with live lensing; farther F9 auto-mesh pair17029190025041604486 has RGB MAE.0005 against vanilla, inspected. A vertical setup pose plus ordinary flight crossed256 (r263.514) and returned (r252.478), with automatic pause/resume and no terrain reload. Optical GPU p95=22.724896ms at427×240 during edge streaming, not a target FPS claim. docs/viewing-range.md contains details. Camera HUD shows distance/256. No block/time/weather edits; stars unchanged.

## Streaming foundation checks

streaming-final-build.log:50 tests, zero failures/errors. Allocator/coalescing/failure bounds and top-level index tests added. streaming-runtime.log: initial6,185,854 triangles/37.09s, temporary lime-block edit changes only chunk(0,-1), mass addition/removal automatically N64→65→64 without reload. Both originally empty cells (10,292,-8) and (13,300,14) restored/rechecked air. No time/weather or existing scene edits. Strafe across chunk(0,-1)→(1,-1) retained702/729 entries, queued27 new edges; rendering continued. GPU p95=18.435296ms, sampled frames p95=20.5804ms at427×240 during queued updates; not target FPS.

streaming-final-runtime.log: frozen monolithic pair7608212941445763645 and streamed pair5953181202543252939 have identical candidate SHA-256 hashes, identical vanilla references, both RGB MAE.0025 against vanilla. Contact sheet inspected. Streamed terrain6,091,694 + moving9276 equals monolithic6,100,970. Initial streamed capture33.78s. Reference→voxel→F10 cleanup passed. Final running client differs only by an allocation-error guard added after runtime checks; final build passes. Detailed implementation/evidence/limits: docs/streaming-terrain.md.

Client left F10 ON at player(16.5,302,-45.5), yaw.281/pitch.91, creative flight,870×519 window. Owner may move/close. Never open save in two clients.

## Next

1. Repeatable demo packaging, with coverage limits clearly presented. Core live integration/range and the small independent curved-mesh fixture are checked. Arbitrary geometry/material and stronger critical-ray convergence remain open; don't call the fixture universal validation. Preserve the owner's existing world when making a self-contained setup. Camera access is256, with loaded-source/data constraints. Finer section updates and cache reuse across F10 toggles remain useful follow-ups.
2. Remaining transparent/special materials and demo packaging. Rain/snow/teleport deferred. Shadows, eye glow, glint, translucent entity layers, fluids, non-living/block entities incomplete.
3. AA then deeper relativity/player-body/horizon/observer-speed plan. No performance certification yet; two-level traversal currently costs more than monolithic in similar nearby views.

AA remains8ad46eb on codex/terrain-antialiasing, excluded/unverified. Source anchor(14,300,14), N64, COM(16,302,16), r_s8. Prior evidence: docs/live-native-mesh.md, mesh-coverage.md, mesh-clouds.md, mesh-entities.md, native-mesh.md, source-refresh.md. Never revive rejected ordinary-camera overlays.

## Efficient runtime

run/stable-init.gradle quickplays Interstellar Calibration. Redirect logs, gate on joined-the-game, close identified game normally/wait before relaunch. GUI/JDK/Gradle need escalation.

Helpers C:\Users\karol\Documents\Codex\2026-09-13\i-want-to-create-a-minecraft\work\control-minecraft.ps1 and capture-minecraft.ps1. Hold keys120ms: Esc27/F9=120/F10=121/F12=123/M77/N78/U85/E69/K75/P80/Space32/B66. Shift+M via helper -Keys '+m' works. run/send-safe-command.ps1 pastes safely/restores clipboard. Fresh logins may need spectator→test-pose teleport→creative for flight; this is setup, not v1 teleport acceptance.

Comparator: java tools/CompareAppearance.java PAIR_DIRECTORY; three-path mode checks references/candidates. Logs/captures/worlds ignored. Use bounded log reads/numeric comparisons; screenshots only for meaningful rendering checkpoints.
