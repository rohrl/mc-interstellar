# Current handoff — 2026-09-22

## Active task override

**Later same-day experiment:** current working branch is `codex/material-coverage-experiment`; packaging remains pushed at24958d8 on `codex/demo-visual-refinement`. See [docs/material-coverage.md](docs/material-coverage.md) before continuing. Native fluid/translucent/nonliving/block-entity coverage works and24 GPU composition cases plus26240 optical checks pass, but the material renderer is not accepted: live FPS dropped. Selective/full compositor pairs are byte-identical; selective helps wall but not heavy down. Active .04/.08 angular-step experiment retains .02 near-critical rays. Do not describe step4 or the post-step4 algorithm review as complete. A new grazing offset/case is edited but not runtime verified yet.

Latest client: `orbit-material-runtime.log`, exec8366, F9 streamed natural-world wall camera; setup/fixture helper34859 may still be running. Fresh capture/current .04 optical checks in progress. `[` cycles .02/.04/.08, Shift+`[` compares; Z toggles selective/full composition, Shift+Z compares. All new changes build except the final small grazing-test edit has not had a separate build yet. New docs contain exact logs/timings/limits. Cold material shader linking takes~100s: wait for `Loaded1399 advancements` after `uniform named FastFetch`, then allow a second before clicking the backup notice. Clicking while loading does not enter. Native F2 diagnoses notices.

Current branch **codex/demo-visual-refinement**, based on79aa1d0. Owner requests completion through step4 WITHOUT reducing current FPS, then a fresh code/algorithm performance review with small barely noticeable approximations allowed for worthwhile gains. Step5 needs later discussion. Weather/general teleport remain deferred. This task is NOT complete when packaging finishes.

Packaging checkpoint: [docs/demo-packaging.md](docs/demo-packaging.md);53 tests, build/package and runtime source/return/visual checks passed. Renderer shaders unchanged so far. Archive under build/distributions, `gradlew packageDemo`; [quickstart](docs/demo-quickstart.md). Current client log demo-packaging-final-runtime.log, exec47293, Player493, F10 off, returned outside demo. Owner position `(45.103214895634814,303.49506601944705,-9.682636277880926)`, yaw56.08086/pitch-.29997176, creative. Preserve this at final return; old benchmark pose below is for testing only. Custom dimension triggers vanilla experimental notice: built-in Create Backup and Load used on both launches (window-relative270,305).

Next implement step4 coverage/secondary-image improvements, measure against the same natural-world wall/down scene and quality settings, then review algorithms. Do not use the simpler demo scene to claim FPS parity. EntityMesh currently skips non-living/block entities and translucent/special layers; WorldMesh skips fluids/translucent blocks. New flags must preserve cloud5/6 and signed entity1/2 semantics. Do not merge old WIP branches wholesale. Previous performance evidence/workflow below remains applicable except stale live-state/paused-packaging statements.

Repo C:\work\code\minecraft\interstellar\interstellar; branch codex/world-mesh-reference; origin https://github.com/rohrl/mc-interstellar.git. JDK C:\Portable\jdks\temurin-21.0.12.1; Minecraft1.21.1/Fabric. Read AGENTS.md. Normal edits/branches/commits/pushes/runtime controls authorized. No force push or subagents. Preserve worlds/.idea/secrets/owner edits. Owner handles movement/flicker feedback; use fixed-pose numerical pairs and occasional milestone screenshots. Keep commentary concise/frequent and batch predictable controls.

## Outcome and scope

Current performance pass finishes under the owner's diminishing-returns condition. Final live1440p medians ~53FPS wall/~34FPS terrain-heavy down. Down frame p50/p95/p99=29.796/31.358/33.704ms: representative30FPS target reached, occasional slow frames remain. Do not claim an absolute30FPS floor, universal FPS, or60FPS. Last isolated improvement is2.3–2.4%; larger gains likely need a geometry/traversal redesign. No lowering of AA/resolution/coverage/optical tolerance to meet these figures.

