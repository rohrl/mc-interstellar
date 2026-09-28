# Handoff — frozen RTX full-image checkpoint, 2026-09-28

## Checkout and scope

Repo C:\work\code\minecraft\interstellar\interstellar; branch codex/rtx-full-image,
based on92aaa3d. Owner authorizes branches/pushes/runtime verification. Preserve
AA WIP8ad46eb and other branches/worlds. No subagents. Latest implementation approval:
full-image RTX experiment, preserving OpenGL fallback and Vulkan-free build options.
This frozen milestone is complete; live-world RTX work is the next proposal.

Side request completed: docs/wormholes-feasibility.md, analysis only. Do not implement
wormholes without a later request. Stationary Ellis model, two-region rendering and
continuous camera transit proposed; physics limits and engineering costs documented.

## Results and implementation

Read docs/rtx-full-image-2026-09-28.md; small evidence in
 docs/profiles/2026-09-28-rtx-image. Summary tool: tools/analyze-full-image.py.
1440p completed optical-frame medians: down22.132→3.543ms (6.25x),
wall9.826→3.417ms (2.88x). Vulkan GPU3.025/2.914ms confirms real work. GL-only timers
can omit external Vulkan work; use paired comparison with both APIs completed.
Not live Minecraft FPS. Two views/two resolutions; RGB8 agrees very closely.
1440p4/8 pixels above16/255, max36, tiny platform patches not yet explained.

Frozen scene6,270,450 triangles; N65 rs3.518; eye16.5/303.62/-45.5,
yaw.281 pitch35.91/.91. Source unchanged. Existing capture29.91s, extra setup~3–4s
(coarse log). Expanded geometry903MB +AS388MB; AS build14.12ms GPU. Board memory
~1.4GiB additional with both renderers retained; sampled, not guaranteed peak.

FrozenWorldBackend interface has no Vulkan types. Optional src/rtx/java generates
compute GLSL from production terrain_shared.glsl and uses hardware ray queries.
Shared RGBA32F output/two semaphores feed existing GL AA fold/resolve. No per-frame
CPU image copy. Prototype has one frame in flight, expanded disk export and no live
updates; software empty-region cache is not replicated. Retains material shader.

-PinterstellarRtxImage adds optional sources/Vulkan/shaderc. F9 Ctrl+Alt+V initializes
or toggles, Ctrl+Alt+P compares. Ready frozen ordinary exterior BH/default selective
native rendering only. Shift+M initially disables lensing; Space restores it. Arrows/L
retain backend. Other settings/resize close it. B timing blocked while RTX active.
Normal F10/extended/horizon variants remain OpenGL. Production packaging is unfinished;
this uses Vulkan beside GL, not VulkanMod. Failure/device-loss recovery not fully tested.

## Checks and next step

Diagnostic build + clean normal build pass79 tests. Material fixture passes736 paired
queries/256 CPU checks after Probe refactor. Runtime shader/image/view changes, toggles,
resize/reinitialization pass. Normal jar excludes optional RTX classes and Vulkan.
Final nonvisual guard/constructor cleanup compiled and fixture-checked after images.
Full optical fixtures not rerun (equations unchanged); movement/flicker owner-deferred.

Proposed next: static terrain resident, actor AS updates/refits and relevant texture
updates. Measure whole live frames, update spikes, memory and moving-material fidelity.
Then chunk streaming/replacement, optical variants and robust lifecycle/distribution.
Do not extrapolate frozen speedups directly into gameplay FPS. Diagnose tiny platform
image differences if they persist in further cases. Keep OpenGL path throughout.

## Current client / owner state

Normal client PID9888, exec session52112, run/rtx-image-restored-runtime.log. Paused,
F10 off, no diagnostic JVM flags, narrator0. Normal F10 reached ready on ownerN8
extended source at3/81/4, rs.433; capture5.13s. Ticks running20tps. No blocks/config/
inventory edits; ordinary world ticks advanced during setup, no time/weather commands.

All eight fresh saved fields compared exactly: creative, flying=false,
interstellar:arrows, feet16.42080350758872/65/-55.7896552801982,
yaw-18.765259/pitch-5.99997, slot8, health20, original inventory/chainmail.
Fresh original outer window1241,579–2111,1098 (854x480 client) restored.
Records run/rtx-image-owner-state.txt, run/rtx-image-restored-state.txt,
run/rtx-image-owner-window.json. Do not use older pose/window backups.

JDK C:\Portable\jdks\temurin-21.0.12.1; Python py -3 (3.8, noPIL).
Normal launcher run/restart-client.ps1 -Log ...; optional run/restart-image.ps1.
Close identified client normally and wait before any restart. Current tests complete;
no input helpers running. Final images/timings in run/rtx-image compare1790567011933,
7028793,7111793,7117386 (full names have common179056 prefix); measured geometry
scene-1790567007118. First exploratory compare1790566582751 lacks Vulkan timestamp
verification and is excluded from archived final table. Large captures stay ignored.
