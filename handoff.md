# Handoff — Continuous wormhole exterior, 2026-09-29

## Current state

Branch codex/rtx-wormhole-demo. See Git for current commit. Branches/pushes and
runtime input authorized; no subagents. Follow AGENTS.md and token-efficient
workflow. Preserve original worlds, .idea and AA WIP8ad46eb. No active goal.

Owner reported nearby open mouths splitting the world and a long 97% wait.
Implemented two localized optical regions in one shared native scene, removing
all midpoint-plane geometry filtering. Both entrances can appear in one view.
Ellis spatial rays near each throat blend smoothly into straight exterior rays;
this is an engineered metric, not an exact global isolated Ellis solution.
Four throat passages per ray bound repeated views; no recursive full-frame draws.
Fog accumulates the native metric along chords, continuous at the region boundary.

Opening completion/progress now uses fixed destination geometry, prioritized ahead
of ordinary moving-camera capture. Chunk/light and presented-frame travel barriers
remain. CPU exact Ellis reference and OpenGL-only build remain available.

See docs/science.md, docs/settings-and-wormhole-seed.md, decision D094 and
**docs/profiles/2026-09-29-continuous-wormholes/README.md** for assumptions, evidence,
measured timings and limits. Previous opening-animation work is recorded in D093
and docs/profiles/2026-09-29-wormhole-opening/; it remains intact.

## Checks

Both final builds pass101 CPU tests; normal jar has zero optional backend entries.
Logs: run/wormhole-continuous-final-normal-build.log and -final-rtx-build.log.
Actual jar: build/libs/interstellar-0.1.0-dev.jar. Final build flavour is RTX.

37 GPU rays against independent Cartesian reference pass (max direction error
0.000026155), including grazing rays and repeated passages. Both entrances visible
with either nearest; paired GL/RTX MAE0.000897 and0.001181/255. A grazing Newton
root loop and Vulkan-reserved identifier were caught and fixed during development.
Final fog-only adjustment follows those optical checks; see evidence for its final
image/timing acceptance: final GL/RTX MAE0.008638/255; GPU p50 11.55ms, frame p50
15.03ms (~67FPS), zero queued chunks at1280/full/fine/2x. No runtime errors in the
final log. Broad movement/flicker testing stays owner-deferred.

Cold cached-shader load opened in24s. Actual replacement while moving opened in5s
with overlapping cached terrain; no universal speedup claim. Initial cold shader
compilation can take several minutes after changes to shared includes. Percentages
count preparation work, not expected seconds remaining.

## Runtime and preservation

Owner explicitly agreed to leave the client idle. Original latest save
Interstellar Opening QA 2026-09-29 was saved normally, then copied to
Interstellar Continuous QA 2026-09-29. ONLY this new copy received test commands.
Original revision26: A(375.5098,168,43.4984), B(547.1056,143,61.0946).
QA replacement revision27: A(547.1056,143,61.0946), B(461.5,170,-61.5).
One QA landing block at(461,160,-62). Owner's full/fine/2x/RTX/weather-on/body-off
options still hash-identical to run/rift-pearl-owner-options.json.

Final client: run/wormhole-continuous-release-runtime.log, Gradle session60695,
quick-play init run/wormhole-continuous-init.gradle, PID8840 (verify first). Left
paused at(475.5,190,-139.5), yaw0/pitch16, with both mouths in view.
Final archived log: run/wormhole-continuous-final-acceptance.log.
Earlier acceptance: run/wormhole-continuous-acceptance1.log. First-pass failures
are archived separately; do not mistake them for the final build.

GUI helpers: run/control-short.ps1 for held keys; run/send-safe-command.ps1 for
commands; run/rtx-live-pair.ps1 for same-frame comparison; run/rtx-live-timings.ps1
for120 warmup+300 measured frames. F9 then C runs the GPU fixture after capture;
F9 releases live terrain, so returning to F10 recaptures it. R resets roll.

## Remaining limits

Remote capture remains five chunks around each mouth outside the local camera
window. Remote mobs/interactions, other entity transit and cross-dimension passage
remain unsupported. Deep repeated images can end at the dark limit. This iteration
checks nearby mouths in the owner's ice landscape; extreme separations/coordinates
and a broad biome survey were not tested. Initial/local capture and per-chunk
rebuild cost remain opportunities, separate from the corrected opening condition.
