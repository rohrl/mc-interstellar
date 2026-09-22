# Separate actor and cloud trees — 2026-09-23

Follow-up to the first proposal in [the profiling report](performance-profile-2026-09-23.md).

## Experiment

Partition the captured moving triangles into actors and native clouds. Build two
stackless trees, concatenate their nodes and rebase cloud escape links. Both use
the existing vertex/node textures. Preserve every vertex, attribute, triangle
intersection and cloud/material composition rule. Once the nearest cloud layer
has been consumed, skip the entire cloud tree. Optical integration, AA, terrain
geometry and actor/cloud update frequency are unchanged.

F9 `W` selects combined/separate moving trees; `Shift+W` saves a same-frame image
pair. Switching rebuilds the existing captured vertices without recapturing the
world. Steady-state live rendering builds only its selected representation each frame.
Developer work-counter programs use the selected layout; counters are not timed.

## Initial result: three independently cached searches

2560x1440 output, 1280x720 logical, two traced samples, render distance 12;
RTX 5070 Ti, Ryzen 5800X3D. Source N65, player 16.5/302/-45.5, yaw .281,
pitches .91 and 35.91. 6,260,834 terrain triangles, 8,608 moving triangles,
including 2,688 cloud triangles; 47 entities and two block entities.
The earlier profiling session had approximately 96 entities. Compare layouts
within this newly frozen capture; do not attribute differences from the earlier
session to this optimization.

Each timing uses 120 warmup frames and 300 measurements; ordinary GPU pass times
include AA resolve, exclude CPU capture/build and native world rendering.

| View | Combined GPU median, ms | Separate GPU median, ms | Result |
| --- | --- | --- | --- |
| Wall | 15.693 / 15.454 | 16.398 / 16.358 | About 5% slower |
| Down | 25.675 / 25.613 | 26.181 / 26.109 | About 2% slower |

Downward initial-pass moving node visits fall from 159.64M to 73.49M (54%).
Moving triangle entries fall from 73.62M to 63.54M; actor entries from 3.82M to
0.03M. Cloud entries remain 63.51M. Entries are counted before material rejection,
not all complete intersection tests. Static terrain work and optical step counts
are identical. Lower work counts do **not** establish lower GPU time: shader
control flow, per-ray state and hardware scheduling remain possible causes.

Both wall/down PNG pairs match exactly (including encoded-file hashes); numerical
image errors are zero. The downward contact sheet was visually inspected.
52,480 sampled optical comparisons and all 84 GPU material checks pass, with zero
mismatches/inconclusive/unresolved cases. The material fixture now covers static,
all-moving and mixed layouts, including empty actor/cloud forests, transparency,
cloud depth/order, reversed inputs and empty-cache toggles. 59 JUnit tests pass.

The instrumented down image retains the previously observed single exhausted
subpixel in both layouts; this change does not alter the optical budget.

Raw compact evidence: [profiles/2026-09-23-moving-trees](profiles/2026-09-23-moving-trees).
Large paired PNGs and runtime logs remain under ignored `run/`.

## Follow-up variants and accepted result

Removing the cloud empty-region cache preserves exact images, but does not win:
wall 16.386ms versus 15.249ms combined; down clean repeat 25.722ms versus 25.541ms.
This capture has 53 entities and 9,176 moving triangles. Removing the cache raises
moving node visits to 147.29M. Clean downward repeats were collected after a short
build overlapped part of the initial timing sequence; use the `*-clean` records.

The accepted variant keeps independent roots but traverses them sequentially inside
the original moving-tree loop, sharing its conservative cache. Skipping consumed
clouds is safe for that cache because clouds cannot contribute again during the
ray; both cache entries reset for every AA ray.

This final capture has 61 entities, two block entities, 10,260 moving triangles
and the same 6,260,834 terrain triangles. Same-capture repeated comparisons:

| View | Combined GPU median, ms | Accepted GPU median, ms | GPU time reduction |
| --- | --- | --- | --- |
| Wall | 15.105 / 15.193 | 14.583 / 14.667 | 3.46% |
| Down | 25.525 / 25.537 | 24.493 / 24.528 | 4.00% |

The downward GPU p95 improves from 26.337/26.362ms to 25.195/25.292ms;
wall p95 also improves. Reciprocal GPU throughput rises approximately 4.16% down
and 3.58% wall. These are controlled pass measurements, not a guaranteed whole-game
FPS uplift. Downward moving node visits fall from 153.44M to 64.10M (58.2%);
cloud entries from 70.05M to 65.94M, actor entries from 8.94M to 0.058M.
Optical steps and terrain work match exactly. This reinforces native cloud
primitive work as the next target; the previous actor-leaf estimates need revision
before investing in another hierarchy change.

Both heavy-view image pairs match exactly, including PNG hashes. All 52,480
optical comparisons and 84 material cases pass again. The additional restored-demo
pair has RGB MAE 0.00001755 and 0.0125% of pixels differing by more than 8/255.
Crop inspection locates differences on overlapping horse-face surfaces, consistent
with the existing unresolved coplanar-surface ordering limitation. Partitioning
changes triangle visitation order; native draw-order ties are not preserved by
the current nearest-hit renderer. Geometry and material data are unchanged; the
selection among overlapping surfaces can change. Do **not** call all scenes pixel-identical or claim this limitation
has been fixed. The full scene and a [diagnostic crop](profiles/2026-09-23-moving-trees/shared-cache-demo-crop.png) were inspected.

Live F10 at the same heavy pose, with actors updating: median GPU 24.805/24.551ms,
frame intervals 25.763/25.531ms, approximately **38.8–39.2 FPS**. This is a runtime
check, not a matched live-baseline claim; actor populations evolve. Current CPU
medians: moving-tree build 3.47ms, upload .17ms, actor capture .89ms, cloud capture
.19ms. Those also must not be compared causally with the older, larger population.

Decision: **adopt the shared-cache forest as the default**, retaining F9 `W` and
`Shift+W` for bounded comparisons. No second per-frame representation is built.
The two independently cached alternatives are rejected. Retained net code,
tests and shader resources: **+387 lines**, excluding docs and evidence artifacts.
59 JUnit tests and the Gradle build pass. Normal profiling-disabled launch
`split-default-runtime.log` loads N64 and the accepted F10 renderer without errors
or CPU/counter instrumentation. The current user pose, walking mode and 854x480
window are restored; the client is paused with F10 ready. Saved demo return data,
blocks, time and weather were not edited.

The two discarded cache variants are reproducible as shader patches against the
shared-cache implementation, alongside the recorded measurements. Lower traversal
counts alone are insufficient for promotion.
