# Handoff — World preparation, 29 September 2026

## Current state

Branch **codex/world-preparation**, based on30b3bb8. See Git for the delivery commit.
Normal commits/pushes and autonomous runtime testing are authorized. No agents.
Read **D099** and **docs/world-preparation-2026-09-29.md**; compact evidence is in
**docs/profiles/2026-09-29-world-preparation/**.

The owner's new-world wormhole switch-off was fixed-capacity GPU row exhaustion.
The natural world needs8.2–8.5M triangles at12 chunks; the previous fixed arena and
7M-triangle guard could not hold it. D097's all-edits-first scheduling also let
far-away random changes starve initial capture.

Implemented and verified:
- Reuse/shrink/extend chunk rows; bounded on-demand GL texture/Vulkan buffer growth.
  Completed RTX chunk BLAS are retained; descriptors change after queue completion.
- Nearby edits retain priority, then missing portal/local geometry, then distant
  changes. Existing5ms capture slice and live entity cadence retained.
- Capture-local section masks skip only provably zero-output native blocks.
- Live mesh skips unused voxel/height/light data; F9 retains full diagnostics.
- Real loaded geometry gates initial drawing; progress includes that local work.
  Production defaults enable the accepted optimizations, without special launch args.

## Checks and limits

- Both builds pass117 tests. Normal jar has no optional backend/Vulkan/shaderc entries.
- Native zero-output audit passes over17M proposed skips by full capture,28.4M over
  the audit run. GL direct/FBO growth tests preserve49140 float bit patterns each.
- Same-frame legacy snapshot versus placeholders: pixel-identical; paired GL GPU
  medians100.75/98.84ms. No promised steady-FPS gain or broad regression bound.
- Copied owner world: full capture24.52s, passage~29s. Moving1440p case23.84/~28s.
  Final Vulkan-free regenerated world with no diagnostic JVM flags31.69/~33s.
  Earlier verbose/fallback-copy generation test34.56/~39s. Under30s is not universal.
- Actual first/second pearls, master off/on, moving capture, player passage and
  nearby edit publication27.5/173.7ms verified. Dense grown GL/RTX image MAE0.006413/255.
- No shader/optical/AA/range/force-law change. Storage growth increases VRAM and can
  briefly stall; device/pointer capacity limits remain. Detailed costs in report.
- Automated general flicker survey remains owner-deferred. QA did exercise movement
  specifically to verify initial capture across changing windows.

## Runtime / preservation

Client saved/closed. Only named **Interstellar Preparation ...2026-09-29** QA copies
were opened/edited. The original **New World** still has level.dat21:47:39 and
wormhole data21:47:06. Original worlds, .idea and AA8ad46eb are untouched.
All original interstellar*.json and options.txt restored byte-for-byte from
**run/preparation-study/**. Owner graphics:50%,fine,4xAA,RTX,12chunks,fullscreen.
Final build artifact is RTX; normal proof copy:run/preparation-study/accepted-opengl.jar.

JDK C:/Portable/jdks/temurin-21.0.12.1; Python C:/Portable/python-3.11.7/python.exe.
Use explicit UTF-8 and newline='\n' for Python edits. Runtime logs, QA launch init
scripts and original failure evidence are preserved under run/preparation-study/.
The old working-notes.md there is historical; this handoff/report supersede it.

## Follow-up

The preparation bug and measured optimization pass are implemented. Further gains
should distinguish vanilla world generation/packet readiness from capture work.
No distant simplification, disk cache or worker-thread renderer was added.
Existing feature roadmap/deferred physics remains unchanged. Relativistic Sight
still lasts2minutes and charges on ordinary W/A/S/D movement; see D098.
