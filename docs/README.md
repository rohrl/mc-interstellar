# Documentation index

Reviewed 6 October 2026. Start with the [player README](../README.md) for installation, features and controls.

## Current guides

| Guide | Purpose |
| --- | --- |
| [Demo quickstart](demo-quickstart.md) | Exhibits, commands and return to your world |
| [Settings and Rift Pearl](settings-and-wormhole-seed.md) | Six-tab F4 menu, AA, portal placement and transit |
| [Relativistic Sight](relativistic-sight.md) | Two-minute walking potion and observer-speed controls |
| [Accretion disk](accretion-disk.md) | Current disk/ambience controls, model and measured checks |
| [Minecraft coverage](minecraft-coverage.md) | Supported features and remaining limits |
| [Science](science.md) | Physical identities, chosen approximations and references; dated sections retain model evolution |
| [Development](development.md) | Building, ordinary artifacts and optional Windows RTX launch |
| [RTX installation](rtx-installation.md) | Standalone Windows RTX jar, dependencies, fallback and distribution checks |
| [Plan](../plan.md) / [handoff](../handoff.md) | Current completion, deferrals and outstanding verification |

## Illustrated explanation

The [visual guide](visual-guide/README.md) explains the earlier renderer snapshot. Its diagrams remain useful, but it predates live RTX, wormholes, the potion and the disk; use the current guides for those features.

## Historical implementation reports and proposals

These preserve evidence and reasoning from development. Old FPS, limits, defaults and future-work statements apply to their recorded checkpoint. Rejected experiments are not active features. Local `run/` screenshots/logs are developer evidence, not bundled downloads. Links below lead to tracked reports; some link onward to local-only evidence.

