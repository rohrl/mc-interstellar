# Current handoff — 2026-09-18

Repo: C:\work\code\minecraft\interstellar\interstellar. Branch codex/world-mesh-reference, origin https://github.com/rohrl/mc-interstellar.git. JDK C:\Portable\jdks\temurin-21.0.12.1, Fabric/Minecraft1.21.1. Read AGENTS.md. Branches/commits/pushes and autonomous runtime checks authorized; no force pushes/subagents. Preserve worlds, .idea and secrets.

## Owner priorities

Native mesh terrain appearance accepted. Initial loading is acceptable for v1; teleport support excluded. Rain/snow deprioritized. Prioritize live exploration and animated mobs, then incremental terrain edits/chunk streaming. Quality before FPS. AA stays after integration/demo packaging. Automated movement/flicker acceptance deferred to owner; brief functional movement/animation checks and occasional milestone images are appropriate.

## Current implementation

F10 now uses native terrain, live camera and animated living mobs/clouds. Large terrain-only BVH is captured once; separate small BVH rebuilt per rendered frame with native entity interpolation. Entity texture tiles persist across frames. Both trees share nearest opaque-hit and nearest cloud ordering in the shader; no baked mob copies or ordinary-camera overlays. Uses existing12 sampler slots. Moving geometry capped200,000 triangles. Pausing the game retains the moving scene. Source/exterior/128-block automatic recovery retained.

IMPORTANT: terrain geometry and baked light/AO stay captured at activation. Block edits/chunk streaming are NOT done; HUD labels this. F10 off releases data, so reactivation repeats initial capture. Initial capture while simulation runs is not atomic. Do not call this complete live-world integration.

Camera-centred terrain footprint: render distance plus one chunk each direction, full height, loaded chunks only. At distance12:27×27 request, ~6.2million triangles/~37s. Seven-million-triangle cap; configured distance above16 refused. F9 M remains frozen comparison, Space lensing, E mobs, N foreground clouds, U old terrain bounds, K old lighting, P vanilla/unbent pair. F12 live timing.

## Latest verification

live-mesh-final-build.log: successful,47 tests, zero failures/errors. live-mesh-runtime.log: shader startup, initial terrain6,184,960 triangles/37.73s, first moving scene12,960 triangles/96 mobs/2,688 cloud triangles. Thousands of updates with changing geometry/entity counts. Inspected run/live-mesh-a.png and live-mesh-b.png six seconds apart: visible mob motion, coherent lensed wall/pillar, no frozen mob copies. Brief strafe changed position without terrain recapture. F10 off/on passed; second terrain6,185,826 triangles/36.15s. Final source differs from running client only by reduced diagnostic log frequency/indentation, rebuilt successfully.

Diagnostic GPU p95=9.431040ms, sampled frame p95=11.3300ms at427×240 internal, close-up r/r_s3.95428. Not target-resolution FPS. See docs/live-native-mesh.md for architecture, evidence, controls and exact limits.

Client left F10 ON, player ~(10.46095,292,-14.50311), yaw0.281/pitch15, creative flight, small870×519 window. Owner may move/close. Never open save in two clients. No block/time/weather/population edits made. Rain/snow was naturally active and remains unrendered by lensing.

## Next work

1. Reusable terrain section/chunk geometry, bounded dirty-section updates and ordinary-movement streaming. Preserve source stability/recovery and accepted appearance; avoid full terrain rebuilds for routine edits/movement. Initial load allowed; teleport out of scope. Add scene cache lifecycle before claiming toggle reuse.
2. Broader camera access (128 guard remains), remaining transparent/special material coverage and live appearance checks. Rain/snow deferred by owner. Native mob shadows, eyes/glow, hurt/flash, glint, translucent layers, non-living/block entities incomplete.
3. Independent curved-mesh convergence checks and then optimization/packaging. Frozen mesh is not a certified optical oracle. Actual player-body returning light, horizon experiences and observer speed remain later.

## Stable foundations / evidence

AA WIP8ad46eb on codex/terrain-antialiasing, excluded/unverified. Source anchor(14,300,14), N64, COM(16,302,16), r_s8. Ignored terrain config enabled=true,distantPrototype=true,renderScale=.5; live native path enables mesh/sky independent of old distant flag.

Earlier evidence: docs/mesh-coverage.md (U comparison, lower-half mountain MAE.0364→.0206 with snowfall), docs/mesh-clouds.md, docs/mesh-entities.md (lightmap parity), docs/native-mesh.md. Source selection/RMB/recovery: docs/source-refresh.md, docs/stable-exploration.md. Never revive rejected straight-camera overlays (87dd3a1 reverted0a264b0).

## Efficient runtime

run/stable-init.gradle quickplays Interstellar Calibration. Redirect logs; gate readiness on joined-the-game. Close identified Minecraft window normally and wait for exit before relaunch. GUI/JDK/Gradle need escalated desktop context.

Helpers: C:\Users\karol\Documents\Codex\2026-09-13\i-want-to-create-a-minecraft\work\control-minecraft.ps1 and capture-minecraft.ps1. Held keys120ms: Esc27,F9=120,F10=121,F12=123,M77,N78,U85,E69,K75,P80,Space32,B66. run/send-safe-command.ps1 safely pastes commands/restores clipboard. Fresh logins may not fly: spectator→teleport→creative retains flight. Teleport is only a test-pose setup, not a v1 feature acceptance test.

Comparator: java tools/CompareAppearance.java PAIR_DIRECTORY; three-path mode verifies reference identity. Logs/captures/worlds ignored. Use bounded log reads, numerical comparisons and occasional visual milestones; no screenshot for simple HUD/docs/logging changes.
