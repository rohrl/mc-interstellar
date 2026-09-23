# Gameplay gravity and automatic sources — proposal, 2026-09-23

## Priority and status

The owner requests gradual mass-block effects, automatic updates, nearby mob attraction and curved projectile paths. Plan this milestone before further GPU profiling, optical-table experiments and moving-tree refit/reuse. The original four performance experiments are complete; retain their accepted renderer.

The owner subsequently accepted this proposal, permitted a small performance cost, and requested close mob lift/capture with an inexpensive visual cue. This document preserves the original design discussion; [gameplay-gravity.md](gameplay-gravity.md) records the implemented behaviour, final choices and measured checks. Player gravity, terrain destruction, accretion, general emission histories and other deeper-relativity features are not added to this milestone.

## 1. Mass and progression

Keep equal mass per block, face-connected clusters and the existing compactness proxy `C = r_s / R`. Here `R` encloses every block corner about the exact mean of block centres; `r_s` is proportional to total block count. Thus shape matters: a spread-out assembly is harder to collapse than a compact assembly of the same mass. This is a spherical approximation, not a solution for nonspherical collapse or stellar pressure.

For reference cubes, propose `r_s per block = sqrt(3)/32 = 0.0541266` blocks. This places a complete 4×4×4 cube at `C=1` using the current enclosing-radius definition:

| Assembly | Blocks | Compactness | Intended appearance |
|---|---:|---:|---|
| Single block | 1 | 0.0625 | Very subtle local bending; no horizon |
| 2×2×2 cube | 8 | 0.25 | Clearly noticeable but mild bending from a few blocks away |
| 3×3×3 cube | 27 | 0.5625 | Stronger bending, still no horizon |
| 4×4×4 cube | 64 | 1 | Horizon threshold |

Intermediate builds use their actual count, centre and radius; there are no discrete force tiers. The examples do not guarantee every possible 63-block arrangement is subcritical. Surface effects at `C=0.25` are not necessarily weak; “mild” describes the intended normal viewing distance, to be checked in-game.

Current calibration is `0.125` per block, which already classifies the 3-cube as a black hole. Recalibration changes the 64-block horizon radius from 8 to about 3.46 blocks. Preserve the existing exhibit and performance fixtures under an explicitly named legacy calibration; use a separate gameplay fixture for the new progression. Calibration must be consistent for every cluster in a given world/preset, not a hidden per-source multiplier. Do not silently change saved demonstrations.

The field should grow with mass before collapse. Crossing the compactness threshold enables the horizon/capture policy, without an extra attraction multiplier. Removing blocks can reverse the proxy classification: this editable construction toy does not model irreversible physical collapse. Use atomic complete scan results and a numerical boundary tolerance, not a wide hysteresis band that changes the advertised threshold.

## 2. Subcritical optics

The current live renderer rejects sources without `blackHoleProxy()`. Supporting the progression requires extended-source optics, not only a new mass constant or removal of that gate.

- Retain the established Schwarzschild exterior for the spherical proxy outside `R`, including native terrain, sky and moving geometry integration.
- Render the mass blocks as ordinary occluding surfaces before collapse. There must be no artificial black-hole capture surface in a subcritical source.
- Rays can enter the enclosing sphere before hitting a cube face, or pass through gaps in irregular clusters. Implement and separately validate a finite interior approximation with continuous boundary behaviour and no singularity or spurious horizon. Do not claim the exterior Schwarzschild vacuum solution is physically correct inside the material distribution. The interior model needs a small mathematical prototype before production shader changes.
- Keep optical accuracy outside that declared approximation. Do not use the entity-force cutoff to truncate light bending or reintroduce an unbent background overlay.

This interior/transition work is the largest new optical uncertainty. A fully self-consistent nonspherical material metric is outside this milestone.

## 3. Automatic discovery and updates

Some infrastructure already exists: mass-block edits and chunk events refresh a manually selected source; scans are bounded and debounced by five ticks (250 ms at 20 TPS). Completed metadata is not rebuilt every tick. The missing work is discovering unselected sources, persistent identification and robust splits/merges/anchor removal.

