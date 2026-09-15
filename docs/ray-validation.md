# GPU ray validation — 2026-09-14

V in the optical lab performs a one-shot 128x72 RGBA32F readback from the production ray shader. It records capture/escape/unresolved outcomes and the sine/cosine of the exit angle. The CPU integrates the corresponding pixel-centre directions with double-precision RK4 at h=0.001; the GPU uses float RK4 at h=0.02. The analytic static shadow angle provides a separate capture check. This compares shared-algorithm implementations, not independent solvers.

## Bug found and corrected

The owner's resized portrait-window tests exposed capture mismatches and large angular differences. At r/rs=8, the old run reported 6 CPU and 6 analytic mismatches, p95 0.01606 rad and maximum 2.504 rad. GUI scaled dimensions are rounded; using them with Minecraft's GUI projection meant the GPU and CPU did not sample identical directions. The optical quad now maps directly to clip space and uses actual framebuffer dimensions. It is independent of GUI scale/projection. The diagnostic uses the same draw routine and shader as the visible scene.

## Results after correction

Every row below sampled 9216 rays, with zero invalid values, CPU outcome mismatches, analytic capture mismatches or unresolved rays.

| Framebuffer aspect | r/rs | Lensing | Escaped rays compared | Angular p95 (rad) | Maximum (rad) |
| --- | ---: | --- | ---: | ---: | ---: |
| 854/480 | 8 | on | 8376 | 3.8233e-6 | 1.2422e-4 |
| 854/480 | 8 | off | 9216 | 1.1281e-7 | 1.6734e-7 |
| 854/911 | 8 | on | 7616 | 4.3812e-6 | 7.2226e-5 |
| 854/911 | 4.629629 | on | 4064 | 9.7000e-6 | 3.5527e-4 |
| 854/911 | 64 | on | 9188 | 2.5717e-6 | 1.1505e-5 |
| 2560/1440 | 8 | on | 8376 | 3.6817e-6 | 1.0235e-4 |

Normal rendering was visually checked after readback in the portrait window. The updated shader's 1440p optical-pass GPU times were p50 1.171296 / p95 1.1784 / p99 1.18144 ms, using the methodology in benchmark.md. No GPU readback is performed during normal rendering. V cancels any current benchmark; diagnostics intentionally synchronize once and may pause the game briefly. Temporary framebuffer/renderbuffer resources are released and previous bindings/viewport restored in finally.

These sparse-grid results do not bound errors arbitrarily close to the capture boundary or prove accurate higher-order images, winding counts, radiance, horizon crossing, or arbitrary terrain. Angular comparison is modulo 2*pi and cannot detect a wrong full winding count. Tests currently cover only the inward-facing camera field of view. Independent horizon-regular integration and targeted near-critical ray tests remain necessary. Existing nine mathematical tests pass.

## Source-selected interior sample — 2026-09-15

STANDARD at r/r_s=0.7067775, falling=true, lookBack=false, lensing=true; framebuffer 854x480, 128x72 diagnostic. Invalid=0, CPU outcome mismatches=0, unresolved=0, escaped compared=1624. Angular p95=9.564955185339465e-5 rad, max=0.005633167300844977 rad (~0.323 degrees). This extends the sampled worst-case beyond prior runs and reinforces the pending near-critical convergence/winding work. The shader was unchanged. Analytic capture mismatch count is not applicable to this falling-frame sample; CPU/GPU still share connectivity classification.