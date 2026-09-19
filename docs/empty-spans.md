# Larger steps in certified empty space — rejected experiment

**Decision:** retain the production stepping algorithm. The candidate regresses against a separately compiled original shader, despite apparently favourable on/off measurements within the modified program. Preserve this experiment separately; its controls and shader changes are not production features.

The existing renderer certifies empty boxes separately for terrain and moving geometry. Inside their intersection, a longer chord cannot encounter geometry if its whole path remains within a conservative distance bound. This experiment relaxes the spatial chord-size limit only in that certified region. It retains the existing0.02radian angular step cap, RK4 equations, sky/horizon exits, AA and geometry. Numerical step sizes change, so independent accuracy and image checks are required.

## Exterior bound

With the existing inverse-radius state q=(u,v), u=r_s/r, the derivatives are u'=v and v'=1.5u²-u. In0<u<1, |v'|<=1/2. For proposed angular span H use loose bounds L=u-|v|H-H², U=u+|v|H+H² and W=|v|+H. Require L>0 and U<1 to bound the RK4 stages in the exterior domain. Cartesian speed is at most r_s*(U+W)/L², using an L1 upper bound for the norm.

Require this speed bound times H, with1% extra margin, to fit in a ball centred at the current point. Its radius is the minimum distance to every face of both empty boxes, minus0.01block rounding margin. Propose at most0.1radians and one quarter of clearance/current speed, then verify the bound; the actual step is additionally capped at0.02. The convex ball contains the chord and its curved exterior path in exact arithmetic. Margins address floating-point arithmetic; this is not formal interval arithmetic. Both box certificates must exist. State resets each ray/AA sample, and moving geometry is fixed during each draw.

The angular accuracy cap still applies even in empty space. Longer numerical steps can change the computed outgoing direction or critical capture boundary, despite leaving the equations unchanged. Do not infer correctness solely from the geometric certificate.

## Checks and status

Experiment controls: F9 F toggles; Shift+F compares off/selected in one frame. Both shader manifests expose EmptySpans and benchmark/capture metadata record it. The CPU/GPU fixture tests both states when caching and adaptive stepping are active. An added empty-scene fixture checks640 capture/escape classifications against analytic b_critical²=27/4 at four observer distances, both path settings and both modes; the exact critical column is excluded. These checks supplement, not replace, multi-view image and1440p timing comparisons.

`empty-step-build.log`:51 tests pass. `empty-step-runtime.log`:23680 sampled comparisons pass, zero mismatches/inconclusive/unresolved;340 distinct directions repeated across modes, including160 near-critical directions. The logged3.86e-14 invariant error belongs to the CPU reference, not GPU precision. Wall/downward pairs10632390050695809705 and18041433592495396892 have RGB MAE0.00000004/0.00000002, respectively; differences over8/255 round to0.0000%. They are not pixel-identical.

At2560x1440 output,1280x720 internal,2xAA and all accepted optimizations, on/off comparisons **within the modified executable** suggested wall GPU p95 improvement55.568→52.217ms (6.0%) and downward86.253→83.835ms (2.8%). These do not establish a gain against production: adding code can affect the compiled shader even when a uniform disables it.

The final experiment therefore adds a separately compiled original native program. A preprocessor guard removes the only changed integration statement; the new helper/uniform are unused. Removing the helper/uniform and guard reproduces the original shared GLSL exactly. `empty-step-reference-build.log` passes; runtime compilation and comparison are in `empty-step-reference-runtime.log`. Same paused wall scene:6092214 terrain triangles,61 supported mobs, source N65/r_s8.125, player(16.5,302,-45.5), yaw.281/pitch.91;RTX5070Ti/driver616.92. Each timing uses120 warmup frames and300 samples and includes the resolve pass.

|Order|Program|GPU p50/p95/p99 ms|Frame p50/p95 ms|
|---|---|---|---|
|1|candidate|50.046/52.149/53.129|50.662/52.905|
|2|original|49.012/50.854/52.090|49.597/51.507|
|3|original repeat|48.870/51.089/52.002|49.494/51.760|
|4|candidate repeat|50.244/52.347/52.838|50.859/52.988|

Same-frame compiled-program pair13078272707038092639 matches the earlier wall metrics: RGB MAE0.00000004, linear-luminance MAE0.00000007. The candidate's GPU p95 is2.5% slower than original in both orders. No occupancy/register explanation is claimed without compiler/profiler evidence. These results justify rejecting the added numerical complexity; the analytic capture-boundary regression fixture remains useful independently.

## Earlier variant rejected

The first variant retained every numerical step and deferred only Cartesian conversion and scene queries. It passed23040 sampled checks and pixel-identical pairs, but saved only0.5% in the matched1440p wall timing (GPU p95 off57.263/on56.976ms). Logs: empty-spans-runtime.log; pair673650807569961964. Earlier windowed pairs10604632665394875345 and12563324187448653341 also matched exactly; exclude them from1440p claims. Those results compare toggles within the modified executable, not separate release builds. The conversion-only variant was rejected for insufficient benefit.
