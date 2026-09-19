# Exact compact nodes

Accepted after the checks below, targeting acceleration-tree memory traffic. Bounds and escape links retain their original32-bit float values. Two adjacent texels hold min/escape and max/header. The header encodes the first triangle or child-tree pointer and leaf kind in a normal finite float, decoded with [ARB_shader_bit_encoding](https://registry.khronos.org/OpenGL/extensions/ARB/ARB_shader_bit_encoding.txt), which specifies GLSL1.50 bit reinterpretation. No bound quantization, reordered triangles, changed intersections or optical steps.

The low27 bits contain a23-bit pointer and4-bit kind; OR with0x40000000 makes a normal finite carrier, avoiding NaN/subnormal transport. Kinds0/1–8/9 denote internal/ordinary leaf/child-tree jump. Kind15 refers to an overflow descriptor preserving an unusually large leaf's exact first/count. The pointer limit covers current triangle and node arenas; out-of-range input fails explicitly.

Keep existing logical addresses (1365 nodes per4095-texel row). The first2730 texels hold compact records; the remaining1365 hold optional overflow descriptors. Thus this experiment targets node-fetch count/locality, **not reduced allocated VRAM**. Both original and compact node textures remain resident for clean same-scene comparisons, adding a268MB streamed reference/candidate mirror. Moving textures reuse their staging buffers. Builders, row allocator and triangle payloads remain unchanged.

F9 F switches complete native programs; Shift+F captures original versus selected in one frame. The reference uses the original shared GLSL branch and original texture layout. Register the compact program and allocate compact world textures only when GL_ARB_shader_bit_encoding is available; otherwise retain the original renderer. The unsupported-hardware path was reviewed/built but not run on a second GPU. Retain dual uploads/textures for now as a measured GPU-speed tradeoff and regression reference; reducing that overhead is a later implementation improvement.

`compact-nodes-build.log`:53 tests pass, including two new codec tests spanning row boundaries, high addresses, exact bound/escape bits and large-leaf descriptors. Runtime evidence follows. The user's30FPS minimum remains active.

## Initial GPU evidence

Candidate passes17600 sampled comparisons, zero mismatches/inconclusive/unresolved. Same-frame wall pair2574873605776832276 is pixel-identical: both PNG SHA256 hashesF98A6BA5F490109527610A3897E921C9FDD0D30095F4E49851A009C5E1033224. Small source/world/render settings remain unchanged: N65/r_s8.125,6092214 terrain triangles, player(16.5,302,-45.5), yaw.281/pitch.91;RTX5070Ti/driver616.92;2560x1440 output,1280x720 internal,2xAA and all earlier optimizations;120 warmup/300 samples, resolve included.

|View/order|Program|GPU p50/p95/p99 ms|Frame p50/p95 ms|
|---|---|---|---|
|Wall,1|compact|43.231/44.246/45.083|43.792/44.861|
|Wall,2|original|48.852/50.483/51.506|49.464/51.218|
|Wall,3|original|48.808/50.784/51.648|49.381/51.381|
|Wall,4|compact|43.322/44.449/45.073|43.936/45.024|
|Down35.91,1|compact|64.823/66.001/66.652|65.636/67.127|
|Down35.91,2|original|80.255/82.063/83.274|81.131/84.336|

GPU p95 improves12.4–12.5% wall and19.6% downward. Frame medians imply~23FPS wall/~15FPS down; still below30FPS. The second wall pair originally attempted arrow input, but synthetic keys lacked scan codes and did not rotate; it is correctly reported as a wall repeat. Actual downward view was visually verified in native screenshot2026-09-19_23.33.43.png after fixing the local helper. Do not label unchanged-camera timings downward. A Gradle build ran during the downward candidate's warmup, before sampled frames; future timings should avoid concurrent builds altogether.

The next build (`compact-nodes-fixture-build.log`,53 tests pass) adds GPU oversized-leaf and cross-row child-address coverage, logs yaw/pitch in benchmarks, and allows same-renderer comparisons after rotating F9. Vanilla comparisons still require the player camera orientation; metadata always records the actual F9 camera. The final fallback build also passes (`compact-nodes-final-build.log`).

## Final validation

`compact-nodes-final-runtime.log`: compact and original native programs each pass26240 sampled comparisons, zero mismatches/inconclusive/unresolved, including oversized48-triangle leaves and child addresses crossing the4095-wide texture's row boundary. Runtime3493/3899ms;340 distinct directions repeated across layouts/flags. This supplements codec tests, not arbitrary-material or all-ray certification.

Both final1440p pairs are pixel-identical, with camera angles recorded correctly:

- Wall pitch.91: pair16281979141588101707, both hashes59B370F1839110726107E943AEDC293A6FED3C2ADF0FAEF1E21DF3518909ABE1.
- Downward pitch35.91: pair18150578009483019108, both hashesBD47C22C50FCC1796D3B106413D75A15E8337A97E152D2B81FDC2002A7B0EFEA. Candidate visually inspected.

Live F10 streamed capture resumes successfully with changing actors over600 updates, different geometry fingerprints and retained1/1 original triangle/node texture allocations. Compact moving textures use the same reusable staging implementation; their allocation count was not separately logged in this run. No renderer failure observed. Camera remains at the wall pose in a small window. Live timing is a smoke test, not a matched1440p gain.
