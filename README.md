# Interstellar

A Minecraft Java mod for educational relativistic optics. Scientific assumptions and numerical limits must be visible and testable.

**Current iteration:** Fabric diagnostics and an F8 optical lab with Schwarzschild sky lensing and a guided free-fall horizon crossing. Terrain lensing, mass blocks and potion are not implemented yet. See [progress.md](progress.md) for verified results rather than assuming planned features exist.

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
