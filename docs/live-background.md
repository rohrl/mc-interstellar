# Live world background — 2026-09-16

F10 now samples the already-rendered Minecraft world for rays that previously displayed the missing-data grid. Horizon capture remains black; captured terrain, unknown cells and unsupported/budget diagnostics retain their existing treatment. F9 deliberately retains its diagnostic grid because its view can rotate independently of the underlying world camera.

The shader reads the main framebuffer colour attachment while writing a separate terrain framebuffer, then presents the completed terrain image. There is no framebuffer feedback loop and no additional world capture or server work. Texture Y is inverted to match the framebuffer convention. The per-render LiveBackground uniform explicitly selects F10 versus F9; diagnostic output remains unchanged.

This is a same-screen visual fallback, not ray continuation through distant geometry. Background terrain, clouds, sky and entities are unbent; uncaptured foreground does not correctly occlude local lensed hits. A captured object can also remain visible in the ordinary background at its original screen location where the lensed ray misses. Boundaries/duplicates are possible. This does not complete full-world lensing, radiometry or entity rendering. The fallback is sampled at the terrain pass resolution (half resolution by default).

## Verification

- Gradle build passed; 45 tests pass. Shader compiled at runtime with no Interstellar rendering errors.
- Existing N=64 source preserved. Near and far F10 images inspected; far camera (16.5,303.62,-85.5), r/r_s=12.68927, shows normal sky/clouds and world surrounding the captured scene. No blocks edited.
- F10 -> F9 -> F10 checked: frozen grid retained and live world background restored. F9 V: 39x26 rays, zero flat mismatches, 60 flat hits, 92 lensed opaque hits, zero unresolved samples. No optical integration change.
- 2560x1440 output / 1280x720 internal, standard path, RTX 5070 Ti, 11579 opaque / zero unknown or unsupported: 120 warmup and 300 measured frames. GPU pass p50/p95/p99 = 5.095424/5.168704/5.211136 ms; frame interval = 8.3261/9.4794/9.7158 ms. Scene-specific, includes cap/vsync in frame intervals; not a before/after comparison or universal FPS claim.
- Logs: background-build.log, background-runtime.log. Images: run/background-live.png, run/background-far.png, run/background-frozen.png (local/ignored).

The client is left in F10 at the far view in an 870x519 window. Demo packaging remains next; this background change took priority at the owner's request.
