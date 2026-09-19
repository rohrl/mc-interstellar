# Decision log

Important decisions are recorded here with stable IDs. Supersede entries rather than silently rewriting history. Date: 2026-09-13 (Australia/Brisbane).

## D001 — Scientific scope — accepted

Build a scaled educational optical simulation, not a dynamical solution of Einstein's field equations for the Minecraft world. GPU performs expensive light propagation. Java manages state, rendering resources, configuration, persistence and synchronization. Reason: scientific clarity with interactive performance. No terrain destruction or full server-side gravitational simulation.

## D002 — Minecraft and Java — accepted baseline

Minecraft 1.21.1, Fabric, JDK 21. Pin dependency versions and use a Gradle wrapper. Reason: established mod ecosystem and a focused version target. Revisit only with measured compatibility evidence. Initial pins: Loom 1.7.4, Loader 0.16.14, Fabric API 0.102.1+1.21.1, Yarn 1.21.1+build.3, Gradle 8.10.2.

## D003 — Rendering integration — provisional

A dedicated Interstellar shader pack is acceptable; compatibility with arbitrary existing packs is not required. Iris is an option, not a mandatory dependency. First establish scene access and horizon-crossing optics; compare an Iris adapter against a mod-owned renderer. Do not invent a public arbitrary-uniform Iris API. Bootstrap intentionally has no Iris/Sodium dependency, permitting a clean Fabric baseline before compatibility trials.

## D004 — Optical core must cross the horizon — accepted requirement; coordinates proposed

Use a horizon-regular formulation and a physical local observer frame. Ingoing Kerr–Schild or Painleve–Gullstrand coordinates are candidates. Bruneton's published implementation excludes interior views, so it is a reference/exterior optimization, not the complete renderer. Final integration scheme requires numerical validation.

## D005 — Scene representation — unresolved engineering choice

Single-view screen-space data cannot recover off-screen or hidden geometry. Validate optics in a controlled scene first; investigate additional captures or GPU scene data for terrain. A cubemap alone does not solve nearby parallax/occlusion. Reason: avoid an attractive but misleading UV warp being presented as accurate GR.

## D006 — GR defaults — accepted starting values, tunable

Schwarzschild, no charge/spin, reference horizon radius 8 blocks, photon sphere 12, ISCO 24, start at 64 blocks from centre. Each mass block will contribute 0.125 blocks of Schwarzschild radius; 64 blocks give r_s=8. Cluster radius/compactness and collapse rule remain to be specified. These are scaled exhibit parameters, not claims of safe metre-sized astrophysical objects. No physical mass in kg is assigned until the length conversion is explicit.

## D007 — Observer controls — accepted

Potion initially controls observer speed independently of actual player movement. SR and GR may use independent scales/defaults. Include HUD and guided observation/free-fall modes. Stationary hovering is available only outside the horizon; an interior tour must follow a valid timelike trajectory. Slowing playback does not alter physical equations.

## D008 — Player returning-light image — accepted

Use the player's actual body and skin, including the back, in the optical scene. A stationary photon-sphere setup is the first candidate; arbitrary moving-body accuracy later requires emission-time pose history. Do not promise a mirror-like image or treat this as a generic event-horizon effect. Reason: user explicitly rejected a mannequin substitute.

## D009 — Deferred features — accepted

Accretion disks and general infalling-entity history/horizon-freeze rendering are follow-ups. Kerr spin, multiple interacting strong sources, CMB spectral rendering, and comprehensive dynamic-scene retarded-time rendering are later milestones. Horizon crossing is not deferred with entity freeze.

## D010 — Performance — accepted, supersedes 120 FPS proposal

Target 2560x1440 at 60 FPS (16.67 ms total frame) on Ryzen 7 5800X3D / RTX 5070 Ti / 32 GB RAM. Measure baseline first and track CPU/GPU costs, tail frame times, scene, resolution, and quality settings. Heavy disabled features must skip passes/resource work. Separate pedagogical effect isolation from numerical quality controls.

## D011 — Repository and continuity — accepted

Actual Git root is C:\work\code\minecraft\interstellar\interstellar (nested folder is intentional). Remote: https://github.com/rohrl/mc-interstellar.git. User authorizes branches and pushes. Keep decision-log.md, plan.md, progress.md, handoff.md and source references in Git so another agent can resume. Bootstrap branch: codex/bootstrap-observatory.

