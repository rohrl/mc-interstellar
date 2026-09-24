# Horizon access and returning-body study — 2026-09-24

Branch `codex/horizon-body-study`, following `982d563`.

## Close viewing and editing

F10 no longer pauses below 1.05 Schwarzschild radii. That limit protected the old
static-observer initialization, which contains `sqrt(1-r_s/r)` and cannot describe
a hovering observer at or inside the horizon. Removing only the guard would create
invalid rays. F9 also permits the new close view with native streamed geometry.

Outside 1.25 r_s retain the static frame. Between 1.25 and 1.05 r_s smoothly change
the optical camera frame to the existing radial free-fall frame; below 1.05 use it
fully. Minecraft movement is unchanged. This is a chosen presentation frame, not
a measurement of the player's velocity or the deferred observer-speed feature.

In units r_s=c=1, let `a=1/sqrt(r)` and `w=a*smoothstep(1.05,1.25,r)`. Convert the
camera's radial cosine to the PG falling frame with `mu_f=(mu-w)/(1-w*mu)`.
Initialize the spatial null orbit with `u'= -u*(mu_f+a)/sin_f`; retain the existing
`u''=1.5*u*u-u`. The initialization agrees with the static frame at the outer
endpoint and is regular across r=1. Background connectivity follows the existing
[free-fall model](free-fall.md), including its one-illuminated-asymptotic-region
boundary condition. This does not model collapse, spectral transport or history.

**Interior editing is an explicit gameplay approximation.** For a camera at or
inside the horizon, first intersect the existing native scene along the straight
aim ray, restricted to the horizon sphere and tagged mass-block surfaces. Mass
blocks and their selection/damage layers appear at their editable Minecraft positions. Omit those surfaces
from the subsequent curved background trace so they are not duplicated. A bounded
32-layer interior pass retains overlay transparency. Clouds and other geometry
stay on the curved path. Camera-body geometry still requires
returning rays and does not enclose the editing camera. Outside observers continue
to see a black horizon rather than the hidden mass blocks.

At r<=0.1 r_s, retain the editing layer and make the remaining background dark.
This explicit central cutoff prevents singular calculations; it is not a physical
model of the singularity. F10 remains armed and compositing at the centre. Changing
source mass can change the horizon immediately; ordinary chunk publication remains
queued. The existing 256-block/source-availability limits still apply.

Dedicated near-horizon shader variants keep the extra observer and editing work
out of the ordinary exterior programs. The interior mask used for Glowing follows
the same view rule. Frozen preview cannot switch to the old voxel backend nearby.

## Returning image experiment

Use the actual player model/skin. Compare the reference64-block cube (`r_s=3.4641`)
with a temporary512-block cube (`r_s=27.7128`): eight times the horizon radius.
Capture2560x1440 at full trace resolution and two-ray AA, above normal demo quality.
Solve the spatial shooting problem for a ray returning to the camera radius after
2 pi radians; align the view and slightly shift the sampling phase. Tested exterior
ratios1.5,1.6,2,3, plus a1.1 close-frame view. Not every ratio was tested at both sizes.

| Full-resolution fixed-world pair | Pixels changing by >8/255 in any channel |
| --- | ---: |
| Large, exact1.5 r_s tangent |0|
| Large,1.6 r_s return direction |249|
| Large,2 r_s return direction |237|
| Small,1.6 r_s |798|
| Small,2 r_s |1129|
| Small,3 r_s |978|
| Small,1.1 r_s close frame |1594|
| Isolated small source above the exhibit,2 r_s |1684|

These are visibility differences, not optical error measurements. Body-off/on
changes concentrate in thin arcs/strips; none of these sampled views yields a
recognisable head or body. The larger source resolves less player detail.

Also test native spyglass zoom and an isolated64-block source at y282 to remove
nearby dispenser occlusion. The isolated zoom resolves a wider brown strip, still
without recognisable features. Zoom snapshots use live ticks to exercise item use;
do not treat their full-image differences as isolated accuracy metrics. In the
large zoom pair, moving clouds also differ away from the body strip. The first
small-source zoom was blocked by exhibit geometry and is excluded as evidence.

