# Gameplay gravity — implementation and verification

Initially implemented on `codex/gameplay-gravity`; range, arrow-course and visual follow-ups are on `codex/gameplay-arrow-exhibit`. The owner accepted the proposal and permits a small performance cost. The checks below establish sampled numerical and runtime behaviour, not complete physical accuracy or a worst-case performance guarantee.

## Behaviour and controls

- Face-connected blocks share a source, with exact equal-weight centre and corner-inclusive enclosing radius. Normal calibration is `r_s/block = sqrt(3)/32`; complete 1/2/3/4 cubes have compactness 0.0625/0.25/0.5625/1. The 4-cube is the reference horizon threshold. Shape still matters.
- Nearby sources are discovered automatically. Placement/removal queues shared incremental component scans; merge, split, anchor removal and chunk changes invalidate the affected components. No per-block tickers, forced chunk loading or duplicate scans per viewer.
- F10 arms/disarms the optical view even before a source is found. Complete updates reuse the captured world view. `/interstellar inspect <pos>` optionally pins a component; `/interstellar source auto` releases the pin; `/interstellar source status` reports tracking state.
- `/interstellar demo gameplay` enters a separate exhibit with the new calibration and active entity gravity. The existing `/interstellar demo enter` exhibit keeps the old calibration and passive mobs. View/leave commands work for both; moving between exhibits preserves the original return record.
- `config/interstellar-gravity.json` has `enabled`, `capture` and `strengthPerBlock` (default 0.05, range 0–0.2). Restart to reload it. Invalid settings disable entity gravity without overwriting the file. F10 controls optics, independently of server physics.
- Operator controls `/interstellar gravity enabled true|false` and `/interstellar gravity capture true|false` override this server session; `status` reports settings/size limits. `/interstellar gravity profile start|stop` enables otherwise inactive CPU timing counters.

## Bounded discovery

The server shares a component index across viewers. Edits debounce for three server ticks; completion latency also depends on queued work. Probes share 256 cell reads per server tick, round-robin across dimensions. Each probe has a 4096-block limit. A stale optical snapshot lasts at most 20 ticks; dirty/incomplete sources immediately stop contributing an entity field.

Chunk discovery checks palettes before scanning block cells. Discovery shares up to 32 section checks and 1024 block reads per server tick across worlds with players; chunks and vertical sections are interleaved near player height. This avoids waiting behind every empty section in the render distance. Inactive dimensions retain discovery hints until visited or unloaded. Load hints must survive the interval before a chunk becomes accessible. Complete source dependencies include adjacent chunks, so unload/reload rechecks classification without treating a partial cluster as a smaller black hole.

Automatic selection prefers the current component until another source is substantially stronger locally. Rendering and motion use a dominant spherical source, not a general multi-hole metric. Overlapping-source dynamics are not a validated superposition.

## Extended optical interior

The established Schwarzschild exterior remains unchanged. The new programs support a finite static interior; black-hole programs do not carry that interior solver. Extended sources keep their ordinary opaque block surfaces and have no artificial capture horizon.

Use areal radius, `C=r_s/R`, `x=r/R`, and metric

`ds² = -A dt² + dr²/B + r² dOmega²`.

Outside `R`, `A=B=1-r_s/r`. Inside, choose

`A=(1-C)² / [1 - C(1+x²)/2]`, `B=1-C*x²`.

For `0<C<1`, the centre is finite, `B(0)=1`, and the lapse is positive. Both metric values and the lapse derivative match the exterior at the surface. The radial derivative of `B` has the jump associated with the chosen density boundary; no smooth-density claim is made. The weak-field lapse agrees to first order with the uniform-sphere Newtonian potential. This is a chosen spherical interior approximation, not a stellar equation of state, a stability calculation or a solution for a nonspherical block assembly.

For `u=r_s/r`, `e²=r_s²/b²`, the null first integral is `u'²=e²*B/A-u²*B`. Outside, retain `u''=1.5u²-u`. Inside, with `t=C³/u²`, use

`u''=e²*t*(3-C-2t)/(2*u*(1-C)²)-u`.

Static local-ray initialization uses `u'=-mu*u*sqrt(B)/sqrt(1-mu²)` and `e²=u²*A/(1-mu²)`. Adaptive chord curvature uses `u³*abs(u+u'')/[r_s*(u²+u'²)^(3/2)]`. Native scene intersection and AA remain active. This model does not add physical spectral/time transport or emission histories.