## D012 — Development JDK — verified

Use complete Temurin 21.0.12.1 at C:\Portable\jdks\temurin-21.0.12.1 on this machine. Minecraft's bundled Java 21 has javac but lacks jmods; do not base the build on that launcher-managed runtime. Do not commit machine-specific JDK paths into Gradle configuration.

## D013 — First code checkpoint — accepted implementation choice

Create a buildable Fabric foundation, diagnostic HUD, validated config, and isolated scientific reference quantities before the GPU experiment. Clearly label the HUD as calibration only. Reason: establish repeatable builds and an observable camera/source convention without confusing unimplemented optics with a simulation.

## D014 — Optional official dependency mirror — accepted implementation choice

The initial machine could reach Maven Central but TCP connections to maven.fabricmc.net timed out on both published IPv4 addresses. maven2.fabricmc.net worked and is listed in the official Fabric installer's Reference.java. Add a fabric_maven_url Gradle property to redirect plugin and Loom-added Fabric repositories when required; preserve the primary service as the default. No third-party mirrors or machine DNS changes. Verification command may supply -Pfabric_maven_url=https://maven2.fabricmc.net/.

2026-09-14 follow-up: IntelliJ imports do not inherit CLI -P flags. Configured the same project-supported override in this machine's previously absent user Gradle properties, outside Git. Reason: allow the owner's normal IDE import to use the known-working official service. Loom's separate intermediary URL is covered by the project override as well.

## D015 — Exterior lab checkpoint — accepted implementation choice (2026-09-14)

Use Fabric core shader registration and a mod-owned F8 screen for an immediately observable controlled-sky experiment. Integrate the exterior planar null-ray equation on the GPU with RK4, and compare a double-precision implementation to analytic limits and step refinement. This is a preliminary checkpoint within iteration 1, not completion of the horizon-capable solver. Reason: validate shader integration and source geometry before terrain access. Iris remains optional. Hovering is restricted to r/r_s >= 1.05; guided horizon crossing still requires a different observer formulation. The procedural source is extended so its Einstein ring is visible without claiming a glowing photon sphere.

## D016 — Opt-in asynchronous GPU timing — accepted (2026-09-14)

Use timestamp pairs with eight outstanding slots; read only available results, skip sampling if busy, and free queries on completion/cancellation. Support ARB_timer_query in Minecraft's OpenGL 3.2 context. Reason: isolate optical draw cost without introducing a GPU stall or confusing capped FPS with shader time. No profiling overhead when unrequested. Preliminary 1440p results support continuing direct integration; they do not settle terrain/horizon architecture. See docs/benchmark.md.

## D017 — Exact framebuffer ray mapping and opt-in readback — accepted (2026-09-14)

The owner's V tests revealed mismatches caused by rounded GUI dimensions/projection. The optical quad now owns clip-space mapping and uses framebuffer dimensions; HUD drawing remains vanilla GUI. Add a one-shot RGBA32F diagnostic of the production shader, comparing 9216 rays to finer CPU RK4 plus analytic capture. Reason: numerical evidence must test actual GPU output and resizing, not only copied CPU equations. The diagnostic is intentionally blocking and excluded from normal rendering. This is not the independent horizon solver. Results/limits: docs/ray-validation.md.

## D018 — Free-fall reference and horizon sky — accepted (2026-09-14)

Choose ingoing PG coordinates and a radial observer falling from rest at infinity. Independent reference uses adaptive Dormand-Prince in backward PG time; GPU keeps the horizon-regular spatial inverse-radius equation with falling-frame initial conditions. Use one asymptotic sky and dark nonconnecting past boundaries, not a collapse/white-hole simulation. T follows the exact radial proper-time law; H pauses at the horizon; stop the tour at 0.35 r_s. Reason: a valid observer and checkable horizon passage without introducing unavailable terrain/history data. CPU/GPU ray integrators differ, but share analytic sky-connectivity classification. Detailed equations, evidence and limits: docs/free-fall.md.

## D019 — Agent-operated runtime verification — explicit owner preference (2026-09-14)

The agent handles ordinary Minecraft navigation, controls, screenshots, V checks and benchmarks autonomously. Leaving the client open does not request owner testing. Ask for attendance only when automation cannot complete a material check or a preference is needed. Reason: owner should not need to constantly attend development.

