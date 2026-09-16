# Stable exploration

## Behavior

Completed source selections depend only on chunks intersecting the horizontal bounding square of the enclosing sphere plus a one-block connectivity shell. Unrelated chunk loads/unloads and distant mass edits no longer clear them. Mass callbacks pass their changed position. Relevant changes still clear the source at the end of the tick; incomplete inspections retain conservative world epochs. This is bounded invalidation, not automatic cluster maintenance or reinspection.

F10 remains armed when the camera is below 1.05 r_s or beyond 128 blocks from the source. The normal-view HUD explains the pause; returning to the supported interval resumes rendering automatically. Pending captures and benchmarks are cancelled on transitions. Source/world invalidation, resource reload and rendering failures still stop the renderer.

F9 and F10 allow an observer up to 128 coordinate blocks from the source. Scene data remains the same source-centred 96^3 volume: terrain outside it is omitted, including potential foreground occluders. This is a wider view of a bounded exhibit, not full-world lensing. Amber marks missing data, pink unsupported material or exhausted ray budget.

Each straight ray chord is clipped against the scene box before voxel traversal. Chords before entry continue the curved orbit; traversal leaving captured data ends at the existing missing-data boundary. Outgoing rays beyond both the data-enclosing sphere and photon sphere can terminate as missing. The existing exterior orbit integrator and path budget are unchanged. Diagnostics and texture coordinates retain the actual traversed voxel rather than reconstructing it from a rounded hit position.

## Verification

Build and all 35 tests pass, including three source footprint regressions for negative coordinates, the neighbour shell across chunk edges and distant chunks. Runtime evidence and timings are recorded below after final verification.

Interim 96^3-camera-limit build: both boundary exits produced a normal-view paused HUD and automatically resumed after return. Screenshot inspected at run/stable-paused.png; log stable-runtime.log. One temporary mass block at (22,306,20) was placed only in air and subsequently removed; original N=63 source preserved. A distant teleport command was malformed and is not evidence of chunk-load stability.

Initial 116-block diagnostic exposed 17 flat mismatches. Detailed output traced affected coordinates to long-ray hit-position rounding; the shader now retains its traversed cell for reporting and UV lookup. Initial failing logs range-runtime.log and range2-runtime.log are local ignored files.

## Separate antialiasing checkpoint

Unverified two-sample antialiasing is preserved at 8ad46eb on codex/terrain-antialiasing, based on 585835c. It is not part of this branch. Its earlier build passed, but visual improvement and frame cost were not verified. Before continuing it: assess K on/off fixed-camera images and 1440p timings, verify JSON false, expose the option in the generated template, and choose the default from measurements. The external resume notes have been folded into this checkpoint.

## Launcher

Double-click Launch Interstellar.cmd in the repository. It runs the current checkout via Gradle, respecting JAVA_HOME and falling back to this machine's known JDK 21 location. It does not select a branch. Do not launch the same saved world in two clients. Runtime verification uses Gradle directly; the wrapper itself has not been launched as a separate GUI test.

## Final runtime checks — 2026-09-15

Final build: 35 tests passing; runtime shader compilation successful. All six 39x26 GPU comparisons had zero flat mismatches and zero sampled lensed unresolved rays. Output aspect 1.7791666667; standard 0.45 chord target; source COM=(15.9761904762,301.9761904762,16.0079365079), r_s=7.875. Player positions below have camera Y 1.6199998856 blocks higher.

| Player position | Yaw / pitch | Flat hits | Lensed opaque hits |
| --- | --- | ---: | ---: |
| (16.5,302,-100.5) | 0 / 0 | 50 | 67 |
| (-100.5,302,16.5) | -90 / 0 | 26 | 30 |
| (128.5,302,16.5) | 90 / 0 | 40 | 36 |
| (16.5,302,128.5) | 180 / 0 | 85 | 105 |
| (16.5,390,16.5) | 0 / 89 | 141 | 181 |
| (16.5,302,-19.5) | 0.84516 / 2.65028 | 479 | 641 |

F10 armed successfully at (160.5,302,-19.5), showed the 128-block pause, and resumed at (16.5,302,-100.5) without reinspection or another F10. This traversed distant chunks while retaining the selected source. At (16.5,302,12.5), F10 paused for the exterior limit; returning to the reference position resumed it. A mass block placed only in air at (22,306,20) invalidated the source and stopped F10; the normal-view invalidation message was visually inspected. Removed that confirmed test block and reinspected: N=63, r_s=7.875. Left the client in F10 at the reference position, window 870x519. World/weather-created terrain was preserved.

1440p output / 1280x720 internal, STANDARD path, lensing on, live refresh active, RTX 5070 Ti, NVIDIA 616.92, 120 warmup frames + 300 samples:

| View | Opaque cells at benchmark start | GPU p50 / p95 / p99 ms | Frame interval p95 ms |
| --- | ---: | --- | ---: |
| Reference near, r/r_s=4.51426 | 11589 | 5.001824 / 5.111360 / 5.171424 | 9.2592 |
| Distant, r/r_s=14.79628 | 11597 | 4.633792 / 4.707744 / 4.744768 | 9.1930 |

Both captures had zero unknown/unsupported cells. These are sampled optical-pass costs, not full-frame GPU timings or universal FPS certification. Scene occupancy differs from the earlier snow checkpoint, so this is not a controlled performance comparison. No independent curved finite-surface validation, multiplayer/dimension-transfer regression or source-chunk-unload runtime test was performed in this iteration.

Local ignored evidence: range3-runtime.log (final checks/timings), range-build.log, run/range-live.png (distant view), run/stable-invalidated.png, run/stable-final.png. Earlier failing logs remain local. Final distant and restored near images were inspected.

## Follow-up — automatic refresh

Relevant edits no longer require repeated inspection/F10: [source refresh](source-refresh.md) keeps an inspected anchor, withdraws stale metadata and resumes live rendering after a complete usable result. Earlier statements here that relevant changes stop F10 describe the prior stable-exploration checkpoint.
