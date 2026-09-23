# Handoff — cloud intersections rejected, 2026-09-23

## Scope and current checkout

Repo C:\work\code\minecraft\interstellar\interstellar, branch codex/cloud-quad-results. Follow AGENTS.md/token-efficient workflow. Normal implementation, branches, commits and pushes authorized; no force-push, subagents, unrelated resets or world edits. Current branch changes documentation/evidence only: production source is exactly accepted e324e84 (codex/separate-moving-trees). Full cloud experiment9815c42 is pushed separately on codex/cloud-quad-intersections; do not merge it into normal rendering by accident.

AA WIP8ad46eb remains on codex/terrain-antialiasing; quad A/B49738ba, accepted quad21d33ed and packaging WIP2fe8674 remain preserved. Step5 needs later discussion; weather, teleport support and automated movement/flicker testing remain deferred.

## Outcome / evidence

Read docs/cloud-quads.md for this iteration and docs/performance-profile-2026-09-23.md for the revised ranking. Both one-plane cloud-face tests and specialized precomputed axis-aligned planes were effectively tied on heavy-view GPU time; neither adopted. Specialized down baseline24.426/24.437ms vs24.434/24.475ms; wall14.603/14.537 vs14.503/14.525ms. No useful gain. Cloud entries fell60% but moving node visits only0.8%; counters include cheap rejects. Do not revive it just because primitive counts are lower.

Experimental build62 tests passes. Both selected GPU layouts pass52,480 optical comparisons,84 material and192 analytic cloud checks. First wall/down pairs are byte-identical PNGs; specialized wall/demo also identical. Specialized down MAE0.00000018 with0.0002% pixels >8/255; do not round this into an exact-match claim. Contact sheet inspected. Tests exposed a background shared-edge rounding miss in the new fixture; moving the unrelated background diagonal retained exact cloud edge probes and tight tolerance. General shared-edge rounding remains a separate limitation. Existing exhausted sample1,x509/y554 bottom-origin repeats in baseline/candidate probe/full counters; production reproduction remains open. No optical limits changed.

Accepted e324e84 forest remains: independent actor/cloud roots in one moving loop, shared conservative empty cache, skip consumed cloud forest. It previously measured4% less heavy GPU time,3.46% less wall time; live~39FPS was a scene-specific observation with different mob population, not a new result here. Details docs/moving-trees.md. Coplanar actor ordering, special additive/glint/text/particles, boat water masks and returning player-body coverage remain limits.

## Next bounded proposal

Conservative tree-node compression/storage, retaining full geometry and intersections. Node visits remain frequent; hardware bandwidth/register/occupancy bottlenecks are not proven. Outward rounding and traversal safety need targeted tests, then same-scene repeated timings/images against e324e84. Keep or reject by measured results. Actor hierarchy is demoted after large reduction in actor leaf entries; optical tables remain a larger low-confidence option. No new optimization started.

## Runtime / resume

Restored production clean build + packageDemo passes59 tests. Normal profiling-disabled launch cloud-default-runtime.log verified N64/F10 readiness, no experimental cloud/counter shaders, no CPU profiling or renderer errors. Client is paused with F10 ready at854x480. Both input helpers completed; none remains running.

Original state for THIS iteration is run/cloud-return-state.txt: dimension interstellar:demo; player40.43597468465466/65/-3.0021334600889626; yaw60.452244,pitch-9.900009; creative flying0. Exact state restored and queried after the final launch. Older split-return-state.txt/profile-return-state.txt are obsolete. Restore final rotation after all preview/window interactions, then query it. Source N64 at inspect0/80/0. Blocks/time/weather/demo return record untouched.

JDK C:\Portable\jdks\temurin-21.0.12.1. Launch Interstellar.cmd uses current checkout. Close identified client normally and wait before restarting; never two clients on the save. Ignored run/cloud-* scripts contain this experiment's log-gated controls; no new tests should rely blindly on old PIDs. Cold shader compilation can take minutes. Focus title bar before experimental-world confirmation. Creative flight double-taps must be in one native-input process. No live timing necessary for rejected variants. Never pipe Java comparisons to Select-Object -First: redirect to log and read the completed metrics.json, whose extra precision matters. Compact evidence in docs/profiles/2026-09-23-cloud-quads; large images stay ignored.