## D020 — Optical configuration and integration quality — accepted (2026-09-15)

Use a separate interstellar-optics.json so the existing calibration config remains compatible. Load partial files over defaults, validate types/ranges, and preserve existing files on success or failure. F8 loads a snapshot; Q and other keys change session state; R restores that snapshot. Persisted changes are explicit JSON edits, not automatic writes during exploration. FAST/STANDARD/FINE use 0.04/0.02/0.01-radian steps with the same 16-radian budget. Reason: adjustable GPU cost without conflating effect isolation with accuracy. Fine steps are not a numerical error guarantee. Measured dynamic-loop overhead and accuracy are recorded in docs/optical-settings.md.

## D021 — Mass-block geometry and bounded inspection — accepted implementation choice (2026-09-15)

Use equal-mass face-connected blocks, arithmetic block-centre COM, and a centre-based enclosing radius including block corners. r_s=0.125*N as planned; C=r_s/R>=1 is a labelled spherical black-hole proxy, not a general collapse law. Inspect on demand with at most 256 queued cell queries per tick, eight jobs, and 4096 mass blocks per job. Unknown/capped components are incomplete, and revisions cancel stale jobs. Reason: inspectable source semantics before renderer coupling or a maintained index. Fixed mass uses normal chunk persistence without BlockEntities/tickers. This is the first part of iteration 2, not completion of persistent live clustering. Detailed bounds and checks: docs/mass-blocks.md.


## D022 — Validated selection bridge — accepted (2026-09-15)

Send completed inspection metadata through Fabric typed S2C payloads; never read integrated-server world objects on the renderer thread. Keep one selection per player. Coalesce world revision changes at the end of the server tick and clear affected selections; a replacement inspection clears its predecessor before starting. Client state is scoped to the actual ClientWorld instance and cleared on disconnect. Reason: prevent stale, capped or unknown components from silently becoming optical sources. World-wide chunk revisions are deliberately conservative and may require reinspection after unrelated chunk activity.

F8 remains the reference lab; S explicitly adopts an inspected black-hole proxy. It uses camera coordinate distance / selected r_s, clamps to the lab's supported radius range, and chooses a falling frame below 1.05 r_s. The lab's axes still point toward/away from the source; they do not follow Minecraft yaw. Controls explore the selected spherical model, not live player motion. Extended sources remain metadata-only because a material-interior optical model does not yet exist. R restores configured reference defaults. No world terrain lensing is implied.
## D023 — Critical-ray limits and next demo priority — accepted (2026-09-15)

Keep the established inverse-radius solver while adding reproducible near-critical diagnostics. V now compares unwrapped orbital angles as well as endpoint directions. C tests 252 explicit float input rays across seven observer/frame cases and three quality presets, including directions outside the current view. CPU-unresolved references, GPU budget exhaustion, wrong resolved outcomes, and full-turn-bin differences are separate quantities. Reason: matching a final direction can conceal extra or missing orbits, and finer float steps do not guarantee better critical-ray accuracy. A shifted-variable formulation was evaluated but not retained because improvements were inconsistent. See docs/critical-rays.md and its raw CSV for the measured limits.

Prioritize a playable terrain-lensing prototype next, ahead of the actual-body exhibit. The owner asks how far we are from mass blocks bending light from behind them. First scope: one selected spherical black-hole proxy, bounded nearby opaque terrain, explicit missing-data behavior and an on/off comparison. This is the iteration 3 scene-data experiment brought forward; player-body demonstrations and numerical refinements remain in scope. Do not turn the diagnostic work into an indefinite prerequisite for this prototype or claim the present sky lab bends Minecraft terrain.
## D024 — Bounded textured voxel terrain preview — accepted (2026-09-15)

Implement F9 as a frozen 96^3 client-world snapshot with textured opaque cubes and GPU curved-ray/voxel intersections. Use actual off-screen block data, not a warped screen image. Capture incrementally with explicit work bounds, no forced loads and no server solver. Preserve separate air/unknown/unsupported meanings and show missing data. World coordinates are mapped to Schwarzschild areal-radius/angular coordinates; only static exterior terrain is supported initially. Reason: a reviewable working mass-block demo that exercises occlusion and off-screen geometry before live renderer integration. Default to half-resolution output after measuring GPU cost. Keep F8's falling sky independent.

