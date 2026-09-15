# Handoff

Read this, decision-log.md, progress.md and docs/science.md before continuing. Do not reconstruct requirements from a generic Minecraft black-hole prompt.

## Location and authorization

- Git root: C:\work\code\minecraft\interstellar\interstellar.
- Parent folder of same name is intentional and is not the Git root.
- Remote: https://github.com/rohrl/mc-interstellar.git.
- Current branch: codex/terrain-prototype. Verify git status/branch before modifying anything.
- User permits branches and pushes. No force pushes or unrelated cleanup.
- User requests notifications for blockers/decisions and durable Markdown project records.

## Environment

Windows PowerShell; JDK C:\Portable\jdks\temurin-21.0.12.1. Git is available through the agent runtime and C:\Portable\PortableGit. IntelliJ 2026.2.2 installed. 5800X3D / 32 GB / 5070 Ti, 1440p60 target.

The original agent task started in a generated Documents/Codex folder, not in the repo. Its sandbox requires reviewed elevation for C:\work writes and network access. This is an execution-environment constraint, not a request to change the project location. Do not hardcode the original task's scratch directory into the project.

## Resume procedure

1. Inspect git status, latest commits, and progress.md. Preserve unrelated local edits, especially .idea.
2. Select a complete JDK 21 and run .\gradlew.bat build.
3. Follow the next unfinished acceptance criterion in plan.md. Never mark GPU/runtime/FPS checks complete based on unit tests.
4. Update decision-log.md for major choices and progress.md/handoff.md with actual results, next steps and blockers before finishing.

## Critical requirements

Actual player body, not mannequin. Crossing the horizon remains in scope despite deferral of entity freeze/history. Accretion disk deferred. Independent observer speed initially, separate SR/GR scales allowed. Dedicated pack acceptable but Iris not mandatory. Screen-space-only rendering is insufficient for the final scientific ambition.

## Next implementation step

After the foundation is built and the HUD checked, implement an independent horizon-regular numerical reference and a controlled GPU scene. Define the observer tetrad and past-directed ray initial conditions before writing the visual effect. Resolve scene-data access with a measurable spike; do not begin by inventing Iris uniform APIs.

## Resume checkpoint — 2026-09-14

Bootstrap commit d7a87e7 contains the scaffold and documentation and has been pushed to origin/codex/bootstrap-observatory. Build succeeded, 5/5 reference tests passed; runtime HUD, F6, F7, F1 and movement were visually checked in a disposable creative world. Check progress.md for remaining checks. Earlier GitHub authentication failures are resolved.

This machine's user Gradle properties now set fabric_maven_url=https://maven2.fabricmc.net/. If an IDE import holds the Loom lock, diagnose the owning process before touching it; never delete an active lock or stop unrelated builds. The agent previously stopped only the identified stalled import for this project, preserving IntelliJ.

## Latest checkpoint — exterior lab

Continue on codex/optical-lab. F8 now opens a mod-owned exterior test-sky shader; Space/A/G/R were visually checked. Up was verified (8 to 6.67); Windows automation needs extended-key flags and scan codes for arrows. Nine mathematical tests pass. No quantitative GPU comparison or performance benchmark yet. Do not confuse the exterior RK4 CPU diagnostic with the planned independent horizon-regular reference. Next: add GPU readback/timing validation, then implement valid falling-observer rays across the horizon. Preserve the planned terrain/body integration scope. The development client was left open for the owner to try the visible lab.

## Latest checkpoint — measured GPU pass

LabBenchmark provides optional B/click timestamp profiling. Three 1440p runs completed; conditions/results are in docs/benchmark.md. Minecraft's 3.2 context exposes ARB_timer_query despite OpenGL33=false. Build passes. Next work is quantitative ray-output readback and an independent horizon-regular solver, followed by falling-observer rendering. Do not present timing checks as physics validation. The client is left running with the optical lab; no user decision blocks continued development.

## Latest checkpoint — GPU ray checks and resize fix

V now samples actual GPU output. Read docs/ray-validation.md: owner testing caught a GUI projection/pixel-centre mismatch; direct clip-space quad plus framebuffer dimensions fixes it. Six sampled runs have no capture mismatches, but near-critical accuracy is not bounded, and modulo-angle comparison does not verify winding counts. Next: independent horizon-regular reference, targeted critical rays, falling-observer sky rendering. Current diagnostics are stationary/exterior and shared RK4. Do not treat successful GPU comparison as full physical validation. Client remains open at default radius in fullscreen.

