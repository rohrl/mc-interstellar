# Wormhole demo implementation — 28 September 2026

## Status

The reference mathematics, two distant scenes, native chunk/light delivery and
server-authoritative player crossing are implemented. The wormhole light-ray
shader is **not implemented yet**. Current screenshots show ordinary Minecraft
views of the exhibits and transported camera orientation; they do not demonstrate
wormhole optics or a seamless rendered transition. The full overnight goal remains
active until those visuals and playable performance are verified.

## Geometry and world mapping

The reference is the ultrastatic Ellis metric, documented in
[the implementation plan](overnight-goals-2026-09-28.md). Areal throat radius is16;
the isotropic-coordinate mouth radius is8 blocks. Mouth centres are
`(8,96,8)` and `(1032,112,520)`, about1145 blocks apart. The new
`interstellar:wormholes` void dimension contains19788 placed blocks. Orange grass
and cyan sandstone exhibits, asymmetric landmarks, foreground pillars, stained
glass and striped walls provide distinct finite-distance objects for optical checks.

`/interstellar demo wormholes` builds this separate exhibit once and preserves the
owner's original dimension, position, game mode and flying state. Named viewpoints:
`/interstellar demo view mouth_a|mouth_b|throat_a|throat_b`. Use
`/interstellar demo wormholes status` for native-region readiness and
`/interstellar demo leave` to return. Physical crossing is currently automatic in
this dimension; optical rendering and its F10 integration are the next work.

## Bounded distant-world delivery

`WormholePair` defines two11×11 chunk regions. All scene blocks fit within three
chunks of their centre; two extra rings provide neighbours, native light and arrival
space. These242 retained chunks are independent of Minecraft's camera-centred
render distance. Ordinary dimensions use their existing behaviour.

`WormholeChunks` adds at most two native tickets per server tick and sends at most
two complete native chunk/light packets per tick, with a2ms send-loop budget.
Ticket propagation loads chunks asynchronously; the send loop uses the nonblocking
`ServerChunkManager.getWorldChunk(x,z)` lookup. A misleadingly named alternative,
`getChunkFutureSyncOnMainThread`, actually pumps tasks until its future completes
when invoked on the server thread, so it is deliberately avoided. A single packet
serialization can exceed the2ms slice; there is no hard real-time guarantee.

The client retains these regions in a small map owned by its `ClientChunkManager`.
Vanilla's ring buffer cannot safely hold two distant areas. Chunk and biome packet
loads use native `WorldChunk` parsing, while designated unload packets retain both
geometry data and lighting. Dimension exit releases the world/cache; server tickets
are removed when the exhibit has no players. Server block/light callbacks coalesce
bounded dirty chunk keys and resend changed chunks. No per-tick block-array scan is
introduced. Full-chunk updates are a v1 simplicity tradeoff, bounded by the send queue.

An ordered acknowledgement follows the initial packets. The client acknowledges
only after its native deferred lighting queue has applied the preceding packets and
all242 chunks exist. This is **native data readiness**, not GPU mesh readiness.
`StreamingTerrain` retains the union of both regions and the camera window, without
publishing missing designated chunks as ready empty geometry. Its wormhole rendering
path has not yet been exercised because the optical shader is still pending.

## Player crossing

`WormholeTravel` changes charts when the eye crosses coordinate radius8, once native
destination data is ready. It applies the tested spherical inversion and z reflection
to the eye position; the differential transports the viewing direction and up axis.
The result is converted to native yaw/pitch plus an explicit roll angle. An off-axis
crossing therefore does not silently snap the player's up axis back to vertical.

A small custom message precedes Minecraft's authoritative teleport. The client
transports its actual creative-flight velocity before vanilla clears it, then applies
it after the matching position packet. Previous-frame positions are moved into the
new chart to avoid interpolating through the1145-block map gap. Camera rotation
includes the transported roll. Shader camera bases still need to consume that roll.
Named viewpoints reset it; returning to an ordinary dimension clears it.

These are prescribed Minecraft controls in the chosen geometry, not free-fall body
dynamics. Riding entities are excluded. General entity/projectile traversal,
arbitrary high-speed teleports and persistent roll across reconnects are not provided
by this foundation. Mouse/flight controls still use Minecraft's yaw/pitch convention;
camera roll is retained rather than replacing the full input system with a flight sim.

## Verification so far

Evidence: [runtime events](profiles/2026-09-28-wormhole-foundation/runtime-events.txt).
All runtime actions used the isolated `Interstellar Overnight Check 2026-09-28` save.

- New scene construction took about13 seconds; both native regions became ready
  about6 seconds after entry, including on re-entry. These are log timestamps,
  not a comprehensive loading benchmark.
- While at A, changed the distant B sample from sandstone to glowstone. Before
  visiting B, the client reported the new block and block light14 above it, sky15.
  Repeated from B to A after vanilla would have unloaded A. Both changes arrived;
  restored both original blocks afterwards.
- Exiting released all242 tickets. Re-entering created a new acknowledged generation
  and recovered the original blocks with normal light. Both ordinary exhibit views
  were visually inspected.
- Actual held W/S input crossed A→B and B→A. Server and client positions agree;
  client forward/backward velocities remained nonzero after transfer.
- Off-centre input crossed both ways too. First exit: yaw−48.63°, pitch−26.58°,
  roll−12.18°. The client retained the vertical component of velocity. The
  [ordinary-world screenshot](profiles/2026-09-28-wormhole-foundation/oblique-camera.png)
  confirms the camera roll. The return used a different controlled path, so it is
  not a claim of an exactly retraced geodesic or zero accumulated rotation.
- Leaving restored the original saved position and mode. Existing original worlds
  were not used for these edits; owner options were restored after normal shutdown.

The seventh Ellis test evolves the same ray from both isotropic charts, on both
sides of the throat, and checks their positions remain related by the transition.
Final optional and clean ordinary builds each pass88 tests; the ordinary jar contains
no optional RTX backend or Vulkan/shaderc classes. Two small guards
(rejecting teleport-sized velocity estimates and singular previous-position
interpolation) were added after the runtime crossing test and covered by compilation;
the ordinary measured crossings do not take those exceptional branches.

## Next acceptance work

1. Shared GL/RTX Ellis ray integration and model selection, splitting ray segments
   exactly at the throat; never query a segment across the map gap.
2. Finite-distance geometry, transparent materials, correct sky direction and fog
   that does not count the physical map separation. Capture native clouds and block
   entities for both ends, with independent placement and no duplicates.
3. F10/HUD and frozen comparisons without synthetic mass-block payloads. Feed the
   transported camera basis into optics. Keep data/mesh preparation visible.
4. Independent optical reference checks, fixed-pose GL/RTX comparisons, rendered
   bidirectional crossings (including oblique entries), finite parallax/occlusion,
   and measured playable frame rates. Only then is Goal2 complete.
