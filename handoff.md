# Handoff — Growing wormhole mouths, 2026-09-29

## Current state

Branch codex/rtx-wormhole-demo. See Git for current commit. Branches/pushes and
runtime input authorized; no subagents. Follow AGENTS.md and the token-efficient
workflow. Preserve original worlds, .idea and AA WIP8ad46eb. No active goal.

This iteration implements the owner's approved opening sequence:

- First pearl grows a small radius0.8 closed core. Native sphere initially; local
  capture then enables Schwarzschild lensing. No server gravity or destruction.
- Second end/replacement: hover Opening N%, smooth radius growth toward8; BH optics
  until destinations are prepared, then0.35s reveal into Ellis optics. Nearest mouth
  drives lensing; another closed mouth in view remains a sphere marker.
- Renderer ownership separated from optical mode. Local mesh survives layout
  changes and continues during remote preparation. Unloaded empty placeholders
  cannot qualify as captured remote geometry. No new geodesic equations.
-100% and travel acknowledgement require the completed passage image. Starting
  inside a growing mouth, or admin teleporting inside, does not cause transit;
  step outside and re-enter. Hover hint explains this.
- R already was the dedicated rebindable upright key, independent of F4. Verified
  resetting -10.86466 degrees of roll to0 without changing position/aim.
- GL/RTX share lifecycle and growth. Ordinary builds stay Vulkan-free.

Implementation/usage: docs/settings-and-wormhole-seed.md. Decision D093.
Evidence and screenshots: docs/profiles/2026-09-29-wormhole-opening/.

## Verification

Normal and RTX builds pass95 tests (seven new progress/entry tests). Normal jar
has zero optional Vulkan/shaderc/backend entries. Logs:
run/wormhole-opening-final-normal-build.log, wormhole-opening-final-rtx-build.log,
wormhole-opening-artifact-check.txt. The actual artifact name includes -dev.jar.

Runtime new shader compilation, actual first/second/replacement throws, native
marker, BH growth, reveal, inside-on-open guard, deliberate crossing and R pass.
Same-frame GL/RTX RGB MAE .0004174/255 closed, .0011306/255 connected.
Settled1280x720/full/fine/2x RTX: GPU p50 13.579ms, frame p50 15.622ms (~64FPS),
6.247M triangles and0 queued chunks,120 warmup +300 measured frames. No matched
before/after regression claim. Earlier216-queued-chunk timing excluded.
Broad movement/flicker remains owner-deferred. Final text-only step-out hint built
after visual checks; final client restarted to include it, no extra screenshot pass.

Acceptance logs: run/wormhole-opening-acceptance.log and
run/wormhole-opening-first-pass.log. Earlier first-pass screenshots involved owner
movement; then owner explicitly agreed to leave idle for controlled final checks.
Tests only modified the copied Opening QA world. Terrain options hash still matches
run/rift-pearl-owner-options.json (full/fine/2x/RTX/weather on/body off).

## Runtime and preserved saves

Final RTX client uses Interstellar Opening QA 2026-09-29,1280x720, PID8248
(VERIFY first), Gradle session46550, log run/wormhole-opening-release-runtime.log.
Uses run/wormhole-opening-init.gradle for quick play. Leave paused after capture.
Close identified client normally and wait before another launch; never run two
clients against the same save.

QA pair revision23, Overworld:
 A (231.078557,298,200.31241433424492)
 B (280.5,298,236.65499433424492)
Last controlled pose: feet(280.5,296.38,195.5), yaw0/pitch0, creative FLYING,
Rift Pearl in main hand. Owner exploration can supersede this; verify logs/state.

The source copy Interstellar Rift Pearl QA 2026-09-29 was already owner-revision16
at this session's start, not the old handoff's15. It remains untouched. Separate
Rift Pearl Check, Overnight Check, Calibration and Visual Check are preserved.
Do not restore old saved snapshots over owner exploration.

Normal launch: Launch Interstellar RTX.cmd / gradlew.bat runClient -PinterstellarRtx.
Ordinary launcher remains GL-only. JDK C:/Portable/jdks/temurin-21.0.12.1.
Helpers: run/control-short.ps1, menu-click.ps1, send-safe-command.ps1 (clipboard
preserved), rtx-live-pair.ps1 and rtx-live-timings.ps1. Named PowerShell parameters
are required for Select-String -Path/-Pattern to avoid accidental positional reversal.
F4 settings; R upright; Alt+F12 renderer; F10 optics; F12 timing.

## Limits / future work

One same-dimension pair, radius8, open volume required. Five chunks around each
remote end, nearest-mouth chart approximation. Progress is a smoothed work estimate,
not ETA;99% can wait for geometry/image readiness. Per-client visual readiness.
Player transit only; no remote entity tracking, vehicles/projectile passage or
remote interaction.8x remains costly and optional. No unrelated feature work queued
by this request. Current implementation is complete; continue from owner feedback.
