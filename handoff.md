# Handoff — wider gravity, arrow course and sky-ring fix, 2026-09-24

## Current checkout and outcome

Repo `C:\work\code\minecraft\interstellar\interstellar`; branch `codex/gameplay-arrow-exhibit`, based on gameplay implementation3bb4b67. Latest owner request is complete: twice the local influence radius, continuously fired illustrative arrows, investigation of small-source rings, clearer horizon cue and recap of the next plan. Branches/commits/pushes and autonomous testing are authorized. Read AGENTS.md; no subagents or unrelated resets. Preserve old AA/performance branches.

`/interstellar demo arrows` now enters a separate calibrated exhibit, with4 colour-marked automatic dispensers: orange transient loop, cyan flyby, magenta reversal, red capture. `/interstellar demo view arrows` restores the close view. `arrows on|off|once|setup` controls the course; `demo leave` returns to the saved world/pose. Launches are calibrated rather than vanilla random dispenser shots, but native gravity, drag and collisions remain. One launch/second,12-arrow cap,8-second expiry, stale demo arrows removed on reload. Existing gameplay and legacy exhibits remain separate.

The owner moved the gameplay source to15/91/-14 and built additional overworld sources. Do NOT reset these to old fixture coordinates. The first trial's8 station blocks were removed from gameplay; final stations exist only in the new arrow dimension. Its source is restored64 blocks at0/80/0 through3/83/3.

## Correctness and performance evidence

75 tests pass, including independent continuous-force arrow references and an analytic finite-wall regression. Actual native arrows:491.875° transient loop (closest4.225), flyby closest3.867, reversal captured at14ticks, direct capture12ticks. Wider influence verified on a normally ticking sheep30 blocks away with walking-speed attribute0: inward vx−.002346. NoAI debug mobs remain immobilized and are not valid fixtures for that movement check.

The one/two-block concentric rings were numerical sky leaks: an outgoing inverse-radius step could overshoot infinity before intersecting finite terrain. Limit outgoing delta-u to half u in extended-source programs only. The first incoming-only clamp was rejected; do not restore it. New adaptive/conservative pair RGB MAE.00005205, >8/255 pixels.0461%; rings removed. 1440p trace-half/AA2 median:11.779ms flawed old adaptive,11.825ms corrected,27.988ms fixed reference. No material slowdown measured; black-hole path unchanged. Expanded3-r_s mob red/dim cue inspected; it remains stylized, with no emission history or physical horizon slowdown.

Arrow-course native projectile scope mean.0405ms/world tick including vanilla work, max.7243ms. Live1440p pass on/off:17.727/17.310ms medians; frame intervals18.060/17.680ms. Short sequential checks, not precise isolated overhead or a worst-case FPS guarantee. Feature details: `docs/gameplay-gravity.md`; compact evidence: `docs/profiles/2026-09-24-arrow-course.txt`. Full logs/pairs/screenshots remain ignored under `arrow-runtime-2.log` and `run/`. Latest accepted image pair is `run/interstellar-captures/pair-17506819866574617223`.

## Runtime and actual user state

Client PID28772, current task exec session89030, log `arrow-runtime-2.log`; query current processes before assuming either is still valid. Left paused in `minecraft:overworld`, F10 off, normal world ticking restored, arrow course enabled for the next visit. Exact restored pose:−67.15136587838147 /192.11873297898615 /−42.595616122740104; yaw−15.582153,pitch21.450012; creative,flying1. Window854×480, outer870×519 at845/449. Fresh record `run/arrow-return-state.txt`; ignore older gameplay/actor return poses. Temporary sheep and earlier station trial cleaned. Do not change the owner's source builds or inventory.

JDK `C:\Portable\jdks\temurin-21.0.12.1`; Launch Interstellar.cmd uses checkout. Close the identified client normally and wait before launching another. `run/restart-client.ps1 -Log ...` checks this and quick-plays the calibration save; experimental confirmation still needs the held native click at window-relative595/305. Distinguish actual pause menu from focus-loss pause. GUI helpers require desktop escalation. F3+T shader recompilation takes about2minutes; gate on the new body-diagnostic registration then reopen F9/F10. Do not run builds/image analysis during measured samples. Simple text changes need no extra screenshots/relaunch.

## Next planned work

Resume targeted GPU measurement to isolate remaining integration/traversal costs, then table-assisted optical integration, then moving-tree reuse/refit. The original first4 performance experiments are finished; results/keep-reject decisions remain in `docs/performance-experiments-1-4.md` and `docs/performance-profile-2026-09-23.md`. Separate roots e324e84 accepted; cloud quads9815c42, actor hierarchy7a168fb and packed bounds e62a71c rejected/preserved. AA WIP8ad46eb on codex/terrain-antialiasing remains separate.

Physical delayed-light/horizon slowing, player gravity, terrain destruction, weather, general teleport support, automated movement/flicker tests and broader step5 relativity remain deferred. Explain the received-image versus ordinary-time mob-motion distinction if asked; do not present the tint as physical redshift. Existing special-layer/material and sampled-ray limitations still apply.
