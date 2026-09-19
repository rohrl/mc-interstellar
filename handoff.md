# Current handoff — 2026-09-19

Repo: C:\work\code\minecraft\interstellar\interstellar. Branch codex/world-mesh-reference; origin https://github.com/rohrl/mc-interstellar.git. JDK C:\Portable\jdks\temurin-21.0.12.1; Fabric/Minecraft1.21.1. Read AGENTS.md. Normal edits, commits/pushes and autonomous runtime inputs authorized. No force push or subagents. Preserve worlds/.idea/secrets and owner edits.

## Priority and preserved work

Owner loves current appearance; optimize FPS with minimal fidelity loss. Packaging paused on codex/demo-packaging-wip at2fe8674 (pushed, unverified, never built/launched). Original AA WIP8ad46eb remains on codex/terrain-antialiasing; do not cherry-pick its obsolete renderer. Current2xAA is already adapted. Owner rejected soft EDGE blur; retain sharp default. Rain/snow, teleport support and star differences deferred; owner handles movement/flicker acceptance. Never revive rejected ordinary-camera background overlays.

## Current verified optimization

Constant-width mesh addressing specializes integer texel division for4095/4096 widths, with general fallback. F9/F10 default enabled; no geometry, optics, resolution or AA reduction. `fetch-build.log`: build51 tests pass. `fetch-runtime.log`:5760 sampled CPU/GPU comparisons pass (180 distinct rays across addressing/bounds/path/layout/step variants); zero mismatches/inconclusive/unresolved. Same-frame streamed images pixel-identical at854x480 and2560x1440; small candidate inspected.

Matched frozen2xAA/adaptive/fast-bounds GPU p95: small44.300→37.261ms (15.9%);1440p160.323→135.443ms (15.5%). Fullscreen optimized median frame133.422ms (~7.5FPS), still far from target.50 mobs this session; do not attribute differences from prior sessions to this change. Exact timings, pair IDs, assumptions and earlier AA/step/bounds results: docs/aa-performance.md. No block/time/weather/quality-config edits.

Next: profile larger ray/BVH traversal costs. Each short curved chord restarts terrain and moving-tree traversal. Preserve same-scene baselines and independent numerical checks; don't trade coverage/AA for a claimed same-quality gain. Remaining materials/features: fluids, transparent/special block models, non-living/block entities, shadows/eye glow/glint/translucent entity layers. Packaging/deeper relativity/player-body remain later.

## Runtime architecture

F10 retains chunk BVHs in GPU row arenas; native section/light and chunk events queue bounded5ms capture slices. Publish individual replacements; retain overlapping chunks on movement. Actors/clouds use a separate per-frame BVH and reusable textures/staging buffers. Both trees share curved nearest-hit/cloud ordering. Source refresh retains terrain and automatically pauses/resumes; initial source inspection still manual. Camera guard256 plus exterior/source-availability guards. F10 off frees the cache, so reactivation recaptures (~35–40s).

GPU arenas:4095-wide RGBA32F,16384 triangle rows/4096 node rows (~1280MiB), first4 node rows reserved for top-level index. RowArena coalesces frees, no defragmentation fallback.7M terrain triangle cap; render distance above16 refused. Camera window=render distance+1 chunks each side, full height, loaded chunks only. Monolithic/moving textures4096 wide. Details: docs/streaming-terrain.md, live-native-mesh.md, viewing-range.md. Independent fixture scope: docs/mesh-ray-validation.md; boxes/sampled hit cells, not arbitrary-material certification.

## Scene and efficient checks

Owner source anchor(14,300,14), N65, r_s8.125, COM approximately(16.01,302.04,15.99). **Do not restore old N64.** Test player pose(16.5,302,-45.5), yaw.281/pitch.91. Owner may move/close client; check process/log before inputs. Never launch two clients against the save.

Launch with JAVA_HOME above: .\gradlew.bat -I run/stable-init.gradle runClient, redirect logs. Gate on joined-the-game before setup commands (shader startup may take longer than a fixed wait). Fresh player may need spectator→test teleport→creative for flight. Setup teleport is not v1 teleport acceptance. GUI/Gradle need escalation.

F9 pauses; wait snapshot, then Shift+M streamed or M monolithic. Switching backend recaptures while paused and sets lensing OFF: Space restores it. Gate on ready logs, never press M twice during capture. F9 beyond128 automatically requires streamed mesh. C independent fixture; B benchmark (120 warmup/300 samples); F12 live benchmark. AA A; adaptive G; bounds T; addressing R. P vanilla/unbent; Shift+P4-ray quality; Ctrl+P original/adaptive; Ctrl+Shift+P bounds; Alt+P addressing. E entities/N clouds/J fine/Q scale. Comparisons require F9 pose to match actual player camera; metadata records settings. F11 fullscreen; restore small window after tests.

Helpers: C:\Users\karol\Documents\Codex\2026-09-13\i-want-to-create-a-minecraft\work\control-minecraft.ps1 and capture-minecraft.ps1; held native keys120ms. run/send-safe-command.ps1 preserves clipboard. Comparator: java tools/CompareAppearance.java PAIR_DIRECTORY; three-path mode for arbitrary images. Logs/captures/saves ignored. Prefer numeric pairs and bounded logs; one milestone image, no repeated screenshot polling. Historical results are in feature docs/progress/decision log, not duplicated here.

Last client state: F10 active, small870x519 window restored. Live smoke passed600 changing mob updates with1/1 texture allocations. Monolithic4096-wide addressing pair also pixel-identical. No separate live FPS measurement this iteration; owner may move or close the client.
