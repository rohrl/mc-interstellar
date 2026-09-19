# Normal-setting shader specialization — accepted

Compile a separate native compact-node program with ordinary settings fixed to their existing values: entities/clouds/coverage/lensing/native sky/native lighting enabled,2xAA, adaptive steps/fast bounds/addressing/empty cache enabled, cache reach1024, diagnostics off. Camera/source/geometry/fog/lightmap/path-step remain live inputs. No numerical equation, geometry or quality setting changes.

Select it only while every fixed setting matches. Changing any relevant toggle automatically selects the accepted dynamic compact shader; noncompact/general paths remain available. F9 D toggles the specialization; Shift+D compares whole programs in one frame. Existing F/S controls retain their purposes. Extra shared-GLSL branches use compile-time constants so the dynamic reference remains the original implementation.

Diagnostic0 enables dead-code removal including diagnostic hit cells and their normals. Consequently C uses the compact diagnostic variant when this new program is selected. Its sampled CPU/GPU classifications do **not** directly test the specialized executable; actual-program acceptance requires same-frame output comparisons and runtime timings across views, with existing independent optical evidence applying to the shared equations. Do not describe C as a direct specialized-program fixture pass.

`live-defaults-build.log`:53 tests pass. Runtime `live-defaults-runtime.log` supplies the compilation, image, timing and live evidence below. Accepted on top of compact-node checkpoint82630f1. The30FPS minimum remains active.

Initial matched wall result:6092222 terrain triangles,97 supported mobs,12900 moving triangles, sourceN65/r_s8.125, player(16.5,302,-45.5), yaw.281/pitch.91.2560x1440 output,1280x720 internal,2xAA, standard/adaptive path and all accepted defaults;RTX5070Ti/driver616.92;120 warmup/300 samples including resolve. Specialized GPU p50/p95/p99=40.205/41.423/42.121ms; dynamic compact reference48.782/50.242/51.250ms. Frame p50/p95=40.899/42.169 versus49.499/51.028ms. Matched p95 improves17.6%; different actors from earlier sessions, so do not use earlier compact timings as this baseline.

Same-frame wall pair8601711111469170298 is pixel-identical: both PNG hashes0B3455674B8727CE1ABA86F3ED5A2053A9AE667B7372FC15CB146454414460A6.

Downward pitch35.91, same paused scene/settings: dynamic compact GPU p50/p95/p99=70.157/71.616/72.667ms, specialized58.572/60.129/60.711ms (16.0% p95 reduction). Frame p50/p95=71.020/73.270 versus59.426/61.189ms. Downward pair12163040637725769906 is pixel-identical; both hashesA87FC0CF7880474BBC4CE0C3DF7B959AAEC8743F934C5183CF76FA1634D612DB. Candidate inspected. Current specialized frame medians imply~24FPS wall/~17FPS down, below30; do not multiply cross-session gains or claim60FPS.

C explicitly runs the compact diagnostic variant:26240 checks, zero mismatches/inconclusive/unresolved,3520ms. Post-C pair18251957346657710241 has the same two hashes as the prior downward pair, confirming restored appearance. These classifications test the diagnostic variant; the specialized executable is covered by actual image/runtime checks.

Live F10 runs program=native-live-defaults with600 changing actor updates and retained1/1 original texture allocations. Small854x480-window GPU p95=11.598ms, frame median11.963ms; no matched live/1440p gain claimed. No new renderer failure. Next isolated candidate: raise the distant segment-length cap only where the existing curvature tolerance and0.02radian limit already permit it, with fresh optical/reference-image validation.
