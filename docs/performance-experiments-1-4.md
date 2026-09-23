# Original four-experiment scope

The active goal is to try items 1–4 of the revised priorities at `e324e84`, before
later evidence changed the ranking. Keep these requirements fixed:

| Original item | Experiment | Evidence / status |
| --- | --- | --- |
| 1 | Separate actor/cloud trees | Completed, accepted; [report](moving-trees.md), implementation `e324e84` |
| 2 | Native cloud shared quads / rectangular intersections | Completed, rejected; [report](cloud-quads.md), preserved experiment `9815c42` |
| 3 | Tighter per-actor hierarchy / traversal | Completed, rejected; [report](actor-hierarchy.md), experiment `7a168fb` |
| 4 | Conservative compressed BVH bounds | Completed, rejected; [report](quantized-node-bounds.md), experiment `e62a71c` |

Later reprioritization does not replace item 3 with optical tables or tree refits.
Each experiment requires an implemented candidate, relevant correctness checks,
paired fixed-scene timings/images, a keep/reject decision and preserved evidence.
Only retained changes need subsequent live/default-launch validation. Preserve
the current quality, all supported geometry and live update cadence. Changed
capture populations are not causal comparisons.

## Outcome

Only item1 is retained:4.0% less heavy-view GPU time (about4.2% more throughput).
Items2 and3 are effectively tied with that renderer. The tested item4 layout
takes20.03% more heavy-view GPU time and is rejected. Geometry, optics, AA and
live update cadence in the normal checkout remain the accepted `e324e84` source.
Each report links the paired timings, correctness checks, image metrics and
preserved implementation. Final clean build/packageDemo passes59 tests. A normal
profiling-disabled launch verifies N64/F10 readiness and restores the exact saved
demo position, rotation and walking mode; client paused at854x480. Production
source matches `e324e84`, so rejected candidates add no default runtime overhead.
[Final verification](profiles/2026-09-23-four-final.txt). All four original items
are complete; remaining untried proposals are outside this goal.
