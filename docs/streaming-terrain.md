# Incremental native terrain — 2026-09-18

F10 now retains independently replaceable chunk meshes. Block/light changes queue affected chunks; ordinary movement retains overlapping chunks and captures the new edge. Mobs/clouds keep their separate per-frame mesh. Initial loading is still allowed; teleport support, precipitation and FPS optimization remain deferred by the owner.

## Cache and traversal

Each loaded chunk captures native block-model triangles over full build height in bounded five-millisecond slices, then builds a local BVH. Minecraft's section-render invalidations queue the containing chunk, including vanilla neighbour/light invalidations. Fabric chunk load/unload events also invalidate adjacent chunks. Events coalesce; a revision change during capture discards that incomplete result and retries. Updates replace an entire chunk, not individual sections yet.

A small top-level BVH points to independently allocated chunk BVHs. The shader traverses this two-level structure and the moving-entity/cloud tree with shared nearest-hit ordering. Chunk trees return to the top-level escape link; frozen monolithic trees remain supported. This retains curved geometry and avoids screen-image overlays.

GPU arenas use4095-wide RGBA32F rows, aligned to both triangle and node records. Upload only replacement chunks and the small top-level index. Publish new pointers before reusing old allocations; free adjacent row ranges coalesce. CPU geometry is released after chunk upload. Triangle arena16384 rows plus node arena4096 rows reserve about1280MiB GPU memory, excluding actors/textures/targets. Total terrain remains capped at seven million triangles; render distances above16 are refused. Fragmentation/capacity failures stop explicitly; there is no defragmentation fallback yet.

The camera window remains configured render distance plus one chunk each way. Only loaded chunks are read; unloaded or retired entries disappear from the index. Queued new terrain can be temporarily absent. Initial rendering waits for the requested footprint; subsequent updates retain published geometry until replacement. The existing128-block/source-exterior guards remain. F10 off still releases the cache and reactivation reloads it; reuse across toggles is not implemented.

Source metadata is now independent of the terrain cache. While a selected source refreshes or becomes unavailable, normal rendering appears and the cache is retained. A usable payload updates the optical centre/radius without rebuilding all terrain. Escape bounds include the displacement from the original capture centre. Disconnect/world change/NONE still release resources.

## Inspection side question

Inspection already refreshes selected sources automatically after relevant mass-block edits, with a five-tick debounce and at most four64-cell probe slices per server tick, shared fairly among jobs. The connected cluster is capped at4096 blocks. Connectivity must be rechecked because removal can split it. Initial selection remains manual; removing the selected anchor requires replacement or selection of another block. The new cache ownership removes the subsequent full terrain reload on source refresh.

## Verified behavior

streaming-final-build.log: successful build,50 tests, zero failures/errors. Added allocator reuse/coalescing, fragmentation/failure/bounds checks and top-level BVH pointer/escape tests. One initial test incorrectly assumed leaf ordering along X despite a larger Y extent; corrected to check the pointer set independently of sort order.

streaming-runtime.log: shader/mixin startup; initial terrain6,185,854 triangles/37.091s; native mobs/clouds active. At player(10.5,292,-14.5), verified two test positions were air, then:

- Added lime concrete at(10,292,-8). Only chunk(0,-1) republished,11792→11804 triangles, within the same logged second. Inspected run/streaming-before.png and run/streaming-edit.png. Removal returned it to11792 triangles.
- Added a connected mass block at(13,300,14): automatic N64→65, r_s8→8.125; removal automatically restored N64/r_s8. Both payloads logged `Live source refreshed without terrain reload`. Only affected terrain chunks republished; no new initial capture.
- Both original air states rechecked successfully. No time/weather or existing scene blocks changed.
- Ordinary strafe crossed camera chunk(0,-1)→(1,-1). Window retained702/729 entries and queued27 edge entries; load/light events added further work. The cache continued rendering and animating mobs. This is a functional streaming check, not an automated flicker acceptance run.

Diagnostic timing during that boundary update, RTX5070Ti,427×240 internal/854×480 window, standard paths, r/r_s3.98913,120 warmup/300 samples: optical GPU p50/p95/p99=17.395904/18.435296/18.639392ms; sampled frame intervals19.2831/20.5804/20.9519ms. GPU timing excludes CPU capture/build/upload, native world rendering, sky capture and upscale. These are not target-resolution FPS results; streaming traversal currently costs more than the earlier monolithic scene in similar nearby views.

## Frozen appearance regression check

F9 **M** retains the monolithic reference; **Shift+M** selects the chunk-based reference. Switching between them recaptures without advancing the paused world. **P** captures the usual vanilla/unbent pair. This developer comparison also keeps mobs/clouds frozen.

streaming-final-runtime.log: camera(16.5,303.62,-45.5), yaw0.281/pitch0.91. Monolithic pair7608212941445763645 versus chunk-based pair5953181202543252939. Vanilla references are identical; **candidate PNGs have identical SHA-256 hashes**, confirming exact equality at this tested pose. Both have RGB MAE0.0025 against vanilla, bottom-half0.0041. Contact sheet inspected. Reports: streaming-reference-repeat and streaming-backend-parity under ignored run/interstellar-captures.

Monolithic6,100,970 triangles = streamed terrain6,091,694 + moving9276 (55 mobs/cloud geometry). Streamed frozen capture33.778s. Switched back to voxel view, closed F9 and reactivated F10 successfully for cleanup/reference-path checks. This is one-pose appearance parity, not independent curved-ray convergence certification.

## Remaining limits

Chunk updates are delayed and individually atomic, not a globally atomic world snapshot. Capture/build/upload and index updates still cause frame work; heavy continuous edits can delay publication. Initial native capture and the legacy voxel snapshot still both run. Broader camera access, finer section invalidation, toggle-cache lifetime, allocator fragmentation and independent curved-hit tests remain open. Fluids, special entity layers, precipitation, non-living/block entities and first-person returning-body images retain their prior limitations. AA remains separate.

Stars still come from Minecraft's renderer through256² cube-face captures. Resampling is a plausible contributor to the owner's reported difference; no star change or new star-specific validation was made.