The independent reference integrates the affine Cartesian Hamiltonian `H=(-E²/A+B*p_r²+p_t²)/2`, not the shader's inverse-radius equation. Unit comparisons cover 30 trajectories starting inside and outside the source for the 1/2/3-cube compactnesses, plus boundary/centre checks through `C=0.999`. These are sampled mathematical checks, not certification of every GLSL ray or near-critical trajectory.

## Entity dynamics and capture

Motion is explicitly scaled Newtonian gameplay dynamics, separate from the optical metric. Exterior acceleration is `-0.05*N*rVector/r³` in blocks/tick², with a uniform finite interior and acceleration capped at 0.35 blocks/tick². On 2026-09-24 the owner requested twice the influence radius: reach is now approximately 14.1/26.0/40 blocks for the 8/27/64 reference builds. The close-range force is unchanged; the outer 40% still uses a quintic taper to zero. Real gravity has no such cutoff.

Local entity dynamics currently support sources with enclosing radius at most 16 blocks and horizon radius at most 12; the outer reach is capped at 64. Large-source optics and the 4096-block metadata limit are separate from this gameplay limit. No horizon is silently shrunk to fit the force range. The HUD and gravity status command expose the limit.

Mobs keep native AI, contact, friction and drag. A physical-movement hook adds the source acceleration once per world tick, including flying/swimming movement overrides. Close pull can exceed the ordinary downward acceleration and lift mobs. Players and mounted/passenger groups are excluded. The radial force preserves sideways motion rather than steering a heading at the source.

Ordinary/spectral arrows and vanilla thrown entities use 2–16 bounded movement substeps near an influencing source. Native hit handling is reused per segment; full velocity remains available to damage/deflection, lifecycle tick runs once, drag is distributed over the substeps, and ordinary gravity is retained. Fast paths check the full swept interval when selecting a source. Tridents, fishing lines and separately controlled/homing projectiles are outside this first integration. Outside a field the vanilla tick remains intact.

Capture removes crossing mobs/projectiles through normal entity removal, without terrain damage, extra mass or a loot cascade. Projectile collision queries stop at the horizon, so a target behind it cannot be damaged first. Mobs use their swept centre and contact of their bounding box with the horizon, avoiding bodies stranded on solid source corners. General retarded/frozen horizon images remain deferred.

