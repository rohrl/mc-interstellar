# Progress

Last updated: 2026-09-14. Branch: codex/bootstrap-observatory.

## Verified environment

- Windows; Ryzen 7 5800X3D, 32 GB RAM; RTX 5070 Ti reported by user.
- Minecraft 1.21.1 installation exists.
- IntelliJ IDEA 2026.2.2 found under C:\Portable.
- Temurin java/javac 21.0.12.1 executed successfully; jmods/compiler module present.
- Git origin points to rohrl/mc-interstellar; local settings preserved.

## Current work

Iteration 0: documentation and Fabric bootstrap implemented.

- Pinned Java 21 / Minecraft 1.21.1 / Fabric build with checked Gradle distribution SHA-256.
- Split common/client source sets and separate development game directory.
- F6 calibration HUD and F7 virtual reference centre; no physical world changes.
- Validated calibration config, preserving malformed user files while falling back to defaults.
- Analytic Schwarzschild radii/static shadow half-angle and one-dimensional SR reference quantities.
- README, agent instructions, decision log, roadmap, science notes, setup and handoff documents.

## Executed verification

- 2026-09-13: Gradle build succeeded in 4m 11s using the official Fabric secondary Maven service.
- JUnit report: 5 tests, 0 failures, 0 errors, 0 skipped. Checks cover shadow angular branches, scale invariance, distant limit, Doppler reciprocity/known velocity, and invalid inputs.
- Remapped mod and sources JARs generated in build/libs (ignored by Git).
- 2026-09-14: recovered those results after a session usage-limit interruption. Tightened the metadata dependency from ~1.21.1 to exact 1.21.1; rebuild and development-client startup are in progress.
- No GPU optical renderer exists yet; no visual-physics or FPS validation is claimed.

## Not implemented

GPU ray tracing, lensing, black-hole rendering, source blocks/clustering, potion, actual-body returning images, guided free fall, disk, and retarded entity states.

## Blockers / user decisions

No product decision blocks the bootstrap. Mobile pairing and notification permissions require the user to operate their device; push delivery is not verified by the agent. Renderer integration and cluster collapse definition remain engineering investigations.
