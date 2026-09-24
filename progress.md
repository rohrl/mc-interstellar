# Progress
## Current checkpoint — 2026-09-24, native world features and tether gravity

Implemented emissive/glint materials, native nearby shadows, ordinary sign/name text,
selection/mining overlays, curved team-coloured Glowing silhouettes, a cheap local
rain/snow approximation and returning images of the actual player model. Tridents and
free fishing bobbers participate in local gravity; native fishing/leash geometry bends
with bounded endpoint-pinned sag. First-person foreground remains native. Glowing
outlines run only when needed; weather is explicitly unbent foreground. Actual-body
images use current pose and can appear as thin arcs. No delayed light history.

Build/package and77 tests pass. Final release shader/mixin runtime:52,480 optical
comparisons and156 material cases, zero mismatches/unresolved/failures. Visual checks
cover the new layers, mining, native shadows, glint on/off, rain/snow, team-coloured
through-wall outlines and actual-body on/off. Trident deflection/loyalty, fishing reel-in
and native leash attachment were exercised. Detailed evidence and limits:
[world features](docs/world-features.md); [current coverage](docs/minecraft-coverage.md).

At the1440p arrow viewpoint: body-on GPU/frame medians21.001/21.601ms (about46FPS), body-off
18.734/19.338ms (about52FPS). Glowing adds about3.7ms in a fixed fixture; weather's frame
median delta0.057ms is within noise. These are scene samples, not a new worst-case floor.
A body-only empty-cache trial gave no meaningful gain and was removed despite matching
images. Next return to targeted GPU measurement, optical tables and moving-tree reuse;
profile camera-body visits before attempting a separate body tree.

Narrator disabled. Temporary fixtures/biome/team changes removed, original recorded
player fields and window restored, normal ticking, F10 off and client paused. Older
AA/performance branches remain preserved. See focused handoff for the final runtime.

## Current checkpoint — 2026-09-24, first-person foreground restored

F10 previously composited the lensed world at HUD HEAD, covering vanilla hands/items and first-person overlays. Split scene/HUD rendering; composite immediately after WorldRenderer.render, before vanilla's hand pass. Preserve normal offhand/use animation, later screen effects and F1 behavior. Suppress the separate unbent glowing-outline framebuffer only when a world composite was actually drawn. Optical shaders, ray sampling and AA are unchanged. Current feature gaps are consolidated in [Minecraft coverage](docs/minecraft-coverage.md).

Build/package succeeds,75 existing tests have zero failures (unchanged test task reused). Final runtime shader/mixin startup and visual checks pass: empty main hand plus offhand bow, drawn bow, F1 hidden HUD with lensing retained, and F9 frozen preview. A1440p arrow-course check (half-scale,AA2,120 warmup/300 samples) measures lensing GPU p50/p95/p99=17.985/18.939/19.295ms, frame intervals18.520/20.123/20.818ms. Comparable to the earlier short arrow-course check; no isolated speedup or worst-case FPS guarantee. Log `first-person-final-runtime.log`; images `run/first-person-*.png`. No broad optical/movement suite rerun for this rendering-order-only change. Exact user pose, flying state, inventory and window restored; F10 off, client paused. Next optimization plan remains unchanged.

## Current checkpoint — 2026-09-24, wider gravity and arrow course

Local influence radii doubled (64-block source:20→40 blocks, maximum64). A separate `/interstellar demo arrows` exhibit preserves the user's relocated gameplay builds and supplies four calibrated automatic dispensers. Native runtime verifies a491.9-degree transient loop, flyby, outward reversal and capture; ordinary drag/downward gravity/collisions remain active. Launches are bounded and cleaned up. The mob approach tint begins farther out and is more visible; physical delayed-light/horizon slowing remains deferred.

Confirmed and fixed the small-source sky rings: outgoing inverse-radius steps could overshoot infinity before testing finite terrain. The extended-source-only guard removes the rings in one/two-block checks; matched conservative-reference image MAE0.000052, with no material change in the measured single-block GPU time.75 tests pass, including independent trajectory and analytic finite-wall regressions. Live arrow-course pass medians:17.727ms firing,17.310ms cleared; sampled scene check only. Full details and limitations: [gameplay gravity](docs/gameplay-gravity.md). Next resume targeted GPU measurement, table-assisted optics and moving-tree refit/reuse.

## Current checkpoint — 2026-09-23, gameplay gravity implemented

The accepted gameplay milestone adds automatic shared source discovery/refresh, gradual compact-cube lensing through a finite optical interior, local mob lift/capture, curved arrows/thrown projectiles and a cheap approach colour cue. `/interstellar demo gameplay` supplies the new exhibit; the original exhibit keeps its calibration and passive entities. The owner permits a small performance cost for this work.

Build/package passes72 tests, including30 independently integrated extended-ray comparisons. Runtime checks cover discovery on reload, cube progression without recapture, mob lift/capture, projectile deflection/damage/horizon ordering and stable wall embedding. Preserved black-hole optics pass52,480 sampled comparisons and84 material checks. Small-scene CPU/GPU measurements and their limits are recorded in [gameplay gravity](docs/gameplay-gravity.md); no new worst-case FPS guarantee. Original player pose/flying state and normal ticking are restored; fixtures cleaned. Next return to the queued performance work.

