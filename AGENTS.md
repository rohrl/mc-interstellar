# Working on Interstellar

Read README.md, decision-log.md, progress.md, and handoff.md before continuing. The user is an experienced developer relying on us to drive graphics and scientific accuracy.

## Working agreement

- User authorizes normal implementation, branches, commits, and pushes to origin. Keep work reviewable on development branches. Do not force-push or discard unrelated edits.
- Maintain decision-log.md for important technical decisions and reasons. Mark proposals, accepted choices, and superseded choices distinctly.
- Update progress.md and handoff.md at meaningful checkpoints and before ending an iteration. Record exact checks and remaining limitations.
- Notify the user in the active task when blocked or when a material product choice requires them. Routine implementation choices are delegated; record their rationale rather than repeatedly asking permission.
- Never claim a renderer is scientifically validated or meets FPS targets based only on compilation or a screenshot.
- Use primary scientific sources and official technical documentation; record URLs and assumptions in docs/science.md.
- Keep server work bounded and event-driven. No terrain destruction or expensive GR solver on the server tick thread.
- The actual player body/skin is required for returning-light demonstrations. Do not substitute a mannequin as the product feature.
- Accretion disk and general entity history/horizon-freeze effects are deferred. Guided horizon crossing remains in scope.
- Keep independent SR and GR scale settings; compose them only through an explicitly defined local observer frame.
- Preserve local .idea settings, saved worlds, and credentials. Do not commit them.
- Do not add a project license without the owner's choice. Retain licenses/notices for copied third-party material.

## Checks

Run the Gradle build for Java/resource changes and relevant mathematical tests for optical changes. In-game rendering changes additionally need runtime shader compilation, visual checks, and measured timings. Clearly record which checks were actually possible.

## Runtime testing ownership

The owner explicitly requests autonomous in-game inputs and verification. Operate the development client, capture results, and iterate without expecting owner attendance. An open client is for optional exploration, not a pending test assignment.
