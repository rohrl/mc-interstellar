# Independent curved-terrain checks

This page describes the legacy voxel check. In native mesh mode, F9 C now runs the separate [opaque mesh fixture](mesh-ray-validation.md).

F9 **C** runs a bounded 12x8 comparison of production GPU curved rays against an independent CPU reference. V retains the larger flat-geometry diagnostic. Both briefly pause the frozen view; neither runs during normal F10 rendering. The C output separates compared rays, mismatches, CPU refinement failures, CPU/GPU budget exhaustion, invalid GPU data, actual opaque hits, and captured rays. Failed CPU refinement is inconclusive, never a pass.

## Reference and independence

`FiniteTerrainRay` uses an affine parameter lambda and state (r, v=dr/dlambda, phi), with conserved energy normalized to E=1. For a static observer's local outward cosine mu, initialize v=mu and L=r*sqrt(1-mu^2)/sqrt(1-r_s/r). Integrate:

- dr/dlambda = v
- dv/dlambda = L^2/r^3 * (1 - 1.5*r_s/r)
- dphi/dlambda = L/r^2

The monitored null invariant is v^2 + (1-r_s/r)*L^2/r^2 = 1. These equations follow from the Schwarzschild metric and conserved energy/angular momentum; see the metric and planar-ray conventions in [Bruneton, sections 3.1–3.3](https://ebruneton.github.io/black_hole_shader/paper.pdf). The implementation derivation is ours; this is not a copied renderer.

CPU integration is adaptive Dormand–Prince 5(4) in double precision, rather than the GPU's fixed inverse-radius RK4. Each accepted chord enumerates nearby cuboids and uses independent slab intersections, rather than GPU voxel stepping. Two reference runs use chord caps 0.1/0.05 blocks and tolerances 1e-9/1e-11. Only matching resolved outcomes and exact hit cells are compared with the GPU. The reported invariant error measures integration consistency, not surface accuracy.

The shared assumptions remain one static exterior Schwarzschild source, the same areal-coordinate embedding, the same immutable terrain snapshot, the same sky/data boundaries and a 16-radian winding budget. Cubes and snow are stationary cuboids; both implementations approximate curved paths with chords. Refinement is evidence for sampled rays, not a bound between samples or a full independent model of Minecraft material/radiometry. Finite cube-edge ties and extreme critical rays need further targeted coverage.

## Tests and runtime evidence

Build and 40 tests pass. Five new tests cover radial foreground occlusion before the horizon, empty-ray capture, outside-entry/parallel rejection and thin snow in flat space, analytic static capture on both sides of the photon sphere, curved wall-hit refinement, invariant conservation and invalid inputs.

Initial actual-GPU suite: four views, each at standard/fine GPU path targets 0.45/0.225 blocks, with 96 samples per run (768 comparisons). Every run had zero mismatches, CPU refinement failures, CPU unresolved rays or GPU unresolved rays. Maximum reported invariant drift was 3.664e-14. Diagnostic wall times were 63–209 ms on this machine. These are diagnostic timings, not rendering performance.

| Player position | Yaw / pitch | Purpose |
| --- | --- | --- |
| (16.5,302,-19.5) | 0.84516 / 2.65028 | Reference scene and foreground pillar |
| (16.5,302,-100.5) | 0 / 0 | Observer outside captured volume |
| (16.5,302,5.5) | 180 / 10 | Outward view inside photon sphere, outside horizon |
| (16.5,390,16.5) | 0 / 89 | Overhead outside-volume entry |

Camera Y is player Y + 1.6199998856 blocks. Source is the existing N=63/r_s=7.875 cluster. Snapshots had 11597 opaque cells and no unknown/unsupported cells. No scene blocks were changed for these checks. Logs are local ignored curved-runtime.log and curved-build.log. Final reporting/visual/timing checks are recorded below.

## Final-build verification

Final near/far C runs each compared 96 rays: zero mismatches, refinement failures, unresolved rays, invalid GPU values or flat mismatches. Near sample contained 64 opaque hits and 26 captured rays; far contained six opaque hits and four captured rays. Final V regression: 39x26, zero flat mismatches, 479 flat hits, 641 lensed opaque hits. The terrain image and C HUD were visually checked at run/curved-check.png. Final logs: curved-final-runtime.log.

Final live benchmark at reference camera, 2560x1440 output / 1280x720 internal, standard path, lensing on, refresh active, 11597 opaque cells: GPU p50=5.033888, p95=5.132864, p99=5.173760 ms. Frame-interval p95=9.1841 ms. RTX 5070 Ti / NVIDIA 616.92; 120 warmup frames, 300 samples. No full-frame GPU or universal performance claim. Client restored to F10 at the reference position, 870x519 window. No world edits performed in this iteration.

## Scope in the delivery plan

This checkpoint supports step 2 correctness. It does not finish step 1 automatic source refresh or step 3 demo packaging. Those take priority next; further numerical research and AA should not displace them. The owner-reaffirmed order is now explicit in plan.md.
