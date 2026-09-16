# Progress

Last updated: 2026-09-15. Current branch: codex/terrain-prototype.

Current: F9 textured terrain-snapshot lensing, F8 sky/horizon lab, bounded source inspection and independent PG reference. 31 tests pass. See docs/terrain-prototype.md and the latest checkpoint below; early sections record historical results.

## Verified environment

- Windows; Ryzen 7 5800X3D, 32 GB RAM; RTX 5070 Ti reported by user.
- Minecraft 1.21.1 installation exists.
- IntelliJ IDEA 2026.2.2 found under C:\Portable.
- Temurin java/javac 21.0.12.1 executed successfully; jmods/compiler module present.
- Git origin points to rohrl/mc-interstellar; local settings preserved.

## Foundation checkpoint (historical)

Iteration 0: documentation and Fabric bootstrap implemented.

- Pinned Java 21 / Minecraft 1.21.1 / Fabric build with checked Gradle distribution SHA-256.
- Split common/client source sets and separate development game directory.
- F6 calibration HUD and F7 virtual reference centre; no physical world changes.
- Validated calibration config, preserving malformed user files while falling back to defaults.
- Analytic Schwarzschild radii/static shadow half-angle and one-dimensional SR reference quantities.
- README, agent instructions, decision log, roadmap, science notes, setup and handoff documents.

## Executed verification

- 2026-09-13: Gradle build succeeded in 4m 11s using the official Fabric secondary Maven service.
- JUnit report: 5 tests, 0 failures, 0 errors, 0 skipped. Checks cover shadow angular branches, scale invariance, distant limit, Doppler reciprocity/known velocity, and invalid inputs.
- Remapped mod and sources JARs generated in build/libs (ignored by Git).
- 2026-09-14: recovered those results after a session usage-limit interruption. Tightened the metadata dependency from ~1.21.1 to exact 1.21.1; rebuild succeeded in 56s. All 5 tests passed again.
- Development client launched successfully; Interstellar registered and wrote the expected default config.
- Created disposable creative world Interstellar Calibration. Visually verified F7 reports r=64 and r/r_s=8, F6 hides the overlay, F1 hides both vanilla and Interstellar HUDs, and movement updates the distance (observed r=67.04). The client later saved and exited normally. World rejoin/reset and malformed-config runtime checks have not been performed.
- No GPU optical renderer exists yet; no visual-physics or FPS validation is claimed.

## Not implemented at bootstrap (historical)

GPU ray tracing, lensing, black-hole rendering, source blocks/clustering, potion, actual-body returning images, guided free fall, disk, and retarded entity states.

## Blockers / user decisions

No product decision blocks the bootstrap. Renderer integration and cluster collapse definition remain engineering investigations.

- Bootstrap commit d7a87e7 was pushed successfully to origin/codex/bootstrap-observatory after authentication recovered. Earlier credential and DNS failures are resolved.
- IntelliJ's stalled Gradle import held the shared Loom cache lock after resuming. Stopped only that project's import helper (not IntelliJ), restarted the waiting agent build, and Loom recovered its stale lock. Editor state was preserved.
- Added the existing supported fabric_maven_url property to this machine's previously absent user Gradle properties, pointing to the official secondary host. New IntelliJ imports can now use it without CLI arguments. No project dependency versions or system DNS settings were changed to solve connectivity.
- Mobile pairing and notification permissions require the user to operate their device; push notification delivery is not verified by the agent.

## Exterior lab checkpoint — 2026-09-14, codex/optical-lab

Implemented GPU Schwarzschild exterior sky ray tracing, F8 screen, lensing/grid/alignment toggles and static-radius controls. Build tasks completed successfully; 9 tests passed (5 prior + 4 exterior tests). Runtime shader compilation succeeded. Visually checked default shadow/aligned ring, Space unlensed comparison, A producing separated source images, G removing the grid, and R restoring the view. Fixed vanilla Screen.render blurring the completed lab. Verified Up changes r/r_s from 8 to 6.67 after correcting Windows automation to send an extended arrow key. Window framebuffer was 854x480; no 1440p benchmark or GPU timings were collected.

Earlier statements above that no GPU renderer exists describe the bootstrap checkpoint only. World terrain lensing, mass blocks, potion, returning player-body images and horizon crossing remain unimplemented. Iteration 1 acceptance is not complete.

## GPU timing checkpoint — 2026-09-14