V validates flat geometry against independent cube-slab intersections and audits off-screen lensed hits; it does not certify curved-surface accuracy. Isolate OpenGL pack/unpack state: inherited skip-row state caused a native upload overread during testing. No invented fading of foreground edges into secondary images. Detailed controls, math, evidence, costs, limits and scene coordinates are in docs/terrain-prototype.md.
## D025 — Live camera with bounded terrain refresh — accepted (2026-09-15)

Add F10 as a client-owned terrain pass before the vanilla HUD. Keep normal movement/input and F9's frozen diagnostic view. Refresh the same 96^3 region with a second incremental snapshot after a one-second wait; publish only completed captures and explicitly release native buffers/textures. Reason: make the demo explorable while bounding capture work and retaining a complete renderable scene during updates. This is periodic full-region recapture, not dirty-region tracking or an atomic world-time snapshot. Display publication age; cells may be older by the capture duration.

Use the existing static exterior observer at each camera position, fixed 70-degree vertical FOV and unchanged curved-ray solver. Movement does not yet introduce relativistic observer velocity. Vanilla interaction rays remain straight and are labelled in the HUD. Stop on source/world invalidation, resource reload, capture-boundary exit or r/r_s<1.05. Body/entities, horizon-crossing terrain, full block models and radiometry remain separate work. See docs/live-terrain.md for actual checks and performance.

## D026 — Leaner agent workflow and snow geometry — accepted (2026-09-15)

Owner explicitly requests lower token usage. Persist scoped reads, compact logs, state-gated batched GUI actions and selective screenshots in AGENTS.md; keep current handoff short and detailed evidence in feature notes. Do not claim measured savings without accounting. An initial batched launch typed into world search; corrected by requiring confirmed world entry before commands. Batching must not remove readiness checks.

Render Blocks.SNOW as a textured cuboid of height layers/8, retaining unknown/unsupported markers for other shapes. Store height in the unused first palette texel alpha, intersect partial-height cells within each ray chord, and extend the independent CPU slab reference. Reason: preserve actual snow geometry rather than hiding it or treating it as full diagnostic cubes. No world blocks/weather were altered to improve the image. Evidence: docs/snow-layers.md.

## D027 — Stable selection and recoverable bounded exploration — accepted (2026-09-15)

Supersede D022's world-wide invalidation for completed selections with conservative source-local chunk dependencies, including a one-block connectivity shell. Retain world epochs for unfinished probes. Supersede D025's permanent stop at camera limits with normal-view pause and automatic resume; invalid source/world/resources still require explicit recovery. Reason: exploration should survive unrelated chunk activity and brief excursions outside supported observer conditions.

Extend observer access to 128 coordinate blocks while keeping 96^3 source-centred scene data. Clip chords into the bounded volume, retain exact traversed voxel identity for UV/diagnostic reporting, and explicitly label omitted outside terrain. This does not provide occlusion by uncaptured foreground geometry, horizon terrain or a maintained source index. Details and verification: docs/stable-exploration.md. Antialiasing WIP remains isolated on codex/terrain-antialiasing at 8ad46eb.

## D028 — Bounded independent finite-surface diagnostic — accepted (2026-09-15)

Add opt-in F9 C using an affine-parameter radial ODE, adaptive DP5(4), independent cuboid slabs, and paired CPU chord/tolerance refinement. Compare resolved outcomes and exact hit cells against production GPU rays; keep refinement failures, unresolved rays and invalid outputs distinct. Record horizon capture separately from missing data in terrain diagnostic output. Reason: flat-only validation cannot check bent-ray foreground ordering. This is sampled finite-surface evidence, not a universal accuracy claim. Details: docs/curved-terrain-validation.md.

Owner reaffirmed the five-step delivery plan: stable exploration, wider useful viewing, demo packaging, visual refinement, deeper relativity. Finish bounded automatic source refresh and repeatable demo packaging next. Do not let further numerical polishing or deeper features displace that sequence.

## D029 — Anchored event-driven source refresh — accepted (2026-09-15)

Maintain one inspected anchor per player/world session. Relevant events immediately withdraw stale metadata, debounce a rescan, and preserve F10 intent through refreshing, unloaded, removed and extended-source states. Splits follow the anchor's fragment; anchor removal waits for replacement or explicit inspection elsewhere. Reuse the bounded probe queue and conservative in-flight epochs, with retry backoff and expanded dependencies for partial/growing components. No forced loads, periodic region polling, global cluster index or silent replacement-fragment selection. Details/evidence: docs/source-refresh.md.

