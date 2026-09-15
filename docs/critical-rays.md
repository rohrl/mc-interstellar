# Critical-ray diagnostics

Press **C** in F8 for a bounded blocking stress test. It runs 252 rays: local looking cosine offsets of +/-10^-2 through +/-10^-7 from the analytic critical direction, seven observer cases, and all three integration qualities. Static cases: r/r_s=8, 1.5, 1.05. Falling cases: 8, 1, 0.7067775, 0.35. Input radii and cosines are cast to float before the independent PG reference is evaluated, matching shader uniform inputs. GPU arithmetic still rounds intermediate normalization and slope calculations.

Each ray is drawn to a 1x1 RGBA32F framebuffer through the production solver. Diagnostic mode overrides only its input direction and emits cos(phi), sin(phi), status and unwrapped phi. Normal output keeps alpha=1. The diagnostic restores framebuffer bindings, viewport, renderbuffer and all changed scene uniforms. It pauses playback, cancels timing, and has no readback/reference-integration cost during ordinary rendering. The screen catches diagnostic runtime failures after framebuffer cleanup. V retains the screen-grid comparison and now also logs maximum unwrapped error and full-turn-bin differences.

The independent CPU reference uses tolerance 1e-12 for C. Its analytic sky-connectivity classification is shared with the falling GPU solver. CPU results labelled UNRESOLVED are inconclusive, not ground truth for a wrong resolved outcome. This occurs especially around the photon-sphere boundary, where small cosine offsets can imply much smaller impact-parameter offsets. GPU UNRESOLVED is counted separately from wrong resolved outcomes. Full-turn-bin differences compare floor(phi/2pi); a small error across a bin boundary can cause a bin difference without an entire lost orbit. Raw angle errors remain necessary.

## Retained solver results — 2026-09-15

[Raw 252-ray dataset](critical-rays-baseline.csv), RTX 5070 Ti, NVIDIA 616.92, Minecraft 1.21.1. Status 0=dark, 1=sky, 2=unresolved. No wrong resolved outcomes against resolved CPU references and no full-turn-bin differences among jointly escaped rays in this sample. These statements do not bound unsampled critical rays.

| Quality | GPU unresolved / 84 | Jointly escaped | Largest raw angular error |
|---|---:|---:|---:|
| FAST | 9 | 35 | 0.768653 rad |
| STANDARD | 8 | 36 | 1.032362 rad |
| FINE | 9 | 35 | 1.203488 rad |

Each quality also has two CPU-unresolved references; their outcomes are inconclusive.

These deliberately extreme samples expose large errors close to the critical direction, despite ordinary screen samples being much better. The finite 16-radian budget cannot represent arbitrarily many orbits. Some reference rays exceeding that budget still resolve early on the GPU because of numerical error; an escaped status is therefore not proof of angular accuracy. Fine is not a guaranteed accuracy ordering. Input precision, invariant drift, near-critical instability, finite budget and filtering remain outstanding work.

V after restoring the retained solver, static r/r_s=8 at aspect 0.93970588235: zero invalid/outcome/analytic-capture mismatches or unresolved samples, 7624 escaped comparisons. Endpoint maximum 0.000293089 rad; unwrapped maximum 0.000293678 rad; zero turn-bin differences. Visual check confirmed restored rendering. A normal 2560x1440 STANDARD pass (r=8, aligned source, grid and lensing on) measured p50=1.401792, p95=1.408512, p99=1.411360 ms after 120 warmup frames and 300 samples. Frame-interval p95=8.6323 ms includes cap/vsync and is not full-frame GPU time.

## Reference checks and experiment

28 JUnit tests pass. Three new tests cover the analytic critical cosine, equivalence of exterior frame transformations, both sides of the boundary, unwrapped-angle tolerance refinement through multiple turns, and the logarithmic increase in strong deflection. The float representation of 0.35 is included: the initial helper's overly narrow input check caused a runtime diagnostic failure, corrected to accept the reference solver's radius domain. The world saved normally during that failure. The completed baseline suite ran after the fix.

An algebraically equivalent x=u-2/3 integration was tested against the same suite but not retained: it greatly improved the photon-sphere angular samples and some other cases, while worsening others (e.g. STANDARD r=0.35 had maximum 1.593964 rad). This did not justify replacing the production solver. The final follow-up adds explicit CPU-unresolved accounting; that reporting-only edit was compiled, while the full baseline runtime used the same rays and solver before the extra counter was added. The dataset retains CPU status for independent recounting.

The next implementation priority is a bounded opaque-terrain scene-data prototype. These numerical limits remain visible; they are not a claim of full optical accuracy or a reason to indefinitely defer the playable demo.