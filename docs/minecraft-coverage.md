# Minecraft feature coverage — 2026-09-24

This describes the live F10 renderer. F9 is intentionally a frozen diagnostic view.
Missing curved graphics do not disable the corresponding vanilla gameplay mechanics.

## Supported

Native terrain models/textures/lighting, sky, sun/moon/stars, clouds, water/lava,
stained glass, ordinary mob/player/item geometry and ordinary block entities such
as beds and chests. Mobs animate and move; block edits and ordinary chunk streaming
refresh automatically. Source placement/removal/splits/merges update automatically.

First-person hands and held items use Minecraft's ordinary foreground pass, including
offhand and item-use animation. The live world composite now runs immediately after
world rendering and before that pass. HUD and menus remain native; F1 hides hands
and HUD without disabling lensing. This is separate from rendering returning images
of the player's body, which remains deferred.

## Graphics gaps

| Feature | Current limitation |
| --- | --- |
| Rain/snow | Precipitation geometry is not captured into the curved scene. |
| Particles | Smoke, flame particles, potion particles, explosions, etc. are not captured. Emissive block textures such as lava are a separate supported feature. |
| Special entity layers | Enchantment glint, additive/glowing eyes, entity blob shadows and some special model layers are omitted from the curved scene. Ordinary held-item glint remains native in the foreground. |
| Glowing outlines | The vanilla screen-space silhouette would be at the wrong, unbent position, so it is suppressed only when a lensed world image was actually drawn. Normal fallback rendering retains it. |
| Text and special block entities | Sign text, name tags, beacon beams, portal surfaces and other unsupported shader layers are incomplete/omitted. The supporting block/model may still render. |
| Interaction overlays | Block selection outlines and mining-crack overlays are not captured into the curved scene. Interaction still works. |
| Transparent/special surfaces | Native surface blending is approximate: finite layer budget, imperfect coplanar overlays and boat water masks, no physical water refraction. |
| First-person body images | The camera player's body is excluded in first person; no returning-light image of your actual body yet. Ordinary third-person player geometry is captured. |
| Fine optical detail | Finite trace resolution and two-ray AA limit thin secondary images and subpixel edges. |

First-person water/fire/inside-block overlays now run after lensing through the
normal Minecraft path, as do later screen post-process effects. This restores their
rendering order; it is not an exhaustive visual certification of every status effect.
World-view bobbing/hurt/nausea transforms are not fully reproduced by the current
perspective-ray extraction, which retains FOV and projection offsets only.

## Gameplay and operating limits

| Area | Current limitation |
| --- | --- |
| Aiming/interacting | Minecraft uses straight collision/interaction rays. A visually bent block/entity can differ from what the crosshair actually targets. |
| Entity gravity | Mobs, ordinary/spectral arrows and vanilla thrown entities participate. Players, mounted/passenger groups, tridents, fishing lines and separately controlled/homing projectiles are excluded. Dropped items/boats/minecarts are not pulled by this mob/projectile implementation. |
| Motion realism | Bounded Newtonian gameplay force, with native gravity/drag/collisions; not full relativistic massive-particle geodesics. The mob red/dim cue is stylized, with no delayed-light history or apparent horizon freezing. |
| Source limits | Optics use one selected spherical source approximation. No combined multi-hole spacetime. Entity physics supports enclosing radius up to16 blocks and horizon radius up to12, with force reach at most64 blocks. |
| Terrain/horizon | No terrain destruction, absorbed-mass growth, physical accretion disk or terrain horizon crossing. Live optics pause inside the exterior camera limit and resume outside it. |
| Range/updates | 256-block camera range from the source, finite geometry budgets and queued terrain updates. Initial preparation can take tens of seconds in natural worlds. Fast travel/teleports are not a v1 support target. |
| Other renderers | Shader packs, alternate rendering mods and arbitrary special resource-pack models are not broadly validated. Do not assume compatibility. |

The original calibration exhibit deliberately disables entity gravity. The gameplay
and arrow exhibits use it. F10 controls optics independently of the server-side force.

## Verification of the first-person fix

See the current checkpoint in [progress.md](../progress.md) for checks and measured
timings. Optical integration, scene sampling and AA settings are unchanged by this fix.
Detailed prior evidence: [materials](material-coverage.md), [gameplay gravity](gameplay-gravity.md).
