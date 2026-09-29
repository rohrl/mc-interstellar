# Handoff — Unified mass and wormhole gameplay, 2026-09-29

## Current state

Branch codex/rtx-wormhole-demo. See Git for current commit. Branches/pushes and
runtime inputs authorized; no subagents. Follow AGENTS.md and token-efficient
workflow. Preserve original saves, .idea and AA WIP8ad46eb. No active goal.

Owner asked to use mass lensing and wormholes together, with clear F4 controls.
Implemented one live owner/cache, automatic activation, saved master F10/F4 switch,
independent mass/portal visuals, and Gameplay/Graphics/Tools tabs. Server gravity,
capture and strength are separate permission-checked session controls. Portal-off
keeps markers/layout and suspends travel until the next presented passage frame.
Esc from F8/F9 restores the saved gameplay setting automatically.

Mixed scenes use shared Cartesian RK4 ray integration for one selected mass
cluster plus the pair. Spatial curvature is composed with finite influence bounds;
this is an explicit gameplay approximation, NOT a multi-source GR metric. Existing
single-effect solvers remain. Mass blocks still render for interior editing. The
OpenGL-only artifact remains Vulkan-free. Both backends share mixed optics.

See decision D095, docs/science.md, docs/settings-and-wormhole-seed.md and
**docs/profiles/2026-09-29-unified-gameplay/** for assumptions, evidence and images.
D094 / continuous-wormhole evidence remains valid for the pure portal renderer.

## Checks

Final normal and RTX builds pass105 CPU tests. Four new tests cover the spatial
coefficient against independent Hamiltonian acceleration, zero-mass limit, body
surface convergence and mouth-label invariance. Normal jar has zero optional
backend/Vulkan entries. Logs: run/unified-final-normal-build.log and
run/unified-final-rtx-build.log. Jar: build/libs/interstellar-0.1.0-dev.jar (RTX).

69 GPU rays pass:37 localized-wormhole and32 mixed cases. Max direction error
0.0005831605 against0.001 tolerance; exact capture/passage classification. GPU RK4
is checked against fine CPU midpoint integration using metric derivatives. A body
surface derivative jump required targeted small steps for numerical convergence.
These checks validate the stated approximate rule, not physical multi-source GR.

Runtime: automatic combined activation, all AA modes on RTX, feature/menu/gravity
controls, lab return, readiness withdrawal/reacknowledgement, actual portal crossing
with mass active, R, and inside-BH mining. Mining changed64→63 (extended body), then
restoring the block returned to BH, automatically and without cache replacement.
Master-off survives client restart, then F10 restores both effects. No runtime errors.

Final same-frame GL/RTX image MAE0.003395/255, RMSE0.279716,54/921600 pixels above16.
At1280x720/full/fine/2x with BH+both mouths, settled frame p50=34.06ms (~29FPS),
GPU31.25ms. Normal paths:32.28ms (~31FPS). At50%/fine/2x:15.43ms (~65FPS), GPU13.59ms.
All these final timings had zero queued chunks. Combined rendering costs more.
Exploratory single-effect/close-up runs had pending terrain; do not claim clean
regression percentages from them. Owner's full/fine/2x/RTX settings restored.

## Runtime and preservation

Saved the owner's then-current Interstellar Continuous QA 2026-09-29 normally,
then copied to **Interstellar Unified QA 2026-09-29**. ONLY the new copy received
test commands. Pair at copy time r28: A(461.5,170,-61.5), B(375.5571,168,43.2909).
Original mass8 centred(502,142,6). The copy now has64 blocks at x500..503,
y140..143,z4..7. Original save retains the owner's eight blocks and r28 pair.

Final client PID13676 (verify), Gradle session88467, run/unified-release-runtime.log,
quickplay init run/unified-gameplay-init.gradle. Left paused at feet(485.5,169.38,-110.5),
yaw10/pitch10, all three objects in view. Master/mass/portal/status true,100%/fine/2x,
RTX preferred, weather on, returning body off. Gravity default .2/on/capture-on.
Logs: run/unified-gameplay-final-optics.log and run/unified-release-acceptance.log.
Early run/acceptance1 logs are not final shader evidence.

GUI helpers in ignored run/: control-short.ps1 (held native keys), menu-click.ps1,
send-safe-command.ps1, size-minecraft.ps1, rtx-live-pair.ps1, rtx-live-timings.ps1.
F9+C runs69 rays, but let native chunks arrive BEFORE opening F9: freezing the
integrated server too early can stall missing-chunk capture. Esc resumes automatically;
DO NOT press F10 afterward unless deliberately disabling the saved master switch.
Cold shader compilation after shared includes took roughly5.5 minutes; cached
launch is fast. Wait a few seconds after Loaded1399 advancements before accepting
the experimental-world dialog. At854x480 click595,305; at1280x720 click885,442.
F4 button positions at1280: tabs x235/645/1045,y175; row centres y264/336/408/480;
left/right x335/965. Wait for server acknowledgements between gravity toggles.

## Remaining limits / opportunities

Only one selected mass cluster plus one portal pair. Overlapping strong fields are
approximate; near/interior mixed observer views have visual/editor checks, not a
new independent spacetime solution. Four passages/8pi turn/2048 steps bound rays.
Remote capture stays five chunks around each mouth beyond the camera window;
remote entities/interactions, other entity transit and cross-dimension passage are
still unsupported. Broad movement/flicker surveys remain owner-deferred.

Mixed full-resolution performance is a future optimization target; finer optics
and a second field add cost. Half resolution is the existing optional speed tradeoff,
not an invisible optimization. Server gravity stays the dominant-source gameplay
model. Readiness/gravity network packets changed; client/server need matching builds.
