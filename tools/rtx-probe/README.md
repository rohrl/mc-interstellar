# RTX intersection probe

A standalone Vulkan 1.2 compute comparison, with an opt-in native capture and
in-game sharing test described below. It does not install a production backend
or parse saved-world files. See the
[measured report](../../docs/rtx-probe-2026-09-24.md) before interpreting its numbers.

On Windows x64 with Java 21 and a Vulkan ray-query-capable discrete GPU:

```powershell
tools/rtx-probe/run.ps1
# Optional: -Jdk C:\path\to\jdk-21
```

Close Minecraft normally first. The runner refuses to compete with the development
client, downloads five pinned LWJGL 3.3.3 jars from Maven Central into ignored `run/`,
verifies SHA-256 hashes, compiles, and runs. No Vulkan SDK or Gradle changes are needed.
LWJGL and shaderc retain their upstream licenses/notices inside those distributions;
the jars are not vendored. Results replace `run/rtx/results.jsonl` on each invocation.

## What is measured

- Two deterministic block-like scenes: exposed column tops/steps, 128 actor boxes,
  and 32 cloud-shaped boxes. Everything is opaque. These are synthetic geometries.
- The same triangle array supplies both implementations. Software uses a longest-axis
  midpoint BVH, eight-triangle leaves, preorder escape links, full-float bounds and
  two-sided Möller–Trumbore tests. This is a simplified compute baseline, **not the
  production OpenGL shader**. Hardware uses a fast-trace BLAS and one-instance TLAS.
- Three deterministic query sets per scene: random 4-block segments, random 16-block
  segments, and a replay of chords from CPU-integrated Schwarzschild paths.
- Replay starts at `(0,12,36)`, with source `(0,12,0)`, `r_s=8`; a hovering optical
  frame, RK4 angle step `0.02`, and at most 16-block chords. It cycles a permuted
  128×128 direction grid until the query buffer is filled. It does **not** stop
  subsequent queries after a preceding chord hits terrain. The CPU integration and
  query generation are outside the GPU timing.
- Each timing brackets 16 dispatches of 262,144 queries. Write-after-write barriers
  serialize reuse of the result buffer. GPU timestamps include these barriers and
  dispatch overhead; the reported value is divided by 16. There are 12 warmup pairs
  and 30 measured pairs, alternating which implementation runs first.
- Buffers prefer host-visible, coherent, device-local memory. The measured card used
  a type with all three properties. Repeated queries deliberately measure warm data.
- Results are read back before warmup. Every query's hit/miss and distance is compared;
  128 sampled queries per scene/workload also use brute-force double-precision CPU
  intersections. Distance tolerance is `1e-4 + 2e-5 * abs(t)` blocks.

Unexplained result discrepancies abort timing. A hit/miss disagreement is separately
classified as an edge case only if the hardware answer matches the double reference
and the putative hit is within `1e-5` world units of a triangle plane and `1e-5`
barycentric units of an edge. Those cases are printed, **not hidden or removed from
the timed workload**. The measured run contained five such comparisons. This rule
documents finite-precision behavior; it does not certify all possible queries.

## Deliberately excluded

Textures/alpha tests, translucency, shading, live BLAS updates, terrain streaming,
OpenGL interoperability, production quad packing, empty-space certificates, chunk
hierarchies, ray integration register pressure, and end-to-end frame scheduling.
The test therefore supports a backend feasibility decision, not a predicted FPS.
No Vulkan validation layer was installed for this run; correctness evidence comes
from actual hit readback, sampled CPU references and API return checks.