Implemented B/click opt-in GPU timing with warmup, percentile logging and query cleanup. Build passed; nine mathematical tests remain passing (unchanged tests reused by Gradle). Runtime verified three 2560x1440 runs: lensed r/rs=8 p95 1.178 ms; unlensed 0.03568 ms; lensed r/rs=6.6667 1.228 ms. See docs/benchmark.md for conditions and limits. Initial core-version-only capability check was corrected to accept ARB_timer_query. B, scene changes and repeat runs worked. No GPU ray readback/accuracy comparison yet; horizon crossing remains pending.

## GPU ray validation checkpoint — 2026-09-14

Implemented V floating-point readback and CPU/analytic comparisons. Owner's initial portrait runs exposed real capture and angle mismatches. Fixed ray initialization to use framebuffer dimensions and direct clip-space mapping instead of rounded GUI projection. Six corrected runs (flat/lensed, landscape/portrait, radii 4.63/8/64) each had zero invalid values, outcome/capture mismatches or unresolved rays. Sampled worst angle difference 3.553e-4 rad. 1440p GPU p95 remains 1.1784 ms. Build and nine tests pass; restored normal rendering visually checked. Exact results, methodology and limitations are in docs/ray-validation.md. Independent horizon-regular reference and crossing remain pending.

## Free-fall checkpoint — 2026-09-14, codex/free-fall

Implemented independent PG-time adaptive reference and GPU falling-frame ray initialization; F/T/H/L controls support paused frames, exact horizon, full playback and look-back. Build passes with 15 tests. Seven sampled GPU comparisons against the independent reference have zero outcome mismatches/invalid values; worst sampled angle error 0.002383 rad near the shadow edge. CPU and GPU share analytic connectivity classification. Automated playback reaches the explicit 0.35 cutoff. Interior 1440p optical-pass p95 measured 0.702272 ms in one run. See docs/free-fall.md for full results and limitations. Terrain, source blocks, actual-body images and spectral transport remain pending; this does not complete all iteration 1 fidelity work.

## Optical settings checkpoint — 2026-09-15, codex/optical-settings

Implemented validated persistent optical defaults and Q quality cycling. Build and 18 tests pass. Runtime checked config creation/reload, all quality GPU outputs/timings, disabled lensing, invalid-value fallback with unchanged file, and restoration of the original config. Default-scene 1440p p95 costs: FAST 0.736 ms, STANDARD 1.414 ms, FINE 2.771 ms. All sampled quality runs had zero outcome mismatches. See docs/optical-settings.md for limits, methodology and the increased STANDARD cost relative to fixed-step rendering. Non-default playback rate was loaded/displayed but not timed. No test edits remain in the user config.

## Mass-block checkpoint — 2026-09-15, codex/mass-blocks

Registered Mass Block/item, creative entry, vanilla-texture model, loot and pickaxe tag. Added on-demand bounded inspections, immutable summaries, revision cancellation and incomplete-result handling. Build and 25 tests pass. Runtime verified cross-chunk 64-block cube, split into 16/32, merge back to 64, empty-hand inspection, loot, and identical data after world save/reload. Full conditions and untested cases are in docs/mass-blocks.md. No live cluster index, shader source selection or world lensing yet; this is an inspection/data checkpoint within iteration 2.

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
## Live-camera checkpoint — 2026-09-15, codex/live-terrain

F10 now follows normal camera movement and periodically refreshes bounded opaque terrain while retaining the native HUD. F12 measures the terrain pass; F9 remains the frozen diagnostic view. Read D025 and docs/live-terrain.md for semantics, checks and limitations. Build/31 existing tests pass. Runtime checks include movement, repeated refresh, temporary block appearance/removal, resize, exterior/capture-boundary fallback and resource-reload invalidation. F9 regression had zero sampled flat mismatches. 1440p output with half-resolution tracing: live pass p95=3.755040 ms; sampled frame interval p95=9.2535 ms with refresh active. These timings precede snowfall in the saved scene.

Saved scene retains 6960 opaque cells plus snowfall (2400 unsupported cells in the final F9 check); pink floor regions mark unsupported snow layers. Preserve owner edits and weather-created blocks. No user testing is pending. Next: improve non-full-cube terrain coverage (especially snow), filter secondary images and validate finite-surface intersections independently. Live source maintenance, actual body, terrain interior, radiometry and SR potion remain unfinished. Live interactions are straight vanilla rays; observer motion has no SR transformation yet.

GitHub's default branch is codex/bootstrap-observatory, so its landing page shows the old bootstrap checkpoint. Development branches hold newer work; do not assume failed pushes from that page. Verify remote refs after publishing. Earlier usage-limit interruption left live changes local; this checkpoint resumes that work.

