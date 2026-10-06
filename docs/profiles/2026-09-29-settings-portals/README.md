# Settings and Wormhole Seed checks

> Historical implementation report or proposal. Results, defaults and pending work describe the recorded checkpoint, not necessarily the current mod. See the [current guides and status](../../README.md).

RTX5070Ti, driver616.92; Ryzen5800X3D; Minecraft1.21.1/Fabric/JDK21.
1280×720 output, existing120FPS cap. Copied save:
`Interstellar Overnight Check 2026-09-28`; original saves untouched.

- `aa-*.txt`: same-frame GL/RTX comparisons at the quality stated in each file.
- `item-pair-opaque.txt`: relocated, persisted pair after the opaque-scene RTX fix.
- `timings.txt`: 120 warmup/300 measured frames per run. Vulkan timing excludes
  GL appearance copies/resolve; frame intervals include cap and CPU overhead.
- `lifecycle.txt`: actual menu actions, pair revisions, retired tickets, crossings,
  readiness and rejected placements. The first demo throw predates the final rule
  treating an existing demo pair like any other pair; use revisions4–7 for the
  create/connect/replace acceptance sequence.
- `settings-menu.png`: native F4 UI (the renderer tooltip is open).
- `wormhole-seed.png`: item-created mouth, held seed, RTX active.
- `aa4-gl-rtx-contact.png`: visually inspected 4× comparison.
- `collision.txt`: lower-rim arrival regression and post-fix blocked/clear crossings.

Two early runs labelled `menu-aa-off-half` / `menu-aa-edge-half` missed their UI
click and were still 2×. They are excluded. A later similarly named Edge attempt
remained Off and is also excluded. Included comparison files verify their own
quality state. A first item-pair comparison could not start because opaque-only
scenes failed RTX eligibility; fixed and repeated after reload.

Reproduce: launch the RTX client, enter an optical view, F4 to select quality,
Esc, Ctrl+Alt+F12 for a same-frame pair; F12 runs live timing. The test landing
course is in `interstellar:gameplay`, floor Y100, X238–342/Z210–272. Actual level
throws from (X,102,220.5), yaw/pitch0, landed at (X,110,243.67978). Tested X250.5,
330.5,290.5; temporary obstruction at (250,109,243) was removed after rejection.

The owner's later revision8 pair and full-resolution/fine-path/2× preferences
were backed up before the final AA checks and restored afterwards. Their loaded
void-death state was recovered to a safe creative-flight viewpoint. Original
Calibration and Visual Check saves were not used for these tests.