- [Antialiasing and ray-step comparison](aa-performance.md)
- [Per-actor hierarchy experiment — 2026-09-23](actor-hierarchy.md)
- [Optical pass benchmark — 2026-09-14](benchmark.md)
- [Native cloud face intersections — 2026-09-23](cloud-quads.md)
- [Exact compact nodes](compact-nodes.md)
- [Critical-ray diagnostics](critical-rays.md)
- [Independent curved-terrain checks](curved-terrain-validation.md)
- [Demo packaging checkpoint — 2026-09-22](demo-packaging.md)
- [Distant height-field experiment — 2026-09-16](distant-prototype.md)
- [Larger steps in certified empty space — rejected experiment](empty-spans.md)
- [Final-hit opaque shading — rejected experiment, 2026-09-19](experiments/final-hit-shading.md)
- [Terrain surface-area tree experiment — rejected, 2026-09-19](experiments/terrain-sah.md)
- [Small-mass lens artifacts and renderer shortcut](extended-lens-artifacts-2026-09-28.md)
- [Captured face lighting — 2026-09-17](face-lighting.md)
- [Final bug bash and gameplay assessment — 30 September 2026](final-bugbash-2026-09-30.md)
- [Known streamed texture layouts — accepted](fixed-layout.md)
- [Free-fall sky experiment](free-fall.md)
- [Gameplay gravity and automatic sources — proposal, 2026-09-23](gameplay-gravity-plan.md)
- [Gameplay gravity — implementation and verification](gameplay-gravity.md)
- [Horizon access and returning-body study — 2026-09-24](horizon-body-study.md)
- [Incremental terrain refresh experiments — 30 September 2026](incremental-refresh-2026-09-30.md)
- [Normal-setting shader specialization — accepted](live-default-specialization.md)
- [Live native terrain and moving mobs — 2026-09-18](live-native-mesh.md)
- [Live terrain preview](live-terrain.md)
- [Curvature-limited longer segments — accepted16-block cap](long-chords.md)
- [Mass blocks and bounded cluster inspection](mass-blocks.md)
- [Native material coverage — accepted refinement, 2026-09-22](material-coverage.md)
- [Native foreground clouds — 2026-09-18](mesh-clouds.md)
- [Camera-centred mesh coverage — 2026-09-18](mesh-coverage.md)
- [Frozen native mobs and terrain lightmap parity](mesh-entities.md)
- [Independent curved mesh fixture](mesh-ray-validation.md)
- [Separate actor and cloud trees — 2026-09-23](moving-trees.md)
- [Native appearance repair — 2026-09-17](native-appearance.md)
- [Native fog and boundary check — 2026-09-17](native-fog.md)
- [Native baked-mesh integration experiment](native-mesh.md)
- [Optical settings and quality](optical-settings.md)
- [Overnight implementation — 28 September 2026](overnight-goals-2026-09-28.md)
- [Original four-experiment scope](performance-experiments-1-4.md)
- [Rendering profile and revised priorities — 2026-09-23](performance-profile-2026-09-23.md)
- [Performance review after demo and material refinement](performance-review-2026-09-22.md)
- [Rain and snow — feasibility assessment, 2026-09-19](precipitation-assessment.md)
- [Wormhole acceptance evidence](profiles/2026-09-28-wormhole-optics/README.md)
- [Continuous exterior and bounded repeated views — 29 September 2026](profiles/2026-09-29-continuous-wormholes/README.md)
- [Prompt live edits and walking-triggered Relativistic Sight](profiles/2026-09-29-live-edits-and-walking/README.md)
- [Relativistic Sight acceptance — 29 September 2026](profiles/2026-09-29-relativistic-sight/README.md)
- [Rift Pearl UX acceptance](profiles/2026-09-29-rift-pearl/README.md)
- [Settings and Wormhole Seed checks](profiles/2026-09-29-settings-portals/README.md)
- [Unified mass and wormhole gameplay — 29 September 2026](profiles/2026-09-29-unified-gameplay/README.md)
- [Preparation evidence](profiles/2026-09-29-world-preparation/README.md)
- [Wormhole opening checks — 2026-09-29](profiles/2026-09-29-wormhole-opening/README.md)
- [Faint Doppler detail — 30 September 2026](profiles/2026-09-30-spectral-visibility/README.md)
- [Shared native quad vertices — experiment, 2026-09-22](quad-vertices.md)
- [Conservative compressed bounds — 2026-09-23](quantized-node-bounds.md)
- [GPU ray validation — 2026-09-14](ray-validation.md)
- [Black-hole and relativity feature ideas](relativity-feature-ideas.md)
- [Where the current black-hole renderer spends its time](rtx-bottleneck-profile-2026-09-28.md)
- [RTX full-image experiment — 28 September 2026](rtx-full-image-2026-09-28.md)
- [Live RTX world integration — 28 September 2026](rtx-live-world-2026-09-28.md)
- [RTX with real Minecraft geometry: feasibility checkpoint](rtx-native-feasibility-2026-09-28.md)
- [RTX for extended masses and horizon views](rtx-optical-variants-2026-09-28.md)
- [RTX feasibility: isolated intersection queries](rtx-probe-2026-09-24.md)
- [Native corner lighting — 2026-09-17](smooth-lighting.md)
- [Snow-layer support — 2026-09-15](snow-layers.md)
- [Automatic source refresh](source-refresh.md)
- [Separate AA ray passes — accepted](split-aa.md)
- [Stable exploration](stable-exploration.md)
- [Incremental native terrain — 2026-09-18](streaming-terrain.md)
- [Minecraft terrain-lensing prototype](terrain-prototype.md)
- [Reuse terrain triangle row addresses — accepted simplification](triangle-row.md)
- [Wider native-mesh viewing — 2026-09-18](viewing-range.md)
- [Visual integration reference: feasibility and next gate](visual-reference-plan.md)
- [World feature integration — 2026-09-24](world-features.md)
- [World-entry preparation and gameplay bug bash — 30 September 2026](world-loading-2026-09-30.md)
- [World preparation: failure analysis and optimization](world-preparation-2026-09-29.md)
- [Wormhole demo implementation — 28 September 2026](wormhole-demo-implementation.md)
- [Traversable wormholes in Interstellar: feasibility study](wormholes-feasibility.md)
