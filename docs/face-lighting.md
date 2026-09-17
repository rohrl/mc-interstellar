# Captured face lighting — 2026-09-17

Nearby opaque surfaces previously assumed sky15/block0, while distant column sides reused light from above the column. Capture adjacent sky/block levels for each of the six faces instead. Include the emitting block's luminance; inset snow tops sample their own cell. Distant caps and representative material beneath them have separate face-light records, so a snow cap does not inherit a dark cliff sample. Shader selection follows the same cap/side and upper/lower layer as material selection.

Each sky/block pair occupies one byte; three faces pack into an exactly representable24-bit integer in a float. Local data uses an RG32F texture matching the existing flattened96-cubed grid; distant data uses a512x256 RGBA32F texture with two layer texels per column, each holding cap/side records. Extra memory is17.5MiB CPU+GPU per snapshot, at most35MiB for published+pending snapshots. Capture remains inside existing per-frame budgets and texture uploads share existing unpack/PBO isolation. All new buffers/textures are released with their snapshots.

F10 uses captured face light by default. In frozen F9, **K** switches between captured and previous lighting for controlled comparisons; **P** captures the selected backend versus vanilla. The switch does not change simulation, scene geometry or persistent configuration. `candidateScene` metadata records `faceLight=true/false`.

## Controlled comparisons

All pair paths below are under ignored `run/interstellar-captures`. Each pair is vanilla versus the full-resolution zero-bending backend. For each on/off comparison, vanilla reference images are pixel-identical, confirmed by the comparison tool. No weather/time changes or blocks were made.

| Scene | Captured light pair | Previous light pair | Full RGB MAE, previous → captured |
| --- | --- | --- | --- |
| Downward, just inside128-block boundary | `1493589069518521945` | `4286719966742164883` |0.0409 →0.0409 (tiny improvement only)|
| Under existing platform, camera(16.5,281.6199998855591,16.5), yaw0/pitch−65 | `8157456065485471487` | `16334007698053537651` |0.0656 →0.0001|

The sheltered test corrects the over-bright underside and closely matches vanilla. The downward contact sheet contains falling snow in vanilla, absent from this backend. Bottom-half error only changes0.0565→0.0564: face lighting is not the main remaining mountain mismatch. Both captured-light contact sheets were inspected. Full smooth lighting/ambient occlusion, per-corner interpolation, texture filtering, missing geometry and precipitation remain. Distant deep cliff lighting is still represented by the block below the top cap; this cannot recreate vertical variation or a cave the height field omits. These nighttime samples do not certify all materials, dimensions or daylight.

## Verification and live cost

- `face-light-final-build.log`: build passes;45 existing tests, zero failures/errors. Ray equations unchanged; no new optical validation claimed.
- `face-light-verified-runtime.log`: actual shader compilation, F9 K/P, and F10 rendering pass. Captured-light comparisons are controlled A/B evidence rather than comparisons across world times.
- Established far pose, N64/11700 opaque, standard,1440p/half-resolution,120 warmup/300 samples: GPU p50/p95/p99=9.536896/9.658944/9.752320ms; frame intervals11.1058/12.2304/12.9544ms. Optical timer excludes scene capture/upload, sky capture and upscale. These are scene-specific measurements, not universal FPS certification.
- Initial frozen capture2.026s; sampled live captures around1.91–1.92s. Budgets bound work per frame; snapshots remain asynchronous/stale between publications.
- Reference-repeat reports: `face-light-repeat`, `face-light-shelter-repeat` (all errors zero).

Client left at the far reference in creative flight, F10 on with captured lighting, window870x519. AA remains isolated. Next address smooth corner shading and terrain representation/coverage; do not treat this as completion of the reported mountain mismatch or hard viewing boundary.