This completes the bounded automatic-refresh portion of delivery step 1. Next prioritize repeatable demo packaging (step 3), while retaining step 2's explicit outside-data limitations. AA and deeper-relativity work remain later in the owner-reaffirmed order.

2026-09-16 owner-reported placement follow-up: MassBlock inspection consumed held-item right-clicks. Return PASS when either hand holds an item; retain inspection only with both hands empty. Actual RMB placement and empty-hand inspection verified. Owner authorized adding source blocks; the current saved cube is N=64/COM=(16,302,16)/r_s=8 after filling only air and removing the one-block RMB test protrusion.

## D030 — Reject mixed-camera background fallback (2026-09-16)

The ordinary-world fallback in 87dd3a1 visibly duplicated the coloured wall while walking: curved-ray hits and straight-camera pixels cannot form one consistent scene. Owner rejected it; reverted in 0a264b0. Earlier visual acceptance was insufficient. Restore the explicit missing-data grid until coherent extended ray/geometry access exists. Step 2's scene coverage and foreground ordering now take priority over packaging. Proposed architecture and concrete acceptance scenes are in plan.md; no claim that this is already implemented.

## D031 — Practical hybrid world rendering is a demo requirement — accepted (2026-09-16)

Owner clarifies that an accurate renderer which cannot fit into the world at usable performance is not useful. Permit distant-scene heuristics with small or hard-to-notice quality losses and substantial measured performance gains. Coherent world integration must ship in the working demo, not be deferred as polish. D030 rejects visibly inconsistent mixed-camera composition, not approximation itself. Prototype a hybrid local/remote representation and compare moving views, occlusion, transition artifacts and frame costs before committing to a large exact geometry cache. Candidate methods and acceptance criteria are in plan.md; no speedup or visual success has yet been established.

## D032 — Opt-in height-field experiment, not demo acceptance (2026-09-16)

Implement and measure a bounded distant column map with exclusive local-volume ownership, coherent first-hit comparisons and independent GPU/CPU column diagnostics. Prototype weak-field outgoing straight continuation and conservative maximum-height rejection; retain strict local diagnostics. The final frozen far-view GPU p95 is 9.932704 ms, but the fog boundary, flat sky and height-field geometry limitations remain conspicuous. Keep distantPrototype false by default; no claim that step 2 or demo world integration is complete. Details, rejected timing variants and next decisions: docs/distant-prototype.md. Owner's token preference is recorded in AGENTS: screenshots after major rendering work, not each simple edit; state-gated inputs and completion-gated timing.

## D033 — Appearance parity before another approximation — accepted direction (2026-09-16)

Owner rejected white mountain columns/dark flat sky and proposed slow-reference paired-image tests. Code confirms top-material column extrusion and simplified lighting/sky. Adopt staged automated comparisons: vanilla versus the integration backend with bending disabled first; independently checked slow lensed reference only after appearance parity. Do not mistake slower rendering of shared omissions for ground truth. Deterministic small image suites, regional/perceptual errors and short movement sequences should replace most routine screenshot inspection, with visual approval at major gates. Architecture feasibility and unimplemented next gate: docs/visual-reference-plan.md. Stop promoting the height-field prototype as world integration.

## D034 — Native appearance and first comparison checkpoint — accepted (2026-09-17)

Reuse Minecraft sky/cloud rendering and lightmap/face brightness, with separate distant cap/side materials. This corrects the gross white-mountain/flat-sky failures without restoring ordinary terrain camera copies. Repair the interrupted shader's reserved `packed` identifier. Same-frame vanilla/zero-bending capture and regional Java comparison now run; appearance parity remains unfinished. Keep code default opt-in, enable locally for owner inspection. Owner explicitly defers automated movement/flicker checks, superseding that part of D033 for now. Evidence, timing boundaries and remaining approximations: docs/native-appearance.md.

## D035 — Use the actual world projection — accepted (2026-09-17)

Paired-image metadata revealed effective FOV 77 degrees despite configured70; the fixed70 backend enlarged terrain. Derive ray scales/offsets from the world projection each frame in F9/F10, use matching camera inputs in independent hit checks, and record effective candidate projection in metadata. Two fixed-pose images plus zero-mismatch diagnostics verify this correction, without claiming camera bob/hurt or appearance parity. Keep the shared vertex shader Viewport uniform independent of optical projection. Evidence: docs/native-appearance.md.

