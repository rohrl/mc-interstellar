# Project plan

## Current priority — world feature integration (2026-09-24)

The owner authorizes emissive/glint layers, Glowing-status outlines, entity shadows,
text, selection/mining overlays, cheap local rain/snow, actual returning player-body
images, and gravity for tridents/fishing/leashes. This supersedes the older weather
and actual-body deferrals below for this bounded implementation. Delayed emission
history, terrain crossing and the independent observer-speed feature remain deferred.
See [world features](docs/world-features.md) and [current coverage](docs/minecraft-coverage.md).
Then resume targeted GPU measurement, table-assisted optics and moving-tree reuse/refit.

## Current priority — gameplay and automatic gravity (2026-09-23)

The first four performance experiments are complete. The accepted gameplay milestone implements progressive mass-block lensing, automatic source discovery/updates, local mob attraction and curved projectile paths, including lift and horizon capture. The September24 follow-up doubles local reach, adds an isolated automatic arrow course, strengthens the approach cue and fixes small-source sky rings. See [implementation and checks](docs/gameplay-gravity.md); the owner permits a small performance cost for this milestone. Next resume targeted GPU measurement, optical-table experiments and moving-tree refit/reuse. Physical delayed-light/horizon slowing, player gravity, terrain destruction and deeper relativity outside this explicit scope stay deferred.

## Product requirements

- Accumulated fixed-mass blocks control an effective gravitational source; compactness governs an explicit approximate collapse rule.
- Faithful Schwarzschild optics first, including guided horizon crossing.
- Demonstrable lensing, multiple images, source-aligned Einstein rings, and photon-sphere views.
- Actual player body/skin available to returning light paths.
- Independent potion-controlled SR observer speed, with separate SR/GR scales.
- Educational HUD; effects individually switchable; configurable hardware load.
- No destructive world physics. User delegates graphics/physics engineering and permits a better stack if evidence warrants it.

## Iteration 0 — Reproducible foundation

- Gradle wrapper, Java 21, pinned Fabric/Minecraft dependencies.
- Client/common source separation; diagnostic HUD and virtual reference source.
- Mathematical reference quantities, configuration validation, automated checks.
- Versioned decisions, roadmap, scientific assumptions, development guide, handoff.
- Acceptance: successful build/tests, tracked runtime verification status, branch pushed.

## Iteration 1 — Horizon-capable optical experiment

- Define coordinates, local tetrad, ray orientation, units, and emission boundary conditions.
- Independent high-accuracy CPU reference ray solver for tests/offline comparisons; production light propagation on GPU.
- Minimal controlled sky/light-source scene; correct zero-mass limit and capture/escape behaviour.
- Cross-check radial null rays, null constraint, critical impact parameter, weak deflection, and step-size convergence.
- Shader compile/link and in-game image checks on this machine; benchmark 1440p baseline and optical pass.
- Decide Iris integration vs mod-owned renderer based on actual scene access and profiling.
- Acceptance: exterior and interior images are continuous at the horizon; no unsupported static camera inside; no inaccurate claim that validation covers arbitrary terrain.

## Iteration 2 — Sources and demonstrations

- Mass block/item registration, event-driven persistent clustering, split/merge/unload handling.
- Explicit cluster radius and compactness rule; source data immutable for rendering.
- Guided distant-lensing, hovering, photon-sphere, and free-fall tours.
- Actual stationary player body ray intersections using skin; assess resolution/visibility.
- Acceptance: no unbounded tick work, no deleted terrain, reproducible reference views.

## Iteration 3 — Minecraft scene integration

- Evaluate additional scene captures versus GPU geometry/voxel data.
- Correct source/terrain depth ordering; transparent geometry policy; camera-relative precision.
- Document missing-surface fallbacks; verify with terrain occlusion test scenes.
- Profile quality presets; disabled features skip unnecessary passes and allocations.

## Iteration 4 — SR feature

- Potion and observer controls, beta/rapidity UI, velocity direction independent of walking.
- Lorentz aberration, documented spectral reconstruction, Doppler/beaming in linear radiance.
- Separate physical effects from exposure/bloom; defined local-frame composition with GR.
- Validate identity, inverse transforms, head-on Doppler limits and known reference images.

## Follow-ups

Accretion disk, dynamic entity/player histories, higher-order image antialiasing, Kerr rotation, CMB spectrum, multiple-source approximations with clear validity limits.

## Performance acceptance

Owner override (2026-09-17): the next world-integration experiment is quality-first. Establish convincing integration before optimizing toward the FPS target. Record costs and bound resources, but do not reject this reference path merely because it is slow.

2560x1440 / 60 FPS is a target, not guaranteed yet. Record actual framebuffer resolution, render distance, world/scene, mod versions, preset, GPU/CPU times, frame-time percentiles and warm-up. A capped 60 FPS counter is insufficient evidence. Do not silently switch off effects to claim success.

