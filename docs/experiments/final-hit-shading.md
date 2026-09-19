# Final-hit opaque shading — rejected experiment, 2026-09-19

Defer vertex-colour/lightmap shading until the final nearest opaque triangle across terrain and moving trees. Preserve every alpha test, nearest-hit decision, cloud contribution, numerical ray step and AA sample. Retain winning triangle/tree, barycentric weights and albedo, then reload its vertex light data. Compile a separate native candidate so the reference executable remains unchanged.

`final-shade-build.log`:51 tests pass. `final-shade-runtime.log`:candidate passes17600 sampled optical comparisons, zero mismatches/inconclusive/unresolved. Same-frame pair1590503795081838478 is pixel-identical (both PNG hashes206A7804C5CDDF084F701502D76243380374742A9F3EF901CD43F4B7E2CDDDB1).

Paused streamed wall scene, sourceN65/r_s8.125, camera player(16.5,302,-45.5), yaw.281/pitch.91;6092214 terrain triangles.2560x1440 output,1280x720 internal,2xAA,all accepted optimizations;RTX5070Ti/driver616.92;120 warmup/300 samples including resolve. Candidate measured first:

|Program|GPU p50/p95/p99 ms|Frame p50/p95 ms|
|---|---|---|
|final-hit shading|50.148/52.360/53.362|50.764/52.952|
|original native|49.238/51.088/51.794|49.851/51.746|

The candidate is2.5% slower on GPU p95. Reject rather than retain extra state and reloads without a measured gain. The result does not establish a universal regression or identify register occupancy; it is sufficient to reject this candidate in the target scene. No downward/live extension was needed after failing this gate. Next investigate reducing acceleration-node fetches and storage, preserving exact bounds and addresses. The owner's30FPS minimum remains active.
