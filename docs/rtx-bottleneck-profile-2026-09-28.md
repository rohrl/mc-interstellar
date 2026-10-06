# Where the current black-hole renderer spends its time

> Historical implementation report or proposal. Results, defaults and pending work describe the recorded checkpoint, not necessarily the current mod. See the [current guides and status](README.md).

**Result: repeated scene searches are the main target. RTX is worth a representative
prototype; optical lookup tables are a lower priority for these views.** This profile
does not measure an RTX version of Minecraft. It connects the earlier isolated RTX
test to the work our current renderer actually performs.

Measured 28 September 2026 on RTX 5070 Ti, driver 616.92, Ryzen 5800X3D,
Java 21, Minecraft 1.21.1. Production baseline: `5642aa7` / renderer `fb0c827`.
The branch adds opt-in diagnostics and a missing-source crash guard; optical equations,
quality settings, native materials and production traversal are unchanged.

## 1. Ordinary renderer timings

Output 2560×1440; logical 1280×720 with two traced AA samples; distance 12,
VSync on, cap 120. Each run has 120 warmup frames and 300 measured samples.
These timings use the **normal shaders**, without invocation clocks.

| View / mode | Optical GPU median | Repeat | GPU p95, repeat | Median frame interval, repeat |
| --- | ---: | ---: | ---: | ---: |
| Frozen inspection, down over terrain | 22.585 ms | 22.573 ms | 25.095 ms | 23.741 ms |
| Frozen inspection, facing wall/hole | 10.422 ms | 10.406 ms | 11.667 ms | 11.177 ms |
| Live world, down over terrain | 22.578 ms | 22.522 ms | 25.148 ms | 24.593 ms |

The live run is about **40–41 FPS** by reciprocal median frame interval, with normal
world ticking and moving entities. The first live run has intermediate stage timestamps;
the repeat has only the original start/end timestamps. This is not a historical
before/after FPS improvement: source calibration, actor population and weather differ
from earlier sessions. The player is a spectator in these measurements; live snow is
visible, and there is no held-item draw. Frozen inspection omits live weather overlays.

Stage **means** from the repeated frozen samples:

| GPU interval | Down | Wall |
| --- | ---: | ---: |
| Both initial ray draws | 18.167 ms | 9.967 ms |
| Copy pending-material mask | 0.279 ms | 0.380 ms |
| Both selective material draws | 4.481 ms | 0.017 ms |
| Fold the two AA samples | 0.015 ms | 0.013 ms |
| Final reconstruction | 0.061 ms | 0.061 ms |
| Sum of stage means | 23.004 ms | 10.438 ms |

Initial rays are 79.0% of the down-view pass and 95.5% of the wall-view pass.
The material draws also retrace paths and search geometry: their entire duration is
not texture shading. Stage means sum to the mean, not to the median in the first table.
Intervals include scheduling/barriers and possible gaps in command supply.

## 2. What happens inside a ray

The new diagnostic uses `ARB_shader_clock` to time regions **within each shader
invocation**. One variant times the complete geometry-query function. A second also
times candidate appearance handling and the orbit-step calculation. It emits the
accumulated counters into a float texture for offline readback.

Think of each ray as repeating this loop:

```text
advance the curved path → search this straight chord → deal with its hit
          ↑                                             │
          └──────────── continue if needed ──────────────┘
```

Shares below are ratios of summed instrumented invocation latency. **They are not
direct percentages of GPU wall time, and are not independently removable costs.**

| Region, initial ray pass | Down | Wall |
| --- | ---: | ---: |
| Geometry search, excluding nested candidate shading | 87.0% | 76.4% |
| Orbit step: adaptive step, integration, endpoint construction | 3.2% | 7.3% |
| Candidate UV/alpha/appearance work inside the query | 0.7% | 0.5% |
| Other: setup, cache/continuation work outside query, final appearance, etc. | 9.1% | 15.8% |

“Geometry search” includes tree/bounds traversal, geometry loads, primitive tests,
query bookkeeping and reuse checks *inside* `meshSegment`. It is broader than a
single triangle test. “Candidate shading” is narrower than all material/lighting work;
appearance work outside that region remains in “other.” GPU memory latency,
divergent lanes and scheduling contribute to measured invocation latency.

