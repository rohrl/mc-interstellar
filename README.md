# Interstellar

A Minecraft Java mod for educational relativistic optics. Scientific assumptions and numerical limits must be visible and testable.

**Current iteration:** Fabric diagnostics and an F8 optical lab with Schwarzschild sky lensing and a guided free-fall horizon crossing. Mass blocks now support bounded cluster inspection. Completed inspections can supply the optical lab with source scale and starting distance. F9 now previews lensing of a bounded, frozen region of textured opaque Minecraft blocks. F10 uses native terrain with a live camera, animated mobs and incremental chunk updates. Full scene coverage and the potion remain unfinished. See [progress.md](progress.md) for verified results rather than assuming planned features exist.

## Start here

- [decision-log.md](decision-log.md): important decisions, rationale, and superseded proposals.
- [plan.md](plan.md): scope, milestones, and acceptance criteria.
- [progress.md](progress.md): current implementation and verification status.
- [handoff.md](handoff.md): resuming with another developer or AI agent.
- [docs/science.md](docs/science.md): equations, reference conventions, sources, and approximations.
- [docs/development.md](docs/development.md): Windows setup and commands.

## Development

Minecraft 1.21.1, Fabric, Java 21. Open this directory as a Gradle project in IntelliJ and select a complete JDK 21 for both Project SDK and Gradle JVM.

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

Use a disposable development world. F6 toggles the diagnostic HUD; F7 places a **virtual reference centre** 64 blocks ahead of the camera. It is a measurement aid only: nothing is placed in the world and no gravity is applied. The HUD reports camera coordinate distance divided by the configured Schwarzschild radius, not a measured proper distance in curved spacetime.

The generated `run/config/interstellar.json` controls this bootstrap HUD and reference scale. Planned effects will get actual switches as they are implemented; placeholder options must not imply functioning effects.

## Target

Windows, Ryzen 7 5800X3D, RTX 5070 Ti, 32 GB RAM, 2560x1440 at 60 FPS. This is a performance target, not a benchmark result.

No project license has been selected yet. Third-party notices are in [THIRD-PARTY.md](THIRD-PARTY.md).

## Try the visible optical lab

Enter a development world and press **F8**. The default view shows a black shadow, a bent sky grid and a bright ring produced by an aligned extended test source. **Space** compares lensing on/off; **A** offsets the source to demonstrate multiple images; **G** hides the grid; **Up/Down** changes the static observer radius; **R** resets; **Esc** returns to Minecraft. F7 remains a measurement aid and does not activate world lensing.

This is an illustrative sky at infinity, not a photograph or a terrain renderer. Colour and brightness are not yet spectral/redshift calculations. The static frame stays outside the horizon; the free-fall tour crosses it. Numerical budget exhaustion is magenta. See the science notes for limits.

Press **B** (or click the benchmark text) in the lab to measure its GPU pass. Results are logged after warm-up. See [measured results and methodology](docs/benchmark.md).

Press **V** to compare sampled GPU rays with the CPU reference and analytic shadow boundary. This intentionally pauses briefly and logs results. [Validation results and limits](docs/ray-validation.md).

## Free-fall tour

In the F8 lab, **T** plays/pauses a radial fall, **H** pauses exactly at the horizon, **L** looks back, and **F** switches observer frames. **R** resets. The tour stops at r/r_s=0.35. These are controlled-sky views; Minecraft terrain and the player body are still future work. [Model, controls and verification](docs/free-fall.md).

Routine builds and in-game tests are handled by the development agent. Exploring the open client is optional; user input is needed only for preferences or checks that automation cannot complete.

## Optical settings

Edit **run/config/interstellar-optics.json** for persistent effect defaults, observer settings, playback speed and quality. Reopen F8 to reload. **Q** cycles FAST/STANDARD/FINE temporarily; **R** restores that lab's loaded settings. Keyboard overrides are not auto-saved. See [settings and measured quality costs](docs/optical-settings.md).

## Mass blocks

Use **/give @s interstellar:mass_block**, place connected blocks, then right-click with both hands empty to inspect mass and compactness. Holding a block uses normal RMB placement. Operator command: **/interstellar inspect x y z**. Blocks do not yet activate world lensing. [Model, budgets and verified behavior](docs/mass-blocks.md).

