# Live settings and placed wormholes

## F4 settings

Press **F4** during gameplay. Rebind it in Controls → Interstellar. The menu
applies changes live and saves visual preferences in `config/interstellar-terrain.json`.
It stays live while open; **Done** or **Esc** returns to play.

| Control | What it changes |
|---|---|
| Resolution | 50% or 100% of each framebuffer dimension. Full resolution traces four times as many pixels. |
| Antialiasing | Off: one centred ray. Edge: one ray plus edge filtering. 2× / 4× / 8×: that many distinct rays plus sharp reconstruction. |
| Smooth lighting | Minecraft's ambient occlusion, persisted in Minecraft options. Recaptures terrain lighting. |
| Fine light paths | Smaller curved-path steps; more work near strong bending. |
| Renderer | Prefer RTX with automatic OpenGL fallback, or force OpenGL. Disabled in a normal, Vulkan-free launch. |
| Live lensing | Same session control as F10. Tracks sources and resumes when their data is ready. |
| Weather | Approximate local foreground rain/snow. |
| Returning body | Experimental returning images of the player's real body; defaults off. |
| Reset camera upright | Same as R: clear wormhole tilt without changing position or aim. |
| Quality defaults | 50% resolution, 2× AA, normal path steps. |

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
3. The first placement shows an opaque dark sphere with a cyan rim. Aim at it for
   “Wormhole end · Closed” and a reminder to throw a second pearl elsewhere.
4. The second connects the pair and automatically enables nearby optical rendering.
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

The unpaired sphere is a visual placeholder: no black-hole gravity, light bending
or teleportation. A fixed sphere mesh renders against Minecraft's normal depth
buffer; its dark surface has a narrow cyan rim. This replaces repeated server
particle packets and does not start remote chunk preparation or an optical capture.
If a nearby mass already has live optics, the same mesh joins the existing entity
capture and therefore participates in that view's GL/RTX ray queries.

The crosshair intersects the mouth sphere out to 96 blocks. A native block raycast
suppresses the label when the entrance surface is hidden behind solid terrain.
The label distinguishes Closed, Preparing destination and Connected. It disappears
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
budget. Travel waits for the native chunk/light queue acknowledgement. Reconnect
and respawn invalidate the viewer so a fresh client cannot inherit stale readiness.

Retired client chunks outside the local viewing region are released along with
their lighting and block entities. Nearby chunks remain usable until vanilla
refreshes or unloads them; their initial packets bypassed vanilla's moving cache.
Dropping them immediately would leave holes around a closed mouth.

This generalizes the existing Ellis renderer and player transfer; it does not
change the geodesic equations. The current model uses the nearest mouth's chart,
not a global spacetime solution for two nearby mouths and other gravity sources.
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
