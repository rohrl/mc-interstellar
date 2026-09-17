# Current handoff

Repo: C:\work\code\minecraft\interstellar\interstellar. Branch: codex/world-mesh-reference. Origin: https://github.com/rohrl/mc-interstellar.git. JDK: C:\Portable\jdks\temurin-21.0.12.1. Fabric/Minecraft 1.21.1. Read AGENTS.md. Branches/commits/pushes and autonomous runtime checks authorized. No force pushes or subagents. Preserve worlds, .idea and secrets.

## Current checkpoint — 2026-09-17

Owner visually accepts the native mesh terrain as essentially matching vanilla, and requests mobs. Quality-first: FPS optimization deferred. Step 2 integration precedes packaging. Automated movement/flicker tests deferred to owner; use fixed-frame numeric pairs and occasional milestone images.

F9 M captures native terrain plus supported frozen living-entity bodies/equipment in one BVH. Capture ~15 s / 2.5 million triangles, loaded 16×16 chunks over full height, four-million-triangle cap. M starts unbent; Space enables lensing; E toggles mobs; P captures vanilla/zero-bending pairs; K compares native terrain lighting with the previous offset. Existing F10 still uses the older live voxel/column backend.

Fixed terrain lighting: native terrain filters/clamps UV2/256, whereas entities fetch discrete UV2/16 texels. Removed incorrect terrain half-texel offset. Controlled close-up RGB MAE with mobs 0.0188→0.0024; mobs OFF gives 0.0070. All three vanilla references identical. 96 supported bodies, zero wholly omitted bodies; shadow/translucent/eyes feature layers still omitted. See docs/mesh-entities.md for implementation, exact pairs, timings and limits; initial mesh evidence in docs/native-mesh.md.

Final build: 47 tests, zero failures/errors, entity-final-build.log. Runtime shader/mixin startup, two-pose appearance checks, curved ordering, cleanup/F10 reentry and mesh reopening passed in entity-final-runtime.log. Diagnostic GPU p95=7.430944 ms at only 427×240 internal; no FPS claim. No block/time/weather/population edits. Creative flight retained.

Client left F9 M / lensing ON / mobs ON / native light ON, at player (16.5,302,-45.5), yaw0.281/pitch0.91, small 870×519 window. Owner may move/close it. Esc returns to play; F10 uses the older backend. Never open the save in two clients.

## Next work

1. Coherent foreground clouds and transparent/emissive layers. Mob eye glow, shadows, hurt/flash, glint, nameplates/leashes and special shaders incomplete; non-living/block entities absent.
2. Broader coverage/viewing boundary and daytime pairs. Camera guard remains 128 blocks. Filtering/atmosphere incomplete.
3. Bring the proven representation into live rendering, then optimize. Frozen mesh is not a converged optical oracle: independent curved-mesh checks and chord/escape/fog limits remain open.

Never revive straight-camera terrain overlays: they duplicated the wall (87dd3a1, reverted 0a264b0). Preserve optical core and source recovery. Player-body returning-light, horizon experiences and observer speed remain later plan items.

## Stable foundations / separate work

Selection/recovery/RMB: dc50cbb, docs/source-refresh.md; viewing: docs/stable-exploration.md. Earlier appearance fixes: docs/native-appearance.md, docs/native-fog.md, docs/face-lighting.md, docs/smooth-lighting.md. Optical/voxel diagnostics do not validate mesh appearance.

AA WIP stays 8ad46eb on codex/terrain-antialiasing, excluded/unverified. Source anchor (14,300,14), N64, COM=(16,302,16), r_s8. Local snapshot 11697 opaque, zero unknown/unsupported. Ignored terrain config: enabled=true, distantPrototype=true, renderScale=0.5; code distant default off.

## Efficient runtime

run/stable-init.gradle quick-plays Interstellar Calibration. Redirect logs; gate readiness on joined-the-game. Close identified game normally with CloseMainWindow and wait before relaunching. GUI and Gradle/JDK access require escalated desktop context.

Helpers: C:\Users\karol\Documents\Codex\2026-09-13\i-want-to-create-a-minecraft\work\control-minecraft.ps1 and capture-minecraft.ps1. Held native keys ~120 ms: Esc27/F9=120/F10=121/M77/E69/K75/P80/Space32/B66. run/send-safe-command.ps1 pastes safely/restores clipboard. Fresh random logins may not fly: spectator→teleport→creative retains flight; settle camera before F9. Gate P on fresh mesh-ready, then verify fresh pair-save log. Avoid stale-directory assumptions.

Comparator: java tools/CompareAppearance.java PAIR_DIRECTORY; three-path mode verifies reference identity. Logs/captures/worlds stay ignored. Detailed historical evidence remains in feature docs/progress/decisions.