## Current checkpoint — 2026-09-22, quad vertices adopted

Requested demo/refinement steps3–4 and subsequent algorithm review are complete for the documented subset. The owner then authorized quad vertices and explicitly accepts a small easier-view regression for a gain in the slowest view. Production uses four-quad leaves, one retained vertex arena and one compact node arena; both native triangles and full-precision appearance remain intact.

Controlled frozen heavy-view GPU median improves4.3%; wall medians stay close with some worse tails. Wall/down/close original/quad image pairs match exactly. Final live medians are about57–60FPS wall and34–35FPS down, with evolving actors and mixed tail results.57 tests/build/package,52,480 optical comparisons and28 material cases pass. General/optimized and full/selective pairs match. Native appearance and final demo inspected; guarded source64→65→64 refresh passes without reload and the temporary block is removed. See docs/quad-vertices.md for complete evidence and limits.

Accepted branches: codex/quad-vertices and codex/demo-visual-refinement. A/B experiment49738ba is preserved and pushed separately. Original AA and older experiments remain untouched. The next bounded performance proposal is sharing planar-face intersection work; it is not implemented. Step5, weather, general teleport and automated movement/flicker testing remain deferred. Historical checkpoints follow.

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

Owner clarified the acceptance tradeoff: distant-scene heuristics are welcome if errors are small/hard to notice and performance gains substantial. Practical, coherent world integration is required in the working demo. Plan/D031 now prioritize a measured hybrid prototype rather than presupposing exact whole-world tracing. This checkpoint changes requirements only; renderer remains at the verified reverted build.

## Distant representation experiment — 2026-09-16, codex/distant-heightfield

Opt-in H/F9 or distantPrototype/F10 height-field renderer implemented and profiled, with local-volume ownership preventing a straight-camera copy of the wall. Build/45 tests pass; sampled distant flat intersections (93 rays, 21 hits) and local V both have zero mismatches. Final frozen far-view 1440p/half-resolution GPU p95=9.932704 ms, frame interval p95=11.5638 ms. Fog/sky/overhang quality remains below demo acceptance; original config/default and normal F10 restored. No block edits. Full evidence and next work: docs/distant-prototype.md, D032. Workflow tightened for fewer screenshots and state-gated GUI checks; no token-saving quantity claimed.

Owner subsequently rejected the prototype's white mountains and dark sky. Assessed a staged visual-reference workflow: vanilla appearance parity first, then slow lensed reference and automatic image comparisons. Recorded concrete capture/metric/coverage gates in docs/visual-reference-plan.md and D033. This checkpoint is code inspection and design assessment only; no renderer, image harness, game state or local configuration changed, and no redundant screenshot/build was run.

## Appearance repair and launch recovery — 2026-09-17

Completed the interrupted native sky/light and cap/side material implementation; fixed the reserved GLSL identifier that blocked shader startup. Build/45 tests, comparator self-tests and actual F9/H/F10 runtime pass. First same-frame vanilla/zero-bending pair RGB MAE 0.0423; repeat vanilla references identical. Local/distant V diagnostics have zero mismatches. Live 1440p/half-resolution GPU p95=9.90976 ms, frame p95=12.0548 ms, with timer limitations documented. Final HUD wording edits rebuilt without redundant screenshots. See docs/native-appearance.md for evidence and remaining appearance errors. Client/local config left in F10 with experiment on; code default off, no block edits, AA separate. World integration and slow lensed reference remain unfinished.

## World projection alignment — 2026-09-17

Pushed repair 6e3b69d to explicitly confirmed origin. Fixed the next concrete comparison failure: actual world FOV was77 degrees while backend used70. F9/F10 and independent diagnostics now use world perspective scales/offsets. Final build/45 tests pass; local/distant V zero mismatches. Two daytime image pairs inspected, wall/platform alignment improved; cloud/sky/material/geometry errors remain. Live 1440p/half-resolution GPU p95=9.685856 ms, frame p95=11.6756 ms (documented exclusions). Client left F10 enabled at far reference; no block edits, AA separate. Exact evidence: docs/native-appearance.md.

## Native sky state repair — 2026-09-17

Corrected sky shader/global matrix setup and restoration. Build/45 tests pass. Near-wall nighttime pair top-third RGB MAE0.0040; moon/gradient align. Downward pair shows aligned cloud pattern, with remaining foreground-cloud and terrain errors. Repeated vanilla references pixel-identical. Live 1440p/half-resolution GPU p95=9.701568 ms, frame p95=11.5768 ms (documented exclusions). Client left F10 on in creative flight; no block edits, AA separate. Usage interruption recovered from saved evidence; improved fixture instructions avoid falling fresh players and stale-capture comparisons. See docs/native-appearance.md.

## Native fog and reported boundary — 2026-09-17

Replaced source-centred distant fade with native camera-relative opaque terrain fog. Build/45 tests and runtime pass. Just-inside-boundary pair has matched effective FOV77 and aligned platform; full RGB MAE0.0211, bottom-half0.0397, with visible remaining mountain errors. F10 automatically pauses/resumes across two fixed boundary poses. Hard128 cutoff and terrain lighting/geometry remain unfinished. Live1440p/half-resolution GPU p95=9.645760ms, frame p95=11.7428ms; exclusions documented. Client left F10 on, creative flight, no block edits or AA changes. Details: docs/native-fog.md.

