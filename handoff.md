# Handoff — profiling complete, 2026-09-23

## Authorization and scope

Repo C:\work\code\minecraft\interstellar\interstellar. Follow AGENTS.md; current detailed evidence/ranking: docs/performance-profile-2026-09-23.md. Owner asked for profiling and a revised list, not immediate implementation of the next optimization. Branch codex/performance-profile. Commits/pushes of any branches are authorized; no force push, subagents, unrelated resets or world edits. Keep token-efficient checks and concise updates.

Accepted renderer remains quad production21d33ed on codex/quad-vertices and codex/demo-visual-refinement. A/B49738ba remains on codex/quad-vertices-experiment; AA WIP8ad46eb on codex/terrain-antialiasing and packaging WIP2fe8674 remain separate. No WIP restored. Steps3–4's documented subset complete; step5/deeper relativity needs later discussion. Weather, general teleport and automated movement/flicker tests deferred. Owner accepts a small easier-view regression if the heavy view improves.

## Findings and next proposal

Normal heavy frozen GPU median27.0–27.2ms. Diagnostic omissions: complete moving tree15.9ms; clouds22.0ms; actors23.1ms. All omissions change the image and are developer-only, not accepted optimizations. New priority: separate actor/cloud traversal/cache regions with original tests/composition, then native cloud quads/rectangles and tighter actor bounds. Terrain planar work drops below these. Full revised complexity/size/time/LoC/FPS/quality table is in the report.

Second heavy probe:447M terrain/215M moving node visits;8.30M terrain/65.09M cloud/13.40M actor triangle entries per frame at1280x720 logical,2xAA. Entries include material rejects before full intersection.52 optical steps/ray; slower view integrates fewer steps than wall. Reconstruction~.061ms.739/1,843,200 rays need selective materials, yet those draws cost~3.4ms. Full/selective paths tie here. No hardware stall/bandwidth/register capture, so do not claim that bottleneck is resolved.

Moving BVH CPU build~5.9ms, actor capture~1.5ms; JFR heavy Java samples320/425 contain MeshTree. CPU/GPU overlap: not5.9ms of removable frame time. Clean pre-JFR live heavy GPU27.93/frame29.02ms (~34.5FPS). Last live repeat/wall overlap JFR and are labeled in CSV; don't use as clean FPS evidence. Initial live stage shortcut used Shift and slightly moved the flying camera; final shortcut is Ctrl+F12. Frozen comparisons unaffected. Artifacts: docs/profiles/2026-09-23; raw JFR/JSON/helpers ignored in run/.

## Profiling tools and checks

Opt-in: gradlew.bat runClient -PinterstellarProfile. Only this mode registers9 extra programs. F9 B ordinary timing, Shift+B stages, Ctrl+B work CSVs; backslash cycles normal/no-moving-tree/no-lightmap shader; Shift+backslash cycles normal/actors-only/clouds-only geometry. Both selectors must be0 for baseline. F12 normal live timing; Ctrl+F12 stages. Counters/readback are separate from timing. Diagnostic compilation can take minutes and may warn about eliminated uniforms/samplers; ordinary launches skip it.

Normal quad fixture52,480 comparisons passes with zero mismatches/inconclusive/unresolved;28 material cases pass. Four CSV consistency checks pass. N64 demo/F10 restored view inspected. Instrumented down view flags one exhausted ray, sample1,x509,bottom-origin y554; wall none. Maximum800 optical steps consistent with phi16/angular guard. Reproduce in production output before calling a new regression; don't silently increase optical limits. This pixel is outside the sampled fixture's assurance.

Final build/test/default-launch status: see progress.md and report. No production optical equations, geometry layout, AA, cloud/actor frequency or materials changed. Special additive/glint/text/particles, coplanar overlays, boat water mask and first-person returning body remain limits.

## Runtime and preserved state

JDK C:\Portable\jdks\temurin-21.0.12.1; username InterstellarDev. Launch Interstellar.cmd uses current checkout with profiling off. /interstellar demo enter; F10 after ready. Never two clients on a save; close the identified one normally. All input helpers finish before final response; inspect current processes/logs rather than old exec IDs.

Original pose in run/profile-return-state.txt, restored: interstellar:demo; player8.279904511876886/148.25499529338907/1.3352740328914046,yaw-127.64824,pitch79.05022,creative flying. N64 at0,80,0;854x480. Demo return record preserved: benchmark travel used direct dimension teleports, not demo enter/leave. No blocks/time/weather settings changed.

Logs: profile-runtime.log (first frozen batch), profile-next-runtime.log (second/live/JFR), profile-default-runtime.log (final normal launch). Helper caveat: file existence does NOT mean JFR completed; gate on JFR.check/state or actual stop time. Wait for Loaded1399 advancements plus UI settling; experimental dialog right button relative590,305 at854x480.600ms Escape reliable; short taps may miss. Gate captures/timings on completion; no builds during measurements. Preserve any newer user pose next turn.
