# Live settings and placed wormholes

## F4 settings

Press **F4** during gameplay. Rebind it in Controls → Interstellar. The menu
applies changes live and saves visual preferences in `config/interstellar-terrain.json`.
It stays live while open; **Done** or **Esc** returns to play.

The tabs are **Gameplay**, **Graphics**, **Relativity**, and **Tools**. World effects start
automatically when a nearby mass cluster or placed mouth is available. Mass
lensing and the wormhole pair share one world view; neither replaces the other.
The status overlay names the active mass and portal state.

| Control | What it changes |
|---|---|
| Resolution | 50% or 100% of each framebuffer dimension. Full resolution traces four times as many pixels. |
| Antialiasing | Off: one centred ray. Edge: one ray plus edge filtering. 2× / 4× / 8×: that many distinct rays plus sharp reconstruction. |
| Smooth lighting | Minecraft's ambient occlusion, persisted in Minecraft options. Recaptures terrain lighting. |
| Fine light paths | Smaller curved-path steps; more work near strong bending. |
| Renderer | Prefer RTX with automatic OpenGL fallback, or force OpenGL. Disabled in a normal, Vulkan-free launch. |
| World effects | Saved master visual switch, also controlled by F10. Defaults on. Does not change server gravity. |
| Mass lensing | Render the automatically selected mass cluster, together with the portals if enabled. |
| Portal views | Render and prepare wormhole passages. Off keeps mouth markers and suspends your transit; it does not delete the pair. |
| Status overlay | Show effects, source, preparation and renderer status. |
| Entity gravity / Horizon capture | Server-session controls for attraction and consuming entities. Require operator permission. |
| Gravity strength | Cycle 25%, 50%, 100% of the strong-gravity default. Changes gameplay forces, not optical mass. Server-session setting. |
| Weather | Approximate local foreground rain/snow. |
| Returning body | Experimental returning images of the player's real body; defaults off. |
| Reset camera upright | Same as R: clear wormhole tilt without changing position or aim. |
| Quality defaults | 50% resolution, 2× AA, normal path steps. |
| Tools | FPS measurement, world-lighting rebuild, automatic mass selection, upright reset, and the F8/F9 laboratories. |
| Relativity | Potion visuals, independent aberration / Doppler colour / brightness controls, optical speed cap and sprint ramp. See [Relativistic Sight](relativistic-sight.md). |

Esc from a laboratory restores the saved gameplay visual setting. F10 switches
all world optics together; use the two feature switches in F4 for independent
control. Turning the master off releases the terrain cache, so turning it back on
needs preparation. Changing only the selected mass or individual effects reuses
an existing cache. A lighting rebuild deliberately recaptures it.

The current renderer handles **one selected mass cluster plus one portal pair**.
Overlapping fields compose spatial ray curvature as a gameplay approximation,
not an exact multi-object spacetime; see [the science notes](science.md).
Server gravity and portal placement remain independent of the graphics backend.

All five AA modes work on RTX. Off/Edge dispatch only one sample. The sample atlas
has two columns: one row for2×, two for4×, four for8×. Each invocation traces one
ray, with its own material mask, and the float resolve averages only active samples.
2× keeps its diagonal offsets,4× uses a2×2 grid, and8× uses eight balanced subpixel
positions. GL and RTX share the pattern. Extra image storage is allocated only for
the selected mode; the default remains2×. Compared with2×,4× traces twice as many
rays and8× four times as many; frame cost also includes fixed rendering work.

Changing resolution or the4×/8× atlas size reinitializes the RTX targets/backend;
lighting changes recapture terrain. Off/Edge/2× share the same target size. The
independent four-sample diagnostic reference explicitly stays on OpenGL.

An eligibility bug found in the item test also kept completely opaque scenes on
OpenGL. RTX no longer requires a scene to contain transparent/special materials.
The normal artifact still excludes the optional Vulkan/backend classes.
An RTX-enabled launch defaults to preferring RTX for connected wormholes too;
only an explicit saved OpenGL preference or unavailable backend selects fallback.

## Rift Pearl (formerly Wormhole Seed)

Find **Rift Pearl** in Creative → Tools & Utilities (or search), or use
`/give @s interstellar:wormhole_seed`. Craft one with a Heart of the Sea surrounded
by eight Ender Pearls. The item is reusable, with a one-second throw cooldown.

1. **Use/right-click** throws a seed in your aiming direction.
2. It follows a ballistic path and opens a mouth after hitting a block surface.
   The centre sits nine blocks out from that surface; the mouth radius is eight.
