# Current handoff — 2026-09-22

## Active task (NOT complete)

Owner requests completion through step4 (demo packaging + visual/model refinement) WITHOUT reducing current FPS, then another code/algorithm performance review. Minor barely noticeable approximations are allowed for worthwhile gains. Step5 deeper relativity needs later discussion. Weather and general teleport remain deferred. Do not stop after packaging or claim performance parity from the simpler demo dimension.

Repo C:\work\code\minecraft\interstellar\interstellar. Read AGENTS.md; existing summaries/evidence suffice for sections already read. Edits/branches/commits/pushes/runtime control authorized. No force push, subagents, automated movement/flicker testing, or changes to .idea/secrets/owner worlds. Keep updates concise/frequent. Preserve original AA WIP codex/terrain-antialiasing8ad46eb and old packaging WIP2fe8674.

## Branches and status

- Verified packaging is pushed: codex/demo-visual-refinement,24958d8, based on79aa1d0 (renderer unchanged). docs/demo-packaging.md and docs/demo-quickstart.md. Separate persistent exhibit, safe return, automatic source selection after chunk tickets load, four camera commands, HUD progress/source/range, packageDemo archive.53 tests/build/package/runtime checks passed.
- Current branch codex/material-coverage-experiment, pushed fa69f0d. It preserves the material/curvature work and rejected extra-ray AA experiment. Current uncommitted changes remove extra-ray AA, trial radial two-ray sampling, add a stable dev username and new-build exhibit displays. These need cleanup/final verification before promoting to demo branch.
- Defaults in the experiment are STILL angular .02 / curvature factor1, so its default material path is slower. Do not release as the final candidate until the FPS gate passes. Runtime experiments use .04/.08 and factors2/4 via F9 controls.
- Detailed evidence, timings and limits: docs/material-coverage.md. Do not repeat all logs in the handoff.

## Implemented coverage and validation

Native fluids (water alpha/lava opaque), translucent block and normal entity/item layers, non-living entities, and block entities now captured through vanilla renderers. Native chest/bed/boat/item, stacked stained glass and contained water visually inspected in demo. No unbent overlays. Transparent hits resume the same optical chord; nearest-cloud semantics preserved and ordered with glass/water.32-layer cap, residual transmission threshold .001. Special additive effects/glint, text/sign lettering, particles, coplanar overlays and boat water masks remain unsupported; weather deferred. Broadening this subset is not universal Minecraft compatibility.

28 analytic GPU material cases pass with zero channel error, including grazing incidence at coordinate512; normal offset avoids repeated self hits (normally .1–.3mm).26240 optical sampled checks pass at .04/.08, including .04 with2/4mm and .08 with4mm. These are repeated settings/layout checks over340 distinct directions, not universal validation or direct Diagnostic=0 certification.

Selective material rendering: cheap first pass marks translucent-hit rays in alpha; copy to separate mask; full compositor retraces only marked rays. Full/selective wall/down PNGs are byte-identical. Helps wall; little gain/slight cost down. Existing two-ray AA and resolution retained.

Curvature experiment: nominal1/2/4mm local sagitta target,16-block max, .04/.08 angular cap, near-critical impact-squared within.005 of27/4 stays at .02. At widened cap, reduce minimum chord to .05 so old .45 minimum cannot violate the chosen target. .08 gave no gain at1mm; .08+4mm is NOW being tested. .04+4mm frozen wall~16.2ms GPU, down~30.6ms GPU; original fresh live baseline20.55/32.145ms GPU. Need final controlled old/new and LIVE acceptance; older79aa1d0 final was17.934/28.636ms GPU, frame18.770/29.796ms. Do not conflate these runs.

AA trials: alternating diagonals and radial-diagonal alignment slightly WORSEN near-critical-region metrics; reject. Four-ray narrow photon-edge pass improves edge error but adds~8ms wall/~6ms down; rejected and already removed in working tree. Keep original AA. Stronger F9 Shift+P reference now4 rays/full resolution/fine path/.02 angular cap/factor1. tools/CompareSecondaryImages.java adds geometric annulus and photon-edge metrics/native crops to avoid whole-image averages hiding ring regressions. Record radial rejection, remove its code before final candidate.