Artifacts (ignored local run directory): `body-large-photon-*`, `body-large-16-*`,
`body-large-20-*`, `body-small-16/20/30/11-*`, `body-isolated-20-*`,
`body-isolated-zoom-*`, `body-large-zoom-*`. `ReturnImageCompare.java` produces
nearest-neighbour crops from exact screenshot pixels; it invents no image detail.
Log `horizon-runtime.log` records source selection and poses. Example small2 r_s:
feet `(2,280.38,-4.9282032303)`, yaw66.82867, pitch0, source `(2,282,2)`.

**Decision:** returning-body rendering defaults off. The opt-in session command
`/interstellar-visuals body true` remains for experiments; disabled mode excludes
the camera body from capture and traversal. This follows the owner's visibility
criterion and removes its previously measured roughly2ms cost in the arrow scene.
It is not proof that no possible geometry or future renderer can produce a better
image. Native hands, other players and mobs remain enabled.

## Validation

Build/package and78 unit tests pass, including static/free-fall equivalence and
regular observer initialization through the horizon. Initial GPU run:52,480 optical
and156 material cases pass;405 near-horizon sky rays agree with independent PG-time
integration, maximum direction chord difference0.001669 (about0.096degrees).

That initial extended diagnostic still referenced deleted material-fixture textures before
the sky test and emitted GL errors. Fix all sampler bindings before the subsequent
draw; final checks below supersede that run. No production texture ownership changed.

Actual LMB/RMB inside the512-block source horizon successfully mines and places a
mass block. Logs confirm `HORIZON_MINED`, `HORIZON_PLACED`, and automatic512→511→512
source updates. `horizon-inside-edit.png` shows the native block/selection view.
Further final numerical, central-view, timing and restoration evidence follows.

The first interior visual check also exposed an overly broad editing layer: it
included clouds. Restrict it to mass-block terrain (`-4`) and tagged interaction
surfaces (`+64`, stripped before ordinary material decoding). Other geometry stays
curved, including inside the horizon. Final evidence below uses that correction.

### Final accepted checks

`horizon-verified-runtime.log`:52,480 optical comparisons,156 material cases and945
horizon checks all pass, with no GL errors or renderer exceptions. The945 include
810 inward/outward sky samples at nine radii against independent PG integration,
and135 finite/dark central-boundary samples at0,0.05 and0.1 r_s. Maximum sampled
direction chord difference remains0.001669. These are sampled checks, not an
arbitrary near-critical accuracy bound or radiometric validation.

Repeat LMB/RMB on the mass-only overlay succeeds (`FINAL_HORIZON_MINED/PLACED`).
`horizon-interior-final.png` shows the corrected interior at0.5 r_s. Earlier captures
cover1.001/0.999 and the centre; the final synthetic centre check uses the corrected
shader. The central air pocket follows native low-light shading. No automated
movement/flicker sweep was added, as requested.

1440p output, normal half-scale/two-ray AA,120 warmup/300 sampled frames:

| Live scene, body off | GPU p50 / p95 / p99 ms | Frame p50 / p95 / p99 ms |
| --- | --- | --- |
|512-block source,1.1 r_s, inward |57.568 /69.894 /73.309|56.810 /70.994 /75.205|
| Ordinary64-block arrow demo |18.885 /19.801 /20.192|19.390 /20.993 /21.988|

The ordinary demo is about52FPS; the large near-horizon view is about18FPS and
remains expensive. Do not extrapolate the normal result to a worst-case minimum or
claim a matched before/after speedup: the current demo camera actually reports
yaw-2.1000004/pitch25.250002 rather than the older0/26 pose. The large run began
with queued streaming work. Raw summaries: [profile](profiles/2026-09-24-horizon-body.txt).

Removed exactly512+64 temporary mass blocks; original source64 remains. Fresh
before/after records match all seven player fields: dimension, position, rotation,
abilities, selected slot, complete inventory and health. Restored normal ticking,
arrow firing, original terrain config,854x480 window and F10 off; client paused.
No permanent fixture, inventory replacement or core air-pocket edit remains.
