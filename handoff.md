# Current handoff — 2026-09-20

Repo C:\work\code\minecraft\interstellar\interstellar; branch codex/world-mesh-reference; origin https://github.com/rohrl/mc-interstellar.git. JDK C:\Portable\jdks\temurin-21.0.12.1, Minecraft1.21.1/Fabric. Read AGENTS.md; normal edits/branches/commits/pushes/runtime controls authorized. No force push or subagents. Preserve worlds/.idea/secrets/owner edits. Keep commentary concise and frequent; use log gates and numerical image comparisons, occasional milestone images. Owner handles movement/flicker acceptance; no automated motion tests.

## Active goal and next work

Aim toward60+FPS at1440p with **at least30FPS**, without noticeable quality loss or correctness compromises. Owner rejected stopping at20FPS. Goal remains active: live wall now~35FPS, terrain-heavy downward~23FPS. Next test exact early back-face rejection for suitable terrain triangles, preserving all existing face visibility, optics and quality. Do not mark complete at current downward FPS.

Precipitation assessed/deferred: geometry small, transparent ray-ordered composition needs new design and measurement; docs/precipitation-assessment.md. Feature brainstorm completed in docs/relativity-feature-ideas.md: clocks/opt-in devices, orbit probes, light-echo instruments first; existing actual-player-body and horizon-crossing requirements remain. These docs do not mean features are implemented or scope newly approved.

## Latest accepted renderer

Sharp2xAA at half linear render scale (1280x720 internal for2560x1440 output). Native meshes, exact compact nodes, default-setting specialization, adaptive paths with nominal1mm local sagitta tolerance,0.02radian RK4 angular cap, **16-block spatial cap**, fast bounds/addressing and conservative per-ray empty boxes initialized at1024. All scene geometry/animation retained. Local curvature estimate is not a rigorous global error bound. Actual default shader specializes unchanged settings; alternate toggles fall back to dynamic shaders.

Accepted/pushed predecessors:82630f1 compact nodes,84fbe31 normal-setting specialization. Current16-block cap acceptance commit/push in progress. long-chords-final-build.log passes53 tests. At16, compact diagnostic variant passes26240 sampled comparisons over340 distinct directions (layouts/settings repeated), zero mismatch/inconclusive/unresolved. Actual specialized program has Diagnostic=0, so C uses a diagnostic variant; do not claim those classifications directly test the specialized executable. Actual-program image pairs cover wall/down/away and show only0.0023%/0.0031%/0.0002% of pixels differ by>8 levels versus4-block reference; downward inspected. No confirmed new correctness bug in reviewed/tested scope.

Matched frozen1440p GPU p95 cap4→16:wall38.849→30.592ms, down58.982→46.591ms (~21% both). Actual live cap16 frame p50/p95:wall28.594/30.147ms (~35FPS), down43.998/45.398ms (~23FPS). Live scenes evolve; only frozen paired runs establish gains. Cap32 passed sampled checks but saved only2% beyond16, so removed from retained V control. Detailed evidence: docs/long-chords.md, docs/live-default-specialization.md, docs/compact-nodes.md; older results docs/aa-performance.md.

## Runtime state

One client: long-chords-runtime.log, exec session69282; live F10 active, sourceN65, player(16.5,302,-45.5), yaw.281/pitch.91, small870x519 outer /854x480 client window restored. Earlier down pose set by test command then restored; no manual block/time/weather changes or teleport-support claim. Current running build still has experimental V cycle4/8/16/32; built retained source narrows it to4/16, otherwise same renderer. Check process/log before inputs; never launch a second client on the same save. Normal close and wait actual exit before relaunch. F10 recaptures when turned off/on (~35–40s).

Launch: set JAVA_HOME above, .\gradlew.bat -I run/stable-init.gradle runClient redirected to a local log. Gradle/GUI tools need escalation; owner already authorized. Gate on joined-the-game. Initial source inspection still required after launch: /interstellar inspect 14 300 14. Source anchor(14,300,14),N65,r_s8.125, COM~(16.01,302.04,15.99); **do not restore oldN64**.