## Snow and workflow checkpoint — 2026-09-15, codex/snow-layers

Token-conscious workflow recorded in AGENTS.md; handoff shortened to current state. Snow layers now render at their actual heights in F9/F10. Build/32 tests pass; sampled GPU flat geometry has zero mismatches; snowy demo now has zero unsupported cells. Live 1440p/half-resolution GPU p95=4.244544 ms; frame interval p95=9.6169 ms. See docs/snow-layers.md for conditions and limitations. Client left in F10 for optional exploration. No owner testing is pending.

## Stable exploration checkpoint — 2026-09-15, codex/stable-exploration

Completed source selection now survives unrelated chunk activity; relevant nearby changes still invalidate it. F10 automatically pauses/resumes at exterior and 128-block observer limits. F9/F10 can view the bounded 96^3 capture from outside; missing external terrain remains explicit. Long-ray diagnostics/UVs retain traversed voxel identity. Antialiasing is separately preserved at 8ad46eb on codex/terrain-antialiasing, unverified and excluded here. Added repository launcher.

Build/35 tests pass. Six near/far/overhead GPU checks have zero flat mismatches and no sampled lensed budget exhaustion. Verified F10 initial out-of-range pause, both automatic recoveries, selection survival during distant travel, and relevant mass-edit invalidation with temporary block restored. 1440p/half-resolution live GPU p95: near 5.111360 ms, far 4.707744 ms; frame interval p95 9.2592 / 9.1930 ms. Detailed evidence and limits: docs/stable-exploration.md and D027. Client left in live mode; no owner testing pending. Curved finite-surface validation and uncaptured foreground occlusion remain unfinished.

## Curved-terrain diagnostic checkpoint — 2026-09-15, codex/curved-terrain-validation

F9 C now checks finite curved hit cells against an independent affine-parameter DP5(4) reference with independent slab geometry and paired refinement. Build/40 tests pass. Eight near/far/overhead/inner-photon-sphere runs at standard/fine path settings compare 768 samples with zero mismatches or inconclusive rays. Final near/far reporting checks also pass, including invalid-output and hit/capture counters. V regression remains zero flat mismatches. Restored terrain image visually checked; live 1440p/half-resolution GPU p95=5.132864 ms, frame interval p95=9.1841 ms. Details and shared assumptions: docs/curved-terrain-validation.md, D028.

This supports delivery step 2; it does not complete source auto-refresh or demo packaging. Owner's five-step order is now in plan.md. Next: bounded automatic source refresh after relevant changes, then repeatable demo packaging. AA remains separately preserved and unverified. Client left in F10; no owner testing pending.

## Automatic source refresh checkpoint — 2026-09-16, codex/source-refresh

Implemented bounded, debounced refresh of the component containing the inspected anchor, with retry backoff and watched dependencies for growing/partial sources. F10 intent survives metadata refresh, removal, unloading and extended-source states; complete black-hole metadata rebuilds its renderer automatically. Manual off stays off. Disconnect/world changes clear the session; respawn replacement also clears stale metadata (compiled, not death-tested).

Build/45 tests pass. Runtime verified N=63->64->63 live metadata updates, anchor removal/replacement recovery, chunk unload/reload, N=1 extended->N=27 black-hole recovery, split to the anchor's N=9 fragment, merge back to N=27, manual-off preservation and zero-mismatch F9 regression. Temporary blocks were removed and the original anchor restored before the owner's later edits. Detailed evidence and limitations: docs/source-refresh.md, D029.

The owner subsequently authorized enlarging the edited source and reported RMB placement was blocked. Fixed inspection consuming held-item clicks; actual RMB placement and empty-hand inspection both verified. Filled only air in the original cube volume, adding 37 blocks: current source N=64, COM=(16,302,16), r_s=8. Final live GPU p95=5.066368 ms at 1440p/half-resolution, frame interval p95=9.423199 ms (r/r_s=4.80295 at start; not a fixed-camera comparison). Client left in F10. Step 1's bounded automatic-refresh work is complete; NEXT is repeatable demo packaging under the owner-reaffirmed order in plan.md.

## Background fallback reverted — 2026-09-16

Owner observed duplicated bent/unbent wall geometry during movement and rejected the same-screen fallback. Reverted 87dd3a1 with 0a264b0; build passes. No world edits or AA changes. Step 2 remains incomplete: prioritize coherent extended geometry and first-hit ordering before packaging. See plan.md and D030. Prior screenshot acceptance did not establish moving-view correctness.
