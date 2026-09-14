# Handoff

Read this, decision-log.md, progress.md and docs/science.md before continuing. Do not reconstruct requirements from a generic Minecraft black-hole prompt.

## Location and authorization

- Git root: C:\work\code\minecraft\interstellar\interstellar.
- Parent folder of same name is intentional and is not the Git root.
- Remote: https://github.com/rohrl/mc-interstellar.git.
- Branch: codex/bootstrap-observatory. Verify git status/branch before modifying anything.
- User permits branches and pushes. No force pushes or unrelated cleanup.
- User requests notifications for blockers/decisions and durable Markdown project records.

## Environment

Windows PowerShell; JDK C:\Portable\jdks\temurin-21.0.12.1. Git is available through the agent runtime and C:\Portable\PortableGit. IntelliJ 2026.2.2 installed. 5800X3D / 32 GB / 5070 Ti, 1440p60 target.

The original agent task started in a generated Documents/Codex folder, not in the repo. Its sandbox requires reviewed elevation for C:\work writes and network access. This is an execution-environment constraint, not a request to change the project location. Do not hardcode the original task's scratch directory into the project.

## Resume procedure

1. Inspect git status, latest commits, and progress.md. Preserve unrelated local edits, especially .idea.
2. Select a complete JDK 21 and run .\gradlew.bat build.
3. Follow the next unfinished acceptance criterion in plan.md. Never mark GPU/runtime/FPS checks complete based on unit tests.
4. Update decision-log.md for major choices and progress.md/handoff.md with actual results, next steps and blockers before finishing.

## Critical requirements

Actual player body, not mannequin. Crossing the horizon remains in scope despite deferral of entity freeze/history. Accretion disk deferred. Independent observer speed initially, separate SR/GR scales allowed. Dedicated pack acceptable but Iris not mandatory. Screen-space-only rendering is insufficient for the final scientific ambition.

## Next implementation step

After the foundation is built and the HUD checked, implement an independent horizon-regular numerical reference and a controlled GPU scene. Define the observer tetrad and past-directed ray initial conditions before writing the visual effect. Resolve scene-data access with a measurable spike; do not begin by inventing Iris uniform APIs.

## Resume checkpoint — 2026-09-14

Bootstrap commit d7a87e7 contains the scaffold and documentation and has been pushed to origin/codex/bootstrap-observatory. Build succeeded, 5/5 reference tests passed; runtime HUD, F6, F7, F1 and movement were visually checked in a disposable creative world. Check progress.md for remaining checks. Earlier GitHub authentication failures are resolved.

This machine's user Gradle properties now set fabric_maven_url=https://maven2.fabricmc.net/. If an IDE import holds the Loom lock, diagnose the owning process before touching it; never delete an active lock or stop unrelated builds. The agent previously stopped only the identified stalled import for this project, preserving IntelliJ.

## Latest checkpoint — exterior lab

Continue on codex/optical-lab. F8 now opens a mod-owned exterior test-sky shader; Space/A/G/R were visually checked. Up was verified (8 to 6.67); Windows automation needs extended-key flags and scan codes for arrows. Nine mathematical tests pass. No quantitative GPU comparison or performance benchmark yet. Do not confuse the exterior RK4 CPU diagnostic with the planned independent horizon-regular reference. Next: add GPU readback/timing validation, then implement valid falling-observer rays across the horizon. Preserve the planned terrain/body integration scope. The development client was left open for the owner to try the visible lab.
