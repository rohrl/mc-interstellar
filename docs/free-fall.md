# Free-fall sky experiment

The lab now follows a radial observer falling from rest at infinity through the Schwarzschild horizon. It remains an illustrative sky at infinity: no terrain, player body, accretion disk, spectral transport or matter collapse is rendered.

## Observer and reference equations

Use r_s=c=1 and ingoing Painleve–Gullstrand (PG) coordinates, with a=1/sqrt(r) and metric ds^2=-dt^2+(dr+a dt)^2+r^2 dOmega^2. A freely falling orthonormal frame has e_0=(1,-a,0), e_r=(0,1,0), e_phi=(0,0,1/r). Its worldline has dr/dtau=-a and t=tau. These coordinates and observer choice follow [Hamilton's free-fall metric](https://jila.colorado.edu/~ajsh/courses/bh/schwp.html) and [Hamilton & Lisle, The river model of black holes](https://arxiv.org/abs/gr-qc/0411060).

Implementation derivation: a past-directed ray with local looking angle psi has k=-e_0+cos(psi)e_r+sin(psi)e_phi. For backward coordinate time s=-t, the independent reference integrates:

- dr/ds = cos(psi)+a
- dphi/ds = sin(psi)/r
- dpsi/ds = -sin(psi)*(1+1.5*a*cos(psi))/r

FreeFallRay uses adaptive Dormand–Prince 5(4), angular absolute tolerance and radius-relative tolerance. The impact parameter b=r*sin(psi)/(1+a*cos(psi)) is monitored. At a distant radius at least max(128,16*b,2*r_initial), a 64-panel Simpson integral of b/sqrt(1-b^2*u^2*(1-u)) from u=0 to 1/r adds the remaining asymptotic angle. This avoids treating the finite integration endpoint as infinity. Diagnostic tolerance is 1e-10. The test suite also compares refinement and impact conservation.

For static exterior comparison, transform the local cosine via mu_fall=(mu_static-a)/(1-a*mu_static). This changes the observer frame while retaining the same geometric ray. The reference agrees with the original exterior implementation in tested cases.

## GPU path and boundary conditions

The GPU retains the planar spatial equation u''=1.5*u^2-u, which is regular across the horizon for nonradial spatial paths. In the falling frame its initial derivative is u'=-u*(mu+a)/sqrt(1-mu^2). The radial case is handled separately. Unlike the static renderer, it does not stop at u=1. Reference: planar null orbits in [Bruneton section 3](https://ebruneton.github.io/black_hole_shader/paper.pdf); the falling-frame initialization above is derived for this implementation.

The future-directed ray's conserved energy is E=1+a*mu and angular momentum L=r*sqrt(1-mu^2). Using E, b=L/E, radial direction and the critical b=3*sqrt(3)/2, classify connection to our asymptotic sky. Nonconnecting rays have a dark boundary condition. The CPU and GPU share this analytic classification, so outcome agreement alone is not an independent check of that classification. Static analytic shadow checks and the closed-form horizon boundary mu=(1-6.75)/(1+6.75) provide additional tests. The reference labels a critical ray approaching the photon orbit unresolved.

This is a stationary vacuum geometry with illumination from one asymptotic sky and all other past boundaries unilluminated. It is not a simulation of a star collapsing or a luminous white hole/other universe. Looking back from inside receives earlier external light; it does not mean a future-directed signal can escape the horizon. RGB values remain illustrative, without Doppler/gravitational brightness or colour transport. Finite angular budget and point sampling still limit fine higher-order images.

## Controls and playback

F8 opens the lab. F switches static/free-fall frames. T starts/pauses falling playback. H selects a paused falling frame exactly at r/r_s=1. L looks back/outward and can be used during playback. Up/Down scrub radius; R resets. B and V pause playback for measurements. H is a paused visualization, not an observer physically hovering at the horizon.

Playback uses r^(3/2)=r_initial^(3/2)-1.5*Delta_tau, at one dimensionless proper-time unit per playback second. Long frame gaps are capped at 0.1 playback seconds. The tour stops at r/r_s=0.35, an explicit presentation cutoff, not a surface or a rendered singularity. Tidal injury, physical Minecraft motion and singularity physics are not modeled.

## Executed checks (2026-09-14)

Build passes: 15 JUnit tests total, including independent exterior agreement, radial interior sky connection, horizon continuity at 1 +/- 1e-6, the closed-form horizon capture boundary, tolerance refinement, critical-orbit branch handling and invalid inputs.

Actual GPU comparisons each sampled 128x72 rays at aspect 854/480. All had zero invalid values, reference outcome mismatches or unresolved rays. Static analytic mismatch count is applicable only in static mode.

| Frame | r/r_s | View | Escaped samples | Angular p95 rad | Maximum rad |
| --- | ---: | --- | ---: | ---: | ---: |
| Static | 8 | inward | 8376 | 3.8233e-6 | 1.2422e-4 |
| Falling | 8 | inward | 8824 | 3.0644e-6 | 5.2636e-5 |
| Falling | 1.0767035 | inward | 3624 | 1.4366e-5 | 3.4229e-4 |
| Falling | 1 | inward | 3288 | 1.7206e-5 | 5.3980e-4 |
| Falling | 0.8972529 | inward | 2796 | 3.4648e-5 | 2.3832e-3 |
| Falling | 0.8972529 | outward | 9216 | 3.3230e-7 | 5.3724e-7 |
| Falling | 0.35 | outward | 9216 | 2.9683e-7 | 1.1230e-6 |

Maximum sampled error is about 0.137 degrees near the shadow edge. This is not a bound for arbitrary critical rays. Comparisons remain modulo 2*pi, not full winding-count validation.

Automated runtime checks covered exact-horizon display, above/below views, look-back mode, complete T playback from 8 to the cutoff, and normal rendering after V. The agent operated the client; user attendance was not required. Interior inward rendering at r/r_s=0.8973 and 2560x1440 measured GPU p50=0.680544, p95=0.702272, p99=0.715264 ms using the existing benchmark protocol. This is one optical-pass run, not completed-mod performance.

Additional runtime checks: the inward view at r/r_s=0.35 has zero sampled sky rays and no outcome mismatches. Switching F back to static clamps the radius to 1.05; that view also has no sampled sky rays. The HUD says no sky rays; NaN angle statistics in the log mean an empty angular-comparison set, not invalid GPU data. R restored the default view.
