# Continuous exterior and bounded repeated views — 29 September 2026

> Historical implementation report or proposal. Results, defaults and pending work describe the recorded checkpoint, not necessarily the current mod. See the [current guides and status](../../README.md).

## What changed

The old GPU triangle filter assigned terrain to opposite sides of the plane halfway
between the mouths. Opening the pair therefore removed half the world and rendered
only the nearest entrance's optical chart. Removing only that filter would still
leave a solver for one mouth. The replacement traces through two disjoint optical
regions in the same native scene, with straight rays between them.

Near-throat paths match Ellis spatial rays; their influence smoothly ends farther
away. This is an engineered optical metric, not the exact isolated Ellis solution.
The existing player crossing map remains compatible. See [the scientific model](../../science.md#two-mouths-in-a-continuous-minecraft-exterior-2026-09-29).

At most four throat passages and eight pi of orbital angle are traced per ray.
Circulating rays reaching those limits return dark. There are no recursive
full-frame portal renders. A separate 2048-iteration safety limit stays magenta
to make a solver failure visible.

## Checks

- Normal and RTX builds pass **101 CPU tests**. The ordinary jar has zero optional
  Vulkan/shaderc/backend entries. Logs: `run/wormhole-continuous-final-*-build.log`
  and `run/wormhole-continuous-artifacts.txt`.
- Six new CPU tests cover the profile, absence of extra circular photon orbits,
  native exterior, both entrances, mouth-order invariance, the passage limit and
  reference convergence.
- **37 GPU reference rays, zero mismatches**, maximum unit-direction error
  0.000026155 (about 0.0015 degrees). Fine Cartesian RK4/bisection on the CPU is
  independent of the polar RK4/Newton GPU integration. Includes grazing boundaries,
  cameras on both sides of the old plane, either target mouth, an inside camera,
  source-order swaps and a deliberately repeating ray. This checks selected rays
  at the conservative path setting; it does not certify every view or a physically
  realizable wormhole. [Log](gpu-reference.txt).
- Actual copied Overworld: both entrances and the intervening landscape remain
  visible with either mouth nearest. [Nearest A](two-mouths-nearest-a.png) and
  [nearest B](two-mouths-nearest-b.png). These two images precede the final fog
  adjustment described below.
- Same-frame GL/RTX RGB mean absolute error: **0.000897 / 255** near A and
  **0.001181 / 255** near B. Reports include camera and quality settings. Scene
  capture was still active during these paired snapshots; each pair uses the same
  captured frame and is valid for renderer agreement, not settled timing.

Early checks caught a Vulkan-reserved identifier, a grazing-boundary Newton root
selecting the entry instead of exit, and a diagnostic status overwritten by the
shared black-colour helper. All were fixed before the passing reference run.
Final review also replaced a fog-formula switch with accumulated native fog
distance along chords, preserving straight-ray vanilla fog continuously at the
optical boundary.

Final fog-adjusted build: [acceptance image](final-fog.png),
[same-frame GL/RTX report](final-fog-comparison.txt). RGB MAE **0.008638 / 255**;
131 of 921,600 pixels differed by more than16 in a channel. Both backends compiled
and rendered; no runtime errors in the final log. The optical integrator did not
change after the37-ray pass.

[Final view of both mouths](final-both-mouths.png), after the replacement and fog
fix. The QA client is paused at this viewpoint.

## Opening delay

The old completion condition included the entire moving camera capture window.
Exploration could extend the queue after progress had already reached 97%.
Opening now measures and prioritizes the fixed destination regions. Other terrain
can continue streaming after the passage opens; travel still waits for native
chunk/light readiness, captured destinations and a presented passage frame.

Cold-copy run: layout at 12:42:31, native regions ready at 12:42:38, visually open
at 12:42:55: **24 seconds**. All native camera terrain took longer to finish.
An actual downward pearl throw replaced the oldest end at 12:47:33. I moved the
camera while preparing; native regions and the passage were ready at 12:47:38:
**5 seconds with a warm overlapping cache**. This is not a universal loading-time
promise or a controlled before/after speedup measurement. Shader compilation and
backend initialization can still make cold setup much slower than a replacement.

## Timing and limits

RTX 5070 Ti, Ryzen 5800X3D, 1280×720, full scale, fine paths, 2× AA, clouds/weather
enabled, body images disabled. Before the final fog adjustment, the settled
replacement view had 5,807,818 triangles and **zero queued chunks**: GPU median
10.615 ms, frame interval median 15.357 ms (about **65 FPS**), 120 warmup and 300
measured frames. Earlier captures with 186 or 13 queued chunks are excluded from
settled results. This is one pose, not a general performance guarantee or a matched
regression comparison. GPU timestamps exclude GL appearance copies and resolve;
frame intervals include the frame cap.

Final fog-adjusted build, same camera/quality but freshly captured live terrain:
**5,696,102 triangles, zero queued chunks; GPU p50 11.546 ms, frame p50 15.026 ms
(about67 FPS), frame p95 18.54 ms**. Geometry/cloud state changed across the restart,
so the earlier measurement is not a controlled fog-performance comparison. Final
runtime log: `run/wormhole-continuous-final-acceptance.log`.

Remote terrain remains a five-chunk radius around each mouth when it lies outside
the ordinary camera window. The model does not provide unlimited remote terrain,
remote mob tracking, cross-dimension travel or mob/projectile transit. Very small
higher-order images can reach the dark passage limit. Broad movement/flicker checks
remain owner-deferred.

## Save preservation

The owner's latest `Interstellar Opening QA 2026-09-29` was saved and copied to
`Interstellar Continuous QA 2026-09-29`. Only the copy received camera commands,
one test landing block and the replacement throw. The original revision-26 pair
was preserved. Quality preferences still match the owner-options backup.
