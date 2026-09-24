# World feature integration — 2026-09-24

Implementation branch: `codex/world-feature-coverage`, based on `ecf1b09`.

Later [horizon/body study](horizon-body-study.md): returning images now default off
after full-resolution and zoom comparisons failed to produce recognisable detail.
The session toggle remains available. Historical measurements below retain their
original explicit body-on/off settings.

## Design

Extend the native entity/block-entity capture with material modes for unlit alpha,
additive eyes, native squared-colour glint blending, red-channel font intensity and
multiplicative mining overlays. Glint and damage UVs wrap after interpolation inside
the entity atlas. Refresh font textures only when emitted glyph UVs change. Reuse
native nearby shadow quads, selection shapes and current mining stages.

Capture the actual first-person camera body with a material marker. Omit it on primary
ray segments; allow it after unwrapped orbital angle exceeds pi. This avoids the camera
being enclosed by its own opaque head while retaining returning rays. No model proxy,
recorded animation history or delayed-light simulation is introduced.

Glowing status uses a small separate tree containing glowing model surfaces and team
colours. Trace that tree using the same camera/optical equations and apply a screen-space
edge filter. Excluding terrain from the mask deliberately preserves vanilla through-wall
visibility. Empty glowing scenes allocate no mask/tree textures and draw no extra pass.

Weather redraws only a small native local precipitation layer after lensing and before
hands. It is an explicit foreground approximation: native biome/roof rules and straight
scene depth, no curved precipitation paths. No work while clear.

Tridents enter the existing bounded projectile substeps; their outer native loyalty tick
still runs once. Free fishing bobbers receive one bounded kick per server tick and swept
horizon capture after native motion, preserving fishing/reeling/buoyancy. Strings add
quasi-static deflection with pinned endpoints, capped at2 blocks and25% of their length.
No force transfers through the rope. Visual clients receive server gravity settings via
a small payload on join/change; they do not read integrated-server mutable globals.

## Verification

Build/package and77 tests pass, including endpoint/far-field/bounded-sag tests.
Final release runtime:156 material cases, zero failures/max channel error0; optical
fixture:52,480 comparisons, zero mismatches/inconclusive/unresolved. Shader/mixin startup
passes (`features-release-runtime.log`). No rendering exception; native unused-sampler
and optimized-out-uniform warnings remain. Representative timings follow below.

Fixed1440p body on/off pair at player(2,80.38,-14), yaw/pitch0, ticks frozen: the difference
is concentrated on a narrow arc outside the shadow. Actual model/skin is used; the default
image is thin, not a large full-body portrait. Files `run/features-body-on.png`,
`features-body-off.png`, `features-body-diff/`. Whole-image RGB MAE about0.0002 and0.17%
of pixels differing by>8/255. This is a visibility check, not an accuracy error score.

Native runtime trident A/B over8 ticks, same initial position/velocity: without field
final z7.5; with field z7.35568 and velocity z-0.04810. Loyalty3 trident moved toward its
actual player owner over8 ticks under gravity. Actual fishing cast moved laterally;
second use reeled it in and removed the owned bobber. A leashed cow retained native fence
attachment. No controlled bobber accuracy/reference comparison was performed.

Visual checkpoints show emissive spider eyes, native sign/name text, selection/mining
cracks and fishing/leash geometry. Logs: `features-final-runtime.log`,
`features-verified-runtime.log`. The subsequent final checks are below.

## Limits

See [Minecraft coverage](minecraft-coverage.md). These sampled checks do not establish
universal mod/resource-pack compatibility, a worst-case FPS floor or physical accuracy
of the foreground weather and rope approximations. Movement/flicker inspection remains
with the owner per their request.

## Final visual and numerical checks

Native-versus-curved comparison exposed missing shadow depth bias; native raster layering
has no geometric separation, so the captured shadow now gets the same2mm separation as
other coplanar overlays. `run/features-shadow-fixed-glint-on.png` shows the restored blob
shadow. Enchanted/plain chestplate pair differences concentrate on the armor
(`features-glint-diff/`, whole-image RGB MAE0.0002;0.49% of pixels over8/255).

Glowing entity behind the existing exhibit wall produces two lensed silhouettes. A temporary
aqua team verifies team colour (`features-glow-team-final.png`). The mask contains model
surfaces only: name-label backgrounds, shadows and glint do not become glowing silhouettes.
The native straight silhouette is suppressed only when F10 actually composites the world.

Rain and snow are visible in `features-rain.png` and `features-snow.png`. The snow test
replaced18 plains biome cells in a small patch and restored the same18 cells afterwards;
no permanent biome or source edits remain. Weather was returned to clear.

## Measured cost

RTX5070Ti,2560x1440 output, half-scale1280x720 trace, two-ray AA, source64 blocks,
arrow-course camera(2,90,-18), yaw0/pitch26. Each run:120 warmup frames,300 samples.
The fixed fixture runs freeze world ticks; the clean live runs evolve normally.
Lensing GPU time includes resolve and the optional glowing-mask pass. The local weather
pass runs afterwards, so assess it using sampled whole-frame intervals, not lensing time.

| Scene | GPU p50 / p95 / p99 ms | Frame interval p50 / p95 / p99 ms |
| --- | --- | --- |
| Clean live demo, body on |21.001 /21.883 /22.207|21.601 /23.001 /23.456|
| Clean live demo, body off |18.734 /19.656 /19.987|19.338 /20.807 /21.229|
| Fixed fixtures, Glowing on |26.723 /27.424 /27.873|27.464 /28.358 /28.721|
| Same fixtures, Glowing off |23.058 /23.983 /24.481|23.584 /24.658 /25.185|
| Fixed rain, local layer on |22.927 /23.818 /24.133|23.566 /24.514 /25.254|
| Same rain, local layer off |22.861 /23.734 /24.100|23.509 /24.471 /24.814|

The clean demo is approximately46FPS with returning images,52FPS without. The earlier
pre-feature checkpoint recorded18.520ms/54FPS frame median at this pose. That earlier run
is not a fixed-state replay, so it is not an isolated batch-overhead measurement. Returning
images cost about2ms in these samples; Glowing costs about3.7ms while active. Rain's median
difference is0.057ms, within ordinary run noise; do not claim exactly zero cost. Neither
this simple exhibit nor these short runs establish a natural-terrain worst-case FPS floor.

Logs: `features-acceptance-runtime.log`; concise samples: `run/features-timings.txt`.

### Rejected body-cache micro-optimization

Tried allowing body-only leaves to contribute to primary-ray empty-space caches, clearing
those caches when the ray first passes pi. A same-frame cached/uncached pair at
(2,80.38,-14), yaw/pitch0, is byte-identical (`pair-3169815753925780448`). The156 material
cases and52,480 optical comparisons pass. However live medians21.577ms body-on/19.416ms
off leave roughly the same2ms difference as before. The trial was removed; conservative
cache behavior remains. A future profile should separate initial camera-body tree visits
from continued ray work before choosing a more substantial body-tree change. No speedup
is claimed from this trial. Log `features-body-cache-runtime.log`.

Final release additionally verifies an invisible glowing pig after advancing native ticks
(`run/features-invisible-glow-curved-final.png`). The temporary pig was removed. All six
recorded player fields match the fresh pre-task record exactly; inventory/flight/window
restored, normal ticking, F10 off, client paused, narrator0. Temporary blocks/entities/team
and biome patch were removed. Brief survival-test damage was healed; initial health had
not been recorded separately. Final release fixture summaries are included in
[the raw profile summary](profiles/2026-09-24-world-features.txt).