## Captured face lighting — 2026-09-17

Implemented local/distant face-specific sky/block light, separate cap/side records, and F9 K comparison. Controlled under-platform RGB MAE 0.0656→0.0001; downward mountains improve only marginally and vanilla snowfall remains omitted. Reference images are identical within both A/B tests. Build/45 tests, runtime and live 1440p timing pass (GPU p95 9.658944 ms, frame p95 12.2304 ms, documented exclusions). New buffers are bounded/released; extra two-snapshot memory 35 MiB. No block edits, creative flight/F10 retained, AA separate. See docs/face-lighting.md; smooth shading, geometry/coverage and hard boundary remain unfinished.

## Native smooth corner lighting — 2026-09-17

Implemented captured native corner light/AO, shared records, quad interpolation and F9 O comparison. Wall/floor RGB MAE 0.0409→0.0333; daylight mountains 0.0380→0.0262, bottom half 0.0716→0.0483. Both paired references identical; contact sheets inspected. Usage-limit interruption recovered, final cache optimization built and runtime verified. Build/45 tests pass. Live 1440p/half-resolution GPU p95 9.719936 ms, frame p95 11.8643 ms; capture ~3.8 s, slower than face-only. No agent block edits; AA separate; F10 left on in creative flight. Foreground clouds, geometry/coverage, hard boundary and refresh efficiency remain next. See docs/smooth-lighting.md.

## Native mesh quality experiment — 2026-09-17

Owner defers FPS optimization until convincing integration. F9 M now captures native baked quads, tint/AO/light and actual full-height loaded terrain into a bounded BVH, with coherent straight/curved triangle hits. F10 unchanged. Build/47 tests pass; runtime mountain/wall pairs and lensed foreground ordering inspected. Nighttime mountain RGB MAE 0.0162→0.0134; wall 0.0106→0.0105, identical vanilla A/B references. Capture 14–16 s, 2.26–2.51 million triangles; memory-heavy reference experiment. Clouds, entities, transparency, coverage and viewing boundary remain open. See docs/native-mesh.md for evidence and diagnostic timings; this does not complete step 2. AA preserved, no block/time/weather edits.

## Frozen mob capture and lightmap parity — 2026-09-17

Owner accepts terrain appearance; F9 M now captures native living mob bodies/opaque equipment into the shared BVH, with E toggle. Corrected mesh terrain lightmap coordinates; K compares native versus previous offset. Close-up RGB MAE 0.0188→0.0024; with mobs disabled 0.0070. All three vanilla references identical. Final build/47 tests, runtime shader/mixin startup, two-pose appearance checks, lensed ordering and F10 reentry pass. Captured 96 supported bodies; special glow/shadows/transparency remain omitted. Diagnostic GPU p95 7.430944 ms at only 427×240 internal; no FPS gate. See docs/mesh-entities.md. Frozen snapshots only; clouds, live representation, coverage and daytime parity remain next. AA preserved; no scene block/time/weather/population edits.

## Native foreground clouds — 2026-09-18

F9 M now includes native cloud geometry/colour/opacity in the shared ray scene; N compares foreground versus background-only clouds. Sky capture excludes clouds in foreground mode, and mesh escape bounds include cloud geometry. Three fixed-pose pairs have identical A/B vanilla references: daylight mountain MAE 0.0126→0.0071, near-terrain below-cloud 0.0120→0.0035, clear night below-cloud 0.0013→0.0007. Contact sheets and a lensed view inspected. Build passes 47 tests; runtime shader/mixin, cleanup/F10 recovery and mesh reopening passed. Diagnostic GPU p95=11.507328 ms at 427×240 internal, not an FPS claim. FANCY only runtime-certified; general transparency and camera-dependent cloud limitations remain. Next: broader terrain capture coverage, which dominates the daylight residual. Details: docs/mesh-clouds.md. AA preserved separately; no scene edits.

## Expanded camera-centred terrain capture — 2026-09-18

F9 M now requests render distance plus one chunk around the camera in all directions (27×27 at distance12), with a seven-million-triangle limit. U compares old terrain bounds on the same frozen scene. Mountain lower-half RGB MAE0.0364→0.0206, identical vanilla references; inspected pair and lensed view. Snowfall is now the conspicuous unrepresented layer in this test. Capture6.18million triangles/38.24s, diagnostic optical GPU p95=20.837664ms at427×240. Build47 tests pass; runtime shader, capture, pairs, cleanup/F10 recovery checked. Final client restarted on the guarded-distance-limit fix. Detailed evidence/limits: docs/mesh-coverage.md. F10 and128-block camera guard unchanged; no world edits, AA still separate.

## First live native-mesh checkpoint — 2026-09-18

