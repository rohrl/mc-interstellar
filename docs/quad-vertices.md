# Shared native quad vertices — experiment, 2026-09-22

Owner approved this experiment and pushes to the existing repository. Baseline2843a50 is preserved on codex/demo-visual-refinement. No performance acceptance yet.

## Isolated implementation

- Pack each native pair (0,1,2), (2,3,0) into four12-float vertices; verify duplicated corners match bit-for-bit before packing. Preserve both triangle tests, diagonal interpolation, full precision, material flags and nonplanar geometry.
- Build a quad BVH with up to8 quad primitives per leaf. The existing reference BVH keeps up to8 triangles. This changes grouping/order and may change equal-depth ties; compare images before accepting it.
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
