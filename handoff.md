# Handoff — native RTX feasibility completed, 2026-09-28

## Checkout and scope

Repo C:\work\code\minecraft\interstellar\interstellar; branch
codex/rtx-native-feasibility, based on c503055. User authorizes implementation,
branches/pushes and autonomous runtime verification. No subagents. Preserve AA
WIP8ad46eb, other branches and owner worlds. Latest approval was the bounded native
RTX feasibility milestone. It is complete; no production RTX backend was started.

Read docs/rtx-native-feasibility-2026-09-28.md for results/limitations and
tools/rtx-probe/README.md for controls. Do not repeat broad history reads or benchmarks
just to explain this checkpoint. Normal optics and quality settings are unchanged.

## Delivered and measured

- Native mesh/atlas export and sparse real-chord logging preserve recorded tree skips.
  Normal-vs-recorder float RGBA components match exactly in both passes/two views.
- Main scene 6,273,150 triangles: 6,261,506 terrain, 8,956 actors, 2,688 clouds.
  N65 rs3.518; eye16.5/303.62/-45.5, yaw0.281, pitch35.91 /0.91. 1440p output,
  logical720p sharp2x. Down286,235 initial queries +231 material; wall336,218 +0.
- Alpha-aware initial queries: software/RTX0.121896/0.015916ms down (7.66x),
  0.071216/0.012576ms wall (5.66x). Software is standalone triangle BVH, not current
  GL chunk/quad traversal. Flattened warm replay excludes optics, full shading,
  cache construction and live updates. Never call these FPS gains.
- 1,245,368 paired comparisons, no differences; all alpha-aware recorded hits match.
  384 sampled double CPU checks pass (shared BVH topology). Analytic material fixture:
  736 paired comparisons, 256 CPU checks, known plane distances, all pass. Not colour
  compositing/lighting parity; sampled production mostly opaque. No validation layer.
- Real Minecraft Vulkan/GL shared RGBA8 image, Win32 handles + two semaphores:
  clear/blit/ownership cycles median wall0.169ms at854x480, 0.178ms at2560x1440;
  65 sampled pixels exact /163 cycles per size. No timed CPU pixel copy. Solid clear
  workload only; not complete backend overhead or long-play lifecycle validation.
- Hardware BLAS initial build15.25ms GPU, TLAS0.033ms, AS388MB; expanded triangles903MB.
  Host-visible large allocation failed; GPU-local buffer +18.9MB staging works.
  Production should keep compact geometry. Dynamic refits/rebuilds still unmeasured.

Evidence: docs/profiles/2026-09-28-rtx-native; tools/analyze-native-replay.py regenerates
summary.json. Main binary dump ignored at run/rtx-native/capture-1790563283292;
SHA-256 manifest committed, not the ~1GB dump. First-down capture/result is separate,
slightly different actor state. Material fixture generator requires only Python.
Guide's RTX chapter and decisionD080 link the report.

## Proposed next experiment

One opt-in frozen full-image Vulkan path with existing optics/material rules and a
shared output texture. Compare fixed-pose images and complete optical-frame cost,
including handoff; measure setup and peak VRAM separately. If gain survives, proceed
to actor BLAS updates and chunk streaming, then production lifecycle/fallback. CPU
tree reuse/refit and sparse material scheduling remain alternatives. This checkpoint
does not establish a full backend, feature parity or an FPS target reached.

## Tooling and checks

- gradlew runClient -PinterstellarRtxProbe: optional Vulkan dependency/source and
  512KiB LWJGL stack. F9 Ctrl+Alt+R records ready frozen ordinary BH/default native
  split-moving selective quad rendering. Shift+M initially disables lensing; Space
  restores it. Ctrl+Alt+I independently runs sharing, even before capture is ready.
- Historical recordings used Ctrl+Alt+B, which also hit Minecraft narrator; final
  shortcut avoids it. Narrator returned to0. Final shortcut/metadata-only edits built;
  no extra capture for those changes. Interop empty-buffer LWJGL fix tested in game.
- Close client normally before tools/rtx-probe/run-native.ps1 -Scene ... . Max a few
  views per process; query buffers retained to exit. Do not rerun heavy capture casually.
- Diagnostic build and clean normal build pass79 tests. Two diagnostic shaders compile;
  ray/colour/interop checks and representative screenshot pass. Normal jar excludes
  experimental interop class and Vulkan bindings. Full optical fixtures not rerun
  (no optical equation changes); automated movement/flicker stays owner-deferred.

## Current client / owner state

Normal client PID8916, exec session3939, run/rtx-native-final-runtime.log, paused with
F10 off. No profiling/RTX JVM flags. Normal F10 reached ready on unchanged owner N8
source at3/81/4, rs0.433. No blocks/config/inventory edits; ordinary ticks advanced
during setup, no time/weather changes. Ticks running20tps; narrator disabled.

All eight fresh owner fields restored and compared exactly: creative, flying=false,
dimension interstellar:arrows, feet16.42080350758872/65/-55.7896552801982,
yaw-18.765259/pitch-5.99997, slot8, health20, original inventory/chainmail. Original
outer window845,449–1715,968 (client854x480) restored. Fresh records:
run/rtx-native-owner-state.txt, run/rtx-native-restored-state.txt and
run/rtx-native-owner-window.json. Do not restore older demo/profile poses.

JDK C:\Portable\jdks\temurin-21.0.12.1; Python py -3 (3.8). Normal helper
run/restart-client.ps1 -Log ... closes identified old client normally before launch.
Diagnostic helper run/restart-native.ps1. Startup confirm595,305 at854x480; gate on
log readiness. All input helpers finished. No need to relaunch for a status question.
