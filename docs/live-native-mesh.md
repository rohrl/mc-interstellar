# Live native terrain and moving mobs — 2026-09-18

F10 now uses the accepted native terrain representation. After initial capture, the camera and Minecraft simulation remain live. Supported living mobs move and animate; clouds update too. F12 measures the optical pass. F9 M remains the frozen appearance-comparison path.

Owner priorities: initial loading is acceptable, teleport support is excluded from v1, and rain/snow is deferred. This checkpoint deliberately retains terrain captured at activation. **Block edits and chunk streaming are not implemented yet.** The live HUD states this limitation. Toggling F10 off releases the scene; enabling it again currently repeats initial loading. Keeping a reusable terrain cache across toggles belongs with incremental scene ownership.

## Scene separation

- The large terrain BVH contains only block-model triangles. No mob or cloud copies remain baked into it.
- A separate, small BVH captures native living-entity bodies/equipment and cloud geometry each rendered frame. It uses the current native animation interpolation and lighting. While the game is paused, retain the last moving scene.
- Both trees participate in the same chord traversal with one nearest opaque hit and one nearest cloud candidate. Terrain can occlude mobs and mobs can occlude terrain; clouds blend only when in front of that hit. This is curved-ray geometry, not a straight-camera overlay.
- Entity texture tiles persist across frames. Read back/upload textures only on first use or when a new tile appears; regenerate geometry independently. Terrain and moving geometry share the existing twelve sampler slots.
- Moving triangles are capped at200,000. GPU buffers are replaced atomically and old textures released; temporary native buffers are scoped. The inherited entity atlas is2048² and remains bounded; resource reload requires a new scene. This is not yet a streaming or animated-texture cache.
- Camera access still uses the existing128-block/source-exterior guards and automatic pause/resume. Initial native capture begins after the camera is within the supported region. Source refresh still recreates the renderer when usable source metadata changes.

## Verification

live-mesh-build.log and live-mesh-final-build.log: successful builds;47 tests, zero failures/errors. Final change after runtime checks only reduces repetitive geometry-fingerprint logging and adjusts indentation. No optical equations changed.

live-mesh-runtime.log: native shader startup, F10 initial capture and sustained live updates passed. Camera began at player `(16.5,292,-14.5)`, yaw0.281/pitch15, render distance12. Static capture:6,184,960 triangles,2,880 missing sections,144,393 omitted fluid/translucent incidences,37.7277s. First moving update:12,960 triangles,96 supported mobs,2,688 cloud triangles,9 entity textures. Thousands of subsequent updates changed geometry fingerprints and entity counts without rebuilding terrain. Native special layers omitted: entity_shadow, entity_translucent_cull and eyes.

Inspected run/live-mesh-a.png and live-mesh-b.png, taken six seconds apart at a fixed camera. Visible mobs change position/pose while the lensed wall and foreground pillar remain coherent; no frozen mob copies are visible. This is a live-animation check, not a new zero-bending appearance comparison or independent curved-ray certificate. Ordinary strafe input changes the player position to `(10.46095,292,-14.50311)` without starting another terrain capture. F10 off/on exercises cleanup and fresh activation. Automated movement/flicker acceptance remains deferred to the owner.

Timing: RTX5070Ti,427×240 internal/854×480 window, standard paths, lensing and all live mesh features enabled, r/r_s3.95428,120 warmup/300 samples. Optical GPU p50/p95/p99=9.049088/9.431040/9.453408ms; sampled frame intervals10.6549/11.3300/11.5644ms. Optical GPU excludes CPU actor capture/tree build and upload, native world rendering, sky capture and upscale; frame intervals include work between samples. Different camera from the mountain coverage test: these numbers do not establish a speedup or target-resolution FPS.

## Next milestone

Replace whole-scene terrain capture with reusable chunk/section geometry, bounded edit invalidation and ordinary-movement streaming. Initial capture is not atomic while Minecraft runs, so mid-capture changes can currently leave inconsistent terrain until reactivation. Existing fog/lighting values update, but baked terrain light/AO and geometry do not. Coverage can become incomplete as the camera moves away from its initial footprint. Handle these before calling this a complete live-world renderer.

Rain/snow, fluids, translucent/glowing/shadow entity layers, non-living entities and block entities remain incomplete. First-person player-body returning-light effects remain a later milestone. AA stays on its separate branch.