Propose a world-level index of loaded clusters, immutable source snapshots and per-source revisions:

- Add/remove events mark affected components dirty. Coalesce a short burst of edits, targeting roughly 100–250 ms before starting work, then publish after the bounded scan completes. This is not a guaranteed completion latency for large builds.
- Adding a block can join neighbouring components; removal may split one and requires a bounded connectivity scan. Retain component identity where possible and choose a new valid anchor when the old one disappears.
- Keep exact equal-weight centres: count and coordinate sums are cheap. Recalculate the enclosing radius on the affected component. No approximate centre is needed for 64 blocks.
- Discover existing blocks on chunk load using a bounded loaded-section/index queue, with persisted hints revalidated against the world. Never force-load chunks or scan the entire world each tick.
- “Background” means incremental work within a shared server-tick budget. Do not read mutable Minecraft world state from a worker thread. Share scans across viewers instead of duplicating work per player.
- Keep the last complete optical snapshot briefly while a local edit settles, patching changed block geometry normally. Bound its lifetime; an uncertain/unloaded source must not keep applying indefinitely stale forces. Suspend its entity field when validity is lost, and remove confirmed empty sources immediately.

With lensing enabled, automatically adopt a nearby supported source and recover on leave/re-entry. F10 remains a global on/off control; inspection becomes an optional diagnostic/pin, not a required gameplay step. Show a compact source/count/compactness/pending indicator without chat spam. Prefer a stable current source, with a margin before switching to another, rather than flickering between equally close clusters.

The renderer currently models one spherical source. For this milestone, explicitly support one dominant optical source per local region; do not claim accurate overlapping black-hole lensing. Multiple nearby sources need an explicit policy before entity-force superposition is enabled. Source metadata changes should reuse the captured world view, applying ordinary local geometry edits rather than restarting the full initial capture.

## 4. Local motion: recommended gameplay model

Start with a server-authoritative central acceleration proportional to source mass and inverse-square distance in the exterior. Use a finite interior force for extended sources, and preserve vanilla downward gravity, collisions and drag. Document this as scaled Newtonian gameplay dynamics, not exact relativistic motion near a horizon.

Use a separate, explicit gameplay strength parameter. A metre-scale Schwarzschild radius and the real speed of light imply enormous accelerations through `r_s = 2GM/c²`; readable walking and bow shots cannot simply use those SI values. Merely lowering a simulated light speed is also not a complete solution while keeping arbitrary vanilla projectile speeds. This proposal preserves the optical metric and mass ratios but deliberately does not claim a single consistent physical spacetime for both the renderer and gameplay motion. Keep this setting separate from the planned SR observer-speed feature.

Initial influence distances, measured from the centre, are tuning candidates:

- 8-block cube: an outer reach around 7 blocks.
- 27-block cube: around 13 blocks.
- 64-block cube: appreciable pull inside roughly 12 blocks, smoothly fading to zero by 20 blocks.

Choose a smooth radial taper in the outer part, so crossing the boundary gives no sudden kick. These are finite-range gameplay approximations; real gravity has no such cutoff. They apply to the 1–64-block reference builds. Before supporting much larger masses, define a supported-size limit/range policy: never silently clip an actual horizon to a force-radius cap.

Tune strength by behaviour rather than promise an untested coefficient: near the outer range a walking mob can continue on its intended route; closer in it drifts or struggles; very close, attraction dominates. A fast grazing arrow bends but can escape. Preserve tangential momentum; do not force every entity into a spiral or steer its heading directly at the centre. The same gravitational acceleration applies to test bodies regardless of their own mass; AI, traction and drag explain different responses.

### Projectiles first, then mobs

Use ordinary arrows as the first motion fixture, then straightforward thrown projectiles. Their free flight is easier to validate than walking AI. Integrate velocity with bounded substeps where curvature or speed requires them, checking swept collisions on every segment. Test paths that enter and leave the field within one tick; checking only the start position would miss fast projectiles. Preserve ownership, damage, hit ordering and ordinary drag. Defer homing/returning projectiles until their steering interaction has an explicit rule.

