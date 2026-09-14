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