F10 now uses native terrain with live camera, animated living mobs and updating clouds. Terrain-only capture6.18million triangles/37.73s; separate moving scene initially12,960 triangles/96 mobs. Entity texture tiles persist; shared nearest-hit ordering avoids frozen mob copies and straight-camera overlays. Build47 tests pass; runtime shader, sustained geometry/animation changes, two inspected fixed-camera snapshots, ordinary movement, F10 cleanup/reactivation and timing checked. Diagnostic optical GPU p95=9.431040ms, sampled frame p95=11.3300ms at427×240; not target FPS. docs/live-native-mesh.md records scope and evidence. Terrain geometry/light remains held at activation; incremental edits/chunk streaming next. Owner accepts initial loading, excludes teleporting from v1 and defers rain/snow. AA preserved separately.

## Incremental terrain and fast source refresh — 2026-09-18

F10 now replaces dirty chunks independently and streams the camera window. Native block/light invalidations and chunk lifecycle events feed a bounded cache; two-level ray traversal retains coherent hit ordering. Temporary block addition/removal republished only its chunk. Mass addition/removal automatically produced N64→65→64 without terrain reload; both original air cells restored and verified. A one-chunk strafe retained702/729 entries and queued27 new edges. Frozen streamed/monolithic candidates have identical SHA-256 hashes and identical vanilla references; RGB MAE0.0025 against vanilla at the test pose. Build50 tests pass; runtime shader/mixin, appearance, edits, streaming, source refresh and cleanup checked. GPU p95=18.435296ms during boundary updates at427×240, no FPS claim. ~1280MiB fixed GPU arenas; initial33–37s, F10 toggle reload remains. Details: docs/streaming-terrain.md. Rain/snow/teleport deferred, AA separate, stars unchanged.

## Wider native viewing range — 2026-09-18

F10 camera limit256 blocks; F9 beyond128 automatically chooses native mesh. Live HUD shows distance/limit. Normal backward flight from125 to148 blocks kept lensing active; inspected the lensed wall view. Farther unbent pair RGB MAE.0005 against vanilla; contact sheet inspected. Vertical flight crossed256 and recovered without a new terrain capture. Build51 tests pass, including analytic CPU shadow-boundary brackets at r/r_s16,24,32,64,128. Diagnostic GPU p95=22.724896ms at427×240 during edge streaming; no FPS claim. Source availability, missing geometry and a hard256 cutoff remain. No scene edits; stars/rain/snow/AA unchanged. Details: docs/viewing-range.md.

## Independent curved mesh fixture — 2026-09-18

F9 native mesh C now compares synthetic opaque-box triangle hits with the independent affine CPU/slab solver, retaining production distant steps. All720 near/far comparisons pass across two mesh layouts/two path settings; no inconclusive or unresolved rays, runtime577ms. Before/after candidate and vanilla hashes identical; contact sheet and lensed image inspected. Build51 tests pass; normal frozen GPU p95=15.975104ms at427×240, not an FPS claim. No source/world/AA edits. Synthetic hit-cell coverage only; arbitrary models/materials and stronger convergence remain limited. Details: docs/mesh-ray-validation.md. Next: repeatable demo packaging with current coverage limits clearly stated.

## AA and first performance improvement — 2026-09-19

Packaging paused by owner; WIP preserved on codex/demo-packaging-wip at2fe8674 and excluded. Ported two-ray AA with independent per-ray cloud state, sharper reconstruction and quality comparison controls; rejected soft filter remains optional. Curvature-guided steps retain the angular cap and full scene coverage, while moving meshes reuse GPU/staging buffers. Build51 tests and expanded1440 CPU/GPU comparisons pass. At matched2x AA in the frozen streamed scene, GPU p95 improves52.367→36.102ms (31.1%); OFF improves27.598→19.573ms.427×240 internal only. Two path-image pairs show~0.007% pixels differing over8 levels; inspected. AA quality/cost remains a trade-off, not an FPS fix by itself. Details and exact limitations: docs/aa-performance.md. Continue performance work with the accepted appearance; packaging stays paused. Owner's current65-block source preserved.

Final live checks confirm changing mob geometry with only1/1 triangle/node allocations after600 updates. Full-screen2560×1440/half-resolution2xAA still measures median166.240ms/frame (~6FPS), GPU159.902ms; GPU traversal work remains the priority. Restored small window/F10. No target-FPS claim.

## Faster geometry bounds checks — 2026-09-19

Shared chord reciprocals and vector slab tests preserve existing geometry/optics/AA. Build51 tests and2880 sampled independent CPU/GPU hit checks pass; paired images are pixel-identical at854x480 and2560x1440. Matched frozen GPU p95 improves32.580 to30.121ms small-window and120.778 to111.447ms at1440p (7.7%). The latter still gives about9FPS median frame rate; this is a limited arithmetic improvement, not the target. Scene actor counts differ from the prior checkpoint; no cross-session speedup claim. F9 T toggles original/fast bounds, Ctrl+Shift+P compares them. Details: docs/aa-performance.md. Continue profiling larger traversal costs; packaging remains paused.

## Faster exact mesh addressing — 2026-09-19

Fixed-width integer texture addressing preserves exact texel selection while simplifying GPU arithmetic. Build51 tests and5760 sampled CPU/GPU comparisons pass. Streamed same-frame images are pixel-identical at854x480 and2560x1440; small candidate inspected. Matched1440p GPU p95 improves160.323→135.443ms (15.5%) with2xAA/adaptive/fast-bounds unchanged. Fullscreen optimized frame median133.422ms (~7.5FPS), still far from target; no cross-session or whole-game claim. F9 R toggles addressing, Alt+P compares. Detailed evidence: docs/aa-performance.md. Handoff shortened; larger traversal costs remain next, packaging stays paused.

