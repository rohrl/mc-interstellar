# Current handoff

Repo: C:\work\code\minecraft\interstellar\interstellar. Branch: codex/world-mesh-reference. Origin: https://github.com/rohrl/mc-interstellar.git. JDK: C:\Portable\jdks\temurin-21.0.12.1. Fabric/Minecraft 1.21.1. Read AGENTS.md. Branches/commits/pushes and autonomous runtime checks authorized. No force pushes or subagents. Preserve worlds, .idea and secrets.

## Current checkpoint — 2026-09-18

Owner visually accepts the native mesh terrain as essentially matching vanilla, and requests mobs. Quality-first: FPS optimization deferred. Step 2 integration precedes packaging. Automated movement/flicker tests deferred to owner; use fixed-frame numeric pairs and occasional milestone images.

F9 M captures native terrain, supported frozen living-entity bodies/equipment and native clouds in one BVH. Capture ~40 s / 6.2 million triangles at render distance12. Camera-centred render distance plus one chunk, full height, seven-million-triangle cap; distance above16 refused. U compares old terrain bounds. M starts unbent; Space enables lensing; E toggles mobs; N compares foreground clouds with the old background-only path; P captures vanilla/zero-bending pairs; K compares native terrain lighting with the previous offset. Existing F10 still uses the older live voxel/column backend.

Fixed terrain lighting: native terrain filters/clamps UV2/256, whereas entities fetch discrete UV2/16 texels. Removed incorrect terrain half-texel offset. Controlled close-up RGB MAE with mobs 0.0188→0.0024; mobs OFF gives 0.0070. All three vanilla references identical. 96 supported bodies, zero wholly omitted bodies; shadow/translucent/eyes feature layers still omitted. See docs/mesh-entities.md for implementation, exact pairs, timings and limits; initial mesh evidence in docs/native-mesh.md.

Cloud checkpoint: native cloud vertices/UV/RGBA and nearest-surface blend now order against terrain/mobs; foreground mode uses a sky-only cube. Escape sphere encloses actual mesh bounds. Daylight mountain RGB MAE 0.0126→0.0071; clear below-cloud night 0.0013→0.0007, identical A/B references. See docs/mesh-clouds.md for exact evidence and limitations. Camera coverage checkpoint now addresses the missing edge terrain; see below.

Coverage checkpoint: identical-reference mountain comparison improves lower-half RGB MAE0.0364→0.0206. Snowfall is active and absent in our renderer, so aggregate error is not comparable with the previous clear-weather cloud test. See docs/mesh-coverage.md. Final build: 47 tests, zero failures/errors, coverage-final-build.log. Runtime shader, 6.18-million-triangle capture, pair/lensed inspection and cleanup/F10 recovery passed in coverage-runtime.log. Final client restarted and captured6,104,960 triangles /97 mobs /2,880 missing sections in36.54s at the wall pose, coverage-final-runtime.log. Diagnostic GPU p95=20.837664 ms at427×240 internal; no FPS claim. No block/time/weather/population edits. Creative flight retained.

Client left F9 M / lensing ON / mobs ON / native light ON / foreground clouds ON, at player (16.5,302,-45.5), yaw0.281/pitch0.91, small 870×519 window. Owner may move/close it. Esc returns to play; F10 uses the older backend. Never open the save in two clients.

## Next work

1. Precipitation is visibly absent in current snowy mountain pairs. Add coherent native rain/snow support, with controlled appearance comparisons. Capture now follows the camera but only loaded chunks are available. Camera guard remains128; broader camera access and daytime/weather coverage still open.
2. Transparent/emissive layers. Mob eye glow, shadows, hurt/flash, glint, nameplates/leashes and special shaders incomplete; non-living/block entities absent. Clouds use native nearest-surface behavior, not general transparency/volumetric transport; only FANCY runtime-certified.
3. Bring the proven representation into live rendering, then optimize. Frozen mesh is not a converged optical oracle: independent curved-mesh checks and chord/escape/fog limits remain open.

Never revive straight-camera terrain overlays: they duplicated the wall (87dd3a1, reverted 0a264b0). Preserve optical core and source recovery. Player-body returning-light, horizon experiences and observer speed remain later plan items.

## Stable foundations / separate work

Selection/recovery/RMB: dc50cbb, docs/source-refresh.md; viewing: docs/stable-exploration.md. Earlier appearance fixes: docs/native-appearance.md, docs/native-fog.md, docs/face-lighting.md, docs/smooth-lighting.md. Optical/voxel diagnostics do not validate mesh appearance.

AA WIP stays 8ad46eb on codex/terrain-antialiasing, excluded/unverified. Source anchor (14,300,14), N64, COM=(16,302,16), r_s8. Final wall local snapshot 11736 opaque, zero unknown, 28 unsupported; source unchanged. Ignored terrain config: enabled=true, distantPrototype=true, renderScale=0.5; code distant default off.

## Efficient runtime

run/stable-init.gradle quick-plays Interstellar Calibration. Redirect logs; gate readiness on joined-the-game. Close identified game normally with CloseMainWindow and wait before relaunching. GUI and Gradle/JDK access require escalated desktop context.

Helpers: C:\Users\karol\Documents\Codex\2026-09-13\i-want-to-create-a-minecraft\work\control-minecraft.ps1 and capture-minecraft.ps1. Held native keys ~120 ms: Esc27/F9=120/F10=121/M77/E69/K75/P80/Space32/B66. run/send-safe-command.ps1 pastes safely/restores clipboard. Fresh random logins may not fly: spectator→teleport→creative retains flight; settle camera before F9. Gate P on fresh mesh-ready, then verify fresh pair-save log. Avoid stale-directory assumptions.

Comparator: java tools/CompareAppearance.java PAIR_DIRECTORY; three-path mode verifies reference identity. Logs/captures/worlds stay ignored. Detailed historical evidence remains in feature docs/progress/decisions.
