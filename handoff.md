# Handoff — World loading and bug bash, 30 September 2026

## Current state

Branch **codex/world-preparation**, based on **76db147**. See Git for the delivery
commit. Branches/pushes and autonomous runtime testing are authorized. No agents.
Read **D100** and **docs/world-loading-2026-09-30.md**; compact evidence and five
screenshots are in **docs/profiles/2026-09-30-world-loading/**.

Implemented:
- Enabled worlds prepare local terrain and one GPU frame after vanilla loading,
  even without items. Continue in background / Escape yields early; disabled entry
  skips preparation. Damage/errors also release the loading screen.
- Native expected-chunk readiness avoids finishing on the first packet batch.
  Local work precedes remote portal capture. Caches survive source-free periods
  and out-of-range views; resource reload recovers automatically.
- Fixed Glowing outlines mirrored vertically, and RTX cloudless-dimension fallback
  from an invalid zero cloud texture. Native cloud texture stays bound when empty.

## Verification

Both final builds pass 117 tests, zero failures/skips; normal jar excludes optional
backend/Vulkan/shaderc entries. New empty survival world: 21.85s / 5.77M triangles.
Final RTX saved-source reload: 17.80s locally; remote work continued to 43.16s.
Nether: 15.83s / 5.70M triangles, actual BH remains RTX. Paired GL/RTX MAE 0.00334/255.
Other BH/combined-image comparisons and timings are in the report; do not present
these as a controlled FPS regression study or universal loading bounds.

Bug bash covered native horizon mining/replacement, sources8→64, first/paired
pearls beside a BH, both portals visible, oldest-mouth relocation, rejected overlap,
distant relocation and actual crossings both ways, R upright, F4/Alt+F12 controls,
all AA levels, mob lift and arrow/trident gravity/capture, corrected Glowing outlines,
actual potion walking W/A/S/D to0.99c and two-minute expiry, F10 recovery, F3+T,
respawn, dimension changes, early loading exit and entry with effects off.

Remaining: rapid command teleports can expose missing geometry until streaming
catches up (already deferred). Single-player/dev GPU only; no exhaustive weather,
leash/fishing, multiplayer, shader-pack or resource-pack certification. Existing
capacity/range limits remain. Further roadmap is unchanged.

## Preservation and runtime

Client saved/closed. Original **New World**, .idea and AA branch **8ad46ebd** untouched.
Only **Interstellar Loading Fresh 2026-** (native name field truncated; seed
-5073909985471291755) and **Interstellar Empty Loading QA** (seed4087341643980156325)
were opened/changed. The first contains test fixtures and a pair at(271,93.028,.5)
and(641,93.028,.5), plus masses in Overworld/Nether; second has empty inventory/no
items. The gameplay world is saved back in the Overworld.

Owner options and interstellar*.json restored byte-for-byte from
**run/loading-study/owner-backup/**. Final build artifact is RTX; accepted normal
copy: **run/loading-study/accepted-opengl.jar**. Both builds/logs, screenshots and
QA launch scripts are under run/loading-study/. Old checkpoint.md there is a
mid-task record superseded by this handoff and the report.

JDK C:/Portable/jdks/temurin-21.0.12.1; Python C:/Portable/python-3.11.7/python.exe.
Use explicit UTF-8 reads and LF writes; Python defaults to cp1252 here.
At 1280×720 GUI2, existing-world experimental confirmation is
window808,305 (image800,274 plus border8,31). Native creation confirmation is
window486,363. Verify chat acknowledgements: input can be dropped while GPU setup
stalls. One early survival fixture teleported into uncleared terrain and died;
respawned, corrected the fixture, and repeated potion testing successfully.
