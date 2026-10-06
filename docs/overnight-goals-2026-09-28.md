# Overnight implementation — 28 September 2026

> Historical implementation report or proposal. Results, defaults and pending work describe the recorded checkpoint, not necessarily the current mod. See the [current guides and status](README.md).

Owner-authorized scope: finish RTX for small masses and near/inside horizons, then
implement a physics-based, playable, bidirectional spherical wormhole demo linking
two distant locations. Both implementations now have bounded runtime acceptance
evidence in the linked reports; the original requirements below are retained.
Keep the optional Vulkan dependency boundary and the OpenGL renderer.

## Required evidence

- RTX extended and horizon variants use the existing optical equations, with paired
  images at relevant radii/source sizes, resolution/performance checks, and working
  interior editing. Changing optical model must retain resident scene geometry.
- A specified traversable metric, independent reference checks, local/remote terrain
  parallax and occlusion, and an explicit mapping into Minecraft coordinates.
- Two mouths in a separate demo, actual player passage both ways, correct camera
  direction/handedness across the transition, remote data ready before crossing.
- Matching hardware/software images and measured playable frame rates for the
  wormhole exterior and transit. Exact pixels are not assumed near critical rays.
- No modifications to existing demo worlds during tests; bounded server work and
  bounded remote loading. Build both optional and ordinary configurations.

## Current progress

RTX optics selection and precompiled pipelines are implemented and running.
Small masses and horizon views retain the loaded geometry when switching model.
Paired-image and timing evidence is in `profiles/2026-09-28-rtx-variants`.
An existing coplanar selection-outline tie differed between GL and RTX. Applying
the native-style depth bias to the captured outline fixes both paths; the checked
interior editing image then matches exactly. Both builds/87 tests and the normal
jar boundary pass. Goal1's report is `rtx-optical-variants-2026-09-28.md`.

The CPU Ellis reference, two distant native scenes, bounded remote chunk/light
delivery, shared GL/RTX optics and server-authoritative crossing are implemented.
Runtime checks cover native lighting, edits at the unvisited end, region lifecycle,
rendered centreline/oblique crossings both ways, chart-invariant views and playable
1440p frame times. Seven maths tests and 2,575 sampled GPU reference rays pass.
See `wormhole-demo-implementation.md` for the evidence, controls and v1 limits.

## Wormhole model and coordinate choice

Use the symmetric ultrastatic Ellis metric with throat areal radius `a`:
`ds² = -dt² + dl² + (l²+a²)dΩ²`. This is a hypothetical traversable geometry,
not a claim that ordinary mass blocks can manufacture a stable wormhole.

Use isotropic charts for the two Minecraft exteriors. Their radial coordinate `R`
satisfies `l = R - a²/(4R)` and spatial conformal factor `1 + a²/(4R²)`.
The throat sphere has **coordinate radius a/2**, while its physical areal radius is
`a`. This avoids the degenerate areal-radius coordinate at the throat and makes
the local camera directions Euclidean orthonormal directions up to a common scale.

The chart transition inverts radius and reflects one angular axis:
`offsetB = reflectZ(offsetA) * a²/(4|offsetA|²)` (plus the mouth-centre translation).
Its differential transports velocity and camera axes. Inversion and the fixed
reflection together preserve spatial handedness, so passage does not mirror the
player. Applying the mapping twice restores position and vectors. This is a chart
identification for the chosen local wormhole geometry; arbitrarily placing two
mouths in one Minecraft world is not a global solution of the Einstein equations.

The reference null solver evolves signed proper distance, radial momentum and plane
angle. Tests cover the invariant, exact radial passage, the throat's circular ray,
reversal, end symmetry, reflected/transmitted elliptic-integral limits, and the
coordinate-map metric and differential. Player controls prescribe travel; this is
not a simulation of a freely falling body or of exotic supporting matter.

Primary sources: [James et al., metric and camera/ray equations](https://arxiv.org/html/1502.03809),
[Nakajima & Asada, exact Ellis deflection](https://arxiv.org/html/1204.3710).
The coordinate transformation and transfer differential above are derived from
that metric; they are implementation choices, not copied rendering code.

## Accepted architecture (implemented and checked)

- Implemented: a new void demo dimension with two distinct built environments,
  1145 blocks apart. Keep the original demos untouched and retain the saved return
  location/game-mode convention. Creative flight is the initial traversal control.
- Implemented: maintain a bounded region around each mouth. Vanilla does not deliver chunks that
  far from the player; send normal chunk/light packets for those regions and retain
  them in a small client cache keyed by chunk position. Scope this to the new demo.
  ClientChunkManager's normal ring buffer cannot hold both regions safely.
- Implemented: a client chunk-manager mixin captures loads/getChunk for these designated chunks.
  Prevent vanilla unload packets from clearing their cached lighting while retained.
  Notify StreamingTerrain when received/changed. Release remote state on dimension
  exit. Server work/load tickets and packet delivery must be bounded.
- StreamingTerrain retains the union of both mouth regions plus the local view;
  a single geometry arena and RTX scene then serve both ends. Ordinary BH behaviour
  stays unchanged. Native light/material data must come from the destination chunks.
- Trace Ellis rays in signed proper radius, radial momentum and plane angle. Split
  chords at l=0 so no segment spans the long Minecraft distance between mouths.
  Map each endpoint to its corresponding isotropic exterior, using the fixed
  reflection on the opposite end. Preserve finite-distance parallax/occlusion.
- Fog must use local/path distance, not the Minecraft separation of the mouths.
  Sky directions and any cloud geometry must match the active end's frame. Avoid
  the old background-paste error. Do not silently substitute a remote screenshot.
- Use the same inversion differential for player velocity, look direction and up.
  Off-axis passages can rotate the camera's up axis; yaw/pitch alone cannot represent
  that roll. Preserve it explicitly. Map the eye position and ensure client camera
  interpolation agrees immediately before/after the authoritative teleport.
- Shader/model selection has a wormhole variant, supplied to both GL and RTX.
  Keep the radius convention explicit: isotropic mouth radius is half the metric's
  throat areal radius. Packet-driven transfer must be reversible and avoid bounce.

Helpful local source extracts: `run/mc-source/ClientChunkManager.java`,
`ClientPlayNetworkHandler.java`, `ChunkDataS2CPacket.java`, `WorldChunk.java`,
`ServerChunkLoadingManager.java`. Extracted from installed Minecraft sources;
they are reference files, not project modifications. Existing helpers are
StreamingTerrain, WorldMesh, EntityMesh, NativeSky and DemoCommands.

## Working environment

Branch `codex/rtx-wormhole-demo`, based on `8c637da`. No subagents. Prior AA work
remains separate. Runtime save: `Interstellar Overnight Check 2026-09-28`, a fresh
copy of `Interstellar Calibration`; existing saves are preserved. The internal
world display name still says Calibration, so verify the quick-play argument.
Options backup: `run/overnight-owner-options.txt`; launch init: `run/overnight-init.gradle`.
Logs: `run/rtx-variants-runtime.log`, `run/rtx-variants-runtime2.log`.
