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
