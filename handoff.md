# Handoff — first-person rendering restored, 2026-09-24

## Checkout and current result

Repo `C:\work\code\minecraft\interstellar\interstellar`; branch `codex/gameplay-arrow-exhibit`, based on a556251. Latest request: fix missing first-person hands/items and explain remaining Minecraft coverage. Implementation and verification are complete. Normal branches/commits/pushes and autonomous testing are authorized. Read AGENTS.md; no subagents or unrelated resets. Preserve prior AA/performance branches.

The old HUD-time world composite covered vanilla hands. Scene/HUD work is now separate; GameRendererMixin calls LiveTerrain.renderWorld immediately after WorldRenderer.render, before vanilla hand rendering. Hands, offhand, item-use animation and camera overlays then follow their normal path. F1 hides HUD/hands but keeps lensing. Separate straight-ray glowing outlines are suppressed only when a lensed image was actually drawn, preserving normal fallback. No optical shader/sampling/AA change or duplicate hand/lensing pass. See D074 and docs/minecraft-coverage.md for current gaps; older coverage lists are historical.

## Checks

Build/package successful;75 existing unit tests have zero failures (unchanged test task reused). Final runtime shader/mixin startup and visual checks: empty main hand + offhand bow, drawn bow, F1, F9 frozen preview. No broad optical/movement rerun for rendering-order-only change. 1440p arrow-course GPU p50/p95/p99=17.985/18.939/19.295ms; sampled frame intervals18.520/20.123/20.818ms. 120 warmup/300 samples, half-scale AA2. Short scene check, not an isolated regression measurement or universal FPS guarantee. Log first-person-final-runtime.log; images run/first-person-*.png; build run/first-person-build.log.

The final startup also logged a player-advancement JSON parse error (null object) for27492c24-0356-36dd-99c9-dc34bebce6cd. No renderer exception. Native autosave subsequently wrote valid JSON (DataVersion3955). Cause not established; no manual save-file edits. Track if it recurs; do not claim all runtime logs are error-free.

## Runtime and user state

Final client PID26004, exec session1020, log first-person-final-runtime.log. Query processes before assuming it is still valid. Left paused, F10 off, normal ticking, in interstellar:arrows. Fresh original and restored state records: run/first-person-return-state.txt and run/first-person-restored-state.txt; exact match for dimension, pose, flight, selected slot and all inventory entries.

Pose -8.284012400041416 /100.11934391327323 /-25.681458146811625; yaw-2.5497742,pitch59.900074; creative/flying1; selected slot1 bow, offhand empty. Window restored854x480, outer870x519 at951/353. Owner had left the old client at the main menu before this turn; these were the saved player coordinates loaded on startup. Do not use older arrow/actor/gameplay return poses. No builds changed. User gameplay source was previously moved to15/91/-14; never reset it to old fixture coordinates.

JDK C:\Portable\jdks\temurin-21.0.12.1. Launch Interstellar.cmd uses checkout. Close the identified client normally and wait before relaunch. run/restart-client.ps1 -Log ... handles this; quickplay experimental confirmation needs held click relative595/305. Distinguish actual pause from focus-loss pause. GUI helpers need desktop escalation. Capture fresh player/window state next turn, because the owner plays between turns.

## Previous feature milestone and next plan

Completed a556251: twice gravity reach, separate4-station arrow course (loop491.9degrees, flyby, reversal, capture), stronger stylized mob red/dim cue and fixed weak-source sky rings. docs/gameplay-gravity.md, D073, docs/profiles/2026-09-24-arrow-course.txt preserve evidence. Reference source64 blocks at0/80/0 through3/83/3 is in interstellar:arrows; keep existing gameplay/legacy exhibits intact. /interstellar demo arrows and demo view arrows enter/show course; arrows on|off|once|setup controls it; demo leave returns to saved prior world. Existing demo return record is preserved.

Next remains targeted GPU measurement of integration/traversal, table-assisted optical integration, then moving-tree reuse/refit. Original4 experiments complete: separate roots e324e84 accepted; cloud quads9815c42, actor hierarchy7a168fb and packed bounds e62a71c rejected/preserved. AA WIP8ad46eb stays on codex/terrain-antialiasing. docs/performance-experiments-1-4.md and docs/performance-profile-2026-09-23.md hold rankings/evidence.

Weather, general teleports, automated movement/flicker tests and broader step5 relativity remain deferred. Gravity excludes players/mounted groups; physical delayed-light/horizon slowing and terrain destruction remain absent. The current coverage document distinguishes visuals, gameplay and approximation limits.