The less intrusive variant attributes **86.2% down / 75.4% wall** to the whole query.
The detailed variant gives **87.7% / 76.9%** before subtracting candidate shading.
That agreement supports the broad conclusion that queries dominate. It does not
make the detailed percentages exact.

The down-view masked material pass has only **2,431 active rays out of 1,843,200
(0.132%)**, yet takes about **4.48 ms** in the normal renderer. Its query share is
83.8% in coarse clocks versus 70.4% in detailed clocks: substantially less stable.
Do not combine these into a precise whole-frame removable percentage. The wall view
has no active material rays. Sparse difficult rays and partially occupied execution
groups remain an interesting separate optimization target.

### The profiler changes the program

| Paired colour-output draw | Normal GPU median | Coarse clocks | Detailed clocks |
| --- | ---: | ---: | ---: |
| Down, both initial samples | 17.375 ms | 17.881 ms (+2.9%) | 21.457 ms (+23.5%) |
| Wall, both initial samples | 9.470 ms | 10.045 ms (+6.1%) | 11.695 ms (+23.5%) |
| Down, both masked samples | 4.345 ms | 4.672 ms (+7.5%) | 4.900 ms (+12.8%) |

These are paired diagnostic draws to RGBA32F, excluding intermediate stage queries
and full-frame work. They should not equal the preceding production stage totals.
Seven timed pairs follow three warmups, alternating execution order. Clock-output
draw timings are retained separately in the raw CSVs.

Shader clock reads are code-motion barriers. Instrumentation can change compilation,
register allocation, execution overlap and occupancy. The extension does not define
clock units as nanoseconds or guarantee a constant rate. Therefore the table above
is **latency attribution with measured interference**, not a hardware stall/occupancy
profile. Nsight shader instruction sampling was not performed.

For each view and pass, both instrumented variants were also run in colour-output
mode and compared with the normal program. **All float colour components matched
exactly**; all recorded clock rows were finite with zero invalid rays. This verifies
the sampled outputs, not every view, horizon case or GPU. Normal timing repeats were
within 0.15% in both frozen views. Representative wall/live-down screenshots were
inspected; no movement/flicker suite was run, per owner preference.

## 3. Does this make RTX worthwhile?

Yes, as the next **measurement prototype**. The expensive region substantially
overlaps the work hardware traversal accelerates. The [previous standalone
test](rtx-probe-2026-09-24.md) made curved-chord intersection queries 3.26–3.76× faster.
This profile makes that result more relevant than a fast isolated kernel with no
evidence that it matters to our renderer.

It still does not establish the fraction of a live frame RTX can remove. Our search
includes bookkeeping and existing empty-space reuse. Hardware acceleration also
requires different geometry structures, native material handling, dynamic updates
and OpenGL interoperability. CPU/GPU work overlaps, so CPU durations cannot simply
be added to or subtracted from the GPU pass.

To show the scale without pretending we have a forecast, the following are
**conditional scenarios**. Suppose a fraction of the original frame is accelerated
by 3.26–3.76×, the remainder is unchanged, and the new backend adds no extra cost:

| Assumed accelerated fraction of whole frame | Resulting FPS multiplier | From this run's ~40.7 FPS |
| --- | ---: | ---: |
| 50% | 1.53–1.58× | ~62–64 FPS |
| 65% | 1.82–1.91× | ~74–78 FPS |
| 80% | 2.25–2.42× | ~91–99 FPS |

These are arithmetic examples, **not a predicted range or confidence interval**.
New frame time is `old × (1 − fraction + fraction / querySpeedup) + new overhead`.
The scenarios intentionally vary the unknown frame fraction rather than equating
an invocation-latency percentage with it. Even 1–2 ms of new overhead matters once
the original expensive work is shortened; CPU work can also become the limit.

## 4. Revised priorities

