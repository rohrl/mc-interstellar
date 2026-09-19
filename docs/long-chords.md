# Curvature-limited longer segments — accepted16-block cap

Raise only the native adaptive integrator's spatial segment cap from4 to16 blocks initially. Keep the existing local curvature estimate, nominal0.001-block sagitta tolerance (scaled with the standard/fine path setting), minimum path step,0.02radian angular cap, RK4 equations and all scene intersections. A larger cap matters only where the existing curvature formula permits a longer segment. It does not skip geometry queries or assume the segment is empty.

The curvature estimate is local, not a rigorous bound over a long segment; changing numerical sampling can affect tangent surfaces and outgoing sky direction. Fresh independent optical and actual-world image comparisons are required. This is distinct from the rejected empty-region stepping experiment, which computed additional clearance bounds and relaxed the spatial limit only inside certified empty space.

Keep the accepted four-block shaders intact. New compact diagnostic and specialized live programs expose MeshStepLimit; V toggles4/16 in F9 native mode and Shift+V compares against the separately compiled four-block program. Other backends retain their existing behavior. C explicitly receives the selected cap in its diagnostic variant. The initial experiment also exposed8/32;32 was tested but not retained because its extra GPU gain was only2% downward.

`long-chords-build.log`:53 tests pass. Runtime `long-chords-runtime.log` supplies the independent optical, image, timing and live evidence below. Accept16 on top of84fbe31 (normal-setting specialization); final control/build in long-chords-final-build.log. At least30FPS remains the active minimum; the terrain-heavy view still needs work.

## Frozen-scene evidence — 2026-09-20

Both16 and32 caps pass26240 sampled checks in the compact diagnostic variant, zero mismatches/inconclusive/unresolved;340 distinct directions repeated over flags/layouts, including critical capture classification. The shader equations and curvature/angular limits remain unchanged; sampled agreement is not a rigorous global error proof.

Same paused scene:6092222 terrain triangles, N65/r_s8.125, player(16.5,302,-45.5), yaw.281 except away180.281;RTX5070Ti/driver616.92.2560x1440 output,1280x720 internal,2xAA and accepted defaults;120 warmup/300 samples, resolve included.

|View / pitch|Cap|GPU p50/p95/p99 ms|Frame p50/p95 ms|
|---|---|---|---|
|Wall / .91|4|37.860/38.849/39.473|38.421/39.651|
|Wall / .91|16|29.550/30.592/31.413|30.109/31.169|
|Down /35.91|4|57.773/58.982/59.607|58.478/59.857|
|Down /35.91|16|45.688/46.591/47.115|46.246/47.318|
|Down /35.91|32|44.612/45.642/46.314|45.231/46.368|
|Away / .91|16|9.936/10.017/10.054|10.287/10.659|

Cap16 improves GPU p95 by21.3% wall/21.0% downward. Frozen frame medians imply~33FPS wall and~22FPS downward; the terrain-heavy view still misses30. Away timing has no matched reference and is observational only. Cap32 saves only2.0% beyond16, so retain the more conservative16. No AA/resolution/coverage reduction.

|Same-frame pair|View/cap|RGB MAE [0,1]|Pixels differing >8/255|
|---|---|---|---|
|12360292774370380936|Wall16|0.00000208|0.0023%|
|16741007297105657178|Down16|0.00000714|0.0031%|
|16964723291290159573|Down32|0.00000717|0.0031%|
|757081570100502806|Away16|0.00000025|0.0002%|

Down16 image inspected; no visible degradation found in this checkpoint. These pairs are not pixel-identical. Fine silhouettes and finite-radius sky exit sampling can move slightly, so retain precise metrics rather than claiming exact rendering equivalence.

## Live1440p check

Same cap16/2xAA/half-scale, with active F10 simulation and moving actors. Wall yaw.281/pitch.91 GPU p50/p95/p99=27.764/28.778/29.262ms; frame28.594/30.147/30.568ms (~35FPS median). At the same position, pitch35.91 GPU43.088/44.356/44.662ms; frame43.998/45.398/45.838ms (~23FPS median). Pose changed by a test command without changing position/chunk window. These are actual live observations, not paired four/sixteen performance comparisons; actors evolve naturally. No automated movement/flicker test or teleport-support claim.

The live wall view clears30FPS; the downward view does not. Continue optimization. Restore player yaw.281/pitch.91 and small window after verification. No manual block/time/weather changes or new renderer failure. The32-block setting is removed from the retained V control because it adds little benefit; V now switches only4/16.
