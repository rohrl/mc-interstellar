# Per-actor hierarchy experiment — 2026-09-23

Original priority 3 of the [four-experiment goal](performance-experiments-1-4.md).
**Rejected for lack of FPS improvement.** Candidate preserved on
`codex/actor-hierarchy`; accepted production source remains `e324e84`.

## Candidate

Tag each native entity/block-entity quad with its owning capture. The collector
retains that owner until its fourth vertex is committed, even when commitment
occurs on the next entity's first vertex. Capture order and native payload remain
unchanged. Cloud faces remain in the accepted independent cloud tree.

Build one triangle tree per actor using all captured geometry, then a spatial
tree over those exact bounds. Flatten these trees into the current preorder
node format, rebasing escape links while retaining absolute primitive offsets.
No shader change, sampler, bound quantization, animation throttling or approximate
entity boxes. Keep raw triangles/owners together; rebuild only the selected sorted
upload. F9 `;` toggles grouped/global actor trees; `Shift+;` captures the same scene.

CPU tests check preserved full payload, conservative parent/leaf bounds, every
primitive visited exactly once, cloud partition/escape links, empty/single-actor
trees, interleaved owners, noncontiguous IDs and coincident group bounds. GPU
material cases now include a grouped moving layout, testing ordered transparency,
opaque occlusion, cloud consumption, reversed input and empty-cache variants.

## Validation and timing

Build passes 61 tests. Actual GPU shader passes 52,480 optical comparisons with
zero mismatches/inconclusive/unresolved cases, plus 112 analytic material checks.
These fixtures do not certify every actual scene ray or native coplanar surface.

Fixed heavy capture: RTX5070Ti, 2560x1440 output/1280x720 logical, sharp 2x AA,
render distance12, 6,260,834 terrain triangles, 13,456 moving triangles,
86 entities and two block entities. Same-scene actor hierarchy/global/hierarchy
comparisons use 120 warmup and 300 measured frames per run, repeated in H–G–G–H
order for wall and downward poses. Counter readback and image analysis occur
outside timing.

| View | Global actor tree, two GPU medians (ms) | Per-actor trees, two GPU medians (ms) |
| --- | --- | --- |
| Wall | 14.674 / 14.554 | 14.629 / 14.669 |
| Down | 24.438 / 24.454 | 24.435 / 24.442 |

The demanding view's means of medians are 24.446ms versus 24.438ms (0.03% less
time); the wall is 0.24% slower. Both are effectively ties, not a useful gain.
Wall/down PNG pairs are byte-identical; the downward contact sheet was inspected.
The existing instrumented exhausted subpixel remains sample1,x509/y554 (bottom
origin), in both baseline/candidate probe/full. No optical limits changed.

The accepted separate actor/cloud forest has already removed most actor leaf
work in this scene. This implementation does not justify retaining more tree
construction/capture bookkeeping. No live FPS gain or CPU speedup is claimed;
do not confuse this rejection with an evaluation of future tree refitting/reuse.
No live animation, first-person skin coverage or unsupported material feature
was changed. Original item4, conservative node compression, remains required.

Evidence: [timings, checks, work counts and images](profiles/2026-09-23-actor-hierarchy).
