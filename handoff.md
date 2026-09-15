# Current handoff

Repo: C:\work\code\minecraft\interstellar\interstellar. Origin: rohrl/mc-interstellar. Current branch: codex/curved-terrain-validation, based on published stable-exploration (4440311). Java: C:\Portable\jdks\temurin-21.0.12.1. Fabric Minecraft 1.21.1. Read AGENTS.md for authorization and token-efficient workflow. GitHub default remains codex/bootstrap-observatory. No license selected.

## Current checkpoint

F8: Schwarzschild sky lab and free-fall tour. F9: frozen bounded textured terrain and diagnostics. F10: normal controls/HUD with periodic terrain refresh. F12: live benchmark. Snow heights supported; other non-cube models remain unsupported.

Completed source selection uses nearby chunk dependencies plus a one-block connectivity shell; unrelated chunk activity no longer clears it. In-flight inspection retains conservative world epochs. F10 stays armed outside 1.05 r_s to 128-block viewing limits, displays normal-view pause and automatically resumes on return. Relevant source edits, world changes and resource reload still invalidate it.

Observer access extends to 128 blocks, but scene data is still source-centred 96^3; outside terrain/foreground occluders are omitted. Shader chords enter the data box from outside. Detailed semantics, checks, timings and limitations: docs/stable-exploration.md, D027. Build/40 tests pass. F9 C now compares curved finite hit cells against independent affine DP5(4) integration and cuboid slabs with paired refinement. Eight runs/768 samples had zero mismatches or inconclusive rays; final near/far counter checks and V regression passed. See docs/curved-terrain-validation.md and D028. Final live 1440p/half-resolution GPU p95=5.132864 ms. Sampled evidence, not universal accuracy or FPS certification.

## Antialiasing preserved separately

codex/terrain-antialiasing at 8ad46eb contains the earlier uncommitted two-sample AA work, based on 585835c. No AA included in stable-exploration. Earlier build passed but image/timing comparison is unverified. Resume tasks are in docs/stable-exploration.md; external stable-exploration-resume.md and antialiasing-resume.md have been incorporated here. Do not describe AA as validated or enable it by default without measurements.

## Saved scene and runtime

World: run/saves/Interstellar Calibration. Preserve owner edits and weather-created snow. Cluster N=63, r_s=7.875, COM=(15.976190476190476,301.9761904761905,16.00793650793651). Select: /interstellar inspect 14 300 14. Reference: /tp @s 16.5 302 -19.5 0.84516 2.65028. F10 left active at this position in an 870x519 window. Test block (22,306,20) restored to air; original cluster preserved. No owner testing pending.

Launcher: double-click Launch Interstellar.cmd in the repository (current checkout, no branch switch). Uses JAVA_HOME with fallback to this machine's JDK21. Launcher code reviewed; runtime checks used Gradle directly. Do not launch another client on the same world.

GUI helper directory: C:\Users\karol\Documents\Codex\2026-09-13\i-want-to-create-a-minecraft\work. Command helper: C:\Users\karol\Documents\Codex\send-minecraft-command.ps1. Gate inputs on fresh world-entry evidence. Local ignored final logs: curved-final-runtime.log, curved-build.log; initial eight-run suite: curved-runtime.log. Image: run/curved-check.png. Earlier stability evidence remains in range3-runtime.log. Optional run/stable-init.gradle launches the saved world directly; do not commit run files.

## Next work / cautions

Owner reaffirmed delivery order in plan.md: (1) stable exploration, (2) wider useful viewing, (3) demo packaging, (4) visual refinement, (5) deeper relativity. NEXT: finish bounded automatic source metadata refresh after relevant changes, then repeatable demo packaging and source/range HUD. Additional numerical polishing, broader model work and AA must not displace these priorities. Source invalidation remains conservative within nearby chunks; no automatic reinspection or maintained cluster index. Actual player body, terrain inside horizon, SR potion and radiometry remain unfinished. Disk/entity history deferred. Science assumptions: docs/science.md; critical-ray limits: docs/critical-rays.md.

Preserve native buffer lifetime and GL pixel-layout/PBO isolation. No forced pushes, world resets, local .idea edits or unsolicited cleanup. Keep detailed evidence in feature docs and this handoff short.
