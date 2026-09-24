# Handoff — illustrated guide and RTX probe, 2026-09-24

## Checkout and authorization

Repo C:\work\code\minecraft\interstellar\interstellar; branch
codex/visual-guide-rtx-probe, based on fb0c827. Normal implementation, branches/pushes
and autonomous runtime verification are authorized. No subagents. Preserve AA WIP
8ad46eb, other branches and owner worlds. Current production Java/resources remain
exactly at fb0c827 (stronger gameplay gravity/course v2).

## Delivered this turn

- docs/visual-guide/interstellar-visual-guide.html: self-contained offline guide,
  18 chapters, about9,200 words,20 SVG diagrams including interactive ray/AA examples,
  three real historical screenshots. Graphics101, physics/model limits, all retained
  rendering optimizations, failed experiments and discoveries. Code links pin fb0c827.
- Source/build instructions in docs/visual-guide/README.md. Node-only build; no packages.
  Desktop/mobile browser rendering and image/diagram inspection, links, SVG text bounds,
  controls and error checks passed. Print styling supplied; no separate PDF generated.
- tools/rtx-probe: standalone Java/LWJGL Vulkan compute microbenchmark, not a mod backend.
  Official Maven jars live only under ignored run/rtx/lib, with pinned checksums in runner.
- docs/rtx-probe-2026-09-24.md plus raw JSONL/setup log. RTX5070Ti driver616.92,
  curved-chord query replay3.26–3.76x faster than simplified software BVH; random4/16
  block segments7.68–13.60x. Six cases,262,144queries each,16dispatch batches,
  12warmup/30alternating measured pairs. All buffers device-local/host-coherent.
-1,572,864 paired hits and768 sampled brute-force double checks. Five triangle-edge
  classification differences, all hardware matches CPU, retained in timed workloads;
  no unexplained classification/distance failures. Not bit-exact equivalence.

Do not apply these speedups to Minecraft FPS. Opaque synthetic fixtures, warm data,
independent chords continuing after hits, no production empty-space certificates,
quad/chunk layout, materials, live updates, integration register pressure or OpenGL
sharing. Hardware builds its own tree. No Vulkan validation layer installed. Build
costs recorded separately; cold wall observations are not a scalability comparison.

Production unchanged, so Gradle/package and optical GPU suites were not rerun.
Standalone javac compile and actual GPU probe passed. D078 records scope/decision.

## Client / saved state

Closed previous client23432 normally for GPU timing; reopened the same saved world
through run/restart-client.ps1 and dismissed its existing join confirmation.
Client PID26904, exec session86101, log run/rtx-resume-runtime.log; left paused.
No new ERROR/Exception/GL_INVALID in startup. No terrain/inventory/config edits.
Owner had changed the scene since the previous checkpoint: source now N216,
centre2/83/3, r_s11.691. Join position18.8877375156/85.4032348458/6.1775771671.
No older state was restored. Capture fresh state before future GUI work; do not
teleport to the older checkpoint pose or shrink the owner's new source.

JDK C:\Portable\jdks\temurin-21.0.12.1. Close identified client normally before
relaunch. run/restart-client.ps1/stable-init.gradle use quickPlaySingleplayer;
Loaded1399 advancements followed by click595/305 has dismissed the confirmation.
Escape needs600ms. External helpers are existing ignored run/control-short.ps1
and run/send-safe-command.ps1. GUI/process enumeration needs elevated sandbox access.

## Next work

User asked for an implementation artifact first and a quick RTX benchmark second;
both are complete. No production Vulkan rewrite was started or committed to.
If RTX becomes the next priority: capture actual native geometry/chord logs, test
cutout/translucency, then sharing/live-update costs before promising a backend.
Original performance options (optical tables, moving-tree reuse/refit) remain.

Gameplay defaults remain strength0.2/cap7*strength, arrow coursev2, body captureoff.
Horizon frame transition1.25→1.05rs, corecutoff0.1rs and interior editing unchanged.
Prior79tests and runtime evidence remain in docs/gameplay-gravity.md; previous optical
and body results in docs/horizon-body-study.md. No new shipping FPS result this turn.