## Controls and efficient checks

Local ignored run/setup-comparison.ps1 -Log LOG handles a fresh client's join, existing source inspection, F9 snapshot and Shift+M streamed capture request. Wait for Streaming terrain ready, then Space restores lensing (backend capture sets it off). Gate on Terrain snapshot/World mesh ready/Streaming terrain ready, never double-request capture. Use native held Shift+M; SendKeys once arrived as plain M.

run/control-short.ps1 forwards the external helper and now supplies actual scan codes for Escape/arrows. Old VK-only events silently failed for those keys. Hold120ms; letter/function keys forward normally. Arrows rotate frozen F9 in5-degree steps. Same-renderer pairs now allow F9 rotation and record its real angles; vanilla P still needs the player camera orientation. Benchmarks log yaw/pitch. Do not label a run downward unless angles/state verify it. Native F2 is reliable when desktop CopyFromScreen produces white.

run/size-minecraft.ps1 sets exact2560x1440 client area; -Restore uses run/window-size-backup.json (backup persists, verify before future reuse). F11 proved unreliable; use logged dimensions. run/measure-pass.ps1 -Log LOG runs B and gates on completion; -ClearPrevious clears an old result; -Key123 uses F12 live. No concurrent builds during sampled timings. Timeout means inspect current state, not restart.

F9 C optical fixture; B frozen/F12 live benchmark120 warmup+300 samples. Space lensing; M monolithic/Shift+M streamed; A AA; G adaptive; T bounds; R addressing; I empty cache/Shift+I reach16/1024; F compact/original nodes; D default-setting specialization; V cap4/16. Shift+F/D/V compare each selected variant against its compiled reference. S general/native, Shift+S comparison. P vanilla-unbent, Shift+P four-ray/full-resolution quality; other older comparison modifiers documented in docs/aa-performance.md. Esc closes F9; F10 live. Safe commands via run/send-safe-command.ps1 preserve clipboard.

Comparator: java tools/CompareAppearance.java PAIR_DIR, or REF.png CAND.png OUT_DIR. Read metrics.json for full precision; hash named reference/candidate PNGs for exact equality. Logs/captures/saves/helpers ignored. Avoid repeated full doc/log reads or screenshots.

## Architecture and limits

F10 retains per-chunk BVHs; section/light/chunk events queue bounded5ms slices and publish replacements individually. Actors/clouds rebuild per frame with reusable upload buffers. Source refresh retains terrain, automatically pauses/resumes. Camera guard256; render distance≤16;7M terrain-triangle cap. Streamed4095-wide RGBA32F arenas,16384 triangle rows/4096 node rows; first4 node rows for scene tree. Compact nodes use exact two-texel records, normal finite header bits and row-tail overflow records; virtual1365nodes/row unchanged. Original node mirror adds268MB (roughly1.5GiB total streamed arenas) for comparisons; duplicate uploads retained. Missing ARB_shader_bit_encoding falls back without compact world allocation (reviewed/built, not tested on second GPU).

Fluids, transparent/special blocks, special entity layers, non-living/block entities and returning actual-player-body images remain incomplete. Native clouds use nearest-layer depth semantics; do not reuse that for weather stacks. Preserve separate SR/GR scales, no terrain destruction or heavy server-tick physics. Do not revive ordinary-camera background overlays.

## Preserved branches

Original AA WIP codex/terrain-antialiasing8ad46eb stays separate; do not cherry-pick obsolete renderer. Packaging paused/unverified on codex/demo-packaging-wip2fe8674. Rejected/pushed experiments:codex/terrain-sah-experiment ba0bd73 (small GPU gain,~4x CPU builds);codex/empty-step-experiment220b883 (slower than compiled original);codex/final-hit-shading-experiment9967e4b (2.5% slower). Evidence in docs/experiments and docs/empty-spans.md. Do not merge them.