3. The first placement grows a small closed core. Aim at it for
   “Wormhole end · Closed” and a reminder to throw a second pearl elsewhere.
4. The second starts opening both ends. The cores grow with preparation progress;
   the aiming hint shows a percentage. Travel starts when the passage is visible.
5. Every subsequent successful throw keeps the newer mouth and relocates the oldest.
6. **Sneak + use** closes the pair, allowing a fresh start, including another dimension.

There is one pair per saved game/server, shared by players and item copies. Both
mouths must be in the same dimension. The pair and its age order survive reloads.
The demo command installs the exhibit as this same global pair; it does not create
an additional wormhole. Using the seed on an existing demo pair relocates its oldest
mouth under the same rules. A successful throw in another dimension closes the
old pair and starts a new, closed first mouth there. Validation runs first: rejected
throws leave the existing pair intact. The placement message explains the move.

The item keeps its original `interstellar:wormhole_seed` registry ID so existing
stacks and recipes continue working. Its custom 64×64 alpha sprite is used in the
inventory, in hand and in flight. [Artwork source and prompt](artwork/README.md).

### Closed appearance and aiming hint

The first end grows to a small closed core (radius 0.8 blocks). Local terrain
capture starts automatically: until it is available, a native dark sphere with a
cyan rim renders against Minecraft's depth buffer. Once captured, the existing
Schwarzschild renderer supplies real light bending and a black shadow. This is
an opening effect, with no server gravity, terrain destruction or teleportation.
A lone end does not request remote chunks.

With both ends placed, the closed core grows toward the eight-block mouth size.
Progress combines chunk delivery (20%) and geometry capture (80%), reserving the
last few percent for an optical image. It is monotonic and smoothed; it is a work
estimate, not a prediction of remaining time. It stays below 100% until both native
regions, their geometry and the passage image are ready. Previously unloaded
chunk placeholders do not count as destination geometry.

The renderer reuses its local mesh when a second end is placed or the pair moves.
BH lensing continues during remote preparation. The nearest mouth supplies the
BH optics; the other closed end uses the sphere marker if also visible. This
preserves the existing nearest-mouth approximation, rather than introducing a
two-black-hole spacetime solver. The preview uses r_s = core radius / (1.5 sqrt(3))
to roughly match its far-field shadow to the intended opening size.

The last BH frame fades over the first wormhole frames in 0.35 seconds. Only one
optical scene is traced per frame; one extra framebuffer exists during the fade.
The actual geodesic equations are unchanged. The radius animation and crossfade
are gameplay presentation, not a physical black-hole-to-wormhole transformation.
GL and optional RTX share the same lifecycle; the ordinary artifact remains
Vulkan-free.

The crosshair intersects the mouth sphere out to 96 blocks. A native block raycast
suppresses the label when the entrance surface is hidden behind solid terrain.
The label distinguishes Closed, Opening N% and Connected. It disappears
when looking away or opening a screen. F12 can time the native closed-sphere draw
when no live optical renderer is running; this uses the existing asynchronous GPU
timer, with no profiling work until requested.

Aim well away from yourself. A placement is rejected if its centre is within
12 blocks of any player, within 20 blocks of the remaining mouth, outside world
bounds, in unloaded terrain, or lacks clear space inside the sphere. Rejections
preserve both existing mouths. Seeds expire after four seconds without landing;
misses consume nothing. Placement never removes terrain. The clearance check
examines at most 17³ block positions and never generates chunks synchronously.

Travel also checks the player's full bounding box at the destination. Near the
lower rim, the chart mapping can put feet below a nearby floor even when the eye
has a clear path. An obstructed exit keeps the player at the last clear entrance
position and shows an action-bar hint, without changing orientation. This check
runs only on a crossing attempt, not for every rendered ray.

## State, chunk lifetime and limits

The server stores an immutable layout in an overworld `PersistentState`, and
sends its revision before destination chunks. Client and integrated-server copies
are separate. Each revision invalidates old readiness acknowledgements and camera
transport state. Frozen wormhole inspection also closes on a layout change.

Relocation releases tickets using the **old** layout, then prepares the new one.
Chunk coordinates are deduplicated where regions overlap, including negative
coordinates. Delivery remains capped at two packets/tick and a two-millisecond
budget. Travel waits for the chunk/light barrier, captured destination geometry,
and the client's completed visual reveal acknowledgement. Reconnect
and respawn invalidate the viewer so a fresh client cannot inherit stale readiness.

