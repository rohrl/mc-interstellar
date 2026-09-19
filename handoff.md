# Current handoff — 2026-09-19

Repo: C:\work\code\minecraft\interstellar\interstellar. Branch codex/world-mesh-reference; origin https://github.com/rohrl/mc-interstellar.git. JDK C:\Portable\jdks\temurin-21.0.12.1; Fabric/Minecraft1.21.1. Read AGENTS.md. Normal edits, commits/pushes and autonomous runtime inputs authorized. No force push or subagents. Preserve worlds/.idea/secrets and owner edits.

## Priority and preserved work

Owner loves current appearance; optimize FPS with minimal fidelity loss. Packaging paused on codex/demo-packaging-wip at2fe8674 (pushed, unverified, never built/launched). Original AA WIP8ad46eb remains on codex/terrain-antialiasing; do not cherry-pick its obsolete renderer. Current2xAA is already adapted. Owner rejected soft EDGE blur; retain sharp default. Rain/snow, teleport support and star differences deferred; owner handles movement/flicker acceptance. Never revive rejected ordinary-camera background overlays.

## Current verified optimization

Ray-local empty-region reuse learns a box from rejected-subtree separating planes during normal traversal, publishes only after no triangle leaf was entered, and skips later segments strictly inside. Separate terrain/moving caches reset every AA ray. No geometry/optical/quality changes. Initial extra occupancy-query prototype was slower and discarded. Retained shader is the version after20:39 reload in cells-runtime.log; regions-build.log has51 passing tests.11520 CPU/GPU comparisons pass (180 distinct directions across modes), zero mismatches/inconclusive/unresolved. Small/1440p wall pairs and downward terrain pair are pixel-identical; images inspected.

Matched frozen2xAA/all earlier optimizations:1440p wall GPU p95=161.340→77.698ms (51.8%); small downward48.696→31.966ms (34.4%).1440p cached median frame83.334ms (~12FPS), still below target.94/95 mobs; no cross-session speedup claims. These compare uniform toggles within the revised shader, not separately compiled releases. Exact evidence/earlier addressing/bounds/AA results: docs/aa-performance.md. No block/time/weather/config edits. F9 I toggles cache, Alt+Shift+P compares; shader/metadata flag is named EmptyCells/emptyCells.
Next: profile larger ray/BVH traversal costs. Learned empty boxes now skip many repeated tree walks; occupied regions still need normal traversal. Preserve same-scene baselines and independent numerical checks; don't trade coverage/AA for a claimed same-quality gain. Remaining materials/features: fluids, transparent/special block models, non-living/block entities, shadows/eye glow/glint/translucent entity layers. Packaging/deeper relativity/player-body remain later.

## Runtime architecture

F10 retains chunk BVHs in GPU row arenas; native section/light and chunk events queue bounded5ms capture slices. Publish individual replacements; retain overlapping chunks on movement. Actors/clouds use a separate per-frame BVH and reusable textures/staging buffers. Both trees share curved nearest-hit/cloud ordering. Source refresh retains terrain and automatically pauses/resumes; initial source inspection still manual. Camera guard256 plus exterior/source-availability guards. F10 off frees the cache, so reactivation recaptures (~35–40s).

GPU arenas:4095-wide RGBA32F,16384 triangle rows/4096 node rows (~1280MiB), first4 node rows reserved for top-level index. RowArena coalesces frees, no defragmentation fallback.7M terrain triangle cap; render distance above16 refused. Camera window=render distance+1 chunks each side, full height, loaded chunks only. Monolithic/moving textures4096 wide. Details: docs/streaming-terrain.md, live-native-mesh.md, viewing-range.md. Independent fixture scope: docs/mesh-ray-validation.md; boxes/sampled hit cells, not arbitrary-material certification.

## Scene and efficient checks

Owner source anchor(14,300,14), N65, r_s8.125, COM approximately(16.01,302.04,15.99). **Do not restore old N64.** Test player pose(16.5,302,-45.5), yaw.281/pitch.91. Owner may move/close client; check process/log before inputs. Never launch two clients against the save.

Launch with JAVA_HOME above: .\gradlew.bat -I run/stable-init.gradle runClient, redirect logs. Gate on joined-the-game before setup commands (shader startup may take longer than a fixed wait). Fresh player may need spectator→test teleport→creative for flight. Setup teleport is not v1 teleport acceptance. GUI/Gradle need escalation.

F9 pauses; wait snapshot, then Shift+M streamed or M monolithic. Switching backend recaptures while paused and sets lensing OFF: Space restores it. Gate on snapshot/mesh ready logs, never press M twice during capture. F3+T shader reload needs normal play; wait for the new snapshot to finish before M (compilation can delay it). F9 beyond128 automatically requires streamed mesh. C independent fixture; B benchmark (120 warmup/300 samples); F12 live benchmark. AA A; adaptive G; bounds T; addressing R. P vanilla/unbent; Shift+P4-ray quality; Ctrl+P original/adaptive; Ctrl+Shift+P bounds; Alt+P addressing; Alt+Shift+P empty regions. E entities/N clouds/J fine/Q scale. Comparisons require F9 pose to match actual player camera; metadata records settings. F11 fullscreen; restore small window after tests.

Helpers: C:\Users\karol\Documents\Codex\2026-09-13\i-want-to-create-a-minecraft\work\control-minecraft.ps1 and capture-minecraft.ps1; held native keys120ms. run/send-safe-command.ps1 preserves clipboard. Comparator: java tools/CompareAppearance.java PAIR_DIRECTORY; three-path mode for arbitrary images. Logs/captures/saves ignored. Prefer numeric pairs and bounded logs; one milestone image, no repeated screenshot polling. Historical results are in feature docs/progress/decision log, not duplicated here.

Last client state: F10 active, small870x519 window. Live cache/actor/streaming smoke passed;600 updates retained1/1 texture allocations. Camera moved during the final F12 sample (r/r_s1.79676,170 queued chunks), so live timings are observational only; owner may be exploring. Do not reset their camera without a new test need. Current runtime log cells-runtime.log; revised shader after20:39 reload. Startup authentication timeout was nonblocking; no renderer failure observed.
