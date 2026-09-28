# Handoff — RTX coverage and wormhole demo, 2026-09-28

## Current state

Both overnight implementations now have runtime acceptance evidence:

1. RTX covers exterior black holes, extended masses and near/inside horizons.
2. Two spherical Ellis mouths connect real demo environments 1,145 blocks apart,
   with curved remote geometry and two-way player passage. GL and RTX both work.

Repo: C:\work\code\minecraft\interstellar\interstellar.
Branch: codex/rtx-wormhole-demo; see Git for latest commit. Foundation: 318e79b and
586f947; final renderer/evidence follows. Branches/pushes/runtime input authorized.
No subagents. Preserve original worlds/settings and AA WIP8ad46eb. Follow AGENTS.md.

Latest follow-up (2026-09-29): R resets wormhole roll to upright without changing
position or aim. Rebind in Controls → Interstellar; HUD uses the current binding.
Client request calls the existing server reset and clears both stored frames.
Build and runtime reset/idempotence check passed; see progress.md. Logs:
run/wormhole-reset-build.log, run/wormhole-reset-runtime.log, run/wormhole-reset-check.txt.

## Read next

- docs/wormhole-demo-implementation.md: implementation, physics, results and limits.
- docs/rtx-optical-variants-2026-09-28.md: Goal1 and final regression checks.
- docs/overnight-goals-2026-09-28.md: original requirements and metric derivation.
- docs/demo-quickstart.md: launch and controls.
- docs/profiles/2026-09-28-wormhole-optics: compact final evidence.
- docs/profiles/2026-09-28-wormhole-foundation: remote block/light/lifecycle checks.

## Implementation

Shared wormhole.glsl uses Ellis Hamiltonian RK4 with curvature-bounded chords,
splitting exactly at l=0. No geometry query spans the map gap. Areal throat16,
coordinate mouth radius8; centres(8,96,8)/(1032,112,520). Inversion plus z reflection
preserves handedness; its differential transports camera axes and flight velocity.
Camera roll and interpolation endpoints cross too. TerrainScreen has explicit
wormhole mode, no fake source. GL and RTX use the same optics/material source.

Both regions share native geometry/RTX acceleration structures. Clouds and block
entities capture at both ends; remote mob tracking is not added. Fog uses traversed
chord distance. Sky rays retain weak bending and an asymptotic tail. Limits:2,048
steps/four windings. Native materials, lighting and AA retain their approximations.
Ultrastatic Ellis has no event horizon, gravitational redshift or compulsory pull.

WormholeChunks delivers native blocks/light for242 chunks with bounded tickets and
packets. Client light-queue acknowledgement gates travel. Edits coalesce; regions
release on exit. Use nonblocking getWorldChunk, not getChunkFutureSyncOnMainThread
(which pumps the main thread until completion).

## Verified results

- Final normal and RTX builds pass88 tests. Normal jar excludes optional backend,
  Vulkan and shaderc classes; run/wormhole-final-build-check.json.
- Seven CPU Ellis tests cover analytic deflection, invariant/reversal, transfer
  differential and chart-equivalent paths. Actual GPU:2,575 rays, zero mismatches;
  max direction-vector error4.193e-5 (~0.0024 degrees), including exact throat orbit.
- Same-camera chart images: centreline exact, oblique RGB MAE.000930 on0–1; far
  .000141. Far finer-step pair MAE.0000576. Representative contact sheets inspected.
- 1440p GL/RTX wormhole MAE.00208–.01036 on0–255. Centreline and oblique rendered
  crossings work both ways, client/server position and velocity agree, roll retained.
- Live1440p RTX: exteriors99/100FPS, oblique return113FPS, off-axis exit120FPS at the
  existing cap. Two300-sample runs per view; tails and queued-chunk states recorded.
  These are this82,312-triangle exhibit, not arbitrary terrain/hardware guarantees.
- Final BH regression: actual small8-block/near/inside variants, RGB MAE
  .000670/.002131/.000573 on0–255. Initial near/inside attempts selected a different
  mass and ran exterior optics: excluded, repeated after explicit inspection.
- Temporary central test mass removed afterwards (initially air). Original save
  timestamps remain21:10:27 Calibration and21:58:12 prior Visual Check. options.txt
  matches run/wormhole-optics-owner-options.txt; AA WIP8ad46eb intact.

## Current runtime / launch

Minecraft is running in isolated save Interstellar Overnight Check 2026-09-28,
RTX ready,1280x720 window. After the reset-control check, the owner's pre-test
position/aim at mouth B was restored and Esc paused the game; the owner may resume.
Current Java PID5536; VERIFY before acting. Gradle session84549; runtime log
run/wormhole-reset-runtime.log. No active input helpers. Close the verified client
normally and wait for actual exit before relaunching. Never two clients per save.

Launch Interstellar RTX.cmd / gradlew.bat runClient -PinterstellarRtx enables RTX.
Original launcher/ordinary build remains OpenGL-only. /interstellar demo wormholes
arms optics automatically; wait for World view ready. Alt+F12 switches backend,
F10 toggles optics. Named views mouth_a/mouth_b/throat_a/throat_b reset roll.
/interstellar demo leave restores saved original location/mode.

Automation init run/overnight-init.gradle targets the isolated copy. JDK21 at
C:/Portable/jdks/temurin-21.0.12.1. Helpers: control-short.ps1, send-safe-command.ps1,
size-minecraft.ps1, rtx-image-controls.ps1, rtx-live-pair.ps1, rtx-live-timings.ps1.
F9 C runs GPU reference; Ctrl+Alt+G compares charts; Ctrl+Alt+V enables RTX then
Ctrl+Alt+P compares backends. Use log readiness and bounded waits. Evidence README
has reproduction details. Final screenshot: docs/profiles/2026-09-28-wormhole-optics/wormhole-mouth-a.png.

## Future scope

Fixed demo/player passage is implemented. Arbitrary pairing UI, remote mob tracking,
vehicles/mobs/projectile transit, remote interaction, persistent roll on reconnect,
massive-body geodesics and global relativistic illumination are not included.
Broad movement/flicker testing remains owner-deferred. The hypothetical supporting
matter is not simulated; placing charts in Minecraft is not a global Einstein
solution. Do not claim otherwise.

The final Java-only guard prevents F9 M switching wormholes to the legacy BH voxel
renderer, and excludes wormholes from the old BH-only replay tool. Both builds and
the ordinary artifact boundary were checked again; the final client includes this
guard. It changes no default optical calculations or measured rendering settings.
