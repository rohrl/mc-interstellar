# Shared native quad vertices — experiment, 2026-09-22

Owner approved this experiment and pushes to the existing repository. Baseline2843a50 is preserved on codex/demo-visual-refinement. The bounded storage experiment is complete and remains separate; it does not yet satisfy the no-regression gate.

## Isolated implementation

- Pack each native pair (0,1,2), (2,3,0) into four12-float vertices; verify duplicated corners match bit-for-bit before packing. Preserve both triangle tests, diagonal interpolation, full precision, material flags and nonplanar geometry.
- Initially build a quad BVH with up to8 quad primitives per leaf; the follow-up uses4. The existing reference BVH keeps up to8 triangles. This changes grouping/order and may change equal-depth ties; compare images before accepting it.
- Publish quad storage before the original tree reorders input triangles. Both representations use the same captured chunk data, camera, optical settings and moving scene. Separate compiled shader variants avoid adding a runtime storage branch to the old shader.
- Quad vertices use4092-wide rows,341 quads per row; original vertices use4095-wide rows,455 triangles per row. Candidate reserves10924 rows instead of16384. Node encoding/layout is unchanged. Chunk unload/replacement releases both allocations; source changes preserve the original shared capture policy.
- Moving entities/clouds retain original triangle storage in this first experiment. Both halves of a quad still execute triangle tests; reduced vertex storage/cache traffic and different BVH grouping are the opportunities, not a claim of halved intersection work.

The comparison build retains both arenas, so its total GPU memory increases. Production cleanup is required if accepted; do not mistake the candidate payload reduction for total memory saved by this A/B executable.

## Checks

Build/57 tests pass (quad-build.log). Added tests verify bit-exact nonplanar pair reconstruction and attributes, malformed-pair rejection, diagnostic active-half preservation, and conservative bounds/payload retention for a random quad tree.

The optical GPU fixture maps each independent test triangle to a quad with one degenerate half, alternating the active half while preserving its exact original vertex order. This tests both indexing branches without changing the CPU reference scene. The material fixture uses real two-triangle quads. Actual captured terrain pairs are still required to cover nondegenerate faces, materials and tree regrouping.

F9 backslash toggles triangle/quad mode; Shift+backslash saves a same-frame pair. C uses the selected diagnostic program. Benchmark both modes at fixed wall/down cameras, reverse order, then live if the result merits promotion. Retain resolution,2xAA, optical quality policy and actors throughout.

## First result: eight-quad leaves do not improve FPS

`quad-runtime.log`: 6,260,826 terrain triangles / 3,130,413 quads, 1,131,121 quad-tree nodes. Raw vertex payload shrinks from901,558,944 to601,039,296 bytes; parallel packing/build/upload adds1,586.63ms during capture. The diagnostic program passes52,480 independent optical comparisons and28 material cases with zero mismatches/errors. Same-frame wall pair12371985903889786516 and down pair6715631800563728647 both have zero pixel difference at1440p.

Frozen natural-world scene,120 warmup /300 measured frames,2560x1440 output,1280x720 logical,2xAA, selective materials, current observer-dependent optical policy. Original and quad programs use the same captured terrain/actors. GPU timings in milliseconds:

| View/mode | p50 | p95 | p99 |
| --- | ---: | ---: | ---: |
| Wall quad |16.032|17.755|18.470|
| Wall original |15.703|17.757|18.090|
| Down original |28.524|29.362|29.921|
| Down quad |28.827|29.667|29.890|
| Down original repeat |28.588|29.303|29.687|
| Down quad repeat |28.776|29.725|30.146|
| Wall original repeat |15.798|17.609|18.207|
| Wall quad repeat |15.976|17.793|18.251|

No FPS improvement; the downward view is about1% slower. Smaller storage alone is insufficient. One follow-up will cap leaves at four quads, matching the original maximum of eight triangle tests per leaf, to check whether doubled leaf work masked any benefit. Do not promote this eight-quad version.

## Follow-up: four-quad leaves help the heavy view, not every view

`quad-four-build.log`:57 tests/build pass. `quad-four-runtime.log`:same6,260,826 triangles,2,127,111 quad-tree nodes,1,754.38ms added packing/build/upload. All52,480 optical comparisons and28 material cases pass again. Wall pair11051665814164455547 and down pair7388782196313150312 are byte-identical original/candidate PNGs; CompareAppearance reports zero error. The first trial's wall contact sheet was also inspected. No scene geometry, rendering resolution, AA, optical settings or moving-object capture policy changed.

| View/mode | GPU p50 | GPU p95 | GPU p99 | Frame p50 | Frame p95 | Frame p99 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Wall quad |15.771|17.900|18.318|16.424|18.771|19.445|
| Wall original |15.682|17.783|18.318|16.358|18.720|19.361|
| Down original |28.579|29.392|29.676|29.296|30.214|30.716|
| Down quad |27.343|28.215|28.433|28.051|28.964|29.272|
| Down original repeat |28.546|29.402|29.770|29.231|30.333|30.618|
| Down quad repeat |27.309|28.202|28.700|27.978|29.145|29.553|
| Wall quad repeat |15.791|18.130|18.648|16.346|19.190|19.604|
| Wall original repeat |15.622|17.735|17.914|16.368|18.374|18.840|

All times are milliseconds, with the same frozen-scene protocol as above. Four-quad leaves reduce downward GPU median by4.3% and p95 by4.0–4.1%. Wall medians are roughly unchanged and wall GPU/tail timings are slightly worse. This is not a demonstrated across-view improvement or a new live-FPS result. It does not justify promoting the extra renderer complexity under the owner's no-regression constraint.

Keep the tested code on codex/quad-vertices-experiment and return the demo to its established renderer. A later, separately bounded follow-up could share the actual plane/intersection work for rectangular planar faces, falling back to both original triangles for other faces and retaining native diagonal shading. Merely sharing stored vertices still performs both original intersections and adds addressing work. This remains a proposal; it was not implemented or measured here.

Before any future promotion: eliminate parallel reference arenas, verify chunk/source update behavior, extend same-frame pairs to close/water/stairs views and full/selective composition, and measure actual F10 live timings. Current fixtures and two natural-world poses are sampled evidence, not arbitrary-model certification. No current production correctness bug was identified in this bounded storage experiment.
