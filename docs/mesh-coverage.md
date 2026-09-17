# Camera-centred mesh coverage — 2026-09-18

F9 M now captures around the camera, covering the configured render distance plus one chunk in every horizontal direction, over full build height. At render distance12 this requests a 27×27 square, replacing the source-centred 16×16 square. All directions matter because curved rays can turn away from the ordinary camera view. Only loaded chunks are read; no chunks are generated or requested by the capture. Missing sections remain explicit.

The frozen reference retains a hard triangle budget, increased from four to seven million (about 961 MiB for triangle data alone at the cap, separately on CPU and GPU during upload). Capture/build/upload peak memory includes array growth, BVH, native upload staging and other game allocations. This is deliberately expensive quality-reference work, not the live representation or a performance optimization. Configured render distance above16 is refused inside the guarded preview capture loop; dense scenes can hit the triangle budget earlier. Failures are logged rather than silently dropping triangles. Existing 128-block camera guard is unchanged.

## Controlled coverage comparison

**U** toggles CAMERA versus OLD BOUNDS. The latter rejects terrain intersections outside the old source-centred square, keeping clouds, mobs, lighting and the captured world identical. It isolates the effect of missing terrain; it does not reproduce the old capture byte-for-byte (boundary-crossing models, entity population and loaded data can differ). Metadata and timing labels record the switch.

Camera `(16.5,303.62,-111.5)`, yaw0.281/pitch35, effective FOV77, 854×480. Pair `16782299209564047590` CAMERA versus `16674543399717287813` OLD BOUNDS; `coverage-repeat` verifies their vanilla references are pixel-identical. Same-frame vanilla versus zero-bending candidate:

| Region | CAMERA RGB MAE | OLD BOUNDS RGB MAE |
| --- | ---: | ---: |
| Full image | 0.0218 | 0.0297 |
| Bottom half | 0.0206 | 0.0364 |
| Centre | 0.0234 | 0.0237 |

Bottom-half error falls about43%; pixels exceeding 8/255 maximum-channel error there fall from15.24% to8.17%. Contact sheet inspected: the expanded footprint fills visible mountain slopes at the lower edges. Snowfall is active (rain1, thunder0, timeOfDay546891) and absent in the candidate, accounting for conspicuous remaining scattered differences. These numbers cannot be compared directly with the earlier clear-weather cloud checkpoint. No time/weather changes were made to obtain this scene.

## Checks and cost

coverage-final-build.log: build successful, 47 tests, zero failures/errors. coverage-runtime.log: shader startup, expanded capture, paired images, lensed view and cleanup/F10 source adoption/fresh snapshot passed. Inspected run/coverage-lensed.png. Final code additionally moves the unsupported-distance check from the input handler into the guarded capture loop; final client restart recorded in coverage-final-runtime.log.

Measured capture: 6,175,518 triangles, 79 supported mobs, 2,688 cloud triangles; 2,880 missing sections in requested chunks `(-12,-20)..(14,6)`; 191,836 omitted fluid/translucent incidences. Capture/build/upload38.2448s. The extra margin includes unloaded chunks; requesting a footprint is not equivalent to complete coverage.

Lensed diagnostic, RTX5070Ti, 427×240 internal, standard paths, all mesh features ON, r/r_s15.93891, 120 warmup/300 samples: GPU pass p50/p95/p99=20.056448/20.837664/20.888544ms; sampled frame intervals21.2452/22.3663/22.6102ms. Excludes capture/build/upload, sky capture and upscale; no target-resolution FPS claim.

Remaining work: precipitation and transparent/special layers, source/camera access beyond128, incremental live capture and independent curved-mesh convergence checks. Terrain outside loaded data remains unavailable. Keep AA separate and defer automated movement/flicker checks to the owner.

Final-build wall reopening at player (16.5,302,-45.5), yaw0.281/pitch0.91: 6,104,960 triangles, 97 supported mobs, 2,880 missing sections, capture36.5405s. Demo left with camera coverage and lensing ON.
Final wall pair987158325819535368: full RGB MAE0.0223, bottom0.0197. Contact sheet inspected; snowfall remains the conspicuous mismatch. This second pose has no old-bounds A/B comparison and is not a parity claim.
