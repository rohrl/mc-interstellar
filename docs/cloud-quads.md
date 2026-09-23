# Native cloud face intersections — 2026-09-23

Follow-up to [separate moving roots](moving-trees.md). **Not adopted:** both tested
cloud-face implementations were effectively tied with the accepted renderer.
The experiment is preserved on `codex/cloud-quad-intersections`; the normal
renderer remains the previously accepted `e324e84` implementation.

## Scope

Store each eligible native cloud quad with its four original vertices, alongside
unchanged actor triangles. The actor/cloud forest and its conservative cache stay
unchanged. Cloud leaves hold four faces instead of eight triangles. The mixed
moving texture has an explicit cloud base address; no extra sampler is needed.

The first prototype uses the first triangle's plane coordinates for the full rectangle. In its basis,
the face is `0 <= v <= 1`, `0 <= u+v <= 1`. For `u >= 0`, use native corners
`(0,1,2)` and weights `(1-u-v,u,v)`; otherwise use `(2,3,0)` with weights
`(u+v,-u,1-v)`. This retains the native diagonal, texture/colour interpolation and
interpolation of native **vertex** fog distances. It avoids the second triangle
intersection. The two-sided/culling rule, determinant threshold, cutout, nearest
cloud depth and material composition remain in place. Floating-point evaluation
can differ, so equivalence requires image checks rather than algebra alone.

The follow-up precomputes the plane axis, signed area and inverse edge lengths in
four unused cloud lightmap-coordinate floats. Positions, native texture coordinates,
colours, alpha and material flags remain exact. It intersects that axis-aligned
plane directly, checks the two edge coordinates and selects the same native
diagonal for shading. This replaces per-entry vector cross/dot products with a
plane solve; images and timings still decide whether it is worth retaining.

Only finite, nondegenerate axis-aligned rectangles qualify. If the native cloud
capture supplies any unsupported shape, the moving scene and shader use the
existing triangle path. No cloud geometry is silently dropped. Raw captured
triangle pairs are retained separately from sorted upload data, so comparison
switches do not recapture animations or attempt to pair sorted triangles.

F9 `;` selects native cloud faces/original cloud triangles, keeping separate roots
enabled. `Shift+;` saves a same-frame pair. F9 `W` still controls combined/separate
moving trees. Diagnostic counters label moving/cloud **primitive** entries: with
cloud faces enabled, each cloud entry is a quad, otherwise it is a triangle.
Counts include entries rejected before complete intersection and are not timings.

## Results

The first quad-plane version passed all 61 JUnit tests, both renderers' 52,480
optical comparisons, 84 material checks and 120 new cloud checks. Wall/down images
match exactly. Wall GPU medians: quads14.639/14.674ms, triangles14.642/14.668ms;
down: quads24.471/24.409ms, triangles24.493/24.473ms. This is effectively a tie,
despite fewer cloud primitive entries, and does not justify promotion. These are
same-capture measurements (77 entities, two block entities,12,288 moving triangles,
6,260,834 terrain triangles;1440p output,1280x720 logical,2xAA,render distance12).

The new cloud test initially exposed a miss on its unrelated large background
triangle diagonal. Shifting that background edge while keeping the cloud ray
exactly on its diagonal made both paths pass (maximum channel error2.98e-8).
The cloud test was not weakened or given a looser tolerance. Shared-edge float
rounding in general triangle intersections remains a separate accuracy limitation.

The specialized-plane version adds an oriented-basis metadata test and checks all
four face edges on the GPU (192 cloud cases). Its build and 62 JUnit tests pass.
Both baseline and candidate pass 52,480 optical comparisons (zero mismatches,
inconclusive or unresolved cases), 84 material checks and 192 cloud cases; maximum
cloud channel error is 2.98e-8. Actor records stay identical across layouts.

Repeated specialized-plane GPU medians, milliseconds, in Q–T–T–Q order:

| View | Triangle baseline, two runs | Specialized plane, two runs | Mean difference |
| --- | --- | --- | --- |
| Wall | 14.603 / 14.537 | 14.503 / 14.525 | 0.38% less time |
| Down | 24.426 / 24.437 | 24.434 / 24.475 | 0.09% more time |

These differences are below a useful, repeatable gain. Each run has 120 warmup
frames and 300 samples. The specialized capture contains 84 entities plus two
block entities, 13,176 moving triangles and 6,260,834 terrain triangles, at the
same pose/resolution/quality as above. Compare within each capture; the changing
mob population prevents a causal comparison between the two experiments.

Specialized-plane wall and restored-demo image pairs match exactly. The downward
pair has RGB MAE 0.00000018 and 0.0002% of pixels changing by more than 8/255;
it is not pixel-identical. The rounded console summary hid this tiny difference;
use the committed full-precision metrics. The contact sheet was visually inspected.
The original cloud-diagonal
fixture issue is documented above; this does not certify arbitrary edge rays.
Both counter programs retain the existing exhausted subpixel at sample1,
x509/y554 (bottom origin), in probe/full; optical limits are unchanged.

In the specialized downward probe, cloud primitive entries drop from 61.07M
triangles to 24.41M faces (60.0%), but moving node visits only drop from 63.46M to
62.94M (0.8%). Actor entries are unchanged at 84,999. Many primitive entries
are cheap rejects; these counts neither measure complete intersections nor
prove a hardware arithmetic/bandwidth bottleneck. A lower count did not produce
better time. Keep the existing cloud renderer and prioritize conservative node
storage/traversal experiments over more cloud intersection arithmetic.

No live FPS gain is claimed or additional live timing run needed for this rejected
candidate. All experimental source and controls described above stay on the
experiment branch; they add no shader registration or runtime work to the normal
checkout. Steps/deferred features and accepted quality settings remain unchanged.

The results checkout `codex/cloud-quad-results` clean-builds/packages the accepted
renderer with all 59 tests passing. An ordinary launch verifies N64/F10 readiness
with profiling/experimental shaders disabled. Exact original demo pose, rotation,
walking mode and 854x480 window are restored; the client is left paused. No blocks,
time/weather settings or demo return record were changed.

Evidence: [profiles/2026-09-23-cloud-quads](profiles/2026-09-23-cloud-quads).
