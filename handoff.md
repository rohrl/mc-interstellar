# Current handoff

Repo: C:\work\code\minecraft\interstellar\interstellar. Origin: rohrl/mc-interstellar. Current branch: codex/snow-layers. Java: C:\Portable\jdks\temurin-21.0.12.1. Fabric Minecraft 1.21.1. Read AGENTS.md for authorization and token-efficient working rules. GitHub default branch remains codex/bootstrap-observatory; newer work is on stacked development branches. Verify refs after pushing. No license selected.

## Current implementation

F8: Schwarzschild sky lab, static/free-fall frames and horizon tour. F9: frozen bounded textured terrain with diagnostics. F10: normal camera/movement/HUD plus periodic terrain refresh. F12: live pass benchmark. Inspection selects one complete spherical black-hole proxy. Snow layers now supported at true height. All other non-cube models remain unsupported. No physical world deformation or server ray solver.

Detailed controls/results: docs/terrain-prototype.md, docs/live-terrain.md, docs/snow-layers.md. Decisions D024-D026. Latest build/32 tests pass; sampled GPU flat geometry has zero mismatches. Snowy live scene GPU p95=4.244544 ms at 1440p output / 1280x720 internal; frame interval p95=9.6169 ms. Not full curved-surface validation or universal performance certification.

## Saved scene and runtime

World: run/saves/Interstellar Calibration. Preserve owner edits and weather-created snow. Cluster N=63, r_s=7.875, COM=(15.976190476190476,301.9761904761905,16.00793650793651). Select: /interstellar inspect 14 300 14. Reference player: /tp @s 16.5 302 -19.5 0.84516 2.65028. Then F10. Selected source may clear on chunk/mass changes. F10 stops outside capture or below 1.05 r_s. Interactions use straight vanilla aim; observer velocity has no SR transform. Latest client left open in live mode; user exploration optional.

GUI helpers live outside repo under C:\Users\karol\Documents\Codex\2026-09-13\i-want-to-create-a-minecraft\work. Command helper: C:\Users\karol\Documents\Codex\send-minecraft-command.ps1. Gate commands on fresh world-entry log evidence; world-list load can exceed fixed sleeps. Default window 870x519; owner can resize. Screenshots work windowed, not fullscreen. Latest scratch logs: snow-build.log, snow-runtime.log; image snow-check.png, all in C:\Users\karol\Documents\Codex.

## Next work / cautions

Next: secondary-image filtering and independent curved finite-surface checks; broaden model coverage without hiding missing data. Actual player body, terrain inside horizon, maintained clusters, SR potion and radiometry remain unfinished. Disk/entity history deferred. Primary science assumptions are in docs/science.md; extreme near-critical errors documented in docs/critical-rays.md.

Keep native buffers explicitly freed on recapture/close. Preserve GL pack/unpack and PBO isolation: inherited skip rows previously caused a native upload overread. Retain resource reload invalidation for baked UVs. No forced pushes, world resets or unsolicited cleanup. Keep evidence in feature docs and only short checkpoint links here/progress; avoid repeated historical dumps.
