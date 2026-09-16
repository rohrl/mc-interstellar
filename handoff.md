# Current handoff

Repo: C:\work\code\minecraft\interstellar\interstellar. Branch: codex/distant-heightfield. Origin: https://github.com/rohrl/mc-interstellar.git. JDK: C:\Portable\jdks\temurin-21.0.12.1. Fabric/Minecraft 1.21.1. Read AGENTS.md; preserve worlds, .idea and secrets. Branches/commits/pushes and autonomous runtime checks authorized. No force pushes or subagents.

## Current priority

LATEST: owner rejected the height-field appearance (white mountain columns, dark flat sky). Read docs/visual-reference-plan.md and D033. Next establish deterministic vanilla-versus-zero-bending appearance comparisons and a Minecraft-compatible appearance path; only then build the slow lensed reference/fast-image suite. These are planned, not implemented. User wants automated metrics to reduce routine screenshot consumption; preserve human approval at major visual gates. No further approximation tuning before this parity gate. Latest assessment changed docs only; owner may have enabled distantPrototype locally, so do not assume the earlier restored config/client state still applies.

Working demo must blend coherently into the world at usable frame rates. Owner welcomes distant heuristics with small/hard-to-notice errors and substantial gains. Step 2 remains unfinished and precedes packaging. Same-screen background copy was rejected for duplicated walls (87dd3a1, reverted 0a264b0). Do not reintroduce it or insist on exact whole-world tracing. Read plan.md, D031/D032 and docs/distant-prototype.md.

## Latest experiment

Opt-in 256-square distant height field, accurate local 96-cubed volume with exclusive ownership, nearest-hit comparison, independent GPU/CPU distant slabs. F9 H compares; F10 option distantPrototype defaults false. Unknown/far geometry fades into a simplified sky; distant caves/overhangs, materials and residual weak-field bending are approximated. No ordinary camera image is sampled.

Build/45 tests pass. Distant 93 rays/21 hits and local V have zero mismatches. Major-rendering visual checks include downward landscape and live side strafe. Final frozen far-view 1440p/half-resolution GPU p95=9.932704 ms, frame interval p95=11.5638 ms. Details, failed optimization timings and limits are in docs/distant-prototype.md. The final HUD wording edits were built, without redundant screenshots. Prototype is NOT demo-ready: conspicuous fog cutoff/flat sky, near-boundary simplification and far-approximation error remain unresolved. Next evaluate sky/coverage continuity and camera-relative near/far representation; do not promote default until moving-view and performance gates pass.

Original run/config/interstellar-terrain.json restored from run/distant-config-backup.json. Client left running normal F10, experiment off, 870x519; local capture ~0.9 s restored. Player position restored to (72.867614,284.998488,-5.180181), selected source anchor (14,300,14). N=64, COM=(16,302,16), r_s=8; latest 11700 opaque/0 unknown/0 unsupported (snowfall changes counts). No blocks edited this iteration. User may move/edit afterward.

## Stable foundations

Source refresh/placement: dc50cbb, docs/source-refresh.md. Anchored source selection, bounded debounced recovery through edits/removal/unload/split/extended source; F10 intent retained. Manual off stays off. Respawn guard compiled, not death-tested. Empty-handed RMB inspection; held-item RMB placement fixed and verified. Owner authorized original source enlargement to 64 blocks.

Viewing: camera range 1.05 r_s to 128 blocks, automatic pause/recovery; docs/stable-exploration.md. Independent curved finite-hit validation: da7771e and docs/curved-terrain-validation.md (768 sampled comparisons at earlier checkpoint). F8 sky lab; F9 frozen view/V/C; F10 live; F12 timing. Actual player body, terrain crossing and observer speed unfinished.

AA WIP stays separate at 8ad46eb on codex/terrain-antialiasing, unverified and excluded. Demo setup command not implemented. Existing launcher runs current checkout, never switches branches. No owner test assignment pending.

## Efficient runtime workflow

Redirect verbose Gradle output; build with JAVA_HOME above. run/stable-init.gradle quick-plays Interstellar Calibration. Close the identified Minecraft process normally with CloseMainWindow and wait for exit before relaunching; never open the save twice. Use a fresh runtime log path and gate input on joined-the-game. Process/window inspection requires the desktop-capable escalated context.

GUI helpers: C:\Users\karol\Documents\Codex\2026-09-13\i-want-to-create-a-minecraft\work. run/send-safe-command.ps1 pastes commands/restores clipboard. Use control-minecraft.ps1 -HoldKey with 100-150 ms for Escape/function keys (27, F9=120, F10=121, F12=123); transient SendKeys sometimes missed them. F3+T needs held keys and a logged resource reload. Wait for benchmark completion (bounded loop), not a fixed seven seconds, before resizing. Screenshot only major visual acceptance or failure diagnosis, at modest resolution. No screenshots needed for simple text/config edits.

Evidence: distant-build.log, distant-verified-runtime.log, distant-bounds-runtime.log and run/distant-*.png (ignored). Avoid dumping logs/docs repeatedly; feature doc contains detailed evidence. Keep future handoffs concise instead of appending contradictory overrides.
