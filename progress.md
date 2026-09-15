# Progress

Last updated: 2026-09-15. Current branch: codex/source-selection.

Current: controlled-sky lensing, independent PG reference and guided horizon crossing. 25 tests pass. See docs/mass-blocks.md, docs/optical-settings.md and docs/free-fall.md and the latest checkpoint below; early sections record historical bootstrap results.

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