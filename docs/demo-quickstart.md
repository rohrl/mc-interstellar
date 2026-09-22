# Interstellar demo

## Install or launch

For this checkout, install a full JDK21 and run **Launch Interstellar.cmd**. It launches the current development build; do not open the same save in two clients.

For an existing Minecraft installation, use Minecraft1.21.1 with Fabric Loader0.16.14 and Fabric API0.102.1+1.21.1 (the versions used for this build). Put the packaged Interstellar jar and Fabric API in that instance's `mods` directory. The package contains Interstellar and these instructions, not Minecraft, Java, Fabric Loader or Fabric API. No modpack or other shader mod is needed for the demonstration.

Open a world with cheats enabled, or use an account with command permission level2. A render distance of12 is the measured default; the current renderer supports at most16. The existing preset uses half linear rendering scale with sharp2xAA. Keep this preset for the recorded performance; full-resolution rendering costs more.

Minecraft may show an experimental-settings notice because the mod registers a custom dimension. On an existing save, use **Create Backup and Load**. This is a Minecraft world-loading step, before the demo command is available.

## Enter the exhibit

1. Run `/interstellar demo enter`. The first entry builds a separate exhibit in the `interstellar:demo` dimension. Existing dimensions are not rebuilt or cleared. Occupied cells that differ from the exhibit stop construction instead of being overwritten.
2. Wait for the ready/source messages, then press **F10**. The first world capture takes a moment. The HUD shows progress, source mass and viewing range.
3. Fly with **WASD**, **Space** up and **Shift** down; look with the mouse. The coloured wall, foreground pillar, terrain steps, stairs/slabs, leaves and sheep demonstrate curved images and occlusion.
4. Run `/interstellar demo leave` to return to your saved dimension, position, view direction, game mode and flight state. This also cancels a queued entry. Your inventory is retained. Inspect your original mass source before re-enabling F10 there.

The exhibit is built once and retained in the save. Re-entering does not reset later edits or duplicate its sheep. The return record is saved with the world. The demo changes to creative flight while inside; it does not apply destructive gravitational physics.

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

The previous calibrated natural-world build measured about53FPS facing the wall and34FPS looking down at terrain, at2560x1440 on an RTX5070Ti. These are representative medians, not an absolute floor. This exhibit is simpler and cannot be used to claim the same improvement on natural terrain. See `docs/triangle-row.md` for the original timing conditions.

Visual refinement is in progress. Fluids/transparency, special entity layers and block entities are not yet fully supported. Rain/snow is deferred. The two sampled ray paths preserve the accepted AA quality; higher-order images remain a refinement target. Do not infer complete Minecraft rendering coverage from the demonstration scene.
