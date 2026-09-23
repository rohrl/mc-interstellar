# Handoff — original performance proposals1–4 tested, 2026-09-23

## Current checkout and scope

Repo C:\work\code\minecraft\interstellar\interstellar, branch codex/performance-four-results. Production source is exactly accepted e324e84. Follow AGENTS.md and the token-efficient workflow. Edits, branches, commits, pushes and autonomous runtime tests authorized; no force push, subagents, unrelated resets or world edits.

The user goal is the ORIGINAL first four proposals from the revised priorities before later reranking: separate actor/cloud roots; cloud shared quads; per-actor hierarchy; conservative compressed bounds. All four have been implemented/tested and have keep/reject decisions; final normal-launch/state-restoration audit passes. See docs/performance-experiments-1-4.md; later numbering does not substitute optical tables/refits for original items3/4. No required experiment remains; no further optimization started.

## Decisions and preserved branches

1. Accepted separate moving roots e324e84, codex/separate-moving-trees:4% less demanding-view GPU time (~4.2% throughput),3.46% less wall time; exact heavy pairs. Actor/cloud roots share the original traversal/cache, skipping consumed cloud forest. Live~39FPS was a specific earlier population, not a new cross-session gain. docs/moving-trees.md.
2. Cloud-face intersection trials9815c42, codex/cloud-quad-intersections: both effectively tied, rejected. docs/cloud-quads.md.
3. Per-actor hierarchy7a168fb, codex/actor-hierarchy: down24.446ms baseline/24.438ms candidate, wall14.614/14.649. No useful gain. Moving visits-.18%, actor entries slightly higher. Build61 tests,52,480 optical and112 material checks pass; heavy PNG pairs identical. Native ownership handles delayed quad commits; all original payload retained. docs/actor-hierarchy.md.
4. Packed bounds e62a71c, codex/quantized-node-bounds: down24.514/29.423ms (+20.03% time), wall+17.1%; rejected. Outward1/16-block signed16-bit boxes with exact float overflow fallback and deferred descriptors. Bounds fit one integer texel but original node arena remains, so no total VRAM saving. Node visits rise<.5%; hardware cause not isolated. Build62 tests,52,480 optical and168 material checks pass; heavy PNG pairs identical, contacts inspected. docs/quantized-node-bounds.md.

All experimental branches above are pushed. Current branch contains reports/evidence only, no rejected runtime overhead. Updated estimates: docs/performance-profile-2026-09-23.md. Tests remain sampled: known counter exhaustion sample1,x509/y554 bottom-origin persists in both baseline/candidates; production reproduction remains open. General shared-edge rounding, coplanar actor ordering, special additive/glint/text/particles, boat water masks and returning player-body coverage remain limits.

AA WIP8ad46eb on codex/terrain-antialiasing; quad A/B49738ba, accepted quad21d33ed and packaging WIP2fe8674 remain preserved. Step5 needs discussion; weather, teleport support and automated movement/flicker tests stay deferred. No changes to optics, AA, cloud geometry or live update cadence in the normal checkout.

## Final runtime verification

Restored clean build/packageDemo passes59 tests. Normal profiling-disabled launch four-default-runtime.log verifies N64/F10, no renderer errors or experimental programs, and exact saved state. The initial helper missed the world-confirmation click and ended; the same live client continued after a180ms held click (no restart), then run/four-default-continue.ps1 completed. No input helper remains running. Saved and queried pose in run/actor-return-state.txt (same as cloud-return-state.txt): interstellar:demo;40.43597468465466/65/-3.0021334600889626; yaw60.452244,pitch-9.900009; flying0. Older split/profile return files are obsolete. Source N64 at inspect0/80/0. Window854x480. Blocks/time/weather/demo return record untouched. Client is paused with F10 ready. Compact final evidence: docs/profiles/2026-09-23-four-final.txt.

JDK C:\Portable\jdks\temurin-21.0.12.1. Launch Interstellar.cmd uses current checkout. Normal close/wait before restart; never two clients on a save. run/actor-* and run/bounds-* scripts are ignored test helpers, do not reuse stale PIDs or run inputs concurrently. Cold shader compilation may take minutes. Focus title bar before world confirmation; native creative-flight double-taps must occur within one helper process. Counter readback, Java image analysis and builds stay outside timings. Read full metrics.json (rounded console summaries can hide tiny differences); never pipe Java to Select-Object -First. Large PNGs remain under ignored run/interstellar-captures; compact evidence is committed under docs/profiles/2026-09-23-{actor-hierarchy,quantized-bounds}.
