# Wider native-mesh viewing — 2026-09-18

## Current limit: 512 blocks (2 October 2026)

The owner requested twice the visibility distance. Client rendering and server
automatic source selection now share a 512-block limit. Selection retains its
existing source to 576 blocks to avoid switching repeatedly around the boundary.
Manual inspection still requires a loaded target within 128 blocks.

This extends the optical viewing guard without increasing Minecraft render distance,
terrain capture radius, GPU arenas, scan budgets, or forced chunk loading. Source
chunks must still be loaded; horizontal travel can unload them before 512 blocks.
Beyond the old cutoff the optical pass now remains active, so that region can cost
more than its former ordinary Minecraft rendering. No universal FPS guarantee.

Both builds pass 117 tests. In a new Range QA copy, a 512-block source was visible
at 500 blocks. Moving past 512 paused the view; returning resumed it. Moving beyond
576 and returning to 500 reacquired the source automatically. GL/RTX paired image
MAE at 500 was 0.000275/255 (four pixels exceeded 16/255 maximum channel difference).

Fixed spectator camera, looking downward, 1280x720 output, 50% optical resolution,
4x AA, RTX: median frame interval 14.531 ms at 252 blocks and 14.431 ms at 500
(approximately 69 FPS); Vulkan update/render medians 4.814 and 4.969 ms. Each has
120 warm-up frames and 300 samples. These compare two distances with the same
4,744,440-triangle scene, not old/new binaries. The first falling-camera measurement
was discarded. Vertical poses keep source chunks loaded without raising render
distance. Logs, screenshot and jars are in ignored `run/range-study/`.
Client saved/closed, owner settings restored byte-for-byte; owner worlds untouched.

## Original 256-block implementation and evidence

F10 now permits camera distances up to256 blocks from the selected source, replacing the old128-block guard. The streaming footprint continues to follow the camera independently; the change does not enlarge its GPU arenas or force-load source chunks. The HUD shows current camera distance and the256-block limit. Source availability and the exterior limit1.05r_s still pause/recover automatically.

F9 between128 and256 blocks automatically selects the native chunk-based reference, starting with lensing OFF; Space enables it and P captures a vanilla/unbent pair. At these distances M cannot disable mesh rendering and expose the older bounded voxel/column path. Inside128, existing F9 controls are unchanged. Initial manual source inspection still requires a target within128 blocks; select it first, then move away.

The optical equations, chord sizes, escape bounds and ray budgets are unchanged. This remains a bounded prototype: there is still a hard camera limit, missing/unloaded terrain remains unavailable, and source unload can pause rendering before that limit. No lensing fade or ordinary-camera background overlay was introduced.

## Checks

viewing-range-build.log: successful build,51 tests, zero failures/errors. Added CPU ray checks at r/r_s16,24,32,64,128, testing impact parameters1% inside/outside the analytic capture boundary; expected capture/escape and invariant error below1e-7 pass. These support the underlying model and do not independently certify the GPU's curved mesh intersections.

viewing-range-runtime.log: activated F10 at player(16.5,302,-108.5), then flew backward normally through the old128-block boundary to about148 blocks. Streaming retained702 entries at each crossed chunk boundary; lensing stayed active. Inspected run/viewing-range-live.png: black-hole silhouette and curved wall remain visible. This is a functional boundary check, not an automated flicker test.

F9 then automatically captured the native scene at camera(16.51054,303.62,-132.42200), yaw.281/pitch.91. Pair17029190025041604486: full RGB MAE.0005, bottom-half.0006, centre.0011 against vanilla; only.27% of pixels exceed8/255 maximum-channel error. Contact sheet inspected. Nighttime scene; this is one-pose unbent appearance evidence, not perfect sky/star parity. Frozen capture6,056,638 terrain triangles/36.08s,65 supported mobs. Returned to F10 successfully.

Diagnostic live timing during edge updates, RTX5070Ti,427×240 internal/854×480 window, standard paths, r/r_s18.52166,120 warmup/300 samples: optical GPU p50/p95/p99=20.006592/22.724896/22.893312ms; sampled frame intervals21.7232/24.4383/24.9967ms. GPU measurement excludes CPU streaming/upload, native world rendering, sky capture and upscale. No target-resolution FPS claim.

No scene block/time/weather/population edits. Rain/snow, teleport support and AA priorities are unchanged. Stars unchanged. Next broaden coverage/curved-ray confidence and continue demo packaging, rather than claiming the new limit completes world integration.

Outer-limit check: positioned the camera at r≈253.998 using a vertical test-pose command while retaining the same horizontal footprint. Inspected run/viewing-range-outer.png: HUD254/256 and a visible lensed silhouette, with no ray-budget diagnostic colour. Normal upward flight reached r≈263.514 and logged `Live terrain paused: Beyond 256-block viewing range`; normal descent returned to r≈252.478 and logged `Live terrain resumed`. No new initial capture occurred. Pose commands are test setup, not a v1 teleport-support claim. Restored the farther demonstration pose with F10 active.
