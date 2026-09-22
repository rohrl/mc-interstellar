# Shared native quad vertices — experiment, 2026-09-22

Owner approved this experiment and pushes to the existing repository. Baseline2843a50 is preserved in history. The original storage comparison is preserved on codex/quad-vertices-experiment at49738ba. After seeing the results, the owner explicitly accepted a gain in the heaviest view with a small regression in the easier view. The consolidated four-quad implementation is accepted on codex/quad-vertices and codex/demo-visual-refinement. The earlier strict no-regression conclusion below is superseded by that clarification.

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

All times are milliseconds, with the same frozen-scene protocol as above. Four-quad leaves reduce downward GPU median by4.3% and p95 by4.0–4.1%. Wall medians are roughly unchanged and wall GPU/tail timings are slightly worse. This is not a demonstrated across-view improvement or a new live-FPS result. Initial decision was to keep the experiment separate under the strict no-regression constraint; the owner then explicitly accepted this tradeoff.

Keep the original A/B code on codex/quad-vertices-experiment. A later, separately bounded follow-up could share the actual plane/intersection work for rectangular planar faces, falling back to both original triangles for other faces and retaining native diagonal shading. Merely sharing stored vertices still performs both original intersections and adds addressing work. This remains a proposal; it was not implemented or measured here.

## Production consolidation

StreamingTerrain now retains one4092-wide quad vertex arena and one compact node arena. The parallel original representation and redundant expanded-node arena are removed; existing chunk queues, allocation replacement/release and source tracking remain. Moving actors/clouds retain their original triangle layout. Both native triangles and all attributes retain full precision.

Shader selection follows the actual storage format independently of AA, bending and diagnostic switches. The optimized split/selective shaders serve the normal preset; the general quad shader handles other settings. This prevents a fallback from decoding quad offsets as triangles. F9 general/optimized comparisons label the actual program comparison; the historical storage A/B control stays on the experiment branch.

Additional close natural-world pair4908299161076398200 (camera16.5,303.62,-24.5, yaw.281,pitch.91) is byte-identical original/quad. Pair8788163427737826186 is byte-identical full/selective quad composition. An earlier attempt was rejected because the physical camera had not settled; it supplies no acceptance evidence. Build/package57 tests pass. These are sampled checks, not arbitrary-model certification.

The retained terrain-arena allocation arithmetic changes from1,610,219,520 to983,586,048 bytes (597.6MiB less), including removal of the expanded node copy. Raw captured vertex payload alone shrinks by one third. These are requested texture-storage sizes, not a measurement of total process/driver VRAM.

## Final live measurements

Fresh reference `quad-live-reference-runtime.log`, final `quad-production-runtime.log`; same natural-world source,6,260,826 triangles, camera16.5/302/-45.5,yaw.281,pitch.91 or35.91,1440p/half-scale/2xAA,120 warmup/300 samples. Each sequence is wall/down/down/wall. Actors, clouds and time continue to evolve; the final capture initially contains94 actors versus83 in the reference. These live runs do not isolate the storage speedup as precisely as the frozen same-scene comparison.

| View/run | GPU p50/p95/p99 ms | Frame p50/p95/p99 ms |
| --- | --- | --- |
| Reference wall1 |15.757/17.372/18.011|16.659/19.001/20.255|
| Reference down1 |28.060/29.021/29.523|29.144/30.720/31.176|
| Reference down2 |27.889/28.826/29.241|28.863/30.423/30.975|
| Reference wall2 |15.903/17.429/17.798|16.649/19.034/19.936|
| Quad wall1 |16.488/18.326/18.897|17.490/20.343/20.849|
| Quad down1 |28.046/28.931/29.521|29.156/30.994/32.015|
| Quad down2 |27.578/28.566/28.952|28.770/30.348/31.007|
| Quad wall2 |15.464/17.532/18.111|16.641/18.936/19.672|

Final live medians are about57–60FPS wall and34–35FPS down. The first final wall run and some tail values are worse; retain them in the comparison. Acceptance uses the repeatable4.3% frozen heavy-view gain and the owner's explicit tradeoff clarification, not a claim that every live metric improved.

Initial large-world capture was37.691s in the parallel reference executable and36.264s in the final build. Earlier accepted builds measured34.686–35.173s; differences in run state and chunk scheduling prevent an isolated loading-time claim. No additional run was performed solely for loading time, per the owner. The small exhibit previously captured in4.933s and is not a substitute for the heavy-world benchmark.

## Final correctness and demo checks

Final compiled quad diagnostic:52,480 optical comparisons, zero mismatches/inconclusive/unresolved;28 material cases, zero failures/channel error. General/optimized quad pair3754333847832677599 and full/selective pair4351572167902373288 are pixel-identical at the close camera. Zero-bending versus vanilla pair15362894805045391712 has RGB MAE0.00090319,0.8355% pixels over8 levels; its contact sheet was inspected. It checks native appearance and the no-AA/no-bending fallback, not equality with Minecraft's complete renderer. Remaining ordinary appearance differences and unsupported special layers are unchanged in scope.

The retained exhibit loads in5.081s in the final run, with53,488 triangles. Screenshot2026-09-22_18.51.24.png was visually inspected: curved coloured wall, terrain, slab/stair area, tree, glass/water exhibit, and live mobs. A guarded temporary mass block was placed only into air at4,80,0; F10 automatically refreshed64→65→64 without a terrain reload. The block was removed in the helper's finally section. The owner saved demo return record was preserved by entering from within the exhibit. Subsequent owner exploration also logged chunk-window retention/replacement without a rendering error; this is not an automated movement/flicker test.

No further confirmed production correctness defect was found. The storage-format fallback issue was prevented during consolidation; existing shader feature/hardware requirements and material limitations remain. The actual new program was verified in F10; no reliance on an F9-only switch. Step5 remains deferred.
