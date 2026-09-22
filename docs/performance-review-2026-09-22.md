# Performance review after demo and material refinement

## Assessment

Keep the present native-geometry approach. It now preserves Minecraft's model geometry, light, fluids and ordinary translucent layers while bending the same world consistently. Replacing distant scenery with an unbent camera image would reintroduce the duplication that the owner rejected.

Reviewed `WorldMesh`, `MeshTree`, `StreamingTerrain`, `MeshArena`, `EntityMesh`, `TerrainSamples`, and the shared optical/traversal shader. The current algorithm integrates each subpixel ray with RK4, tests straight chords against terrain and moving-object BVHs, reuses certified empty regions, then shades/composites ordered hits. Two separately scheduled rays feed the sharp reconstruction. Chunk geometry is retained; moving geometry and its tree are rebuilt every frame.

The optical GPU pass still occupies most of the measured frame interval. That observation does **not** distinguish arithmetic, memory traffic, register pressure or traversal divergence; stage timings alone cannot prove one of those is the hardware bottleneck. See [material coverage](material-coverage.md) for the matched live results and their limits.

## Review and follow-up

### 1. Shared native quad vertices — implemented

Follow-up completed: four-quad leaves reduce the controlled heavy-view GPU median by4.3%, with a small easier-view tradeoff explicitly accepted by the owner. Original/quad wall, down and close image pairs match exactly; final live medians remain about57–60FPS wall and34–35FPS down. Production retains one quad vertex arena and one compact node arena. [Full experiment, final timings and limitations](quad-vertices.md). The planar-intersection shortcut below remains a separate proposal.

`WorldMesh.quad` and `entityQuad` expand each four-vertex face into two independent triangles; `MeshTree.STRIDE` stores36 floats per triangle. Thus each original face stores72 floats (288bytes), although its four original vertices need48 floats (192bytes).

An implicit triangle-pair primitive could remove exactly one third of that raw vertex payload without rounding positions, lighting, UVs or colours. For the measured6,260,820 terrain triangles, the payload falls from901,558,080 to601,038,720bytes: about286.6MiB less data before row padding and acceleration nodes. These are storage calculations, **not measured VRAM or FPS savings**: the present arenas reserve fixed capacities, so their allocation policy must also change to reclaim memory.

Keep both original triangle tests/interpolation initially, sharing vertex loads. Pair faces before BVH sorting. Then separately test a planar-face intersection shortcut, with the original two-triangle fallback for nonplanar fluid surfaces and unusual models. Do not use bilinear lighting interpolation: it changes Minecraft's diagonal shading. Pair bounds may be less selective than separate triangle bounds, and changed ordering can expose coplanar tie differences; neither a halved tree nor a speedup is guaranteed.

Acceptance: optical/material fixtures, full/selective image pairs, wall/down/close geometry including water and stairs, then matched live timings. This is the preferred next substantive prototype because it attacks work and data duplication while retaining the current image model.

### 2. Reuse precomputed optical trajectories

The inverse-radius orbit equation depends on a small set of scalar parameters, while azimuth changes only the orbital plane. A table could replace repeated RK4 arithmetic in smooth parts of the path. This is supported as a general technique by [Bruneton's 2020 black-hole renderer](https://arxiv.org/abs/2010.08735), which uses precomputed tables for a disc/background-star scene. Its constant-time scene intersection result does not transfer directly to arbitrary Minecraft terrain.

Our proposed adaptation is an inference: interpolate orbit states, continue testing terrain chords against the existing trees, and keep numerical integration for near-critical/turning regions or where an interpolation error estimate fails. It would not paste unbent background pixels. Table coordinates, interpolation error, camera-radius changes and incoming/outgoing branches need independent tests. A table does not remove geometry traversal, so estimate the removable integration cost before undertaking this larger change.

### 3. Reduce geometry bandwidth further, with conservative error bounds

After quads, test chunk-relative compressed BVH bounds or packed appearance attributes. Round bounds outward so quantization cannot exclude real geometry; retain full-precision triangle positions. Quantized colours/light may be acceptable if regional pixel metrics and visual checks show only tiny changes. Wider bounds can increase false-positive traversal, and unpacking adds arithmetic, so smaller storage alone is insufficient evidence.

A smaller option is switching between selective and full transparency composition when most rays need transparency. Existing frozen terrain-heavy measurements put the full path only about0.4ms ahead, while selective wins clearly at the wall. Any automatic selection must use actual asynchronous coverage/timing evidence, avoid GPU readback stalls, and not oscillate with the camera. This is lower priority than quads.

## Avoid repeating weak experiments

- Extra photon-edge AA rays improved the edge metric but cost6–8ms; alternative two-ray diagonals worsened the relevant image metrics. Retain current AA.
- The earlier surface-area BVH experiment saved only a few percent and greatly increased capture/build cost; final-hit shading and facing hints also failed their measured gates. A new representation may justify revisiting them later, but no immediate rerun is indicated.
- Updating actors less frequently would directly undermine the requested live mobs. Separate static block-entity geometry/refit trees only if CPU profiling identifies rebuild cost; do not freeze animation as an easy FPS claim.
- A compute or hardware ray-tracing backend is a larger renderer project. Current curved rays still need segmented scene intersections; changing APIs does not eliminate that requirement.

## Correctness findings in this pass

- Fixed grazing transparent self-hits with a small normal-directed offset;28 GPU material cases include the failing geometry.
- Wider optical steps changed two close-observer surface-cell classifications. Preserve the established settings through4 source radii and blend to the faster settings by6. The policy is computed once per image, and the fixture applies it independently at every tested observer distance.
- A changing offline development username broke access to the per-player saved demo return record after restart. A stable development identity fixes it; restart/return now preserves position, rotation and flight.
- No further confirmed blocker was found in the reviewed code. Unsupported additive/glint/text/particle layers, coplanar overlays, boat water masks, weather and deeper-relativity features remain explicit limits; this review is not general renderer certification.

Step5 remains deferred for discussion. These performance proposals do not require starting it.