The red/dimming cue is a stylized approach indicator in captured mob vertex colours. It now begins at 3 horizon radii (previously1.8) and darkens/reddens more strongly, making it easier to notice during the short approach. It adds no geometry, history buffer or extra draw pass, and is disabled in the legacy exhibit. It is not physical gravitational/Doppler spectral transport; observer dependence and emission history are not modeled. Correct apparent slowing near the horizon concerns the received image; mobs are currently advanced in ordinary game time and removed on capture. A physical observer-dependent slowing/fading image requires delayed-light/history work, which remains deferred. See [the University of Texas relativity notes](https://www.as.utexas.edu/astronomy/education/fall13/wheeler/secure/rev.ex4.fall.13.pdf).

## Automatic arrow exhibit — 2026-09-24

`/interstellar demo arrows` enters a separate reference exhibit, preserving edits to the older gameplay world and its relocated sources. `/interstellar demo view arrows` returns to the closer viewing position. The usual `/interstellar demo leave` returns to the saved original world/pose.

Four colour-marked dispensers launch deterministic native arrows: orange attempts a transient loop around the source, cyan a deflected flyby, magenta an outward shot pulled back, and red direct capture. They are calibrated for the scene's original64-block cube and default gravity strength. Editing the mass changes their paths. These demo dispensers use a timed calibrated launch rather than inventory or vanilla random spread; ordinary dispensers elsewhere remain vanilla. Downward gravity, air drag and native collisions stay active. The loop is not a permanent circular orbit.

One station fires each second, staggered across four stations. Only a nearby player in this exhibit activates firing. At most12 demo arrows are retained, with an8-second lifetime and removal of stale demo arrows on chunk reload. Destroying/turning a station disables its launch. Occupied setup cells are preserved.

`/interstellar demo arrows on|off` controls automatic fire for this session; `once` stops automatic fire and launches one measured set, reporting winding/minimum-radius results to the log. `setup` retries station installation without overwriting other blocks. Ordinary player arrows are unaffected by demo cleanup. Tests compare the intended qualitative paths to an independent continuous-force RK4 reference including downward gravity and continuous drag; live native-tick checks remain the acceptance criterion.

### Arrow-course verification — 2026-09-24

The user's existing gameplay source had moved to `(15,91,-14)`. The first fixed-layout trial consequently did not produce the intended paths; it was rejected and its eight new station blocks were removed. The final course has its own dimension and known source at `(2,82,2)`, preserving the user's edited scene.

The native runtime with the reference64-block source measured:

| Station | Result |
|---|---|
| Orange loop | 491.875 degrees of azimuth winding, closest centre distance4.225 blocks; later hits ground |
| Cyan flyby | Closest distance3.867 blocks, escapes the source and subsequently embeds in exhibit terrain |
| Magenta return | Launches outward, reverses and crosses the horizon after14 ticks |
| Red capture | Crosses the horizon after12 ticks |

Azimuth winding is a measured loop, not evidence for a stable closed orbit or relativistic periapsis precession. Downward gravity and drag remain active. Final simulation/reference checks include75 unit tests. In a160-tick four-arrow window, tracking averaged0.0067ms/server callback; the native mob-movement scope averaged0.0477ms/world tick and native projectile scope0.0405ms/world tick (maximum projectile call0.7243ms). These include vanilla work and are not an isolated added-cost measurement. The stronger mob cue was visually checked against an unchanged white sheep outside its range.

A normally ticking sheep30 blocks from the source, with its AI walking-speed attribute set to zero, acquired inward x velocity−0.002346 blocks/tick. This location was outside the previous20-block reach. The earlier NoAI fixture stayed immobile and was not used as evidence for the field check; the movement hook preserves native immobilization rather than force-ticking such fixtures.

### Small-source ring correction

The reported concentric sky rings were reproduced around one mass block. Disabling adaptive steps removed them. A first incoming-distance limit changed the pattern but failed the image comparison and was discarded. The retained fix bounds the outgoing angular step by `0.5*u/(-u')`, only in extended-source programs. The old local spatial-step estimate could overshoot `u=0` (infinity), return sky immediately and skip finite foreground/background geometry. The new guard preserves a positive endpoint so the finite chord is intersected first. The metric and black-hole program path are unchanged.

An analytic straight-ray regression uses impact parameter0.5, initial radius8 and a wall at x=28: the old16-block estimate overshoots infinity; guarded steps hit the analytically known point on the wall. Runtime images confirm the rings are removed while lensing remains. The new adaptive/conservative same-frame pair has whole-image RGB MAE below0.0001 (normalized0–1), with about0.05% of pixels differing by more than8/255. These are sampled geometry/image checks, not arbitrary critical-ray certification.

At1440p, half-resolution trace,2×AA, the fixed-step reference measured27.988ms GPU p50, the flawed original adaptive path11.779ms, and the retained guard11.825ms (p95:28.365/12.034/12.093ms). The0.39% median-time difference between original and corrected adaptive runs is within the scale of normal run variation; no exact zero-cost claim. The black-hole path does not contain the new guard. The first rejected candidate measured11.787ms but retained visible errors, so its timing did not justify keeping it.

The two-block follow-up is visually clean too. A live1440p arrow-view check, normal world ticking, measured pass p50/p95 of17.727/18.406ms firing and17.310/18.058ms after demo arrows cleared. Frame interval medians were18.060/17.680ms. This is a short sequential scene check with evolving actors, not a precise isolated overhead or a worst-case FPS guarantee. Compact raw evidence is in [the follow-up verification record](profiles/2026-09-24-arrow-course.txt).

## Initial verification — 2026-09-23

- Build and package checks cover 72 unit tests, including component merge/split/removal, chunk reload, in-flight edits, cube calibration, field taper/orbits/contact and the independent optical comparisons above.
- All new programs compiled in Minecraft. A 64→27 edit refreshed F10 without terrain recapture; the 27-block source remained visible while the surrounding wall was lensed.
- Final 64→8→27→64 edits selected the correct compactness and shader automatically. Screenshots show visible mass surfaces for the smaller cubes, increasing wall deflection, and a black shadow only at the 4-cube threshold. The approaching sheep's red/dim cue was also inspected. Movement/flicker automation remains deferred by the owner.
- Fresh world entry found the existing 64-block source within about one second, without inspection or block edits. This is one favourable loaded-chunk case, not an upper latency bound for every source in render distance.
- A sheep below the source gained positive vertical velocity (0.06775 blocks/tick after the first step), then was captured. A nearby arrow and snowball curved inward and escaped; an outside-field control arrow kept vanilla velocity `(2.9700000286,-0.05,0)` after one tick from initial `(3,0,0)`.
- An arrow retained its expected six points of damage to a target. A nine-block/tick projectile was captured before reaching a target behind the horizon. A fast arrow hit a one-block glass wall and retained exactly the same embedded position after ten more ticks.
- Runtime testing found and fixed a MixinExtras receiver-type mismatch, initially slow discovery ordering, stranded mob contact at source corners, and double-scaled arrow impact displacement. The last fix preserves vanilla's 0.05-block embedding offset.
- The existing black-hole diagnostic passed 52,480 sampled optical comparisons and 84 material checks with zero mismatches/unresolved rays. This exercises the preserved Schwarzschild path, not a GPU-vs-affine certification of the new extended interior.

### CPU timing scope

After a 40-tick warmup, two consecutive 100-world-tick windows measured the exhibit at small-window resolution. These scopes include native Minecraft work. Enabled/disabled windows are not a fixed-state replay: actors advance and projectile counts change. They indicate modest cost here; their subtraction is not a precise overhead estimate.

| Scope | Gravity enabled | Gravity disabled |
|---|---:|---:|
| Shared tracking, mean per server callback | 0.0055 ms | 0.0054 ms |
| Mob movement, mean per world tick (1700 calls each) | 0.1529 ms | 0.1320 ms |
| Projectile native-tick scope, mean per world tick | 0.0202 ms (310 calls) | 0.0112 ms (300 calls) |

The earlier partly cold 11-tick check included a 5.16 ms single-projectile-call outlier; the warmed enabled window's maximum was 0.071 ms. The warmed window mostly measures resting/outgoing projectiles, not a barrage continuously traversing the strongest field. Large populations, many sources, multiplayer replication and long-session allocations still need stress profiling. Timing counters are off in normal play. The simple exhibit's initial native terrain capture took about 5.17 s; this does not supersede the older natural-world capture measurements.

### GPU checks

RTX 5070 Ti, 2560×1440 output, half-resolution trace target, two AA samples, default adaptive/native-material settings. Each measurement uses 120 warmup frames and 300 samples; times include optical resolve, not the entire frame. World ticks were frozen for these checks. The gameplay wall camera is `(2,80.38,-54)`, yaw/pitch `(0,0)`; the legacy down view uses pitch35.

| Scene | Pass p50 | Pass p95 | Frame interval p50 |
|---|---:|---:|---:|
| Gameplay 64 blocks, live F10, wall | 10.499 ms | 11.132 ms | 10.933 ms |
| Gameplay 8 blocks, live F10, wall | 9.001 ms | 9.626 ms | 9.501 ms |
| Legacy 64 blocks, frozen F9, wall | 14.113 ms | 14.770 ms | 14.495 ms |
| Legacy 64 blocks, frozen F9, down | 11.012 ms | 11.983 ms | 11.437 ms |

These scenes differ in source radius, geometry and live/frozen mode. There is no paired before/after performance claim, and this small exhibit does not establish the demanding natural-terrain FPS floor. The extended-program timing log originally used the generic `native-quads` label; the final label now identifies `native-body-quads`. Compact raw evidence is in [the verification record](profiles/2026-09-23-gameplay-gravity.txt); full logs/screenshots remain under ignored development files.

## References and provenance

- [Einstein Online, Schwarzschild radius](https://www.einstein-online.info/en/explandict/schwarzschild-radius/) and [free fall/geodesics](https://www.einstein-online.info/en/spotlight/geometry_force/) motivate the exterior/compactness and force-free motion distinctions.
- [Carroll, GR lecture notes](https://arxiv.org/abs/gr-qc/9712019) is the metric/geodesic foundation. The rational interior lapse and gameplay coupling above are this project's explicit choices, not formulas attributed to that source.
- Projectile integration was checked against locally generated sources for the pinned Minecraft 1.21.1/Yarn build 3. [MixinExtras WrapMethod](https://github.com/LlamaLad7/MixinExtras/wiki/WrapMethod) and [WrapOperation](https://github.com/LlamaLad7/MixinExtras/wiki/WrapOperation) document the hooks. No Minecraft implementation was copied into the mod.
