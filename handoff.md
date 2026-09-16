# Current handoff

Repo: C:\work\code\minecraft\interstellar\interstellar. Origin: rohrl/mc-interstellar. Current branch: codex/source-refresh, based on published curved-terrain-validation (da7771e). Java: C:\Portable\jdks\temurin-21.0.12.1. Fabric Minecraft 1.21.1. Follow AGENTS.md and token-efficient workflow. GitHub default remains codex/bootstrap-observatory. No license selected.

## Current checkpoint

F8: sky lab and free-fall tour. F9: frozen terrain, V flat checks, C independent curved-hit checks. F10: normal controls/HUD with periodic terrain refresh. F12: live benchmark. Snow layers supported; other non-cube models remain unsupported.

Sources now track the inspected block. Relevant changes withdraw stale metadata and automatically rescan, bounded to eight queued jobs / 256 probe queries per tick, with five-tick debounce and 20-tick retry cooldown. Splits follow the anchor fragment. Removed, unloaded, limited and extended states pause F10 without disarming it; complete black-hole metadata resumes rendering. Manual off remains off. Disconnect/world changes clear tracking; respawn replacement guard compiled but not death-tested. Read docs/source-refresh.md and D029.

Fixed owner-reported RMB placement failure: inspection no longer consumes clicks while either hand holds an item. Normal block placement works; inspect with both hands empty or the command. Actual held-block RMB placement and empty-hand RMB inspection were runtime verified.

Build/45 tests pass. Runtime checked edit/removal/restoration, unload/reload, extended-to-black-hole recovery, split/merge, manual-off preservation and zero-mismatch V regression. Earlier independent curved suite: eight runs/768 samples without mismatches/inconclusive rays (docs/curved-terrain-validation.md). Observer range remains 1.05 r_s to 128 blocks with automatic recovery; capture stays 96^3 with outside terrain omitted. Final live 1440p/half-resolution GPU p95=5.066368 ms; frame interval p95=9.423199 ms. Player moved from earlier reference; not a controlled comparison or universal FPS claim.

## Current saved scene

World: run/saves/Interstellar Calibration. Owner reduced the source, then explicitly authorized adding blocks after reporting RMB failure. Filled only air at (14,300,14)..(17,303,17), adding 37 blocks. Current verified source is a complete N=64 cube, COM=(16,302,16), r_s=8, enclosing R=sqrt(12). Existing non-air scene blocks preserved. Actual RMB added one test protrusion at (16,302,13), verified N=65 refresh, then removed it to restore N=64. Current selected anchor from empty-hand inspection is (16,302,14); /interstellar inspect 14 300 14 is also valid.

Reference camera: /tp @s 16.5 302 -19.5 0.84516 2.65028. Client left in F10, 870x519 window; owner may move it. Latest benchmark began at r/r_s=4.80295. No owner testing pending. Preserve later edits and weather-created snow. All earlier temporary test blocks at (13,300,14) and (512,310,512)..(514,312,514) were removed; original anchor restored before the authorized enlargement.

## Separate antialiasing and launcher

AA WIP remains at 8ad46eb on codex/terrain-antialiasing, based on 585835c; excluded here. Still needs fixed-camera comparison, timings, JSON false/template checks and a measured default. External resume notes incorporated in docs/stable-exploration.md.

Double-click Launch Interstellar.cmd to run the current checkout with JAVA_HOME/fallback JDK21; no branch switch. Runtime checks used Gradle directly. Do not open the same world in two clients.

## Evidence and next priority

Logs: refresh-runtime.log (transitions), refresh-final-runtime.log (manual-off/V), placement-runtime.log (final placement, enlargement, timing), refresh-build.log. Images: run/refresh-removed.png, run/refresh-extended.png, run/refresh-unloaded.png, run/placement-final.png. All local/ignored. GUI helpers: C:\Users\karol\Documents\Codex\2026-09-13\i-want-to-create-a-minecraft\work. Prefer local run/send-safe-command.ps1 in the existing STA PowerShell session: it clears chat input and pastes commands while restoring clipboard contents, avoiding Caps Lock/raw typing interference. No execution policy changes. Gate input on fresh world-entry logs; optional run/stable-init.gradle quick-plays the saved world.

Owner's order: stable exploration, wider useful viewing, demo packaging, visual refinement, deeper relativity. NEXT: repeatable self-contained demo setup and coherent source/range HUD. Preserve the owner's scene; further numerical polishing or AA must not displace packaging. Full-world background/foreground access remains a step-2 limitation. Actual player body, terrain crossing, observer speed and radiometry remain unfinished; disk/entity history deferred.

Preserve native buffer lifetime and GL pixel-layout/PBO isolation. No forced pushes, world resets, local .idea changes or unsolicited cleanup. Keep detailed evidence in feature docs.

## Latest override — live background, 2026-09-16

Current branch codex/demo-package, based on dc50cbb source-refresh. Demo packaging was interrupted by the owner's request to replace the grid with the surrounding world. F10 now uses an ordinary, unbent same-screen world background for missing rays; F9 retains the diagnostic grid. Read docs/live-background.md and D030 for evidence and compositing limits. Build/45 tests, runtime shader, near/far visual, F9 transition/V and 1440p timing verified. No blocks changed. Client left in F10 at far player position (16.5,302,-85.5), yaw0.281/pitch0.91, 870x519. AA remains isolated. NEXT: resume repeatable demo packaging; no demo command code exists yet.