Final checks:4096-wide monolithic addressing pair is also pixel-identical. Live F10 passed600 changing actor updates with retained1/1 texture allocations. No new live FPS comparison. Client left F10 active in the small window.

## Conservative empty-region reuse — 2026-09-19

Renderer learns empty regions from its existing rejected-subtree checks and skips later geometry searches wholly inside them. Per-ray/per-tree state preserves AA and live geometry; optics and coverage unchanged. Build51 tests and11520 sampled CPU/GPU comparisons pass. Wall pairs at small/1440p sizes and a downward terrain pair are pixel-identical; images inspected. Matched GPU p95 improves161.340→77.698ms at1440p (51.8%), and48.696→31.966ms in the small downward view (34.4%).1440p cached frame median83.334ms (~12FPS), still not the target. Detailed results and scope: docs/aa-performance.md. Initial separate occupancy-search prototype was discarded for a slowdown. Continue measured traversal work; packaging stays paused.

Final live check: changing actors over600 updates and active chunk streaming pass; benchmark confirms cache enabled. Live camera moved to a different radius, so live timings are observational only. F10 remains active; no subsequent camera reset.

## Wider reuse of proven empty space — 2026-09-19

Initial learned-box half-extent rises16→1024, with unchanged conservative clipping/publication rules. Build51 tests and17280 sampled CPU/GPU comparisons pass. Four pairs at wall/down/away views are pixel-identical; away image inspected. Same-quality GPU p95 improves84.342→61.369ms at1440p wall view (27.2%); small downward32.869→25.705ms (21.8%), away17.566→14.787ms (15.8%). Fullscreen median frame66.670ms (~15FPS), not target FPS. F9 Shift+I toggles extents; Ctrl+Alt+P compares. Details: docs/aa-performance.md. Next: occupied geometry traversal; packaging remains paused.

Live F10 confirms the larger default, changing actors over600 updates and retained1/1 texture allocations. Small-window live median frame20.042ms; no matched live speedup claim. Client remains F10 active at the wall pose.

## Renderer review and native shader specialization — 2026-09-19

Reviewed traversal/addressing/shader state and retained the quality-preserving performance plan. Native meshes now compile from shared GLSL with their backend fixed, avoiding the unused voxel backend without duplicating optical code. Build51 tests pass; both executable programs pass17280 sampled hit comparisons each. Three image pairs are pixel-identical; downward candidate inspected. Matched1440p streamed GPU p95 improves61.011→59.059ms, confirmed in reverse order61.424→59.319ms (3.2–3.4%). Small downward terrain has no meaningful timing gain. No new correctness bug confirmed. F9 S/Shift+S switches/compares programs; F10 defaults to native. Detailed scope, measurements and next candidates: docs/aa-performance.md. Next: occupied-geometry search and nearest-hit shading experiments; packaging remains paused.

Final live check confirms native-mesh default, changing actors over600 updates, retained1/1 texture allocations and no renderer failure. Small-wall live GPU p95=19.018ms, frame median18.848ms; no matched live gain claimed. Client left in F10, small window, wall pose; focus loss may pause it normally.

## Performance review checkpoint; optimization continues — 2026-09-19

Retain the established b41b2f2 renderer and sharp2xAA. Surface-area trees give only1.9–3.3% GPU gains at3.7–4.1x CPU build cost; rejected/preserved on codex/terrain-sah-experiment (ba0bd73). Longer empty-space steps regress2.5% against a separately compiled original, confirmed in reverse order; rejected/preserved on codex/empty-step-experiment (220b883). This corrects their misleadingly favourable within-program toggle comparison. No new production speedup is claimed. Latest original wall median frame~49.5ms (~20FPS) at1440p; separate downward scene~82.6ms (~12FPS).60FPS remains unmet. Detailed evidence/stopping rationale and unproven future directions: docs/aa-performance.md.

Retained only the independent analytic capture-boundary diagnostic:320 additional comparisons. Restored build succeeds;51 existing tests pass. Native/general runtime suites each pass17600 checks across340 distinct directions, zero mismatches/inconclusive/unresolved. Native image after diagnostics inspected; no new correctness defect confirmed within reviewed/tested scope. Logs: performance-review-build.log and performance-review-runtime.log. No renderer/resource changes remain from the rejected experiments.

Precipitation assessment complete: defer transparent weather composition pending its own design/measurement; native geometry alone is cheap. docs/precipitation-assessment.md records evidence, a bounded candidate approach and limits without an invented FPS estimate. docs/relativity-feature-ideas.md proposes clocks/devices, orbit probes and light echoes first, plus observatory/tidal/redshift instruments and the existing deeper-relativity roadmap. No gameplay features implemented this pass; packaging remains paused and original AA work remains separate.

## Exact compact nodes — 2026-09-19

