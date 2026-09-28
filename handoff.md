# Handoff — small-mass lens artifacts, 2026-09-28

## Current state

Repo C:\work\code\minecraft\interstellar\interstellar; branch codex/extended-lens-artifacts,
based on 8306d6d (live RTX). This iteration fixes the owner's jagged extended-mass
screenshots and ambiguous Alt+F12 behaviour. Branches/pushes/runtime checks authorized;
no subagents. Preserve AA WIP 8ad46eb and unrelated branches/worlds.

Read docs/extended-lens-artifacts-2026-09-28.md and its linked evidence. For the
previous live RTX architecture/results, read docs/rtx-live-world-2026-09-28.md.
The wormhole side study is complete; implementation remains unauthorized.

## Findings and implementation

Owner F2 images are run/screenshots/2026-09-28_21.00.{29,31,39}.png, N14 extended
mass, C~0.32. That launch was OpenGL-only. Extended sources also use OpenGL with
RTX enabled; the old integrator predates RTX. RK stages straddled the metric surface,
where the radial derivative jumps. bodyAdvance now uses one branch per RK step,
splits at the surface, fixes the slope via the null invariant, and handles grazing
entry/immediate exit. AA, resolution and BH equations are unchanged.

F12 now uses the registered key's press-event modifiers through KeyboardMixin;
repeat events do not retrigger. Alt+F12 in a normal build explains the launch flag,
with no benchmark. Optional builds toggle renderer preference and explain extended/
near-horizon fallback. HUD always names the active renderer. Plain F12 measures,
Ctrl+Alt+F12 compares. Launch RTX: gradlew.bat runClient -PinterstellarRtx.

## Checks and limits

Optional build and clean normal build pass 81 tests. Runtime shaders compile.
Independent Hamiltonian checks cover both directions/4 compactnesses/3 incidence
angles; grazing tests cover entry and immediate exit. Final fixed-pose crop error
against finer angular steps falls 94.5%; inspected 14-block and 2x2x2 cases.
Tool tools/LensBoundaryEvidence.java checks camera/projection/quality before comparing
static terrain; full-frame cross-restart comparisons would include moving actors.

Final small-source GPU medians 4.118/4.297 ms, frame medians 8.337/8.373 ms at 120 FPS
cap. Original GPU medians 3.455/4.109 ms; intermediate correction 3.910–4.554 ms.
Noisy, different actors after restart: some correction cost is plausible; do not
claim zero overhead. See report for raw timings. RTX BH final smoke check: frame
8.338 ms, Vulkan GPU 1.605 ms (excludes GL copies/resolve). Same-frame RTX/GL MAE
0.00220/255, 28 pixels over16/255; tiny differences remain. This is not a repeat of
the prior 1440p regression series. BH shader equations were not changed.

Brief (20 ms) Alt+F12 switches optional renderer both directions, benchmark-start
count remains 2->2. Normal launch gives guidance. Plain F12 benchmark still finishes.
Automated movement/flicker tests remain owner-deferred.

## Runtime and preservation

No development client left running. Closed test PID23720 normally, then restored
run/options.txt from run/lens-check-owner-options.txt. Both builds pass; final build
is normal OpenGL-only. To try optional RTX, use the launch flag above.

All in-world edits/tests were in the separate save folder:
run/saves/Interstellar Visual Check 2026-09-28
Created from Interstellar Calibration. Its internal display name is still
Interstellar Calibration, so logs alone do not distinguish it. Verified running
command line: --quickPlaySingleplayer "Interstellar Visual Check 2026-09-28".
Original world was not opened: level.dat remains21:10:27, session.lock21:08:54.
Do NOT restore old player coordinates from prior handoffs: the owner explored after
8306d6d and changed their original source to64 blocks. That save remains authoritative.

Test copy is frozen/spectator, last source64 blocks at centre3/82/3. Earlier14-block
fixture: fill2/80/3..3/82/4 plus4/80/3..4/80/4; eye0.4/82.4199998856/1.0, yaw-43,
pitch18. Use decimal1.0 in /tp (integer1 centres at1.5). Body reference pair uses
F9 Shift+[ after capture readiness. The final shared shader was reloaded before
final pair18411850027943348760; it includes both grazing guards. Pair9626865030655472664
is the actual8-block case. Before pair15350767883361577764. Archived under
 docs/profiles/2026-09-28-lens-boundary.

Owner briefly explored during the final check, then explicitly left the client idle;
fixed camera and recaptured before final measurements. Earlier wrong-pose/unfinished
captures are not final evidence. Shader reload/compile can take about3 minutes;
do not send input until completion. The last diagnostic shader warning marks the
end, but gate actual captures on their completion log.

JDK C:\Portable\jdks\temurin-21.0.12.1. Python py -3 (3.8, no PIL).
Local logs run/lens-check-baseline-runtime.log, run/lens-check-rtx-runtime.log;
builds run/lens-final-build.log (optional), run/lens-normal-build.log (clean normal).
Startup helper run/lens-check-init.gradle targets only the copied world.

## Next work

No outstanding work on this reported defect. Preserve GL fallback and optional
Vulkan-free builds. Existing RTX candidates remain additional optical variants,
measured update spikes, selective texture copies/refits, lifecycle/cross-vendor checks,
then optional distribution. Do not infer all-angle scientific validation or uncapped
FPS from the limited artifact checks.