## D036 — Match the native sky caller's render state — accepted (2026-09-17)

Pinned Minecraft bytecode shows renderSky inherits its initial shader and some sky elements use global matrices. Explicitly select the position shader and establish/restore world capture matrices and colour when capturing from the HUD stage; use current tick interpolation. Nighttime sky/moon/cloud alignment and repeated-reference identity verified; no claim of full atmosphere or cloud occlusion parity. Detailed captures, limits and live timings: docs/native-appearance.md.

## D037 — Native camera-relative opaque terrain fog — accepted (2026-09-17)

Owner's boundary report exposed source-relative sky-colour fading unrelated to Minecraft fog. Replace it with actual terrain fog inputs captured before the HUD, applied to opaque hits with native spherical/cylindrical distance. Curved-ray endpoint fog is an appearance approximation, not optical-depth transport. Fixed zero-bending camera/FOV matches near128; hard activation cutoff still removes nonzero lensing and remains unresolved. Do not hide it by silently fading physical lensing. Evidence and limits: docs/native-fog.md.

## D038 — Captured face light with frozen A/B checks — accepted (2026-09-17)

Capture six-face sky/block levels for local opaque cells and separate distant cap/side samples, replacing unconditional local sky15 and reused top-only distant light. Keep geometry/ray equations unchanged. F9 K enables controlled same-scene old/new comparison; F10 uses the correction. Under-platform RGB error0.0656→0.0001 justifies the fix; downward improvement is tiny, so do not present it as solving mountain shading. Additional two-snapshot memory35MiB, measured live frame p9512.2304ms with usual limitations. Evidence and remaining smooth-light/geometry work: docs/face-lighting.md.

## D039 — Native corner lighting with bounded shared records — accepted (2026-09-17)

Reuse BlockModelRenderer's corner brightness and lightmap coordinates, preserving quad triangulation in the ray-hit shading. Deduplicate into a capped atlas; fallback to captured face light on unsupported layouts/cap exhaustion. Controlled wall and daylight-mountain pairs improve with identical references. Accept the measured appearance gain while documenting slower refresh (~3.8 s) and additional bounded resources; per-frame GPU cost stays close in the sampled scene. Scope the native brightness cache to one capture slice and check budgets every cell. F9 O retains the comparison path. Evidence and limitations: docs/smooth-lighting.md.

## D040 — Quality-first native mesh experiment — accepted (2026-09-17)

Owner explicitly puts visual integration ahead of FPS optimization. Preserve the optical core and existing F10, but test actual baked geometry/appearance in a separate F9 M path instead of further patching distant columns. Capture native quads, tint/AO/light, build a bounded BVH, and use coherent nearest triangle hits along ray chords. Frozen vanilla pairs show a modest mountain improvement; wall geometry is similar, while omitted mobs/clouds/coverage remain conspicuous. This establishes an experiment, not final world integration or a perfect slow reference. No FPS rejection gate yet. Controls, measurements, memory costs and next missing layers: docs/native-mesh.md. AA remains separate.

## D041 — Native mob bodies and correct terrain lightmap coordinates — accepted (2026-09-17)

Owner visually accepts the baked terrain approach and requests mobs. Capture native living-entity solid/cutout geometry, textures and lighting into the same nearest-hit BVH; E compares bodies on/off. Keep unsupported transparency/glow/shadows explicit and retain frozen-reference scope. Pinned shader inspection also reveals terrain uses filtered UV2/256, unlike entity texelFetch(UV2/16); remove the erroneous terrain half-texel offset, with K comparison retained. Controlled close-up RGB MAE 0.0188→0.0024 with corrected lighting; disabling mobs raises it to 0.0070, with identical vanilla references. Details, bounds and remaining composition/live work: docs/mesh-entities.md. Quality remains the gate, not FPS.

## D042 — Native cloud geometry in the shared ray scene — accepted (2026-09-18)

