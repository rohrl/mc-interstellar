# RTX intersection probe

A standalone Vulkan 1.2 compute comparison. It does not install a Minecraft backend,
change production shaders, or read any saved world. See the
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
