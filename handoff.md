# Current handoff

Repo: C:\work\code\minecraft\interstellar\interstellar. Branch: codex/distant-heightfield. Origin: https://github.com/rohrl/mc-interstellar.git. JDK: C:\Portable\jdks\temurin-21.0.12.1. Fabric/Minecraft 1.21.1. Read AGENTS.md; preserve worlds, .idea and secrets. Branches/commits/pushes and autonomous runtime checks authorized. No force pushes or subagents.

## Current checkpoint — 2026-09-17

Interrupted appearance implementation repaired and runtime verified. GLSL reserved name `packed` was the terrain compilation blocker; renamed `lightCode`. Native sky/cloud capture, Minecraft lightmap/face shading and separate snow-cap/side materials work in F9/H and F10. Same-frame vanilla/zero-bending capture (F9 P) and Java comparator implemented. Read docs/native-appearance.md for controls, results and limits.

Build/45 tests and comparator self-tests pass. Local/distant V checks have zero mismatches. Snowy-terrain pair RGB MAE 0.0423; repeated vanilla references pixel-identical. Contact sheet inspected. Live F10 1440p/half-resolution GPU p95=9.90976 ms, frame interval p95=12.0548 ms; optical timer excludes capture/upload/sky/upscale. No universal performance or appearance-parity claim.

Client left running F10 with local enabled=true, distantPrototype=true, renderScale=0.5, window 870x519. Source anchor (14,300,14), N64, COM=(16,302,16), r_s=8; 11700 opaque/0 unknown/0 unsupported. Camera near (16.5,302,-85.5), yaw .281, pitch .91. No blocks edited. Owner may move/edit afterward. Code default remains experiment off. Existing launcher runs current checkout without changing branches.

## Next priority

Step 2 world integration remains unfinished and precedes packaging. Native appearance fixes remove the gross white mountains/flat dark sky, but bounded height-field edges/fade, incomplete geometry/materials/lighting, entities/transparency and cloud alignment remain. Expand fixed-pose vanilla comparisons and resolve conspicuous errors before building the slow lensed reference (not implemented). Owner deferred automated movement/flicker checks and will report those issues; retain periodic snapshot inspection at major checkpoints. The first harness does not establish parity or validate arbitrary projection/settings.

Owner welcomes distant heuristics with small/hard-to-notice errors and substantial measured gains. Same-screen background copy caused duplicated walls (87dd3a1, reverted 0a264b0); do not reintroduce it. Native sky capture contains no terrain image. See plan.md, docs/visual-reference-plan.md and D031–D034.

## Stable foundations

Source refresh/placement: dc50cbb, docs/source-refresh.md. Anchored selection and bounded debounced automatic recovery; manual F10 off stays off. Respawn guard compiled, not death-tested. Held-item RMB placement and empty-hand inspection verified previously.

Viewing: camera range 1.05 r_s to 128 blocks, automatic pause/recovery; docs/stable-exploration.md. Independent curved-hit validation: da7771e and docs/curved-terrain-validation.md. F8 sky lab; F9 frozen view/V/C/H/P; F10 live; F12 timing. Player-body images, terrain crossing, observer speed and demo setup command unfinished.

AA WIP stays separate at 8ad46eb on codex/terrain-antialiasing, unverified and excluded.

## Efficient runtime workflow

Redirect verbose Gradle output. run/stable-init.gradle quick-plays Interstellar Calibration. Close the identified Minecraft process normally with CloseMainWindow and wait for exit before relaunching; never open the save twice. Use a fresh runtime log and gate input on joined-the-game. GUI process/window access requires desktop-capable escalated context.

GUI helpers: C:\Users\karol\Documents\Codex\2026-09-13\i-want-to-create-a-minecraft\work. run/send-safe-command.ps1 pastes commands/restores clipboard. Use control-minecraft.ps1 -HoldKey with 100–150 ms for Escape/function keys (27, F9=120, F10=121, F12=123); transient SendKeys may miss them. Gate benchmarks on completion before resizing. No screenshots for simple text/config changes.

Current ignored evidence: appearance-recovery-build.log, appearance-recovery-final-build.log, appearance-recovery-runtime.log and run/interstellar-captures. Captures/metrics indexed in docs/native-appearance.md. Local experiment config intentionally remains on for owner inspection.
