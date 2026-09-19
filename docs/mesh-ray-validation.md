# Independent curved mesh fixture

In a ready **F9 native mesh preview**, press **C**. The test briefly pauses rendering, logs a compact comparison table, and restores the normal preview. It creates no blocks, changes no world state, and allocates only two temporary one-row mesh textures. The ordinary F10 path never runs the fixture. In the legacy voxel preview, C retains its previous world-snapshot diagnostic.

## What is compared

Four stationary opaque boxes provide a rear wall, side occluder, foreground occluder and farther background. The GPU receives outward-facing triangles. The CPU receives an independent occupied-cell description and intersects cuboid slabs; it does not use the triangle builder or either GPU acceleration structure.

The existing [affine-parameter CPU reference](curved-terrain-validation.md) uses adaptive double-precision Dormand–Prince integration. Each ray runs at chord limits 0.1/0.05 blocks and tolerances 1e-9/1e-11. A changed hit cell or unresolved CPU result makes that ray inconclusive. GPU results distinguish hit cells, horizon capture, escape, unresolved traversal/integration and invalid floating-point values.

- Source radius: 8 blocks. Camera distances: 32, 96, 148 and 252 blocks.
- 9×5 directions per distance, including the inward radial ray; angular windows narrow with distance to keep sampling the hole and adjacent geometry.
- Both monolithic and two-level mesh trees; child addresses lie beyond the top-level end sentinel, matching the live arena convention.
- Both standard/fine path settings, 0.45/0.225 blocks. **Production far-distance steps remain enabled**, reaching 4 blocks beyond 144 blocks. Diagnostics must not silently replace these with fine steps.
- 180 distinct rays, compared in four GPU configurations: 720 comparisons.

Diagnostic mode3 bypasses texture alpha/material shading for this deliberately opaque fixture. It reports the entered unit cell from the surface position/normal. The usual ray integrator and triangle traversal run unchanged. Camera/source coordinates use exactly representable float values; projections use the same float inputs on CPU and GPU.

## Scope

### Additional analytic capture-boundary fixture (2026-09-19)

The current suite also removes all geometry and samples close to the Schwarzschild critical impact parameter. For the static exterior observer, define `u=r_s/r`, `b/r_s=sin(alpha)/(u*sqrt(1-u))`; incoming rays are captured below `b²/r_s²=27/4` and escape above it. Use the existing four distances and two production path settings, with projection slopes offset about0.0022–0.0089% from the critical tangent. Exclude the central column: an exactly critical ray does not have a finite escape time and is not a finite-precision oracle.

This adds320 comparisons across160 distinct directions. Together with the accumulated optimization/layout variants, the current total is17600 checks over340 distinct directions. The shader uses its ordinary stepping, horizon/escape handling and iteration budget. This checks classification, not outgoing-angle accuracy or every critical direction. Earlier immutable result sections below retain their original counts. The rejected empty-step experiment used640 boundary checks because it additionally repeated both experimental step modes; production does not retain that toggle.

`performance-review-build.log`: build passes,51 existing tests successful. `performance-review-runtime.log`: native and general programs each pass17600 comparisons, zero mismatches/inconclusive/unresolved; runtime1773/1712ms. The native lensed image after both diagnostics was inspected (`2026-09-19_23.12.01.png`); normal world rendering resumes. These are sampled regression results, not universal correctness certification.

This checks sampled curved paths, first-hit ownership and the two mesh layouts on synthetic opaque boxes. It is not an independent triangle-intersection implementation for arbitrary models, a guarantee at every subpixel boundary, a material/transparency test, or complete strong-lensing convergence. Both numerical methods approximate paths with chords. Hit-cell agreement can hide subcell position error. Actual-world appearance pairs remain separate evidence. No rendering FPS target follows from diagnostic runtime.

The first development run caught an error in the fixture's compact child addresses: its first child overlapped the top-level termination sentinel. The production arena already keeps these addresses separate. Corrected the fixture before accepting two-level results; no production traversal change was needed for this issue.

## Verified checkpoint — 2026-09-18

`mesh-fixture-final-build.log`: build and all51 existing tests pass. `mesh-fixture-final-runtime.log`: actual shader suite completes720 comparisons with **zero mismatches**, zero inconclusive CPU rays, zero unresolved/invalid GPU rays. All16 distance/backend/path combinations pass. Across180 distinct rays:81 opaque hits,97 captures,2 escapes. Maximum CPU invariant drift3.8636e-14; complete diagnostic wall time577.32ms. This is a deliberately small sample, not universal coverage.

Same frozen world pose before/after C: player(16.5,302,-45.5), yaw.281/pitch.91. Pairs5283397356082865452 and16073401041559583963 have identical candidate SHA-256 hashes and identical reference hashes. Candidate F5330D853E8A8247184D5DBE3811E1F5DD427C8B1443C570E91EFFCFC0B86D58; reference449DCFE213F4F850CC1E1765ECC62ADB03371DE9961A0A9E698A497F0DCA6F9C. The diagnostic leaves the rendered appearance unchanged. Candidate-versus-vanilla RGB MAE.0014, lower half.0022; contact sheet inspected. Nighttime scene,101 supported mobs,6,105,722 triangles. Lensed screenshot also inspected; no blocks/time/weather/population edited.

Normal frozen lensed pass after the diagnostic:427×240 internal,854×480 window, half scale, standard paths, RTX5070Ti, r/r_s7.69042. After120 warmup frames/300 samples, GPU p50/p95/p99=14.389696/15.975104/15.98384ms; frame intervals15.6243/17.0748/17.591899ms. GPU timing excludes world rendering, capture, native sky updates and final upscale; frozen scene does not measure live actor/streaming costs. This is not the1440p target or a speedup comparison.