Accepted two-texel node records with exact float32 bounds/escape links and a finite bit-packed header; overflow descriptors preserve large leaves.53 build tests pass; compact/original native shaders each pass26240 sampled optical checks including cross-row child pointers and oversized leaves. Three1440p same-frame pairs are pixel-identical; downward image inspected. Matched GPU p95 improves12.4–12.5% wall (50.483→44.246ms, reversed50.784→44.449) and19.6% down (82.063→66.001ms). Medians~23FPS wall/~15FPS down still miss the owner's renewed30FPS minimum. Keep optimizing. Evidence/limits/dual-texture cost: docs/compact-nodes.md.

F9 F switches node layouts; Shift+F compares. Native/live defaults to compact when the shader extension is supported; original fallback retained. Same-renderer comparison captures now support F9 arrow rotation and record actual preview angles; vanilla pairs retain the matching player-camera requirement. Local helper scan codes fixed for Escape/arrows; previously ignored arrow inputs are correctly classified as wall repeats. Live F10:600 changing actor updates, original texture allocations1/1; small-window GPU p95=12.355ms, frame median12.721ms, not a1440p comparison. Final-hit shading was rejected (2.5% slower) and preserved on codex/final-hit-shading-experiment9967e4b. Next: specialize normal live settings without changing their values or quality.

## Normal-setting specialization — 2026-09-19

Accepted compile-time constants for unchanged normal live settings, with automatic dynamic-program fallback for alternate toggles and a dedicated diagnostic variant.53 tests pass; actual specialized-program wall/down images are pixel-identical. Same-scene1440p GPU p95 improves50.242→41.423ms wall (17.6%) and71.616→60.129ms down (16.0%). Diagnostic variant passes26240 sampled checks; post-diagnostic appearance unchanged. Live F10 confirms default specialization,600 changing actor updates and retained original allocations1/1. Median frames~24FPS wall/~17FPS down still miss30; continue. F9 D/Shift+D toggles/compares specialization. Full distinctions/evidence: docs/live-default-specialization.md. Next: cautiously test a larger spatial chord cap while retaining the current curvature tolerance/angular limit.

## Longer curvature-limited segments — 2026-09-20

Accept16-block maximum only where the existing local curvature estimate permits it, retaining nominal1mm tolerance,0.02radian cap, RK4 and all intersections.53 tests pass; diagnostic variant at16 passes26240 sampled comparisons with zero mismatch/inconclusive/unresolved. Wall/down/away image differences over8 levels affect0.0023%/0.0031%/0.0002% of pixels; downward inspected. Matched1440p frozen GPU p95 gains21.3% wall and21.0% downward versus separately compiled4-block reference.32 adds only2% downward and is not retained. V/Shift+V toggles/compares4/16. Detailed limits/evidence: docs/long-chords.md.

Actual live1440p median frame:28.594ms (~35FPS) wall,43.998ms (~23FPS) downward. The wall passes30; the terrain-heavy view still misses the active minimum. No quality/coverage reduction or confirmed new correctness defect. Player restored to small-window wall pose. Next: exact early back-face rejection for suitable terrain triangles; no change to optics or face visibility is intended.

## Separate AA scheduling — 2026-09-20

Accepted two shorter single-ray draws plus full-precision averaging before the existing8-bit reconstruction input. Both AA rays, all geometry and optics remain unchanged;28.125MiB additional temporary storage at1440p.53 tests pass; wall/down/away output pairs are pixel-identical and downward inspected. Matched frozen GPU p95 improves34.3% wall and28.8% down; repeated downward run confirms it. Live1440p median~44FPS wall/~29FPS down: continue for30FPS headroom. C's unchanged diagnostic variant passes26240 sampled checks. Details, exact timings and guard validation scope: docs/split-aa.md. X/Shift+X toggles/compares serial scheduling. Early-facing rejection gave no gain and is preserved separately at codex/facing-hints-experiment7f9fad2. Next: specialize known streamed triangle texture layouts.

## Known streamed texture layouts — 2026-09-20

Accepted fixed4095/4096 triangle addressing for actual streamed terrain/moving meshes.53 tests and matching diagnostic26240 sampled checks pass; wall/down/post-diagnostic image pairs are pixel-identical. Matched GPU p95 saves8.7% wall/10.7% down. Fixed a new experimental selection error that initially enabled this only in F9; now selects from actual WorldMesh streaming state and verified F10 program names. Corrected live1440p frame medians~53FPS wall/~32FPS down; downward frame p95/p99=33.502/34.468ms, so30FPS medians achieved but slow-frame headroom remains limited. No quality/coverage change. Y/Shift+Y toggles/compares; docs/fixed-layout.md has full evidence. Next small experiment: reuse per-triangle row addressing.

## Performance pass completed at diminishing returns — 2026-09-20

Retain a small row-address simplification in the existing streamed shader (14 added/11 removed lines including callers), with no new buffers/programs/controls. Separate experiment09f93cf shows pixel-identical wall/down pairs,26240 sampled checks passing and repeatable2.3–2.4% downward GPU gain. Consolidated final build passes53 tests; live1440p medians~53FPS wall/~34FPS down. Heavy frame p50/p95/p99=29.796/31.358/33.704ms: representative30FPS target reached, occasional slower frames remain,60FPS and a universal minimum are not claimed. Final native screenshot inspected;600 live updates retain1/1 original allocations. Client remains open at original pose/small window with F10 active. Exact evidence: docs/triangle-row.md.

