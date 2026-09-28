# RTX for extended masses and horizon views

The live RTX backend now renders all three existing optical models: exterior black
holes, finite extended masses and near/inside-horizon views. Alt+F12 selects RTX or
OpenGL for each. Ordinary builds retain no Vulkan requirement.

## Implementation

`WorldRenderBackend.Optics` is a Vulkan-free selector. The live Vulkan backend
precompiles probe/material pipelines for all three models during initialization.
They share the same descriptor layout, images, geometry and acceleration structures.
Moving through the horizon or editing a source selects pipelines without rebuilding
the captured scene. Window resizing still recreates the backend as before.

The source loader expands the extended-metric include. `FullImageShader` sets the
corresponding production GLSL definition; the equations, AA and material handling
are reused. The active GL material program supplies the matching uniforms (including
BodyRadius). Eligibility no longer excludes the extended or horizon model. The old
frozen-only `-PinterstellarRtxImage` experiment retains its original exterior scope;
the current `-PinterstellarRtx` supports the new variants in F10 and manual F9 checks.

The comparison revealed a pre-existing selection-outline problem: captured ribbons
lay exactly on block faces, producing renderer-dependent nearest-hit ties. A 0.002
block offset towards the camera implements a small depth bias for both backends.
The checked interior editing view then agrees exactly; its previous serrated/dotted
outline is gone. No changes to the optical equations were needed.

## Image checks

Same-frame, same-scene pairs with simulation frozen, sharp 2× AA and 0.5 logical
scale. A visual check accompanied the metrics. Tiny platform differences remain;
small mean errors alone are not treated as proof of identical imagery.

| Case | Output | RGB MAE, 0–255 | Pixels with a channel error >16 |
| --- | --- | ---: | ---: |
| 8-block extended mass | 2560×1440 | 0.000784 | 56 |
| 14-block extended mass | 2560×1440 | 0.000605 | 25 |
| Near horizon, sideways, r/rs=1.155 | 2560×1440 | 0.001750 | 49 |
| Inside horizon, sideways, r/rs=0.866 | 2560×1440 | 0.000622 | 34 |
| Interior editing/selection, facing blocks | 1280×720 | 0 | 0 |

Additional exterior, near-horizon inward-facing and centre-cutoff cases were checked
at 720p. The last two are all-black image cases and only establish finite/stable
cutoff behaviour; the informative sideways cases exercise the actual optical views.
Source edits switched EXTERIOR→EXTENDED→HORIZON while retaining resident geometry.
Both GL and RTX rendering participated in each comparison. The singularity remains
the existing bounded background with editable matter, not a physical continuation.

## Performance with simulation running

RTX 5070 Ti, Ryzen 5800X3D, NVIDIA driver616.92. Existing120 FPS cap retained.
Output2560×1440, logical1280×720, sharp2× AA. Each run has120 warmup frames and300
samples; table shows the median of run medians. Actors and normal simulation run.

| Case | Runs | Whole-frame interval | Vulkan update/render GPU |
| --- | ---: | ---: | ---: |
| 14-block extended mass | 3 | 8.321 ms | 3.029 ms |
| Near horizon, sideways | 3 | 8.328 ms | 7.215 ms |
| Inside horizon, sideways | 2 | 8.374 ms | 3.137 ms |

Vulkan timing excludes GL appearance copies/resolve; frame intervals include the
client, cap and synchronization. These views remain near120 FPS, not an uncapped
performance estimate or a guarantee for every scene. Frozen-simulation timings are
archived separately by label. No broad movement/flicker suite was run.

Both the optional build and clean normal build pass all87 tests (including six new
wormhole reference tests for the next goal). Runtime initialization compiles all
six RTX pipelines. The normal jar excludes optional RTX classes. Ordinary GL optical
shaders are unchanged; the selection bias is the only shared appearance change.

Evidence and reproduction:
[paired metrics/contact sheets and timing logs](profiles/2026-09-28-rtx-variants),
`py -3 tools/analyze-variant-checks.py docs/profiles/2026-09-28-rtx-variants`.
Full raw pairs remain under `run/rtx-image`, with exact coordinates in comparison.txt.

All tests used the new `Interstellar Overnight Check 2026-09-28` save copy. Original
Calibration and prior Visual Check saves were preserved. Client closed normally and
original options restored before continuing implementation.

## Regression after wormhole integration

The final wormhole build passes 88 tests in both configurations. The shared BH fog
expression was factored into a helper without changing its arithmetic; wormhole
branches compile out of the existing models. Fresh 1440p same-scene GL/RTX checks
give RGB MAE 0.000670/255 (8-block extended), 0.002131/255 (near-horizon sideways)
and 0.000573/255 (inside-horizon sideways), with 27/106/40 pixels above 16 levels.
The near-horizon contact sheet was inspected. Source/model changes retain resident
geometry and the inspected horizon checks use `native-horizon-quads`.

An initial pair of checks had the wrong source selected and exercised exterior
optics; those are excluded from the horizon results. The corrected tests explicitly
inspected the central source first. Temporary blocks in the isolated copy were
restored afterwards. Evidence is in
[the final regression folders](profiles/2026-09-28-wormhole-optics).
