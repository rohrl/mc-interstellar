# Handoff — settings, placed wormholes and 4×/8× AA, 2026-09-29

## Current checkpoint

Branch `codex/rtx-wormhole-demo`; see Git for latest commit. Commits/pushes and
runtime inputs authorized. No subagents. Preserve original saves, .idea and AA
commit 8ad46eb. Follow AGENTS.md. No active overnight goal.

Implemented F4 live settings, reusable throwable Wormhole Seed with one persistent
pair (later throws replace oldest; sneak-use closes), and Off/Edge/2×/4×/8× AA on
both renderers. Native AO recaptures lighting; preferences persist. RTX extra
sample-atlas rows allocate only for selected higher modes. Default remains 2×.
Fixed opaque-only scenes failing RTX eligibility. Normal build remains Vulkan-free.

Revisioned layouts handle chunk readiness, ticket release, overlapping regions,
reconnect/respawn and retired client chunks. Demo pair follows the same global
oldest-mouth rule. Confirmed lower-rim arrival could embed feet in the exit floor;
full destination bounding-box check now rejects obstructed travel before camera
transport, returning to the prior clear entrance position.

## Read next / evidence

- docs/settings-and-wormhole-seed.md: implementation, usage, results and limits.
- docs/profiles/2026-09-29-settings-portals/: compact evidence and screenshots.
- docs/wormhole-demo-implementation.md: original Ellis physics/GPU validation.
- docs/rtx-optical-variants-2026-09-28.md: BH/extended/horizon RTX coverage.
- docs/demo-quickstart.md: launch and controls.

Both builds pass 88 tests, zero failures/errors. Normal artifact checked for no
optional/Vulkan/shaderc classes before the final common-code collision guard;
both builds passed again afterwards. Logs: run/settings-portals-collision-build.log,
run/settings-portals-collision-rtx-build.log; earlier artifact verification in
run/settings-item-artifact-check.txt.

Runtime checked menu, AO recapture, settings persistence, renderer selection,
actual throws/replacement/rejection, reload, retired-mouth inactivity and two-way
passage. Native shader compilation and inspected GL/RTX comparisons passed.
1280×720 full/fine MAE on 0–255: 2× .00304, 4× .00326, 8× .00293. RTX frame medians:
2× 15.95ms, 4× 26.45ms, 8× 48.36ms. Default 2× within about 1% of earlier timing.
Opaque item course MAE .00776 at half/2×. These are scene-specific checks.

Collision regression: old arrival feet Y100.39 inside floor ending Y101; fixed
attempt retained entrance feet Y101, then clear centre crossings passed both ways.
Optical equations unchanged; no redundant Ellis solver rerun. Broad movement and
flicker testing remains owner-deferred.

## Current runtime / owner state

One RTX client, isolated save `Interstellar Overnight Check 2026-09-28`, 1280×720,
paused safely. PID 23144; VERIFY first. Gradle session 81370. Runtime log:
run/settings-portals-collision-runtime.log. No active input helpers. Close the
identified client normally and wait for actual exit before relaunching.

Owner pair revision 8 restored and verified, dimension interstellar:gameplay:
A(250.5,110,243.67978012696636), B(243.75889273363606,110,212.71281489326503).
Do not replace it with the fixed exhibit merely for convenience. Owner quality
restored: full resolution, fine paths, 2×, RTX preferred, weather on, body off.
Backups: run/aa8-owner-wormhole.dat and run/aa8-owner-terrain.json.
Owner had fallen into the void before restart; respawned and restored creative
flight with one seed. Final safe feet(250.5,108.38,275.7), yaw180, pitch0, roll0.
Original Calibration and previous Visual Check saves untouched.

Test course floor in Gameplay: Y100, X238–342/Z210–260 plus two approach pads.
Temporary placement blocker removed. New owner exploration supersedes saved poses.

Launch `Launch Interstellar RTX.cmd` / `gradlew.bat runClient -PinterstellarRtx`.
Ordinary launcher remains OpenGL-only. Test init `-I run/overnight-init.gradle`
selects the isolated save. JDK: C:/Portable/jdks/temurin-21.0.12.1.
F4 settings; R upright; Alt+F12 renderer; F10 optics; F12 timing.
Seed: Creative Tools & Utilities or `/give @s interstellar:wormhole_seed`.

Ignored run/ helpers: menu-click.ps1 uses reliable held clicks (old click helper
can miss); control-short.ps1 uses held keys; send-safe-command.ps1 preserves the
clipboard; rtx-live-pair.ps1 and rtx-live-timings.ps1 gate on log completion.
Use screenshots for meaningful visual changes and compact logs for routine checks.

## Limits

One same-dimension pair, radius 8, open volume required. Capture remains five chunks
around each mouth. Nearest-mouth chart approximation is not a global two-mouth
Einstein solution. Player passage only; no remote mob tracking, vehicle/projectile
transit, remote block interaction or persistent roll on reconnect. Supporting
exotic matter and massive-body geodesics are not simulated. 8× is expensive and
optional; do not silently raise defaults.