Current quality is unchanged by split AA/fixed addressing/row reuse; measured same-frame pairs are pixel-identical. Earlier16-block adaptive cap has separately documented tiny numerical differences under unchanged nominal curvature tolerance. Further substantial speedups likely need a geometry/traversal redesign, so stop the current pass under the owner's cost constraint. Weather is assessed/deferred in docs/precipitation-assessment.md; visual/functional feature proposals in docs/relativity-feature-ideas.md. Packaging and original AA WIP remain isolated. The new F9-only selection error was fixed before acceptance; no new remaining correctness defect identified in reviewed scope.

## Demo/refinement pass and subsequent algorithm review — 2026-09-22

Completed the requested bounded steps3–4 pass with a matched FPS gate. Separate persistent demo, repeatable views, saved return, source/range HUD and package archive are verified. Stable dev identity fixes restart/return; new-build exhibits add ordinary materials without resetting older layouts. Native fluids, translucent blocks, normal entity/item layers and block entities now participate in the curved scene. Sharp2xAA retained after three alternatives fail quality or performance gates. Special effects/text/particles and the documented material edge cases remain outside the demonstrated subset.

Final build53 tests;52,480 sampled GPU optical checks over680 distinct directions and28 material cases pass. Wider numerical steps initially exposed two close-observer cell mismatches; retain the original settings through4r_s, blending to faster settings at6r_s, with a separate critical-ray guard. Grazing transparent self-hits fixed. Close pair is byte-identical to the fine-step reference; final wall MAE0.000153,0.1021% pixels over8 levels. Actual final screenshot inspected.

Fresh matched live1440p baseline wall~53FPS/down~33FPS; final~59/~34. Final down frame p50/p95/p99=29.698–29.802/31.411–31.652/32.423–32.452ms; all lower than the fresh baseline runs. Resolution, AA and live updates retained. Earlier less consistent tails and discarded mismatched pose are recorded, not hidden. Evidence: docs/material-coverage.md; logs final-baseline-runtime.log and final-quality-runtime.log.

Post-step4 code/algorithm review complete: prioritize native quad pairs to remove duplicated vertex data, then assess table-assisted optical integration, then conservative geometry compression. Memory arithmetic is not a promised FPS gain. docs/performance-review-2026-09-22.md. Step5 remains for later discussion; weather/general teleport remain deferred. Owner original pose restored,854x480 client,F10 active. Original AA and earlier experiments preserved.

## Targeted profiling and revised ranking — 2026-09-23

Added opt-in GPU stage timers, separately compiled full-resolution ray work counters/diagnostic removals and moving-scene CPU timings. Profiling changes no default optics, geometry, AA or animation. At the fixed heavy view, normal GPU median27.0–27.2ms; diagnostic complete moving-tree omission15.9ms, cloud omission22.0ms, actor omission23.1ms. These are image-changing diagnostic removals, not adopted settings or additive savings. Second probe visits447M terrain/215M moving nodes and8.30M terrain/65.09M cloud/13.40M actor triangle entries per frame; entries include pre-intersection material rejection. Reconstruction~.061ms; full/selective materials tied in this capture.

Live moving BVH build~5.9ms CPU, actor capture~1.5ms; CPU/GPU overlap means no equal frame-time saving. Independent JFR heavy subset has320/425 render-thread Java samples in MeshTree. Clean pre-JFR live heavy median GPU27.93/frame29.02ms (~34.5FPS). JFR overlap and slight Shift-flight descent invalidate the last live runs as clean paired FPS evidence; retained/labeled in artifacts, final live profiling shortcut corrected to Ctrl+F12. Details, all estimates and revised ranking: docs/performance-profile-2026-09-23.md. Prioritize separate actor/cloud traversal, then cloud primitives/tighter actor bounds; terrain planar work moves down. No speedup adopted during this task.

Normal quad fixture52,480 comparisons and28 material cases pass. Work-count consistency checks pass for four captures. Instrumented down view reports one exhausted ray at sample1/x509/y554; wall none. Record for production reproduction, not established as a new regression. Original demo pose/dimension/flight restored and N64 F10 view inspected; saved return and blocks untouched. Step5 remains deferred.

Final build57 tests pass. Ordinary launch profile-default-runtime.log verifies extra profiling shaders/CPU logging disabled, N64 demo ready at restored pose; client paused with F10 active for optional exploration. No input helpers remain active.

## Separate moving-tree roots — accepted (2026-09-23)

Implemented the first profiling proposal and measured three variants. Independent actor/cloud caches lose despite54% fewer moving node visits; removing the cloud cache also loses. Adopt separate roots traversed sequentially inside the original moving-tree loop, with the original shared conservative cache and whole-cloud skip after consumption. Native geometry, intersections, AA, materials and live capture frequency retained; no second steady-state representation. F9 W/Shift+W provide layout timing/image comparisons. +387 net source/test/resource lines.

Controlled same-capture down GPU25.525/25.537→24.493/24.528ms (4.00% less time); wall15.105/15.193→14.583/14.667ms (3.46%). Tails improve. Live candidate heavy view~38.8–39.2FPS at1440p, with mobs updating; no matched live-baseline claim. Final frozen capture61 entities, versus~96 in older profiling; comparisons are within each capture. Actor leaf entries now much smaller: native cloud primitive work is next, while tighter actor-bound estimates need rechecking.

