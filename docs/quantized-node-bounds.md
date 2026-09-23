# Conservative compressed bounds — 2026-09-23

Original item4 of the [four-experiment goal](performance-experiments-1-4.md).
**Rejected: about20% more heavy-view GPU time.** The complete candidate is
preserved on `codex/quantized-node-bounds`; normal source remains `e324e84`.

## Representation

Each box uses three pairs of signed 16-bit coordinates on a 1/16-block grid,
plus its existing escape link, in one `RGBA32UI` texel. Minima round down and
maxima round up. Original triangle/quad vertices and material data remain exact.
Nodes outside [-2048,2047.9375] use their original float bounds; no geometry is
clamped or silently dropped. The original leaf descriptor is fetched only after
the box passes, preserving the current leaf/jump format and large-leaf fallback.

This first candidate retains the full original node texture for descriptors,
overflow fallback and the same-scene comparison. Thus it tests fewer bound fetches
and tighter packing, **not lower total allocated VRAM**. It adds a companion
texture and upload; any retained version needs live timing as well as frozen GPU
measurements. The extra decode arithmetic and larger boxes can offset fetch
savings. No shader hardware bandwidth bottleneck has been established.

F9 `;` toggles float/quantized bounds, `Shift+;` saves the same-scene pair.
Diagnostic and counter programs use the selected storage path. Tests/timings
are recorded below; this candidate is not enabled in the normal checkout.

## Checks

Build passes all62 unit tests. New tests exhaust every signed16-bit grid value
and its neighbouring floats, verify outward enclosure and fallback boundaries,
test multirow records and exact escape links, and reject invalid payloads.
The actual GPU material fixture is expanded to include translated scenes outside
the packed coordinate range, exercising exact fallback for static and moving
nodes. Runtime shader compilation passes, as do52,480 optical comparisons (zero
mismatches/inconclusive/unresolved cases) and168 material checks (zero error).
The wall and down PNG pairs are byte-identical; the downward contact sheet was
visually inspected. The previously known instrumented exhausted subpixel remains
sample1,x509/y554 (bottom origin), probe/full, in both programs; no optical limits
were changed. These are sampled checks, not a universal renderer certification.

## Paired results

RTX5070Ti,2560x1440 output/1280x720 logical,sharp2x AA,render distance12. Frozen
capture:6,260,834 terrain triangles,13,074 moving triangles,82 entities and two
block entities. P–F–F–P order,120 warmup frames and300 samples per run:

| View | Float bounds, two GPU medians (ms) | Packed bounds, two GPU medians (ms) |
| --- | --- | --- |
| Wall | 14.314 / 14.200 | 17.056 / 16.337 |
| Down | 24.515 / 24.513 | 29.435 / 29.412 |

Downward means of medians:24.514→29.423ms,20.03% more time (16.69% less GPU
throughput). Wall is17.1% slower, with more variation between packed repeats.
Tails also regress. This clearly fails the FPS gate; no additional live benchmark
is needed to reject it. The initial scene capture took36.19s, an observation
rather than a paired startup comparison.

Probe node visits rise only0.38% for terrain (448.03M→449.72M) and0.49% for moving
geometry (61.88M→62.18M). Optical steps are identical. Cloud entries rise0.76%;
actor entries91,065→112,388. The modest node-count changes do not establish the
cause of the large slowdown. Decoding, dependent descriptor reads, texture/cache
behaviour and register use are possible contributors; none is isolated by these
counters. One fewer bound fetch is not a timing guarantee.

Reject this tested representation, preserving the code and evidence. This is not
proof that every chunk-relative or different packed layout must be slower. The
four requested original proposals have now each been implemented and measured;
only the separate actor/cloud forest earned retention.

Evidence: [timings, checks, counts and image metrics](profiles/2026-09-23-quantized-bounds).
