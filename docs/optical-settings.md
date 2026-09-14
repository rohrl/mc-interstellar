# Optical settings and quality

The lab reads config/interstellar-optics.json whenever F8 opens it. In the development client this is run/config/interstellar-optics.json. The separate interstellar.json still controls the calibration HUD/reference scale. Existing files are never rewritten by the loader; missing fields inherit defaults, unknown fields are ignored, and invalid fields cause a logged fallback to defaults for that lab instance. Invalid files remain unchanged.

```json
{
  "lensing": true,
  "grid": true,
  "aligned": true,
  "falling": false,
  "lookBack": false,
  "startRadius": 8.0,
  "playbackRate": 1.0,
  "quality": "STANDARD"
}
```

Booleans require JSON true/false. startRadius is r/r_s, supported from 1.05 to 64 for a static frame and 0.35 to 64 for a falling frame. playbackRate is 0.05 through 4 and changes presentation speed, not relativistic equations. Quality names are case-sensitive FAST, STANDARD or FINE.

Q cycles integration quality in the current lab session. Other effect keys also remain temporary overrides. R restores the settings loaded when that lab opened. To persist choices, edit the JSON and close/reopen F8. Keyboard overrides are not automatically saved. Playback and numerical quality controls are distinct from the lensing on/off comparison; disabled lensing bypasses geodesic integration.

## Measured trade-offs

Measured 2026-09-14 at 2560x1440 on RTX 5070 Ti / NVIDIA 616.92, same default static r/r_s=8 scene, grid/aligned source on. Benchmark protocol: 120 warmup frames, 300 timestamp samples, VSync on, cap 120, render/simulation distance 12. These are single short runs of the optical pass only.

| Quality | Step radians | Maximum steps | GPU p50 ms | GPU p95 ms | Ray angular p95 rad | Ray maximum rad |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| FAST | 0.04 | 400 | 0.732064 | 0.735776 | 1.5031e-6 | 1.2409e-4 |
| STANDARD | 0.02 | 800 | 1.405440 | 1.413664 | 1.0402e-6 | 1.0085e-4 |
| FINE | 0.01 | 1600 | 2.757600 | 2.771168 | 1.2109e-6 | 8.6393e-5 |

All three 128x72 ray checks had zero invalid values, outcome mismatches or unresolved rays. Comparisons used the independent PG reference; these samples do not bound near-critical errors. Smaller integration steps do not guarantee monotonically smaller floating-point errors. FINE is a smaller-step option, not certified accuracy. No new near-critical/winding guarantee is made.

The shader now computes its accumulated integration angle from the integer iteration count instead of repeated float addition. Selectable loop/step handling raises STANDARD cost versus the earlier fixed-step shader (~1.18 ms p95). The angular budget remains 16 radians in every quality; FINE does not add higher winding orders beyond that budget. Unresolved rays remain magenta.

## Verification

Build and all 18 JUnit tests passed. Config tests cover missing defaults, unknown fields, malformed types, invalid ranges, invalid quality names and the static/interior restriction. Gson 2.10.1, matching Minecraft, is explicitly available to the test runtime.

Runtime checks verified default creation, Q cycling, V comparisons and B timings for all qualities. A partial config with lensing/grid disabled, falling=true, radius=4, rate=0.5 and FAST loaded upon reopening; its nonlensed draw measured p95 0.03344 ms. On 2026-09-15 an invalid quality value produced the intended logged fallback and the file was verified byte-for-byte unchanged. Original config was then restored and visually checked; V again reported zero outcome mismatches. Test configuration changes were not committed.

Diagnostics log the current quality and step; benchmarks include quality in their scene metadata. Playback rate is loaded and displayed, but a timed non-default-rate trajectory check has not been performed.
