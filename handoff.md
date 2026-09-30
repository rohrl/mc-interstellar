# Handoff — Final bug bash, 30 September 2026

## Current state

Branch **codex/incremental-refresh**, based on **37733f7**. See Git for the delivery
commit. Experimental implementations are preserved in **35e72a8**. Branches,
pushes and autonomous GUI testing remain authorized. No agents.

Read **D101** and **docs/incremental-refresh-2026-09-30.md**. Compact timing,
comparison, build/settings evidence and three images are in
**docs/profiles/2026-09-30-incremental-refresh/**.

Latest follow-up: **D102** and **docs/final-bugbash-2026-09-30.md** record the final
bug bash and gameplay assessment. Fixed AA reductions rebuilding the RTX backend
despite sufficient sample capacity. Runtime 4x→8x→Off→Edge→2x→4x creates only the
necessary initial larger backend; paired images pass, both builds pass117 tests.
No other new blocking fault found in the exercised paths. Compact HUD, placement
preview and interaction feedback are recommendations, not an approved new scope.

Final session used **Interstellar Final QA**, copied from Refresh QA. Saved/closed;
six owner files restored from **run/final-bash/owner-backup/**. Final accepted jars
and logs are in **run/final-bash/** (superseding refresh-study jars for delivery).
QA pair is now (41,88.004,.605) and (161,88.255,.5); source64 remains. The QA player
is back at the lane start. The notes below describe the prior refresh measurements.
For automated GUI checks, wait for the backend-ready log after renderer activation
or capacity/resolution growth; commands sent during that setup can be dropped.

## Delivered choices

- Direct recent CPU geometry to RTX, bounded to one 16 MiB payload; revision/key
  match or GL readback fallback. No handoff payload retained in OpenGL-only mode.
- Combine fitting Vulkan upload and BLAS build in one submission.
- One worker packs quads/builds the search tree. World reads/capture and GPU work
  remain on the render thread. Near edits bypass it. Validate results before
  publication and invalidate a job if its chunk leaves the retained window.
- Reject lighting debounce, position fingerprints/BLAS reuse, and total-pipeline
  budgeting. **Owner explicitly deferred the section-storage rewrite**: roughly
  98% of sampled walking captures followed whole-column content invalidation.

## Verification and limitations

Warmed same-client RTX walking: mean 19.90→18.84 ms (~5.6% more FPS), p95 ~9%
better, p99 ~11% better, ~7% more published columns per leg. Do not repeat the
confounded early separate-launch claim of ~25% FPS improvement.

OpenGL-only is neutral: 64.52 vs 64.46 ms in its heavier local-portal view.
These are matched comparisons within each client, not equivalent scenes across
backends. 700+ direct updates match GL bytes. Paired image MAE 0.00172/255 during
streaming and 0.00199/255 after actual crossing. RTX near command edits publish
in ~23 ms; native pearl relocation, F10 off/on and F3+T recover correctly.

Final late-window worker guard was added after the runtime/timing matrix and
built in both variants; the rare exact race was not forcibly reproduced.
Both delivery builds pass 117 tests, zero failures/errors/skips. Normal jar has
no optional backend/Vulkan/shaderc entries. No shader/quality/range changes.
No universal FPS guarantee, exhaustive multiplayer or low-core CPU testing.

## Preservation and resuming

Client saved/closed. Only **Interstellar Refresh QA**, a copy of the previous
loading fixture, changed. Owner worlds, .idea and AA work **8ad46ebd** untouched.
Owner options and interstellar*.json restored byte-for-byte from
**run/refresh-study/owner-backup/**; hashes are in verification.json.

Final build artifact is RTX. Accepted jars: **run/refresh-study/accepted-rtx.jar**
and **accepted-opengl.jar**. Raw CSV/logs/scripts are under run/refresh-study/;
checkpoint.md there is obsolete. Developer opt-outs and analyzer usage are in
the report. Control mode reset to default; no profiling enabled in normal launch.

QA lane: x=.5, y=66, z=-90.5, yaw/pitch0; W/S18s each. Mass64 at x5..8,
y68..71,z18..21. Relocated portal pair at (641,93.028,.5) and
(41,88.432,.5915). Floating glass launch walls are test fixtures only.
Seed -5073909985471291755. QA player saved in creative at the lane start.

JDK C:/Portable/jdks/temurin-21.0.12.1; Python C:/Portable/python-3.11.7/python.exe.
Use explicit UTF-8 and LF. Final normal build used the optional-free source set;
final RTX build followed it. Preserve ordinary/Vulkan-free packaging.
The larger product roadmap is unchanged; no further optimization is pre-approved
solely by this handoff. Ask the owner what to prioritize next if no new request.
