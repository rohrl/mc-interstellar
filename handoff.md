# Handoff — moving-tree optimization accepted, 2026-09-23

## Scope and authorization

Repo C:\work\code\minecraft\interstellar\interstellar, branch codex/separate-moving-trees. Follow AGENTS.md and token-efficient workflow. Owner authorized implementing the first profiling proposal, normal edits/tests, branches, commits and pushes; no force push, subagents, unrelated resets or world edits. Accepted result and complete evidence: docs/moving-trees.md. Profiling/ranking: docs/performance-profile-2026-09-23.md.

AA WIP8ad46eb remains on codex/terrain-antialiasing; quad A/B49738ba on codex/quad-vertices-experiment; prior accepted quad21d33ed and packaging WIP2fe8674 remain preserved. No WIP restored. Deeper relativity/step5 needs later discussion; weather, teleport support and automated movement/flicker testing remain deferred.

## Accepted implementation

MovingMeshTrees partitions exact actor/cloud triangles, builds independent roots in the existing buffers and rebases cloud escape links. The winning shader traverses both roots inside the original moving-tree loop, sharing its conservative empty-region cache; after the native cloud layer is consumed, the remaining cloud forest is skipped. No optical/AA/material/update-frequency change, no extra samplers or second steady-state representation. F9 W selects combined/separate trees, Shift+W saves the same-scene pair. Developer counters follow the selected layout.

Two alternatives were rejected: three independently cached searches (down ~2% slower, wall ~5% slower), then no cloud cache (down ~0.7% slower, wall ~7%). Patches and evidence are in docs/profiles/2026-09-23-moving-trees. Do not revive them solely because their node counts are lower.

Accepted controlled frozen GPU medians at1440p output/1280x720 logical/2xAA/render-distance12: down baseline25.525/25.537ms vs24.493/24.528ms, 4.00% less GPU time (~4.16% throughput); wall15.105/15.193 vs14.583/14.667ms, 3.46% less time. Both tails improve. Final capture61 entities+2 block entities,10,260 moving triangles,6,260,834 terrain triangles. Earlier profiling had~96 entities; do not compare across captures as causal evidence. Moving-node visits153.44M→64.10M; cloud entries70.05M→65.94M; actor entries8.94M→.058M. Counts include pre-intersection rejects.

Live candidate heavy view:24.805/24.551ms GPU,25.763/25.531ms frame (~38.8–39.2FPS), actors updating. This is not a paired live-baseline gain. CPU medians tree3.47ms/upload.17ms/actor capture.89ms/cloud capture.19ms at this population; CPU/GPU overlap. No hardware register/bandwidth diagnosis proven.

## Verification and remaining correctness limits

59 JUnit tests/build pass. Accepted runtime fixture52,480 optical comparisons and84 material checks pass (zero mismatches/inconclusive/unresolved). Material fixture now tests static/all-moving/mixed storage, empty actor/cloud trees and cloud/transparency ordering. Wall/down image pairs match exactly. Restored-demo pair RGB MAE .00001755; .0125% of pixels change >8/255, localized on overlapping horse-face surfaces. Crop inspected; consistent with the existing coplanar draw-order limitation. Do not claim universal pixel equality or that coincident actor surfaces are fixed. All vertices/materials remain intact.

The previously observed instrumented exhausted subpixel persists in baseline and candidate: sample1,x509,y554 bottom-origin, probe/full. Optical limits unchanged. Reproduce separately in production before calling it a new regression. Native draw-order ties, special additive/glint/text/particles, boat water masks and first-person returning-body coverage remain limits.

## Next bounded proposal

Native cloud shared-quad/rectangular intersection work is next; retain exact native faces, cutouts, colours and nearest-cloud semantics. Actor-leaf work now contributes far fewer entries in this view, so rerank tighter actor bounds before implementing them. No next optimization started. Use paired fixed scenes and occasional crops; do not time counter readbacks or builds. Avoid Java output piped to Select-Object -First (can terminate comparison before artifacts are written); redirect to a log, then read its summary.

## Runtime / user state

Normal profiling-disabled launch split-default-runtime.log verified N64/F10, no counter shaders or CPU logging, no errors; client paused with F10 ready at854x480. Current restored pose (different from older profile-return-state.txt): dimension interstellar:demo, player44.2374620427497/65/-7.074375242855579, yaw81.7522,pitch-31.349997, flying0. Saved in run/split-return-state.txt. Never restore the obsolete older pose blindly. Blocks/time/weather/demo return record untouched. No input helper should remain running; identify current client rather than trusting old PIDs/session IDs.

JDK C:\Portable\jdks\temurin-21.0.12.1. Launch Interstellar.cmd uses current checkout. Normal close/wait before relaunch; never two clients against one save. Ignored helpers run/split-* and run/restart-client.ps1; profile helpers use opt-in profiling. Large paired images in run/interstellar-captures; compact images/metrics/timings committed under docs/profiles/2026-09-23-moving-trees. Cold shader compilation may take minutes. Focus title bar before clicking experimental-world confirmation. Creative-flight double taps must use direct native events in one process: separate helper invocations are too slow.
