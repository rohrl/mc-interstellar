# Native appearance repair — 2026-09-17

The interrupted implementation now builds and runs in F9/F10. The shader startup blocker was a variable named `packed`, a reserved GLSL keyword; renaming it to `lightCode` fixes compilation. Changing its arithmetic alone did not fix the reserved name. See the [Khronos GLSL specification](https://registry.khronos.org/OpenGL/specs/gl/GLSLangSpec.4.10.pdf).

## Appearance changes

- Sample Minecraft's native sky/cloud rendering in six cached 256-square directions. This contains no terrain camera image, so it does not restore the rejected straight-view wall copy.
- Use Minecraft's current lightmap and directional face brightness. Distant columns capture sky/block light; local opaque geometry currently assumes open sky.
- Retain a separate material beneath the top cap, avoiding snow stretched down an entire cliff. One lower material remains an approximation of geological layers.
- Keep the existing local ownership and nearest-hit rules. The optional distant representation remains a finite 256-square height field; code default stays off.

Local `run/config/interstellar-terrain.json` has `enabled`, `distantPrototype` true and `renderScale` 0.5 for owner inspection. Launch the current checkout with the existing launcher. Inspect the mass source if necessary, then F10 toggles live rendering. In F9, H toggles the appearance experiment; P captures a vanilla/zero-bending pair from an unchanged frozen camera in paused singleplayer.

## Numerical image feedback

`AppearanceCapture` saves same-frame world images before HUD/hand, camera/projection/settings metadata and an actual full-resolution, zero-bending backend render under `run/interstellar-captures/pair-*`. It restores the previous preview settings afterward.

```powershell
java tools/CompareAppearance.java run/interstellar-captures/pair-14751648673734232993
java tools/CompareAppearance.java --self-test
```

The Java 21 tool writes RGB MAE, linear-luminance MAE, p95 channel error and fraction over 8/255 for full/top/bottom/centre regions, worst 32-square tile error, a heatmap and a compact contact sheet. It requires equal image dimensions; pair mode requires metadata, but does not yet validate matching scene metadata. Explicit three-path mode can compare arbitrary images. These are diagnostics, without an acceptance threshold or perceptual certification. The projection correction below replaces the initial hard-coded 70-degree FOV; complete camera-effect parity is unfinished.

## Verified checkpoint

- Gradle build and 45 existing tests pass; comparator identity/extreme/patch self-tests pass.
- Actual client startup and F9/H/F10 shader rendering pass. The reported entity-emissive Sampler2 warning was not the terrain shader compilation blocker.
- Fixed snowy-terrain pair above: full RGB MAE 0.0423, linear-luminance MAE 0.0113; bottom-half RGB MAE 0.0605. Visual contact-sheet inspection confirms nighttime terrain lighting and native sky, with remaining geometry/appearance errors.
- Earlier pre-fix pair `3812270522131605506`: full RGB MAE 0.2169, luminance MAE 0.1760. Different simulation times prevent treating the between-run improvement as a controlled A/B result; each individual pair is same-frame.
- Repeat pair `1471452617544092476`: vanilla references are pixel-identical (all comparator errors zero).
- F9 V: distant 93 rays / 21 hits / zero mismatches; local 39x26 flat comparison zero mismatches, 69 flat hits, 61 lensed opaque hits, 2 outside ordinary FOV margin, zero unresolved. No new curved-solver certification is claimed.
- Live F10, 2560x1440 output / 1280x720 internal, standard preset, N64, r/r_s 12.68927, 11700 opaque / zero unknown or unsupported: GPU p50/p95/p99 9.79536/9.90976/9.958112 ms; frame intervals 11.0772/12.0548/12.389 ms (120 warmup, 300 samples).
- Optical GPU timer excludes sky capture, scene capture/upload and upscale; frame intervals include surrounding work. These short scene-specific timings do not establish performance across exploration. Height-field capture was about 1.9 seconds. Its height/appearance buffers total about 4 MiB CPU+GPU per snapshot, excluding local geometry and sky.

Evidence: ignored `appearance-recovery-build.log`, `appearance-recovery-runtime.log`, capture directories and `repeat-recovery` report.

## Remaining work

### Subsequent projection correction — 2026-09-17

Saved metadata exposed a real size mismatch: configured FOV 70, but the actual world projection had m11=1.2571725, giving an effective vertical FOV of 77 degrees. F9/F10 now use the current world frame's perspective scales and offsets instead of hard-coded ray slopes. The GPU diagnostic and independent CPU ray construction use those same camera inputs. Metadata records actual candidate slopes/FOV. Perspective inversion uses x=(NDC_x+m20)/m00 and y=(NDC_y+m21)/m11; this does not reproduce model-view effects such as bobbing/hurt rotation.

The first runtime check caught an accidentally omitted shared vertex-stage Viewport uniform; restored it before the verified build. Final evidence:

- `projection-final-build.log`: build passes (45 existing tests, zero failures/errors). `projection-verified-runtime.log`: startup and F9/F10 rendering pass.
- Snowy downward pose: `pair-12491455709715691085`, actual/candidate FOV 77 degrees, full RGB MAE 0.0492, linear luma MAE 0.0545, bottom RGB MAE 0.0789. Daytime here differs from the earlier nighttime run; do not interpret those cross-run scores as an A/B improvement.
- Near wall, camera (60.5,303.6199998855591,16.5), yaw90/pitch10: `pair-16576722822841254786`, full RGB MAE 0.0446, linear luma MAE 0.0786. Both contact sheets inspected: wall/platform screen positions align; sky gradient/cloud placement, texture filtering/shading and distant geometry remain visibly different.
- V diagnostics: distant 93 comparisons/20 hits/zero mismatches; local 39x26, 71 flat hits, 66 lensed hits, zero flat mismatches or unresolved rays.
- Live far pose, same N64/11700 opaque and 1440p/half-resolution conditions: GPU p50/p95/p99 9.568384/9.685856/9.729184 ms; frame intervals 10.8149/11.6756/12.0231 ms, 120 warmup/300 samples. Same timer exclusions as above. F10 remains on locally, window restored to 870x519. No blocks edited; AA remains isolated.

### Open appearance work

This is a launchable appearance repair, not finished world integration or vanilla parity. Height-field edges, bounded fade, missing caves/overhangs, simplified materials, local ambient occlusion/light sampling, entities/transparency and cloud alignment remain. The sky cache updates every ten world ticks or one block of camera displacement; other dimensions/graphics modes are not comprehensively tested. Next expand the small fixed-pose appearance suite and resolve its conspicuous errors before building the slow lensed quality reference. Owner deferred automated movement/flicker checks and will report those issues. AA remains separate at `8ad46eb` on `codex/terrain-antialiasing`.