If the player is already inside when a mouth opens, they must step outside before
entering. Admin teleporting inside also does not count as entry. This prevents
growth or a newly completed capture from unexpectedly moving the player.
**R** is the dedicated camera-upright key (rebindable in Controls → Interstellar),
independent of the F4 menu. It clears wormhole roll while preserving position and aim.

Retired client chunks outside the local viewing region are released along with
their lighting and block entities. Nearby chunks remain usable until vanilla
refreshes or unloads them; their initial packets bypassed vanilla's moving cache.
Dropping them immediately would leave holes around a closed mouth.

Connected mouths now share one continuous exterior. Either entrance can appear
in the same view; switching which entrance is nearer no longer deletes half the
terrain. Rays bend inside two finite regions, with Ellis spatial paths near each
throat and a smooth transition to straight rays farther away. This is an engineered
optical metric, not the exact isolated Ellis spacetime; see [the model](science.md#two-mouths-in-a-continuous-minecraft-exterior-2026-09-29).
Player transfer keeps the same throat mapping.

Repeated views through the pair are bounded to four throat passages per ray.
The renderer follows that ray through the existing scene rather than rendering a
new full image for every nested view. Rays still circulating at the limit return
dark; very small higher-order images can therefore end in darkness.

Opening progress measures capture of the fixed regions around the two mouths.
Missing destination chunks take priority over ordinary camera-window updates;
walking around cannot keep adding work to the opening requirement. Other nearby
terrain continues streaming after opening. Cold startup still needs local geometry
and shader/backend preparation, and percentages are work counts, not time estimates.
Remote capture remains five chunks around each mouth. Arbitrary remote landscapes
can expose that capture boundary. Remote mob tracking, remote block interaction,
vehicles and mob/projectile transit remain outside this version. Optics activate
near the pair, with the ordinary source/normal-view path outside that range.

## Verification — 2026-09-29

Both build flavours pass 88 tests. Normal jar checked for absence of Vulkan,
shaderc and optional backend classes. In-game shader compilation, F4 controls,
lighting recapture, renderer selection, reset and settings persistence checked.

Actual throws in a copied save created mouths in `interstellar:gameplay`, outside
the fixed exhibit. Second, third and fourth valid throws connected/replaced the
expected ends. Close-player and obstructed-volume throws kept the layout unchanged.
Walked across both ways; the retired mouth's old position no longer transported
the player. A full client restart restored the coordinates and age order.
Overlapping regions loaded correctly: 176 then 143 unique chunks, versus 242 for
the separated demo. This was a bounded void-world course, not a survey of biomes.

The lower-rim collision regression reproduced feet at Y100.39 inside a floor
whose top was Y101. With the guard, the player stayed at entrance Y101; clear
centre crossings then passed in both directions. The owner's placed pair and
quality preferences were restored after testing.

1280×720 output, same-pose GL/RTX RGB mean absolute error on the 0–255 scale:

| Check | MAE |
|---|---:|
| AA Off, 50% | 0.00193 |
| Edge AA, 50% | 0.00240 |
| 2× AA, 100%, fine paths, final | 0.00304 |
| 4× AA, 100%, fine paths | 0.00326 |
| 8× AA, 100%, fine paths | 0.00293 |
| Item-created opaque scene, 2× AA, 50% | 0.00776 |

At full resolution with fine paths in the demo view:

| AA | RTX GPU median | Frame median | Approximate FPS |
|---|---:|---:|---:|
| 2× | 15.05 ms | 15.95 ms | 63 |
| 4× | 25.96 ms | 26.45 ms | 38 |
| 8× | 47.46 ms | 48.36 ms | 21 |

Before adding 4×/8×, the same 2× setup measured 14.91 ms GPU / 15.78 ms frame;
the final result is within about 1%. 8× is expensive and remains optional.
The opaque course's RTX GPU median was 2.74 ms; frame intervals were 8.32 ms at
the 120 FPS cap. Default-quality OpenGL fallback was 18.39 ms GPU / 19.10 ms frame
at the checked oblique demo pose. These are scene-specific checks, not an uncapped
universal FPS claim or a broad before/after regression study.

Evidence and screenshots: [settings/portal checks](profiles/2026-09-29-settings-portals).

Follow-up: [Rift Pearl UX acceptance](profiles/2026-09-29-rift-pearl) checks the empty
first-end flow, dimension move, closed-end reload, depth/label occlusion, custom
sprite and automatic RTX connection. Native closed-sphere draw GPU p50 was
0.00934ms at1280×720; connected GL/RTX MAE was0.00206/255. The original per-save
item ID and the owner's existing saves/settings were preserved.
