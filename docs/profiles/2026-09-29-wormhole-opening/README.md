# Wormhole opening checks — 2026-09-29

## Result

First mouth: small closed core, native depth-tested marker while local geometry
loads, then the existing BH optics. Paired mouths: increasing hover percentage
and core radius, followed by a short reveal into Ellis optics. No gameplay gravity
is attached to this visual effect. R remains the independent upright key.

| Checkpoint | Image |
|---|---|
| First pearl; local capture still loading | [First core](first-core.png) |
| Warm-cache opening at 61%, RTX BH lensing | [61%](opening-61.png) |
| Same position at 98%, larger shadow | [98%](opening-98.png) |
| Same position, passage open | [Connected](open.png) |

The latter three images use the same fixed exterior pose. No optical equations
were altered. The growth and 0.35-second crossfade are presentation choices,
not a physical model of black-hole conversion.

## Checks performed

- Normal and RTX Gradle builds pass 95 tests, including seven new progress/entry
  tests. Normal jar has zero optional Vulkan/shaderc/backend entries.
- Actual first pearl, second pearl and oldest-end replacement in a disposable
  Overworld save; original saves preserved. RTX activates automatically.
- New reveal shader compiles and runs. Native closed marker, BH growth and final
  passage visually inspected. No renderer errors in either runtime pass.
- First-to-second layout changes retain the local geometry cache. Once local
  optics are ready, moving the capture window does not revert to the native marker.
- Native region readiness precedes the visual/travel acknowledgement. Warm-cache
  replacements took approximately four seconds in these checks, including bounded
  packet delivery; this is not a universal load-time promise.
- Player at feet (231.078557,296.38,184.657420) remained unchanged before and after
  revision21 opened around them. A later deliberate outside-to-inside crossing
  succeeded. Separately, a large admin teleport inside did not trigger travel.
- Off-axis crossing produced -10.86466 degrees of roll. Pressing R reset it to
  zero while preserving aim and position (normal flight inertia continued).
- Owner's full-resolution/fine/2x/RTX/weather-on/body-off settings preserved,
  matching the saved options hash. Broad movement/flicker tests remain deferred.

## Image comparison and timing

Same-frame GL/RTX comparisons at 1280x720, full resolution, fine paths and 2x AA:

| View | RGB mean absolute error, 0–255 scale |
|---|---:|
| Closed BH preview | 0.0004174 |
| Connected wormhole | 0.0011306 |

The paired renders use frozen camera/scene inputs within each comparison. They
check backend agreement, not scientific accuracy. Terrain was still streaming
around these captures; their raw short timings are not used as settled gameplay
measurements. Full records: [closed](closed-gl-rtx.txt), [open](open-gl-rtx.txt).

The settled live run had 6,246,870 triangles and zero queued chunks at completion,
120 warmup frames and 300 samples:

| Metric | p50 | p95 | p99 |
|---|---:|---:|---:|
| RTX update/render GPU time | 13.58 ms | 16.09 ms | 17.60 ms |
| Frame interval | 15.62 ms | 18.21 ms | 19.72 ms |

Approximately 64 FPS at the median in this view. Frame intervals include the
120 FPS cap/vsync; Vulkan GPU timing excludes GL appearance copies and resolve.
[Raw timing](settled-timing.txt). An earlier sample with 216 queued chunks was
excluded from the settled result. This is not a matched before/after regression
study; steady-state optical tracing is unchanged and the extra framebuffer is
released after the reveal.

## Limits

The nearest mouth drives BH preview optics; another closed mouth in the same view
uses a native sphere marker. Progress is a smoothed work estimate, not ETA. It can
wait at 99% for geometry or the first passage frame. Readiness is per client.
This feature does not expand remote capture bounds or add remote entities/transit.
The final text-only “step outside” hint was compiled after the runtime checks;
it required no extra screenshot pass.

Detailed implementation: [settings and Rift Pearl](../../settings-and-wormhole-seed.md).
