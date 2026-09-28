# Handoff — renderer bottleneck profile, 2026-09-28

## Checkout and authorization

Repo C:\work\code\minecraft\interstellar\interstellar; branch codex/rtx-bottleneck-profile,
based on5642aa7. User authorizes implementation, branches/pushes and autonomous runtime
verification. No subagents. Preserve AA WIP8ad46eb, other branches and owner worlds.
Latest request: profile actual query/integration cost to assess the RTX opportunity.
This is completed; no production RTX backend has been started.

## Delivered

- docs/rtx-bottleneck-profile-2026-09-28.md: normal GPU stages/live timings, invocation
  clocks, measured instrumentation interference, numeric colour equivalence, conditional
  RTX scenarios and revised priorities. Raw data in docs/profiles/2026-09-28-rtx-bottleneck;
  tools/analyze-shader-clocks.py regenerates summary.json using Python3 standard library.
- Initial-ray geometry search accounts for~87% down /76% wall of instrumented invocation
  latency; orbit steps~3% /7%. Coarse whole-query shares86% /75% broadly agree with
  detailed inclusive88% /77%. Detailed clocks add~24% to initial draw time, so these
  are NOT exact GPU elapsed-time or removable-frame fractions. Mask attribution less stable.
- Normal heavy live GPU median22.52ms; median frame24.59ms (~40.7FPS), 1440p output,
  logical720p sharp2x, distance12, VSync/cap120. Source N65 rs3.518;6.26M triangles,
  72 entities/5 block entities. Historical source calibration differs; no before/after claim.
- Frozen down/wall repeats differ<0.15%. Coarse/detail float colour components match
  baseline exactly in both passes/views; no invalid clock rows. Stage/CPU data retained.
- -PinterstellarShaderClocks enables eight separate diagnostic programs, stage/CPU timing;
  normal launch does not register them. Alt+B in ready F9 native streamed/default
  selective split-moving BH mode records clocks. Shift+M initially DISABLES lensing;
  Space restores it. B/Shift+B normal/stage; Ctrl+F12 live stages. Read report controls.
- Existing null-source crash found during setup: horizonView now checks source/camera
  before centre/radius access. No optics equation/default changed. Small HUD completion
  text and stronger nonfinite diagnostic checks added after measurements.
- Guide's RTX chapter, decisionD079, science notes and previous ranking updated.

Build passes79 existing tests. Eight diagnostic shaders compiled in game. Two-view
numeric comparisons and representative screenshots inspected; full optical fixtures
and automated movement/flicker tests not rerun. Normal launch/F10 on owner N8 source
reached ready without renderer errors. Shader JSON formatting compacted with parsed
object equality checks; normal GPU code has no clock calls.

## Recommended next work

1. RTX replay using captured native geometry and actual chord distributions; preserve
   hit/alpha/translucent correctness checks. Measure AS builds/updates and OpenGL sharing
   before committing to a live backend. Prior3.26–3.76x microbenchmark is not an FPS gain.
2. CPU moving-tree reuse/refit (~3.7–3.9ms CPU construction/update here, overlaps GPU).
3. Sparse material scheduling/compaction:0.132% active rays still cost~4.48ms.
4. Optical tables lower priority for these exterior views; critical/close rays unprofiled.

## Current client and owner state

Normal client PID22192, exec session40120, run/rtx-profile-final-runtime.log; paused,
profiling JVM flags absent, F10 off. Original owner source N8 in interstellar:arrows
unchanged. No blocks/config/inventory edits. Ticks running20tps; no time/weather commands.
Restored and verified all eight recorded player fields before and after normal restart:
creative, walking (flying=false), dimension interstellar:arrows,
feet16.42080350758872/65/-55.7896552801982, yaw-18.765259/pitch-5.99997,
slot8, health20, original inventory+chainmail. Exact original outer window bounds
845,449–1715,968 restored (client854x480). These are this turn's fresh owner state;
never restore the older N216/N64 checkpoint poses.

Ignored run/rtx-profile-owner-state.txt, restored-state.txt and rtx-profile-window.json
hold records. Profiling runtime log is run/rtx-clocks-runtime.log; wall/down-live PNGs
are ignored visual checkpoints. Helpers finished. JDK C:\Portable\jdks\temurin-21.0.12.1;
Python via py -3 (3.8 default). Normal relaunch: run/restart-client.ps1 -Log ...; close
identified old client normally and wait before launching. Startup may be minimized:
ShowWindow(handle,9), then dismiss the existing join confirmation at595,305 with the
854x480 client. Gate on log readiness, not arbitrary long sleeps. No new run is needed
merely to explain these results. Detailed evidence and limitations are in the report.
