# Wormhole acceptance evidence

See [the implementation report](../../wormhole-demo-implementation.md) for scope,
physics, controls, results and limitations. No single scalar image metric is used
as an automatic visual acceptance threshold.

- `mouth-a`, `mouth-b`, `oblique`: same-scene OpenGL/RTX image and timing comparisons.
  Output 2560×1440, sharp 2× AA, logical scale 0.5. `comparison.txt` contains pose,
  settings, full pixel-error histogram and individual timing samples.
- `chart-centre`, `chart-oblique`, `chart-far`: same physical camera rendered in the
  two isotropic charts, with transported look/up vectors. Centreline uses RTX;
  oblique/far use OpenGL. Metadata identifies the reference. This isolates the
  coordinate change from actual movement.
- `fine-far`: default versus finer integration limits at R=96; same geometry,
  sampling and display resolution. Near views already use the conservative limits.
- `bh-small`, `bh-near`, `bh-inside`: final regression pairs after the shared shader
  work. Horizon cases explicitly select the central N64 cluster before capture.
- `timings.txt`: live RTX runs, 120 warm-up frames plus 300 samples each. First runs
  can include queued empty camera-window chunks after travel; second runs have no
  queue. Vulkan GPU time excludes the OpenGL copies/resolve. Frame intervals include
  the live client, VSync and existing 120 FPS cap.
- `runtime-events.txt`: actual GPU reference result, source/backend switches and
  two-way rendered player transfers. Earlier native-region, remote light/edit and
  release/re-entry evidence remains in `../2026-09-28-wormhole-foundation`.
- `normal-build-check.json` and build logs: 88 tests, clean ordinary artifact with
  no optional backend/Vulkan/shaderc classes. RTX source is compiled separately.
- `summary.json`: numerical backend summary and SHA-256 identities of the full raw
  images retained locally under `run`. Contact sheets are small visual summaries;
  numerical metrics use the original images without resizing or alignment.
- `wormhole-mouth-a.png`: final 1280×720 live RTX screenshot, HUD hidden for the image.

Reproduce chart checks with F9, then Ctrl+Alt+G. F9 C runs the independent GPU ray
fixture (2,575 rays). F9 Ctrl+Alt+V enables RTX, then Ctrl+Alt+P compares backends.
In live F10 mode use Alt+F12 to select the backend, Ctrl+Alt+F12 for a paired image
and F12 for timings. In a frozen view, Shift+[ compares finer angular/chord limits.
Analyze a saved chart pair with `java tools/CompareAppearance.java PAIR_DIRECTORY`.

The first near/inside-labelled regression attempts used a different selected mass
and actually ran exterior optics. They are not included as horizon evidence.
Likewise, the exact close-view finer-limit pair is not counted as a convergence
test: the close-view policy already sets those same limits. Corrected informative
checks are retained above.
