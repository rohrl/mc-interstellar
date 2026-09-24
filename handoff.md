# Handoff — horizon access and body-image study, 2026-09-24

## Checkout and authorization

Repo C:\work\code\minecraft\interstellar\interstellar; branch codex/horizon-body-study,
based on982d563. Normal implementation, branches, commits/pushes and autonomous testing
are authorized. No subagents. Preserve AA WIP8ad46eb and prior performance branches.

## Result

F10/F9 can approach/cross the horizon. Static optical frame outside1.25r_s changes
smoothly to falling frame by1.05r_s. This is an optical presentation frame, not player
motion or the deferred observer-speed feature. Inside, mass blocks and their selection/
mining marks render at straight-aim positions; other geometry stays curved. Marker-4
identifies mass terrain, +64 its interaction layers; +32 remains returning-only body.
At/below0.1r_s the background is dark but mass editing remains active, including centre.
Source/range availability limits remain. No physical singularity or terrain infall claim.

Full-resolution body-on/off tests include an8x larger horizon, calculated return rays,
an isolated small source and native spyglass zoom. Only strips, no recognisable head.
Body capture now defaults OFF; experimental /interstellar-visuals body true remains.
Hands, other players, mobs and all previously integrated features remain enabled.

## Verification and cost

Build/package,78 tests. Final horizon-verified-runtime.log:52,480 optical comparisons,
156 materials and945 horizon checks pass. The945 comprise810 PG-reference sky rays
and135 central-cutoff samples; max sampled direction chord error0.001669. No final GL
errors or renderer exceptions. Fixed diagnostic bindings of deleted fixture textures.
Actual LMB/RMB confirmed, source512→511→512 refreshes automatically. Final interior
image verifies a mass-only overlay (initial broader overlay wrongly included clouds).
Normal1440p demo frame median19.390ms (~52FPS); huge source at1.1r_s56.810ms (~18FPS).
Not a matched old/new speedup or universal FPS floor. docs/horizon-body-study.md,
docs/profiles/2026-09-24-horizon-body.txt and D076 contain details and limits.

Useful ignored images: run/horizon-interior-final.png, body-large-20-detail.png,
body-isolated-zoom-detail.png and the corresponding full on/off pairs. Body screenshots
use full trace resolution, above demo quality. Zoom comparisons use live ticks; large
zoom includes unrelated cloud motion. Do not use that full-image diff as an accuracy metric.

## Restoration and runtime

Client PID13308, exec51895, horizon-verified-runtime.log; query before assuming current.
Paused, F10 off, normal ticking/arrow firing, source auto, body default off, narrator0.
Window restored854x480. Fresh run/horizon-return-state.txt and horizon-restored-state.txt
match all seven recorded player fields exactly: dimension interstellar:arrows;
feet8.62670489421098 /75.78331551739505 /-25.078440545926302;
yaw26.998535,pitch-7.0500093; creative/flying1; selected slot1 bow;
original nine hotbar stacks and empty offhand; health20. Do not demo leave to restore.
Removed precisely512 temporary mass blocks at0..7/200..207/0..7 and64 at0..3/280..283/0..3.
Temporary centre pocket was inside that removed source. Original source64/owner edits
untouched. Terrain config restored verbatim: renderScale0.5,distantPrototype true, AA2x
by default. User plays between turns: capture fresh state before further GUI work.

## Next and workflow

Original queued work remains targeted GPU measurement, table-assisted optics, then
moving-tree reuse/refit. Huge near-horizon views are a newly measured expensive case;
no further approximation or optimization accepted here. Body remains opt-in unless a
future experiment resolves recognisable detail. General emission history, physical
terrain crossing/destruction and independent observer-speed feature remain deferred.

JDK C:\Portable\jdks\temurin-21.0.12.1; Launch Interstellar.cmd uses this checkout.
Close the identified client normally before restart. Shader changes can take~3minutes
on first compile; cached launches much faster. Gate on Loaded1399 advancements,
wait3s, held click relative595/305, then joined-the-game+3s. F9/F8 STOP F10. Esc600ms.
Use concise logs/numerical pairs, screenshots for major visual checkpoints. Modest
window for setup; explicitly resize for1440p measurements after each restart.