## Current runtime / immediate next actions

One client radial-aa-runtime.log, exec74045. F9 frozen streamed natural-world scene, sourceN65,6260820 terrain triangles. Long helper58625 is testing .08/factor4, selective/full wall/down and same-frame .02 reference pairs; consume its completion before more GUI input. Started16:52:10; C fixture passed26240+28. Helper restores854x480 and ends wall/selective/radialAA=false. Previous radial trial helper68475 complete.

Working tree has unbuilt follow-up Java changes: choose the curvature-capable probe even for opaque-only scenes when angular cap>.02 (skip masked pass when no materials); stable runClient username InterstellarDev; new DemoScene bed/chest/glass/pool displays (fluid inserted after completed container). Build.gradle username and new exhibits not runtime verified yet. Existing saved exhibit is retained; new displays are for new builds. Existing demo test dimension already contains manually added equivalent fixtures.

Next: inspect .08+4mm results/quality pairs, choose least costly acceptable default, remove rejected radial-AA code, verify another close/side pose, then fresh baseline A/B and final LIVE1440p gate. Commit experiment before switching branches for baseline; no simultaneous clients. Final candidate must be buildable/pushed, package rebuilt/docs current. Then do the requested post-step4 review of algorithms (potential exact quad-based representation reduces duplicated geometry; not implemented yet). Do not claim complete if any required gate remains.

## Owner state to restore at end

Original owner overworld position is (45.103214895634814,303.49506601944705,-9.682636277880926), yaw56.08086/pitch-.29997176, creative/flying. This was confirmed on demo leave. Benchmark player position16.5,302,-45.5 is NOT their original pose. Restore owner pose, inspect14 300 14, leave a usable client at854x480. Keep N65,r_s8.125, COM approximately(16.0076923077,302.0384615385,15.9923076923). Do not rebuild/edit original-world blocks/time/weather.

Dev launches previously use random usernames: saved demo return records are per UUID, so restarting inside demo with a new random name loses access to that return record. New stable username args fix this for future dev launches; verify enter/restart/leave once with InterstellarDev. Normal account identity remains unchanged. Use recorded original overworld pose for restoring this development session if no matching return record exists.

## Efficient tools

JDK C:\Portable\jdks\temurin-21.0.12.1. Gradle/GUI/git mutations require escalation, already authorized. Build logs redirected; check exit status. run/restart-client.ps1 -Log LOG normally closes one identified client and waits for actual exit, then launches with run/stable-init.gradle (quickPlay calibration save). Never two clients on a save. Cold material shader registration takes~100s: wait for Loaded1399 advancements, then2s, then click Create Backup and Load at window-relative270,305. Clicking earlier does nothing. NativeF2 diagnoses loading notices; don't repeatedly restart shader linking.

Ignored helpers: control-short.ps1 -X430 -Y15 focus; -HoldKey VK -HoldMillis120 (F2=113,F9=120,F10=121,F12=123, Escape27; arrows with scan codes). send-safe-command.ps1 -Command '/...' preserves clipboard. setup-comparison.ps1 -Log LOG waits join, inspects N65, F9, heldShift+M; wait Streaming terrain ready then Space. measure-pass.ps1 -Log LOG frozenB, -Key123 liveF12, -ClearPrevious only if old completed result still shown;120 warmup/300samples. size-minecraft.ps1 exact2560x1440, -Restore854x480. No builds during timings.

F9 [ (VK219) cycles .02/.04/.08; ] (VK221) cycles factor1/2/4; Z full/selective; backslash(VK220) radial-AA trial (remove after rejection). HeldShift+[ compares .02/1mm; Shift+Z compares full. Shift+P high-quality AA reference. C optical/material fixture. F9 arrows5degrees,7 taps move wall pitch.91 to down35.91. Benchmark uses yaw.281.

java tools/CompareAppearance.java PAIR_DIR; java tools/CompareSecondaryImages.java PAIR_DIR 16.0076923077 302.0384615385 15.9923076923 8.125. Pair metrics/metadata under ignored run/interstellar-captures. Native screenshots under run/screenshots. Use occasional milestone views, not every HUD/doc edit.
