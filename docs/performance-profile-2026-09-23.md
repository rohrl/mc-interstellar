# Rendering profile and revised priorities — 2026-09-23

## Decision

Prioritize **moving-scene GPU traversal**, particularly the shared cloud/actor tree. Profile evidence now supports this ahead of another terrain-only intersection change. Keep native geometry, live animation, current optics and sharp 2x AA. No performance optimization or visual approximation is adopted by this profiling branch.

The next bounded experiment should separate cloud and actor acceleration structures, retaining their original geometry, visibility and ordered composition. Measure against the normal shader in the heavy scene before extending the redesign. Cloud-specific quad/rectangular traversal and tighter per-actor bounds are subsequent candidates, not a commitment to a backend rewrite.

## Method and reproducibility

RTX 5070 Ti, NVIDIA 616.92, OpenGL 3.2 with timer-query support; Ryzen 5800X3D. Output 2560x1440, logical 1280x720, two traced AA samples, render distance 12, VSync on, cap 120. Natural-world source N=65; 6,260,826 terrain triangles. Player position 16.5/302/-45.5, yaw .281, pitch .91 (wall) or 35.91 (down). Frozen camera eye height 303.6199999. First capture: 97 actors, two block entities, 15,300 moving triangles including 2,688 cloud triangles. Second capture: 96 actors, two block entities, 15,204 moving triangles. Compare removals within each capture; these are not identical scenes across launches.

- Ordinary benchmarks: 120 warmup frames, 300 samples, asynchronous GPU timestamps. Baselines repeated after diagnostic variants. No builds during timing or JFR recording.
- Stage timestamps: two initial ray draws, mask copy, two selective material draws, sample fold, final reconstruction. Intervals include GPU scheduling/barriers and possible command-supply gaps; they are not isolated instruction costs. Compare their sum, not the sum of stage percentiles.
- Separate compiled counter programs: full-resolution per-fragment counts for both AA samples, probe/masked/full compositors. Blocking readback and instrumentation are excluded from production timings. **Triangle counters count leaf entries before material rejection**, including clouds skipped after a previous cloud hit; they are not all complete triangle-intersection calculations. Counters may change compilation and are not a timing profiler.
- Two shader removals: omit the moving tree entirely; omit the three lightmap reads used in material shading. Separate scene removals rebuild the moving tree with actors only or clouds only, using the normal shader. These deliberately alter the image and are diagnostic comparisons, not proposed quality settings. Changing work can change compilation, traversal and termination; savings are neither independent nor exact removable percentages.
- CPU wall-time samples around actor capture, cloud capture, moving-tree construction and upload. A 35-second JFR recording provides independent Java stack samples. CPU/GPU work overlaps; do not add CPU durations to GPU duration.

Launch diagnostics with `gradlew.bat runClient -PinterstellarProfile`. Ordinary launches do not register the extra programs. In a ready frozen F9 streamed scene: **B** is ordinary timing, **Shift+B** adds stage timing, **Ctrl+B** writes work-count CSVs under `run/profiles`; **backslash** cycles normal / no moving-tree shader / no lightmap reads; **Shift+backslash** cycles normal / actors-only / clouds-only moving geometry. Both diagnostic selectors must be zero for a baseline. **Ctrl+F12** adds live stage timing; F12 remains ordinary live timing. CPU summaries log per 600 updates while profiling is enabled. Counter capture requires selective composition; Z returns to that mode if necessary. The initial compilation of diagnostic shaders can be slow.

Raw retained evidence: [timings](profiles/2026-09-23/timings.csv), [frozen stage intervals](profiles/2026-09-23/frozen-stages.txt), [CPU batch](profiles/2026-09-23/live-cpu.txt), [JFR summary](profiles/2026-09-23/jfr-analysis.json). Work captures: first [wall](profiles/2026-09-23/work-1790116212385.csv)/[down](profiles/2026-09-23/work-1790116242576.csv), second [wall](profiles/2026-09-23/work-1790116708111.csv)/[down](profiles/2026-09-23/work-1790116742594.csv). Large raw JFR/JSON and GUI helpers remain ignored under run/.

## Measurements

### GPU medians, milliseconds

| Diagnostic comparison | Wall | Down |
| --- | ---: | ---: |
| First normal baseline | 15.973 | 27.197 |
| Entire moving tree omitted | 10.125 | 15.888 |
| Material lightmap reads omitted | 15.748 | 26.091 |
| First normal repeat | 16.043 | 27.189 |
| Full compositor instead of selective | — | 27.284 |
| Second normal baseline | 15.742 | 27.009 |
| Actors only: clouds omitted, tree rebuilt | 14.477 | 21.999 |
| Clouds only: actors omitted, tree rebuilt | 13.290 | 23.100 |
| Second normal repeat | — | 27.010 |

