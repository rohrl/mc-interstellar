# Scientific model and references

This document separates physical identities from chosen gameplay approximations. Equations are not evidence that an implementation has been validated.

## Conventions

- Initial physical model: isolated, stationary, uncharged, non-rotating Schwarzschild source.
- r_s = 2GM/c^2. Use dimensionless coordinates r/r_s in optical calculations when practical.
- Photon sphere r_ph = 1.5 r_s; marginal stable timelike circular orbit r_ISCO = 3 r_s.
- Critical impact parameter b_crit = (3 sqrt(3)/2) r_s. This is not a physical surface radius or the near-observer angular shadow radius.
- For a static observer outside the photon sphere, sin(alpha_shadow) = b_crit sqrt(1-r_s/r)/r for a distant luminous background. Inside the photon sphere the angular branch differs; never reuse asin blindly. Static observers do not exist at/inside the horizon.
- Bootstrap HUD's Euclidean camera-to-centre distance is a proposed coordinate embedding, not GR proper distance. It applies no metric or observer transformation.
- SR beta = v/c must be finite with |beta| < 1. gamma = 1/sqrt(1-beta^2). Forward head-on frequency ratio D = sqrt((1+beta)/(1-beta)). Full directional transforms and spectral radiance are future work.
- Do not use a generic RGB tint for exact spectral Doppler. Minecraft RGB spectra are underdetermined.
- Never present photon sphere as a glowing shell; Einstein rings need source alignment; multiple images come from different light paths, not reflections.
- A physical free-fall horizon crossing is optically continuous. Hovering and falling observers at equal radius see different skies.

## Chosen approximations

One mass block will add 0.125 coordinate blocks to r_s; mass is additive by design. Binding energy, pressure, real collapse, terrain gravity and astrophysical matter dynamics are not simulated. Cluster radius/collapse treatment needs an explicit shape rule. Strong-field Schwarzschild metrics cannot simply be summed for multiple sources.

GR exhibit scale and SR speed scale are independently configurable. A later combined mode must define a coherent local observer frame. Playback slowing must not be mislabelled physical time dilation.

Stationary actual-player-body demonstrations can omit motion history because the geometry is time independent. Moving player-body images require past poses and emission times. Static Minecraft terrain behind an interior observer is not a physically valid stationary interior material model; controlled interior boundary conditions must be defined before the tour is claimed physical.

## Primary references (consulted 2026-09-13)