### Iteration 1 checkpoint (2026-09-14)

Exterior controlled-sky lab implemented and visually checked; nine reference tests pass. Independent horizon-regular integration, quantitative GPU comparison, timings and horizon crossing remain open. This checkpoint does not satisfy full iteration 1 acceptance.

### Horizon-sky checkpoint (2026-09-14)

Independent PG reference and GPU free-fall sky crossing implemented and tested. Targeted critical-ray/winding accuracy, radiometric transport, quality/configuration work and broader performance checks remain. Mass blocks/exhibits remain iteration 2; terrain and hidden-geometry rendering remain iteration 3. Routine in-game validation is agent-operated; user attendance is optional.

### Settings checkpoint (2026-09-15)

Persistent optical defaults and three integration-quality levels implemented and measured. Ordinary render work skips diagnostics unless requested; lensing-off bypasses integration. Critical-ray/winding validation remains open. Proceed toward the source-block/clustering milestone while retaining those numerical limits in documentation.

### Source-data checkpoint (2026-09-15)

Mass block/item and bounded on-demand component inspections implemented and verified. Chunk persistence saves the blocks, and fresh inspections reflect splits/merges/reloads. Live indexing, source-to-renderer data, guided source selection and actual-body images remain unfinished in iteration 2. Terrain rendering remains iteration 3.

### Source-selection checkpoint (2026-09-15)

Completed inspections now synchronize to the client, and F8/S adopts an eligible spherical source's scale and starting camera radius. Revision/disconnect invalidation and extended-source rejection were runtime checked. Guided exhibits, actual player body, maintained clustering, and critical-ray/winding work remain unfinished. Terrain remains iteration 3.

### Immediate demo priority (2026-09-15)

Bring forward iteration 3's scene-data experiment: one selected source and bounded opaque Minecraft terrain, with on/off comparison and explicit missing-data behavior. Actual-body exhibits follow this playable mass-block demo. Critical diagnostics now expose numerical limits; further accuracy work remains tracked without blocking the scene-data prototype indefinitely.


### Terrain prototype checkpoint (2026-09-15)

F9 renders actual textured opaque-block snapshots, curved-ray intersections and off-screen geometry for one selected source. On/off comparison, foreground occlusion, flat-hit validation and initial timings are implemented. This completes the initial scene-data spike, not iteration 3: live camera/update integration, finite-surface convergence, natural-terrain/model coverage, transparency and filtering remain. See docs/terrain-prototype.md.

### Live-camera checkpoint (2026-09-15)

F10 integrates moving-camera terrain with bounded periodic recapture and the vanilla HUD. F9 stays frozen. This is an explorable exterior preview, not full scene/model coverage or relativistic player motion. Next prioritize unsupported terrain models (snow is visible in the test scene), secondary-image filtering and independent finite-surface accuracy. See docs/live-terrain.md.

## Current delivery order — owner reaffirmed 2026-09-15

**Current owner instruction (2026-09-22):** resume and complete through step4, preserving the current FPS baseline79aa1d0. Finish demo packaging, then visual/model coverage and secondary-image refinement. Keep step5 for a later discussion. After step4, review code and algorithms again and measure faster alternatives; minor barely noticeable visual approximations are allowed for worthwhile FPS gains. Existing weather/teleport deferrals remain. Compare the same scene/pose/resolution and frame-time percentiles; a simpler demo scene cannot establish performance parity with the existing wall/terrain benchmark.

**Owner update (2026-09-18):** current FPS is acceptable for the demo. Demo packaging is now explicitly paused in favour of AA and low-hanging performance improvements. Preserve appearance; do not turn this into a broad FPS rewrite. Unverified packaging work is isolated on codex/demo-packaging-wip (2fe8674), excluded from the running build. Return to packaging after this AA/performance checkpoint unless the owner redirects.

**Latest owner update (2026-09-19):** visuals are accepted, but FPS is now the priority. Continue measured performance improvements with minimal quality/fidelity loss; packaging stays paused. Keep identical-scene image and numerical checks alongside timings. This supersedes the earlier decision to defer FPS work.

This order takes precedence over older checkpoint suggestions above. Diagnostics should support a usable demo, not indefinitely postpone it.

| Step | Status and remaining work |
| --- | --- |
| 1. Stable exploration | Implemented: unrelated-chunk stability, viewing-limit recovery and bounded automatic metadata refresh. Splits follow the inspected anchor; removed/unloaded/extended states pause and recover. See docs/source-refresh.md. |
| 2. Wider useful viewing range | **Core implemented:** native streamed terrain, live mobs/clouds, and camera access up to256 blocks with automatic recovery. Independent synthetic curved-mesh checks now pass; arbitrary geometry/material limits remain explicit. Ordinary-screen background compositing remains rejected. |
| 3. Demo packaging | Resumed2026-09-22: finish isolated repeatable exhibit/setup, return flow, controls and source/range/loading presentation; package the mod and instructions. Preserve the owner's world. |
| 4. Visual refinement | Sharp2xAA is implemented/verified. Improve difficult secondary images and broaden block/entity coverage, including fluids/transparency/special layers and block entities, with a performance gate against79aa1d0. |
| 5. Deeper relativity | Deferred for owner discussion: terrain horizon crossing, actual player-body images, then independent observer-speed feature. Sky-lab crossing does not complete terrain crossing. |

