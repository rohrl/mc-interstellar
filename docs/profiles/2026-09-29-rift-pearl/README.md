# Rift Pearl UX acceptance

> Historical implementation report or proposal. Results, defaults and pending work describe the recorded checkpoint, not necessarily the current mod. See the [current guides and status](../../README.md).

Minecraft 1.21.1/Fabric/JDK21, RTX5070Ti driver616.92, Ryzen5800X3D,
1280×720. Both builds pass 88 tests; normal artifact has zero optional backend,
Vulkan or shaderc entries. No optical equations or shaders changed.

## Checked flow

- Owner log showed the previous Gameplay pair rejecting Overworld throws.
- Invalid close-player throw preserved that pair. Valid Overworld throw created
  revision11 with one closed end and reported the old pair closing.
- The owner then connected revision12 while exploring. That world was preserved
  separately; further checks used `Interstellar Rift Pearl QA 2026-09-29`.
- Sneak-clear produced revision13 with zero mouths. First actual throw produced
  revision14 with one mouth, with no prerequisite destination.
- Full restart restored revision14 and its closed sphere. Second throw produced
  revision15 with two ends. RTX started automatically without a renderer toggle.
- The crosshair label appeared on the mouth and disappeared behind an opaque
  foreground block. Native depth correctly hid the sphere behind that block.
  The temporary orange wall was removed afterwards.
- Custom sprite visible in hand and hotbar; item renamed without changing its ID.

Screenshots: `closed-mouth.png`, `occluded-label.png`, `connected-rtx.png`.
The floor in these pictures is an actual test platform in the Overworld.
`lifecycle.txt` records revisions, readiness, backend activation and timing.

## Rendering checks

The accepted closed-sphere timing at 10:10:45 used a stationary player on a glass
podium at feet(200.5,296,195.5), yaw0/pitch0, facing centre(200.5,298,218.87674).
120 warmup/300 samples: native sphere GPU p50 **0.009344ms**, p95 0.009504ms;
whole frame interval p50 **8.3452ms** at the120FPS cap. This measures the draw,
not all CPU work or an uncapped FPS gain. Earlier 10:08:57 sampling occurred while
the non-flying player settled onto the floor and is excluded as a fixed-pose test.

The connected same-frame GL/RTX comparison at full resolution,2×AA,fine paths
has RGB MAE **0.0020613/255**; see `connected-gl-rtx.txt`. HUD explicitly showed
RTX and Connected. Neither an OpenGL-only native sphere nor a lone-end first
placement requires Vulkan. Owner quality config matches its pre-test backup.

Original Calibration, Visual Check, and the owner's explored Overnight/Rift Pearl
Check worlds were preserved. Extra test platform and podium exist only in copies.
Closed-marker capture alongside a separate active BH is implemented through the
existing entity-mesh path but was not separately image-compared in this iteration.
