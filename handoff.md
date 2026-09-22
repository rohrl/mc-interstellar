# Handoff — quad-vertex experiment active, 2026-09-22

## Result and direction

Requested bounded steps3–4 pass and subsequent code/algorithm review are complete. Preserve FPS in matched tests. Step5 needs later discussion; weather, general teleport and automated movement/flicker testing remain deferred. Minor barely noticeable numerical approximations are authorized.

Repo C:\work\code\minecraft\interstellar\interstellar. Follow AGENTS.md. Edits/branches/commits/pushes/runtime control authorized. No force push, subagents, reset of older layouts, or edits to .idea/secrets/owner worlds. Read this checkpoint plus relevant feature docs; avoid dumping old history.

Accepted branch codex/demo-visual-refinement; experiment history codex/material-coverage-experiment. Check git log/status for exact hashes. Preserve original AA WIP codex/terrain-antialiasing8ad46eb and old packaging WIP2fe8674. Earlier rejected experiments remain separate.

The owner explicitly approved pushing ANY branches to https://github.com/rohrl/mc-interstellar.git. Both completed branches were successfully pushed through2843a50; no approval blocker remains. New authorized task: try shared quad vertices. Current branch codex/quad-vertices-experiment, uncommitted prototype. Reference and candidate retain separate GPU terrain storage from one capture; moving actors unchanged. New QuadVertices losslessly packs native pairs, generic MeshTree accepts4 vertices, QuadTerrain maintains chunk arenas. F9 backslash toggles; Shift+backslash captures mode15. Four separate quad shader variants retain both original triangle tests. Build/57 tests pass; GPU/image/FPS acceptance pending. Current client quad-runtime.log, exec89527; startup/fixture helper records original player state in run/quad-return-state.txt. Consume helper completion before other inputs. Prior client exited normally before launch. Do not call the quad trial accepted or claim a speedup yet.

## Delivered and evidence

Separate persistent exhibit, fixed viewpoints, bounded build, saved return, automatic source selection, source/range HUD, packageDemo archive. New-build chest/bed/glass/pool retain older saved layouts. Stable dev username InterstellarDev fixes saved return across restarts; restart/leave verified with exact original pose/flight restored.

Native fluids, ordinary translucent block/entity/item layers, non-living entities and block entities use ordered curved composition. Selective fast probe plus masked full compositor. Grazing self-hit fix passes material tests. Current sharp2xAA retained; alternating/radial diagonals worsen quality and extra photon-edge rays cost6–8ms. Rejected code removed.

Optical max angular cap.08 / nominal4mm chord target at observer radius>=6r_s; original.02/1mm through4r_s, smooth4–6 transition in CPU shader setup. Critical impact-squared within.005 of27/4 keeps angular cap.02.16-block cap, coverage, resolution, AA and live updates retained. Local heuristics are not global error bounds. Expanded close-distance tests caught/fixed two surface-cell boundary errors.

Final build53 unit tests; final-quality-runtime.log has52,480 optical comparisons over680 distinct directions/eight distances, zero mismatches/inconclusive/unresolved;28 material cases zero channel error. Diagnostic variants are not direct certification of every specialized executable. Close pair9886427308615695907 is byte-identical to.02/1mm. Final wall pair11317958034842552915 MAE.000153,0.1021% pixels over8 levels. Final screenshot2026-09-22_17.24.34.png inspected.

Fresh matched live1440p baseline final-baseline-runtime.log: wall frame p50 18.793/18.842ms (~53FPS), down30.625/30.261 (~33). Final final-quality-runtime.log: wall16.959/16.844 (~59), down29.802/29.698 (~34); final down p95 31.411/31.652,p99 32.423/32.452, below fresh baseline. Same source/camera/AA. Actors/time evolve; no universal floor or isolated same-frame speedup claim. Do not headline faster intermediate runs. Full results/rejected trials/limits: docs/material-coverage.md.

Known limits: additive/glint/text/particle layers, coplanar overlays, boat water masks and some special renderers. Water is native surface blending, not physical refraction. First-person returning body remains step5; fine secondary images remain sample-limited. New layout additions are build-verified; equivalent manual material fixtures runtime-checked without rebuilding the retained save.

Post-step4 review complete: docs/performance-review-2026-09-22.md. Prefer native quad pairs preserving triangulation/light while sharing vertices; raw payload reduction1/3 is arithmetic, NOT measured FPS. Later table-assisted orbit integration and conservative compression remain proposals. Packaging/controls in docs/demo-packaging.md and docs/demo-quickstart.md; D063/D064 and latest progress.md checkpoint.

## Runtime and continued work

One client final-quality-runtime.log, long-lived exec70326. GUI helper67840 completed/consumed; no helper issuing inputs. Owner original overworld pose restored:
(45.103214895634814,303.49506601944705,-9.682636277880926), yaw56.08086,pitch-.29997176, creative/flying.854x480,F10 active,N65/r_s8.125,anchor14 300 14. Benchmark camera16.5/302/-45.5 yaw.281 pitch.91/35.91 differs. No owner blocks/time/weather edited.

JDK C:\Portable\jdks\temurin-21.0.12.1. Archive build/distributions/interstellar-demo-0.1.0-dev.zip; Launch Interstellar.cmd runs checkout. Gradle/GUI/git escalations already authorized. Cold shader startup1–3minutes on this driver; wait Loaded1399 advancements, then click Create Backup and Load at window-relative270,305 in854x480. Close normally before relaunch; never two clients on a save.

Ignored helpers run/restart-client.ps1, control-short.ps1, send-safe-command.ps1, size-minecraft.ps1 (-Restore), measure-pass.ps1 (-Key123 for live; defaultB frozen). Teleport pose AFTER resizing and verify logged pose. Gate readiness/benchmarks by logs; no builds during timings. setup-live/setup-comparison use historical pause heuristics; do not blindly reuse on an active session.

F9 Shift+M streamed capture, Space lensing after ready, C fixtures. [ max angular cap, ] tolerance, Shift+[ reference.02/1mm; Z full/selective, Shift+Z pair; Shift+P full-resolution4ray/fine reference. Rejected AA controls removed. Java tools CompareAppearance.java and CompareSecondaryImages.java operate on ignored run/interstellar-captures/pair-*; the latter needs source16.0076923077,302.0384615385,15.9923076923,radius8.125.
