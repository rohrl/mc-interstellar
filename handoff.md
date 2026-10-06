# Handoff — Documentation refreshed, 6 October 2026

## Current state

Documentation audit complete: README now covers normal Fabric installation,
source/RTX launch, features, controls, troubleshooting and scientific references.
Current guides are indexed in docs/README.md; older experiment reports are clearly
historical. All checked relative links resolve to tracked files/directories.
No build or Minecraft launch during this documentation-only task. Standalone RTX
packaging remains unfinished: the current Gradle launch supplies runtime libraries
and JVM properties that an ordinary copied RTX jar does not supply.

Gas veil follow-up: distance from the disk plane is now 3× the original
threshold (0.6–9 blocks depending on BH size), with doubled opacity (max 36%).
Radial annulus limits and smooth fading remain. Source-only; no new tests/build/launch.

Latest follow-up: star light contribution increased 50% through vanilla's sky
draw, also affecting both lensed backends. The multiplier is sqrt(1.5) because
vanilla multiplies RGB and source alpha together. Source-only; no new launch,
build or tests per owner instruction.

Branch `codex/incremental-refresh`. Accretion disk, visual refinements and
ambience are complete; implementation, assumptions and evidence are in
[docs/accretion-disk.md](docs/accretion-disk.md), with decisions in D105.
Preserve owner worlds, IDE settings and the earlier AA branch/commit `8ad46ebd`.

- Shared OpenGL/RTX disk, new brightness scale (old 200% = 100%; up to 400%),
  animated patches/shimmer, granular hot rims and bounded apparent parallax.
  Apparent depth is shading, not new geometry or a gas simulation.
- Fixed texture wedges (noise corner hashing) and fullscreen glow echoes
  (bounded bloom resolution and contiguous blur taps).
- F4 now has Gameplay / Graphics / Disk / Ambience / Relativity / Tools.
  Ambience controls stars (default 2×), BH music and gentle disk gas opacity.
- Latest change selects basalt-delta, crimson-forest or End music randomly
  once per BH approach, avoiding immediate repetition of the music event.
  Normal Music volume applies. No new music assets or dependencies.

## Verification and limits

Before the final random-music change, both builds passed 120 JVM tests and all
16 RTX shader variants compiled. Logs: `run/disk-study/final-{opengl,rtx}-build.log`.
The normal artifact was checked for absence of Vulkan/shaderc backend classes.
The final small music-selection change is source-reviewed only: the owner
explicitly requested no tests or Minecraft launch while the PC is busy.
Existing accepted jars predate that selection change; next build picks it up.

GPU checks passed 50 observer rays, 54 colour contracts, 15 disk cases and
30 noise seams (maximum numerical error 1.64e-6). Final GL/RTX image comparison
`run/rtx-image/compare-1791001960973` has RGB MAE 0.000269/255.
Windowed/fullscreen rendering, F4 controls, star changes and gas on/off checked.
`run/disk-study/final-smoke-runtime.log` confirms repeated music start/stop and
clean client shutdown. Do not launch or control Minecraft until allowed again.

Matched 1280×720, 4× AA RTX view: disk-visible frame median 25.06→25.68 ms
(~2.5% cost), disk-off 33.40→33.50 ms. No more benchmarks needed for music.
Final screenshots: `run/disk-study/final-fullscreen.png`, `final-stars.png`,
`ambience-menu.png`, `gas-on.png`, `gas-off.png`.

An earlier transient RTX black frame was not root-caused; later clean launches,
fullscreen and image comparisons passed without debugging readbacks. Do not
count black-output timings. VulkanWorldBackend has no remaining debug edits.
Very high-resolution AA can still exhaust VRAM and trigger OpenGL fallback.
Automated movement/flicker checks remain deferred to owner feedback.

## Owner settings and QA

Only copied `Interstellar Disk QA` was used. Do not restore old backup settings
over the owner's newer choices. Last restored preferences: disk Auto, animation
on, brightness 200%, outer radius 6, tilt 0, Intense glow; 4× AA, full scale,
fine paths and RTX preferred. New ambience defaults: stars 2×, music/gas on.

No further feature work is pending for this batch. Final random-music runtime
confirmation can wait until the owner next plays; it was intentionally skipped.