Capture Minecraft's native cloud faces, UVs and colours into the mesh BVH; blend the nearest visible cloud surface over opaque hits/sky, following native FANCY depth-prepass behavior. Foreground mode captures sky without clouds to avoid duplication; N compares the previous background-only path. Derive escape radius from mesh bounds to retain the broader cloud footprint. Identical-reference daylight and nighttime pairs improve, with daylight mountain RGB MAE 0.0126→0.0071. Keep camera-dependent geometry, single-surface transparency and curved fog limitations explicit. Prioritize broader terrain coverage next; no FPS gate. Implementation, exact checks and cost: docs/mesh-clouds.md.

## D043 — Camera-centred native mesh footprint — accepted (2026-09-18)

Replace the source-centred 16×16 chunk mesh footprint with camera-centred configured render distance plus one chunk of margin in all directions. Capture only loaded chunks, keep the existing camera guard and refuse budget overflow rather than silently omit geometry. Raise the frozen reference budget to seven million triangles; configured distances above16 are explicitly refused. U clips terrain hits to the old bounds for identical-frame comparisons. At the mountain boundary, lower-half RGB MAE improves0.0364→0.0206; snowfall remains a separate visible omission. Accept the larger capture/memory cost for this quality-first reference, not as a live architecture or FPS result. Evidence: docs/mesh-coverage.md.

## D044 — Separate retained terrain from live mob/cloud geometry — accepted (2026-09-18)

Owner accepts initial loading for v1, excludes teleport support, and prioritizes live exploration/animated mobs over rain/snow. F10 now retains a native terrain-only BVH and rebuilds a separate small mob/cloud BVH each frame, reusing entity texture tiles. Traverse both with shared nearest-hit/cloud ordering so moving actors remain part of the curved scene, without baked duplicates. Runtime verifies sustained animation, ordinary strafe, cleanup/reactivation and diagnostic timing. The HUD explicitly labels retained terrain: edit invalidation and ordinary-movement chunk streaming are the next milestone, not completed by this change. Details and limits: docs/live-native-mesh.md. Initial capture is currently repeated on reactivation; no FPS claim or teleport work.

## D045 — Independently allocated chunk meshes and source-independent cache — accepted (2026-09-18)

Retain native chunk BVHs in row-allocated GPU arenas, with a small top-level BVH. Capture only invalidated/new loaded chunks in bounded slices; discard captures invalidated during work and publish each completed replacement. Native section/light invalidations and chunk load/unload events drive updates. Camera movement keeps overlapping entries; no ordinary-camera overlays or whole-scene rebuild on routine edits. Keep cached terrain while selected-source metadata refreshes, updating optical parameters separately. Verified temporary block/mass edits and chunk-boundary movement; frozen streamed and monolithic images have identical hashes. Accept ~1280MiB fixed GPU arenas and current slower traversal for the quality-first checkpoint, without an FPS claim. Limits, timings and reference controls: docs/streaming-terrain.md. Stars unchanged; resolution/resampling suspected but not established.

## D046 — Extend native camera access independently of terrain capture — accepted (2026-09-18)

Raise F10's camera guard from128 to256 blocks now that terrain streams around the observer. Farther F9 previews automatically use the native chunk-based reference and cannot drop into the bounded voxel backend. Preserve source-availability/exterior guards and recovery; show camera distance/limit in the HUD. No optical equations, ray budgets, force-loaded chunks or lensing fades changed. Verified normal movement through the old cutoff, far-pose vanilla comparison and automatic pause/resume across256 without reloading terrain. CPU analytic capture-boundary checks cover farther radius ratios; no independent GPU curved-mesh certification claimed. Evidence: docs/viewing-range.md.

## D047 — Small independent mesh fixture before further rendering changes — accepted (2026-09-18)

Reuse the affine CPU solver with a separate cuboid description of synthetic opaque geometry; compare production GPU triangle hits in both monolithic/two-level layouts. Preserve production distant path steps during diagnostics, and mark CPU refinement failures inconclusive. All720 sampled comparisons pass at camera distances32/96/148/252, standard/fine paths; runtime about0.58s. Identical before/after appearance hashes verify cleanup. Keep scope explicit: sampled hit cells in boxes, not arbitrary materials/triangles or universal convergence. This provides a cheap regression tool while continuing toward repeatable demo packaging. Evidence and limits: docs/mesh-ray-validation.md.

## D048 — Sharp AA and measured adaptive stepping; performance now prioritized — accepted (2026-09-19)