## Current checkpoint — codex/free-fall

FreeFallRay is an independent adaptive PG-time reference. GPU spatial orbit equation now initializes in a freely falling frame and crosses u=1 for sky-connected rays. F toggles observer, T plays/pause, H jumps to a paused exact-horizon frame, L looks back. V uses the PG reference in both static and falling modes; analytic shadow comparison is only counted for static mode. CPU/GPU share analytic connectivity classification. Read docs/free-fall.md before changing signs or claiming collapse physics. All 15 tests pass; automated runtime and timings recorded there.

Next: targeted near-critical accuracy/winding checks, quality controls and configured defaults, then source blocks/guided exhibits (iteration 2) and terrain scene data (iteration 3). Owner confirmed terrain distortion is expected and asks agent to operate runtime tests without their attendance. No owner input is needed for routine continuation.

## Current checkpoint — codex/optical-settings

Persistent defaults now load from config/interstellar-optics.json on each F8 open. Q cycles integration step; R restores loaded settings; keyboard changes are temporary. Read docs/optical-settings.md for validation and costs. Original local config was restored after testing. 18 tests pass, including Gson parsing; test runtime needs Gson 2.10.1 explicitly. Shader computes angle from loop index, and V logs quality. Current client is left open with restored STANDARD defaults. Next: targeted near-critical/winding accuracy and source-block/clustering work; terrain integration remains iteration 3.

## Current checkpoint — codex/mass-blocks

SourceBlocks registers interstellar:mass_block. SourceInspector runs bounded on-demand jobs; ClusterProbe is the pure tested component/geometry kernel. Read docs/mass-blocks.md for model, budgets, incomplete/stale behavior, test-world positions and untested paths. Fixed mass uses ordinary chunk persistence, with no BlockEntities or custom save data. 25 tests pass; runtime split/merge, right-click, loot and save/reload passed. The client remains at the high-altitude test structure.

Next: integrate validated source selection/synchronization with the lab, then maintained cluster tracking if needed. Do not claim the current inspector is a live global index. Preserve unknown/capped/stale distinctions. Terrain geometry rendering remains iteration 3; critical-ray/winding accuracy is still tracked separately.

## Remote publication pending — 2026-09-15

Mass-block implementation is committed locally on codex/mass-blocks (implementation commit ee55b74). Two pushes failed connecting to github.com:443. System DNS and an explicit Cloudflare resolver query both returned 4.237.22.38; no network settings were changed. Retry git push -u origin codex/mass-blocks when connectivity returns. Tests and runtime verification are complete for this documented checkpoint; origin does not yet contain this branch.


## Remote publication recovered — 2026-09-15

Verified origin/codex/mass-blocks at c697e0b with git ls-remote. The earlier publication-pending note is superseded.

## Source-selection checkpoint — 2026-09-15, codex/source-selection

Completed inspection data now synchronizes through a typed S2C payload. World HUD displays the selection; F8 then S uses a black-hole proxy's scale and camera distance in the sky lab. R returns to configured defaults. Server revisions clear stale selections, and client state is scoped to its ClientWorld/connection. Read D022 and docs/mass-blocks.md. No terrain rendering or live cluster index has been added.

Build passed and all 25 existing tests passed. Autonomous runtime checks passed: source selection, falling-frame initialization, reset, rejection of an extended source, invalidation after a temporary mass-block placement, and clearing after disconnect/reload. The saved main cluster now has 63 blocks (owner edits preserved), r_s=7.875; camera r/r_s=0.706777535. The only temporary block at (20,306,20) was removed using a mass-only replacement. No owner input is required for these checks.

V at that radius (STANDARD, 854x480 framebuffer, 128x72 diagnostic) had zero invalid/outcome-mismatch/unresolved rays. Among 1624 escaped rays, angular p95=9.56496e-5 rad and max=0.00563317 rad (~0.323 degrees). This is a larger sampled worst-case error than earlier runs; near-critical accuracy remains unfinished. No shader changes or new performance benchmark in this checkpoint. Live multiplayer invalidation while the lab remains open, dimension transfer, stale/capped/unknown server-job integration, and dedicated-server startup remain untested. Existing pure probe tests cover incomplete/capped components.

Next: targeted critical-ray/winding checks and guided source/body demonstrations. Terrain/hidden-geometry integration is still iteration 3. Source selection is deliberately conservative: unrelated chunk activity can clear it, and no automatic reinspection is performed.
## Critical-ray checkpoint — 2026-09-15, codex/critical-rays