| Priority | Experiment | Why now / remaining uncertainty |
| --- | --- | --- |
| 1 | RTX replay with captured native geometry and actual chord logs | Highest demonstrated overlap with the expensive work. Retain software baseline and hit/material equivalence checks. More representative workloads must earn a live-backend trial. |
| 2 | Moving-tree reuse/refit on CPU | Current 600-update batches show ~3.7–3.9 ms tree construction, ~1.0–1.1 ms actor capture, ~0.19 ms clouds, ~0.16–0.18 ms upload. Lower integration risk; overlap means FPS savings may be smaller than CPU savings. |
| 3 | Compact sparse material work, or improve its scheduling | 0.132% active rays consume ~4.48 ms. Useful lead; do not assume queue construction or compaction will recover all of it. |
| 4 | Table-assisted optical trajectories | Still interesting, but orbit-step latency is only ~3–7% in the measured initial rays. Error-controlled tables are less attractive before addressing search. Very close/critical-ray views may rank differently. |

The next RTX gate should include larger native geometry, real chord distributions,
alpha-tested leaves, translucent continuation and material checks. Then measure
acceleration-structure updates and OpenGL/Vulkan resource sharing. Only a correct
full image with measured live timing justifies a production migration. None of that
backend work is implemented by this profiling change.

## Reproduction and retained evidence

Launch `gradlew.bat runClient -PinterstellarShaderClocks`. This separately enables CPU
and stage summaries plus eight clock programs; it does not enable the older full
counter/ablation shader set. Normal launches register none of these extra programs.
An implementation supporting `GL_ARB_shader_clock` is required for the clock capture.

1. Select a black-hole source and wait for discovery before freezing the world.
2. F9 opens inspection; Shift+M captures the streamed native mesh. That switch initially
   turns lensing off: **Space restores lensing**. Wait for `Streaming terrain ready`.
3. Keep default selective materials, split AA, separate moving geometry and 2× AA.
   **B** runs ordinary timing; **Shift+B** adds stage timestamps. Clear a completed
   benchmark with B before starting another.
4. **Alt+B** writes coarse/detailed clock CSVs and paired colour/overhead checks under
   `run/profiles`. Blocking readback is diagnostic only and cancels an active benchmark.
5. Repeat normal timing, then use **Ctrl+F12** for stages or **F12** for ordinary timing
   in live F10 mode. Do not benchmark during capture, compilation or other GPU work.

Clock capture currently supports selective split-moving black-hole programs, including
the horizon variants, not extended subcritical sources. This session measures only
the ordinary exterior programs; horizon-clock programs compiled but were not profiled.
One setup benchmark with lensing off is retained and explicitly excluded from findings.

Scene: eye `(16.5,303.6199998855591,-45.5)`, yaw `.281`, pitch `35.91` down / `.91` wall;
N65 at `(16.0076923,302.0384615,15.9923077)`, rs `3.5182282`, range `17.48454 rs`.
6,261,506 terrain triangles represented as 3,130,753 quads; 72 captured entities,
5 block entities, 12,184 moving triangles including 2,688 cloud triangles.
Initial terrain capture was ~32.1 s. It is not in frame timing.

- Raw clocks and equivalence/overhead metadata: [down](profiles/2026-09-28-rtx-bottleneck/down.csv),
  [down checks](profiles/2026-09-28-rtx-bottleneck/down.csv.txt),
  [wall](profiles/2026-09-28-rtx-bottleneck/wall.csv),
  [wall checks](profiles/2026-09-28-rtx-bottleneck/wall.csv.txt).
- [Normal timing log](profiles/2026-09-28-rtx-bottleneck/timings.txt),
  [run labels](profiles/2026-09-28-rtx-bottleneck/labels.txt),
  [CPU batches](profiles/2026-09-28-rtx-bottleneck/live-cpu.txt),
  [derived JSON](profiles/2026-09-28-rtx-bottleneck/summary.json).
- Recompute JSON with Python 3: `python tools/analyze-shader-clocks.py docs/profiles/2026-09-28-rtx-bottleneck`.
- [Clock specification](https://registry.khronos.org/OpenGL/extensions/ARB/ARB_shader_clock.txt):
  undefined/local clock units, counter semantics and code-motion barriers.

Build passes the existing 79-test suite. All eight diagnostic programs compiled in
game. A small existing crash was fixed: renderer controls could consult the horizon
predicate before source discovery, dereferencing a null source. It now checks source
and camera availability. No optics equation changed; the complete optical fixture
suites were not rerun. Normal client restart was checked with profiling disabled;
owner position/dimension/rotation/mode/flight/inventory/health and window were restored.
