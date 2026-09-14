# Optical pass benchmark — 2026-09-14

Measured on RTX 5070 Ti, NVIDIA 616.92, Minecraft 1.21.1 / Fabric baseline, OpenGL 3.2 context with ARB_timer_query. Fullscreen framebuffer 2560x1440; VSync enabled, configured FPS cap 120, render/simulation distance 12. Disposable Interstellar Calibration world is paused while the lab screen renders. Grid and aligned source enabled. Shader uses 800 maximum RK4 steps of 0.02 radians. No Iris/Sodium installed.

| Scene | GPU p50 ms | GPU p95 ms | GPU p99 ms | Sampled frame interval p95 ms |
| --- | ---: | ---: | ---: | ---: |
| Lensing, r/rs=8 | 1.170912 | 1.178368 | 1.185472 | 8.8747 |
| Lensing off, r/rs=8 | 0.035168 | 0.035680 | 0.035904 | 8.7476 |
| Lensing, r/rs=6.6667 | 1.215776 | 1.228064 | 1.233888 | 8.9054 |

Each run discards 120 warmup frames and records 300 GPU samples. Timestamp pairs surround the optical draw call, using eight outstanding slots. Results are read only when available; there is no glFinish or waiting readback. These are individual short runs, not a thermal/stability study. CPU-side frame intervals include pacing and other work; they are not full-frame GPU measurements or paired with the delayed GPU sample.

The measurements support continuing the direct ray-integration experiment. They do not establish the performance of terrain ray tracing, horizon crossing, spectral transport or the completed mod, nor do they validate GPU ray accuracy. Quantitative ray readback versus an independent solver remains pending.

## Reproduce

Open a world, F8, select the scene and framebuffer size, then press B or click the benchmark text. Do not change settings during the run. Results appear in the lab and run/logs/latest.log. B cancels/clears; other key input, screen closure or resizing cancels and releases query objects. No query objects or timestamp work are created until requested. Logs record resolution, scene, GPU and driver. Keep the window focused and record VSync/FPS limits separately.

Technical reference: [Khronos glQueryCounter](https://registry.khronos.org/OpenGL-Refpages/gl4/html/glQueryCounter.xhtml). Minecraft's 3.2 context requires checking ARB_timer_query as well as the core version; the RTX driver exposes the extension.
