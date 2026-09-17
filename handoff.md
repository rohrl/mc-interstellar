# Current handoff

Repo: C:\work\code\minecraft\interstellar\interstellar. Branch: codex/world-mesh-reference. Origin: https://github.com/rohrl/mc-interstellar.git. JDK: C:\Portable\jdks\temurin-21.0.12.1. Fabric/Minecraft 1.21.1. Read AGENTS.md. Branches/commits/pushes and autonomous runtime checks authorized. No force pushes or subagents. Preserve worlds, .idea and secrets.

## Current checkpoint — 2026-09-18

Owner visually accepts the native mesh terrain as essentially matching vanilla, and requests mobs. Quality-first: FPS optimization deferred. Step 2 integration precedes packaging. Automated movement/flicker tests deferred to owner; use fixed-frame numeric pairs and occasional milestone images.

F9 M captures native terrain, supported frozen living-entity bodies/equipment and native clouds in one BVH. Capture ~15 s / 2.5 million triangles, loaded 16×16 chunks over full height, four-million-triangle cap. M starts unbent; Space enables lensing; E toggles mobs; N compares foreground clouds with the old background-only path; P captures vanilla/zero-bending pairs; K compares native terrain lighting with the previous offset. Existing F10 still uses the older live voxel/column backend.

Fixed terrain lighting: native terrain filters/clamps UV2/256, whereas entities fetch discrete UV2/16 texels. Removed incorrect terrain half-texel offset. Controlled close-up RGB MAE with mobs 0.0188→0.0024; mobs OFF gives 0.0070. All three vanilla references identical. 96 supported bodies, zero wholly omitted bodies; shadow/translucent/eyes feature layers still omitted. See docs/mesh-entities.md for implementation, exact pairs, timings and limits; initial mesh evidence in docs/native-mesh.md.

Cloud checkpoint: native cloud vertices/UV/RGBA and nearest-surface blend now order against terrain/mobs; foreground mode uses a sky-only cube. Escape sphere encloses actual mesh bounds. Daylight mountain RGB MAE 0.0126→0.0071; clear below-cloud night 0.0013→0.0007, identical A/B references. See docs/mesh-clouds.md for exact evidence and limitations. Largest daylight residual is terrain capture coverage.

Final build: 47 tests, zero failures/errors, cloud-build.log. Runtime shader/mixin startup, above/below-cloud pairs, lensed inspection, cleanup/F10 reentry and mesh reopening passed in cloud-runtime.log / cloud-final-runtime.log. Diagnostic GPU p95=11.507328 ms at only 427×240 internal; no FPS claim. No block/time/weather/population edits. Creative flight retained.

Client left F9 M / lensing ON / mobs ON / native light ON / foreground clouds ON, at player (16.5,302,-45.5), yaw0.281/pitch0.91, small 870×519 window. Final reopened mesh: 2,526,480 triangles, 82 supported mobs, 2,688 cloud triangles, zero missing sections. Owner may move/close it. Esc returns to play; F10 uses the older backend. Never open the save in two clients.

## Next work

1. Broader camera-relative terrain coverage/viewing boundary and daytime pairs. Camera guard remains 128 blocks. Missing capture edges dominate the daylight mountain residual. Preserve coherent ray hits; no straight-camera terrain overlay.
2. Transparent/emissive layers. Mob eye glow, shadows, hurt/flash, glint, nameplates/leashes and special shaders incomplete; non-living/block entities absent. Clouds use native nearest-surface behavior, not general transparency/volumetric transport; only FANCY runtime-certified.
3. Bring the proven representation into live rendering, then optimize. Frozen mesh is not a converged optical oracle: independent curved-mesh checks and chord/escape/fog limits remain open.

Never revive straight-camera terrain overlays: they duplicated the wall (87dd3a1, reverted 0a264b0). Preserve optical core and source recovery. Player-body returning-light, horizon experiences and observer speed remain later plan items.

## Stable foundations / separate work

Selection/recovery/RMB: dc50cbb, docs/source-refresh.md; viewing: docs/stable-exploration.md. Earlier appearance fixes: docs/native-appearance.md, docs/native-fog.md, docs/face-lighting.md, docs/smooth-lighting.md. Optical/voxel diagnostics do not validate mesh appearance.

AA WIP stays 8ad46eb on codex/terrain-antialiasing, excluded/unverified. Source anchor (14,300,14), N64, COM=(16,302,16), r_s8. Final reopened local snapshot 11725 opaque, zero unknown, 28 unsupported. Ignored terrain config: enabled=true, distantPrototype=true, renderScale=0.5; code distant default off.

## Efficient runtime

run/stable-init.gradle quick-plays Interstellar Calibration. Redirect logs; gate readiness on joined-the-game. Close identified game normally with CloseMainWindow and wait before relaunching. GUI and Gradle/JDK access require escalated desktop context.

Helpers: C:\Users\karol\Documents\Codex\2026-09-13\i-want-to-create-a-minecraft\work\control-minecraft.ps1 and capture-minecraft.ps1. Held native keys ~120 ms: Esc27/F9=120/F10=121/M77/E69/K75/P80/Space32/B66. run/send-safe-command.ps1 pastes safely/restores clipboard. Fresh random logins may not fly: spectator→teleport→creative retains flight; settle camera before F9. Gate P on fresh mesh-ready, then verify fresh pair-save log. Avoid stale-directory assumptions.

Comparator: java tools/CompareAppearance.java PAIR_DIRECTORY; three-path mode verifies reference identity. Logs/captures/worlds stay ignored. Detailed historical evidence remains in feature docs/progress/decisions.