Weather assessment complete/deferred: docs/precipitation-assessment.md. Capturing weather geometry is small; correct transparent composition needs new design and measurement. No measured weather FPS claim. Feature brainstorm complete: docs/relativity-feature-ideas.md, favour clocks/opt-in devices, orbit probes and light-echo instruments. These are proposals, not approved implementation. Packaging stays paused/separate. Actual-player-body and horizon-crossing requirements remain on the longer roadmap.

## Current renderer and evidence

Sharp2xAA at half linear scale:1280x720 logical for2560x1440 output. Both diagonal rays remain. Two shorter GPU draws write RGBA32F samples, then average into the original8-bit target before bounded cubic reconstruction; extra28.125MiB at1440p. Fixed streamed dimensions and per-triangle row reuse reduce address work. No new row-specific programs/controls retained.

Native terrain/moving meshes, exact compact nodes, unchanged default-setting specialization, adaptive paths with nominal1mm local sagitta tolerance and0.02rad RK4 cap,16-block spatial maximum, conservative learned per-ray empty boxes initially1024. Local curvature estimate is not a rigorous global error bound. Alternate settings fall back to existing programs. Split sample target guards maximum texture width and framebuffer completeness.

Accepted checkpoints:82630f1 compact nodes;84fbe31 default specialization;7792557 spatial cap16;8f44879 split AA;17e8005 fixed texture layouts. Latest commit additionally consolidates row reuse in terrain_shared.glsl. Evidence: docs/compact-nodes.md, docs/live-default-specialization.md, docs/long-chords.md, docs/split-aa.md, docs/fixed-layout.md, docs/triangle-row.md.

Matched frozen GPU p95 gains: compact~12%wall/20%down, default specialization~18%/16%, cap16~21%both, split AA~34%/29%, fixed layouts~9%/11%, row reuse~1%/2%. Scenes differ between checkpoints; do not multiply these into a purported matched overall benchmark. Split/fixed/row pairs are pixel-identical. Cap16 has separately recorded tiny numerical pixel changes, not identical output.

performance-final-build.log:53 tests pass. Row experiment's matching diagnostic:26240 sampled checks over340 distinct directions/settings/layout repeats, zero mismatch/inconclusive/unresolved. Exact wall/down image pairs pass. Consolidated final client has build/runtime/timing/native-screenshot checks; the26240 run was in the separate row experiment using the same address expression. Do not claim actual Diagnostic=0 executable directly produced diagnostic classifications. Native screenshot run/screenshots/2026-09-20_01.04.51.png inspected. Live600updates retain original triangle/node allocations1/1. A new experimental guard initially omitted F10; fixed before acceptance by checking actual mesh.streamed(). No new remaining correctness defect identified in reviewed scope.

## Live state

One client: performance-final-runtime.log, unified exec session82674; F10 active. Player(16.5,302,-45.5),yaw.281/pitch.91 restored. Client854x480, outer870x519 at(77,113), restored after exact1440p checks. SourceN65,r_s8.125, anchor(14,300,14),COM~(16.01,302.04,15.99). Do not restore oldN64. No manual block/time/weather changes. Source inspection still required after a fresh launch.

Final live GPU p50/p95/p99:wall17.934/18.573/18.886ms,down28.636/29.280/29.559ms. Framewall18.770/20.639/22.180ms,down29.796/31.358/33.704ms.6092222terrain triangles,120warmup/300samples, includes optical/fold/reconstruction passes. RTX5070Ti/driver616.92. Live world/time evolves; paired frozen runs establish speedups.

## Efficient runtime workflow

Gradle/GUI need escalation; owner authorized. Set JAVA_HOME above, run gradlew.bat build redirected to log. Launch with -I run/stable-init.gradle runClient. Never two clients on one save. Ignored run/restart-client.ps1 -Log LOG closes the identified client normally, waits for actual exit with stopping errors, audits remaining processes, then launches. A timeout must not lead to another launch.

