# Current handoff — 2026-09-19

Repo C:\work\code\minecraft\interstellar\interstellar; branch codex/world-mesh-reference; origin https://github.com/rohrl/mc-interstellar.git. JDK C:\Portable\jdks\temurin-21.0.12.1, Fabric/Minecraft1.21.1. Read AGENTS.md. Branches/commits/pushes and autonomous runtime checks authorized. No force push/subagents. Preserve worlds/.idea/secrets.

## Owner priorities

Native terrain/live mobs visually accepted. Initial loading acceptable for v1; teleport support excluded. Rain/snow deferred. Continue live-world integration, then packaging, then AA. Quality before FPS. Owner handles movement/flicker acceptance; brief functional movement checks and fixed-pose images remain appropriate. Owner asks about automatic inspection and notices different stars: inspection already refreshes selected sources; star resampling is suspected, no star change made.

**Latest override,2026-09-19:** packaging is paused; owner requested AA then performance with minimal quality/fidelity loss. Owner rejected the soft edge-filter blur. Unverified packaging is isolated at2fe8674 on codex/demo-packaging-wip, never built/launched. Active branch adds2x traced AA + sharper reconstruction, optional EDGE/OFF, adaptive paths and dynamic upload reuse. Preserve the accepted appearance; optimize based on paired images and timings, not FPS alone.

## Current checkpoint

Latest bounds optimization: `bounds-build.log` build51 tests pass; `bounds-runtime.log` runtime shader and2880 independent sampled original/fast bounds hit comparisons pass. Fast bounds reuse chord reciprocals and vectorize slab tests, preserving explicit near-parallel containment and previous padding/order. Matched frozen2xAA/adaptive GPU p95: small32.580→30.121ms;1440p120.778→111.447ms (7.7%). Same-frame original/fast images pixel-identical at both resolutions; small candidate inspected. Full details and pair IDs: docs/aa-performance.md. F9 T toggles bounds, Ctrl+Shift+P captures comparison. Default fast in F9/F10. This scene had28 mobs versus109 previously: do not claim cross-session timing differences as gains. Further GPU traversal work remains the priority.

Client exited normally and saved before the final live smoke check/window restore could run. No new live benchmark this iteration; frozen streamed renderer and both fixture layouts verified. Do not launch a second client if owner has since started one. Last tested pose(16.5,302,-45.5), yaw.281/pitch.91, N65 preserved. Local options show fullscreen:false after exit. No block/time/weather/quality-config edits.

Latest AA/performance: adaptive-build.log build51 tests pass. adaptive-runtime.log expanded1440 independent original/adaptive hit checks all pass. Frozen streamed p95 at427×240 internal:2xAA original52.367ms→adaptive36.102ms; OFF27.598→19.573ms. Original/adaptive image pairs at straight/downward views differ over8 levels in~0.007% pixels. Full-resolution4-ray AA reference exists;2x improves mean error modestly, still costly and half-resolution. Dynamic live uploads retain GPU/native buffers; after600 frames both textures still only one allocation, with changing mob geometry. Detailed measurements/limitations: docs/aa-performance.md. F9 A cyclesAA; G toggles adaptive; Shift+P quality reference; Ctrl+P old/new path; P preserves vanilla unbent comparison withAAOFF. Benchmarks now include resolve.

Full-screen live2560×1440 output/1280×720 internal,2xAA/adaptive: GPU median159.902ms/p95167.944ms; frame median166.240ms (~6FPS). Optical GPU work dominates; do not imply the target is met or extrapolate the small-window improvement. Window restored afterward. Prioritize ray/geometry traversal profiling/acceleration next, not just CPU upload costs.

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

1. Continue performance work while retaining accepted appearance. The first same-quality GPU reduction is~31% at the small fixed test pose; no1440p/60 claim. Profile ray/BVH traversal next; don't silently disable AA/effects or omit geometry to claim a same-quality speedup. Keep original/adaptive comparisons and independent fixtures. Packaging remains paused on its WIP branch.
2. Remaining transparent/special materials and demo packaging. Rain/snow/teleport deferred. Shadows, eye glow, glint, translucent entity layers, fluids, non-living/block entities incomplete.
3. AA then deeper relativity/player-body/horizon/observer-speed plan. No performance certification yet; two-level traversal currently costs more than monolithic in similar nearby views.

Original AA WIP8ad46eb remains preserved on codex/terrain-antialiasing; its two-ray idea is now adapted and tested in the current renderer. Owner's current source anchor(14,300,14), N65, COM approximately(16.01,302.04,15.99), r_s8.125; do not restore the old64-block scene. Prior evidence: docs/live-native-mesh.md, mesh-coverage.md, mesh-clouds.md, mesh-entities.md, native-mesh.md, source-refresh.md. Never revive rejected ordinary-camera overlays.

## Efficient runtime

run/stable-init.gradle quickplays Interstellar Calibration. Redirect logs, gate on joined-the-game, close identified game normally/wait before relaunch. GUI/JDK/Gradle need escalation.

Helpers C:\Users\karol\Documents\Codex\2026-09-13\i-want-to-create-a-minecraft\work\control-minecraft.ps1 and capture-minecraft.ps1. Hold keys120ms: Esc27/F9=120/F10=121/F12=123/M77/N78/U85/E69/K75/P80/Space32/B66. Shift+M via helper -Keys '+m' works. run/send-safe-command.ps1 pastes safely/restores clipboard. Fresh logins may need spectator→test-pose teleport→creative for flight; this is setup, not v1 teleport acceptance.

Comparator: java tools/CompareAppearance.java PAIR_DIRECTORY; three-path mode checks references/candidates. Logs/captures/worlds ignored. Use bounded log reads/numeric comparisons; screenshots only for meaningful rendering checkpoints.
