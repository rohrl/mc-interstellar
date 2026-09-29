# Handoff — Rift Pearl gameplay UX, 2026-09-29

## Current state

Branch codex/rtx-wormhole-demo. See Git for latest commit. Pushes, branches and
runtime input authorized; no subagents. Follow AGENTS.md. Preserve original worlds,
.idea and AA WIP commit8ad46eb. No active overnight goal.

Previous checkpoint0510340 added F4 settings, all five GL/RTX AA modes, one saved
throwable wormhole pair, chunk lifetime/readiness fixes and exit collision guard.
This follow-up fixes the confusing first-mouth experience:

- The actual rejection was an old pair in Gameplay rejecting Overworld throws.
  A valid throw in another dimension now starts a fresh local pair and reports
  the old pair closing. Invalid placement preserves it. Same-dimension later
  throws still replace only the oldest end. Cross-dimension travel not added.
- First end is a native opaque dark sphere with a cyan rim. No wormhole ray tracing,
  remote preparation or gravity for an unpaired end. Crosshair says Closed and
  prompts a second throw. Connected/loading states also labelled; terrain occludes
  the hint. Closed mesh can join existing BH entity capture if already active.
- Item is now Rift Pearl, custom64×64 sprite (source/prompt in docs/artwork).
  ID remains interstellar:wormhole_seed for existing stacks/recipe compatibility.
  Creative Tools & Utilities; /give @s interstellar:wormhole_seed; sneak-use clears.
- RTX already defaulted on for connected wormholes in an RTX launch. Verified
  actual automatic activation. Preserve explicit saved OpenGL choice and fallback.
- Native closed-mouth F12 timing reuses LabBenchmark only when requested.

Read docs/settings-and-wormhole-seed.md and docs/profiles/2026-09-29-rift-pearl/.
Physics/background: docs/wormhole-demo-implementation.md; RTX optical coverage:
docs/rtx-optical-variants-2026-09-28.md. Controls: docs/demo-quickstart.md.

## Verification

Normal and RTX builds pass88 tests. Normal jar contains zero optional/Vulkan/
shaderc entries. Logs: run/rift-pearl-normal-build.log, run/rift-pearl-final-build.log,
run/rift-pearl-artifact-check.txt. No optical equations/shaders changed.

Actual empty-pair first throw, dimension move, invalid-placement preservation,
closed-end restart, second-end connection, custom sprite and depth/label occlusion
passed. Closed-sphere fixed-view GPU draw p50 .009344ms; frame p50 8.3452ms at120FPS
cap,1280×720,300 samples after120 warmup. Earlier falling-player timing excluded.
Connected full/fine2× GL/RTX same-frame RGB MAE .0020613/255. Screenshot shows RTX
and Connected. Closed-marker capture alongside a BH was not separately compared.
Broad movement/flicker remains owner-deferred.

## Current runtime and preserved state

One RTX client is paused in `Interstellar Rift Pearl QA 2026-09-29`,1280×720.
PID27952; VERIFY first. Gradle session20538. Log run/rift-pearl-reload-runtime.log.
No active input helpers. Close identified client normally; wait for exit before
another launch. Do not run two clients against the same save.

QA pair revision15 in Overworld: (200.5,298,218.87673861773072) and
(280.5,298,218.87673861773072). Player on glass podium at feet(200.5,296,195.5),
yaw0/pitch0, creative but NOT flying. Main hand holds pearl. Temporary wall removed.
Quality remains owner's full resolution/fine paths/2×/RTX/weather on/body off;
config hash equals run/rift-pearl-owner-options.json. Do not reset these casually.

The owner explored `Interstellar Rift Pearl Check 2026-09-29` during the first
checks, creating revision12: (200.5,298,223.67978012696622) and
(273.78536059994,298,203.69964719214747). That world is preserved separately.
The preceding `Interstellar Overnight Check 2026-09-28` was copied at09:47:03 and
not modified in this iteration. Original Calibration/Visual Check untouched.
An optional idle question was sent after noticing movement; no answer was needed
because subsequent tests used another disposable copy. New owner changes supersede
recorded poses. Do not restore old revision8 over their later exploration.

Launch normally: Launch Interstellar RTX.cmd / gradlew.bat runClient -PinterstellarRtx.
Ordinary launcher remains OpenGL-only. QA launch adds -I run/rift-pearl-qa-init.gradle;
run/rift-pearl-init.gradle selects the owner's separate Rift Pearl Check copy.
JDK C:/Portable/jdks/temurin-21.0.12.1. Reliable helpers in run/: menu-click.ps1,
control-short.ps1, send-safe-command.ps1 (clipboard preserved), rtx-live-pair.ps1.
F4 settings; R upright; Alt+F12 renderer; F10 optics; F12 timing.

## Limits

One same-dimension pair, radius8, open volume required. Remote capture remains
five chunks at each end; nearest-mouth chart is not a global spacetime solution.
Player transit only; no remote mob tracking, vehicles/projectile passage or remote
block interaction. Camera roll not persisted on reconnect.8× remains expensive
and optional. Collision guard rejects obstructed exits instead of embedding feet.
