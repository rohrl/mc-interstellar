# Original four-experiment scope

The active goal is to try items 1–4 of the revised priorities at `e324e84`, before
later evidence changed the ranking. Keep these requirements fixed:

| Original item | Experiment | Evidence / status |
| --- | --- | --- |
| 1 | Separate actor/cloud trees | Completed, accepted; [report](moving-trees.md), implementation `e324e84` |
| 2 | Native cloud shared quads / rectangular intersections | Completed, rejected; [report](cloud-quads.md), preserved experiment `9815c42` |
| 3 | Tighter per-actor hierarchy / traversal | Completed, rejected; [report](actor-hierarchy.md), preserved on `codex/actor-hierarchy` |
| 4 | Conservative compressed BVH bounds | Still required; not started |

Later reprioritization does not replace item 3 with optical tables or tree refits.
Each experiment requires an implemented candidate, relevant correctness checks,
paired fixed-scene timings/images, a keep/reject decision and preserved evidence.
Only retained changes need subsequent live/default-launch validation. Preserve
the current quality, all supported geometry and live update cadence. Changed
capture populations are not causal comparisons. The goal remains active until
all four original proposals have been tried and reviewed.