1. [Bruneton, Real-time High-Quality Rendering of Non-Rotating Black Holes (2020)](https://ebruneton.github.io/black_hole_shader/paper.pdf). Precomputed exterior light-path method; explicitly excludes inside-horizon views. Use as exterior reference, not a complete interior solver.
2. [James, von Tunzelmann, Franklin & Thorne, DNGR (2015)](https://arxiv.org/abs/1502.03808). Kerr ray-bundle rendering; distinguish physical transport from film presentation choices.
3. [Perlick & Tsupko, Calculating black hole shadows (2021)](https://arxiv.org/abs/2105.07101). Shadow geometry, observer dependence, analytic benchmarks.
4. [Hamilton, Journey into a Schwarzschild black hole](https://jila.colorado.edu/~ajsh/courses/insidebh/schw.html). Observer-dependent crossing and hovering views; illustrated grids are explanatory overlays.
5. [Hamilton, Schwarzschild geometry](https://jila.colorado.edu/~ajsh/courses/bh/schwp.html). Coordinate choices and free-fall geometry.
6. [Nemiroff, Approaching the Photon Sphere](https://www.phy.mtu.edu/bht/gotops.html). Idealized returning light from one's own head at the photon sphere. Not a generic crossing effect.
7. [MIT OpenRelativity](https://gamelab.mit.edu/research/openrelativity/). Real-time SR, light-runtime effects, Doppler and beaming.
8. [ESA, Planck and the CMB](https://www.esa.int/Science_Exploration/Space_Science/Planck/Planck_and_the_cosmic_microwave_background). Approximately 2.73 K blackbody background; no arbitrary 0.99c whiteout.

## Validation ladder

1. Analytic units and limits: zero mass, known radii, SR identity/inverse/head-on shifts.
2. Independently integrated reference rays: null invariant, radial solutions, capture boundary, weak deflection, convergence with tolerance.
3. GPU comparison to reference, including horizon crossing and finite observer radii.
4. Calibrated images with known emitters and explicit camera/worldline conventions.
5. Minecraft scene comparisons: hidden surfaces, returning body rays, transparency, depth and scale.
6. Performance and temporal stability at declared settings.

## Exterior lab implementation (2026-09-14)

The camera is stationary at +z, looks toward the centre with a 70-degree vertical field of view, and samples an illustrative sky at infinity. In r_s=1 units, integrate u'' = 1.5 u^2 - u with u=1/r. For outward radial local direction cosine mu, initialize u'=-mu*u*sqrt(1-u)/sqrt(1-mu^2). This follows the static orthonormal observer basis and planar null-ray equation in Bruneton sections 3.1–3.3 (reference 1). The invariant is u'^2+u^2-u^3. Capture terminates at u>=1; escape is u<=0, with linearly interpolated exit angle.

The GPU uses 800 steps of 0.02 radians maximum. Unresolved rays are magenta. CPU tests check the flat limit, analytic capture boundary including the inner photon-sphere branch, weak deflection and step refinement. The CPU implementation shares RK4 with the GPU: it is not an independent numerical solver, and these tests do not measure GPU floating-point error. No interior observer is supported. The zero-lensing comparison bypasses integration entirely.

RGB sky colours and the extended orange source are illustrative; no gravitational frequency/intensity transport is applied. Point sampling aliases fine stellar and higher-order structures. Beam filtering, quantitative GPU readback, independent horizon-regular reference and GPU timings remain required. The bright ring is source alignment, not a luminous material shell.

## Free-fall extension

The earlier exterior-only description is superseded for the falling-frame lab by [the PG reference, observer definitions and GPU extension](free-fall.md). That document specifies signs, boundary conditions, independent integration, playback law, observed errors and remaining limits. The sky renderer now crosses the horizon; terrain, spectral transport and emission histories remain outside current validation.

## Mass-block source proxy

[Mass-block model](mass-blocks.md) defines equal weights, corner-inclusive enclosing radius and the explicitly approximate compactness classification. It does not solve nonspherical collapse or generate a dynamical spacetime. The gameplay milestone adds automatic discovery, shared updates, extended-source optics and local entity motion; its current calibration supersedes the original mass-block defaults outside the legacy exhibit.

The accepted [gameplay design](gameplay-gravity-plan.md) derives `r_s/block = sqrt(3)/32` to put a complete 4-cube at the proxy threshold. [The implementation](gameplay-gravity.md) specifies the finite rational-lapse interior, its independent affine reference, scaled Newtonian entity motion and current verification status. Finite force reach, independent gameplay strength and the red/dimming capture cue are explicit departures from a single physical metric and spectral transport. [Einstein Online's radius definition](https://www.einstein-online.info/en/explandict/schwarzschild-radius/) supplies the spherical criterion; [its free-fall/geodesic account](https://www.einstein-online.info/en/spotlight/geometry_force/) explains why freely falling projectiles and constrained walking mobs need different treatment. Our cube threshold, interior lapse, force range and gameplay choices are project assumptions, not claims made by those sources.

## Critical-angle validation

See [critical-ray diagnostics](critical-rays.md) for full-angle and boundary stress checks. CriticalRays solves b_c^2(1+mu/sqrt(r))^2=r^2(1-mu^2), with b_c^2=27/4 and the branch approaching the photon orbit. The static result follows the shadow formula; the falling result follows its local Lorentz transform. [Bozza, Gravitational lensing in the strong field limit (2002)](https://arxiv.org/abs/gr-qc/0208075) establishes logarithmic strong-deflection divergence for spherical metrics. Our finite-observer Schwarzschild test checks the asymptotic increment ln(10) when the horizon looking-cosine offset shrinks by a decade; it does not apply an infinity-to-infinity lens equation directly to the falling camera.
## Finite Minecraft terrain

[Terrain prototype](terrain-prototype.md) extends the spatial orbit integration to ordered finite voxel intersections in a documented areal-coordinate embedding. Foreground material terminates a ray before any later background hit. Textures and directional shading are illustrative radiance; spectral transport is not implemented. The camera is static and exterior in this prototype. See that document for bounded-volume and straight-chord limitations, distinct from the sky lab's horizon-capable observer.

Technical APIs checked against the pinned 1.21.1 family: [BakedQuad](https://maven.fabricmc.net/docs/yarn-1.21.1%2Bbuild.3/net/minecraft/client/render/model/BakedQuad.html), [SimpleFramebuffer](https://maven.fabricmc.net/docs/yarn-1.21.1%2Bbuild.3/net/minecraft/client/gl/SimpleFramebuffer.html). Atlas data remains Minecraft's runtime resource; no game textures are copied into this repository.
## Finite-surface independent diagnostic

[Curved terrain checks](curved-terrain-validation.md) derive and implement an affine-parameter radial system from the Schwarzschild metric and constants of motion. Adaptive DP5(4), independent cuboid slabs and paired chord/tolerance refinement compare sampled finite hit cells to the production inverse-radius GPU solver. Shared scene/embedding and finite-chord limitations remain explicit; passing samples do not certify arbitrary critical rays or radiometric transport.

The additional [native-mesh capture-boundary fixture](mesh-ray-validation.md) uses the analytic Schwarzschild static-observer relation `b/r_s = sin(alpha)/(u*sqrt(1-u))`, where `u=r_s/r`, and critical `b²/r_s²=27/4`. It samples incoming rays on both sides at four exterior observer distances, excluding the exact critical direction. It tests capture/escape classification only; it does not certify outgoing angles, all near-critical directions or arbitrary material transport. No numerical orbit from the production shader supplies its expected classifications.

[Relativity feature proposals](relativity-feature-ideas.md) use the Schwarzschild/observer framework as their starting point; [Carroll's general-relativity lecture notes](https://arxiv.org/abs/gr-qc/9712019) are a primary foundation for future derivations and validation. Clock transport, timelike probes, travel-time signals and a later Kerr model need their own assumptions and tests before implementation. Their appearance in the proposal list is not scientific validation or a claim of current support.

## Current native-scene approximation and further performance work

The [horizon access update](horizon-body-study.md) adds a smooth static-to-falling
optical frame, regular horizon crossing, a straight-aim interior editing layer,
and an explicit central background cutoff. Interior matter and the frame transition
are presentation choices, not a stationary physical source or player-motion model.
The existing [free-fall derivation and primary references](free-fall.md) supply the
PG initialization; the independent PG-time solver checks sampled outgoing directions.

The material-refinement default retains the existing inverse-radius equation and triangle intersections. It uses the established0.02-radian angular cap/nominal1mm chord target through observer radius4r_s, smoothly transitioning to maxima0.08/4mm at6r_s. Near-critical impact-squared within0.005 of27/4 retains the0.02 angular cap. The16-block spatial cap and two AA rays remain. These are numerical heuristics, not global error bounds. [Material evidence](material-coverage.md) records sampled independent checks, image errors and limits; transparent composition approximates native surface blending, without water refraction, spectral transport or emission histories.

[Bruneton, Real-time High-Quality Rendering of Non-Rotating Black Holes (2020)](https://arxiv.org/abs/2010.08735) demonstrates precomputed optical tables for disc/background-star rendering. The [post-refinement review](performance-review-2026-09-22.md) proposes investigating table-assisted orbit integration with retained terrain intersections. This is an unimplemented adaptation, not a claim that the paper provides constant-time arbitrary-terrain rendering or that its FPS transfers to Minecraft.


## Illustrated account and hardware-intersection probe (2026-09-24)

The [illustrated guide](visual-guide/interstellar-visual-guide.html) separates the
Schwarzschild optical model, finite-world embedding, radius-dependent observer-frame
transition, material approximations and gameplay overlays. Primary explanatory sources
include [Perlick and Tsupko's shadow review](https://arxiv.org/abs/2105.07101) and
[Chang and Zhu on freely falling observers](https://arxiv.org/abs/1911.02190).
The latter's horizon angular size applies to the specified observer state, not every
Minecraft trajectory. Its educational interactive RK4 ray is not a production test.

The [RTX probe](rtx-probe-2026-09-24.md) uses
[Khronos Vulkan ray queries](https://docs.vulkan.org/guide/latest/extensions/ray_tracing.html)
for ordinary straight-segment/triangle intersections. It does not move geodesic
integration into RT cores or validate a new spacetime model. Replay segments are
precomputed, continue after scene hits, and see opaque synthetic geometry. The
reported speedups therefore apply only to that isolated workload. Five triangle-edge
rounding differences are retained and documented; no complete numerical equivalence
or production FPS gain is claimed.

## Invocation clocks and performance inference (2026-09-28)

The [current-renderer profile](rtx-bottleneck-profile-2026-09-28.md) uses
[ARB_shader_clock](https://registry.khronos.org/OpenGL/extensions/ARB/ARB_shader_clock.txt).
The specification defines invocation-local observations in undefined clock units,
not nanoseconds, and makes clock reads code-motion barriers. Summed invocation
latencies do not directly measure GPU elapsed-time fractions or occupancy.
We compare coarse/detailed instrumentation, separately time its overhead, verify
float colour equivalence, and use normal shaders for frame/stage timing. Sparse
masked-ray attribution proves less stable and is not treated as an exact removable
cost. No Nsight instruction/stall sampling was performed.

The geometry-search dominance supports a representative RTX experiment; conditional
Amdahl scenarios remain inferences with explicitly assumed accelerated fractions and
zero extra overhead. No transfer of synthetic query speedups to live FPS is claimed.
Optical equations and scientific model remain unchanged.

## Native RTX replay and GPU sharing (2026-09-28)

The [native feasibility checkpoint](rtx-native-feasibility-2026-09-28.md) records actual
production chords, tree-reuse masks and nearest-hit results, then compares Vulkan
ray queries against a standalone triangle BVH. Alpha-aware exterior initial-query
ratios are 5.66–7.66x; these are not whole-frame speedups. Integration, cache creation,
full compositing and live scene updates remain outside the timed replay. The sampled
double CPU reference shares software BVH topology; analytic material fixtures add
known expected distances. Two-view recording leaves float colours exactly unchanged.

The [GL external objects specification](https://registry.khronos.org/OpenGL/extensions/EXT/EXT_external_objects.txt)
defines imported memory and GPU semaphore synchronization. The Windows smoke test
uses those mechanisms in Minecraft's real context: Vulkan clear, GL blit, ownership
return. Measured RGBA8 cycles cost about 0.17–0.18ms wall time, with pixel checks.
This is not full-renderer transfer overhead or portability validation. Future dynamic
geometry must respect the [Vulkan build/update rules](https://docs.vulkan.org/refpages/latest/refpages/source/VkAccelerationStructureBuildGeometryInfoKHR.html)
and be measured separately. No new physical model or production backend is adopted.

## Frozen full-image RTX and proposed wormholes (2026-09-28)

The [complete-image experiment](rtx-full-image-2026-09-28.md) reuses production optics
and material equations while replacing geometry queries. Paired RGB8 images agree
closely, with a few unresolved platform-pixel differences; this is not independent
validation of the underlying geodesics. Explicit cross-API completion and Vulkan
timestamps establish frozen optical-frame savings, not live-world FPS.

The [wormhole feasibility study](wormholes-feasibility.md) preceded implementation. The
stationary Ellis geometry has a smooth signed throat coordinate and a constant time
coefficient. It permits precise light-path and observer calculations within that
hypothetical metric; supporting matter, stability and matching two arbitrary game
regions are separate questions. Its primary sources are
[James et al., visualizing wormholes](https://arxiv.org/abs/1502.03809),
[Nakajima and Asada, Ellis deflection](https://arxiv.org/abs/1204.3710), and the
[energy-condition discussion](https://arxiv.org/abs/2202.07431). No wormhole model has
been assumed physically constructible; the implemented local metric is described below.
## Wormhole model implemented 28 September 2026

The original demo used the symmetric ultrastatic Ellis metric. Its CPU reference
remains available; gameplay now uses the localized model in the next section.
Reference null rays
evolve signed proper radius, its conjugate momentum and a plane angle; independent
elliptic-integral deflections, reversal and invariant tests are in EllisWormholeTest.
The isotropic coordinate derivation and orientation-preserving chart transfer are
documented in [the active implementation note](overnight-goals-2026-09-28.md).
Primary sources: [James et al., equations1,2,16](https://arxiv.org/html/1502.03809),
[Nakajima & Asada, exact deflection](https://arxiv.org/html/1204.3710).
The shared OpenGL/RTX renderer integrates these rays and queries native geometry
at both ends. Seven CPU tests, 2,575 GPU reference rays, paired renderers and
same-physical-camera chart comparisons provide the bounded validation described in
[the implementation report](wormhole-demo-implementation.md). Player crossings
transport the camera frame through the same orientation-preserving differential.
Minecraft controls prescribe the observer's path; massive-body geodesics are not
simulated. Do not present hypothetical supporting matter or the two-mouth world
identification as a demonstrated physical construction.

## Two mouths in a continuous Minecraft exterior (2026-09-29)

The isolated Ellis solution describes two separate asymptotic exteriors. The
previous renderer assigned native triangles to opposite halves of the Minecraft
world to approximate those charts. This is unsuitable for nearby entrances in one
world: it deletes otherwise visible geometry and only lenses the nearest mouth.

Gameplay now uses two disjoint, finite optical regions in a shared exterior. This
is an engineered metric, not the unmodified isolated Ellis solution or a claim
about physically realizable supporting matter. The original solver remains as a
diagnostic reference. The source for the isolated metric and ray construction is
[James et al.](https://arxiv.org/abs/1502.03809); the matching profile below is our
own construction and is not taken from that paper.

In isotropic coordinates Ellis has Fermat index n=1+b²/r², where b=8 is the
coordinate mouth radius. We specify the derivative of log n: the Ellis derivative
-2b²/[r(r²+b²)], multiplied by a smooth window. That window is one up to halfway
between the throat and the outer boundary, then smoothly falls to zero. Set n=1
outside and integrate this derivative inward to define a positive optical metric.
Near the throat n is a constant multiple of the Ellis index, so the unparameterized
spatial light paths and throat matching are unchanged there. The distant deflection
and physical areal scale do change; do not label the complete scene exact Ellis.

The outer radius is min(12b,0.45 times mouth separation). The regions never overlap,
including the minimum allowed separation. The profile has 1+r*d(log n)/dr>0
outside the throat, avoiding artificial extra circular photon orbits in its blend.
Both n and its first derivative join flat space continuously. No midpoint plane
clips geometry; a ray may visit either region, in either order, and query the same
native scene before and after a throat transfer.

The GPU integrates radius, radial direction cosine and plane angle using RK4 and
the existing chord-error budget. Between regions, one straight ray query suffices.
A separate fine Cartesian RK4 reference evolves direction via the Fermat gradient
and locates boundaries by bisection. The throat still uses inversion plus z reflection
and its differential for position/direction, consistent with player crossings.

Fog accumulates Minecraft's distance measure along each chord: Euclidean for
spherical fog, max(horizontal length, absolute vertical displacement) for cylindrical
fog. Summing that measure along a straight ray reproduces native fog, with no
formula switch at the optical boundary. Throat transfers add no fictitious fog
distance between the mouths' Minecraft coordinates. This is an appearance rule,
not a model of relativistic radiative transfer.

Work is bounded to four throat passages per ray, eight pi of total orbital angle,
and 2048 solver iterations. Rays still circulating beyond a passage/angle limit
return dark; iteration exhaustion stays diagnostic magenta. Most rays use zero or
one passage. This is bounded higher-order visibility, not unlimited recursive images.

## Mass and wormholes in the same gameplay view (2026-09-29)

The unified renderer follows one ray through one native world, with one selected
mass cluster and up to two wormhole mouths. It does not combine separately warped
screen images. This avoids duplicating ordinary and bent terrain and lets a ray
be deflected by a mass before or after passing through a mouth.

**The overlap model is a gameplay approximation, not a solution of Einstein's
equations for several objects.** We add the perpendicular spatial ray-curvature
vectors of the selected mass and the localized mouths. This defines a reproducible
ray rule, but we do not assert that the complete rule comes from one Lorentzian
metric. Minecraft coordinates identify the mass's areal chart with the mouths'
isotropic charts; that identification is another deliberate approximation.

State consists of position and unit coordinate direction, parameterized by
Euclidean arc length. For the mass, let r be distance to its centre, n the outward
radial unit vector, d the ray direction, and mu=n·d. The exterior acceleration is
`-3 rs (1-mu²) (n-mu d) / (2 r²)`. It follows by projecting the areal-coordinate
Schwarzschild Hamiltonian acceleration perpendicular to d. The finite body's
coefficient similarly follows from the existing interior lapse/radial metric.
`MixedWorld.affineAcceleration` derives it from metric derivatives and canonical
momentum; tests compare that independent form with the simplified GPU expression.

Mass curvature stays unchanged inside half the outer radius, then tapers smoothly
to zero at `max(96,48 rs,8 bodyRadius)` blocks. The mouths retain the localized
Ellis gradient described above. The observer uses the existing mass static/falling
frame convention, with its far-field correction tapered too. Single-effect scenes
retain their established solvers and untapered mass rendering.

The GPU uses adaptive Cartesian RK4. Chord-error, angular-motion and radial-motion
bounds limit each step; a tighter boundary rule resolves the finite body's metric
derivative jump at its surface. Outside all optical regions, one straight query
reaches the next region or the distant scene. Mouth crossings use the same
inversion/reflection and four-passage limit as player travel. A ray can encounter
either source in any order; exterior capture is decided by an actual horizon
intersection, not the isolated-hole capture cone. Another field or a mouth could
otherwise redirect a ray before it reaches that horizon.

The finite-body metric has a derivative discontinuity at its surface. Early
constant-step reference tests exposed this as poor numerical convergence; refining
steps specifically near that boundary fixes it without making every step tiny.
A fine Cartesian midpoint reference uses the unsimplified metric acceleration and
bisection at throats. GPU fixtures compare directions and passage/capture codes,
including the zero-mass limit. These tests validate implementation of the stated
composition rule, not the physical accuracy of combining strong fields.

Closed/preparing mouths use a localized Schwarzschild preview alongside the mass.
The interior mass editor, shared materials, native light/fog, AA and GL/RTX geometry
queries remain available. The near-horizon mixed observer and overlapping closed
previews have visual checks rather than a new independent spacetime reference.
Server entity gravity remains its bounded dominant-source gameplay model; it does
not use this optical curvature sum or simulate wormhole gravity on mobs.

## Walking observer frame (D096, D103)

Relativistic Sight adds a prescribed horizontal local observer velocity before
baseline camera-to-GR ray mapping. In flat space this is inverse Lorentz
aberration and the associated directional frequency ratio; the independent CPU
reference boosts a null photon four-vector. Baseline GR frames and mixed-metric
approximations remain unchanged. Optical speed is separate from Minecraft
movement; there is no retarded dynamic-entity history, server-clock modification,
or combined gravitational spectral transport in this feature.

RGB colour assumes linear spectral samples at 450/550/650 nm. D103 replaces the
original zero-at-380/780-nm cutoff with a fictional weak continuum: at 380 nm its
amplitude is 0.005 times the average of linear RGB luminance and blue; at 780 nm,
the same expression uses red. Interpolate to the visible anchors. Below 380 nm
multiply the boundary amplitude by (wavelength/380)^4; above 780 nm multiply by
(780/wavelength)^2. These choices keep the model continuous, nonnegative and
unchanged at the RGB anchors. They are not measured spectra, a thermal-emission
model, or a reconstruction of UV/IR from Minecraft RGB.

Full mode samples the assumed source spectrum at output wavelength times Doppler
D; gentle mode uses D^0.06. The independent exposure cue
uses a compressed bolometric exponent 1.4, bounded log gain, and a highlight
shoulder. These are explicit display approximations, not exact spectra or
calibrated radiometry. After display conversion, colour-enabled output has a
hue-preserving exposure floor: its maximum channel is at least 0.04 times the
input's maximum display-RGB channel. Black remains black. This deliberately
preserves nearly black texture detail, even when a real scene might be invisible.
The model uses three spectral samples, not integrated cone-response functions or
consistent absolute spectral radiometry. See [controls and limitations](relativistic-sight.md).

Primary background: https://www.spacetimetravel.org/aur ;
https://www.spacetimetravel.org/ejpvis/ejpvis.pdf (why Doppler visualization needs
source spectra outside the visible band as well as within it) ;
https://www.einstein-online.info/en/spotlight/doppler/ ;
https://github.com/MITGameLab/OpenRelativity . No third-party shader is copied.
