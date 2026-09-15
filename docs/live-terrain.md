# Live terrain preview

Inspect a complete black-hole proxy with `/interstellar inspect x y z`, stand outside 1.05 r_s and within the captured region, then press **F10**. Move and look normally. F10 switches back to Minecraft; **F12** starts/cancels pass timing. **F9** retains the frozen comparison/geometry lab; F8 retains the sky lab. Opening either stops the live preview. `run/config/interstellar-terrain.json` controls enabled/renderScale for both terrain modes, loaded on opening.

The normal HUD and crosshair remain visible. **Interactions use vanilla straight aim**, not curved light paths: switch the preview off when selecting/building blocks. The camera uses a fixed 70-degree vertical FOV and a static local observer at its current position; movement is not yet relativistic motion. Native entity/hand/world images are covered by the terrain pass. Only supported opaque cubes are represented; no returning player-body images yet.

## Refresh and limits

The source-centred 96^3 capture is unchanged from [the frozen prototype](terrain-prototype.md). A second snapshot starts one second after publication, with at most 8192 block queries per rendered frame and a soft three-millisecond capture budget checked every 64 queries. Upload is additional work, not covered by that soft budget. The old complete snapshot remains visible until replacement is ready. Native buffers/textures are explicitly freed; at most two captures are owned. This polls the full bounded region, not changed blocks. Capture spans multiple world ticks and is not an atomic historical state. No retarded-time light transport is simulated.

The HUD shows time since publication, not the age of every block observation. In the test scene capture takes about 0.90 seconds at roughly 120 FPS: publication is about 1.9 seconds apart. An edit can wait longer if its cell was already sampled. Lower frame rates lengthen capture. Refresh pauses while the integrated game is paused.

Source/world invalidation, texture reload, leaving the captured region and approaching r/r_s<1.05 stop the preview. Reinspect if selection was cleared, then reopen. Amber marks missing scene data; pink marks unsupported blocks or exhausted tracing. Snow layers, fluids and non-cube models are unsupported and can occlude surfaces as diagnostic cubes. This is visible in the current saved snowy scene; it is not a physical pink emission effect. No terrain was removed to hide it.

## Verification — 2026-09-15

Build passed; all 31 existing tests pass (final build reused their unchanged passing results). Autonomous runtime checks verified F10 activation, normal strafing, native hotbar/crosshair/chat, repeated completed captures, fullscreen/windowed resize, temporary block appearance/removal, exterior-limit fallback, resource-reload invalidation and capture-boundary fallback. Temporary emerald at (16,304,-8) was placed only into air and removed with a material-specific replacement; scene count returned from 6961 to 6960 opaque cells. Owner edits were preserved.

F9 final-build regression: 43x29 sampled rays, zero flat-hit mismatches, 610 flat hits, 587 lensed opaque hits, zero off-screen-with-margin hits, zero lensed unresolved samples. Scene now contains 2400 unsupported cells after snowfall, plus 6960 opaque cells. This is flat traversal regression evidence, not independent curved-surface validation. Resource-reload fallback was observed in the live build before only message/cleanup edits. Disconnect/dimension transfer, source invalidation during live rendering, F1 hidden-HUD behavior, dense-world performance and long-duration native-memory stability were not separately certified in this iteration.

## Measured performance

RTX 5070 Ti, driver 616.92, 2560x1440 output, 1280x720 internal, standard path sampling, r/r_s=4.51426, 6960 opaque cells, zero unknown/unsupported at measurement time. Live camera stationary, periodic recapture active. 120 warmup frames and 300 samples:

| Measurement | p50 | p95 | p99 |
|---|---:|---:|---:|
| Terrain GPU pass | 3.654176 ms | 3.755040 ms | 3.817920 ms |
| Sampled frame interval | 8.3408 ms | 9.2535 ms | 9.5172 ms |

The GPU measurement excludes native world rendering, capture/upload, upscale and HUD. Frame intervals include cap/vsync and surrounding work, but are a short scene-specific sample, not a general 60 FPS guarantee. No new optical equations or accuracy claims accompany this integration.

Snow limitation update: vanilla snow layers are now supported; see [snow-layer verification](snow-layers.md). The unsupported-snow observations above describe the earlier live-camera checkpoint.

Current observer-range and recovery behavior supersedes the original stopping limits above: see [stable exploration](stable-exploration.md). F10 pauses to normal view and automatically resumes between 1.05 r_s and 128 blocks; the scene itself remains bounded to 96^3.
