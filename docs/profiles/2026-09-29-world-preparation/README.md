# Preparation evidence

See [the full report](../../world-preparation-2026-09-29.md) for interpretation.
All original raw logs and init scripts remain in the ignored run/preparation-study/.
These extracts preserve milestones/work counts without megabytes of per-frame logs.

- owner-failure: original two capacity failures.
- baseline/fair/candidate/occlusion/complete: staged optimization experiments.
- audit: native renderer called for every proposed enclosed-block skip.
- fresh: regenerated seed with verbose edit tracing and forced FBO copy.
- pearls: actual item placement,1440p movement during capture, passage and copy audit.
- opengl-fresh: final Vulkan-free default run, regenerated terrain, no diagnostic flags.
- grown/fresh-fbo/placeholder/legacy-backends: same-frame GL/RTX checks.
- same-frame-snapshot: same GL frame, legacy voxel/height/light resources versus
  placeholders. Historical filename rtx.png denotes the placeholder variant here;
  both draws are OpenGL. The report header explicitly identifies this diagnostic.
- live-timings: scene/settings and GPU/frame distributions; read comparability limits
  in the full report before treating them as speedup measurements.
- artifacts/restoration: artifact contents/hashes, test count and restored preferences.

The across-reload snapshot comparison was rejected as equivalence evidence because
clouds/populations changed. same-frame-snapshot replaces it with an exact paired check.