Heavy-view removal differences: all moving geometry ~11.3 ms, clouds ~5.0 ms, actors ~3.9 ms, lightmap reads ~1.1 ms. The separate removals do not sum to the complete-tree removal. Full/selective composition is effectively tied here; the earlier ~0.4 ms full-compositor advantage remains a different capture's result.

### Stage means in the first frozen scene, milliseconds

| Stage | Wall | Down |
| --- | ---: | ---: |
| First AA ray draw | 7.500 | 10.492 |
| Second AA ray draw | 7.933 | 12.872 |
| Mask-copy interval | 0.671 | 0.341 |
| Both selective material draws | 0.016 | 3.379 |
| Sample fold | 0.011 | 0.012 |
| Reconstruction | 0.061 | 0.061 |

The heavy view spends about 86% of this measured pass in the initial two ray draws. Only 739 of 1,843,200 rays (0.040%) require the selective material pass, but that pass still costs ~3.4 ms. Sparse work/long individual rays can matter much more than pixel coverage suggests. This identifies a future scheduling opportunity, not evidence that compaction would recover all 3.4 ms. In the full compositor the fold interval rises to ~0.33 ms; some transition cost moves between intervals, so the mask-copy number is not a promised saving from removing the copy.

### Work counts

Second downward probe, mean per AA ray:

| Work | Mean |
| --- | ---: |
| Integrated optical steps | 52.21 |
| Terrain BVH node visits | 242.64 |
| Moving BVH node visits | 116.50 |
| Terrain triangle entries | 4.50 |
| Cloud triangle entries | 35.31 |
| Actor/block-entity triangle entries | 7.27 |

Across both AA samples this is approximately 447 million terrain node visits, 215 million moving node visits, 8.30 million terrain triangle entries, 65.09 million cloud entries and 13.40 million actor entries per frame. Thus clouds account for about 83% of moving triangle entries, but actor removal still saves substantially: entry counts alone do not attribute elapsed time.

The wall averages ~57 optical steps per ray, slightly MORE than the slower downward view. Increased integration count does not explain the heavy-view slowdown. This supports traversal work as the first target, while leaving the absolute removable integration cost unmeasured. Node counts also justify considering conservative BVH compression. They do not measure DRAM bytes: caches reuse many fetched nodes.

All four count files pass consistency checks: 1,843,200 probe rays and masked-ray count equal to pending-probe count (zero at wall, 739 down).

### Live CPU profile and limits

First 600-update batch before JFR:

| CPU work | Mean ms | p50 ms | p95 ms |
| --- | ---: | ---: | ---: |
| Actor/block-entity capture | 1.523 | 1.343 | 1.785 |
| Cloud capture | 0.251 | 0.184 | 0.775 |
| Moving BVH build | 5.905 | 5.833 | 6.173 |
| Moving upload | 0.216 | 0.209 | 0.238 |

Before JFR, live downward GPU/frame medians are 27.930/29.019 ms (~34.5 FPS). The separately instrumented live stage run gives 27.688/28.827 ms. Moving-tree construction is a real CPU hotspot, but its ~5.9 ms mostly overlaps GPU work; eliminating it would not automatically turn a 29 ms frame into 23 ms.

JFR's heavy-view subset contains 425 render-thread Java execution samples; 320 contain MeshTree (75.3%). Top frames include build (220) and swap (97), consistent with repeatedly scanning bounds and moving complete 36-float triangle records while sorting. These are shares of sampled Java execution, not shares of total frame time or all native CPU execution. Refitting/reusing trees or sorting indices may help CPU headroom and allocation pressure after the GPU work improves.

Two collection limitations are explicitly retained: the initial live stage shortcut used Shift, causing slight creative-flight descent; it is now Ctrl+F12. Also, the helper initially treated JFR file creation as recording completion. The final live repeat/wall timings overlap JFR and are marked `jfrOverlap=True` in the CSV; do not use them as clean FPS comparisons. The recording's heavy portion is selected by timestamp before the wall teleport at 08:41:59 +10:00. Frozen comparisons and the first live timings preceded JFR and are unaffected. No extra run was needed to answer the ranking question.

No hardware shader-stall/register/DRAM-throughput capture was collected. This session identifies expensive scene work and CPU functions; it does not prove whether each GPU traversal instruction is limited by arithmetic, cache latency, bandwidth or occupancy.

## Revised ranking and estimates

