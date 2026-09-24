# Interstellar demo

## Install or launch

For this checkout, install a full JDK21 and run **Launch Interstellar.cmd**. It launches the current development build; do not open the same save in two clients.

Development launches use the stable offline name `InterstellarDev`, so the saved return record remains associated with the same player across restarts. A normal Minecraft installation uses your normal account.

Gameplay gravity now defaults to four times the original pull. If upgrading an existing installation, set `strengthPerBlock` to `0.2` in `config/interstellar-gravity.json` and restart. The arrow exhibit automatically relocates its original empty dispensers and uses launches calibrated for that strength and64 mass blocks. Edited or stocked stations are preserved.

The first shader compilation can leave the window unresponsive for roughly1–3minutes on the tested driver. Wait for startup to finish; initial terrain capture begins after entering the world and enabling F10.

For an existing Minecraft installation, use Minecraft1.21.1 with Fabric Loader0.16.14 and Fabric API0.102.1+1.21.1 (the versions used for this build). Put the packaged Interstellar jar and Fabric API in that instance's `mods` directory. The package contains Interstellar and these instructions, not Minecraft, Java, Fabric Loader or Fabric API. No modpack or other shader mod is needed for the demonstration.

Open a world with cheats enabled, or use an account with command permission level2. A render distance of12 is the measured default; the current renderer supports at most16. The existing preset uses half linear rendering scale with sharp2xAA. Keep this preset for the recorded performance; full-resolution rendering costs more.

Minecraft may show an experimental-settings notice because the mod registers a custom dimension. On an existing save, use **Create Backup and Load**. This is a Minecraft world-loading step, before the demo command is available.

## Choose an exhibit

- **New gameplay:** `/interstellar demo gameplay` builds a separate exhibit with gradual mass progression, automatic discovery and local mob/projectile gravity. A complete 4×4×4 source is at the black-hole threshold; removing blocks reduces the field, and compact 2×2×2/3×3×3 builds keep visible material with lensing. Close mobs can lift and be captured; ordinary/spectral arrows and thrown projectiles bend. Players and terrain are unaffected. Model, settings and limits: `docs/gameplay-gravity.md` in the repository or demo archive.
- **Arrow course:** `/interstellar demo arrows` enters another separate exhibit with four automatic calibrated dispensers. Orange demonstrates a transient loop, cyan a flyby, magenta a shot pulled back, and red capture. `/interstellar demo view arrows` restores the close viewpoint; `/interstellar demo arrows on|off|once` controls firing. The scene is tuned for its original64-block source and default gravity; edits change the paths. Native arrow drag, downward gravity and collisions remain active. Your existing gameplay exhibit is preserved.
- **Preserved optics reference:** `/interstellar demo enter` keeps the previous stronger mass calibration and passive mobs. Use this for comparison with earlier screenshots.

## Explore and return

1. Run either entry command above. The first entry builds its exhibit in a separate dimension. Occupied cells that differ from the exhibit stop construction instead of being overwritten.
2. Press **F10** to arm lensing. Nearby sources are found automatically; initial discovery/capture takes a moment. The HUD shows progress, source mass, compactness and viewing range.
3. Fly with **WASD**, **Space** up and **Shift** down; look with the mouse. The coloured wall, foreground pillar, terrain steps, stairs/slabs, leaves and sheep demonstrate curved images and occlusion.
4. Run `/interstellar demo leave` to return to your saved dimension, position, view direction, game mode and flight state. This also cancels a queued entry. Your inventory is retained. Nearby sources in your original world are discovered automatically too.

Each exhibit is built once and retained in the save. Re-entering does not reset later edits or duplicate its sheep. The return record is saved with the world and survives switching exhibits. Entry enables creative flight. Gameplay gravity can remove captured mobs/projectiles; it does not destroy terrain or affect the player. Use spawn eggs/bows to experiment, or build your own mass-block assembly in a normal world.

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
| `/interstellar inspect x y z` | Optionally pin a connected source for inspection |
| `/interstellar source auto` | Resume automatic source selection |
| `/interstellar source status` | Report discovered sources and pending work |
| `/interstellar gravity enabled true/false` | Enable/disable entity gravity for this server session |
| `/interstellar gravity capture true/false` | Enable/disable horizon absorption for this session |
| `/interstellar-visuals body true/false` | Experimental returning player images (default off) |
| `/interstellar-visuals weather true/false` | Show/hide the inexpensive local rain/snow approximation |

Source edits, splits, merges and chunk reloads refresh automatically. F10 stays on through the horizon; interior blocks use normal positions for editing. Leaving the supported viewing range pauses lensing; returning resumes it. F10 controls optics independently of entity gravity. Persistent physics settings are in `config/interstellar-gravity.json`. Entity dynamics currently support enclosing radius up to16 blocks and horizon radius up to12; larger sources retain optics and show the physics size limit. The central background is dark below0.1 horizon radii; block editing stays active. See [horizon assumptions and checks](horizon-body-study.md).

## Performance and coverage

First-person hands and held items remain visible over the live F10 scene. The included `docs/minecraft-coverage.md` lists current graphics and gameplay gaps.

The final calibrated natural-world build measured about59FPS facing the wall and34FPS looking down at terrain, at2560x1440 on an RTX5070Ti. The fresh pre-refinement comparison measured53/33FPS; measured slow-frame percentiles also improved. These are representative medians, not an absolute floor. This exhibit is simpler and cannot establish natural-terrain performance. See `docs/material-coverage.md` for both runs, conditions and limitations.

Native water/lava, glass, ordinary entities/block entities, emissive/glint materials, text, shadows and selection/mining overlays join the curved scene. Glowing-status outlines follow curved images through walls. Actual player-body images can appear as thin arcs near the black-hole edge; no delayed pose history is simulated. Rain/snow uses a small unbent foreground pass. Particles and various special surfaces remain absent; water blending is not physical refraction. Current sharp2xAA still limits fine secondary detail. See [current coverage](minecraft-coverage.md) and [feature checks/timings](world-features.md).
