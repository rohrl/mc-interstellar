# Black-hole and relativity feature ideas

These are proposals, not implemented behavior or commitments. Start with features that add interaction without another expensive full-screen pass. The physical models should be validated against primary references, including [Carroll's general-relativity notes, especially the black-hole chapter](https://arxiv.org/abs/gr-qc/9712019), and the project's existing independent ray tests.

| Idea | What the player does | Implementation direction / likely cost |
| --- | --- | --- |
| **Gravitational clock experiment** | Place clocks at different distances, exchange readings, then bring them together and compare accumulated time. | Integrate proper time for a small set of custom clocks. Low rendering cost; define the common coordinate time and observer motion explicitly. |
| **Relativistic machines** | Build a laboratory where custom timers, furnaces or signal gates run according to their local clocks. | Apply clock rates to opt-in mod devices, with bounded server work. Avoid changing the whole Minecraft tick system. This gives gravity a practical engineering use. |
| **Orbit launcher** | Launch instrumented probes with adjustable direction/speed; discover stable, precessing and plunging trajectories. | A timelike geodesic solver for a bounded set of custom probes, with trails and conservation diagnostics. Separate this from changing every mob's movement. |
| **Light-echo beacon** | Fire a flash or pulse and measure arrival times through different paths around the hole. Build a delayed-signal puzzle. | Extend ray tracing with travel time and event-based emitters/receivers. Multiple images become observable echoes. Rendering arbitrary moving history is a separate, larger task. |
| **Tidal laboratory** | Release a small cloud of probes, or a spring-connected test object, and watch relative stretching/compression. | Reuse probe worldlines; derive relative motion consistently. Spring/material failure is a game model and should be labelled separately from the gravitational calculation. No terrain destruction needed. |
| **Lensing observatory** | Aim a telescope at marked beacons, identify multiple images and infer source position/mass from their apparent locations. | Reuse the current lensing plus markers, measurements and a selectable ray-path overlay. Mostly tools/UI rather than another render pass. |
| **Redshift communication** | Compare emitted/received beacon pulse rates and tone or spectral readings at different heights. | Start with instrument readouts and event timing, then optional audio/colour. Do not tint all terrain as a shortcut for a validated spectral model. |
| **Guided horizon crossing** | Ride an instrumented falling capsule and compare local observations with a distant observer's signals. | Already planned. Requires a horizon-safe terrain camera/worldline and carefully separated observer viewpoints; substantially larger than clocks/probes. |
| **Returning view of your own body** | Use a strongly bent path to see the actual player's back/body and investigate image multiplicity. | Already required/planned. Capture the real player skin/body; do not substitute a mannequin. Delayed moving-body images additionally require history. |
| **Observer-speed laboratory** | Accelerate a camera/probe and compare aberration, Doppler shift and gravitational effects. | Already planned as a separate feature. Preserve independent SR/GR scale controls and compose through an explicit local observer frame. |

## Suggested order

Start with **clocks plus a paired comparison instrument**, then **orbit probes**, then a **light-echo beacon**. They turn the current visual demonstration into experiments and useful mechanisms, with bounded CPU work and little additional per-pixel rendering. Keep the existing horizon-crossing and actual-player-body requirements on the roadmap.

## Ambitious later ideas

- **Spinning black hole:** frame dragging, an ergosphere experiment and an energy-extraction demonstration. This needs a Kerr model and new validation; adding rotation to the existing spherical lens is not sufficient.
- **Multiple holes or mergers:** compelling, but a sum of spherical lenses is not a general strong-field solution. Any approximation needs an explicit valid regime; realistic dynamics is a separate research project.
- **Accretion disk and moving-world history:** remain deferred. Both can be attractive, but they add radiative/temporal questions and rendering work before the basic demo reaches its performance target.

The best next gameplay addition is the clock experiment: it introduces a measurable physical quantity, supports comparisons and puzzles, and builds infrastructure for later signal timing and observer motion.
