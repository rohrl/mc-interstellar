# Interstellar

Black holes, traversable wormholes and near-light-speed visuals in **Minecraft
Java Edition 1.21.1**. Build a gravitational source from mass blocks, watch the
world bend around it, throw a pair of portals, or drink a potion to explore
relativistic vision while walking.

This is an experimental Fabric mod. It combines physics-based light paths with
explicit gameplay and appearance approximations; it is not a complete simulation
of general relativity. Current documentation reviewed **6 October 2026**.

## Features

| Feature | What you can do |
| --- | --- |
| Mass blocks and black holes | Connected blocks automatically form a source. Compact 2×2×2 and 3×3×3 cubes bend light; a complete 4×4×4 cube reaches the black-hole proxy threshold. Edits recompute the source automatically. |
| World lensing | See native terrain, mobs, block entities, fluids, sky and clouds through curved light paths, including multiple images and horizon views. |
| Accretion disks | Animated golden disks with Doppler asymmetry, drifting granular patches, apparent surface depth and glow. Adjustable size, tilt and brightness. |
| Wormholes | Throw Rift Pearls to place two spherical entrances; see and walk through them. Later throws relocate the oldest entrance. Both entrances and a selected mass can coexist in one view. |
| Relativistic Sight | A two-minute potion adds aberration, Doppler colour and directional brightness while walking. Normal movement speed; simulated optical speed ramps toward 0.99c. |
| Local gravity | Nearby mobs and supported projectiles are pulled toward mass sources; close mobs can lift and be captured. Players and terrain are unaffected. |
| Ambience | Denser, brighter stars; random Nether/End music near black holes; irregular gas haze while skimming a disk. |
| Quality controls | OpenGL renderer, optional experimental RTX acceleration, resolution and Off/Edge/2×/4×/8× AA controls in one menu. |

## Install into your Minecraft instance

### Requirements

| Component | Version / status |
| --- | --- |
| Minecraft | **Java Edition 1.21.1**, not Bedrock |
| Mod loader | **Fabric Loader 0.16.14** is the tested version; the mod requires at least 0.16.14 |
| Fabric API | **0.102.1+1.21.1** is the pinned, tested version |
| Java | **Java 21**; building from source requires a complete JDK 21 |
| Graphics | Ordinary build uses OpenGL. The optional RTX edition supports Windows x64 with a compatible GPU/driver; see below. |

