# Known streamed texture layouts — accepted

Streamed terrain triangle arenas are4095 texels wide; moving triangle textures are4096. Compile those existing dimensions into a separate shader, removing repeated textureSize/dimension branches from sceneTriangle. Integer index arithmetic, every texel/attribute, traversal, AA and optics stay unchanged. Select only for streamed native meshes with normal settings/split AA; monolithic and alternate settings keep their existing programs. No extra buffers.

Y toggles this specialization; Shift+Y compares to the accepted split-AA shader. C uses a matching streamed-layout diagnostic variant (including the fixed terrain addressing), while actual-program image pairs cover material/moving-tree fetches and AA. Builds fixed-layout-build.log, fixed-layout-final-build.log and fixed-layout-live-build.log pass53 tests. Accepted after the corrected live selection was verified.

RTX5070Ti/616.92,1440p output/1280x720 logical,2xAA,cap16,6092222 terrain triangles,120 warmup/300 samples, all optical/fold/reconstruction passes included. `fixed-layout-runtime.log` matched frozen GPU p50/p95/p99:

| View | Dynamic dimensions ms | Fixed dimensions ms | Fixed frame p50/p95/p99 ms |
| --- | --- | --- | --- |
| Wall yaw.281/pitch.91 |21.522/22.228/22.520|19.638/20.294/20.671|20.199/20.981/21.276|
| Down pitch35.91 |33.040/33.496/33.645|29.389/29.903/30.165|30.042/30.719/30.988|
| Down repeat after diagnostics |—|29.473/30.010/30.237|30.126/30.885/32.005|

GPU p95 gains8.7% wall/10.7% down. Wall pair2482040448657534935 and down13191837356848975683 are pixel-identical. SHA256 respectively036B0645479CB13449FD004AE797941CDBD5657B770A76E7527F86D16379DF96 and667B2CCC0506FEE09758FC7282CEC2EB1182986D1A77468C31FD302F37BBAAF2. Post-diagnostic down16812410278650097837 remains identical with the same downward hash.

At00:48:28 the log explicitly selects `streamed-layout diagnostic variant`;26240 sampled checks pass with zero mismatch/inconclusive/unresolved. The earlier C invocation before enabling lensing selected the original diagnostic and is not evidence for the fixed layout. The earlier live run in this log also selected `native-live-split-16.0`, exposing a new experimental selection bug: it checked the F9-only streamedReference flag. Fixed selection now reads the actual WorldMesh streaming state, covering F10 too. Do not attribute that earlier live timing to fixed layouts. A fresh live launch is being verified in fixed-layout-live-runtime.log. Existing published renderer optics were unaffected by this selection bug.

Corrected live acceptance in `fixed-layout-live-runtime.log`: both benchmarks explicitly select `native-live-layout-16.0`. Wall GPU p50/p95/p99=17.926/18.572/18.883ms; frame18.708/20.585/21.599ms (~53FPS median). Down GPU30.413/31.607/32.465ms; frame31.448/33.502/34.468ms (~32FPS median). Thus representative medians clear30FPS, but slow downward frames remain near/below30 and this is not a universal minimum guarantee. Live actor updates continue, original allocations1/1. Pose restored to yaw.281/pitch.91 at(16.5,302,-45.5). Split-AA framebuffer guards from8f44879 also execute successfully in these clients. Further small headroom improvement worth testing: reuse row calculations for a triangle's nine texels.