Owner paused packaging (unverified WIP2fe8674, isolated branch) for AA, then prioritized FPS with minimal appearance loss. Keep the soft edge filter optional after owner rejected its blur. Default to two traced subpixels and clamped cubic reconstruction; expose OFF and same-scene four-sample/full-resolution comparisons. Reuse dynamic upload storage without reducing animation or scene coverage. Adopt curvature-guided spatial steps with the existing angular cap and4-block maximum:1440 independent sampled hit comparisons pass, and two actual-world pairs differ over8 colour levels in only~0.007% of pixels. At matched2x AA, frozen streamed GPU p95 drops52.367→36.102ms (427×240 internal, not1440p). AA itself remains expensive; no whole-game or target-FPS certification. Detailed trade-offs, metrics and controls: docs/aa-performance.md.

## D049 — Reuse chord reciprocals and vectorize geometry bounds — accepted (2026-09-19)

Keep the same padded-box/triangle tests and traversal order, while sharing reciprocal directions across each chord's nodes and testing axes together. Explicit parallel-axis containment avoids NaNs and preserves the previous threshold. Adopt by default after2880 independent sampled hit comparisons pass and same-frame images are pixel-identical at small-window and1440p sizes. Matched1440p GPU p95 improves120.778 to111.447ms (7.7%), with2x AA/adaptive paths unchanged. The scene is frozen and has fewer actors than the previous session, so only within-session comparisons establish this gain. Retain the original implementation behind F9 T and Ctrl+Shift+P for regression comparisons. Full measurements and limits: docs/aa-performance.md.

## D050 — Specialize exact integer mesh addresses — accepted (2026-09-19)

Use explicit4095/4096 fixed-divisor texture addressing so the shader compiler can simplify repeated integer arithmetic; preserve a general-width fallback and identical integer texel coordinates. Matched frozen2xAA/adaptive/fast-bounds GPU p95 improves44.300→37.261ms small-window and160.323→135.443ms at1440p (15.5%). Same-frame images pixel-identical at both resolutions;5760 sampled ray comparisons pass. Retain original addressing behind F9 R/Alt+P. No optical/geometry/quality reduction. Actor counts differ from prior sessions; only matched within-session gains are established. Details: docs/aa-performance.md. Handoff condensed to current facts and document links to avoid repeated historical context.

## D051 — Learn empty regions from ordinary traversal — accepted (2026-09-19)

Cache a ray-local empty box by intersecting separating half-spaces from rejected subtrees during existing traversal. Publish only after complete traversal with no triangle leaf entered; reuse only for segments strictly inside the box. Keep independent terrain/moving caches and reset each AA ray. This skips geometry checks while retaining identical optical chords, geometry and materials. A separate fixed-cell occupancy-query prototype was slower and rejected. The retained approach passes11520 sampled hit comparisons and pixel-identical wall/downward pairs, with GPU p95 reductions51.8% at1440p wall view and34.4% in the small downward view. F9 I/Alt+Shift+P retain uncached comparison. Evidence, invariants and limits: docs/aa-performance.md.

## D052 — Expand the starting extent of learned empty regions — accepted (2026-09-19)

Increase initial cache half-extent16→1024; geometry still clips it with the same conservative subtree separation rules. This expands reuse, not captured/viewing distance or optical steps. Adopt after17280 sampled comparisons pass and four wall/down/away pairs are pixel-identical. Matched GPU p95 reduction27.2% at1440p wall view;21.8% downward/15.8% away at small size. F9 Shift+I and Ctrl+Alt+P retain16/1024 comparisons. Full evidence/limitations in docs/aa-performance.md. No image-quality compromise; occupied-geometry traversal remains the next optimization target.

## D053 — Specialize native rendering while sharing shader logic — accepted (2026-09-19)

Compile native mesh and general programs from one shared GLSL implementation, fixing only the backend in the native variant. Preserve all optics, coverage, AA, materials and previous diagnostic/performance toggles. Both programs pass17280 sampled CPU/GPU comparisons; three same-frame image pairs are pixel-identical across mesh layouts/views/resolutions. Reversed-order1440p streamed measurements confirm a modest3.2–3.4% GPU p95 reduction; small downward terrain has no meaningful gain. Adopt with native default and general reference behind F9 S/Shift+S. No occupancy or universal-FPS claim. Review found no confirmed new correctness defect and supports continuing toward better occupied-geometry traversal. Evidence and next experiments: docs/aa-performance.md.
