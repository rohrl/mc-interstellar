// Small teaching examples. These are independent of the game's renderer.
(() => {
  const impact = document.getElementById('impact');
  function drawRay() {
    const b = Number(impact.value), h = 0.004;
    let u = 1 / 8, angle = 0;
    const sin = b * u * Math.sqrt(1 - u);
    let v = Math.sqrt(1 - sin * sin) * u * Math.sqrt(1 - u) / sin;
    const points = ['M702 220'];
    let status = 'iteration limit reached';
    const acceleration = x => 1.5 * x * x - x;
    for (let i = 0; i < 10000; i++) {
      const a = acceleration(u);
      const bu = v + h * a / 2, bv = acceleration(u + h * v / 2);
      const cu = v + h * bv / 2, cv = acceleration(u + h * bu / 2);
      const du = v + h * cv, dv = acceleration(u + h * cu);
      u += h * (v + 2 * bu + 2 * cu + du) / 6;
      v += h * (a + 2 * bv + 2 * cv + dv) / 6;
      angle += h;
      const radius = 34 / Math.max(u, 0.001);
      points.push(`L${(430 + radius * Math.cos(angle)).toFixed(2)} ${(220 + radius * Math.sin(angle)).toFixed(2)}`);
      if (u >= 1) { status = 'captured: reaches the horizon'; break; }
      if (u < 1 / 14 && v < 0) { status = 'escapes outward'; break; }
    }
    document.getElementById('ray-path').setAttribute('d', points.join(' '));
    document.getElementById('ray-result').textContent = `b/rₛ = ${b.toFixed(4)} · ${status} · ${(angle * 180 / Math.PI).toFixed(1)}° swept around the centre`;
  }
  impact.addEventListener('input', drawRay);
  document.querySelectorAll('[data-impact]').forEach(button => button.addEventListener('click', () => {
    impact.value = button.dataset.impact;
    drawRay();
  }));
  drawRay();

  const shift = document.getElementById('aa-shift');
  function drawAA() {
    const phase = Number(shift.value), shapes = [];
    const inside = (x, y) => y < 0.75 * x + 1.15 + phase;
    const bg = [243, 246, 236], fg = [19, 120, 111];
    for (let panel = 0; panel < 3; panel++) {
      for (let y = 0; y < 10; y++) for (let x = 0; x < 10; x++) {
        let coverage = 0;
        if (panel === 0) coverage = Number(inside(x + 0.5, y + 0.5));
        else if (panel === 1) coverage = (Number(inside(x + 0.25, y + 0.25)) + Number(inside(x + 0.75, y + 0.75))) / 2;
        else {
          for (let sy = 0; sy < 16; sy++) for (let sx = 0; sx < 16; sx++) coverage += Number(inside(x + (sx + 0.5) / 16, y + (sy + 0.5) / 16)) / 256;
        }
        const colour = bg.map((c, i) => Math.round(c * (1 - coverage) + fg[i] * coverage));
        shapes.push(`<rect x="${42 + 295 * panel + 24 * x}" y="${60 + 24 * y}" width="24" height="24" fill="rgb(${colour.join(',')})" stroke="#c9d7d2" stroke-width="0.5"/>`);
      }
    }
    document.getElementById('aa-cells').innerHTML = shapes.join('');
    document.getElementById('aa-result').textContent = `Edge offset ${phase.toFixed(2)} pixels · dense reference: 256 samples per cell · two samples still miss some coverage`;
  }
  shift.addEventListener('input', drawAA);
  drawAA();

  const sections = [...document.querySelectorAll('section[id]')];
  let navFrame = false;
  function updateNavigation() {
    const current = sections.filter(section => section.getBoundingClientRect().top < innerHeight * 0.3).at(-1);
    document.querySelectorAll('nav a').forEach(a => a.classList.toggle('active', Boolean(current) && a.hash === '#' + current.id));
    navFrame = false;
  }
  addEventListener('scroll', () => {
    if (!navFrame) { navFrame = true; requestAnimationFrame(updateNavigation); }
  }, {passive: true});
  updateNavigation();
  let openedForPrint = [];
  addEventListener('beforeprint', () => {
    openedForPrint = [...document.querySelectorAll('details:not([open])')];
    openedForPrint.forEach(details => details.open = true);
  });
  addEventListener('afterprint', () => openedForPrint.forEach(details => details.open = false));
})();