Primary API references: [Khronos ray queries](https://docs.vulkan.org/samples/latest/samples/extensions/ray_queries/README.html),
[Vulkan ray-tracing guide](https://docs.vulkan.org/guide/latest/extensions/ray_tracing.html),
[LWJGL bindings](https://www.lwjgl.org/).

## Native capture and interop milestone

The [real-scene report](../../docs/rtx-native-feasibility-2026-09-28.md) covers
`NativeReplay.java`, `native-query.comp`, the capture shaders and the optional
`client/.../RtxInteropSmoke.java`. Ordinary builds exclude the latter and its Vulkan
dependency. The standalone replay still uses the hash-checked jars above.

### Capture actual renderer work

```powershell
./gradlew.bat runClient -PinterstellarRtxProbe
```

Use a disposable scene or record the owner's original state before testing. Capture
requires SSBO and 420pack support; the tested sharing path requires Windows external
memory/semaphore extensions. Diagnostic Vulkan setup uses a 512 KiB LWJGL stack.

1. Use an ordinary exterior BH source, then F9 for the frozen terrain screen.
2. Shift+M selects the streamed native scene and initially disables lensing; Space
   restores lensing. Wait for `Streaming terrain ready:` in the log.
3. Keep default selective, split-moving, shared-quad settings. **Ctrl+Alt+R** records
   native geometry, atlases, actual chords and paired colour checks. It pauses briefly
   for GPU readback and file writes; it is never a live performance measurement.
4. Rotate with the arrow keys, then record another view. Geometry is reused within
   that screen. Keep geometry/material settings and frozen world state unchanged
   between captures; reopen F9 for a different scene. Large captures consume about
   1 GB each; use a small number of views per replay process.
5. Completion logs name `run/rtx-native/capture-.../view-...`. Missing completion,
   buffer overflow, nonfinite data or colour differences invalidate that capture.

Historical recordings used Ctrl+Alt+B; the final shortcut avoids Minecraft's global
Ctrl+B narrator action. The final shortcut/metadata-only edits were build-checked.

### Replay, with Minecraft closed normally

```powershell
tools/rtx-probe/run-native.ps1 -Scene run/rtx-native/capture-TIMESTAMP
```

The runner refuses an open development client. It builds three native-geometry
BLAS plus a TLAS, a separate software BVH, then replays each view in opaque-acceptance
and alpha-aware modes. Results use unique `replay-results-TIMESTAMP.jsonl` files;
unexplained hit/reference discrepancies suppress that case's timing. Retain the log
as well: it has CPU/GPU setup costs, tree masks and correctness counters.

The software baseline is a flat triangle BVH, not the production chunk/quad shader.
Recorded empty-region masks are consumed without paying to create them. Query inputs
are compact and warm; no complete optical frame or live update is timed.

Small reproducible material fixture (no Minecraft world needed):

```powershell
py -3 tools/rtx-probe/native-fixture.py
tools/rtx-probe/run-native.ps1 -Scene run/rtx-native/material-fixture
```

Fixture timings are not performance evidence. It checks candidate acceptance and
analytic distances, not final colour compositing. CPU checks share the software BVH
but use double-precision intersections. See the report for boundary/tolerance rules.

### In-game image sharing

In the opt-in client, **Ctrl+Alt+I** in F9 runs the independent Vulkan/GL test, even
before terrain capture is ready. It does not need an exported scene. Results appear
under `run/rtx-native/interop-TIMESTAMP/interop.txt`; errors are logged explicitly.

The test matches device UUIDs, imports a Vulkan RGBA8 image and two Win32 semaphore
handles into GL, clears/blits with ownership round trips, and checks sampled pixels.
Win32 exported handles are closed after import. Framebuffers, texture bindings,
pixel pack/unpack state and scissor state are restored. No Vulkan validation layer
is assumed. Clear/blit results are not full-backend timing or long-duration validation.

Return to the ordinary client with `./gradlew.bat clean build` and
`./gradlew.bat runClient` without the property. Restore any changed player/test state.

## Complete frozen-image backend

The subsequent experiment uses `./gradlew.bat runClient -PinterstellarRtxImage`.
In a ready default native frozen exterior BH scene, **Ctrl+Alt+V** initializes/toggles
RTX and **Ctrl+Alt+P** saves paired complete images and synchronized timings.
Arrow keys retain the scene; size/settings changes invalidate it. F10 stays OpenGL.
Use the paired timing rather than GL-only B timing, which can miss external work.

This flag includes `src/rtx/java` and the probe allocation helpers; ordinary builds
exclude them and Vulkan/shaderc dependencies. It is an experimental development
configuration, not a finished optional-backend installer. See the
[full-image report](../../docs/rtx-full-image-2026-09-28.md) for measurements,
source-sharing architecture, memory costs and remaining limits.

## Live world backend

`./gradlew.bat runClient -PinterstellarRtx` adds live exterior-BH rendering. F10
selects RTX when supported; Alt+F12 switches to/from OpenGL, F12 benchmarks the
active path, Ctrl+Alt+F12 records paired images. Extended sources and near/inside
horizon views retain GL automatically. Ordinary builds exclude this backend and
its Vulkan/shaderc dependencies. No new distribution flavour is packaged.

See [live implementation and regression checks](../../docs/rtx-live-world-2026-09-28.md).
