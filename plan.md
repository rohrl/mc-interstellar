# Project plan

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