### Coherent world rendering — next implementation proposal (2026-09-16)

Use one ray/first-hit result per pixel across local and extended terrain. Do not composite the ordinary camera image into missing rays: this duplicates original-position objects and breaks occlusion. Multiple images must arise from distinct curved light paths, not mixed cameras.

Evaluate a bounded, client-side chunk/section geometry cache with hierarchical empty-space traversal, retaining the existing curved-ray solver and independent hit reference. Incrementally capture loaded geometry and invalidate changed/unloaded sections. Measure memory, upload work and traversal cost before selecting coverage/quality defaults. Increasing the dense 96-cubed array alone scales cubically and does not solve continuous exploration.

Continue rays into extended geometry with consistent first-hit ordering. Finite loaded-world coverage remains a declared limit; this proposal does not promise infinite scene data or complete clouds/entities/transparency. The owner explicitly permits distant-scene approximations with small or hard-to-notice errors and substantial measured performance gains; exact traversal of all distant terrain is not a prerequisite.

Acceptance scenes: orbit the coloured wall (no extra straight-view copy), move the wall across cache boundaries, test a foreground occluder outside the old capture, compare zero-mass/straight rays with geometry references, check near/far curved hits against the independent solver, exercise edit/unload/reload, and measure frame times/memory at 1440p. Correctness and visual acceptance precede demo packaging. Architecture is proposed, not yet implemented or performance-certified.

### Practical world integration — required for the working demo (2026-09-16)

**Owner priority update (2026-09-18):** initial loading time is acceptable for v1; teleport support is out of scope. Deprioritize rain/snow. Next bring the accepted native mesh appearance into live exploration and unfreeze mobs, then implement incremental terrain edits/chunk streaming for ordinary movement. AA remains after integration and demo packaging. The first live checkpoint may retain terrain captured at activation, but must clearly expose that limitation; it does not complete live terrain updates.

Accuracy remains the reference, but coherent appearance and usable frame times are product requirements. A grid-free static screenshot alone does not pass. The working demo must blend into the surrounding world during walking, orbiting and approach/retreat, without obvious duplicate geometry, seams, popping or foreground leaks. Do not postpone this to post-demo refinement or require an impractical exact whole-world renderer.

Prototype a hybrid before committing to the full sparse-cache architecture: accurate local curved-ray intersections; cheaper distant geometry or depth-aware directional captures reached by the outgoing ray; reduced distant detail/update frequency where motion tests support it. Investigate cheaper propagation in weak-field regions, with error compared against the reference. These are candidates, not validated speedups. Distance alone is insufficient: nearer occluders, parallax and strong deflection can expose an approximation. Prevent the local scene from reappearing as an ordinary-camera background copy; account for capture depth/origin and ownership across the transition.

Choose the simplest approach that passes moving-view acceptance and the established 1440p performance target. Measure total frame intervals, GPU costs, capture/update spikes and memory, alongside approximation errors in near/far views. Record each accepted approximation and its visible limits. Keep strict missing-data/reference views as diagnostics; select an intentional, tested distant/unloaded-world presentation for the demo instead of silently claiming unavailable geometry is empty. Maintain a comparison route to the accurate local/reference implementation.

Height-field prototype checkpoint: see docs/distant-prototype.md and D032. The experiment is opt-in and does not satisfy this acceptance gate. Next resolve the visible sky/fog boundary and assess camera-relative near/far geometry before adopting a demo default; preserve measured comparison and diagnostic paths.

Owner's subsequent visual rejection changes the immediate next gate: establish Minecraft appearance parity and deterministic vanilla/zero-bending image comparisons before another distant approximation. The first same-frame capture/comparator and native sky/light repair now run (docs/native-appearance.md, D034); parity and broader pose coverage remain unfinished. Then develop a bounded slow lensed reference and automated fast/reference comparisons. Owner deferred automated movement/flicker checks. See docs/visual-reference-plan.md and D033.

Incremental terrain checkpoint (2026-09-18): F10 now uses independently replaceable chunk BVHs and a top-level index. Native invalidations queue block/light changes; movement retains overlapping chunks and queues new edges. Selected-source metadata refresh retains the terrain cache. Same-pose streamed/monolithic candidate images are byte-identical. See docs/streaming-terrain.md. Next address remaining viewing-range/coverage limits and independent curved-mesh validation, then demo packaging; finer updates and cache reuse across F10 toggles remain useful follow-ups. Rain/snow and teleport remain deferred.
