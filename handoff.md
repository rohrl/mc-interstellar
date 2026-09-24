# Handoff — world features integrated, 2026-09-24

## Checkout and authorization

Repo C:\work\code\minecraft\interstellar\interstellar; branch codex/world-feature-coverage,
based on ecf1b09. Requested feature batch implemented and verified; normal branches,
commits/pushes and autonomous testing are authorized. No subagents. Preserve older AA and
performance branches. User also asked to disable narrator: run/options.txt narrator:0.

## Result

Native emissive/glint, entity blob shadows, ordinary sign/name text, selection and mining
layers now join F10. Glowing-status outlines use a separate glowing-model tree and curved
mask, including team colour and through-wall visibility. Actual camera-body skin/model is
eligible only after a ray bends around the source; its returning image can be a thin arc.
There is no delayed pose history. Local native rain/snow is a small unbent foreground
approximation. Body/weather session toggles: /interstellar-visuals body|weather true|false.
The distinct client root is intentional: a client /interstellar root shadows server commands.

Tridents use existing bounded server stepping while loyalty runs once. Free bobbers get a
local kick and swept horizon capture, retaining native fishing/reeling. Fishing/leash
geometry bends with capped, endpoint-pinned sag, no rope tension/collision solver. Client
visual strength receives a server snapshot on join/change rather than shared server globals.
See docs/world-features.md, docs/minecraft-coverage.md and D075 for details/current limits.

## Checks and cost

Build/package,77 tests pass. Final features-release-runtime.log:52,480 optical comparisons
and156 material cases, zero mismatches/unresolved/failures. No renderer exception. Native
unused-sampler/optimized-out-uniform warnings remain. Visual checks cover glint on/off,
shadows, text, mining, rain/snow, coloured through-wall outlines, invisible glowing entity
and actual-body on/off. Physics checks cover trident deflection/loyalty, fishing/reel-in and
native leash attachment. No broad movement/flicker test (owner handles it).

1440p arrow-course medians: body-on GPU/frame21.001/21.601ms (~46FPS), body-off18.734/19.338ms
(~52FPS). Glowing adds~3.7ms with a glowing fixture; rain's0.057ms frame delta is within noise.
These short scene samples are not a new worst-case FPS guarantee. A body-only empty-cache
trial was pixel-identical but did not improve performance and was removed. Detailed rows
and raw summaries are in docs/world-features.md and docs/profiles/2026-09-24-world-features.txt.

## Runtime and exact restoration

Final client PID4772, exec session11649, log features-release-runtime.log; query before
assuming it still exists. Left paused, F10 off, normal ticking, body/weather defaults on,
gravity enabled, narrator off. Window restored854x480 (outer870x519 at845/449).

Fresh run/features-return-state.txt and features-restored-state.txt match all six recorded
fields exactly: dimension interstellar:arrows; position -8.284012400041416 /
100.11934391327323 /-25.681458146811625; yaw-0.7497861,pitch25.55015; creative/flying1;
selected slot1; original nine hotbar stacks (pig egg,bow,arrow,yellow/lime/light-blue/purple
concrete,magma,end rod), offhand empty. Brief survival mining verification caused damage;
healed afterwards. Initial health was not separately recorded. No inventory replacement
remains. Preserve the existing demo return record; do not demo leave to restore this pose.

Temporary tagged mobs, knot, four individual fixture blocks, aqua test team and18-cell
snow biome patch were removed/restored. Source64 and owner builds were untouched. Clear
weather restored. The owner gameplay source remains at15/91/-14 in the separate exhibit.

## Next and efficient workflow

Resume targeted GPU measurement, table-assisted optics, then moving-tree reuse/refit.
Camera-body tree visits are a measured new cost worth isolating; a dedicated returning-only
body tree is a candidate, not an implemented/accepted speedup. General teleports, terrain
crossing/destruction, emission history and observer-speed work remain deferred.

JDK C:\Portable\jdks\temurin-21.0.12.1. Launch Interstellar.cmd uses this checkout. Close the
identified client normally before relaunch. Startup shader compilation can take~150seconds;
do useful independent work and report progress. Gate on Loaded1399 advancements, wait3s,
held click relative595/305, then joined-the-game and another3s before commands. F9/F8 STOP
F10, so do not assume live mode survives closing them. Held Escape600ms is reliable.
Use log readiness and fixed-pose numerical pairs; reserve screenshots for major visual checks.
run/feature-benchmark.ps1 gates on completion; helpers/logs/images stay ignored. Capture fresh
user/window state next turn because the owner plays between sessions.

Preserve AA WIP8ad46eb on codex/terrain-antialiasing and rejected performance experiments.
Accepted separate roots e324e84 remains; original experiment1–4 outcomes are unchanged.
