# Handoff — gameplay gravity implemented, 2026-09-23

## Current checkpoint

Repo `C:\work\code\minecraft\interstellar\interstellar`, branch `codex/gameplay-gravity`, based on proposal f3b6ae1 / accepted renderer e324e84. The owner approved the gameplay milestone, allows a small performance cost, and requested close mob lift/capture and an inexpensive colour cue. Implementation and verification are complete; see `docs/gameplay-gravity.md` and its compact evidence record. Normal branches/commits/pushes and autonomous runtime tests are authorized. Follow AGENTS.md; no subagents or unrelated resets.

Implemented: shared bounded automatic cluster discovery and edit/split/reload tracking; exact equal-weight COM; normal calibration sqrt(3)/32 per block; finite spherical optical interior for extended sources; automatic F10 source recovery; local server mob attraction/lift/capture; bounded native-collision projectile substeps; stylized mob approach tint. `/interstellar demo gameplay` is the separate new exhibit. `/interstellar demo enter` retains legacy calibration and passive mobs. Players, terrain destruction, true timelike geodesics, physical spectral transport and general emission histories remain outside this milestone. Large-source entity limits are explicit in HUD/status/docs.

## Checks and limits

Build/package:72 unit tests;30 independent affine-vs-angular extended-ray comparisons from interior/exterior observers. Runtime: all programs compile; automatic discovery on fresh join (~1s in the loaded exhibit);64→8→27→64 updates without recapture; mob lift and capture; bent escaping arrow/snowball; vanilla outside-field arrow; preserved arrow damage; fast horizon ordering; embedded-arrow position stable after10 more ticks. Legacy fixture:52,480 optical and84 material checks, zero mismatches/unresolved rays. Screenshots inspected for progression and the capture cue.

Warmed CPU scopes in this exhibit: tracking .0055ms/callback; native mob movement plus gravity .1529ms/world tick; native projectile ticks plus gravity .0202ms/world tick. The first cold projectile call reached5.16ms. Warm tests mostly cover resting/outgoing projectiles; no barrage/multiplayer stress claim. 1440p gameplay wall pass p50:10.499ms atN64,9.001ms atN8. Legacy frozen wall/down:14.113/11.012ms. These are different scenes, not before/after FPS gains or proof of the demanding natural-world floor. Detailed settings/limitations are in the feature doc.

## Runtime and user state

Latest development client PID29816/session25917, log `gameplay-runtime-3.log`; normal launch, profiling disabled. Original user pose freshly recorded in `run/gameplay-return-state.txt` and restored: `interstellar:demo`;41.067393064332435/67.91326917125787/18.85183203406512; yaw111.15211,pitch-4.349992; creative,flying1. Normal world ticking restored, gravity enabled/capture true, F10 ready,854×480 window, client paused. Test walls/tagged entities were cleaned from the new exhibit; its source restored64. Legacy blocks and persistent original demo-return record preserved. Do not use obsolete actor-return-state.txt.

JDK `C:\Portable\jdks\temurin-21.0.12.1`. Launch Interstellar.cmd uses the checkout. Before relaunching, close the identified window normally and wait; never open a second client against a save. GUI helpers `run/control-short.ps1`, `run/send-safe-command.ps1`, `run/size-minecraft.ps1` require desktop escalation. Focus-loss auto-pause is different from the actual menu: confirm state before Escape. F10 can cover menus. Use native held keys and gate on log completion. Simple HUD/doc changes need no screenshots. Old helper scripts/PIDs are not reusable assumptions.

## Next work and preserved branches

Resume targeted GPU measurement, optical-table experiments and moving-tree refit/reuse after this gameplay milestone; see `docs/performance-profile-2026-09-23.md`. The original first four experiments are finished: separate moving roots accepted e324e84; cloud-face trials9815c42, per-actor hierarchy7a168fb and packed bounds e62a71c rejected after measurements, all pushed/preserved. Detailed results: `docs/performance-experiments-1-4.md`. No rejected runtime optimization is included here.

AA WIP8ad46eb on `codex/terrain-antialiasing`; quad A/B49738ba, accepted quad21d33ed and packaging WIP2fe8674 remain preserved. Weather, teleport support and automated movement/flicker tests remain deferred. Known general limits include sampled counter exhaustion, shared-edge/coplanar ordering, special additive/glint/text/particle layers, boat water masks and returning player-body coverage. Step5/deeper relativity still needs discussion; this gameplay approval is not blanket authorization for it.
