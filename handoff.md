# Handoff — live RTX exterior world, 2026-09-28

## Checkout and scope

Repo C:\work\code\minecraft\interstellar\interstellar; branch codex/rtx-live-world,
based on2f4bd2d. Branches/pushes/runtime checks authorized. Preserve AA WIP8ad46eb,
other branches and worlds. No subagents. Owner requested live RTX with OpenGL fallback,
a future Vulkan-free flavour option (do not package it now), and an OpenGL-only
performance regression check. This bounded live milestone is implemented and checked.
Wormhole side study is complete; no implementation authorized.

## Read first

docs/rtx-live-world-2026-09-28.md and docs/profiles/2026-09-28-rtx-live.
Live1440p frame medians: heavy terrain23.981→8.361ms (~42→120 FPS),
wall11.640→8.352ms (~86→120), at unchanged120 FPS cap/VSync settings. Three
120-warmup/300-sample runs per view/backend; simulation running. RTX Vulkan GPU
3.327/3.263ms includes AS builds but excludes GL copies/resolve. Use frame intervals
for live FPS. Six same-frame image cases show0–12 pixels over16/255, not pixel identity.

Separate normal-build regression, simulation frozen: GPU down24.437→23.762ms,
wall11.073→10.821ms. No observed regression; small gains are treated as variation.
Static terrain count identical; moving triangles8944→8908 across restarts. Raw logs
and standard-library tools/analyze-live-rtx.py reproduce the summary.

## Implementation and limits

WorldRenderBackend replaces FrozenWorldBackend. No Vulkan types in the common API.
WorldBackendBridge reflects into src/rtx/java only when -PinterstellarRtx is enabled.
Ordinary clean jar/dependency graph exclude optional classes and Vulkan/shaderc.
No new flavour/installer; this is a Windows development configuration beside OpenGL.

LiveGeometry retains compact quad terrain, a BLAS per chunk and small reusable actor/
cloud BLAS buffers. Changed chunks alone read back/synchronize/rebuild; removed chunks
release their structures. Moving BLAS/TLAS rebuild, no refits yet. VulkanWorldBackend
GPU-copies five native appearance images each frame and shares the two-AA-sample result
through Win32 memory/semaphores. FullImageShader derives optics/materials from production
GLSL. One frame in flight. About1.54GiB extra observed whole-board memory, not peak.

F10 auto-selects RTX for default ordinary exterior BH rendering. Alt+F12 toggles,
F12 measures active live backend, Ctrl+Alt+F12 saves paired images. Extended sources
and r<1.25rs use GL; exterior return resumes RTX. Setup/frame failure retains GL;
Alt+F12 retries. Historical -PinterstellarRtxImage frozen F9 experiment still works
by design (not rerun this turn). Other vendors, device-loss and production packaging
remain unverified. Automated movement/flicker suite is owner-deferred.

Known-triangle startup query catches empty acceleration structures. During development,
missing geometryCount (LWJGL pGeometries does not set it) caused missing terrain;
fixed and images rechecked. No broken-image timing accepted. Normal terrain capture
~30sec; extra initial terrain BLAS setup~1–2sec in coarse logs. Single glass edit
rebuilt one chunk. Update spikes/long-duration behaviour are not characterized.

Next candidates: remaining optical variants, measured update spikes, selective texture
copies/refits, lifecycle/cross-vendor validation, then optional distribution. Keep GL.

## Verification and current client

Final optional build and clean normal build pass;79 tests. Material fixture passes736
paired queries/256 CPU checks. Runtime image/live-mob/edit/chunk/resize/toggle/horizon
checks pass. Final initializer/storage-limit guards compiled after those runtime checks.
No shader-equation changes. Read report for limits rather than extrapolating FPS.

Normal client PID12488, exec session43438, run/rtx-live-gl-after-runtime.log. Paused,
F10 off, no RTX flags, narrator0; ticks unfrozen20tps. To try RTX, close this client
normally first, then gradlew.bat runClient -PinterstellarRtx. No second client/world.

All eight fresh owner player fields match: creative, flying=false, interstellar:arrows,
feet16.42080350758872/65/-55.7896552801982, yaw-18.765259/pitch-5.99997,
slot8, health20, inventory/chainmail unchanged. Source auto restored. Temporary glass
at17/303/-5 in overworld was placed only after an air check and removed. Normal time
advanced; no time/weather edits. Window854x480 at outer1241,579–2111,1098 restored
from previous checkpoint's window record (not freshly captured before this turn).
Player records: run/rtx-live-owner-state.txt, run/rtx-live-restored-state.txt.

JDK C:\Portable\jdks\temurin-21.0.12.1; Python py -3 (3.8, noPIL).
Local helpers run/restart-client.ps1, run/restart-rtx-live.ps1; run/rtx-live-pair.ps1
waits for comparison completion, run/rtx-live-timings.ps1 handles live F12 tests.
Runtime5 contains successful live checks; earlier logs are failed development attempts.
Initial down-live-rtx labels had wrong pose and are excluded from archived evidence.
