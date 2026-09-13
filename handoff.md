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
