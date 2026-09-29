# Handoff — Relativistic Sight, 29 September 2026

## Completed state

Branch **codex/rtx-wormhole-demo**; see Git for the final commit. User authorizes
normal branches, commits, pushes and autonomous runtime controls. No agents.

Relativistic Sight potion, sprint-driven optical speed and independent F4
aberration / Doppler colour / brightness controls are implemented. Normal player
movement is unchanged. Eight-minute duration, ~0.1c to 0.99c over 15s, <=0.34s
release, on-foot sprint (jumping allowed), horizontal travel sets direction.
HUD gives preparation, current c fraction and charge. Cap/ramp are adjustable.
Creative Food & Drinks; Awkward Potion + Amethyst; /interstellar relativity potion.

Works without a mass or with mass/wormholes. OpenGL and RTX share the boost;
normal jar still excludes Vulkan/shaderc/backend code. Source-free idle uses
native drawing while retaining the bounded scene cache. Colour/brightness are
explicit approximations; no entity history or server time effects.

Read **docs/relativistic-sight.md**, D096 and
**docs/profiles/2026-09-29-relativistic-sight/README.md** for exact checks, images,
measurements and limitations. Prior unified gameplay is D095 / 8b0ed4d.

## Verification

- Final normal and RTX builds pass 109 CPU tests. Normal jar checked for absence
  of optional backend entries; proof copy run/sr-opengl-only.jar.
- Runtime shaders compile/link. Final acceptance launch has no ERROR/Exception/
  shader-link-failure messages. Existing optimized-out uniform warnings remain.
- 50 GPU local observer rays pass, maximum direction/relative-frequency error
  1.6369261e-6; independent CPU photon four-vector reference.
- Final gentle 0.99c GL/RTX mean RGB error 0.000686/255; mixed optics 0.000110/255.
  No pixels in these two pairs have max-channel difference >16/255.
- Actual drinking, automatic activation, cap, release, wall stop, independent
  menu switches, restart persistence and mixed optics exercised.
- Frozen simple-course p50 frame 8.352ms (120 FPS cap); timings are not a claim
  about heavy BH FPS or isolated SR cost. Report contains renderer wall timings.
- Brewing recipe compiles/registered; a real brewing-stand cycle was not tested.
  Broad movement/flicker survey remains owner-deferred.

## Runtime and preservation

Client **saved and closed**. Only **run/saves/Interstellar Relativity QA 2026-09-29**
was edited: copied from prior Unified QA, with a colour-gate track at
x446..458, y210, z-185..-5. Saved at feet(452.5,211,-174.5), yaw0/pitch0, survival,
with a fresh potion in inventory. Native mass64 and pair revision28 retained.
Original owner and previous QA saves were not edited. AA WIP8ad46eb and .idea
untouched.

Prior terrain preferences restored from run/sr-terrain-prefs-backup.json:
master/mass/portal/status on, 100%/fine/2x/RTX, weather on, body off. New relativity
preferences remain defaults (all three on, Gentle, 0.99c/15s).

Final jar build/libs/interstellar-0.1.0-dev.jar is RTX flavour. Ordinary launchers
remain unchanged. Cached shader launches are fast; changing the huge shared
shader forced approximately six minutes of cold GL compilation in this session.
JDK C:/Portable/jdks/temurin-21.0.12.1; Python C:/Portable/python-3.11.7/python.exe.

Useful ignored files: run/sr-init.gradle (quickplay QA), sr-sprint.ps1,
sr-chord.ps1, sr-acceptance-runtime.log, sr-acceptance-qa.log, sr-final-qa-first.log,
sr-normal-build.log, sr-rtx-build.log. Final backend pairs:
run/rtx-image/compare-1790661422024 (Gentle), compare-1790661572579 (mixed).
Their reports and accepted screenshots are copied into the tracked evidence.

## Follow-up scope

Owner feedback on visual strength/feel is the next useful input. No new goal is
active. Dynamic retarded-time histories, true spectra/UV/IR and gravitational
spectral transport are separate work. Do not call the palette approximation exact
Doppler radiometry. A before/after heavy-world performance regression study was
not part of these feature checks. Preserve the optional OpenGL-only flavour.