59 tests/build, runtime shader compilation,52,480 optical comparisons and84 GPU material cases pass. Heavy wall/down pairs exact; restored-demo pair MAE.00001755, .0125% pixels >8/255 different at overlapping horse-face surfaces. Crop inspected; existing coplanar draw-order limitation remains, not universal pixel equality. Existing instrumented exhausted ray unchanged in both layouts. Detailed decisions, rejected patches and compact evidence: docs/moving-trees.md and docs/profiles/2026-09-23-moving-trees.

Normal non-profile launch split-default-runtime.log verified N64/F10 without errors/instrumentation. Current user demo pose44.2374620427497/65/-7.074375242855579,81.7522/-31.349997,walking restored;854x480,paused,F10 ready. Saved return record and blocks untouched. Earlier AA work remains separately preserved; step5 stays deferred.

## 2026-09-23 — Cloud-face experiments completed, baseline retained

- Implemented two native cloud-face intersection variants on an isolated branch. Repeated same-scene measurements show no useful heavy-view improvement; neither is adopted. Experiment9815c42 is pushed on codex/cloud-quad-intersections. Current codex/cloud-quad-results retains production source exactly at e324e84, with reports/evidence only.
- Specialized downward GPU means of run medians24.432ms baseline/24.455ms candidate; wall14.570/14.514ms. Cloud entries fall60% but moving node visits only0.8%; counts include cheap rejects. Full timings and rejected first version are in docs/cloud-quads.md.
- Experimental62-test build, both52,480-case optical/84-material/192-cloud GPU checks pass. First wall/down and specialized wall/demo image pairs are identical; specialized down MAE0.00000018,0.0002% pixels >8/255. Contact sheet inspected; no blanket pixel-equivalence claim. Existing shared-edge rounding and one instrumented exhausted ray remain documented.
- Restored renderer clean build/packageDemo passes59 tests. Ranking now demotes cloud intersections and tighter actor work; conservative tree-node compression is the next unimplemented proposal. No new live FPS claim or unnecessary live timing after rejection.
- Normal profiling-disabled launch confirms N64/F10 readiness without experimental shader registration or renderer errors. Exact saved position/rotation/dimension/walking mode and854x480 window restored; client paused. Blocks/time/weather/demo return record untouched. Both input helpers completed.

## 2026-09-23 — Original performance proposals1–4 tested

- Completed original item3, per-actor hierarchy, on isolated7a168fb. Down24.446→24.438ms is a tie, wall slightly slower; no useful improvement. Build61 tests,52,480 optical and112 material checks pass; both heavy image pairs byte-identical. Rejected and pushed separately.
- Completed original item4, conservative packed bounds, on isolated e62a71c. Down24.514→29.423ms regresses20.03% GPU time; wall17.1% slower. Build62 tests,52,480 optical and168 material checks pass, including overflow fallback; heavy image pairs byte-identical. Rejected and pushed separately. Compact contact sheets inspected; previous exhausted subpixel persists in baseline/candidates, no optical limits raised.
- Current codex/performance-four-results retains only e324e84 production source. It adds reports/evidence and revised estimates; no new default shader, capture bookkeeping, extra texture or upload overhead. Clean build/packageDemo passes59 tests. Normal launch verifies N64/F10 readiness, no renderer errors, exact original position/rotation/dimension/walking mode and854x480 window. Client paused; all input helpers finished. An initial missed confirmation click was recovered on the same live client with a held click.
- Scope is fixed in docs/performance-experiments-1-4.md. Only separate actor/cloud roots produced a retained gain (~4% less heavy GPU time); cloud faces and per-actor trees tied, packed bounds were slower. Detailed evidence: docs/actor-hierarchy.md, docs/quantized-node-bounds.md. No additional optimization started.

## 2026-09-23 — Gameplay/UX milestone proposed

- Owner moves progressive mass-block effects, automatic source discovery and local mob/projectile dynamics ahead of remaining optimization experiments. docs/gameplay-gravity-plan.md records proposed behaviour, work order, scientific compromises and acceptance; no gameplay implementation is claimed.
- Reviewed the current cluster calculation, five-tick edit debounce and black-hole-only render gates. Derived reference cube compactness values1/16,1/4,9/16,1 using a proposed `sqrt(3)/32` Schwarzschild radius per block. Existing calibration instead makes the 3-cube supercritical. Preserve the legacy demonstration when implementing.
- Proposed shared bounded cluster indexing, finite pre-collapse optical interior work, scaled central-force entity motion, local taper and swept projectile collisions. Exact timelike paths remain a separately validated option; the default would not claim one consistent optical/gameplay metric. Players and terrain are unaffected by proposed scope; horizon capture remains a proposed setting, tested separately from legacy comparison fixtures.
- Documentation-only branch codex/gameplay-gravity-plan retains production source exactly at e324e84. No build, screenshot or client run was needed or performed; no settings or saved-state changes. Checked calibration arithmetic, documentation links and whitespace. Existing performance evidence and known limitations remain unchanged.
