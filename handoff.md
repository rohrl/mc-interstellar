# Handoff — stronger gameplay gravity and arrow course, 2026-09-24

## Checkout and authorization

Repo C:\work\code\minecraft\interstellar\interstellar; branch codex/stronger-gravity-arrows,
based on01dad4b. Normal implementation, branches/pushes and autonomous runtime testing
are authorized. No subagents. Preserve AA WIP8ad46eb, previous branches and owner worlds.

## Current result

Default gameplay strength/block0.05→0.2. Acceleration cap is7*strength (default1.4),
so the close pull increases fourfold too. Range/optics unchanged. Development config
updated to0.2; explicit existing configs elsewhere retain their values (upgrade docs
explain setting0.2). Version2 arrow course relocates all four original empty stations
once. Edited/stocked old dispensers and occupied destination cells are preserved.

New course: orange(-6,81,1) south, cyan(-22,87,7) east, magenta(-8,82,1) west,
red(1,85,-22) south. Native results: orange579.823deg XZ winding (~1.61 turns),
cyan clears hole at minimum radius4.656 then lands, magenta reverses/captures19ticks,
red captures13ticks. These remain scaled Newtonian gameplay trajectories.

79 tests pass, including RK4 arrow reference and full force/cap scaling. Nearby sheep
lifts/captures; outside-range control retains vanilla fall. All four stations migrated;
protected migration branches reviewed but not separately runtime-fixtured. F10 layout
visually checked. No shader edits; full optical GPU suites were not rerun this time.
Runtime no ERROR/exception/GL_INVALID. Live1440p,half-scale,2xAA,bodyoff,default arrow
view: GPU median19.170ms,frame19.698ms (~51FPS). Not a matched before/after comparison.
205-tick CPU scopes: source0.0465ms/callback,mob0.3499ms/tick,projectile0.0108ms/tick;
include vanilla work and owner mobs. Evidence: docs/profiles/2026-09-24-strong-gravity.txt,
docs/gameplay-gravity.md,D077; ignored strong-gravity-runtime.log and run/strong-gravity-*.

## Restored runtime

Client PID23432, exec74503; query before assuming it still runs. World Interstellar
Calibration, dimension interstellar:arrows. Paused,F10off,world ticks unfrozen,arrows on.
Seven recorded player fields match exactly: feet9.01961962471024/85.93707693404563/
-17.067633313258963; yaw16.948606,pitch21.450026; creative/flying,slot3 lead,health20,
original inventory unchanged (pig egg,bow,arrow,lead,fishing rod,trident,purple concrete,
magma,end rod). Tagged fixtures removed; no source blocks altered. Original outer
window bounds0,0,2560x1440 restored. User may play between turns; capture fresh state.

## Previous features and next work

F10 horizon crossing/interior mass editing remain as in docs/horizon-body-study.md:
static outside1.25rs→falling by1.05rs; presentation frame independent of player motion.
Interior mass-only editing overlay; background core cutoff0.1rs. Returning actual
body images defaultoff (only distorted strips even with8x horizon/spyglass); opt-in
/interstellar-visuals body true remains. Other entity features/native hands stay on.

Original queue: targeted GPU measurements, optical tables, moving-tree reuse/refit.
Owner asked about Kerr effects, mob shadows and RTX only for understanding. Explained
native blob shadows already included; ray tracing uses ordinary OpenGL shaders, not
RT cores; hardware segmented-ray intersections are a plausible larger future backend,
requiring a measured prototype. No new Kerr/RTX implementation authorized/requested.

JDK C:\Portable\jdks\temurin-21.0.12.1. Launch Interstellar.cmd uses this checkout.
Close the identified client normally before another launch. run/restart-client.ps1
and stable-init.gradle use quickPlaySingleplayer; wait Loaded1399 advancements,then
held click595/305 if the join confirmation is shown. Esc needs600ms. Token-efficient:
narrow logs/tests, screenshots only at meaningful visual changes; no repeated optical
validation when optics are untouched. Owner handles movement/flicker feedback.