Ignored helpers:
- run/setup-live.ps1 -Log LOG: fresh join, inspect source, F10. Wait for Streaming terrain ready before timing.
- run/setup-comparison.ps1 -Log LOG: fresh join, inspect, F9 snapshot, heldShift+M streamed capture. Wait ready, then Space enables lensing (capture turns it off).
- run/control-short.ps1 -HoldKey VK -HoldMillis120 or -Keys '+y'. Supplies actual scan codes for Escape/arrows; old VK-only events silently failed. Focus titlebar with -X430 -Y15. Use actual spaces in commands.
- run/measure-pass.ps1 -Log LOG: B frozen; -Key123 liveF12; -ClearPrevious clears an old completed result before starting. Gates on completion,120warmup/300samples. No builds during sampling. Timeout means inspect state, not restart.
- run/size-minecraft.ps1 sets exact2560x1440 client; -Restore uses run/window-size-backup.json. F11 was unreliable; verify logged1280x720 atscale.5.
- run/send-safe-command.ps1 -Command '/interstellar inspect 14 300 14' preserves clipboard. Fixed-pose tests use '/tp @s 16.5 302 -45.5 0.281 35.91'; restore pitch.91. No position/chunk shift or teleport-feature acceptance implied.

F9 arrows rotate5degrees; same-renderer comparisons record actual preview angles, vanilla pairs require matching player camera. C optical fixture: first enable lensing if testing the specialized layout variant; verify logged program. B frozen/F12live timing. Space lensing; M monolithic/Shift+Mstreamed; A AA; G adaptive; T bounds; R addressing; I empty cache/Shift+I reach; F compact; D defaults; V cap4/16; X split/serialAA; Y fixed layout including row reuse. Shift+F/D/V/X/Y compare compiled references. S general/native; Shift+S compare. P vanilla-unbent; Shift+P full-resolution4ray quality. Z row-only controls exist on experiment branch only. Escape closes F9; F10 toggles live and clearing it causes recapture (~35–40s).

java tools/CompareAppearance.java PAIR_DIR gives metrics. Hash named reference.png/candidate.png for exact equality. NativeF2 screenshots work when desktop CopyFromScreen returns white; do not repeat broken desktop captures. Logs/captures/saves/helpers are ignored.

## Architecture/limits and preserved branches

F10 keeps per-chunk BVHs; section/light/chunk events queue bounded5ms capture slices and publish individual replacements. Actors/clouds rebuild per frame with reusable uploads. Source refresh retains terrain and automatically pauses/resumes. Camera guard256, render distance<=16,7M terrain cap. Streamed4095-wide RGBA32F arenas,16384triangle rows/4096node rows;4top rows reserved. Compact nodes preserve float32 bounds/escape with finite bit-packed headers and overflow descriptors. Original-node reference mirror adds268MB (~1.5GiB total streamed arenas). Missing ARB_shader_bit_encoding falls back without compact-world allocation (reviewed/built, not tested on another GPU).

Fluids, special/transparent blocks, special entity layers, non-living/block entities and returning actual-player-body images remain incomplete. Native clouds use nearest-layer depth semantics; not valid for overlapping weather stacks. Keep SR/GR scales separate. No terrain destruction/heavy server-tick solver; do not revive unbent background overlays.

Preserved/pushed: original AA WIP codex/terrain-antialiasing8ad46eb; paused packaging codex/demo-packaging-wip2fe8674. Rejected experiments:terrain-sah ba0bd73 (~4xCPU cost for small gain),empty-step220b883 (slower than original),final-hit-shading9967e4b (2.5%slower),facing-hints7f9fad2 (no gain). codex/triangle-row-experiment09f93cf preserves isolated comparisons; only its small address expression was retained on main development branch. Do not merge whole experimental branches.