V now logs unwrapped angles; C runs a bounded 252-ray off-screen critical-direction suite across all quality presets. Read docs/critical-rays.md and its CSV, which preserve actual results and limits. Build/28 tests pass. Complete baseline suite, restored shader visual check, portrait V and 1440p STANDARD timing completed autonomously. GPU pass p95=1.408512 ms. Extreme near-critical samples reveal large angular errors (up to 1.203488 rad), no sampled full-turn-bin differences, and budget exhaustion. CPU-unresolved results are inconclusive; the added reporting counter was compiled after the baseline runtime. A shifted-variable solver was evaluated and rejected; production orbit integration remains unchanged.

An initial diagnostic helper rejected the float representation of 0.35 and stopped the client; fixed with regression coverage and diagnostic exception handling. The saved world was preserved. No blocks or terrain were changed during this checkpoint. All runtime checks are agent-operated; no owner attendance is pending.

NEXT: prioritize a working mass-block/terrain optical demo. Start the bounded scene-data prototype for one selected spherical source and opaque nearby terrain; provide off-screen geometry and explicit missing-data behavior rather than pretending screen-space distortion can reveal hidden surfaces. Bring this ahead of actual-body demonstrations (D023). Do not spend the next iteration solely polishing critical-ray numerics. Player-body, transparent terrain, spectral transport, maintained clustering and improved critical precision remain tracked follow-ups. No product decision blocks proceeding.
## Terrain prototype checkpoint — 2026-09-15, codex/terrain-prototype

F9 captures a bounded 96^3 region around the inspected source and renders actual textured opaque Minecraft cubes through curved rays. Read docs/terrain-prototype.md and D024 before modifying it. Snapshot is frozen; exterior static observer only; amber means missing scene data, pink unsupported/budget. Space compares lensing; arrows/L aim; Q resolution; J chord target; V flat geometry/off-screen audit; B timing. Config interstellar-terrain.json supports enabled and renderScale. F8 sky remains independent.

Build/31 tests pass. Agent-operated runtime checks exercised textured floor/backdrop, foreground pillar, Space comparison, off-screen hits and landscape/portrait geometry diagnostics. Zero flat-hit mismatches in recorded samples; a turned view found 192 off-screen lensed hits. This is not full curved-surface validation. Half-resolution lensed p95=3.601376 ms at a 1440p window; full-resolution fine p95=16.449408 ms (owner-triggered run, verified log), too costly for the full 60 FPS budget. Capture about 0.90 s spread across frames. Subsequent owner edits to the wall were preserved.

Fixed a native texture-upload overread caused by inherited GL_UNPACK_SKIP_ROWS. Upload/readback now isolate pixel-layout/PBO state; repeat opens succeeded with inherited skip rows 19 and 10. Resource reload invalidates UV snapshots. Preserve local run/hs_err_pid7212.log and world data; neither belongs in Git. No owner attendance is needed for routine tests.

Next: make the terrain demo easier to use in normal play (live camera/bounded updates), improve explicit snapshot-boundary presentation and filtered secondary images, and validate finite-surface ray intersections independently. Do not describe F9 as a live world shader or certify the floor/rings from screenshots alone. Foreground floor has a valid sharp occlusion edge; its secondary image appears around the hole, while amber boundary markers are artificial. Player body, interior terrain, full block models/fluids/entities and radiometry remain unfinished.
## Final verification addendum — 2026-09-15

Final-build V pair used the same logged camera (16.5,303.6199998855591,-19.5), yaw=0.8451603, pitch=2.6502786, aspect=1.7791666666666666. Standard/fine path targets 0.45/0.225 both reported zero flat mismatches, 721 flat hits, 982 lensed opaque hits, one off-screen hit with margin and zero lensed unresolved samples. Aggregate counts agree; individual curved hit-cell convergence was not measured. Config enabled=false was runtime checked and the exact original bytes restored. Repeated captures after the pixel-unpack fix succeeded. Resource-reload invalidation is implemented but the attempted F3+T input in the screen did not trigger a reload, so that path is not runtime verified.

Final-build 1440p-window timing, internal 1280x720, STANDARD path, lensing on, source aimed with L, 6960 opaque cells: GPU p50=3.638848, p95=3.740352, p99=3.791520 ms; sampled frame-interval p95=8.6177 ms. This is the most recent default-mode measurement. The client is left open for optional exploration; no user testing is pending.