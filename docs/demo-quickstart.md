# Interstellar demo

## Install or launch

For this checkout, install a full JDK21 and run **Launch Interstellar.cmd**. It launches the current development build; do not open the same save in two clients.

Development launches use the stable offline name `InterstellarDev`, so the saved return record remains associated with the same player across restarts. A normal Minecraft installation uses your normal account.

The first shader compilation can leave the window unresponsive for roughly1–3minutes on the tested driver. Wait for startup to finish; initial terrain capture begins after entering the world and enabling F10.

For an existing Minecraft installation, use Minecraft1.21.1 with Fabric Loader0.16.14 and Fabric API0.102.1+1.21.1 (the versions used for this build). Put the packaged Interstellar jar and Fabric API in that instance's `mods` directory. The package contains Interstellar and these instructions, not Minecraft, Java, Fabric Loader or Fabric API. No modpack or other shader mod is needed for the demonstration.

Open a world with cheats enabled, or use an account with command permission level2. A render distance of12 is the measured default; the current renderer supports at most16. The existing preset uses half linear rendering scale with sharp2xAA. Keep this preset for the recorded performance; full-resolution rendering costs more.

Minecraft may show an experimental-settings notice because the mod registers a custom dimension. On an existing save, use **Create Backup and Load**. This is a Minecraft world-loading step, before the demo command is available.

## Enter the exhibit

1. Run `/interstellar demo enter`. The first entry builds a separate exhibit in the `interstellar:demo` dimension. Existing dimensions are not rebuilt or cleared. Occupied cells that differ from the exhibit stop construction instead of being overwritten.
2. Wait for the ready/source messages, then press **F10**. The first world capture takes a moment. The HUD shows progress, source mass and viewing range.
3. Fly with **WASD**, **Space** up and **Shift** down; look with the mouse. The coloured wall, foreground pillar, terrain steps, stairs/slabs, leaves and sheep demonstrate curved images and occlusion.
4. Run `/interstellar demo leave` to return to your saved dimension, position, view direction, game mode and flight state. This also cancels a queued entry. Your inventory is retained. Inspect your original mass source before re-enabling F10 there.

The exhibit is built once and retained in the save. Re-entering does not reset later edits or duplicate its sheep. The return record is saved with the world. The demo changes to creative flight while inside; it does not apply destructive gravitational physics.

New exhibits also contain a bed/chest, two stained-glass layers and a contained pool with a glass front. Older saved exhibits retain their existing layout.

## Repeatable viewpoints

| Command after `/interstellar demo view` | Demonstration |
| --- | --- |
| `wall` | Centred wall/ring view |
| `side` | Side view and occlusion |
| `close` | Stronger bending on approach |
| `terrain` | Look down over the foreground terrain |

These commands work inside the exhibit. They are static demonstration viewpoints, not a claim of general teleport support.

## Controls

| Control | Action |
| --- | --- |
| F10 | Enable/disable live lensing; disabling releases the capture |
| F12 | Start/clear timing; the result is also logged |
| F1 | Minecraft HUD visibility |
| F9 | Frozen inspection view for comparisons; Escape returns |
| F8 | Separate optical sky lab |
| `/interstellar inspect x y z` | Select a connected mass-block source in a loaded nearby chunk |

Selected-source edits refresh automatically. Leaving the supported exterior/range pauses lensing and restores normal viewing; returning resumes it. Terrain horizon crossing is a later feature.

## Performance and coverage

The final calibrated natural-world build measured about59FPS facing the wall and34FPS looking down at terrain, at2560x1440 on an RTX5070Ti. The fresh pre-refinement comparison measured53/33FPS; measured slow-frame percentiles also improved. These are representative medians, not an absolute floor. This exhibit is simpler and cannot establish natural-terrain performance. See `docs/material-coverage.md` for both runs, conditions and limitations.

Native water/lava, stained glass, ordinary entity/item layers and block entities such as beds/chests are supported in the curved scene. Additive/glint effects, sign text, particles, coplanar overlays and boat water masks remain incomplete; rain/snow is deferred. Water uses native surface blending rather than physical refraction. Current sharp2xAA is retained: tested replacements either cost too much FPS or worsened fine-ring metrics. Very fine secondary-image detail remains limited by the sample count.
