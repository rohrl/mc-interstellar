# Interstellar

A Minecraft Java mod for educational relativistic optics. Scientific assumptions and numerical limits must be visible and testable.

**Current iteration:** Fabric diagnostics and an F8 exterior optical lab with GPU Schwarzschild sky lensing. Terrain lensing, mass blocks, potion and horizon crossing are not implemented yet. See [progress.md](progress.md) for verified results rather than assuming planned features exist.

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

This is an illustrative sky at infinity, not a photograph or a terrain renderer. Colour and brightness are not yet spectral/redshift calculations. The camera stays outside the horizon. Numerical budget exhaustion is magenta. See the science notes for limits.
