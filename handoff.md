# Handoff — overnight RTX + wormhole goals, 2026-09-28

## Goal and scope

ACTIVE full user goal: (1) RTX for small masses and near/inside horizons without
visible correctness regressions; (2) implement the studied wormhole as a playable,
physics-based two-mouth demo connecting distant locations, with smooth two-way
player passage, preferably RTX. The owner leaves the PC overnight. Continue both
until verified; do not mark the overall goal complete after finishing only RTX.
Wormholes are NOW explicitly authorized, superseding earlier analysis-only notes.

Repo C:\work\code\minecraft\interstellar\interstellar, branch codex/rtx-wormhole-demo,
based on8c637da. No subagents. Branches/commits/pushes/runtime actions authorized.
Preserve AA WIP8ad46eb, existing saves and settings. Token-efficient fixed-pose image
metrics plus visual inspection; broad movement/flicker suite remains deferred.
Actual wormhole passage still needs runtime checks because it is part of the goal.

## Read next

- docs/overnight-goals-2026-09-28.md: requirements, chosen physics, pending architecture.
- docs/rtx-optical-variants-2026-09-28.md: completed Goal1 and measured evidence.
- docs/wormholes-feasibility.md: original study, now implementation authorized.
- docs/rtx-live-world-2026-09-28.md: underlying optional backend architecture.

## Goal1 finished

WorldRenderBackend.Optics selects EXTERIOR/EXTENDED/HORIZON. VulkanWorldBackend
precompiles six probe/material pipelines during setup and switches them without
rebuilding geometry. FrozenBackendCapture expands the extended_source include;
FullImageShader defines the matching production variant. TerrainScreen removes
variant eligibility exclusions and reads uniforms from the correct material shader.
The historical -PinterstellarRtxImage frozen experiment remains exterior-only;
-PinterstellarRtx covers all current models in live and manually enabled F9 views.

A shared correctness fix moves captured selection-outline ribbons .002 blocks
towards the camera to remove coplanar hit-order ties. Its interior edit image now
matches exactly. No optical-equation changes. Normal GL shader source unchanged.
1440p comparisons: N8/N14/near/inside MAE .0006–.0018 on0–255;25–56 pixels over16.
Centre cutoff and forward black views are trivial checks; sideways images validate
actual sky/terrain optics. Representative contact sheets were inspected.

With simulation RUNNING at1440p, frame medians8.321/8.328/8.374ms forN14/near/inside,
at existing120FPS cap. GPU3.029/7.215/3.137ms excludes GL copies/resolve. Raw evidence
in docs/profiles/2026-09-28-rtx-variants; standard-library analyze-variant-checks.py.
Both optional build and clean normal build pass87 tests; normal jar excludes optional
classes. Do not extrapolate to every view or call cap-limited timings uncapped FPS.

## Goal2 progress — incomplete

Only reference mathematics is implemented so far:
src/main/java/io/github/rohrl/interstellar/science/EllisWormhole.java and six tests.
All87 total tests pass. Metric is ultrastatic Ellis; signed proper radial coordinate
l and Hamiltonian radial momentum/plane angle. Exact elliptic-integral reflection
and transmission limits, invariant, reversal, end symmetry and circular throat ray
are checked. Isotropic charts use l=R-a²/(4R), conformal factor1+a²/(4R²), mouth
coordinate radiusa/2. Transition is spherical inversion followed by z reflection,
with differential for velocity and camera axes. It is reversible and preserves
handedness. See goal doc for physics sources and careful explanation.

NEXT: implement bounded distant chunk delivery/cache, union capture of both regions,
wormhole shader/shared RTX variant, separate demo and continuous authoritative
player crossing. Read the planned details in the goal doc; none is implemented yet.
Use real remote geometry, not cubemaps for terrain. Fog must not count the long
Minecraft separation; preserve camera up/roll across off-axis crossings. Pair
centres can be roughly1024 blocks apart in a new void demo dimension. Avoid changing
render distance or making mouths artificially close merely to evade remote loading.

Minecraft source extracts for APIs are in run/mc-source, obtained with
run/read-mc-sources.py (Windows long-path prefix required). Important APIs:
ClientChunkManager.loadChunkFromPacket/getChunk; handler.onChunkData reads native
lighting afterwards. Native onUnloadChunk separately clears light; retained remote
chunks need protection from that. Normal chunk ring buffer cannot store both regions.
StreamingTerrain currently one camera-centred window capped16 chunks; adapt it to
retain both bounded regions in wormhole world. Other world behaviour must stay intact.

## Runtime safety and current state

No development client running. Closed latest overnight test client normally.
Original options restored from run/overnight-owner-options.txt. Final build is
OpenGL-only; use -PinterstellarRtx to launch optional backend again.

All tests used NEW save folder: run/saves/Interstellar Overnight Check 2026-09-28.
Its internal display name is still Interstellar Calibration; verify quick-play
argument, not log display name. Init script run/overnight-init.gradle targets it.
Original Calibration level.dat remains21:10:27; prior Visual Check remains21:58:12.
Both preserved. Test copy ends spectator, ticks running,64-block sourcecentre3/82/3,
feet3/80.38/0 yaw90 pitch0, F10 was active before exit.

Logs run/rtx-variants-runtime.log and runtime2.log. Last build logs
run/rtx-variants-final-build.log / run/rtx-variants-normal-build.log.
Initial test client21744 closed; subsequent client also closed by verified quick-play
match. Do not reuse old PIDs. Existing control scripts run/control-short.ps1,
run/send-safe-command.ps1 (preserves clipboard), run/size-minecraft.ps1,
run/rtx-live-pair.ps1, run/rtx-live-timings.ps1. They gate on completed log events.
Startup confirmation click595/305 in854x480; resize explicitly after world entry.

JDK C:\Portable\jdks\temurin-21.0.12.1. Python py -3 (3.8, no PIL/numpy).
For plain F12 timing, client must be in game. Alt+F12 toggles; Ctrl+Alt+F12 pairs.
Remember decimal1.0 in /tp (integer1 becomes1.5). Helper focus can shift mouse view
when leaving menus; check captured camera metadata. Shader compile/reload may take
minutes when common GLSL changes; wait for actual completion before GUI sequences.
