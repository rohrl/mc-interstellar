# Handoff — Faint Doppler detail, 30 September 2026

## Current state

Branch **codex/incremental-refresh**, following **0732557**. See Git for the
current delivery commit. Branches, pushes and autonomous GUI checks remain
authorized. No agents. Read **D103**, **docs/relativistic-sight.md** and
**docs/profiles/2026-09-30-spectral-visibility/README.md** for this change.

Shared observer shader now assumes weak continuous UV/IR tails around the existing
RGB anchors. Full uses actual Doppler D; Gentle retains D^0.06. A hue-preserving
4% display peak floor keeps nearly black detail. Black inputs stay black.
Spectra and exposure are explicitly approximate; no new rays or passes.

Both builds pass 117 tests. Actual GPU fixture passes 50 independent boost
comparisons and 54 colour contracts. Forward/rear 0.99c views inspected;
GL/RTX paired MAEs 0.000383 and 0.0000224 /255. Renderer medians, unadjusted images
and limitations are in the report. These are not before/after performance data.
The first launch after changing shared includes spent about six minutes compiling
GL variants; world preparation then took 23.19 seconds.

## Owner world and test ownership

**Interstellar Final QA now contains owner play changes; preserve it.** Restored
only the requested portal pair from the beginning of that play session:
A=(41,88.00432496543833,.6045897075471959),
B=(161,88.2551570825417,.5), revision14. Other uncompressed portal-save bytes and
all other owner-world files unchanged. Backup of moved pair, saved owner-session
log and exact restoration record: **run/visibility-study/**.

All runtime checks used a new **Interstellar Spectrum QA** copy. Runtime confirmed
the restored coordinates. Client saved/closed. Six owner options/config files
restored byte-for-byte from **run/visibility-study/owner-backup/** (fresh snapshot
AFTER the owner's play session; do not restore older bug-bash defaults over it).
Owner settings include fullscreen, Full Doppler, RTX, 4x AA and mass/portals enabled.

Accepted jars: **run/visibility-study/accepted-opengl.jar** and **accepted-rtx.jar**.
Final build output is RTX. Ordinary jar audited for absent RTX/Vulkan/shaderc
entries. Logs and helper scripts are under the same ignored study directory.
Don't rerun restore-portals.py: the one-shot patch already succeeded.

## Previous completed work / deferred scope

**D102**, **docs/final-bugbash-2026-09-30.md**: retained sufficient RTX sample
capacity when reducing AA. Broad combined gameplay bug bash passed. Compact HUD,
placement preview and curved interaction feedback remain proposals.

**D101**, **docs/incremental-refresh-2026-09-30.md**: bounded worker geometry packing,
direct CPU-to-RTX handoff and combined upload/build. Warmed matched RTX walking
improved mean frame time ~5.3%; OpenGL-only neutral. Rejected trials preserved in
35e72a8. Owner explicitly deferred section storage (~98% whole-column invalidation).
No further optimization is authorized solely by these notes.

Preserve .idea, owner worlds and independent AA work **8ad46ebd**.
JDK: C:/Portable/jdks/temurin-21.0.12.1; Python: C:/Portable/python-3.11.7/python.exe.
Use UTF-8 and LF. Client commands can be dropped during backend setup: wait for
readiness logs. Normal builds exclude optional Vulkan/shaderc; final RTX build
follows normal build. Do not rebuild source sets while a client is still loading.