1. Obtain **`interstellar-0.1.0-dev.jar`** (OpenGL) or
   **`interstellar-rtx-0.1.0-dev.jar`** (Windows RTX with OpenGL fallback), or
   [build it from this repository](#build-or-launch-from-source). Use the remapped
   jar in `build/libs`, **not** a sources jar or an unremapped development artifact.
   The `-dev` in the version `0.1.0-dev` is expected. Install only one edition.
2. Install Fabric for **Minecraft 1.21.1** using the
   [official Fabric installer](https://fabricmc.net/use/installer/). Select the
   matching Fabric profile in your launcher. Fabric's
   [installation guide](https://docs.fabricmc.net/players/installing-fabric/)
   explains the launcher-specific steps.
3. Open that profile's **game directory** and create a `mods` folder if needed.
   Put the Interstellar jar and the **Fabric API jar for 1.21.1** in it. Fabric
   Loader and Fabric API are separate components; both are needed. See Fabric's
   [mod installation guide](https://docs.fabricmc.net/players/installing-mods).
4. Start the Fabric profile. Use a new Creative world for your first experiment;
   enable cheats if you want the demo and convenience commands. When using an
   existing world, keep a backup, including if Minecraft offers an experimental
   settings warning for the mod's custom dimensions.
5. Wait for **Preparing world view** to finish, then press **F4**. The Interstellar
   menu is the quickest check that the client loaded the mod.

On Windows, the default Minecraft directory is `%APPDATA%\.minecraft`, but a
launcher profile may use a different one. The correct `mods`, `config`, `saves`
and `screenshots` folders are all relative to **that instance's game directory**.
Install only one Interstellar jar in an instance. No shader pack is required.
Forge/NeoForge, shader packs and alternate rendering mods are not validated here.
The tested gameplay workflow is single-player; this is not a certified multiplayer
or dedicated-server deployment guide.

### OpenGL versus RTX

The normal build has no Vulkan/shaderc dependencies and keeps the OpenGL renderer.
It supports the same gameplay features. The optional backend accelerates geometry
intersection queries; curved light propagation is still computed by the mod.

**The RTX jar installs into the same Fabric `mods` folder.** It bundles the
Vulkan/shaderc libraries and Windows native compiler, and enables RTX preference
without special JVM flags. An existing saved OpenGL preference is respected.
You need a compatible GPU
and driver supporting Vulkan ray queries and Windows Vulkan/OpenGL sharing;
development measurements used an **RTX 5070 Ti**. Other hardware is not certified.
Unsupported or failed backend initialization falls back to OpenGL where possible.

No separate Vulkan SDK or bindings installation is needed; the GPU driver supplies
Vulkan support. You can force OpenGL from F4 at any time, or install the smaller
OpenGL-only edition. See [RTX installation and packaging](docs/rtx-installation.md)
for hardware requirements, build commands and verification limits. Packaging and
native shader compilation are checked headlessly; a full normal-launcher session
has not yet been tested for this new distribution.

## First things to try

### Build a black hole

Find **Mass Block** in Creative → Functional Blocks (or search), or run:

```mcfunction
/give @s interstellar:mass_block 64
```

Place a connected cube: 2×2×2 gives gentle lensing, 3×3×3 increases it, and
4×4×4 forms a black-hole proxy under the default calibration. Nearby sources
are discovered automatically; you do not need to inspect or repeatedly press F10.
Optics render one selected mass cluster at a time. **F4 → Gameplay** controls
mass lensing, entity gravity and horizon capture separately.

For a disk, choose **F4 → Disk → All BHs**, or build a larger compact source:
the default **Auto** threshold is a horizon diameter of 32 blocks (normally
a 7×7×7 mass cube). Disk brightness, size, tilt, animation and glow are adjustable.
The surface-depth effect is shading/parallax on a thin disk, not a gas volume.

### Open a wormhole

Find **Rift Pearl** in Creative → Tools & Utilities, or run:

```mcfunction
/give @s interstellar:wormhole_seed 2
```

Throw one onto a surface well ahead of you, then throw the second elsewhere in
the same dimension. The first entrance is a closed dark core until its partner
exists. Aim at either entrance to see preparation progress. Once both ends are
ready, walk through. **R** resets camera tilt after crossing.

There is at most **one pair per save**. Later throws move the oldest entrance;
**sneak + use** with the pearl closes the pair. A successful placement in another
dimension replaces the old pair with a new first entrance. Player passage is
supported; mob/projectile passage and remote mob tracking are not.

### Try near-light-speed vision

Get **Potion of Relativistic Sight** from Creative → Food & Drinks, brew an
**Awkward Potion + Amethyst Shard**, or run:

```mcfunction
/interstellar relativity potion
```

Drink it and walk in any direction. Optical speed ramps from about **0.1c to
0.99c over 15 seconds**; the HUD shows your current `v/c`. You still move at
normal Minecraft speed. Stopping releases the visual boost, and the potion
expires after **two minutes of game time**. **F4 → Relativity** independently
controls aberration, Doppler colour, brightness, speed cap and ramp duration.

### Visit the prepared demos

Commands require cheats/operator permission:

| Command | Exhibit |
| --- | --- |
| `/interstellar demo gameplay` | Progressive mass, mobs and gravity |
| `/interstellar demo arrows` | Automatic dispensers showing a transient loop, flyby, turnaround and capture |
| `/interstellar demo arrows off` | Stop the arrow course firing |
| `/interstellar demo wormholes` | A ready-made pair of entrances in a separate exhibit |
| `/interstellar demo leave` | Return to your saved position, dimension and game mode |

Exhibits persist in the save; re-entry does not reset your edits. See the
[demo guide](docs/demo-quickstart.md) for viewpoints and additional controls.

## Controls and settings

| Key / menu | Action |
| --- | --- |
| **F4** | Open Interstellar settings |
| **F10** | Toggle world optics; defaults on. Does not disable server-side entity gravity |
| **R** | Reset wormhole camera tilt |
| **Alt+F12** | Switch RTX preference / OpenGL in an RTX-enabled launch |
| **F12** | Start or clear the frame-time measurement |
| **F8 / F9** | Optical sky lab / frozen inspection lab; Esc returns to gameplay |
| **F2** | Minecraft screenshot, saved in the instance's `screenshots` directory |

Rebind mod keys under Minecraft **Options → Controls → Key Binds → Interstellar**.
On some keyboards, function keys also require Fn. F4's six tabs are:

- **Gameplay:** master effects, mass lensing, portal views, status overlay,
  server gravity/capture/strength and camera reset.
- **Graphics:** resolution, AA, fine light paths, smooth lighting, renderer,
  local weather and experimental returning-body images.
- **Disk:** visibility, animation, brightness, outer radius, tilt, threshold and glow.
- **Ambience:** 1×/2×/3× star density, BH music and disk gas veil. Stars are 50%
  brighter than vanilla; music respects Minecraft's Music volume. The gas veil
  fades smoothly within 0.6–9 blocks of the disk plane, with at most 36% opacity.
- **Relativity:** potion visual components and optical speed controls.
- **Tools:** timing, world-lighting rebuild, automatic source selection and labs.

Visual preferences persist in `config/interstellar-terrain.json`,
`interstellar-disk.json`, `interstellar-atmosphere.json` and
`interstellar-relativity.json`. Gravity menu controls affect the server session;
persistent physics settings live in `config/interstellar-gravity.json`.
Use the menu for normal changes. F8 lab settings are separate from gameplay.

## Loading, performance and troubleshooting

- **First launch:** Gradle may download dependencies/assets. Graphics drivers
  also compile shaders, sometimes taking several minutes after shader changes.
  This is separate from world preparation and can make the window unresponsive.
- **World entry:** with effects enabled, the mod prepares the local world view
  during loading, even without Interstellar items. This can take tens of seconds,
  depending on terrain, render distance and hardware. Distant portal views may
  still prepare later while displaying their opening animation. Preparation is
  not merely a one-time Java build cost.
- **Low FPS / low VRAM:** start with **50% resolution, 2× AA, normal light paths**.
  Full resolution traces four times as many pixels; 4×/8× AA adds more rays and
  image storage. Very high settings can cause RTX allocation failure/fallback.
  Render distance above **16 chunks** is unsupported by the current capture path.
- **Nothing bends:** check F4's World effects and relevant feature toggles, and
  allow preparation to finish. Mass source chunks must remain loaded. The optical
  viewing guard is **512 blocks**; this does not force distant chunks to load.
  `/interstellar source auto` clears a manually pinned source.
- **RTX option unavailable:** a normal OpenGL launch cannot enable the absent
  backend just by toggling the menu. Install the RTX edition or use its source launch and inspect
  the status overlay for the renderer actually in use.
- **Block changes appear late:** captures are incremental and queued. If lighting
  remains stale after work completes, F4 → Tools offers Rebuild world lighting.
- **Turning F10 off/on:** disabling releases the world cache; re-enabling needs
  preparation. Use individual feature switches when you want to keep the cache.
- **Report a problem:** include version/commit, active renderer, resolution/AA,
  render distance, reproduction steps, screenshots and `logs/latest.log` from
  that instance. Redact personal/server information before sharing logs.

Performance measurements are scene-specific, not minimum-FPS promises. The latest
disk refinement comparison cost about **0.6 ms (+2.5%)** at its fixed RTX test pose;
see [conditions and evidence](docs/accretion-disk.md).

## Build or launch from source

Install Git and a **complete JDK 21**. From the repository directory containing
`gradlew.bat` and `build.gradle`:

```powershell
# Build the normal installable OpenGL mod (does not launch Minecraft).
.\gradlew.bat clean build
# Output: build\libs\interstellar-0.1.0-dev.jar

# Build the optional installable RTX edition without launching Minecraft:
.\gradlew.bat build packageDemo -PinterstellarRtx
# Output: build\libs\interstellar-rtx-0.1.0-dev.jar

# Development launch, using its own run\ game directory:
.\gradlew.bat runClient
# Optional Windows RTX development launch:
.\gradlew.bat runClient -PinterstellarRtx
```

The Windows shortcuts **Launch Interstellar.cmd** and **Launch Interstellar RTX.cmd**
run those launch commands. Set `JAVA_HOME` to your JDK 21 if necessary. Their
machine-specific fallback path is optional, not a dependency for other users.
On other operating systems, the ordinary Gradle command is `./gradlew`; the RTX
interop/native setup is currently Windows-specific.

Development worlds, config, screenshots and logs are in **`run/`**, separate from
your normal launcher instance. Do not open the same save in two clients.
The source launch uses the stable development player name `InterstellarDev`.

`gradlew.bat packageDemo` creates the ordinary demo ZIP under
`build/distributions`; adding `-PinterstellarRtx` creates a separately named RTX
ZIP. Fabric API, Fabric Loader, Java and Minecraft are not included.
See [development details](docs/development.md).

## Scientific and compatibility limits

Black-hole optics use a nonrotating Schwarzschild model; the disk's moving gas
does not imply a rotating Kerr black hole. Finite source shapes, gameplay forces,
wormhole/exterior matching, assumed colour spectra and cinematic exposure have
explicit approximations. There is no physical terrain destruction, accretion
supply, delayed entity history or exact combined multi-black-hole spacetime.

Interactions still aim along Minecraft's straight ray, even when an image bends.
Rain/snow is a local unbent approximation. Particles and several special graphics
layers remain incomplete. Returning-body images are optional and often only thin
arcs. Read [current coverage](docs/minecraft-coverage.md) before planning a modpack.

## Documentation

- [Documentation index](docs/README.md): current guides versus historical experiments.
- [Illustrated visual guide](docs/visual-guide/interstellar-visual-guide.html):
  graphics basics, light paths, algorithms and optimization discoveries; open locally in a browser.
- [Scientific model](docs/science.md): assumptions, equations and detailed references.
- [Roadmap and deferrals](plan.md), [progress](progress.md), [decisions](decision-log.md).

No project license has been selected. [Third-party notices](THIRD-PARTY.md) apply
to the referenced/copied third-party material; they do not license the whole mod.

## Scientific references

These sources informed the model, validation and visual design. They do not
validate this implementation or imply that every method they describe is used.

1. **Bruneton (2020), [Real-time High-Quality Rendering of Non-Rotating Black Holes](https://arxiv.org/abs/2010.08735).** Exterior disk/star rendering and an optical-table reference. The mod does not currently replace arbitrary terrain tracing with this paper's constant-time scene method.
2. **James, von Tunzelmann, Franklin & Thorne (2015), [Gravitational Lensing by Spinning Black Holes in Astrophysics, and in the Movie Interstellar](https://arxiv.org/abs/1502.03808).** Ray bundles, disk appearance and physical versus cinematic choices. Our current black holes are nonrotating.
3. **Perlick & Tsupko (2021 preprint), [Calculating black hole shadows: Review of analytical studies](https://arxiv.org/abs/2105.07101).** Shadow geometry and observer-dependent analytic benchmarks.
4. **Hamilton, [Journey into a Schwarzschild black hole](https://jila.colorado.edu/~ajsh/courses/insidebh/schw.html).** Falling versus hovering observers and horizon-crossing visualization.
5. **James et al. (2015), [Visualizing Interstellar's Wormhole](https://arxiv.org/abs/1502.03809), and Nakajima & Asada (2012), [Deflection angle of light in an Ellis wormhole geometry](https://arxiv.org/abs/1204.3710).** Spherical wormhole visualization and Ellis light deflection; the mod's shared Minecraft exterior is an additional approximation.
6. **[MIT OpenRelativity](https://github.com/MITGameLab/OpenRelativity) and [Einstein Online: The Doppler effect](https://www.einstein-online.info/en/spotlight/doppler/).** Background for observer-speed effects; Minecraft RGB cannot determine exact infrared/ultraviolet spectra.
7. **M. C. Miller, [Accretion disk lecture](https://pages.astro.umd.edu/~miller/teaching/astr498/lecture12.pdf), and [NASA's black-hole visualization](https://www.nasa.gov/universe/nasa-visualization-shows-a-black-holes-warped-world/).** Disk energetics, thermal appearance and approaching/receding asymmetry. Our procedural gas texture and glow are display choices.

Further sources and the distinction between implemented models and future ideas
are recorded in [science.md](docs/science.md).
