# Demo packaging checkpoint — 2026-09-22

Branch `codex/demo-visual-refinement`, based on performance baseline `79aa1d0`. The owner resumed steps3–4 with an FPS floor; this checkpoint completes the packaging increment, not all visual refinement.

The separate `interstellar:demo` dimension contains a deterministic wall, pillar, terrain, stairs/slabs, tree,64-block source and3 persistent named sheep. Construction checks occupied cells before writing, then rechecks each write, with256-cell/4ms tick slices. A cold chunk load may overrun a slice. Entry and leave persist/restore dimension, position, view, game mode and flying state; inventory is retained. Repeat entry preserves edits and does not spawn duplicate exhibit sheep. View commands provide four fixed cameras. Loading/source/range/AA status is visible in F10.

`gradlew build packageDemo` passed (`demo-package-final-build.log`,53 tests); the archive contains the remapped jar, quickstart, science/performance notes and third-party notices. It excludes Minecraft/Fabric/API/Java. No renderer shader changed in this increment; no new1440p FPS claim is made.

Runtime on the existing calibration save:

- Vanilla showed the custom-dimension experimental notice on both launches; used **Create Backup and Load**, including the verified39,464,479-byte first backup. Quickstart documents this.
- First build completed13,754 exhibit blocks. Visually inspected native screenshot `2026-09-22_15.17.36.png`: correct bent wall, source shadow, foreground pillar, leaves, terrain and new ready/source/range HUD. Terrain viewpoint also exercised.
- Found/fixed source inspection racing the new dimension's player chunk tickets. Entry now waits up to200 ticks for the source chunk. Fresh corrected launch automatically selectedN64,r_s8 at15:22:14; no manual inspection was required. Persistent exhibit reused without rebuilding.
- Leave/re-enter/leave preserved the original-world position `(45.103214895634814,303.49506601944705,-9.682636277880926)` and creative mode. Final return rotation was `(56.08086,-0.29997176)`. Current runtime log `demo-packaging-final-runtime.log`. Do not replace this owner position with the older benchmark camera when finishing work.

## Final refinement follow-up

Native material coverage, AA evaluation, matched FPS acceptance and the [algorithm review](performance-review-2026-09-22.md) are complete for the demonstrated subset. [Detailed checks and limits](material-coverage.md). New exhibits include a bed/chest, stained-glass layers and a contained pool; old saved layouts remain unchanged. Equivalent manually placed material fixtures were checked in the retained test exhibit; the added layout is build-verified, not a newly rebuilt copy of that save.

Development launches now use the stable offline usernameInterstellarDev. Runtime `guarded-material-runtime.log` confirms a normal restart inside interstellar:demo, then `/interstellar demo leave` restores the exact original position/rotation and creative flight recorded above. No saved return records were deleted. Normal installed clients use their account identity.

Final source builds with53 passing unit tests; `packageDemo` includes current material/performance notes. Rain/snow, general teleport support and step5 remain deferred. See [quickstart](demo-quickstart.md) for controls and current limits.
