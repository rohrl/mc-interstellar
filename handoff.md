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

- docs/wormhole-demo-implementation.md: current native-region/travel implementation and evidence.
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

Reference Ellis mathematics plus SEVEN tests (88 total) now include chart-invariant
ray paths. Areal throat a=16, isotropic mouth radius8. Inversion plus z reflection
preserves handedness; signed proper l=R-a²/(4R). See goal doc for maths.

Implemented/runtime-checked native foundation:
- /interstellar demo wormholes builds19788 blocks in new interstellar:wormholes.
  Centres(8,96,8)/(1032,112,520),1145 blocks apart. Orange/cyan real environments.
- WormholeChunks adds2 tickets/tick, sends at most2 native chunk/light packets/tick
  (2ms loop slice), retains242 designated chunks. Dirty block/light events coalesce.
  getWorldChunk(x,z) is nonblocking; getChunkFutureSyncOnMainThread actually waits!
- Client manager cache bypasses vanilla ring for designated chunks; retains native
  lighting on unload. Ordered light-queue acknowledgement gates native readiness.
  StreamingTerrain union retention added, but optical capture still unexercised.
- Remote edits/light verified BEFORE visiting either destination; leaving releases
  242 tickets, re-entry re-acknowledges. Initial native load around6sec after entry.
- WormholeTravel maps eye/direction/up, sends payload before vanilla teleport; client
  restores transformed flight velocity and maps interpolation endpoints to new chart.
  WormholeCameraMixin applies roll to actual camera. Both centreline and oblique
  W/S crossings checked in BOTH directions. Off-axis exit roll−12.18° visually checked.
- Commands: view mouth_a|mouth_b|throat_a|throat_b; wormholes status; demo leave.
  Viewpoints reset roll. Leave restores original mode/location. No player vehicles.
- Tiny exceptional guards added after runtime: ignore teleport-sized velocity
  estimates and skip singular previous-camera inversion; final builds cover them.

NEXT: implement the actual shared GL/RTX WORMHOLE shader variant and wire F10/F9/HUD.
NO WORMHOLE OPTICAL SHADER EXISTS YET. Current views/travel use normal Minecraft;
do not claim rendered smooth transit, wormhole visual accuracy or FPS acceptance.
TerrainScreen should get explicit wormhole mode, no fake SourcePayload. Choose current
mouth for Source, other mouth uniform, and the metric throat radius. Use rolled camera
basis (currently BH configureShader reconstructs only yaw/pitch). Shader segment must
split at l=0, map each side separately; never trace the1145-block map gap. Preserve
native materials and fog without counting that gap. NativeSky/CloudMesh and EntityMesh
need both regions: CloudMesh currently builds around current camera only, EntityMesh
culls to camera chunk window and native block-entity dispatcher can distance-cull.
Add paired GL/RTX images, independent ray checks, rendered two-way crossings and FPS.
Source extracts remain in run/mc-source; no agents; read detailed feature doc once.

## Runtime safety and current state

No development client running. Closed latest wormhole test client normally.
Original options restored from run/wormhole-owner-options.txt. Final build is
OpenGL-only; use -PinterstellarRtx to launch optional backend again.

All tests used NEW save folder: run/saves/Interstellar Overnight Check 2026-09-28.
Its internal display name is still Interstellar Calibration; verify quick-play
argument, not log display name. Init script run/overnight-init.gradle targets it.
Original Calibration level.dat remains21:10:27; prior Visual Check remains21:58:12.
Both preserved. Test copy returned to its original saved location/mode via demo leave.
Its new wormhole dimension is built; both temporary glowstone edits were restored.

Logs run/rtx-variants-runtime.log and runtime2.log. Last build logs
run/rtx-variants-final-build.log / run/rtx-variants-normal-build.log.
Newer logs: run/wormhole-regions-runtime.log / wormhole-transit-runtime.log;
run/wormhole-foundation-final-build.log / wormhole-foundation-normal-build.log.
Final optional and clean ordinary builds both pass88 tests. Native-region/travel
evidence is in docs/profiles/2026-09-28-wormhole-foundation. Native screenshot there
shows camera roll only, not wormhole optics. All test clients closed normally.
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
