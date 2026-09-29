# Handoff — Live block edits and walking potion, 29 September 2026

## Completed state

Branch **codex/rtx-wormhole-demo**; see Git for final commit. User authorizes normal
commits, pushes and autonomous runtime controls. No agents.

Latest requests implemented:
- Placed/mined blocks get priority over background scenery capture. Lighting
  notifications queue a follow-up without repeatedly invalidating geometry.
  Content changes still invalidate captures; edge neighbours refresh for AO/culling.
- Relativistic Sight lasts **2 minutes**, superseding the interim 1-minute request.
  W/A/S/D walking charges it; sprinting still works. Horizontal travel sets optical
  direction independently of gaze. Walls/passive pushes do not charge. Existing
  15-second ramp to 0.99c, release and normal movement remain.
- F4 controls, HUD, tooltip, command help and user guide updated.

Read D097/D098, **docs/relativistic-sight.md** and
**docs/profiles/2026-09-29-live-edits-and-walking/README.md**. Prior SR optical
implementation is D096 / 955c5e8; no shader equations changed in this fix.

## Verification and limits

- Both final Gradle builds pass 114 tests. Normal jar checked for optional backend
  exclusion; proof copy run/refresh-opengl-only.jar. Final jar is RTX flavour.
- Native RTX placement/mining with stationary camera: 208/198 ms to publication,
  251/241 background chunks. Native OpenGL placement: 175 ms. Corner lighting refresh
  reached ~500 ms. Capture remains whole-column/incremental, not a hard latency bound.
- GL/RTX edited-image mean RGB difference 0.000067636/255; none over 16/255.
- W/A/S/D without sprint verified; normal walking reaches0.99c. Wall stop verified.
  Actual drinking and expiry verified with the interim 60 s duration. Final 2400-tick
  registration was built after owner's 2-minute request; no second full timed run.
- RTX frame p50=8.350 ms at a simple downward pose; GL 13.457 ms at a different pose
  with background capture. No comparative performance regression bound claimed.
  One attempted extra GL benchmark received no inputs/timed out, so has no result.
- Runtime shaders loaded; no ERROR/Exception/shader-link failures. Existing unused
  uniform warnings remain. Broad movement/flicker survey remains owner-deferred.

## Runtime and preservation

Client saved/closed. Only **run/saves/Interstellar Refresh QA 2026-09-29** edited,
a copy of prior Relativity QA. It retains a test glowstone near (452, 211, -173).
Previous/original saves, AA WIP 8ad46eb and .idea untouched.
All original interstellar*.json preferences and options.txt restored byte-for-byte
from run/refresh-backup. Owner's current graphics: 50%/fine/2x/RTX, fullscreen.
Relativity controls remain saved defaults (all on/Gentle/.99c/15s).

JDK C:/Portable/jdks/temurin-21.0.12.1; Python C:/Portable/python-3.11.7/python.exe.
Use explicit UTF-8 and newline='\n' for Python text edits; default codepage is 1252.
Useful ignored files: refresh-init.gradle, refresh-walk-check.ps1,
refresh-accepted-runtime.log, refresh-normal-build.log, refresh-final-build.log,
refresh-backup/. Opt-in edit diagnostics: -Dinterstellar.traceEdits=true.

No new goal is active. Further optimization/feature work follows owner direction.
Preserve the OpenGL-only flavour. Initial streaming still takes time; this change
prioritizes subsequent edits without expanding the existing capture budget.
