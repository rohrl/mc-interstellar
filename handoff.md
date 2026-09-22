# Handoff — quad vertices adopted, 2026-09-22

## Direction and authorization

Requested bounded steps3–4 and subsequent code/algorithm review are complete for the documented subset. The following quad experiment is now adopted. Owner explicitly accepts improving the slowest view at the cost of a small easier-view regression; do not reapply the superseded strict all-view no-regression gate. Step5 needs later discussion. Weather, general teleport and automated movement/flicker testing remain deferred.

Repo C:\work\code\minecraft\interstellar\interstellar. Follow AGENTS.md. Edits, runtime control, branches, commits and pushes of ANY branches to https://github.com/rohrl/mc-interstellar.git are authorized. No push approval blocker remains. No force push, subagents, resetting older exhibit layouts, or changes to .idea/secrets. Original AA WIP codex/terrain-antialiasing8ad46eb and packaging WIP2fe8674 remain preserved.

Accepted branches codex/quad-vertices and codex/demo-visual-refinement; see git status/log for final hash. Original parallel storage A/B is separately pushed at codex/quad-vertices-experiment49738ba. Earlier material experiment remains on its own branch. Do not restore parallel comparison arenas into production.

## Quad result

Native pairs(0,1,2),(2,3,0) share four full-precision vertices, retaining both triangle intersections and original diagonal shading. Four quads per leaf. StreamingTerrain retains one4092-wide vertex arena and one compact node arena; original triangle and expanded-node copies removed. Moving actors/clouds retain triangle storage. Actual storage format selects the quad shader even under alternative AA/bending/diagnostic settings; prevents accidental triangle decoding of quad offsets.

Eight-quad leaves did not help. Four-quad frozen matched tests save4.3% GPU median in the heavy downward view; wall medians stay close and some tails worsen. Owner explicitly accepts that tradeoff. Original/quad wall, down and close images match exactly. Final actual F10 medians~57–60FPS wall/~34–35FPS down at1440p, half scale, sharp2xAA. Actors/time evolve; mixed live/tail changes are recorded, not claimed as isolated4% live gain. docs/quad-vertices.md has all timings, pair IDs and limitations.

Final build/package57 tests; final quad diagnostic52,480 optical comparisons with zero mismatches/inconclusive/unresolved;28 material cases with zero error. General/optimized and full/selective close pairs are pixel-identical. Zero-bending/vanilla MAE.00090319,0.8355% over8 levels; contact sheets inspected. Final demo screenshot2026-09-22_18.51.24.png inspected. Guarded temporary mass at demo4,80,0 refreshes64→65→64 without reload; removed in finally. No remaining test block. Raw vertex payload is one third smaller; terrain arena allocation arithmetic saves597.6MiB including the removed expanded-node copy, not measured total VRAM.

Production initial heavy capture36.264s, parallel reference37.691s; historical baseline34.686–35.173s. Exhibit5.081s. Not controlled loading-time comparisons; owner explicitly says no extra run just to measure loading time.

## Established behavior and remaining work

Persistent separate exhibit, fixed views, saved return, automatic source selection, source/range HUD and packageDemo archive remain. Native fluids, ordinary translucency, live mobs, block entities and nonliving entities participate. Keep current sharp2xAA. Optical cap.08/nominal4mm at>=6r_s, original.02/1mm through4r_s with smooth transition; near-critical angular guard and16-block cap unchanged. No new optical approximation in quad storage.

Known limits: additive/glint/text/particle layers, coplanar overlays, boat water masks, first-person returning body, and sample-limited fine secondary images. Water uses native blending, not physical refraction. Details: docs/material-coverage.md. Next bounded proposal: share actual intersection work for rectangular planar faces, retain both native triangles for other geometry and native diagonal interpolation. Not implemented. Table-assisted integration remains a later proposal; see docs/performance-review-2026-09-22.md. No further action is required for this accepted quad iteration.

## Runtime and launch

One final client quad-production-runtime.log, exec9873. All GUI helpers completed; no helper issuing input. Last controlled state: demo dimension, wall view(2,80.38,-54), yaw0,pitch0, creative flight,854x480,F10 active,N64/r_s8. Owner started exploring afterward; do not overwrite their newer pose. They had entered the exhibit during the experiment. Their existing saved return record was preserved by returning to the demo dimension before calling demo enter. No owner blocks/time/weather were changed; only the documented temporary source probe was added and removed.

Launch Interstellar.cmd runs the current checkout. In a loaded world: /interstellar demo enter, wait ready, F10. Views: /interstellar demo view wall|side|close|terrain. /interstellar demo leave restores the saved return. Archive build/distributions/interstellar-demo-0.1.0-dev.zip. JDK C:\Portable\jdks\temurin-21.0.12.1. Stable offline username InterstellarDev preserves return identity.

Runtime helpers are ignored under run/. Close the identified client normally before any restart; never two clients on a save. Gate startup on Loaded1399 advancements, then the experimental notice at relative270,305 in854x480. Verify dimension and source before assuming a benchmark pose; owner may now be in the demo. Resize before teleport; wait3s after teleport before F9 to let camera motion settle. A120ms Escape sometimes misses; use600ms and confirm a harmless chat query before issuing dependent commands. RecordTiming is safe as a PowerShell helper name; Measure collides with a built-in alias. Gate benchmarks by log completion, with no builds during timings. No repeated screenshots for simple text changes; fixed same-frame pairs plus occasional contact sheets suffice.
