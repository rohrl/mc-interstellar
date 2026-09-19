# Separate AA ray passes — accepted

Keep the accepted two diagonal subpixel rays at offsets(-.25,-.25) and(+.25,+.25), all traversal/optics/materials, and bounded cubic reconstruction. A shorter shader traces one ray per fragment. Two draws use the original logical viewport/interpolated coordinates, with physical viewports occupying two halves of an RGBA32F target. A small fold pass averages both samples into the existing RGBA8 target before reconstruction. This avoids quantizing individual samples and retains the original order of averaging/clamping. Extra temporary storage at1280x720 logical resolution is29,491,200 bytes (28.125MiB), plus one fold pass; no scene coverage or sample count reduction.

Only the normal-settings, compact,16-block-cap program uses this experiment. Alternate settings fall back to existing shaders. X toggles split/serial AA; Shift+X captures a same-frame pair. C still checks the existing diagnostic variant of the unchanged trace/traversal, not the split executable's sample scheduling. Actual scheduling needs image comparisons.

`split-aa-build.log` and `split-aa-final-build.log`:53 tests pass. Final source also checks framebuffer completeness and falls back to serial rendering if doubling the logical width would exceed GL_MAX_TEXTURE_SIZE. Those guards were built after the measured client launch; rendering math/manifests are unchanged. No new confirmed renderer correctness defect found in reviewed scope.

`split-aa-runtime.log`, RTX5070Ti/driver616.92,2560x1440 output/1280x720 logical,6092222 terrain triangles,N65,r_s8.125,120 warmup/300 samples, includes fold and reconstruction:

| Frozen view | Serial GPU p50/p95/p99 ms | Split GPU p50/p95/p99 ms | Split frame p50/p95 ms |
| --- | --- | --- | --- |
| Wall yaw.281/pitch.91 |30.571/31.679/32.158|20.180/20.817/21.060|20.730/21.536|
| Down pitch35.91 |45.624/46.753/47.073|32.920/33.284/33.505|33.654/34.209|
| Down repeated serial then split |45.709/46.834/47.558|32.946/33.330/33.554|33.666/34.150|

GPU p95 gain34.3% wall and28.8% downward. Three same-frame pairs have identical reference/candidate PNG hashes: wall15597452219349976782 (A5AABA0AFF296CC87B66C810E51C94F4E8F47C816B7C64F03E3E8BD5FA4ABF31), down1528177232396356785 (E93B5E971BB9CA5F2A2CA8DF8EBFAD0366C9851112F521765BFAB1DC7E070395), away7204454271998283890 (EA5851A57A9F1CAC400878B8F79722BC7C55BCE4D3F5CC01B92069D5DAF4E431). Downward candidate visually inspected. The unchanged diagnostic variant passes26240 sampled optical checks, zero mismatch/inconclusive/unresolved; actual split executable is covered by image comparisons, not diagnostic classifications.

Live1440p GPU p50/p95/p99:wall21.581/22.318/22.693ms,down33.754/34.700/35.150ms. Frame p50/p95/p99:wall22.506/23.798/24.759ms (~44FPS median),down34.845/36.307/37.068ms (~29FPS). Live actors remain animated; these are evolving scenes, not paired speedup measurements. Player view restored to yaw.281/pitch.91 at original position. Down still needs headroom above30FPS; continue optimization.

Early facing rejection was measured with identical pixels but no useful timing gain and preserved on codex/facing-hints-experiment7f9fad2; see docs/facing-hints.md there.
