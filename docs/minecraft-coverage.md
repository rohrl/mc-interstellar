# Minecraft feature coverage — 2026-09-24

This describes live F10. F9 remains a frozen diagnostic view. Missing curved graphics
do not disable the corresponding vanilla gameplay mechanics.

## Supported

Native terrain models/textures/lighting, sky, sun/moon/stars, clouds, water/lava,
stained glass, ordinary mob/player/item geometry and ordinary block entities such
as beds and chests. Mobs animate and move; block edits, chunk streaming and mass-block
placement/removal/splits/merges refresh automatically.

First-person hands, offhand and item-use animation use Minecraft's foreground pass,
after the lensed world. F1 hides hands/HUD without disabling lensing. Native water/fire/
inside-block overlays and later screen effects retain their normal rendering order.
World bobbing/hurt/nausea transforms are not fully reproduced in the lensing camera.

| Added feature | Implementation and practical limits |
| --- | --- |
| Emissive eyes/materials, enchantment glint | Native textures and glint UV animation, with unlit/additive blending. Coplanar layers use a small geometry offset; arbitrary shader-pack effects remain unsupported. |
| Entity shadows | Minecraft's native nearby blob shadows are included in curved geometry. These are not ray-traced shadows cast by the black hole. |
| Text | Ordinary sign text and visible name tags use the native font atlas. See-through name labels remain omitted. |
| Selection/mining | Selection shape edges and native damage textures follow the curved scene. The selected target still comes from Minecraft's straight interaction ray. |
| Glowing status outlines | A separate curved mask of glowing model geometry supplies team-coloured outlines, including through terrain. Only runs while glowing geometry is present. Adds a ray pass; thin secondary outlines share the scene's resolution limits. |
| Rain/snow | Small native foreground pass within three columns of the camera, respecting biome/roof checks. Unbent approximation, using straight-world depth; not weather transported along curved rays. Session switch: `/interstellar-visuals weather false` (or `true`). |
| Returning player image | Off by default after full-resolution/zoom tests yielded only stretched strips. Opt in with `/interstellar-visuals body true`. Actual model/skin, current pose, no emission history. [Study](horizon-body-study.md). |
| Fishing lines/leashes | Native geometry is visible in the curved scene; the local gravity field adds bounded sag with endpoints pinned. No rope mass, collision or tension solver. |

## Remaining graphics gaps

Particles (smoke, flames, potion/explosion effects), beacon beams, portal surfaces,
entity hurt/flash overlays, and arbitrary special model/shader layers remain incomplete
or omitted. Transparent blending has a finite layer budget and imperfect coplanar
surfaces/boat water masks; water is not physically refracted. Thin secondary images
and subpixel edges are limited by trace resolution and two-ray AA.

Shader packs, alternate renderers and arbitrary resource packs are not broadly
validated. Rendering order is not exhaustive certification of every status effect.

## Gameplay and operating limits

| Area | Current limitation |
| --- | --- |
| Aiming/interacting | Straight collision/interaction rays: the visually bent position can differ from the actual target. |
| Entity gravity | Mobs, ordinary/spectral arrows, tridents, vanilla thrown entities and free fishing bobbers participate. Players, mounted/passenger groups, homing projectiles, dropped items, boats and minecarts remain outside this implementation. Loyalty, fishing/reeling and leash attachments keep native rules. |
| Motion realism | Bounded Newtonian gameplay force with native gravity/drag/collisions, not relativistic massive-particle geodesics. Red/dim approach cue is stylized; no delayed light or apparent horizon freezing. |
| Source limits | One selected spherical optical source. No combined multi-hole spacetime. Physics supports enclosing radius up to16 blocks, horizon radius up to12 and force reach at most64. Visual tether sag uses the selected source. |
| Terrain/horizon | F10 stays active through the horizon with a falling optical frame and straight-aim interior block editing. Central background cutoff at0.1 r_s. No physical terrain infall/destruction, absorbed-mass growth or accretion disk. [Assumptions](horizon-body-study.md). |
| Range/updates | 256-block camera range, finite geometry budgets and queued terrain updates. Initial preparation can take tens of seconds in natural worlds. Teleporting is not a v1 support target. |

The legacy calibration exhibit disables entity gravity; gameplay/arrow exhibits use it.
F10 controls optics independently of server forces. See [feature evidence](world-features.md),
[materials](material-coverage.md) and [gameplay gravity](gameplay-gravity.md).