For mobs, retain navigation and desired movement, adding the source field to actual motion. Ground contact/friction, jumping and swimming are forces/constraints, so a walking mob is not freely following a geodesic. Test AI cancellation of pull, sliding, wall contact and airborne transitions; a naive velocity injection may be overwritten by the normal movement code. Players, spectators and creative builders are unaffected by default in this first milestone.

Proposed capture policy: an extended source attracts entities onto its ordinary block surfaces; a black-hole horizon captures crossing mobs/projectiles. Define capture initially as removal through the normal server entity lifecycle, without terrain damage, extra mass feedback or an item-drop cascade. Expose capture as a setting, test it in disposable fixtures and leave entity gravity disabled in the legacy comparison exhibit. General relativistic delayed/frozen images at the horizon remain deferred; disappearance is an explicit visual approximation.

### What about actual geodesics?

Freely falling matter follows timelike geodesics; light follows null geodesics. Our light-ray integrator is not an arrow-motion solver. A small test-particle integrator in the fixed spherical metric is feasible without solving the Einstein field equations on the server. Use an independent reference, horizon-regular coordinates, bounded work and a defined time/velocity conversion if we pursue it.

However, exact geodesics alone do not solve AI propulsion, collisions, vanilla drag or the optical/gameplay scale mismatch. Recommend the scaled central-force model for the first playable milestone. Keep a physically consistent ballistic-probe experiment as a separate possible follow-up if its near-hole trajectories visibly improve the experience; do not label the default approximation “exact GR”.

## Delivery order and acceptance

1. **Automatic sources and calibration:** discovery, shared edit queue, split/merge/reload handling, reference cube tests and a small status display. Preserve the legacy demo configuration.
2. **Pre-collapse lensing:** finite interior prototype, progressive reference builds and correct horizon transition. Verify representative views and native-world integration once the feature is substantial.
3. **Local projectile field:** strength/range tuning, arrows first, swept hits, continuity and zero-field equivalence. Validate the central-force model against an independent Newtonian reference; do not reuse optical validation as evidence for matter dynamics.
4. **Mob integration and capture:** AI/ground/air behaviour, supported projectile coverage, explicit capture setting and complete build/remove/re-enter UX. Ordinary source and entity correctness tests remain required; owner-reported camera flicker testing stays deferred as agreed.
5. **Resume performance work:** targeted integration-cost measurement; conditional optical-table prototype with exact critical-ray fallback and retained terrain intersections; moving-tree refit/reuse for CPU headroom. These remain experiments with unmeasured gains.

Each delivery needs relevant bounded-work tests and a declared server tick budget, including edit bursts and dense entity populations. Start by targeting under 1 ms of added server work per tick in the reference scene, then measure; a work-count limit and overload policy are required rather than relying on elapsed-time guesses alone. Broad-phase entity queries must be local and must include fast swept paths. Avoid silent skipped collisions/physics under overload.

Retain the existing FPS constraint: compare matched scenes/settings with the accepted renderer, including the prior heavy view, and reject material regressions or report a tradeoff before adoption. A smaller rebalanced horizon or fewer mobs is not proof of an optimization. Do not trade away AA, native world appearance or live animation. No performance improvement is claimed for this proposal; no client run is needed for this documentation change.

## Scientific references

- [Einstein Online: Schwarzschild radius](https://www.einstein-online.info/en/explandict/schwarzschild-radius/) — spherical radius/mass criterion; the cube calibration above is our derived game proxy.
- [Einstein Online: Gravity, from weightlessness to curvature](https://www.einstein-online.info/en/spotlight/geometry_force/) — free fall, test-body acceleration and geodesics; walking constraints and gameplay design above are our application.
- [Carroll: Lecture Notes on General Relativity](https://arxiv.org/abs/gr-qc/9712019) — reference foundation for a future timelike solver, not validation of an implementation here.