Gain estimates below mean potential FPS gain in the current heavy view after a successful implementation, not removal-test speedups. They remain provisional, can fail or regress, and overlap. Complexity is technical difficulty, size is scope, time includes validation (1 smallest/easiest/shortest, 5 largest/hardest/longest). Visual impact: 1 no intended loss, 2 tiny possible differences, 5 major compromise. Net lines include retained code/tests, exclude docs and generated/temporary helpers.

| Priority / idea | Complexity | Size | Time | Net LoC | Heavy FPS gain | Visual impact / confidence |
| --- | ---: | ---: | ---: | ---: | --- | --- |
| **1. Separate cloud and actor traversal/cache regions — new** | 3 | 3 | 3 | +350–800 | **5–15%** | 1; strong hotspot evidence, medium confidence in opportunity, unmeasured gain |
| **2. Cloud-specific shared quads/rectangular intersections — new** | 3 | 3 | 3 | +250–650 | **3–10%** | 1–2; retain native faces, cutouts, colours and cloud depth rules |
| **3. Tighter per-actor hierarchy / traversal — new** | 4 | 3 | 4 | +500–1,100 | **3–10%** | 1; retain every animated part; medium hotspot confidence |
| **4. Conservative compressed BVH bounds** | 4 | 3 | 4 | +250–600 | **0–8%** | 1; node traffic is frequent, hardware bottleneck still unknown |
| **5. Table-assisted optical integration** | 5 | 4 | 5 | +800–2,000 | **5–20%, still low confidence** | 2; absolute integration cost not isolated; critical-ray fallback essential |
| **6. Moving-tree refit/reuse** | 4 | 3 | 4 | +350–800 | **0–3% now** | 1; strong CPU evidence, mostly headroom rather than current FPS |
| 7. Planar intersections for terrain quads | 3 | 3 | 3 | +200–500 | **0–4%**, reduced from 3–12% | 1–2; terrain primitive work is relatively small |
| 8. Packed colour/light attributes | 3 | 3 | 3 | +200–450 | **0–4%** | 2; shading fetches are a smaller target than node/geometry work |
| 9. Sparse transparency scheduling — new, conditional | 4 | 3 | 4 | +500–1,200 | **0–8%** | 1; low confidence; requires scheduling support and avoids changing composition |
| 10. Adaptive full/selective compositor | 3 | 2 | 3 | +150–350 | **0–2%** | 1; current capture tied, previous small gain scene-dependent |
| 11. Static block-entity geometry cache | 3 | 2 | 3 | +200–400 | **0–1% here** | 1; two block entities, CPU mostly overlaps GPU |
| Deferred: whole compute backend | 4 | 4 | 4 | +800–2,500 | 0–15%, very low confidence | 1 if math preserved; prefer targeted changes first |
| Deferred: hardware ray-tracing backend | 5 | 5 | 5 | +4,000–12,000 | Not credibly bounded | 1–2; segmented curved rays and interop remain |
| **Already adopted: shared terrain quad vertices** | 3 | 3 | 3 | **+292 actual** | **~4–5% controlled frozen FPS** against its previous baseline | **1; matched images identical** |

The priority-1 prototype is intentionally bounded: separate the spatial searches, retain original triangle tests, compare pixels and optical/material fixtures, then repeat wall/down timings in both orders. Use work counters to check that the intended node/entry reduction actually occurred. Combine neither cloud approximations nor another optical-step change into that experiment. An isolated shader/backend move is not justified by these results. Existing rejected AA/SAH/final-shading/facing experiments remain deferred.

## Correctness and final state

The instrumented downward shader flags one budget-exhausted subpixel (sample 1, x=509, bottom-origin y=554) out of 1,843,200, repeated in both counter captures; wall has none. Maximum optical count is 800, consistent with the retained near-critical angular/winding guard. This warrants a focused production-shader reproduction; instrumentation can change compilation, and it is not established as a new regression. Do not raise numerical limits silently during profiling.

The normal quad diagnostic passes 52,480 independent optical comparisons with no mismatches/inconclusive/unresolved cases; all 28 material cases pass. This sampled fixture does not cover every actual screen ray, as the counter finding demonstrates. The restored small-window N=64 demo/F10 view was visually inspected. Player dimension, original position/rotation and creative flight were restored; no blocks, weather settings or demo return record were changed.

Final Gradle build passes all 57 tests. An ordinary launch (profile-default-runtime.log) loads the restored N=64 demo and confirms that diagnostic shaders and CPU logging are disabled; client left paused with F10 ready. Profiling-only shader variants and counters are opt-in. Default ray equations, geometry storage, AA, cloud/actor update policy and appearance are unchanged. Step 5 remains deferred.
