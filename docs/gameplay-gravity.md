# Gameplay gravity — implementation and verification

Implemented on `codex/gameplay-gravity`. The owner accepted the proposal and permits a small performance cost. The checks below establish sampled numerical and runtime behaviour, not complete physical accuracy or a worst-case performance guarantee.

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

Motion is explicitly scaled Newtonian gameplay dynamics, separate from the optical metric. Exterior acceleration is `-0.05*N*rVector/r³` in blocks/tick², with a uniform finite interior and acceleration capped at 0.35 blocks/tick². The finite reach is approximately 7.1/13.0/20 blocks for the 8/27/64 reference builds. The outer 40% uses a quintic taper to zero; real gravity has no such cutoff.

Local entity dynamics currently support sources with enclosing radius at most 16 blocks and horizon radius at most 12; the outer reach is capped at 32. Large-source optics and the 4096-block metadata limit are separate from this gameplay limit. No horizon is silently shrunk to fit the force range. The HUD and gravity status command expose the limit.

Mobs keep native AI, contact, friction and drag. A physical-movement hook adds the source acceleration once per world tick, including flying/swimming movement overrides. Close pull can exceed the ordinary downward acceleration and lift mobs. Players and mounted/passenger groups are excluded. The radial force preserves sideways motion rather than steering a heading at the source.

Ordinary/spectral arrows and vanilla thrown entities use 2–16 bounded movement substeps near an influencing source. Native hit handling is reused per segment; full velocity remains available to damage/deflection, lifecycle tick runs once, drag is distributed over the substeps, and ordinary gravity is retained. Fast paths check the full swept interval when selecting a source. Tridents, fishing lines and separately controlled/homing projectiles are outside this first integration. Outside a field the vanilla tick remains intact.

Capture removes crossing mobs/projectiles through normal entity removal, without terrain damage, extra mass or a loot cascade. Projectile collision queries stop at the horizon, so a target behind it cannot be damaged first. Mobs use their swept centre and contact of their bounding box with the horizon, avoiding bodies stranded on solid source corners. General retarded/frozen horizon images remain deferred.

The red/dimming cue is a stylized approach indicator in captured mob vertex colours. It adds no geometry, history buffer or extra draw pass, and is disabled in the legacy exhibit. It is not physical gravitational/Doppler spectral transport; observer dependence and emission history are not modeled.

## Verification — 2026-09-23

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
