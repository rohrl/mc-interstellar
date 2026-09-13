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
