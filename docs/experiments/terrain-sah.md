# Terrain surface-area tree experiment — rejected, 2026-09-19

Do not enable this experiment in the production branch. It replaces terrain midpoint splits with12-bin surface-area splits, keeping all geometry, GPU intersection code and ray steps. Moving actors retain their original trees. The cost model follows the [surface-area heuristic described in PBRT](https://pbr-book.org/4ed/Primitives_and_Intersection_Acceleration/Bounding_Volume_Hierarchies); the implementation is original.

## Evidence

`sah-build.log`:52 tests pass, covering both builders, conservative bounds/escape links, complete triangle payload preservation, and degenerate/planar/skewed geometry. `sah-runtime.log`:34560 sampled CPU/GPU comparisons pass, zero mismatches/inconclusive/unresolved,2175ms;180 distinct directions over existing flags/layouts and both builders. This does not certify arbitrary materials or all rays.

Matched paused streamed scene:6,092,214 terrain triangles, source N65/r_s8.125, player(16.5,302,-45.5), yaw.281.2560x1440 output,1280x720 internal,2xAA, native shader, all previous optimizations enabled. RTX5070Ti/driver616.92;120 warmup/300 samples. H rebuilds only terrain and preserves the paused world, selected source, lensing and frozen moving geometry. Candidate measured first in each view.

| View / pitch | Tree | GPU p50 / p95 / p99 ms | Frame p50 / p95 ms |
| --- | --- | --- | --- |
|Wall / .91|midpoint|51.229 /53.162 /54.437|52.170 /54.170|
|Wall / .91|surface area|49.942 /52.146 /52.736|50.845 /53.100|
|Downward /35|midpoint|81.407 /83.139 /83.924|82.590 /85.953|
|Downward /35|surface area|78.541 /80.363 /81.193|79.895 /82.717|

GPU p95 gains only1.9% wall and3.3% downward. No cross-session gain claimed. CPU tree build totals rise from1.57 to5.75seconds in the wall capture, and1.56 to6.31seconds downward. Maximum measured individual build rises from7.1ms to80.6/103.0ms; these maxima include scheduling/JIT effects and are not pure algorithm timings. Node count falls2,193,919→2,046,847. Initial wall capture rises34.72→37.39seconds, but capture resolution/warmup differed, so that wall-time delta is descriptive only.

Pixel-identical candidate PNGs with matching SHA256 within each comparison:

- Wall: midpoint pair6319277755941653327 versus surface-area pair4142834704097778400.
- Downward: midpoint pair4681040667962348374 versus surface-area pair9882232367554322622; numeric report `run/interstellar-captures/sah-downward`.

These compare two captures of one paused scene, not a same-frame tree switch. Each source pair's same-frame comparison is native versus general shader; use the candidate files across captures for tree comparisons. No block/time/weather edits. Client closed normally after verification.

## Decision

Reject for now: small GPU gains do not justify added code and substantially more chunk build work. Further CPU tuning would not establish a larger GPU win. Preserve the experiment on a separate branch; keep midpoint trees in production. This follows the owner's explicit diminishing-returns limit. A different next candidate is deferring coordinate conversions inside certified empty regions while retaining every existing numerical ray step.
