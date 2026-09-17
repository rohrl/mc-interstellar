# Native corner lighting — 2026-09-17

Capture the corner brightness and lightmap coordinates emitted by Minecraft's own `BlockModelRenderer`, using a quad consumer instead of drawing those quads. This supplies native smooth lighting/ambient occlusion for the supported block faces. Runtime lighting still follows the current Minecraft lightmap. No ray equations, scene geometry or world blocks are changed.

Each face stores four corner records: 8-bit brightness and native block/sky lightmap coordinates, packed into an exactly representable 24-bit integer. A sign bit preserves the baked quad's triangle diagonal. The fragment shader reconstructs each corner's lit colour and interpolates over the same triangles at the terrain hit. Inset snow faces use their captured model height. Unsupported layouts or exhausted record capacity fall back to the previous face-light path. Brightness quantization and the existing material/tint limitations remain; this is not a claim of arbitrary baked-model support.

Identical records are shared across local blocks and distant cap/side representatives. The atlas is capped at 65,536 records (6 MiB GPU maximum). Local/distant record-ID maps add 4.375 MiB each on CPU and GPU per snapshot; at most two terrain snapshots coexist. Record payloads and the temporary upload buffer are bounded by the same cap; Java map/object overhead is additional. Captured record contents are discarded after upload, and textures/native buffers close with the snapshot. Tested atlases used about 3.1–3.3 MiB GPU with zero capped captures.

Capture checks its time budget after every cell/column instead of every 64 reads. Minecraft's brightness cache is enabled only within each capture slice and disabled/cleared afterward, so it cannot preserve stale lighting across later world changes. This optimization was interrupted by usage limits, then built and verified after resuming.

## Comparison controls

In F9, **O** toggles native corner lighting against the prior face lighting; **K** must be on. **P** captures the selected zero-bending backend versus vanilla. Metadata records both switches. F10 enables the new lighting by default. Antialiasing remains separate.

## Controlled evidence

All pairs are under ignored `run/interstellar-captures`. Vanilla references within each on/off comparison are pixel-identical. These are controlled same-scene comparisons, not scores compared across world times.

| Scene | Smooth pair | Face-only pair | RGB MAE, face-only → smooth |
| --- | --- | --- | --- |
| Wall/floor junction: camera (0.5, 291.62, 35.5), yaw 0, pitch 25 | `1132989478347281953` | `10935301844190255255` | Full: 0.0409 → 0.0333; centre: 0.0514 → 0.0356 |
| Daylight mountains near the viewing boundary: camera (16.5, 303.62, -111.5), yaw 0.281, pitch 35 | `15225779807639016176` | `16087620137338088191` | Full: 0.0380 → 0.0262; bottom half: 0.0716 → 0.0483 |

Both smooth-light contact sheets inspected. The wall contact shadow and distant terrain shading improve. Foreground clouds are still missing where they should cover terrain, and the height-field geometry differs from vanilla. Residual material/filtering errors remain on broad surfaces. Reports `smooth-wall-repeat` and `smooth-down-repeat` confirm zero reference-image difference. The wall test preceded the cache optimization; the downward test used the final build.

## Build, timing and limitations

- `smooth-light-final-build.log`: build passes; 45 existing tests, zero failures/errors. Actual shader rendering and F9 O/P verified. No new curved-ray validation is claimed for this appearance-only change.
- Initial uncached capture: 5.209 s, 35,532 records. Final frozen capture: 3.945 s, 33,387 records; sampled live captures about 3.79–3.80 s with 34,364 records. The scene changed during the interruption (11,697 opaque cells, 37 materials in final run), so those capture timings are not a controlled cache speedup. They do show refresh is slower than the earlier roughly 1.9 s face-light capture. The usual one-second publication delay is additional.
- `smooth-light-verified-runtime.log`: established far pose, N64, 11,697 opaque / zero unknown or unsupported, standard, 1440p output / half-resolution tracing, 120 warmup / 300 samples. GPU p50/p95/p99: 9.580736 / 9.719936 / 9.764576 ms; frame intervals: 10.7460 / 11.8643 / 12.5614 ms. Optical GPU timing excludes capture/upload, sky capture and upscale; these short samples do not certify every scene or refresh spike.
- Distant deep cliff lighting still reuses the block below the top cap. Smooth lighting cannot repair caves, overhangs or other geometry missing from the height field. Tint/model coverage, cloud foreground occlusion, precipitation, finite coverage and the hard 128-block viewing cutoff remain unfinished. No automatic movement/flicker suite was added.

Client left in creative flight at the far reference, F10 enabled, small inspection window. No blocks edited by this work. Next address foreground-cloud depth and useful scene coverage; cache/invalidation work will be needed before substantially expanding full captures.
