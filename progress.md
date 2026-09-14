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
- 2026-09-14: recovered those results after a session usage-limit interruption. Tightened the metadata dependency from ~1.21.1 to exact 1.21.1; rebuild succeeded in 56s. All 5 tests passed again.
- Development client launched successfully; Interstellar registered and wrote the expected default config.
- Created disposable creative world Interstellar Calibration. Visually verified F7 reports r=64 and r/r_s=8, F6 hides the overlay, F1 hides both vanilla and Interstellar HUDs, and movement updates the distance (observed r=67.04). The client later saved and exited normally. World rejoin/reset and malformed-config runtime checks have not been performed.
- No GPU optical renderer exists yet; no visual-physics or FPS validation is claimed.

## Not implemented

GPU ray tracing, lensing, black-hole rendering, source blocks/clustering, potion, actual-body returning images, guided free fall, disk, and retarded entity states.

## Blockers / user decisions

No product decision blocks the bootstrap. Renderer integration and cluster collapse definition remain engineering investigations.

- Bootstrap commit d7a87e7 was pushed successfully to origin/codex/bootstrap-observatory after authentication recovered. Earlier credential and DNS failures are resolved.
- IntelliJ's stalled Gradle import held the shared Loom cache lock after resuming. Stopped only that project's import helper (not IntelliJ), restarted the waiting agent build, and Loom recovered its stale lock. Editor state was preserved.
- Added the existing supported fabric_maven_url property to this machine's previously absent user Gradle properties, pointing to the official secondary host. New IntelliJ imports can now use it without CLI arguments. No project dependency versions or system DNS settings were changed to solve connectivity.
- Mobile pairing and notification permissions require the user to operate their device; push notification delivery is not verified by the agent.

## Exterior lab checkpoint — 2026-09-14, codex/optical-lab

Implemented GPU Schwarzschild exterior sky ray tracing, F8 screen, lensing/grid/alignment toggles and static-radius controls. Build tasks completed successfully; 9 tests passed (5 prior + 4 exterior tests). Runtime shader compilation succeeded. Visually checked default shadow/aligned ring, Space unlensed comparison, A producing separated source images, G removing the grid, and R restoring the view. Fixed vanilla Screen.render blurring the completed lab. Verified Up changes r/r_s from 8 to 6.67 after correcting Windows automation to send an extended arrow key. Window framebuffer was 854x480; no 1440p benchmark or GPU timings were collected.

Earlier statements above that no GPU renderer exists describe the bootstrap checkpoint only. World terrain lensing, mass blocks, potion, returning player-body images and horizon crossing remain unimplemented. Iteration 1 acceptance is not complete.