After inspecting a black-hole proxy, open **F8** and press **S** to use its scale and camera distance in the sky lab. Extended sources are metadata-only. Relevant source changes now refresh automatically from the inspected block; unrelated chunk activity preserves the selection. F10 waits through removal, unloading or extended-source states and resumes when usable metadata returns. See [automatic source refresh](docs/source-refresh.md). **R** restores the configured reference view. This does not yet bend terrain.

Press **C** in the lab for the near-critical ray stress test (brief blocking pause). **V** now also logs unwrapped-angle comparisons. [Measured accuracy limits](docs/critical-rays.md).

## Terrain preview

**Quality-first integration experiment:** in F9, press **M** to capture native terrain, frozen mob models and clouds (about 40 seconds at render distance12 in the test scene), then **Space** to enable lensing. **U** compares camera-centred terrain coverage with the old bounds; **E** toggles mobs; **N** compares foreground clouds with the previous background-only clouds; **P** compares the unbent mesh with vanilla. F10 now uses the same native terrain representation with live mobs; fluids, special entity effects and full visible-world coverage remain incomplete. [Terrain evidence](docs/native-mesh.md) · [Mobs and lighting correction](docs/mesh-entities.md) · [Cloud composition and measurements](docs/mesh-clouds.md) · [Expanded terrain coverage](docs/mesh-coverage.md).

Inspect a black-hole proxy, move outside 1.05 r_s, and press **F9**. **Space** compares lensing; arrows look around; **L** aims at the source; **Q** changes resolution; **J** changes path sampling; **V** checks flat geometry; **C** compares curved hits against an independent CPU reference; **B** measures GPU cost. Esc returns to Minecraft. Reopen to capture a new view. This is a frozen opaque-block preview with explicit missing-data boundaries. [Controls, performance, limits and saved test scene](docs/terrain-prototype.md).

## Live terrain

**AA and performance:** the default uses two traced subpixel samples with sharp reconstruction; the soft EDGE filter is optional. In F9, **A** cycles AA, **G** compares adaptive/original ray steps, **Shift+P** captures an AA quality pair, and **Ctrl+P** captures an original/adaptive pair. **T** toggles faster bounds checks; **Ctrl+Shift+P** captures their original/selected comparison. **R** toggles fixed-width mesh addressing; **Alt+P** captures its original/selected comparison. **I** toggles empty-region reuse; **Alt+Shift+P** compares it with uncached traversal. **Shift+I** toggles the original/larger reuse extent; **Ctrl+Alt+P** compares them. Adaptive steps reduced GPU p95 by about31% at the measured small-window, same-quality test pose. Faster bounds checks save another7.7% in a matched1440p test with identical paired pixels. AA still costs time and does not restore all half-resolution detail. [Settings, measurements and limits](docs/aa-performance.md).

**Shader comparison:** native terrain now uses a dedicated shader compiled from the same shared ray/traversal code as the general renderer. In F9, **S** switches programs and **Shift+S** captures a same-frame comparison. Matched1440p tests show a modest3.2–3.4% reduction in GPU pass p95 with identical pixels. All earlier optimization toggles remain available. [Measurements](docs/aa-performance.md).

**Numerical mesh check:** in a ready F9 native mesh preview, **C** runs a small synthetic scene against the independent CPU ray solver (about1.8seconds here). It checks both mesh layouts at near/far distances without changing the world. [Results and limits](docs/mesh-ray-validation.md).

After inspecting a black-hole proxy, press **F10** and allow initial native terrain capture (about40 seconds at render distance12). Then explore up to **256 blocks from the selected source**, with normal movement, animated mobs/clouds and the vanilla HUD. Source availability can impose an earlier limit. **F12** measures the pass; F10 returns to normal rendering. Block/light edits update affected chunks, and movement streams the camera window. Updates are queued rather than immediate. Source mass changes refresh automatically after initial selection, without reloading all terrain. Re-enabling F10 currently recaptures it. Interactions still use straight aim. [Current implementation and checks](docs/streaming-terrain.md) · [Live mob implementation](docs/live-native-mesh.md) · [Earlier voxel prototype](docs/live-terrain.md) · [Wider viewing controls and checks](docs/viewing-range.md).
